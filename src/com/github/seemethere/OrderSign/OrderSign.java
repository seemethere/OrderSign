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
    private HashMap<String, String> i_sign = new HashMap<String, String>();
    private String error_m = "Use /ordersign to see the available signs";
    private Set<String> signs = null;

    @Override
    public void onDisable() {
        i_sign = null;
        error_m = null;
        signs = null;
        economyApi = null;
        logger.info(this.toString() + "has been Disabled!");
        logger = null;
    }

    @Override
    public void onEnable() {
        LoadConfig();
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null)
            economyApi = economyProvider.getProvider();
        else
            logger.severe("[OrderSign] Unable to initialize Economy Interface with Vault!");
        if (this.getConfig().getConfigurationSection("signs") != null)
            signs = this.getConfig().getConfigurationSection("signs").getKeys(false);
        this.getServer().getPluginManager().registerEvents(this, this);
        logger.info(this.toString() + "has been Enabled!");
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
            if (!(sender instanceof Player)) {
                logger.info(PLUGIN_NAME + "NO COMMANDS FOR CONSOLE");
                return true;
            }
            Player p = (Player) sender;
            if (args.length > 1) {
                sender.sendMessage(String.format("%s%sToo many arguments! %s", PLUGIN_NAME, ChatColor.RED, error_m));
                return true;
            }
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("about") || args[0].equalsIgnoreCase("version"))
                    c_about(p);
                    //Checks if the arg entered is a number (page)
                else if (convert_int(args[0]) >= 0)
                    c_list(p, convert_int(args[0]), 5);
                else
                    c_sign(find_Sign(args[0]), p);
            } else //No arguments at all, defaults to first page of signs
                c_list(p, 1, 5);
        }
        return true;
    }

    private void c_about(Player p) {
        //About, for build number
        p.sendMessage(String.format("%s%sby seemethere and jjkoletar", PLUGIN_NAME, ChatColor.AQUA));
        p.sendMessage(String.format("%sVersion: %s", ChatColor.DARK_RED, getDescription().getVersion()));
    }

    private void c_sign(String sign, Player p) {
        Economy e = economyApi;
        double cost;
        if (sign != null) {
            cost = this.getConfig().getDouble("signs." + sign + ".cost");
            //If p does not have enough money
            if (e.getBalance(p.getName()) < cost) {
                p.sendMessage(String.format("%s%sInsufficient Funds! Sign costs %s$%s",
                        PLUGIN_NAME, ChatColor.RED, ChatColor.GREEN, cost));
                return;
            }
            toggleBoughtSign(p, true, sign);
        } else { //If they enter a weird name
            p.sendMessage(String.format("%s%sUnknown sign!", PLUGIN_NAME, ChatColor.RED));
            p.sendMessage(String.format("%s%s%s", PLUGIN_NAME, ChatColor.RED, error_m));
        }
    }

    private void c_list(Player p, int page, int page_sz) {
        //Catch page numbers greater than the max
        if ((((page * page_sz) - 1) - page_sz) > signs.size() || page == 0) {
            p.sendMessage(String.format("%s%sInavlid page number", PLUGIN_NAME, ChatColor.RED));
            return;
        }
        int i = 0;
        int max = page * 5;
        int min = max - 5;
        int pg_max = (int) Math.ceil(signs.size() / (float) page_sz);
        if (page == 1) {
            min = 0;
            max = 5;
        }
        p.sendMessage(String.format("%sAvailable Signs: (Page %d of %d)", ChatColor.YELLOW, page, pg_max));
        for (String s : signs) {
            if (i >= max)
                break;
            else if (i >= min)
                p.sendMessage(String.format("%s* %s%s%s ($%.2f)",
                        ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, s, ChatColor.GREEN,
                        this.getConfig().getDouble("signs." + s + ".cost")));
            i++;
        }
        p.sendMessage(String.format("%s   - Use %s/ordersign <page> %sfor more pages!",
                ChatColor.YELLOW, ChatColor.GREEN, ChatColor.YELLOW));
        p.sendMessage(String.format("%s   - Use %s/ordersign <sign name>%s to order a sign!",
                ChatColor.YELLOW, ChatColor.GREEN, ChatColor.YELLOW));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        Block b = event.getBlock();
        Player p = event.getPlayer();
        if (b.getState() instanceof Sign) {
            BlockState b_st = b.getState();
            Sign sign = (Sign) b_st;
            boolean is_Empty = check_emptySign(sign);
            if (this.i_sign.containsKey(p.getName())) {
                event.setCancelled(true);
                if (is_Empty) {
                    Economy e = economyApi;
                    String s = this.i_sign.get(p.getName());
                    double cost = this.getConfig().getDouble("signs." + s + ".cost");
                    fillSign(p.getName(), b);
                    e.withdrawPlayer(p.getName(), cost);
                    p.sendMessage(String.format("%s%s$%.2f has been charged from your account!",
                            PLUGIN_NAME, ChatColor.GREEN, cost));
                    logger.info("[OrderSign] Sold " + p.getName() + " a "
                            + s + " at " + event.getBlock().getLocation().toString());
                } else
                    p.sendMessage(String.format("%s%sSign was not blank! Transaction Cancelled!",
                            PLUGIN_NAME, ChatColor.RED));
                toggleBoughtSign(p, false, "");
            }
        }
    }

    private int convert_int(String arg) {
        try {  //Catch bad page numbers
            return Integer.valueOf(arg);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String find_Sign(String c) {
        for (String s : signs)
            if (s.equalsIgnoreCase(c))
                return s;
        return null;
    }

    private void toggleBoughtSign(Player p, boolean toggle, String sign) {
        if (toggle) {
            this.i_sign.put(p.getName(), sign);
            p.sendMessage(String.format("%s%sBreak a blank sign to complete your order!",
                    PLUGIN_NAME, ChatColor.LIGHT_PURPLE));
        } else
            this.i_sign.remove(p.getName());
    }

    public String colorHandler(String s) {
        return s.replaceAll("&([0-9a-f])", "\u00A7$1");
    }

    public boolean check_emptySign(Sign sign) {
        for (int i = 0; i < 4; i++)
            if (!sign.getLine(i).isEmpty())
                return false;
        return true;
    }

    public void fillSign(String player, Block block) {
        String s = this.i_sign.get(player);
        Sign sign = (Sign) block.getState();
        for (int i = 0; i < 4; i++)
            sign.setLine(i, colorHandler(this.getConfig().getString("signs." + s + ".line" + (i + 1))));
        sign.update();
    }
}
