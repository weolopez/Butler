package com.weolopez.butler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import java.util.logging.Level;


import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Owner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

//import com.palmergames.bukkit.towny.object.TownBlock;


public class Butler extends JavaPlugin {

    public List<Integer> Boots = new LinkedList<Integer>(java.util.Arrays.asList(301, 305, 309, 313, 317));

    public List<Integer> Chestplates = new LinkedList<Integer>(java.util.Arrays.asList(299, 303, 307, 311, 315));
    //SimpleClans Support
    boolean ClansActive = false;
    public int Crit1Chance;
    public String Crit1Message = "";
    public int Crit2Chance;
    public String Crit2Message = "";
    public int Crit3Chance;
    public String Crit3Message = "";
    public boolean debug = false;
    //***Denizen Hook
    public boolean DieLikePlayers = false;

    public boolean BodyguardsObeyProtection = true;

    public boolean IgnoreListInvincibility = true;

    //Factions Support
    static boolean FactionsActive = false;
    public int GlanceChance;
    public String GlanceMessage = "";
    public boolean GroupsChecked = false;
    public List<Integer> Helmets = new LinkedList<Integer>(java.util.Arrays.asList(298, 302, 306, 310, 314, 91, 86));

    public String HitMessage = "";

    public List<Integer> Leggings = new LinkedList<Integer>(java.util.Arrays.asList(300, 304, 308, 312, 316));

    public int LogicTicks = 10;

    public int magi = -1;

    public int MissChance;

    public String MissMessage = "";

    public net.milkbowl.vault.permission.Permission perms = null;

    public int pyro1 = -1;

    public int pyro2 = -1;
    public int pyro3 = -1;
    public int sc1 = -1;
    public int sc2 = -1;
    public int sc3 = -1;
    public int ButlerEXP = 5;
    public Map<Integer, Double> SpeedBuffs = new HashMap<Integer, Double>();
    public Map<Integer, Double> StrengthBuffs = new HashMap<Integer, Double>();
    // Towny Support
    boolean TownyActive = false;
    //War Support
    boolean WarActive = false;
    public int warlock1 = -1;
    public int warlock2 = -1;
    public int warlock3 = -1;
    public Map<Integer, List<PotionEffect>> WeaponEffects = new HashMap<Integer, List<PotionEffect>>();
    public int witchdoctor = -1;

    boolean checkPlugin(String name) {
        if (getServer().getPluginManager().getPlugin(name) != null) {
            if (getServer().getPluginManager().getPlugin(name).isEnabled()) {
                return true;
            }
        }
        return false;
    }

    public void debug(String s) {
        if (debug) this.getServer().getLogger().info(s);
    }

    public void doGroups() {
        if (!setupPermissions())
            getLogger().log(Level.WARNING, "Could not register with Vault!  the GROUP target will not function.");
        else {
            try {
                String[] Gr = perms.getGroups();
                if (Gr.length == 0) {
                    getLogger().log(Level.WARNING, "No permission groups found.  the GROUP target will not function.");
                    perms = null;
                } else
                    getLogger().log(Level.INFO, "Registered sucessfully with Vault: " + Gr.length + " groups found. The GROUP: target will function");

            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error getting groups.  the GROUP target will not function.");
                perms = null;
            }
        }

        GroupsChecked = true;

    }

    public boolean equip(NPC npc, ItemStack hand) {
        Equipment trait = npc.getTrait(Equipment.class);
        if (trait == null) return false;
        int slot = 0;
        Material type = hand == null ? Material.AIR : hand.getType();
        // First, determine the slot to edit

        if (Helmets.contains(type.getId())) slot = 1;
        else if (Chestplates.contains(type.getId())) slot = 2;
        else if (Leggings.contains(type.getId())) slot = 3;
        else if (Boots.contains(type.getId())) slot = 4;

        // Now edit the equipment based on the slot
        // Set the proper slot with one of the item

        if (type == Material.AIR) {
            for (int i = 0; i < 5; i++) {
                if (trait.get(i) != null && trait.get(i).getType() != Material.AIR) {
                    try {
                        trait.set(i, null);
                    } catch (Exception e) {
                        // TODO: Handle?
                    }
                }
            }
            return true;
        } else {
            ItemStack clone = hand.clone();
            clone.setAmount(1);

            try {
                trait.set(slot, clone);
            } catch (Exception e) {
                return false;
            }
            return true;
        }

    }

