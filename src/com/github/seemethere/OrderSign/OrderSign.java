package com.github.seemethere.OrderSign;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

/**
 * 
 * @author seemethere
 *
 */

public class OrderSign extends JavaPlugin implements Listener{

	public Logger logger = Logger.getLogger("Minecraft");
	FileConfiguration config;
	private Economy economyApi = null;

	public HashMap<String, Boolean> BoughtSign = new HashMap<String, Boolean>();
	public HashMap<String, String> identifyBoughtSign = new HashMap<String, String>();
	
	public void onDisable() {
		System.out.println(this.toString() + "has been Disabled!");
	}

	public void onEnable() {

		LoadConfig();

		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		if(economyProvider != null) {
			economyApi = economyProvider.getProvider();
		} else {
			logger.severe("[OrderSign] Unable to initialize Economy Interface with Vault!");
		}

		this.getServer().getPluginManager().registerEvents(this, this);

		System.out.println(this.toString() + "has been Enabled!");
	}


	private void LoadConfig() {
		File pluginFolder = getDataFolder();
		if(!pluginFolder.exists() && !pluginFolder.mkdir()) {
			System.out.println("Could not make plugin folder!");
			return;
		}

		File configFile = new File(getDataFolder(), "config.yml");
		if(!configFile.exists()) {
			this.saveDefaultConfig();
			return;
		}
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equalsIgnoreCase("ordersign")) {
			String pluginName = colorHandler(this.getConfig().getString("DisplayName"));
			Player player = (Player) sender;
			String errorMessage = "Use /ordersign to see the available signs";
			Set<String> sign = this.getConfig().getConfigurationSection("signs").getKeys(false);
			Economy e = economyApi;

			double cost = 0.00;

			//Too many arguments
			if(args.length > 1) {
				sender.sendMessage(pluginName + ChatColor.RED + "Too many arguments! " + errorMessage);
				return true;
			}

			//If they enter a weird name
			if(args.length == 1 && !sign.contains(args[0])) {
				sender.sendMessage(pluginName + ChatColor.RED + "Unknown sign! " + errorMessage);
				return true;
			}

			//If they enter an available name
			if(args.length == 1 && sign.contains(args[0])) {
				cost = this.getConfig().getDouble("signs." + args[0] + ".cost");

				//If player does not have enough money
				if(e.getBalance(player.getName()) < cost) {
					sender.sendMessage(pluginName + ChatColor.RED + "Insufficient Funds! Sign costs " + ChatColor.GREEN + "$" + cost);
					return true;
				}

				toggleBoughtSign(sender, true, args[0]);

				return true;
			}

			//Displays all available signs
			if(args.length == 0) {
				sender.sendMessage(ChatColor.DARK_RED + "NOTE: All signs are case sensitive");
				sender.sendMessage(ChatColor.YELLOW + "Available Signs: ");
				for(String s : this.getConfig().getConfigurationSection("signs").getKeys(false)) {
					cost = this.getConfig().getDouble("signs." + s + ".cost");
					sender.sendMessage(ChatColor.LIGHT_PURPLE + s + ChatColor.GREEN + " ($" + cost + ")");
				}
				return true;
			}
		}
		return true;

	}


	private void toggleBoughtSign(CommandSender sender, boolean toggle, String signBought) {
		try {
			String pluginName = colorHandler(this.getConfig().getString("DisplayName"));
			String playerName = sender.getName();

			if(toggle == true) {
				this.BoughtSign.put(playerName, true);
				this.identifyBoughtSign.put(playerName, signBought);
				sender.sendMessage(pluginName + ChatColor.LIGHT_PURPLE + "Right click a blank sign to complete your order!");
			}

			if(toggle == false) {
				this.BoughtSign.remove(playerName);
				this.identifyBoughtSign.remove(playerName);
			}

		} catch (Exception e) {
			getLogger().info("An error occured with toggleBoughtSign");
		}
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			String pluginName = colorHandler(this.getConfig().getString("DisplayName"));
			Player player = event.getPlayer();
			Block block = event.getClickedBlock();

			if(block.getState() instanceof Sign) {
				BlockState stateBlock = block.getState();
				Sign sign = (Sign) stateBlock;
				boolean check = emptySign(sign, player);

				if(this.BoughtSign.containsKey(player.getName())) {
					if(check == true) {
						Economy e = economyApi;
						String s = this.identifyBoughtSign.get(player.getName());
						double cost = this.getConfig().getDouble("signs." + s + ".cost");
						//player.sendMessage("BoughtSign was true!");
						fillSign(player, block);
						e.withdrawPlayer(player.getName(), cost);
						player.sendMessage(pluginName + ChatColor.GREEN + "$" + cost + " has been charged from your account!");
						toggleBoughtSign(player, false, "");
						return;
					} else if(check == false) {
						player.sendMessage(pluginName + ChatColor.RED + "Sign was not blank! Transaction Cancelled!");
						toggleBoughtSign(player, false, "");
						return;
					} 
				}
			}
		}
	}

	public String colorHandler(String s) {
		return s.replaceAll("&([0-9a-f])", "\u00A7$1");
	}

	public boolean emptySign(Sign sign, Player p) {

		for(int i = 0; i <= 3; i++) {
			if(!sign.getLine(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public void fillSign(Player player, Block block) {

		String s = this.identifyBoughtSign.get(player.getName());
		BlockState blockstate = block.getState();
		Sign sign = (Sign) blockstate;

		String line0 = colorHandler(this.getConfig().getString("signs." + s + ".1"));
		String line1 = colorHandler(this.getConfig().getString("signs." + s + ".2"));
		String line2 = colorHandler(this.getConfig().getString("signs." + s + ".3"));
		String line3 = colorHandler(this.getConfig().getString("signs." + s + ".4"));

		try {
			sign.setLine(0, line0);
			sign.setLine(1, line1);
			sign.setLine(2, line2);
			sign.setLine(3, line3);
			sign.update();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
