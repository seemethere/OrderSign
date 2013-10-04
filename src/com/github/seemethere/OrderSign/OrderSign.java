/*
 * OrderSign.java
 *
 * Copyright 2013 seemethere
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 *
 *
 */

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author seemethere
 */

public class OrderSign extends JavaPlugin implements Listener {
    private static final String PLUGIN_NAME = "[\u00A7eOrderSign\u00A7f] ";
    public Logger logger;
    private Economy economyApi = null;
    private HashMap<String, String> i_sign = new HashMap<String, String>();
    private String error_m = "Use /ordersign to see the available signs";
    private HashMap<String, SignData> signDataHashMap;

    @Override
    public void onDisable() {
        i_sign = null;
        error_m = null;
        economyApi = null;
        logger.info(this.toString() + "has been Disabled!");
        logger = null;
    }

    @Override
    public void onEnable() {
        if (!LoadConfig())
            return;
        logger = this.getLogger();
        //Set up Economy
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null)
            economyApi = economyProvider.getProvider();
        else
            logger.severe("Unable to initialize Economy Interface with Vault!");

        this.getServer().getPluginManager().registerEvents(this, this);
        logger.info("OrderSign has been Enabled!");
    }

    private boolean LoadConfig() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists() && !pluginFolder.mkdir()) {
            logger.severe("Could not make plugin folder!");
            return false;
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists())
            this.saveDefaultConfig();
        //Get Config
        this.reloadConfig();
        loadSigns();
        return true;
    }

    private void loadSigns() {
        signDataHashMap = new HashMap<String, SignData>();
        if (this.getConfig().getConfigurationSection("signs") != null) {
            for (String s : this.getConfig().getConfigurationSection("signs").getKeys(false)) {
                signDataHashMap.put(s, new SignData(s,
                        this.getConfig().getString("signs." + s + ".line" + (1)),
                        this.getConfig().getString("signs." + s + ".line" + (2)),
                        this.getConfig().getString("signs." + s + ".line" + (3)),
                        this.getConfig().getString("signs." + s + ".line" + (4)),
                        this.getConfig().getString("signs." + s + ".permission"),
                        this.getConfig().getDouble("signs." + s + ".cost")));
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ordersign")) {
            if (!(sender instanceof Player)) {
                if (sender.isOp() && args[0].equalsIgnoreCase("reload")) {
                    c_reload(sender);
                    return true;
                }
                logger.info("Not a valid command for console");
                return true;
            }
            Player p = (Player) sender;
            if (args.length > 1) {
                sender.sendMessage(String.format("%s%sToo many arguments! %s", PLUGIN_NAME, ChatColor.RED, error_m));
                return true;
            }
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("about") || args[0].equalsIgnoreCase("version")) {
                    c_about(p);
                } else if (p.isOp() && args[0].equalsIgnoreCase("reload")) {
                    c_reload(p);
                }
                //Checks if the arg entered is a number (page)
                else if (convert_int(args[0]) >= 0) {
                    c_list(p, convert_int(args[0]), 5);
                } else {
                    c_sign(find_Sign(args[0]), p);
                }
            } else { //No arguments at all, defaults to first page of signs
                c_list(p, 1, 5);
            }
        }
        return true;
    }

    private void c_reload(CommandSender sender) {
        if (LoadConfig()) {
            sender.sendMessage(PLUGIN_NAME + "Config reload");
        } else {
            sender.sendMessage(PLUGIN_NAME + ChatColor.DARK_RED + "ERROR RELOADING CONFIG");
        }
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
            if (!p.hasPermission(signDataHashMap.get(sign).getPermission())) {
                p.sendMessage(String.format("%s%sInsufficient permissions for sign!",
                        PLUGIN_NAME, ChatColor.DARK_RED));
                p.sendMessage(String.format("%s%s%s", PLUGIN_NAME, ChatColor.RED, error_m));
                return;
            }
            cost = signDataHashMap.get(sign).getCost();
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

    private void c_list(Player player, int page, int page_sz) {
        //Catch page numbers greater than the max
        List<String> signs = getApplicableSigns(player);
        if (((page * page_sz) - page_sz) >= signs.size() || page == 0) {
            player.sendMessage(String.format("%s%sInavlid page number", PLUGIN_NAME, ChatColor.RED));
            return;
        }
        //Pagination
        int i = 0;
        int pg_max = (int) Math.ceil(signs.size() / (float) page_sz);
        int max = page * page_sz;
        int min = max - page_sz;
        //Actual sending of the message
        player.sendMessage(String.format("%sAvailable Signs: (Page %d of %d)", ChatColor.YELLOW, page, pg_max));
        for (String s : signs) {
            if (i >= max) {
                break;
            } else if (i >= min) {
                player.sendMessage(String.format("%s* %s%s%s ($%.2f)",
                        ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, s, ChatColor.GREEN,
                        signDataHashMap.get(s).getCost()));
            }
            i++;
        }
        player.sendMessage(String.format("%s   - Use %s/ordersign <page> %sfor more pages!",
                ChatColor.YELLOW, ChatColor.GREEN, ChatColor.YELLOW));
        player.sendMessage(String.format("%s   - Use %s/ordersign <sign name>%s to order a sign!",
                ChatColor.YELLOW, ChatColor.GREEN, ChatColor.YELLOW));
    }

    public List<String> getApplicableSigns(Player player) {
        List<String> signs = new ArrayList<String>();
        for (String s : signDataHashMap.keySet()) {
            if (player.hasPermission(signDataHashMap.get(s).getPermission())) {
                signs.add(s);
            }
        }
        return signs;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        Player p = event.getPlayer();
        if (b.getState() instanceof Sign) {
            BlockState b_st = b.getState();
            Sign sign = (Sign) b_st;
            boolean is_Empty = check_emptySign(sign);
            if (this.i_sign.containsKey(p.getName())) {
                event.setCancelled(true);
                Economy e = economyApi;
                String s = this.i_sign.get(p.getName());
                double cost = signDataHashMap.get(s).getCost();
                // Had to add due to a possible exploit where players could be charged to a negative amount
                if (e.getBalance(p.getName()) < cost) {
                    p.sendMessage(String.format("%s%sInsufficient Funds! Sign costs %s$%s",
                            PLUGIN_NAME, ChatColor.RED, ChatColor.GREEN, cost));
                } else if (is_Empty) {
                    fillSign(p.getName(), b);
                    e.withdrawPlayer(p.getName(), cost);
                    p.sendMessage(String.format("%s%s$%.2f has been charged from your account!",
                            PLUGIN_NAME, ChatColor.GREEN, cost));
                    logger.info("[OrderSign] Sold " + p.getName() + " a "
                            + s + " at " + event.getBlock().getLocation().toString());
                } else {
                    p.sendMessage(String.format("%s%sSign was not blank! Transaction Cancelled!",
                            PLUGIN_NAME, ChatColor.RED));
                }
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
        for (String s : signDataHashMap.keySet()) {
            if (s.equalsIgnoreCase(c)) {
                return s;
            }
        }
        return null;
    }

    private void toggleBoughtSign(Player p, boolean toggle, String sign) {
        if (toggle) {
            this.i_sign.put(p.getName(), sign);
            p.sendMessage(String.format("%s%sBreak a blank sign to complete your order!",
                    PLUGIN_NAME, ChatColor.LIGHT_PURPLE));
        } else {
            this.i_sign.remove(p.getName());
        }
    }

    public String color_h(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public boolean check_emptySign(Sign sign) {
        for (int i = 0; i < 4; i++) {
            if (!sign.getLine(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void fillSign(String player, Block block) {
        String s = this.i_sign.get(player);
        Sign sign = (Sign) block.getState();
        for (int i = 0; i < 4; i++) {
            sign.setLine(i, color_h(signDataHashMap.get(s).getLine(i + 1)));
        }
        sign.update();
    }
}