    private int GetMat(String S) {
        int item = -1;

        if (S == null) return item;

        String[] args = S.toUpperCase().split(":");


        org.bukkit.Material M = org.bukkit.Material.getMaterial(args[0]);

        if (item == -1) {
            try {
                item = Integer.parseInt(S.split(":")[0]);
            } catch (Exception e) {
                // TODO: Handle?
            }
        }

        if (M != null) {
            item = M.getId();
        }

        return item;
    }

    private PotionEffect getpot(String S) {
        if (S == null) return null;
        String[] args = S.trim().split(":");

        PotionEffectType type = null;

        int dur = 10;
        int amp = 1;

        type = PotionEffectType.getByName((args[0].toUpperCase()));

        if (type == null) {
            try {
                type = PotionEffectType.getById(Integer.parseInt(args[0]));
            } catch (Exception e) {
                // TODO: Handle?
            }
        }

        if (type == null) return null;

        if (args.length > 1) {
            try {
                dur = Integer.parseInt(args[1]);
            } catch (Exception e) {
                // TODO: Handle?
            }
        }

        if (args.length > 2) {
            try {
                amp = Integer.parseInt(args[2]);
            } catch (Exception e) {
                // TODO: Handle?
            }
        }

        return new PotionEffect(type, dur, amp);
    }

    public ButlerInstance getButler(Entity ent) {
        if (ent == null) return null;
        if (!(ent instanceof org.bukkit.entity.LivingEntity)) return null;
        NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(ent);
        if (npc != null && npc.hasTrait(ButlerTrait.class)) {
            return npc.getTrait(ButlerTrait.class).getInstance();
        }

        return null;
    }

    public ButlerInstance getButler(NPC npc) {
        if (npc != null && npc.hasTrait(ButlerTrait.class)) {
            return npc.getTrait(ButlerTrait.class).getInstance();
        }
        return null;
    }

    public void loaditemlist(String key, List<Integer> list) {
        List<String> strs = getConfig().getStringList(key);

        if (strs.size() > 0) list.clear();

        for (String s : getConfig().getStringList(key)) {
            int item = GetMat(s.trim());
            list.add(item);
        }

    }


    private void loadmap(String node, Map<Integer, Double> map) {
        map.clear();
        for (String s : getConfig().getStringList(node)) {
            String[] args = s.trim().split(" ");
            if (args.length != 2) continue;

            double val = 0;

            try {
                val = Double.parseDouble(args[1]);
            } catch (Exception e) {
                // TODO: Handle?
            }

            int item = GetMat(args[0]);

            if (item > 0 && val != 0 && !map.containsKey(item)) {
                map.put(item, val);
            }
        }
    }

    private void loadpots(String node, Map<Integer, List<PotionEffect>> map) {
        map.clear();
        for (String s : getConfig().getStringList(node)) {
            String[] args = s.trim().split(" ");

            if (args.length < 2) continue;


            int item = GetMat(args[0]);

            List<PotionEffect> list = new ArrayList<PotionEffect>();

            for (int i = 1; i < args.length; i++) {
                PotionEffect val = getpot(args[i]);
                if (val != null) list.add(val);

            }

            if (item > 0 && !list.isEmpty()) map.put(item, list);


        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] inargs) {

        if (inargs.length < 1) {
            sender.sendMessage(ChatColor.RED + "Use /butler help for command reference.");
            return true;
        }

        CommandSender player = sender;

        int npcid = -1;
        int i = 0;

        //did player specify a id?
        if (tryParseInt(inargs[0])) {
            npcid = Integer.parseInt(inargs[0]);
            i = 1;
        }

        String[] args = new String[inargs.length - i];

        for (int j = i; j < inargs.length; j++) {
            args[j - i] = inargs[j];
        }


        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Use /butler help for command reference.");
            return true;
        }


        Boolean set = null;
        if (args.length == 2) {
            if (args[1].equalsIgnoreCase("true")) set = true;
            else if (args[1].equalsIgnoreCase("false")) set = false;
        }


