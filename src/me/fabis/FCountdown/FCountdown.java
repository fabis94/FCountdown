package me.fabis.FCountdown;

import java.util.List;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

public class FCountdown extends JavaPlugin {
	private final FCountdownPlayerListener playerListener= new FCountdownPlayerListener(this);
	private final FCountdownBlockListener blockListener = new FCountdownBlockListener(this);
	
	public static PermissionHandler permissionHandler;
	Logger log = Logger.getLogger("Minecraft");
	ArrayList<String> frozen = new ArrayList<String>();
	boolean stopEarly = false;
	Player stopPlayer;
	public Configuration config;
	public Configuration teleSigns;
	
	HashMap<String, Integer> currCountdowns = new HashMap<String, Integer>();
	HashMap<String, Integer> cdStarters = new HashMap<String, Integer>();
	HashMap<String, Location> currTeleports = new HashMap<String, Location>();

	//Config settings
	boolean allowFreeze; //Disable/enable freezing
	boolean priorityPublic; //Is Public countdown's priority higher than others'
	boolean allowTeleport; //Allow teleporting after a countdown
	boolean sendNumbers; //Output each number of the countdown in the chat
	
	public boolean registerSign(Player player, World world, int x, int y, int z, int time, boolean freeze) {
		teleSigns = new Configuration(new File(getDataFolder().getPath() + "/telesigns.yml"));
		teleSigns.load();
		//Write new sign
		List<String> signBlocks = teleSigns.getKeys();
		int signId = signBlocks.size();
		teleSigns.setProperty("block"+signId+".x", x);
		teleSigns.setProperty("block"+signId+".y", y);
		teleSigns.setProperty("block"+signId+".z", z);
		teleSigns.setProperty("block"+signId+".world", world.getName().toLowerCase());
		teleSigns.setProperty("block"+signId+".owner", player.getName());
		teleSigns.save();
		return false;
	}
	
