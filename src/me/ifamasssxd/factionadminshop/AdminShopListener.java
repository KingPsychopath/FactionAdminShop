package me.ifamasssxd.factionadminshop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdminShopListener implements Listener {
	public HashMap<String, ItemStack> inv_item = new HashMap<String, ItemStack>();
	public HashMap<String, Inventory> inv = new HashMap<String, Inventory>();
	public HashMap<String, ItemStack> item_buying = new HashMap<String, ItemStack>();

	@EventHandler
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Villager) {
			LivingEntity e = (LivingEntity) event.getRightClicked();
			if (FactionAdminShop.villager_shop_inv.containsKey(e)) {
				event.setCancelled(true);
				Player p = event.getPlayer();
				if (FactionAdminShop.removing_vil.contains(p.getName())) {
					p.sendMessage(ChatColor.RED + "Shop " + ChatColor.UNDERLINE + ChatColor.BOLD + "Removed");
					e.remove();
					FactionAdminShop.villager_shop_inv.remove(e);
					FactionAdminShop.villager_loc.remove(e);
					return;
				}
				if (inv_item.containsKey(p.getName())) {
					p.sendMessage(ChatColor.RED + "You are already setting an item.");
					return;
				}
				p.openInventory(FactionAdminShop.villager_shop_inv.get(e));
			}
		}
	}

	@EventHandler
	public void onInventoryOpClick(InventoryClickEvent event) {
		if (!event.getInventory().getTitle().contains("Shop")) {
			return;
		}
		Player p = (Player) event.getWhoClicked();
		if (p.isOp() && p.getGameMode().equals(GameMode.CREATIVE)) {
			/* Handle this later */
			if (event.getRawSlot() < event.getInventory().getSize() && event.getRawSlot() > -1) {
				/* They are clicking in the inventory */
				if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
					ItemStack is = event.getCursor();
					event.setCancelled(true);
					if (is.getType().equals(Material.AIR) || is == null) {
						return;
					}
					inv_item.put(p.getName(), is.clone());
					inv.put(p.getName(), event.getInventory());
					is.setType(Material.AIR);
					p.sendMessage(ChatColor.GOLD + "Please enter the amount for this item.");
					p.closeInventory();
				} else {
					/* They are clicking a item */
					ItemStack current = event.getCurrentItem();
					ItemMeta im = current.getItemMeta();
					if (hasLore("Cost", current)) {
						String s = im.getLore().get(getLoreSpot("Cost", current));
						List<String> lore = im.getLore();
						lore.remove(s);
						im.setLore(lore);
						current.setItemMeta(im);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerThrow(PlayerDropItemEvent event) {
		Player p = event.getPlayer();
		if (inv.containsKey(p.getName())) {
			event.getItemDrop().remove();
		}
	}

	public int getItemCost(ItemStack is) {
		int cost = Integer.parseInt(is.getItemMeta().getLore().get(getLoreSpot("Cost:", is)).split("Cost: ")[1]);
		return cost;
	}

	public boolean hasLore(String s, ItemStack is) {
		if (is.hasItemMeta() && is.getItemMeta().hasLore()) {
			for (String ss : is.getItemMeta().getLore()) {
				if (ss.contains(s)) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void onRegularPlayerClick(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();
		if (!event.getInventory().getTitle().contains("Shop")) {
			return;
		}
		if (p.getGameMode().equals(GameMode.CREATIVE)) {
			if (p.isOp()) {
				/* Handled elsewhere */
				return;
			}
		}
		/* They are not op so they are trying to buy something. */
		event.setCancelled(true);
		if (event.getRawSlot() < event.getInventory().getSize() && event.getRawSlot() > -1) {
			ItemStack is = event.getCurrentItem();
			if (is.getType() != Material.AIR || is != null) {
				if (item_buying.containsKey(p.getName())) {
					p.sendMessage(ChatColor.RED + "You are currently buying an item.");
					return;
				}
				try {
					p.updateInventory();
					if (is.getType() == Material.AIR) {
						return;
					}
					item_buying.put(p.getName(), is);
					p.sendMessage(ChatColor.YELLOW + "Please type how many items you want to buy.");
					p.closeInventory();
				} catch (Exception e) {
					e.printStackTrace();
					p.sendMessage("Error!");
					return;
				}
			}
		}
	}

	@EventHandler
	public void onEntityTarget(EntityTargetLivingEntityEvent event) {
		if (FactionAdminShop.villager_shop_inv.containsKey(event.getTarget())) {
			event.setCancelled(true);
		}
	}

	public int getLoreSpot(String s, ItemStack is) {
		if (is.hasItemMeta() && is.getItemMeta().hasLore()) {
			int counter = -1;
			for (String ss : is.getItemMeta().getLore()) {
				counter++;
				if (ss.contains(s)) {
					return counter;
				}
			}
		}
		return 0;

	}

	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (FactionAdminShop.villager_shop_inv.containsKey(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player p = event.getPlayer();
		if (inv_item.containsKey(p.getName())) {
			event.setCancelled(true);
			if (event.getMessage().equalsIgnoreCase("cancel")) {
				p.getInventory().addItem(inv_item.get(p.getName()));
				inv_item.remove(p.getName());
				p.sendMessage(ChatColor.RED + "Cancelled.");
				return;
			}
			try {
				int amount = Integer.parseInt(event.getMessage());
				Inventory inve = inv.get(p.getName());
				ItemStack is = inv_item.get(p.getName());
				is.setAmount(1);
				ItemMeta im = is.getItemMeta();
				List<String> lore;
				if (is.hasItemMeta() && is.getItemMeta().hasLore()) {
					lore = is.getItemMeta().getLore();
				} else {
					lore = new ArrayList<String>();
				}
				lore.add(ChatColor.LIGHT_PURPLE + "Cost: " + amount);
				im.setLore(lore);
				is.setItemMeta(im);
				inve.addItem(is);
				p.openInventory(inve);
				inv.remove(p.getName());
				inv_item.remove(p.getName());
				p.sendMessage(ChatColor.RED + "Cost set!");
			} catch (Exception e) {
				p.sendMessage(ChatColor.RED + "Please enter a valid amount.");
				return;
			}

		}
		if (item_buying.containsKey(p.getName())) {
			event.setCancelled(true);
			if (event.getMessage().equalsIgnoreCase("cancel")) {
				inv_item.remove(p.getName());
				p.sendMessage(ChatColor.RED + "Cancelled.");
				return;
			}
			int amount;
			try {
				amount = Integer.parseInt(event.getMessage());
			} catch (Exception e) {
				p.sendMessage(ChatColor.RED + "Please enter a valid amount.");
				return;
			}
			if (amount <= 0) {
				p.sendMessage(ChatColor.RED + "Amount must be greater than 0");
				return;
			}

			int total_cost = getItemCost(item_buying.get(p.getName())) * amount;
			if (total_cost <= 0) {
				p.sendMessage(ChatColor.RED + "This isnt possible. Cancelling...");
				item_buying.remove(p.getName());
				return;
			}
			System.out.print("Cost: " + total_cost);
			if (FactionAdminShop.economy.has(p.getName(), total_cost)) {
				if (p.getInventory().firstEmpty() == -1) {
					p.sendMessage(ChatColor.RED + "You do not have enough space to do this.");
					return;
				}
				ItemStack is = item_buying.get(p.getName()).clone();
				ItemMeta im = is.getItemMeta();
				List<String> lore = im.getLore();
				lore.remove(im.getLore().get(getLoreSpot("Cost", is)));
				im.setLore(lore);
				is.setItemMeta(im);
				Inventory inv = Bukkit.createInventory(null, p.getInventory().getType());
				inv.setContents(p.getInventory().getContents());
				HashMap<Integer, ItemStack> leftOver = inv.addItem(is);
				is.setAmount(1);
				if (!leftOver.isEmpty()) {
					p.sendMessage(ChatColor.RED + "You do not have enough inventory space.");
					return;
				}
				for (int i = 0; i < amount; i++) {
					p.getInventory().addItem(is);
				}
				FactionAdminShop.economy.withdrawPlayer(p.getName(), total_cost);
				item_buying.remove(p.getName());
				p.sendMessage(ChatColor.GOLD + "Purchase successful");
			} else {
				p.sendMessage(ChatColor.RED + "You cannot afford this.");
				p.sendMessage(ChatColor.RED + "Purchasing Cancelled");
				item_buying.remove(p.getName());
				return;
			}
		}
	}
}