        if (args[0].equalsIgnoreCase("help")) {

            player.sendMessage(ChatColor.GOLD + "------- Butler Commands -------");
            player.sendMessage(ChatColor.GOLD + "You can use /butler (id) [command] [args] to perform any of these commands on a butler without having it selected.");
            player.sendMessage(ChatColor.GOLD + "");
            player.sendMessage(ChatColor.GOLD + "/butler reload");
            player.sendMessage(ChatColor.GOLD + "  reload the config.yml");
            player.sendMessage(ChatColor.GOLD + "/butler target [add|remove] [target]");
            player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to attack.");
            player.sendMessage(ChatColor.GOLD + "/butler target [list|clear]");
            player.sendMessage(ChatColor.GOLD + "  View or clear the target list..");
            player.sendMessage(ChatColor.GOLD + "/butler ignore [add|remove] [target]");
            player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to ignore.");
            player.sendMessage(ChatColor.GOLD + "/butler ignore [list|clear]");
            player.sendMessage(ChatColor.GOLD + "  View or clear the ignore list..");
            player.sendMessage(ChatColor.GOLD + "/butler info");
            player.sendMessage(ChatColor.GOLD + "  View all Butler attributes");
            player.sendMessage(ChatColor.GOLD + "/butler equip [item|none]");
            player.sendMessage(ChatColor.GOLD + "  Equip an item on the Butler, or remove all equipment.");
            player.sendMessage(ChatColor.GOLD + "/butler speed [0-1.5]");
            player.sendMessage(ChatColor.GOLD + "  Sets speed of the Butler when attacking.");
            player.sendMessage(ChatColor.GOLD + "/butler health [1-2000000]");
            player.sendMessage(ChatColor.GOLD + "  Sets the Butler's Health .");
            player.sendMessage(ChatColor.GOLD + "/butler armor [0-2000000]");
            player.sendMessage(ChatColor.GOLD + "  Sets the Butler's Armor.");
            player.sendMessage(ChatColor.GOLD + "/butler strength [0-2000000]");
            player.sendMessage(ChatColor.GOLD + "  Sets the Butler's Strength.");
            player.sendMessage(ChatColor.GOLD + "/butler attackrate [0.0-30.0]");
            player.sendMessage(ChatColor.GOLD + "  Sets the time between the Butler's projectile attacks.");
            player.sendMessage(ChatColor.GOLD + "/butler healrate [0.0-300.0]");
            player.sendMessage(ChatColor.GOLD + "  Sets the frequency the butler will heal 1 point. 0 to disable.");
            player.sendMessage(ChatColor.GOLD + "/butler range [1-100]");
            player.sendMessage(ChatColor.GOLD + "  Sets the Butler's detection range.");
            player.sendMessage(ChatColor.GOLD + "/butler warningrange [0-50]");
            player.sendMessage(ChatColor.GOLD + "  Sets the range, beyond the detection range, that the Butler will warn targets.");
            player.sendMessage(ChatColor.GOLD + "/butler respawn [-1-2000000]");
            player.sendMessage(ChatColor.GOLD + "  Sets the number of seconds after death the Butler will respawn.");
            player.sendMessage(ChatColor.GOLD + "/butler follow [0-32]");
            player.sendMessage(ChatColor.GOLD + "  Sets the number of block away a bodyguard will follow. Default is 4");
            player.sendMessage(ChatColor.GOLD + "/butler invincible");
            player.sendMessage(ChatColor.GOLD + "  Toggle the Butler to take no damage or knockback.");
            player.sendMessage(ChatColor.GOLD + "/butler retaliate");
            player.sendMessage(ChatColor.GOLD + "  Toggle the Butler to always attack an attacker.");
            player.sendMessage(ChatColor.GOLD + "/butler criticals");
            player.sendMessage(ChatColor.GOLD + "  Toggle the Butler to take critical hits and misses");
            player.sendMessage(ChatColor.GOLD + "/butler drops");
            player.sendMessage(ChatColor.GOLD + "  Toggle the Butler to drop equipped items on death");
            player.sendMessage(ChatColor.GOLD + "/butler killdrops");
            player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the butler's victims drop items and exp");
            player.sendMessage(ChatColor.GOLD + "/butler mount");
            player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the butler rides a mount");
            player.sendMessage(ChatColor.GOLD + "/butler targetable");
            player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the butler is attacked by hostile mobs");
            player.sendMessage(ChatColor.GOLD + "/butler spawn");
            player.sendMessage(ChatColor.GOLD + "  Set the butler to respawn at its current location");
            player.sendMessage(ChatColor.GOLD + "/butler warning 'The Test to use'");
            player.sendMessage(ChatColor.GOLD + "  Change the warning text. <NPC> and <PLAYER> can be used as placeholders");
            player.sendMessage(ChatColor.GOLD + "/butler greeting 'The text to use'");
            player.sendMessage(ChatColor.GOLD + "  Change the greeting text. <NPC> and <PLAYER> can be used as placeholders");
            return true;
        } else if (args[0].equalsIgnoreCase("debug")) {

            debug = !debug;

            player.sendMessage(ChatColor.GREEN + "Debug now: " + debug);
            return true;
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("butler.reload")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            this.reloadMyConfig();
            player.sendMessage(ChatColor.GREEN + "reloaded Butler/config.yml");
            return true;
        }
        NPC ThisNPC;