	public void onEnable() {
		log.info("FCountdown has been enabled.");
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_DROP_ITEM, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		setupPermissions();
		teleSigns = new Configuration(new File(getDataFolder().getPath() + "/telesigns.yml"));
		teleSigns.setHeader("#Dont touch this file!");
		teleSigns.load();
		config = new Configuration(new File(getDataFolder().getPath() + "/fconfig.yml"));
		config.setHeader("#More info on the forum thread");
		config.load();
		config.save();
		teleSigns.save();
		allowFreeze = config.getBoolean("allowFreeze", true);
		priorityPublic = config.getBoolean("priorityPublic",true);
		allowTeleport = config.getBoolean("allowTeleport", true);
		sendNumbers = config.getBoolean("sendNumbers", true);
		FCountdownCdRunnable cdTask = new FCountdownCdRunnable(this);
		if (this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, cdTask, 0, 20) == -1) {
			log.info("Error: Scheduler returned -1");
		}
		
	}
	
	
	public void onDisable() {
		log.info("FCountdown has been disabled");
	}
	
    private static final Set<String> fCommands = new HashSet<String>(Arrays.asList(new String[] {
            "cdprivate",
            "cdpublic",
            "cdgroup",
            "stop",
            "redstone",
            "about",}));
    
    
	public boolean onCommand(CommandSender sender, Command cmd, String cLabel, String[] cArgs) {
		Player player = null;
		Player starter = null;
		int cdown = 99;
		boolean freeze = false;
		String frozenPeople;
		String groupPeople;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		starter = player;
		if (cmd.getName().equalsIgnoreCase("fcountdown") || cmd.getName().equalsIgnoreCase("fcd")) {
			if (!hasPermission(player, "fcountdown.fcountdown")) {
				return true;
			}
			if (cArgs.length > 0) {
				if (!fCommands.contains(cArgs[0])) {
					return false;
				}
				if (cArgs[0].contains("cdprivate")) {
					if (!hasPermission(player, "fcountdown.fcountdown.cdprivate")) {
						return true;
					}
					if (cArgs.length == 1) {
						return false;
					}
					if (isInteger(cArgs[1])) { //2nd argument
						cdown = Integer.parseInt(cArgs[1]);
					} else {
						player.sendMessage("Incorrect arguments");
						return false;
					}
					if (cArgs.length >= 3) { //3rd argument
						freeze = Boolean.parseBoolean(cArgs[2]);
						if (freeze && allowFreeze) {
							frozen.add(player.getDisplayName().toLowerCase());
							player.sendMessage(ChatColor.GREEN+"[FCountdown] You've been frozen!");
						} else {
							if (allowFreeze) {
								
							} else {
								player.sendMessage(ChatColor.RED+"Freezing has been disabled on this server.");
							}
						}
					}
					if (cArgs.length >= 4) { //Teleporting
						String[] teleArgs = cArgs[3].split(",");
						Location teleLoc;
						World teleWorld;
						int x,y,z;
						switch (teleArgs.length) {
						case 3: //Use default world
							teleWorld = getServer().getWorlds().get(0);
							x = Integer.parseInt(teleArgs[0]);
							y = Integer.parseInt(teleArgs[1]);
							z = Integer.parseInt(teleArgs[2]);
							teleLoc = new Location(teleWorld, x, y, z);
							break;
						case 4: //Use specified world
							teleWorld = getServer().getWorld(cArgs[0]);
							x = Integer.parseInt(teleArgs[1]);
							y = Integer.parseInt(teleArgs[2]);
							z = Integer.parseInt(teleArgs[3]);
							teleLoc = new Location(teleWorld, x, y, z);
							break;
						default:
							return false;
						}
						if (hasPermission(player, "fcountdown.fcountdown.teleprivate")) {
							if (allowTeleport) {
								currTeleports.put(player.getDisplayName().toLowerCase(), teleLoc);
							} else {
								player.sendMessage(ChatColor.RED+"FCountdown teleports are disabled on this server.");
							}
						}
					}
					if (currCountdowns.containsKey(player.getDisplayName().toLowerCase()) || currCountdowns.containsKey("*")) {
						player.sendMessage(ChatColor.RED+"You can't have more than 1 countdown.");
					} else {
						currCountdowns.put(player.getDisplayName().toLowerCase(), cdown);
						cdStarters.put(starter.getDisplayName().toLowerCase(), cdown);
						player.sendMessage(ChatColor.GREEN+"[FCountdown] Countdown started!");
					}
					return true;
				} else if (cArgs[0].contains("about")) {
					player.sendMessage("FCountdown by Fabis");
					return true;
				} else if (cArgs[0].contains("stop")) {
					if (!hasPermission(player, "fcountdown.fcountdown.stop")) {
						return true;
					}
					log.info("Displaytolower: "+player.getDisplayName().toLowerCase());
					if (frozen.contains(player.getDisplayName().toLowerCase())) {
						frozen.remove(player.getDisplayName().toLowerCase());
					}
					currCountdowns.remove(player.getDisplayName().toLowerCase());
					currTeleports.remove(player.getDisplayName().toLowerCase());
					cdStarters.remove(player.getDisplayName().toLowerCase());
					player.sendMessage(ChatColor.GREEN+"[FCountdown] Countdown finished.");
					return true;
				} else if (cArgs[0].contains("cdpublic")) {
					if (!hasPermission(player, "fcountdown.fcountdown.cdpublic")) {
						return true;
					}
					if (cArgs.length == 1) {
						return false;
					}
					if (isInteger(cArgs[1])) {
						cdown = Integer.parseInt(cArgs[1]);
					} else {
						return false;
					}
					
					if (currCountdowns.containsKey("*")) { //Global cdown already exists
						player.sendMessage(ChatColor.RED+"You can't create more than 1 public countdown.");
					} else {
						if (priorityPublic) {
							currTeleports.clear();
							frozen.clear();
							cdStarters.clear();
							currCountdowns.clear();
							currCountdowns.put("*", cdown);
							cdStarters.put(starter.getDisplayName().toLowerCase(), cdown);
							
							if (cArgs.length >= 3) {
								frozenPeople = cArgs[2];
								if (allowFreeze) {
									if (!frozenPeople.equalsIgnoreCase("false")) {
										freeze = true;
										String[] frozenPeopleArray = frozenPeople.split(",");
										for (int i = 0; i < frozenPeopleArray.length; i++) {
											frozen.add(frozenPeopleArray[i].toLowerCase());
											Player frozenPlayer = getServer().getPlayer(frozenPeopleArray[i].toLowerCase());
											frozenPlayer.sendMessage(ChatColor.GREEN+"[FCountdown] You've been frozen!");
										}
									}
								} else {
									player.sendMessage(ChatColor.RED+"Freezing has been disabled on this server.");
								}
							}
							
							if (cArgs.length >= 4) { //Teleporting
								String[] teleArgs = cArgs[3].split(",");
								Location teleLoc;
								World teleWorld;
								double x,y,z;
								switch (teleArgs.length) {
								case 3: //Use default world
									teleWorld = getServer().getWorlds().get(0);
									x = Integer.parseInt(teleArgs[0]);
									y = Integer.parseInt(teleArgs[1]);
									z = Integer.parseInt(teleArgs[2]);
									teleLoc = new Location(teleWorld, x, y, z);
									break;
								case 4: //Use specified world
									teleWorld = getServer().getWorld(cArgs[0]);
									x = Integer.parseInt(teleArgs[1]);
									y = Integer.parseInt(teleArgs[2]);
									z = Integer.parseInt(teleArgs[3]);
									teleLoc = new Location(teleWorld, x, y, z);
									break;
								default:
									return false;
								}
								if (hasPermission(player, "fcountdown.fcountdown.teleall")) {
									if (allowTeleport) {
										currTeleports.put("*", teleLoc);
									} else {
										player.sendMessage(ChatColor.RED+"FCountdown teleports are disabled on this server.");
									}
								}
							}
							
							getServer().broadcastMessage(ChatColor.GREEN+"[FCountdown] Countdown started!");
						} else {
							player.sendMessage(ChatColor.RED+"There is at least 1 countdown going on, at the moment.");
							player.sendMessage(ChatColor.RED+"Please wait untill it ends.");
						}
					}
					return true;
				} else if (cArgs[0].contains("cdgroup")) {
					if (!hasPermission(player, "fcountdown.fcountdown.cdgroup")) {
						return true;
					}
					if (cArgs.length == 1) {
						return false;
					}
					groupPeople = cArgs[1];
					String[] groupPeopleArray = groupPeople.split(",");
					if (isInteger(cArgs[2])) {
						cdown = Integer.parseInt(cArgs[1]);
					} else {
						return false;
					}
					if (cArgs.length >= 4) {
						frozenPeople = cArgs[2];
						if (allowFreeze) {
							freeze = true;
							String[] frozenPeopleArray = frozenPeople.split(",");
							for (int i = 0; i < frozenPeopleArray.length; i++) {
								frozen.add(frozenPeopleArray[i].toLowerCase());
								Player frozenPlayer = getServer().getPlayer(frozenPeopleArray[i].toLowerCase());
								frozenPlayer.sendMessage(ChatColor.GREEN+"[FCountdown] You've been frozen!");
							}
						} else {
							player.sendMessage(ChatColor.RED+"Freezing has been disabled on this server.");
						}
					}
					Location teleLoc = null;
					if (cArgs.length >= 5) { //Teleporting
						if (!allowTeleport) {
							player.sendMessage(ChatColor.RED+"FCountdown teleports are disabled on this server.");
						} else {
							String[] teleArgs = cArgs[3].split(",");
							World teleWorld;
							double x,y,z;
							switch (teleArgs.length) {
							case 3: //Use default world
								teleWorld = getServer().getWorlds().get(0);
								x = Integer.parseInt(teleArgs[0]);
								y = Integer.parseInt(teleArgs[1]);
								z = Integer.parseInt(teleArgs[2]);
								teleLoc = new Location(teleWorld, x, y, z);
								break;
							case 4: //Use specified world
								teleWorld = getServer().getWorld(cArgs[0]);
								x = Integer.parseInt(teleArgs[1]);
								y = Integer.parseInt(teleArgs[2]);
								z = Integer.parseInt(teleArgs[3]);
								teleLoc = new Location(teleWorld, x, y, z);
								break;
							default:
								return false;
							}
						}
					}					

					String ignoredPpl = "";
					cdStarters.put(starter.getDisplayName().toLowerCase(), cdown);
					for (int a = 0; a < groupPeopleArray.length; a++) {
						if (!currCountdowns.containsKey(groupPeopleArray[a])) {
							currCountdowns.put(groupPeopleArray[a].toLowerCase(), cdown);
							if (teleLoc != null) {
								if (hasPermission(player, "fcountdown.fcountdown.telegroup") && allowTeleport) {
									currTeleports.put(groupPeopleArray[a].toLowerCase(), teleLoc);
								}
							}
							Player groupPlayer = getServer().getPlayer(groupPeopleArray[a].toLowerCase());
							groupPlayer.sendMessage(ChatColor.GREEN+"[FCountdown] Countdown started!");
						} else {
							ignoredPpl += groupPeopleArray[a]+",";
						}
					}
					player.sendMessage(ignoredPpl.substring(0,ignoredPpl.length()-1)+" already have a countdown.");
					return true;
				}
			}
			return false;
		}

		return false;
	}
	
    public boolean isInteger(String s) {
    	try {
    		Integer.valueOf(s);
    		return true;
    	} catch (NumberFormatException e) {
    		return false;
    	}
    }
    
    public boolean hasPermission(Player player, String node) {
    	if (permissionHandler.has(player, node)) {
    		return true;
    	}
    	player.sendMessage("You don't have permission to use this command.");
    	log.info(player.getDisplayName() + " doesn't have permission to use this command.");
    	return false;
    }
    
    public boolean primitivePermission(Player player, String node) {
    	return permissionHandler.has(player, node);
    }

    private void setupPermissions() {
        if (permissionHandler != null) {
            return;
        }
        
    Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");
        
        if (permissionsPlugin == null) {
            log.info("Permission system not detected, defaulting to OP");
            return;
        }
        
        permissionHandler = ((Permissions) permissionsPlugin).getHandler();
        log.info("Found and will use plugin "+((Permissions)permissionsPlugin).getDescription().getFullName());
    }
    
    public void reloadConfig() {
        config = new Configuration(new File(getDataFolder().getPath() + "/fconfig.yml"));
        teleSigns = new Configuration(new File(getDataFolder().getPath() + "/telesigns.yml"));
    }

}
