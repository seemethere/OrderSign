package com.github.seemethere.OrderSign;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author seemethere
 */

public class OrderSign extends JavaPlugin implements Listener {

    private static final String PLUGIN_NAME = "[\u00A7eOrderSign\u00A7f] ";
    public Logger logger = Logger.getLogger("Minecraft");
    private Economy economyApi = null;
    private HashMap<String, String> identifyBoughtSign = new HashMap<String, String>();
    private String errorMessage = "Use /ordersign to see the available signs";

    public void onDisable() {
        System.out.println(this.toString() + "has been Disabled!");
    }

    public void onEnable() {

        LoadConfig();

        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economyApi = economyProvider.getProvider();
        } else {
            logger.severe("[OrderSign] Unable to initialize Economy Interface with Vault!");
        }

        this.getServer().getPluginManager().registerEvents(this, this);

        System.out.println(this.toString() + "has been Enabled!");
    }


    private void LoadConfig() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists() && !pluginFolder.mkdir()) {
            System.out.println("Could not make plugin folder!");
            return;
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists())
            this.saveDefaultConfig();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ordersign")) {
            if(!(sender instanceof Player)) {
                logger.info(PLUGIN_NAME + "NO COMMANDS FOR CONSOLE");
                return true;
            }
            Player p = (Player) sender;
            Set<String> signs = this.getConfig().getConfigurationSection("signs").getKeys(false);
            //Too many arguments
            if (args.length > 1) {
                sender.sendMessage(PLUGIN_NAME + ChatColor.RED + "Too many arguments! " + errorMessage);
                return true;
            }
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("about") || args[0].equalsIgnoreCase("version"))
                    c_about(p);
                //Checks if the arg entered is a number (page)
                else if (convert_int(args[0]) >= 0)
                    c_list(p, convert_int(args[0]), signs);
                else
                    c_sign(find_Sign(args[0], signs), p);
            } else
                c_list(p, 1, signs);
        }
        return true;
    }

    private void c_about(Player p) {
        //About, for build number
        p.sendMessage(PLUGIN_NAME);
        p.sendMessage(ChatColor.AQUA + "by seemethere and jjkoletar");
        p.sendMessage(ChatColor.DARK_RED + "Version: " + getDescription().getVersion());
    }

    private void c_sign(String sign, Player p) {
        Economy e = economyApi;
        double cost;
        if (sign != null) {
            cost = this.getConfig().getDouble("signs." + sign + ".cost");
            //If p does not have enough money
            if (e.getBalance(p.getName()) < cost) {
                p.sendMessage(PLUGIN_NAME + ChatColor.RED + "Insufficient Funds! Sign costs " + ChatColor.GREEN + "$" + cost);
                return;
            }
            toggleBoughtSign(p, true, sign);
        } else //If they enter a weird name
            p.sendMessage(PLUGIN_NAME + ChatColor.RED + "Unknown sign! " + errorMessage);
    }

    private int convert_int(String arg) {
        //Catch bad page numbers
        try {
            return Integer.valueOf(arg);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    private void c_list(Player p, int page, Set<String> signs) {
        //Catch page numbers greater than the max
        if ((((page * 5) - 1) - 5) > signs.size() || page == 0) {
            p.sendMessage(PLUGIN_NAME + ChatColor.RED + "Inavlid page number");
            return;
        }
        int i = 0;
        int max = page * 5;
        int min = max - 5;
        int pg_max = (int) Math.ceil(signs.size() / (float) 5);
        if (page == 1) {
            min = 0;
            max = 5;
        }
        p.sendMessage(ChatColor.YELLOW + "Available Signs: (Page " + page + " of " + pg_max + ")");
        for (String s : signs) {
            if (i >= max)
                break;
            else if (i >= min) {
                double cost = this.getConfig().getDouble("signs." + s + ".cost");
                p.sendMessage(ChatColor.YELLOW + "* " + ChatColor.LIGHT_PURPLE + s
                        + ChatColor.GREEN + " ($" + cost + ")");
            }
            i++;
        }
        p.sendMessage(ChatColor.YELLOW + "   - Use " + ChatColor.GREEN + "/ordersign <page> "
                + ChatColor.YELLOW + "for more pages!");
        p.sendMessage(ChatColor.YELLOW + "   - Use " + ChatColor.GREEN + "/ordersign <sign name>"
                + ChatColor.YELLOW + " to order a sign!");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getState() instanceof Sign) {
            BlockState stateBlock = block.getState();
            Sign sign = (Sign) stateBlock;
            boolean check = check_emptySign(sign);

            if (this.identifyBoughtSign.containsKey(player.getName())) {
                event.setCancelled(true);
                if (check) {
                    Economy e = economyApi;
                    String s = this.identifyBoughtSign.get(player.getName());
                    double cost = this.getConfig().getDouble("signs." + s + ".cost");
                    //player.sendMessage("BoughtSign was true!");
                    fillSign(player, block);
                    e.withdrawPlayer(player.getName(), cost);
                    player.sendMessage(PLUGIN_NAME + ChatColor.GREEN + "$" + cost + " has been charged from your account!");
                    logger.info("[OrderSign] Sold " + player.getName() + " a " + s + " at " + event.getBlock().getLocation().toString());
                    toggleBoughtSign(player, false, "");
                } else {
                    player.sendMessage(PLUGIN_NAME + ChatColor.RED + "Sign was not blank! Transaction Cancelled!");
                    toggleBoughtSign(player, false, "");
                }
            }
        }
    }

    private String find_Sign(String c, Set<String> signs) {
        for (String s : signs) {
            if (s.equalsIgnoreCase(c)) {
                return s;
            }
        }
        return null;
    }

    private void toggleBoughtSign(CommandSender sender, boolean toggle, String signBought) {
        try {
            String playerName = sender.getName();

            if (toggle) {
                this.identifyBoughtSign.put(playerName, signBought);
                sender.sendMessage(PLUGIN_NAME + ChatColor.LIGHT_PURPLE + "Break a blank sign to complete your order!");
            } else
                this.identifyBoughtSign.remove(playerName);
        } catch (Exception e) {
            getLogger().info("An error occured with toggleBoughtSign");
        }
    }

    public String colorHandler(String s) {
        return s.replaceAll("&([0-9a-f])", "\u00A7$1");
    }

    public boolean check_emptySign(Sign sign) {
        for (int i = 0; i <= 3; i++)
            if (!sign.getLine(i).isEmpty())
                return false;
        return true;
    }

    public void fillSign(Player player, Block block) {

        String s = this.identifyBoughtSign.get(player.getName());
        BlockState blockstate = block.getState();
        Sign sign = (Sign) blockstate;

        String line0 = colorHandler(this.getConfig().getString("signs." + s + ".line1"));
        String line1 = colorHandler(this.getConfig().getString("signs." + s + ".line2"));
        String line2 = colorHandler(this.getConfig().getString("signs." + s + ".line3"));
        String line3 = colorHandler(this.getConfig().getString("signs." + s + ".line4"));

        try {
            sign.setLine(0, line0);
            sign.setLine(1, line1);
            sign.setLine(2, line2);
            sign.setLine(3, line3);
            sign.update();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
