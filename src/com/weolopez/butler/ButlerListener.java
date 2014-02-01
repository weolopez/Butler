package com.weolopez.butler;

import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;


import net.citizensnpcs.api.CitizensAPI;


import net.citizensnpcs.api.npc.NPC;


public class ButlerListener implements Listener {

    public Butler plugin;

    public ButlerListener(Butler butler) {
        plugin = butler;
    }


    @EventHandler
    public void kill(org.bukkit.event.entity.EntityDeathEvent event) {

        if (event.getEntity() == null) return;

        //dont mess with player death.
        if (event.getEntity() instanceof Player && !event.getEntity().hasMetadata("NPC")) return;


        Entity killer = event.getEntity().getKiller();
        if (killer == null) {
            //might have been a projectile.
            EntityDamageEvent ev = event.getEntity().getLastDamageCause();
            if (ev != null && ev instanceof EntityDamageByEntityEvent) {
                killer = ((EntityDamageByEntityEvent) ev).getDamager();
                if (killer instanceof Projectile) killer = ((Projectile) killer).getShooter();
            }
        }

        ButlerInstance butler = plugin.getButler(killer);


        if (butler != null && !butler.KillsDropInventory) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void despawn(net.citizensnpcs.api.event.NPCDespawnEvent event) {
        ButlerInstance butler = plugin.getButler(event.getNPC());
        //dont despawn active bodyguaards on chunk unload
        if (butler != null && event.getReason() == net.citizensnpcs.api.event.DespawnReason.CHUNK_UNLOAD && butler.guardEntity != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void entteleportevent(org.bukkit.event.entity.EntityTeleportEvent event) {
        ButlerInstance butler = plugin.getButler(event.getEntity());
        if (butler != null && butler.epcount != 0 && butler.isWarlock1()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void entteleportevent(org.bukkit.event.player.PlayerTeleportEvent event) {
        ButlerInstance butler = plugin.getButler(event.getPlayer());
        if (butler != null) {
            if (butler.epcount != 0 && butler.isWarlock1() && event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void projectilehit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.EnderPearl) {
            ButlerInstance butler = plugin.getButler(event.getEntity().getShooter());
            if (butler != null) {
                butler.epcount--;
                if (butler.epcount < 0) butler.epcount = 0;
                event.getEntity().getLocation().getWorld().playEffect(event.getEntity().getLocation(), org.bukkit.Effect.ENDER_SIGNAL, 1, 100);
                //ender pearl from a butler
            }
        } else if (event.getEntity() instanceof org.bukkit.entity.SmallFireball) {
            final org.bukkit.block.Block block = event.getEntity().getLocation().getBlock();
            ButlerInstance butler = plugin.getButler(event.getEntity().getShooter());

            if (butler != null && butler.isPyromancer1()) {

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run() {

                        for (BlockFace face : org.bukkit.block.BlockFace.values()) {
                            if (block.getRelative(face).getType() == org.bukkit.Material.FIRE)
                                block.getRelative(face).setType(org.bukkit.Material.AIR);
                        }

                        if (block.getType() == org.bukkit.Material.FIRE) block.setType(org.bukkit.Material.AIR);

                    }
                }
                );
            }
        }
    }

//	@EventHandler(ignoreCancelled = true, priority =org.bukkit.event.EventPriority.HIGH)
//	public void tarsdfget(EntityTargetEvent event) {
//		ButlerInstance inst = plugin.getButler(event.getTarget());
//		if(inst!=null){
//			event.setCancelled(false); //inst.myNPC.data().get(NPC.DEFAULT_PROTECTED_METADATA, false));
//		}
//	}

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void EnvDamage(EntityDamageEvent event) {

        if (event instanceof EntityDamageByEntityEvent) return;

        ButlerInstance inst = plugin.getButler(event.getEntity());
        if (inst == null) return;

        event.setCancelled(true);

        DamageCause cause = event.getCause();
        //	plugin.getLogger().log(Level.INFO, "Damage " + cause.toString() + " " + event.getDamage());

        switch (cause) {
            case CONTACT:
            case DROWNING:
            case LAVA:
            case SUFFOCATION:
            case CUSTOM:
            case BLOCK_EXPLOSION:
            case VOID:
            case SUICIDE:
            case MAGIC:
                inst.onEnvironmentDamae(event);
                break;
            case LIGHTNING:
                if (!inst.isStormcaller()) inst.onEnvironmentDamae(event);
                break;
            case FIRE:
            case FIRE_TICK:
                if (!inst.isPyromancer() && !inst.isStormcaller()) inst.onEnvironmentDamae(event);
                break;
            case POISON:
                if (!inst.isWitchDoctor()) inst.onEnvironmentDamae(event);
                break;
            case FALL:
                break;
            default:
                break;
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST) //highest for worldguard...
    public void onDamage(org.bukkit.event.entity.EntityDamageByEntityEvent event) {

        Entity entfrom = event.getDamager();
        Entity entto = event.getEntity();

        if (entfrom instanceof org.bukkit.entity.Projectile) {
            entfrom = ((org.bukkit.entity.Projectile) entfrom).getShooter();
        }

        ButlerInstance from = plugin.getButler(entfrom);
        ButlerInstance to = plugin.getButler(entto);

        //process this event on each butler to check for respondable events.
        if (!event.isCancelled() && entfrom != entto && event.getDamage() > 0) {
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                ButlerInstance inst = plugin.getButler(npc);

                if (inst == null || !npc.isSpawned() || npc.getEntity().getWorld() != entto.getWorld())
                    continue; //not a butler, or not this world, or dead.

                if (inst.guardEntity == entto) {
                    if (inst.Retaliate && entfrom instanceof LivingEntity) inst.setTarget((LivingEntity) entfrom, true);
                }

                //are u attacking mai horse?
                if (inst.getMount() != null && inst.getMount().getEntity() == entto) {
                    if (entfrom == inst.guardEntity) event.setCancelled(true);
                    else if (inst.Retaliate && entfrom instanceof LivingEntity)
                        inst.setTarget((LivingEntity) entfrom, true);

                }

                if (inst.hasTargetType(16) && inst.butlerStatus == com.weolopez.butler.ButlerInstance.Status.isLOOKING && entfrom instanceof Player && !CitizensAPI.getNPCRegistry().isNPC(entfrom)) {
                    //pv-something event.
                    if (npc.getEntity().getLocation().distance(entto.getLocation()) <= inst.butlerRange || npc.getEntity().getLocation().distance(entfrom.getLocation()) <= inst.butlerRange) {
                        // in range
                        if (inst.NightVision >= entfrom.getLocation().getBlock().getLightLevel() || inst.NightVision >= entto.getLocation().getBlock().getLightLevel()) {
                            //can see
                            if (inst.hasLOS(entfrom) || inst.hasLOS(entto)) {
                                //have los
                                if ((!(entto instanceof Player) && inst.containsTarget("event:pve")) ||
                                        (entto instanceof Player && !CitizensAPI.getNPCRegistry().isNPC(entto) && inst.containsTarget("event:pvp")) ||
                                        (CitizensAPI.getNPCRegistry().isNPC(entto) && inst.containsTarget("event:pvnpc")) ||
                                        (to != null && inst.containsTarget("event:pvbutler"))) {
                                    //Valid event, attack
                                    if (!inst.isIgnored((LivingEntity) entfrom)) {
                                        inst.setTarget((LivingEntity) entfrom, true); //attack the aggressor
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        plugin.debug("start: from: " + entfrom + " to " + entto + " cancelled " + event.isCancelled() + " damage " + event.getDamage() + " cause " + event.getCause());

        if (from != null) {

            //projectiles go thru ignore targets.
            if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
                if (entto instanceof LivingEntity && from.isIgnored((LivingEntity) entto)) {
                    event.setCancelled(true);
                    event.getDamager().remove();
                    Projectile newProjectile = (Projectile) (entfrom.getWorld().spawnEntity(event.getDamager().getLocation().add(event.getDamager().getVelocity()), event.getDamager().getType()));
                    newProjectile.setVelocity(event.getDamager().getVelocity());
                    newProjectile.setShooter((LivingEntity) entfrom);
                    newProjectile.setTicksLived(event.getDamager().getTicksLived());
                    return;
                }
            }

            //from a butler
            event.setDamage((double) from.getStrength());

            //uncancel if not bodyguard.
            if (from.guardTarget == null || !plugin.BodyguardsObeyProtection) event.setCancelled(false);

            //cancel if invulnerable non-butler npc
            if (to == null) {
                NPC n = CitizensAPI.getNPCRegistry().getNPC(entto);
                if (n != null) {
                    boolean derp = (Boolean) n.data().get(NPC.DEFAULT_PROTECTED_METADATA, true);
                    event.setCancelled(derp);
                }
            }

            //dont hurt guard target.
            if (entto == from.guardEntity) event.setCancelled(true);

            //stop hittin yourself.
            if (entfrom == entto) event.setCancelled(true);

            //apply potion effects
            if (from.potionEffects != null && !event.isCancelled()) {
                ((LivingEntity) entto).addPotionEffects(from.potionEffects);
            }

            if (from.isWarlock1()) {
                if (!event.isCancelled()) {
                    if (to == null)
                        event.setCancelled(true); //warlock 1 should not do direct damamge, except to other sentries which take no fall damage.

                    double h = from.getStrength() + 3;
                    double v = 7.7 * Math.sqrt(h) + .2;
                    if (h <= 3) v -= 2;
                    if (v > 150) v = 150;

                    entto.setVelocity(new Vector(0, v / 20, 0));

                }

            }

        }

        if (to != null) {
            //to a butler

            //stop hittin yourself.
            if (entfrom == entto) return;

            //innate protections
            if (event.getCause() == DamageCause.LIGHTNING && to.isStormcaller()) return;
            if ((event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) && (to.isPyromancer() || to.isStormcaller()))
                return;

            //only bodyguards obey pvp-protection
            if (to.guardTarget == null) event.setCancelled(false);

            //dont take damamge from guard entity.
            if (entfrom == to.guardEntity) event.setCancelled(true);

            if (entfrom != null) {
                NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(entfrom);
                if (npc != null && npc.hasTrait(ButlerTrait.class) && to.guardEntity != null) {
                    if (npc.getTrait(ButlerTrait.class).getInstance().guardEntity == to.guardEntity) { //dont take damage from co-guards.
                        event.setCancelled(true);
                    }
                }
            }

            //process event
            if (!event.isCancelled()) to.onDamage(event);

        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(net.citizensnpcs.api.event.NPCDeathEvent event) {
        final NPC hnpc = event.getNPC();
        //if the mount dies carry aggression over.
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            final ButlerInstance inst = plugin.getButler(npc);
            if (inst == null || !npc.isSpawned() || !inst.isMounted()) continue; //not a butler, dead, or not mounted
            if (hnpc.getId() == inst.MountID) {
                ///nooooo butterstuff!

                Entity killer = ((LivingEntity) hnpc.getEntity()).getKiller();
                if (killer == null) {
                    //might have been a projectile.
                    EntityDamageEvent ev = hnpc.getEntity().getLastDamageCause();
                    if (ev != null && ev instanceof EntityDamageByEntityEvent) {
                        killer = ((EntityDamageByEntityEvent) ev).getDamager();
                        if (killer instanceof Projectile) killer = ((Projectile) killer).getShooter();
                    }
                }

                final LivingEntity perp = killer instanceof LivingEntity ? (LivingEntity) killer : null;


                if (perp == null) return;
                if (inst.isIgnored(perp)) return;

                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    //delay so the mount is gone.
                    public void run() {
                        inst.setTarget(perp, true);
                    }
                }, 2);

                return;
            }
        }
    }


    @EventHandler
    public void onNPCRightClick(net.citizensnpcs.api.event.NPCRightClickEvent event) {
        ButlerInstance inst = plugin.getButler(event.getNPC());
        if (inst == null) return;

        if (inst.myNPC.getEntity() instanceof org.bukkit.entity.Horse) {
            if (inst.guardEntity != event.getClicker()) {
                event.setCancelled(true);
            }
        }
    }

}

