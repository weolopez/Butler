package com.weolopez.butler;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;

//Version Specifics
import net.minecraft.server.v1_7_R1.EntityHuman;
import net.minecraft.server.v1_7_R1.EntityPotion;
import net.minecraft.server.v1_7_R1.Packet;
//import net.minecraft.server.v1_7_R1.Packet18ArmAnimation;
import org.bukkit.craftbukkit.v1_7_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
/////////////////////////

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;


public class ButlerInstance {

    public enum hittype {
        block, disembowel, glance, injure, main, miss, normal,
    }

    public enum Status {
        isDEAD, isDYING, isHOSTILE, isLOOKING, isRETALIATING, isSTUCK, isWWAITING
    }

    private Set<Player> _myDamamgers = new HashSet<Player>();

    private Location _projTargetLostLoc;

    public Integer Armor = 0;

    public Double AttackRateSeconds = 2.0;

    public boolean KillsDropInventory = true;
    public boolean DropInventory = false;
    public boolean Targetable = true;

    public int MountID = -1;

    public boolean isMounted() {
        return MountID >= 0;
    }

    public int epcount = 0;

    public String GreetingMessage = "&a<NPC> says: Welcome, <PLAYER>!";
    public LivingEntity guardEntity = null;
    public String guardTarget = null;

    // Packet healanim = null;
    public Double HealRate = 0.0;

    public List<String> ignoreTargets = new ArrayList<String>();
    public List<String> validTargets = new ArrayList<String>();

    public Set<String> _ignoreTargets = new HashSet<String>();
    public Set<String> _validTargets = new HashSet<String>();

    private boolean inciendary = false;
    public Boolean Invincible = false;
    Long isRespawnable = System.currentTimeMillis();
    boolean lightning = false;
    int lightninglevel = 0;
    public boolean loaded = false;
    public Boolean LuckyHits = true;
    public LivingEntity meleeTarget;
    public NPC myNPC = null;
    private Class<? extends Projectile> myProjectile;
    /* Setables */
    public ButlerTrait myTrait;
    public Integer NightVision = 16;
    private long oktoFire = System.currentTimeMillis();
    private long oktoheal = System.currentTimeMillis();
    private long oktoreasses = System.currentTimeMillis();
    private long okToTakedamage = 0;
    /* plugin Constructer */
    Butler plugin;
    public List<PotionEffect> potionEffects = null;
    ItemStack potiontype = null;
    public LivingEntity projectileTarget;
    Random r = new Random();
    public Integer RespawnDelaySeconds = 10;
    public Boolean Retaliate = true;
    public double butlerHealth = 20;

    public Integer butlerRange = 10;

    public float butlerSpeed = (float) 1.0;

    /* Internals */
    public Status butlerStatus = Status.isDYING;

    public Double butlerWeight = 1.0;

    public Location Spawn = null;

    public Integer Strength = 1;

    /* Technicals */
        private Integer taskID = null;

    public int FollowDistance = 16;

    public String WarningMessage = "&a<NPC> says: Halt! Come no further!";

    public Integer WarningRange = 0;

    private Map<Player, Long> Warnings = new HashMap<Player, Long>();

    public ButlerInstance(Butler plugin) {
        this.plugin = plugin;
        isRespawnable = System.currentTimeMillis();
    }

    public void cancelRunnable() {
        if (taskID != null) {
            plugin.getServer().getScheduler().cancelTask(taskID);
        }
    }


    public boolean hasTargetType(int type) {
        return (this.targets & type) == type;
    }

    public boolean hasIgnoreType(int type) {
        return (this.ignores & type) == type;
    }

