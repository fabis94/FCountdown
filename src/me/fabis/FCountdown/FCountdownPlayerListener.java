package me.fabis.FCountdown;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class FCountdownPlayerListener extends PlayerListener {
	//Credits to Freezer plugin for this code
	FCountdown plugin;
	public FCountdownPlayerListener (FCountdown instance) {
		plugin = instance;
	}
	
	//Commit test

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if (plugin.frozen.contains(player.getDisplayName().toLowerCase())) {
			event.setCancelled(true);
		}
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && block.getType().equals(Material.SIGN) || block.getType().equals(Material.SIGN_POST)) {
			//Player right clicked a sign LETS DO THIS
		}
	}
	
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (plugin.frozen.contains(player.getDisplayName().toLowerCase())) {
			Location from = event.getFrom();
			player.teleport(from);
		}
	}

		public void onPlayerDropItem(PlayerDropItemEvent event) {
			Player player = event.getPlayer();
			if (plugin.frozen.contains(player.getDisplayName().toLowerCase())) {
				event.setCancelled(true);
			}
		}

		public void onPlayerPickupItem(PlayerPickupItemEvent event) {
			Player player = event.getPlayer();
				if (plugin.frozen.contains(player.getDisplayName().toLowerCase())) {
				event.setCancelled(true);
			}
		}
	
	
}