        if (npcid == -1) {

            ThisNPC = ((Citizens) this.getServer().getPluginManager().getPlugin("Citizens")).getNPCSelector().getSelected(sender);

            if (ThisNPC != null) {
                // Gets NPC Selected
                npcid = ThisNPC.getId();
            } else {
                player.sendMessage(ChatColor.RED + "You must have a NPC selected to use this command");
                return true;
            }
        }


        ThisNPC = CitizensAPI.getNPCRegistry().getById(npcid);

        if (ThisNPC == null) {
            player.sendMessage(ChatColor.RED + "NPC with id " + npcid + " not found");
            return true;
        }


        if (!ThisNPC.hasTrait(ButlerTrait.class)) {
            player.sendMessage(ChatColor.RED + "That command must be performed on a Butler!");
            return true;
        }


        if (sender instanceof Player && !CitizensAPI.getNPCRegistry().isNPC((Entity) sender)) {

            if (!ThisNPC.getTrait(Owner.class).getOwner().equalsIgnoreCase(player.getName())) {
                //not player is owner
                if (!sender.hasPermission("citizens.admin")) {
                    //no c2 admin.
                    player.sendMessage(ChatColor.RED + "You must be the owner of this Butler to execute commands.");
                    return true;
                }
            }
        }

        // Commands

        ButlerInstance inst = ThisNPC.getTrait(ButlerTrait.class).getInstance();