    public boolean isIgnored(LivingEntity aTarget) {
        //cheak ignores

        if (aTarget == this.guardEntity) return true;

        if (ignores == 0) return false;

        if (hasIgnoreType(all)) return true;

        if (aTarget instanceof Player && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)) {

            if (hasIgnoreType(players)) return true;

            else {
                String name = ((Player) aTarget).getName();

                if (this.hasIgnoreType(namedplayers) && containsIgnore("PLAYER:" + name)) return true;

                if (this.hasIgnoreType(owner) && name.equalsIgnoreCase(myNPC.getTrait(Owner.class).getOwner()))
                    return true;

            }
        } else if (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)) {

            if (this.hasIgnoreType(npcs)) {
                return true;
            }

            NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(aTarget);

            if (npc != null) {

                String name = npc.getName();

                if (this.hasIgnoreType(namednpcs) && this.containsIgnore("NPC:" + name)) return true;

                else if (hasIgnoreType(groups)) {

                    String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(), name); // world perms
                    String[] groups2 = plugin.perms.getPlayerGroups((World) null, name); //global perms

                    if (groups1 != null) {
                        for (int i = 0; i < groups1.length; i++) {
                            //	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
                            if (this.containsIgnore("GROUP:" + groups1[i])) return true;
                        }
                    }

                    if (groups2 != null) {
                        for (int i = 0; i < groups2.length; i++) {
                            //	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
                            if (this.containsIgnore("GROUP:" + groups2[i])) return true;
                        }
                    }
                }
            }
        } else if (aTarget instanceof Monster && hasIgnoreType(monsters)) return true;

        else if (aTarget instanceof LivingEntity && hasIgnoreType(namedentities)) {
            if (this.containsIgnore("ENTITY:" + aTarget.getType())) return true;
        }


        //not ignored, ok!
        return false;
    }

    public boolean isTarget(LivingEntity aTarget) {

        if (targets == 0 || targets == events) return false;

        if (this.hasTargetType(all)) return true;

        //Check if target
        if (aTarget instanceof Player && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)) {

            if (this.hasTargetType(players)) {
                return true;
            } else {
                String name = ((Player) aTarget).getName();

                if (hasTargetType(namedplayers) && this.containsTarget("PLAYER:" + name)) return true;

                if (this.containsTarget("ENTITY:OWNER") && name.equalsIgnoreCase(myNPC.getTrait(Owner.class).getOwner()))
                    return true;

                if (hasTargetType(groups)) {

                    String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(), name); // world perms
                    String[] groups2 = plugin.perms.getPlayerGroups((World) null, name); //global perms

                    if (groups1 != null) {
                        for (int i = 0; i < groups1.length; i++) {
                            //			plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
                            if (this.containsTarget("GROUP:" + groups1[i])) return true;
                        }
                    }

                    if (groups2 != null) {
                        for (int i = 0; i < groups2.length; i++) {
                            //	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
                            if (this.containsTarget("GROUP:" + groups2[i])) return true;
                        }
                    }
                }
            }
        } else if (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)) {

            if (this.hasTargetType(npcs)) {
                return true;
            }

            NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(aTarget);

            String name = npc.getName();

            if (this.hasTargetType(namednpcs) && containsTarget("NPC:" + name)) return true;

            if (this.hasTargetType(groups)) {

                String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(), name); // world perms
                String[] groups2 = plugin.perms.getPlayerGroups((World) null, name); //global perms
                //		String[] groups3 = plugin.perms.getPlayerGroups(aTarget.getWorld().getName(),name); // world perms
                //	String[] groups4 = plugin.perms.getPlayerGroups((Player)aTarget); // world perms

                if (groups1 != null) {
                    for (int i = 0; i < groups1.length; i++) {
                        //	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
                        if (this.containsTarget("GROUP:" + groups1[i])) return true;
                    }
                }

                if (groups2 != null) {
                    for (int i = 0; i < groups2.length; i++) {
                        //	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
                        if (this.containsTarget("GROUP:" + groups2[i])) return true;
                    }
                }
            }
        } else if (aTarget instanceof Monster && this.hasTargetType(monsters)) return true;

        else if (aTarget instanceof LivingEntity && hasTargetType(namedentities)) {
            if (this.containsTarget("ENTITY:" + aTarget.getType())) return true;
        }
        return false;

    }


    // private Random r = new Random();

    public boolean containsIgnore(String theTarget) {
        return _ignoreTargets.contains(theTarget.toUpperCase());
    }

    public boolean containsTarget(String theTarget) {
        return _validTargets.contains(theTarget.toUpperCase());

    }

    public void deactivate() {
        plugin.getServer().getScheduler().cancelTask(taskID);
    }

    public void die(boolean runscripts, org.bukkit.event.entity.EntityDamageEvent.DamageCause cause) {
        if (butlerStatus == Status.isDYING || butlerStatus == Status.isDEAD) return;

        butlerStatus = Status.isDYING;

        setTarget(null, false);
        //		myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);

        butlerStatus = Status.isDEAD;

        if (this.DropInventory)
            myNPC.getEntity().getLocation().getWorld().spawn(myNPC.getEntity().getLocation(), ExperienceOrb.class).setExperience(plugin.ButlerEXP);


        List<ItemStack> items = new java.util.LinkedList<ItemStack>();

        if (myNPC.getEntity() instanceof HumanEntity) {
            //get drop inventory.
            for (ItemStack is : ((HumanEntity) myNPC.getEntity()).getInventory().getArmorContents()) {
                if (is.getTypeId() > 0) items.add(is);
            }

            ItemStack is = ((HumanEntity) myNPC.getEntity()).getInventory().getItemInHand();
            if (is.getTypeId() > 0) items.add(is);

            ((HumanEntity) myNPC.getEntity()).getInventory().clear();
            ((HumanEntity) myNPC.getEntity()).getInventory().setArmorContents(null);
            ((HumanEntity) myNPC.getEntity()).getInventory().setItemInHand(null);
        }

        if (items.isEmpty()) myNPC.getEntity().playEffect(EntityEffect.DEATH);
        else myNPC.getEntity().playEffect(EntityEffect.HURT);

        if (!DropInventory) items.clear();

        for (ItemStack is : items) {
            myNPC.getEntity().getWorld().dropItemNaturally(myNPC.getEntity().getLocation(), is);
        }


        if (plugin.DieLikePlayers) {
            //die!
            ((LivingEntity) myNPC.getEntity()).setHealth((double) 0);
        } else {
            org.bukkit.event.entity.EntityDeathEvent ed = new org.bukkit.event.entity.EntityDeathEvent((LivingEntity) myNPC.getEntity(), items);

            plugin.getServer().getPluginManager().callEvent(ed);
            //citizens will despawn it.

        }


    }


    private void faceEntity(Entity from, Entity at) {

        if (from.getWorld() != at.getWorld())
            return;
        Location loc = from.getLocation();

        double xDiff = at.getLocation().getX() - loc.getX();
        double yDiff = at.getLocation().getY() - loc.getY();
        double zDiff = at.getLocation().getZ() - loc.getZ();

        double distanceXZ = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
        double distanceY = Math.sqrt(distanceXZ * distanceXZ + yDiff * yDiff);

        double yaw = (Math.acos(xDiff / distanceXZ) * 180 / Math.PI);
        double pitch = (Math.acos(yDiff / distanceY) * 180 / Math.PI) - 90;
        if (zDiff < 0.0) {
            yaw = yaw + (Math.abs(180 - yaw) * 2);
        }

        net.citizensnpcs.util.NMS.look((LivingEntity) from, (float) yaw - 90, (float) pitch);

    }

    private void faceForward() {
        net.citizensnpcs.util.NMS.look(myNPC.getEntity(), myNPC.getEntity().getLocation().getYaw(), 0);
    }

    private void faceAlignWithVehicle() {
        org.bukkit.entity.Entity v = myNPC.getEntity().getVehicle();
        net.citizensnpcs.util.NMS.look((LivingEntity) myNPC.getEntity(), v.getLocation().getYaw(), 0);
    }

    public LivingEntity findTarget(Integer Range) {
        Range += WarningRange;
        List<Entity> EntitiesWithinRange = myNPC.getEntity().getNearbyEntities(Range, Range, Range);
        LivingEntity theTarget = null;
        Double distanceToBeat = 99999.0;

        // plugin.getServer().broadcastMessage("Targets scanned : " +
        // EntitiesWithinRange.toString());

        for (Entity aTarget : EntitiesWithinRange) {
            if (!(aTarget instanceof LivingEntity)) continue;

            // find closest target

            if (!isIgnored((LivingEntity) aTarget) && isTarget((LivingEntity) aTarget)) {

                // can i see it?
                // too dark?
                double ll = aTarget.getLocation().getBlock().getLightLevel();
                // sneaking cut light in half
                if (aTarget instanceof Player)
                    if (((Player) aTarget).isSneaking())
                        ll /= 2;

                // too dark?
                if (ll >= (16 - this.NightVision)) {


                    double dist = aTarget.getLocation().distance(myNPC.getEntity().getLocation());

                    if (hasLOS(aTarget)) {


                        if (WarningRange > 0 && butlerStatus == Status.isLOOKING && aTarget instanceof Player && dist > (Range - WarningRange) && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget) & !(WarningMessage.isEmpty())) {

                            if (Warnings.containsKey(aTarget) && System.currentTimeMillis() < Warnings.get(aTarget) + 60 * 1000) {
                                //already warned u in last 30 seconds.
                            } else {
                                ((Player) aTarget).sendMessage(getWarningMessage((Player) aTarget));
                                if (!getNavigator().isNavigating()) faceEntity(myNPC.getEntity(), aTarget);
                                Warnings.put((Player) aTarget, System.currentTimeMillis());
                            }

                        } else if (dist < distanceToBeat) {
                            // now find closes mob
                            distanceToBeat = dist;
                            theTarget = (LivingEntity) aTarget;
                        }
                    }


                }

            } else {
                //not a target

                if (WarningRange > 0 && butlerStatus == Status.isLOOKING && aTarget instanceof Player && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget) && !(GreetingMessage.isEmpty())) {
                    boolean LOS = ((LivingEntity) myNPC.getEntity()).hasLineOfSight(aTarget);
                    if (LOS) {
                        if (Warnings.containsKey(aTarget) && System.currentTimeMillis() < Warnings.get(aTarget) + 60 * 1000) {
                            //already greeted u in last 30 seconds.
                        } else {
                            ((Player) aTarget).sendMessage(getGreetingMEssage((Player) aTarget));
                            faceEntity(myNPC.getEntity(), aTarget);
                            Warnings.put((Player) aTarget, System.currentTimeMillis());
                        }
                    }
                }

            }

        }


        if (theTarget != null) {
            // plugin.getServer().broadcastMessage("Targeting: " +
            // theTarget.toString());
            return theTarget;
        }

        return null;
    }


    String getGreetingMEssage(Player player) {
        String str = GreetingMessage.replace("<NPC>", myNPC.getName()).replace("<PLAYER>", player.getName());
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public LivingEntity getGuardTarget() {
        return this.guardEntity;
    }

    public double getHealth() {
        if (myNPC == null) return 0;
        if (myNPC.getEntity() == null) return 0;
        return ((CraftLivingEntity) myNPC.getEntity()).getHealth();
    }

    public float getSpeed() {
        if (!myNPC.isSpawned()) return butlerSpeed;
        double mod = 0;
        if (myNPC.getEntity() instanceof Player) {
            for (ItemStack is : ((Player) myNPC.getEntity()).getInventory().getArmorContents()) {
                if (plugin.SpeedBuffs.containsKey(is.getTypeId())) mod += plugin.SpeedBuffs.get(is.getTypeId());
            }
        }
        return (float) (butlerSpeed + mod) * (this.myNPC.getEntity().isInsideVehicle() ? 2 : 1);
    }

    public String getStats() {
        DecimalFormat df = new DecimalFormat("#.0");
        double h = getHealth();

        return ChatColor.RED + "[HP]:" + ChatColor.WHITE + h + "/" + butlerHealth + ChatColor.RED + " [AP]:" +
                ChatColor.RED + " [STR]:" + ChatColor.WHITE + getStrength() + ChatColor.RED + " [SPD]:" + ChatColor.WHITE + df.format(getSpeed()) +
                ChatColor.RED + " [RNG]:" + ChatColor.WHITE + butlerRange + ChatColor.RED + " [ATK]:" + ChatColor.WHITE + AttackRateSeconds + ChatColor.RED + " [VIS]:" + ChatColor.WHITE + NightVision +
                ChatColor.RED + " [HEAL]:" + ChatColor.WHITE + HealRate + ChatColor.RED + " [WARN]:" + ChatColor.WHITE + WarningRange + ChatColor.RED + " [FOL]:" + ChatColor.WHITE + Math.sqrt(FollowDistance);

    }

    public int getStrength() {
        double mod = 0;

        if (myNPC.getEntity() instanceof Player) {
            if (plugin.StrengthBuffs.containsKey(((Player) myNPC.getEntity()).getInventory().getItemInHand().getTypeId()))
                mod += plugin.StrengthBuffs.get(((Player) myNPC.getEntity()).getInventory().getItemInHand().getTypeId());
        }

        return (int) (Strength + mod);
    }

    String getWarningMessage(Player player) {
        String str = WarningMessage.replace("<NPC>", myNPC.getName()).replace("<PLAYER>", player.getName());
        return ChatColor.translateAlternateColorCodes('&', str);

    }

    public void initialize() {

        // plugin.getServer().broadcastMessage("NPC " + npc.getName() +
        // " INITIALIZING!");

        // check for illegal values

        if (butlerWeight <= 0)
            butlerWeight = 1.0;
        if (AttackRateSeconds > 30)
            AttackRateSeconds = 30.0;

        if (butlerHealth < 0)
            butlerHealth = 0;

        if (butlerRange < 1)
            butlerRange = 1;
        if (butlerRange > 200)
            butlerRange = 200;

        if (butlerWeight <= 0)
            butlerWeight = 1.0;

        if (RespawnDelaySeconds < -1)
            RespawnDelaySeconds = -1;

        if (Spawn == null) {
            Spawn = myNPC.getEntity().getLocation();
        }


        if (plugin.DenizenActive) {
            if (myNPC.hasTrait(net.aufdemrand.denizen.npc.traits.HealthTrait.class))
                myNPC.removeTrait(net.aufdemrand.denizen.npc.traits.HealthTrait.class);
        }

        //disable citizens respawning. Cause Butler doesnt always raise EntityDeath
        myNPC.data().set("respawn-delay", -1);

        setHealth(butlerHealth);

        _myDamamgers.clear();

        this.butlerStatus = Status.isLOOKING;
        faceForward();

        //healanim = new Packet18ArmAnimation(((CraftEntity) myNPC.getEntity()).getHandle(), 6);

        //	Packet derp = new net.minecraft.server.Packet15Place();

        if (guardTarget == null) {
            myNPC.teleport(Spawn, TeleportCause.PLUGIN); //it should be there... but maybe not if the position was saved elsewhere.
        }

        float pf = myNPC.getNavigator().getDefaultParameters().range();

        if (pf < butlerRange + 5) {
            pf = butlerRange + 5;
        }

        myNPC.data().set(NPC.DEFAULT_PROTECTED_METADATA, false);
        myNPC.data().set(NPC.TARGETABLE_METADATA, this.Targetable);


        myNPC.getNavigator().getDefaultParameters().range(pf);
        myNPC.getNavigator().getDefaultParameters().stationaryTicks(5 * 20);
        myNPC.getNavigator().getDefaultParameters().useNewPathfinder(false);
        //	myNPC.getNavigator().getDefaultParameters().stuckAction(new BodyguardTeleportStuckAction(this, this.plugin));

        // plugin.getServer().broadcastMessage("NPC GUARDING!");


        if (taskID == null) {
            taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new ButlerLogic(), 40 + this.myNPC.getId(), plugin.LogicTicks);
        }

        mountCreated = false;
    }

    private boolean mountCreated = false;

    public boolean isPyromancer() {
        return (myProjectile == Fireball.class || myProjectile == SmallFireball.class);
    }

    public boolean isPyromancer1() {
        return (!inciendary && myProjectile == SmallFireball.class);
    }


    public boolean isPyromancer2() {
        return (inciendary && myProjectile == SmallFireball.class);
    }

    public boolean isPyromancer3() {
        return (myProjectile == Fireball.class);
    }

    public boolean isStormcaller() {
        return (lightning);
    }

    public boolean isWarlock1() {
        return (myProjectile == org.bukkit.entity.EnderPearl.class);
    }

    public boolean isWitchDoctor() {
        return (myProjectile == org.bukkit.entity.ThrownPotion.class);
    }


    public void onDamage(EntityDamageByEntityEvent event) {

        event.setCancelled(true);
        if (butlerStatus == Status.isDYING) return;

        if (myNPC == null || !myNPC.isSpawned()) {
            // \\how did you get here?
            return;
        }

        if (guardTarget != null && guardEntity == null) return; //dont take damage when bodyguard target isnt around.

        if (System.currentTimeMillis() < okToTakedamage + 500) return;
        okToTakedamage = System.currentTimeMillis();

        event.getEntity().setLastDamageCause(event);

        NPC npc = myNPC;

        LivingEntity attacker = null;

        hittype hit = hittype.normal;

        double finaldamage = event.getDamage();

        // Find the attacker
        if (event.getDamager() instanceof Projectile) {
            if (((Projectile) event.getDamager()).getShooter() instanceof LivingEntity) {
                attacker = ((Projectile) event.getDamager()).getShooter();
            }
        } else if (event.getDamager() instanceof LivingEntity) {
            attacker = (LivingEntity) event.getDamager();
        }

        if (Invincible)
            return;


        if (plugin.IgnoreListInvincibility) {
            if (isIgnored(attacker)) return;
        }

        // can i kill it? lets go kill it.
        if (attacker != null) {
            if (this.Retaliate) {
                if (!(event.getDamager() instanceof Projectile) || (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(attacker) == null)) {
                    // only retaliate to players or non-projectlies. Prevents stray butler arrows from causing retaliation.

                    setTarget(attacker, true);

                }
            }
        }

        if (LuckyHits) {
            // Calulate crits
            double damagemodifer = event.getDamage();

            int luckeyhit = r.nextInt(100);

            if (luckeyhit < plugin.Crit3Chance) {
                damagemodifer = damagemodifer * 2.00;
                hit = hittype.disembowel;
            } else if (luckeyhit < plugin.Crit3Chance + plugin.Crit2Chance) {
                damagemodifer = damagemodifer * 1.75;
                hit = hittype.main;
            } else if (luckeyhit < plugin.Crit3Chance + plugin.Crit2Chance + plugin.Crit1Chance) {
                damagemodifer = damagemodifer * 1.50;
                hit = hittype.injure;
            } else if (luckeyhit < plugin.Crit3Chance + plugin.Crit2Chance + plugin.Crit1Chance + plugin.GlanceChance) {
                damagemodifer = damagemodifer * 0.50;
                hit = hittype.glance;
            } else if (luckeyhit < plugin.Crit3Chance + plugin.Crit2Chance + plugin.Crit1Chance + plugin.GlanceChance + plugin.MissChance) {
                damagemodifer = 0;
                hit = hittype.miss;
            }

            finaldamage = Math.round(damagemodifer);
        }

    }

    Random R = new Random();

    public void onEnvironmentDamae(EntityDamageEvent event) {

        if (butlerStatus == Status.isDYING) return;

        if (!myNPC.isSpawned() || Invincible) {
            return;
        }

        if (guardTarget != null && guardEntity == null) return; //dont take damage when bodyguard target isnt around.

        if (System.currentTimeMillis() < okToTakedamage + 500) return;
        okToTakedamage = System.currentTimeMillis();

        myNPC.getEntity().setLastDamageCause(event);

    }

    @EventHandler
    public void onRightClick(NPCRightClickEvent event) {
        // TODO: ??
    }

    final int all = 1;
    final int players = 2;
    final int npcs = 4;
    final int monsters = 8;
    final int events = 16;
    final int namedentities = 32;
    final int namedplayers = 64;
    final int namednpcs = 128;
    final int faction = 256;
    final int towny = 512;
    final int war = 1024;
    final int groups = 2048;
    final int owner = 4096;
    final int clans = 8192;
    final int townyenemies = 16384;
    final int factionenemies = 16384 * 2;
    private int targets = 0;
    private int ignores = 0;

    List<String> NationsEnemies = new ArrayList<String>();
    List<String> FactionEnemies = new ArrayList<String>();

    public void processTargets() {
        try {

            targets = 0;
            ignores = 0;
            _ignoreTargets.clear();
            _validTargets.clear();
            NationsEnemies.clear();
            FactionEnemies.clear();

            for (String t : validTargets) {
                if (t.contains("ENTITY:ALL")) targets |= all;
                else if (t.contains("ENTITY:MONSTER")) targets |= monsters;
                else if (t.contains("ENTITY:PLAYER")) targets |= players;
                else if (t.contains("ENTITY:NPC")) targets |= npcs;
                else {
                    _validTargets.add(t);
                    if (t.contains("NPC:")) targets |= namednpcs;
                    else if (plugin.perms != null && plugin.perms.isEnabled() && t.contains("GROUP:"))
                        targets |= groups;
                    else if (t.contains("EVENT:")) targets |= events;
                    else if (t.contains("PLAYER:")) targets |= namedplayers;
                    else if (t.contains("ENTITY:")) targets |= namedentities;
                    else if (Butler.FactionsActive && t.contains("FACTION:")) targets |= faction;
                    else if (Butler.FactionsActive && t.contains("FACTIONENEMIES:")) {
                        targets |= factionenemies;
                        FactionEnemies.add(t.split(":")[1]);
                    } else if (plugin.TownyActive && t.contains("TOWN:")) targets |= towny;
                    else if (plugin.TownyActive && t.contains("NATIONENEMIES:")) {
                        targets |= townyenemies;
                        NationsEnemies.add(t.split(":")[1]);
                    } else if (plugin.TownyActive && t.contains("NATION:")) targets |= towny;
                    else if (plugin.WarActive && t.contains("TEAM:")) targets |= war;
                    else if (plugin.ClansActive && t.contains("CLAN:")) targets |= clans;
                }
            }
            for (String t : ignoreTargets) {
                if (t.contains("ENTITY:ALL")) ignores |= all;
                else if (t.contains("ENTITY:MONSTER")) ignores |= monsters;
                else if (t.contains("ENTITY:PLAYER")) ignores |= players;
                else if (t.contains("ENTITY:NPC")) ignores |= npcs;
                else if (t.contains("ENTITY:OWNER")) ignores |= owner;
                else {
                    _ignoreTargets.add(t);
                    if (plugin.perms != null && plugin.perms.isEnabled() && t.contains("GROUP:")) ignores |= groups;
                    else if (t.contains("NPC:")) ignores |= namednpcs;
                    else if (t.contains("PLAYER:")) ignores |= namedplayers;
                    else if (t.contains("ENTITY:")) ignores |= namedentities;
                    else if (Butler.FactionsActive && t.contains("FACTION:")) ignores |= faction;
                    else if (plugin.TownyActive && t.contains("TOWN:")) ignores |= towny;
                    else if (plugin.TownyActive && t.contains("NATION:")) ignores |= towny;
                    else if (plugin.WarActive && t.contains("TEAM:")) ignores |= war;
                    else if (plugin.ClansActive && t.contains("CLAN:")) ignores |= clans;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private class ButlerLogic implements Runnable {

        @Override
        public void run() {
            // plugin.getServer().broadcastMessage("tick " + (myNPC ==null) +
            if (myNPC.getEntity() == null) butlerStatus = Status.isDEAD; // incase it dies in a way im not handling.....

            if (UpdateWeapon()) {
                //ranged
                if (meleeTarget != null) {
                    plugin.debug(myNPC.getName() + " Switched to ranged");
                    LivingEntity derp = meleeTarget;
                    boolean ret = butlerStatus == Status.isRETALIATING;
                    setTarget(null, false);
                    setTarget(derp, ret);
                }
            } else {
                //melee
                if (projectileTarget != null) {
                    plugin.debug(myNPC.getName() + " Switched to melee");
                    boolean ret = butlerStatus == Status.isRETALIATING;
                    LivingEntity derp = projectileTarget;
                    setTarget(null, false);
                    setTarget(derp, ret);
                }
            }

            if (butlerStatus != Status.isDEAD && HealRate > 0) {
                if (System.currentTimeMillis() > oktoheal) {
                    if (getHealth() < butlerHealth && butlerStatus != Status.isDEAD && butlerStatus != Status.isDYING) {
                        double heal = 1;
                        if (HealRate < 0.5) heal = (0.5 / HealRate);


                        setHealth(getHealth() + heal);


                        //if (healanim != null)
                            //net.citizensnpcs.util.NMS.sendPacketsNearby(myNPC.getEntity().getLocation(), healanim);
                        // TODO: Repair this


                        if (getHealth() >= butlerHealth) _myDamamgers.clear(); //healed to full, forget attackers

                    }
                    oktoheal = (long) (System.currentTimeMillis() + HealRate * 1000);
                }

            }

            if (butlerStatus == Status.isDEAD && System.currentTimeMillis() > isRespawnable && RespawnDelaySeconds > 0 & Spawn.getWorld().isChunkLoaded(Spawn.getBlockX() >> 4, Spawn.getBlockZ() >> 4)) {
                // Respawn

                plugin.debug("respawning" + myNPC.getName());
                if (guardEntity == null) {
                    myNPC.spawn(Spawn.clone());
                    //	myNPC.teleport(Spawn,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                } else {
                    myNPC.spawn(guardEntity.getLocation().add(2, 0, 2));
                    //	myNPC.teleport(guardEntity.getLocation().add(2, 0, 2),org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
                return;
            } else if ((butlerStatus == Status.isHOSTILE || butlerStatus == Status.isRETALIATING) && myNPC.isSpawned()) {

                if (!isMyChunkLoaded()) {
                    setTarget(null, false);
                    return;
                }

                if (targets > 0 && butlerStatus == Status.isHOSTILE && System.currentTimeMillis() > oktoreasses) {
                    LivingEntity target = findTarget(butlerRange);
                    setTarget(target, false);
                    oktoreasses = System.currentTimeMillis() + 3000;
                }
            } else if (butlerStatus == Status.isLOOKING && myNPC.isSpawned()) {

                if (myNPC.getEntity().isInsideVehicle())
                    faceAlignWithVehicle(); //sync the rider with the vehicle.


                if (guardEntity instanceof Player) {
                    if (!((Player) guardEntity).isOnline()) {
                        guardEntity = null;
                    }
                }

                if (guardTarget != null && guardEntity == null) {
                    // daddy? where are u?
                    setGuardTarget(guardTarget);
                }

                if (guardEntity != null) {

                    Location npcLoc = myNPC.getEntity().getLocation();
                }

                LivingEntity target = null;

                if (targets > 0) {
                    target = findTarget(butlerRange);
                }

                if (target != null) {
                    oktoreasses = System.currentTimeMillis() + 3000;
                    setTarget(target, false);
                }

            }

        }
    }


    private boolean isMyChunkLoaded() {
        if (myNPC.getEntity() == null) return false;
        Location npcLoc = myNPC.getEntity().getLocation();
        return npcLoc.getWorld().isChunkLoaded(npcLoc.getBlockX() >> 4, npcLoc.getBlockZ() >> 4);
    }

    public boolean setGuardTarget(String name) {

        if (myNPC == null)
            return false;

        if (name == null) {
            guardEntity = null;
            guardTarget = null;
            setTarget(null, false);// clear active hostile target
            return true;
        }

        List<Entity> EntitiesWithinRange = myNPC.getEntity().getNearbyEntities(butlerRange, butlerRange, butlerRange);

        for (Entity aTarget : EntitiesWithinRange) {

            if (aTarget instanceof Player) {
                //chesk for players
                if (((Player) aTarget).getName().equals(name)) {
                    guardEntity = (LivingEntity) aTarget;
                    guardTarget = ((Player) aTarget).getName();
                    setTarget(null, false); // clear active hostile target
                    return true;
                }
            } else if (aTarget instanceof LivingEntity) {
                //check for named mobs.
                String ename = ((LivingEntity) aTarget).getCustomName();
                if (ename != null && ename.equals(name)) {
                    guardEntity = (LivingEntity) aTarget;
                    guardTarget = ename;
                    setTarget(null, false); // clear active hostile target
                    return true;
                }
            }

        }
        return false;
    }

    public void setHealth(double health) {
        if (myNPC == null) return;
        if (myNPC.getEntity() == null) return;
        if (((CraftLivingEntity) myNPC.getEntity()).getMaxHealth() != butlerHealth)
            ((LivingEntity) myNPC.getEntity()).setMaxHealth(butlerHealth);
        if (health > butlerHealth) health = butlerHealth;

        ((LivingEntity) myNPC.getEntity()).setHealth(health);
    }


    public boolean UpdateWeapon() {
        int weapon = 0;

        ItemStack is = null;

        if (myNPC.getEntity() instanceof HumanEntity) {
            is = ((HumanEntity) myNPC.getEntity()).getInventory().getItemInHand();
            weapon = is.getTypeId();
            if (weapon != plugin.witchdoctor) is.setDurability((short) 0);
        }

        lightning = false;
        lightninglevel = 0;
        inciendary = false;
        potionEffects = plugin.WeaponEffects.get(weapon);

        myProjectile = null;

        return true; //ranged
    }

    public void setTarget(LivingEntity theEntity, boolean isretaliation) {

        if (myNPC.getEntity() == null) return;

        if (theEntity == myNPC.getEntity()) return; //I don't care how you got here. No. just No.

        if (guardTarget != null && guardEntity == null)
            theEntity = null; //dont go aggro when bodyguard target isnt around.

        if (theEntity == null) {
            plugin.debug(myNPC.getName() + "- Set Target Null");
            // this gets called while npc is dead, reset things.
            butlerStatus = Status.isLOOKING;
            projectileTarget = null;
            meleeTarget = null;
            _projTargetLostLoc = null;
        }

        if (myNPC == null)
            return;
        if (!myNPC.isSpawned())
            return;

        if (theEntity == guardEntity)
            return; // dont attack my dude.

        if (isretaliation) butlerStatus = Status.isRETALIATING;
        else butlerStatus = Status.isHOSTILE;


        if (!getNavigator().isNavigating()) faceEntity(myNPC.getEntity(), theEntity);
    }

    protected net.citizensnpcs.api.ai.Navigator getNavigator() {
        NPC npc = getMount();
        if (npc == null || !npc.isSpawned()) npc = myNPC;
        return npc.getNavigator();
    }

    protected net.citizensnpcs.api.ai.GoalController getGoalController() {
        NPC npc = getMount();
        if (npc == null || !npc.isSpawned()) npc = myNPC;
        return npc.getDefaultGoalController();
    }

    public void dismount() {
        //get off and despawn the horse.
        if (myNPC.isSpawned()) {
            if (myNPC.getEntity().isInsideVehicle()) {
                NPC n = getMount();
                if (n != null) {
                    myNPC.getEntity().getVehicle().setPassenger(null);
                    n.despawn(net.citizensnpcs.api.event.DespawnReason.PLUGIN);
                }
            }
        }
    }

    public boolean hasLOS(Entity other) {
        if (!myNPC.isSpawned()) return false;
        return ((LivingEntity) myNPC.getEntity()).hasLineOfSight(other);
    }


    protected NPC getMount() {
        if (this.isMounted() && net.citizensnpcs.api.CitizensAPI.hasImplementation()) {

            return net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getById(this.MountID);

        }
        return null;
    }


}
