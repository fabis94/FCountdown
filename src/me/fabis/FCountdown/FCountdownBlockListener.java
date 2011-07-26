package me.fabis.FCountdown;

import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

public class FCountdownBlockListener extends BlockListener {
	FCountdown plugin;
	
	public FCountdownBlockListener(FCountdown instance) {
		this.plugin = instance;
	}
	
	@Override
	public void onSignChange(SignChangeEvent event) {
		plugin.log.info("SIGNACHANGEAEVENTAHH");
		Player player = event.getPlayer();
		Block block = event.getBlock();
		String[] signLines = null;
		String[] teleLocs = null;
		if (block.getType().equals(Material.SIGN) || block.getType().equals(Material.SIGN_POST)) {
			signLines = event.getLines();
			if (signLines[0].equalsIgnoreCase("[FCD]")) { //FCD Teleport sign
				if (signLines[2].length() != 0 && plugin.isInteger(signLines[2])) {
					if (!plugin.hasPermission(player, "fcountdown.fcountdown.create.sign")) {
						event.setLine(0, ChatColor.RED+"*INSUFFICIENT");
						event.setLine(1, ChatColor.RED+"PERMISSIONS*");
						event.setLine(2, "");
						event.setLine(3, "");
					}
					teleLocs = signLines[1].split(",");
					int signTime;
					boolean signFreeze = false;
					signTime = Integer.parseInt(signLines[2]);
					if (signLines[3].length() != 0) {
						signFreeze = Boolean.parseBoolean(signLines[3]);
					}
					
					switch (teleLocs.length) {
					case 3:
						event.setLine(0, ChatColor.AQUA+"[FCD]");
						//Registering the sign
						if (plugin.registerSign(player, player.getWorld(), Integer.getInteger(teleLocs[0]), Integer.getInteger(teleLocs[1]), Integer.getInteger(teleLocs[2]), signTime, signFreeze)) {
							player.sendMessage(ChatColor.GREEN+"Your FCD Teleport sign has been registered!");
						} else {
							player.sendMessage(ChatColor.RED+"Your FCD Teleport sign has not been registered!");
						}
						break;
					case 4:
						event.setLine(0, ChatColor.AQUA+"[FCD]");
						//Registering the sign
						if (plugin.registerSign(player, plugin.getServer().getWorld(teleLocs[0]), Integer.getInteger(teleLocs[1]), Integer.getInteger(teleLocs[2]), Integer.getInteger(teleLocs[3]), signTime, signFreeze)) {
							player.sendMessage(ChatColor.GREEN+"Your FCD Teleport sign has been registered!");
						} else {
							player.sendMessage(ChatColor.RED+"Your FCD Teleport sign has not been registered!");
						}
						break;
					default:
						event.setLine(0, ChatColor.RED+"*FCD ERROR*");
						event.setLine(1, "");
						event.setLine(2, "");
						event.setLine(3, "");
						break;
					}
				} else {
						event.setLine(0, ChatColor.RED+"*FCD ERROR*");
						event.setLine(1, "");
						event.setLine(2, "");
						event.setLine(3, "");
				}
			}
		}
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Location blockLoc = block.getLocation();
		int x = blockLoc.getBlockX();
		int y = blockLoc.getBlockY();
		int z = blockLoc.getBlockZ();
		World world = blockLoc.getWorld();
		//String blString = ""+x+","+y+","+z+","+world.getName()+"";
		if (block.getType().equals(Material.SIGN) || block.getType().equals(Material.SIGN_POST)) {
			//Since I can't check Sign lines (for some retarded reason)
			//Have to check locs
			plugin.teleSigns.load();
			//Shit's goin down now:
			Map<String, Object> mapOfConfigs = plugin.teleSigns.getAll();
			List<String> signBlocks = plugin.teleSigns.getKeys();
			int configX;
			int configY;
			int configZ;
			World configWorld;
			for (int i = 0; i < signBlocks.size(); i++) {
				String currentBlock = signBlocks.get(i).toString();
				configX = Integer.parseInt(mapOfConfigs.get(""+currentBlock+".x").toString());
				configY = Integer.parseInt(mapOfConfigs.get(""+currentBlock+".y").toString());
				configZ = Integer.parseInt(mapOfConfigs.get(""+currentBlock+".z").toString());
				configWorld = plugin.getServer().getWorld(mapOfConfigs.get(""+currentBlock+".world").toString().toLowerCase());
				if (configX == x && configY == y && configZ == z && configWorld.equals(world)) {
					//This is an [FCD] sign, yay!
					mapOfConfigs.remove(""+currentBlock+"");
					player.sendMessage("Sign has been unregistered.");
					break; //No need to waste resources after this
				}
			}
			
		}
	}
	
	
}