        if (args[0].equalsIgnoreCase("spawn")) {
            if (!player.hasPermission("butler.spawn")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (ThisNPC.getEntity() == null) {
                player.sendMessage(ChatColor.RED + "Cannot set spawn while " + ThisNPC.getName() + " is dead.");
                return true;
            }
            inst.Spawn = ThisNPC.getEntity().getLocation();
            player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will respawn at its present location.");   // Talk to the player.
            return true;

        }

        else if (args[0].equalsIgnoreCase("invincible")) {
            if (!player.hasPermission("butler.options.invincible")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            inst.Invincible = set == null ? !inst.Invincible : set;

            if (!inst.Invincible) {
                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now takes damage..");   // Talk to the player.
            } else {
                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now INVINCIBLE.");   // Talk to the player.
            }


            return true;
        } else if (args[0].equalsIgnoreCase("retaliate")) {
            if (!player.hasPermission("butler.options.retaliate")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            inst.Retaliate = set == null ? !inst.Retaliate : set;

            if (!inst.Retaliate) {
                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will not retaliate.");   // Talk to the player.
            } else {
                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will retalitate against all attackers.");   // Talk to the player.
            }

            return true;
        } else if (args[0].equalsIgnoreCase("criticals")) {
            if (!player.hasPermission("butler.options.criticals")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            inst.LuckyHits = set == null ? !inst.LuckyHits : set;

            if (!inst.LuckyHits) {
                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will take normal damage.");   // Talk to the player.
            } else {
                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will take critical hits.");   // Talk to the player.
            }


            return true;
        } else if (args[0].equalsIgnoreCase("guard")) {
            if (!player.hasPermission("butler.guard")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            if (args.length > 1) {

                String arg = "";
                for (i = 1; i < args.length; i++) {
                    arg += " " + args[i];
                }
                arg = arg.trim();


                if (inst.setGuardTarget(arg)) {
                    player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " is now guarding " + arg);   // Talk to the player.
                } else {
                    player.sendMessage(ChatColor.RED + ThisNPC.getName() + " could not find " + arg + " in range.");   // Talk to the player.
                }
            } else {
                if (inst.guardTarget == null) {
                    player.sendMessage(ChatColor.RED + ThisNPC.getName() + " is already set to guard its immediate area");   // Talk to the player.
                } else {
                    player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " is now guarding its immediate area. ");   // Talk to the player.
                }
                inst.setGuardTarget(null);

            }
            return true;
        } else if (args[0].equalsIgnoreCase("follow")) {
            if (!player.hasPermission("butler.stats.follow")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Follow Distance is " + inst.FollowDistance);
                player.sendMessage(ChatColor.GOLD + "Usage: /butler follow [#]. Default is 4. ");
            } else {

                int HPs = Integer.valueOf(args[1]);
                if (HPs > 32) HPs = 32;
                if (HPs < 0) HPs = 0;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " follow distance set to " + HPs + ".");   // Talk to the player.
                inst.FollowDistance = HPs * HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("health")) {
            if (!player.hasPermission("butler.stats.health")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Health is " + inst.butlerHealth);
                player.sendMessage(ChatColor.GOLD + "Usage: /butler health [#]   note: Typically players");
                player.sendMessage(ChatColor.GOLD + "  have 20 HPs when fully healed");
            } else {

                int HPs = Integer.valueOf(args[1]);
                if (HPs > 2000000) HPs = 2000000;
                if (HPs < 1) HPs = 1;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " health set to " + HPs + ".");   // Talk to the player.
                inst.butlerHealth = HPs;
                inst.setHealth(HPs);
            }

            return true;
        } else if (args[0].equalsIgnoreCase("armor")) {
            if (!player.hasPermission("butler.stats.armor")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Armor is " + inst.Armor);
                player.sendMessage(ChatColor.GOLD + "Usage: /butler armor [#] ");
            } else {

                int HPs = Integer.valueOf(args[1]);
                if (HPs > 2000000) HPs = 2000000;
                if (HPs < 0) HPs = 0;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " armor set to " + HPs + ".");   // Talk to the player.
                inst.Armor = HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("strength")) {
            if (!player.hasPermission("butler.stats.strength")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Strength is " + inst.Strength);
                player.sendMessage(ChatColor.GOLD + "Usage: /butler strength # ");
                player.sendMessage(ChatColor.GOLD + "Note: At Strength 0 the Butler will do no damamge. ");
            } else {

                int HPs = Integer.valueOf(args[1]);
                if (HPs > 2000000) HPs = 2000000;
                if (HPs < 0) HPs = 0;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " strength set to " + HPs + ".");   // Talk to the player.
                inst.Strength = HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("nightvision")) {
            if (!player.hasPermission("butler.stats.nightvision")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Night Vision is " + inst.NightVision);
                player.sendMessage(ChatColor.GOLD + "Usage: /butler nightvision [0-16] ");
                player.sendMessage(ChatColor.GOLD + "Usage: 0 = See nothing, 16 = See everything. ");
            } else {

                int HPs = Integer.valueOf(args[1]);
                if (HPs > 16) HPs = 16;
                if (HPs < 0) HPs = 0;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Night Vision set to " + HPs + ".");   // Talk to the player.
                inst.NightVision = HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("respawn")) {
            if (!player.hasPermission("butler.stats.respawn")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                if (inst.RespawnDelaySeconds == 0)
                    player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " will not automatically respawn.");
                if (inst.RespawnDelaySeconds == -1)
                    player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " will be deleted upon death");
                if (inst.RespawnDelaySeconds > 0)
                    player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " respawns after " + inst.RespawnDelaySeconds + "s");

                player.sendMessage(ChatColor.GOLD + "Usage: /butler respawn [-1 - 2000000] ");
                player.sendMessage(ChatColor.GOLD + "Usage: set to 0 to prevent automatic respawn");
                player.sendMessage(ChatColor.GOLD + "Usage: set to -1 to *permanently* delete the Butler on death.");
            } else {

                int HPs = Integer.valueOf(args[1]);
                if (HPs > 2000000) HPs = 2000000;
                if (HPs < -1) HPs = -1;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now respawns after " + HPs + "s.");   // Talk to the player.
                inst.RespawnDelaySeconds = HPs;

            }
            return true;
        } else if (args[0].equalsIgnoreCase("speed")) {
            if (!player.hasPermission("butler.stats.speed")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Speed is " + inst.butlerSpeed);
                player.sendMessage(ChatColor.GOLD + "Usage: /butler speed [0.0 - 2.0]");
            } else {

                Float HPs = Float.valueOf(args[1]);
                if (HPs > 2.0) HPs = 2.0f;
                if (HPs < 0.0) HPs = 0f;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " speed set to " + HPs + ".");   // Talk to the player.
                inst.butlerSpeed = HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("attackrate")) {
            if (!player.hasPermission("butler.stats.attackrate")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Projectile Attack Rate is " + inst.AttackRateSeconds + "s between shots.");
                player.sendMessage(ChatColor.GOLD + "Usage: /butler attackrate [0.0 - 30.0]");
            } else {

                Double HPs = Double.valueOf(args[1]);
                if (HPs > 30.0) HPs = 30.0;
                if (HPs < 0.0) HPs = 0.0;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Projectile Attack Rate set to " + HPs + ".");   // Talk to the player.
                inst.AttackRateSeconds = HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("healrate")) {
            if (!player.hasPermission("butler.stats.healrate")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Heal Rate is " + inst.HealRate + "s");
                player.sendMessage(ChatColor.GOLD + "Usage: /butler healrate [0.0 - 300.0]");
                player.sendMessage(ChatColor.GOLD + "Usage: Set to 0 to disable healing");
            } else {

                Double HPs = Double.valueOf(args[1]);
                if (HPs > 300.0) HPs = 300.0;
                if (HPs < 0.0) HPs = 0.0;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Heal Rate set to " + HPs + ".");   // Talk to the player.
                inst.HealRate = HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("range")) {
            if (!player.hasPermission("butler.stats.range")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Range is " + inst.butlerRange);
                player.sendMessage(ChatColor.GOLD + "Usage: /butler range [1 - 100]");
            } else {

                Integer HPs = Integer.valueOf(args[1]);
                if (HPs > 100) HPs = 100;
                if (HPs < 1) HPs = 1;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " range set to " + HPs + ".");   // Talk to the player.
                inst.butlerRange = HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("warningrange")) {
            if (!player.hasPermission("butler.stats.warningrange")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            if (args.length <= 1) {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Warning Range is " + inst.WarningRange);
                player.sendMessage(ChatColor.GOLD + "Usage: /butler warningrangee [0 - 50]");
            } else {

                Integer HPs = Integer.valueOf(args[1]);
                if (HPs > 50) HPs = 50;
                if (HPs < 0) HPs = 0;

                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " warning range set to " + HPs + ".");   // Talk to the player.
                inst.WarningRange = HPs;

            }

            return true;
        } else if (args[0].equalsIgnoreCase("equip")) {
            if (!player.hasPermission("butler.equip")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            if (args.length <= 1) {
                player.sendMessage(ChatColor.RED + "You must specify a Item ID or Name. or specify 'none' to remove all equipment.");
            } else {


                if (ThisNPC.getEntity().getType() == org.bukkit.entity.EntityType.ENDERMAN || ThisNPC.getEntity().getType() == org.bukkit.entity.EntityType.PLAYER) {
                    if (args[1].equalsIgnoreCase("none")) {
                        //remove equipment
                        equip(ThisNPC, null);
                        inst.UpdateWeapon();
                        player.sendMessage(ChatColor.YELLOW + ThisNPC.getName() + "'s equipment cleared.");
                    } else {
                        int mat = GetMat(args[1]);
                        if (mat > 0) {
                            ItemStack is = new ItemStack(mat);
                            if (equip(ThisNPC, is)) {
                                inst.UpdateWeapon();
                                player.sendMessage(ChatColor.GREEN + " equipped " + is.getType().toString() + " on " + ThisNPC.getName());
                            } else player.sendMessage(ChatColor.RED + " Could not equip: invalid mob type?");
                        } else player.sendMessage(ChatColor.RED + " Could not equip: unknown item name");
                    }
                } else player.sendMessage(ChatColor.RED + " Could not equip: must be Player or Enderman type");
            }

            return true;
        } else if (args[0].equalsIgnoreCase("warning")) {
            if (!player.hasPermission("butler.warning")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            if (args.length >= 2) {
                String arg = "";
                for (i = 1; i < args.length; i++) {
                    arg += " " + args[i];
                }
                arg = arg.trim();

                String str = arg.replaceAll("\"$", "").replaceAll("^\"", "").replaceAll("'$", "").replaceAll("^'", "");
                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " warning message set to " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', str) + ".");   // Talk to the player.
                inst.WarningMessage = str;
            } else {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Warning Message is: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', inst.WarningMessage));
                player.sendMessage(ChatColor.GOLD + "Usage: /butler warning 'The Text to use'");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("greeting")) {
            if (!player.hasPermission("butler.greeting")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            if (args.length >= 2) {

                String arg = "";
                for (i = 1; i < args.length; i++) {
                    arg += " " + args[i];
                }
                arg = arg.trim();

                String str = arg.replaceAll("\"$", "").replaceAll("^\"", "").replaceAll("'$", "").replaceAll("^'", "");
                player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Greeting message set to " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', str) + ".");   // Talk to the player.
                inst.GreetingMessage = str;
            } else {
                player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Greeting Message is: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', inst.GreetingMessage));
                player.sendMessage(ChatColor.GOLD + "Usage: /butler greeting 'The Text to use'");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("info")) {
            if (!player.hasPermission("butler.info")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }
            player.sendMessage(ChatColor.GOLD + "------- Butler Info for (" + ThisNPC.getId() + ") " + ThisNPC.getName() + "------");
            player.sendMessage(ChatColor.GREEN + inst.getStats());
            player.sendMessage(ChatColor.GREEN + "Invincible: " + inst.Invincible + "  Retaliate: " + inst.Retaliate);
            player.sendMessage(ChatColor.GREEN + "Drops Items: " + inst.DropInventory + "  Critical Hits: " + inst.LuckyHits);
            player.sendMessage(ChatColor.GREEN + "Kills Drop Items: " + inst.KillsDropInventory + "  Respawn Delay: " + inst.RespawnDelaySeconds + "s");
            player.sendMessage(ChatColor.BLUE + "Status: " + inst.butlerStatus);
            if (inst.meleeTarget == null) {
                if (inst.projectileTarget == null) player.sendMessage(ChatColor.BLUE + "Target: Nothing");
                else player.sendMessage(ChatColor.BLUE + "Target: " + inst.projectileTarget.toString());
            } else player.sendMessage(ChatColor.BLUE + "Target: " + inst.meleeTarget.toString());

            if (inst.getGuardTarget() == null) player.sendMessage(ChatColor.BLUE + "Guarding: My Surroundings");
            else player.sendMessage(ChatColor.BLUE + "Guarding: " + inst.getGuardTarget().toString());

            return true;
        } else if (args[0].equalsIgnoreCase("target")) {
            if (!player.hasPermission("butler.target")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.GOLD + "Usage: /butler target add [entity:Name] or [player:Name] or [group:Name] or [entity:monster] or [entity:player]");
                player.sendMessage(ChatColor.GOLD + "Usage: /butler target remove [target]");
                player.sendMessage(ChatColor.GOLD + "Usage: /butler target clear");
                player.sendMessage(ChatColor.GOLD + "Usage: /butler target list");
                return true;
            } else {

                String arg = "";
                for (i = 2; i < args.length; i++) {
                    arg += " " + args[i];
                }
                arg = arg.trim();

                if (args[1].equals("add") && arg.length() > 0 && arg.split(":").length > 1) {


                    if (!inst.containsTarget(arg.toUpperCase())) inst.validTargets.add(arg.toUpperCase());
                    inst.processTargets();
                    inst.setTarget(null, false);
                    player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Target added. Now targeting " + inst.validTargets.toString());
                    return true;
                } else if (args[1].equals("remove") && arg.length() > 0 && arg.split(":").length > 1) {

                    inst.validTargets.remove(arg.toUpperCase());
                    inst.processTargets();
                    inst.setTarget(null, false);
                    player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Targets removed. Now targeting " + inst.validTargets.toString());
                    return true;
                } else if (args[1].equals("clear")) {


                    inst.validTargets.clear();
                    inst.processTargets();
                    inst.setTarget(null, false);
                    player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Targets cleared.");
                    return true;
                } else if (args[1].equals("list")) {
                    player.sendMessage(ChatColor.GREEN + "Targets: " + inst.validTargets.toString());
                    return true;
                } else {
                    player.sendMessage(ChatColor.GOLD + "Usage: /butler target list");
                    player.sendMessage(ChatColor.GOLD + "Usage: /butler target clear");
                    player.sendMessage(ChatColor.GOLD + "Usage: /butler target add type:name");
                    player.sendMessage(ChatColor.GOLD + "Usage: /butler target remove type:name");
                    player.sendMessage(ChatColor.GOLD + "type:name can be any of the following: entity:MobName entity:monster entity:player entity:all player:PlayerName group:GroupName town:TownName nation:NationName faction:FactionName");


                    return true;
                }
            }
        } else if (args[0].equalsIgnoreCase("ignore")) {
            if (!player.hasPermission("butler.ignore")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.GOLD + "Usage: /butler ignore list");
                player.sendMessage(ChatColor.GOLD + "Usage: /butler ignore clear");
                player.sendMessage(ChatColor.GOLD + "Usage: /butler ignore add type:name");
                player.sendMessage(ChatColor.GOLD + "Usage: /butler ignore remove type:name");
                player.sendMessage(ChatColor.GOLD + "type:name can be any of the following: entity:MobName entity:monster entity:player entity:all player:PlayerName group:GroupName town:TownName nation:NationName faction:FactionName");

                return true;
            } else {

                String arg = "";
                for (i = 2; i < args.length; i++) {
                    arg += " " + args[i];
                }
                arg = arg.trim();

                if (args[1].equals("add") && arg.length() > 0 && arg.split(":").length > 1) {
                    if (!inst.containsIgnore(arg.toUpperCase())) inst.ignoreTargets.add(arg.toUpperCase());
                    inst.processTargets();
                    inst.setTarget(null, false);
                    player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore added. Now ignoring " + inst.ignoreTargets.toString());
                    return true;
                } else if (args[1].equals("remove") && arg.length() > 0 && arg.split(":").length > 1) {

                    inst.ignoreTargets.remove(arg.toUpperCase());
                    inst.processTargets();
                    inst.setTarget(null, false);
                    player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore removed. Now ignoring " + inst.ignoreTargets.toString());
                    return true;
                } else if (args[1].equals("clear")) {

                    inst.ignoreTargets.clear();
                    inst.processTargets();
                    inst.setTarget(null, false);
                    player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore cleared.");
                    return true;
                } else if (args[1].equals("list")) {

                    player.sendMessage(ChatColor.GREEN + "Ignores: " + inst.ignoreTargets.toString());
                    return true;
                } else {

                    player.sendMessage(ChatColor.GOLD + "Usage: /butler ignore add [ENTITY:Name] or [PLAYER:Name] or [GROUP:Name] or [ENTITY:MONSTER]");
                    player.sendMessage(ChatColor.GOLD + "Usage: /butler ignore remove [ENTITY:Name] or [PLAYER:Name] or [GROUP:Name] or [ENTITY:MONSTER]");
                    player.sendMessage(ChatColor.GOLD + "Usage: /butler ignore clear");
                    player.sendMessage(ChatColor.GOLD + "Usage: /butler ignore list");
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onDisable() {

        getLogger().log(Level.INFO, " v" + getDescription().getVersion() + " disabled.");
        Bukkit.getServer().getScheduler().cancelTasks(this);

    }


    boolean DenizenActive = false;

    @Override
    public void onEnable() {

        if (getServer().getPluginManager().getPlugin("Citizens") == null || !getServer().getPluginManager().getPlugin("Citizens").isEnabled()) {
            getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ButlerTrait.class).withName("butler"));

        this.getServer().getPluginManager().registerEvents(new ButlerListener(this), this);

        reloadMyConfig();
    }

    private void reloadMyConfig() {
        this.saveDefaultConfig();
        this.reloadConfig();
        loadmap("StrengthBuffs", StrengthBuffs);
        loadmap("SpeedBuffs", SpeedBuffs);
        loadpots("WeaponEffects", WeaponEffects);
        loaditemlist("Helmets", Helmets);
        loaditemlist("Chestplates", Chestplates);
        loaditemlist("Leggings", Leggings);
        loaditemlist("Boots", Boots);
        pyro1 = GetMat(getConfig().getString("AttackTypes.Pyro1", null));
        pyro2 = GetMat(getConfig().getString("AttackTypes.Pyro2", null));
        pyro3 = GetMat(getConfig().getString("AttackTypes.Pyro3", null));
        sc1 = GetMat(getConfig().getString("AttackTypes.StormCaller1", null));
        sc2 = GetMat(getConfig().getString("AttackTypes.StormCaller2", null));
        witchdoctor = GetMat(getConfig().getString("AttackTypes.WitchDoctor", null));
        magi = GetMat(getConfig().getString("AttackTypes.IceMagi", null));
        sc3 = GetMat(getConfig().getString("AttackTypes.StormCaller3", null));
        warlock1 = GetMat(getConfig().getString("AttackTypes.Warlock1", null));
        warlock2 = GetMat(getConfig().getString("AttackTypes.Warlock2", null));
        warlock3 = GetMat(getConfig().getString("AttackTypes.Warlock3", null));
        DieLikePlayers = getConfig().getBoolean("Server.DieLikePlayers", false);
        BodyguardsObeyProtection = getConfig().getBoolean("Server.BodyguardsObeyProtection", true);
        IgnoreListInvincibility = getConfig().getBoolean("Server.IgnoreListInvincibility", true);
        LogicTicks = getConfig().getInt("Server.LogicTicks", 10);
        ButlerEXP = getConfig().getInt("Server.ExpValue", 5);
        MissMessage = getConfig().getString("GlobalTexts.Miss", null);
        HitMessage = getConfig().getString("GlobalTexts.Hit", null);
        Crit1Message = getConfig().getString("GlobalTexts.Crit1", null);
        Crit2Message = getConfig().getString("GlobalTexts.Crit2", null);
        Crit3Message = getConfig().getString("GlobalTexts.Crit3", null);
        GlanceMessage = getConfig().getString("GlobalTexts.Glance", null);
        MissChance = getConfig().getInt("HitChances.Miss", 0);
        GlanceChance = getConfig().getInt("HitChances.Glance", 0);
        Crit1Chance = getConfig().getInt("HitChances.Crit1", 0);
        Crit2Chance = getConfig().getInt("HitChances.Crit2", 0);
        Crit3Chance = getConfig().getInt("HitChances.Crit3", 0);


    }


    private boolean setupPermissions() {
        try {

            if (getServer().getPluginManager().getPlugin("Vault") == null || !getServer().getPluginManager().getPlugin("Vault").isEnabled()) {
                return false;
            }


            return (perms != null);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private boolean tryParseInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }


}

