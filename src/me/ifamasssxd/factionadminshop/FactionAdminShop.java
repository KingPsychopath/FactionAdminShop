package me.ifamasssxd.factionadminshop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.v1_7_R1.EntityVillager;
import net.minecraft.server.v1_7_R1.GenericAttributes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionAdminShop extends JavaPlugin {

	public static HashMap<LivingEntity, Inventory> villager_shop_inv = new HashMap<LivingEntity, Inventory>();
	public static HashMap<LivingEntity, Location> villager_loc = new HashMap<LivingEntity, Location>();
	public static HashSet<String> removing_vil = new HashSet<String>();
	File shopFile = null;
	FileConfiguration shop = null;
	static FactionAdminShop plugin;
	public static Economy economy;

	public void onEnable() {
		getServer().getPluginManager().registerEvents(new AdminShopListener(), this);
		plugin = this;
		saveDefaultShop();
		setupEconomy();
		loadShops();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for (Entry<LivingEntity, Location> l : villager_loc.entrySet()) {
					l.getKey().teleport(l.getValue());
				}
			}
			/* 5 seconds */
		}, 0, 100);
	}

	public void onDisable() {
		saveShops();
	}

	public static FactionAdminShop getInstance() {
		return plugin;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("spawnshop")) {
			Player p = (Player) sender;
			if (!p.isOp()) {
				return true;
			}
			if (args.length == 1) {
				String name = args[0].replace("_", " ");
				Location l = p.getLocation();
				LivingEntity e = (LivingEntity) l.getWorld().spawnEntity(l, EntityType.VILLAGER);
				e.setCustomName(name.replace("&", "§"));
				e.setCustomNameVisible(true);
				villager_shop_inv.put(e, Bukkit.createInventory(null, 54, "                 " + ChatColor.UNDERLINE + "Shop"));
			}
		} else if (label.equalsIgnoreCase("removeshop")) {
			Player p = (Player) sender;
			if (!p.isOp()) {
				return true;
			}
			if (removing_vil.contains(p.getName())) {
				p.sendMessage(ChatColor.GRAY + "Shop Edit Mode: " + ChatColor.RED + "Disabled");
				p.sendMessage(ChatColor.GOLD + "You are no longer in Shop Edit Mode.");
				removing_vil.remove(p.getName());
			} else {
				p.sendMessage(ChatColor.GRAY + "Shop Edit Mode: " + ChatColor.GREEN + "Enabled");
				p.sendMessage(ChatColor.RED + "To remove a Shop right click it.");
				removing_vil.add(p.getName());
			}
		}
		return false;
	}

	public static void loadShops() {
		/* Format - Name:ShopName$World:WorldX:10Y:10Z:10#SerializedInventory */
		for (String s : getInstance().getShop().getStringList("Shops")) {
			String name = s.split("Name:")[1].split("World")[0];
			int x = Integer.parseInt(s.split("X:")[1].split("Y:")[0]);
			int y = Integer.parseInt(s.split("Y:")[1].split("Z:")[0]);
			int z = Integer.parseInt(s.split("Z:")[1].split("#")[0]);
			Location spawn_loc = new Location(Bukkit.getWorld(s.split("World:")[1].split("X:")[0]), x, y, z);
			Inventory real_inv = Bukkit.createInventory(null, 54, "                 " + ChatColor.UNDERLINE + "Shop");
			Inventory inv = InventorySerialization.convertStringToInv(s.split("#")[1]);
			real_inv.setContents(inv.getContents());
			LivingEntity e = (LivingEntity) spawn_loc.getWorld().spawnEntity(spawn_loc, EntityType.VILLAGER);
			e.setCustomName(name.replace("&", "§"));
			e.setCustomNameVisible(true);
			EntityVillager vil = ((CraftVillager) e).getHandle();
			vil.getAttributeInstance(GenericAttributes.d).setValue(0);
			villager_shop_inv.put(e, real_inv);
			villager_loc.put(e, spawn_loc.clone());
		}
	}

	public static void saveShops() {
		List<String> new_list = new ArrayList<String>();
		for (Entry<LivingEntity, Inventory> inv_data : villager_shop_inv.entrySet()) {
			String new_string = "";
			LivingEntity e = (LivingEntity) inv_data.getKey();
			Inventory inv = inv_data.getValue();
			Location l = villager_loc.get(e);
			new_string = "Name:" + e.getCustomName() + "World:" + l.getWorld().getName() + "X:" + l.getBlockX() + "Y:" + l.getBlockY() + "Z:" + l.getBlockZ() + "#" + InventorySerialization.convertInvToString(inv);
			new_list.add(new_string);
		}
		for (LivingEntity e : villager_shop_inv.keySet()) {
			e.remove();
		}
		getInstance().getShop().set("Shops", new_list);
		getInstance().saveshop();
	}

	public FileConfiguration getShop() {
		if (shop == null) {
			this.reloadshop();
		}
		return shop;
	}

	// Reloads the shop
	public void reloadshop() {
		if (shopFile == null) {
			shopFile = new File(getDataFolder(), "shops.yml");
		}
		shop = YamlConfiguration.loadConfiguration(shopFile);

		// Look for defaults in the jar
		InputStream defConfigStream = this.getResource("shops.yml");
		if (defConfigStream != null) {
			YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
			shop.setDefaults(defConfig);
		}
	}

	// saves the shop
	public void saveshop() {
		if (shop == null || shopFile == null) {
			return;
		}
		try {
			getShop().save(shopFile);
		} catch (IOException ex) {
		}
	}

	// Save shop for first time
	public void saveDefaultShop() {
		if (shopFile == null) {
			shopFile = new File(getDataFolder(), "shops.yml");
		}
		if (!shopFile.exists()) {
			saveResource("shops.yml", false);
		}
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return economy != null;
	}
}
