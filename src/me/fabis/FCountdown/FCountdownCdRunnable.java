package me.fabis.FCountdown;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FCountdownCdRunnable implements Runnable {
	FCountdown plugin;
	
	public FCountdownCdRunnable(FCountdown instance) {
		this.plugin = instance;
	}
	
	public boolean updateCountdowns() {
		int mapSize = plugin.currCountdowns.size();
		if (mapSize < 1) {
			return false;
		}
		String currCountdownsString = plugin.currCountdowns.toString();
		String cCSnoBrackets = currCountdownsString.substring(1, currCountdownsString.length()-1);
		String[] entries = cCSnoBrackets.split(", ");
		plugin.currCountdowns.clear();
		for (int i = 0; i < entries.length; i++) {
			String[] entry = entries[i].split("=");
			String key = entry[0];
			Integer value = Integer.parseInt(entry[1]);
			if (value == 0) {
				//Countdown is over
				plugin.frozen.remove(key);
			} else {
				plugin.currCountdowns.put(key, value-1);
			}
		}
		return true;
	}
	
	public boolean updateStarters() {
		int mapSize = plugin.cdStarters.size();
		if (mapSize < 1) {
			return false;
		}
		String currStarterString = plugin.cdStarters.toString();
		String cSSnoBrackets = currStarterString.substring(1, currStarterString.length()-1);
		String[] entries = cSSnoBrackets.split(", ");
		plugin.cdStarters.clear();
		for (int i = 0; i < entries.length; i++) {
			String[] entry = entries[i].split("=");
			String key = entry[0];
			Integer value = Integer.parseInt(entry[1]);
			if (value == 0) {
				//Countdown is over
				
			} else {
				plugin.cdStarters.put(key, value-1);
			}
		}
		return true;
	}
	
	public void teleportAll(Location loc) {
		Player[] players = plugin.getServer().getOnlinePlayers();
		for (int i = 0; i < players.length; i++) {
			players[i].teleport(loc);
		}
	}
	
	public boolean outputCountdowns() {
		int mapSize = plugin.currCountdowns.size();
		if (mapSize < 1) {
			return false;
		}
		String currCountdownsString = plugin.currCountdowns.toString();
		String cCSnoBrackets = currCountdownsString.substring(1, currCountdownsString.length()-1);
		String[] entries = cCSnoBrackets.split(", ");
		for (int i = 0; i < entries.length; i++) {
			String[] entry = entries[i].split("=");
			String key = entry[0];
			String value = entry[1];
			if (key.equalsIgnoreCase("*")) {
				if (plugin.sendNumbers) {
					plugin.getServer().broadcastMessage(ChatColor.AQUA+value);
				}
				if (Integer.parseInt(value) == 0) {
					plugin.getServer().broadcastMessage(ChatColor.GREEN+"[FCountdown] Countdown finished!");
					if (plugin.currTeleports.containsKey("*")) {
						teleportAll(plugin.currTeleports.get("*"));
					}
				}
			} else {
				Player player = plugin.getServer().getPlayer(key);
				if (plugin.sendNumbers) {
					player.sendMessage(ChatColor.AQUA+value);
				}
				if (Integer.parseInt(value) == 0) {
					player.sendMessage(ChatColor.GREEN+"[FCountdown] Countdown finished.");
					if (plugin.currTeleports.containsKey(player.getDisplayName().toLowerCase())) {
						Location endTele = plugin.currTeleports.get(player.getDisplayName().toLowerCase());
						if (endTele == null) {
							plugin.log.info("WHAT THA HELL MAYNE");
						}
						player.teleport(endTele);
					}
				}
			}
		}
		return true;
	}
	
	public void run() {
			outputCountdowns();
			updateStarters();
			updateCountdowns();
	}
}
