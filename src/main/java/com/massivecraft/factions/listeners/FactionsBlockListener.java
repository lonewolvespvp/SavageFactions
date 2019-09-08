package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Worldguard;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.XMaterial;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;


public class FactionsBlockListener implements Listener {

    public static HashMap<String, Location> bannerLocations = new HashMap<>();
    private HashMap<String, Boolean> bannerCooldownMap = new HashMap<>();

    /**
     * Determine if a player has access to do a
     * action in a specific area.
     *
     * @param player Player in question
     * @param location Location action occurred
     * @param action Action Type
     * @param justCheck Check access, or take action
     * @return
     */
    public static boolean playerCanBuildDestroyBlock(Player player, Location location, String action, boolean justCheck) {

        if (Conf.playersWhoBypassAllProtection.contains(player.getName())) return true;

        FPlayer me = FPlayers.getInstance().getById(player.getUniqueId().toString());
        if (me.isAdminBypassing()) return true;

        FLocation loc = new FLocation(location);
        Faction otherFaction = Board.getInstance().getFactionAt(loc);
        Faction myFaction = me.getFaction();

        if (otherFaction.isWilderness()) {
            if (Conf.worldGuardBuildPriority && Worldguard.getInstance().playerCanBuild(player, location)) return true;
            if (!Conf.wildernessDenyBuild || Conf.worldsNoWildernessProtection.contains(location.getWorld().getName())) return true;
            if (!justCheck) me.msg(TL.ACTION_DENIED_WILDERNESS, action);
            return false;
        } else if (otherFaction.isSafeZone()) {
            if (Conf.worldGuardBuildPriority && Worldguard.getInstance().playerCanBuild(player, location)) return true;
            if (!Conf.safeZoneDenyBuild || Permission.MANAGE_SAFE_ZONE.has(player)) return true;
            if (!justCheck) me.msg(TL.ACTION_DENIED_SAFEZONE, action);
            return false;
        } else if (otherFaction.isWarZone()) {
            if (Conf.worldGuardBuildPriority && Worldguard.getInstance().playerCanBuild(player, location)) return true;
            if (!Conf.warZoneDenyBuild || Permission.MANAGE_WAR_ZONE.has(player)) return true;
            if (!justCheck) me.msg(TL.ACTION_DENIED_WARZONE, action);
            return false;
        } else if (!otherFaction.getId().equals(myFaction.getId())) { // If the faction target is not my own
            if (SavageFactions.plugin.getConfig().getBoolean("hcf.raidable", false) && otherFaction.getLandRounded() > otherFaction.getPowerRounded()) return true;
            // Get faction pain build access relation to me
            boolean pain = !justCheck && otherFaction.getAccess(me, PermissableAction.PAIN_BUILD) == Access.ALLOW;
            return CheckActionState(otherFaction, loc, me, PermissableAction.fromString(action), pain);
        } else if (otherFaction.getId().equals(myFaction.getId())) {
            boolean pain = !justCheck && myFaction.getAccess(me, PermissableAction.PAIN_BUILD) == Access.ALLOW;
            return CheckActionState(myFaction, loc, me, PermissableAction.fromString(action), pain);
        }

        // Something failed prevent build
        return false;
    }

    private static boolean CheckPlayerAccess(Player player, FPlayer me, FLocation loc, Faction myFaction, Access access, PermissableAction action, boolean shouldHurt) {
        boolean landOwned = (myFaction.doesLocationHaveOwnersSet(loc) && !myFaction.getOwnerList(loc).isEmpty());
        if ((landOwned && myFaction.getOwnerListString(loc).contains(player.getName())) || (me.getRole() == Role.LEADER && me.getFactionId().equals(myFaction.getId())))
            return true;
        else if (landOwned && !myFaction.getOwnerListString(loc).contains(player.getName())) {
            me.msg(TL.ACTIONS_OWNEDTERRITORYDENY.toString().replace("{owners}", myFaction.getOwnerListString(loc)));
            if (shouldHurt) {
                player.damage(Conf.actionDeniedPainAmount);
                me.msg(TL.ACTIONS_NOPERMISSIONPAIN.toString().replace("{action}", action.toString()).replace("{faction}", Board.getInstance().getFactionAt(loc).getTag(myFaction)));
            }
            return false;
        } else if (!landOwned && access == Access.DENY) { // If land is not owned but access is set to DENY anyway
            if (shouldHurt) {
                player.damage(Conf.actionDeniedPainAmount);
                me.msg(TL.ACTIONS_NOPERMISSIONPAIN.toString().replace("{action}", action.toString()).replace("{faction}", Board.getInstance().getFactionAt(loc).getTag(myFaction)));
            }
            me.msg(TL.ACTIONS_NOPERMISSION.toString().replace("{faction}", myFaction.getTag(me.getFaction())).replace("{action}", action.toString()));
            return false;
        } else if (access == Access.ALLOW) return true;
        me.msg(TL.ACTIONS_NOPERMISSION.toString().replace("{faction}", myFaction.getTag(me.getFaction())).replace("{action}", action.toString()));
        return false;
    }

    private static boolean CheckActionState(Faction target, FLocation location, FPlayer me, PermissableAction action, boolean pain) {
        if (Conf.ownedAreasEnabled && target.doesLocationHaveOwnersSet(location) && !target.playerHasOwnershipRights(me, location)) {
            // If pain should be applied
            if (pain && Conf.ownedAreaPainBuild)
                me.msg(TL.ACTIONS_OWNEDTERRITORYPAINDENY.toString().replace("{action}", action.toString()).replace("{faction}", target.getOwnerListString(location)));
            if (Conf.ownedAreaDenyBuild && pain) return false;
            else if (Conf.ownedAreaDenyBuild) {
                me.msg(TL.ACTIONS_NOPERMISSION.toString().replace("{faction}", target.getTag(me.getFaction())).replace("{action}", action.toString()));
                return false;
            }
        }
        return CheckPlayerAccess(me.getPlayer(), me, location, target, target.getAccess(me, action), action, pain);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (!event.canBuild()) return;
        if (event.getBlockPlaced().getType() == Material.FIRE) return;

        if (!playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "build", false)) {
            event.setCancelled(true);
            return;
        }

        if (event.getBlock().getType().equals(XMaterial.SPAWNER.parseMaterial())) {
            if (!SavageFactions.plugin.spawnersPlacing) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(SavageFactions.plugin.color(TL.COMMAND_SPAWNERTOGGLE_PLACE_DENIED.toString()));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!Conf.handleExploitLiquidFlow) return;
        if (event.getBlock().isLiquid()) {
            if (event.getToBlock().isEmpty()) {
                Faction from = Board.getInstance().getFactionAt(new FLocation(event.getBlock()));
                Faction to = Board.getInstance().getFactionAt(new FLocation(event.getToBlock()));
                if (from == to) return;
                // from faction != to faction
                if (to.isNormal()) {
                    if (from.isNormal() && from.getRelationTo(to).isAlly()) {
                        return;
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getInstaBreak() && !playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "destroy", false))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (!Conf.pistonProtectionThroughDenyBuild) return;
        Faction pistonFaction = Board.getInstance().getFactionAt(new FLocation(event.getBlock()));

        // target end-of-the-line empty (air) block which is being pushed into, including if piston itself would extend into air
        Block targetBlock = event.getBlock().getRelative(event.getDirection(), event.getLength() + 1);

        // if potentially pushing into air/water/lava in another territory, we need to check it out
        if ((targetBlock.isEmpty() || targetBlock.isLiquid()) && !canPistonMoveBlock(pistonFaction, targetBlock.getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onVaultPlace(BlockPlaceEvent e) {
        if (e.getItemInHand().getType() == Material.CHEST) {

            ItemStack vault = SavageFactions.plugin.createItem(
                    Material.CHEST,
                    1,
                    (short) 0,
                    SavageFactions.plugin.color(SavageFactions.plugin.getConfig().getString("fvault.Item.Name")),
                    SavageFactions.plugin.colorList(SavageFactions.plugin.getConfig().getStringList("fvault.Item.Lore")));

            if (e.getItemInHand().isSimilar(vault)) {
                FPlayer fme = FPlayers.getInstance().getByPlayer(e.getPlayer());
                if (fme.getFaction().getVault() != null) {
                    fme.msg(TL.COMMAND_GETVAULT_ALREADYSET);
                    e.setCancelled(true);
                    return;
                }
                FLocation flocation = new FLocation(e.getBlockPlaced().getLocation());
                if (Board.getInstance().getFactionAt(flocation) != fme.getFaction()) {
                    fme.msg(TL.COMMAND_GETVAULT_INVALIDLOCATION);
                    e.setCancelled(true);
                    return;
                }
                Block start = e.getBlockPlaced();
                int radius = 1;
                for (double x = start.getLocation().getX() - radius; x <= start.getLocation().getX() + radius; x++) {
                    for (double y = start.getLocation().getY() - radius; y <= start.getLocation().getY() + radius; y++) {
                        for (double z = start.getLocation().getZ() - radius; z <= start.getLocation().getZ() + radius; z++) {
                            Location blockLoc = new Location(e.getPlayer().getWorld(), x, y, z);
                            if (blockLoc.getX() == start.getLocation().getX() && blockLoc.getY() == start.getLocation().getY() && blockLoc.getZ() == start.getLocation().getZ()) {
                                continue;
                            }

                            Material blockMaterial = blockLoc.getBlock().getType();

                            if (blockMaterial == Material.CHEST || (SavageFactions.plugin.getConfig().getBoolean("fvault.No-Hoppers-near-vault") && blockMaterial == Material.HOPPER)) {
                                e.setCancelled(true);
                                fme.msg(TL.COMMAND_GETVAULT_CHESTNEAR);
                                return;
                            }
                        }
                    }
                }

                fme.msg(TL.COMMAND_GETVAULT_SUCCESS);
                fme.getFaction().setVault(e.getBlockPlaced().getLocation());

            }
        }
    }

    @EventHandler
    public void onHopperPlace(BlockPlaceEvent e) {
        if (e.getItemInHand().getType() != Material.HOPPER && !SavageFactions.plugin.getConfig().getBoolean("fvault.No-Hoppers-near-vault")) return;
        Faction factionAt = Board.getInstance().getFactionAt(new FLocation(e.getBlockPlaced().getLocation()));
        if (factionAt.isWilderness() || factionAt.getVault() == null) return;

        FPlayer fme = FPlayers.getInstance().getByPlayer(e.getPlayer());

        Block start = e.getBlockPlaced();
        int radius = 1;
        for (double x = start.getLocation().getX() - radius; x <= start.getLocation().getX() + radius; x++) {
            for (double y = start.getLocation().getY() - radius; y <= start.getLocation().getY() + radius; y++) {
                for (double z = start.getLocation().getZ() - radius; z <= start.getLocation().getZ() + radius; z++) {
                    Location blockLoc = new Location(e.getPlayer().getWorld(), x, y, z);
                    if (blockLoc.getX() == start.getLocation().getX() && blockLoc.getY() == start.getLocation().getY() && blockLoc.getZ() == start.getLocation().getZ()) {
                        continue;
                    }

                    if (blockLoc.getBlock().getType() == Material.CHEST) {
                        if (factionAt.getVault().equals(blockLoc)) {
                            e.setCancelled(true);
                            fme.msg(TL.COMMAND_VAULT_NO_HOPPER);
                            return;
                        }
                    }
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        // if not a sticky piston, retraction should be fine
        if (!event.isSticky() || !Conf.pistonProtectionThroughDenyBuild) {
            return;
        }

        Location targetLoc = event.getRetractLocation();
        Faction otherFaction = Board.getInstance().getFactionAt(new FLocation(targetLoc));

        // Check if the piston is moving in a faction's territory. This disables pistons entirely in faction territory.
        if (otherFaction.isNormal() && SavageFactions.plugin.getConfig().getBoolean("disable-pistons-in-territory", false)) {
            event.setCancelled(true);
            return;
        }

        // if potentially retracted block is just air/water/lava, no worries
        if (targetLoc.getBlock().isEmpty() || targetLoc.getBlock().isLiquid()) return;
        Faction pistonFaction = Board.getInstance().getFactionAt(new FLocation(event.getBlock()));
        if (!canPistonMoveBlock(pistonFaction, targetLoc)) event.setCancelled(true);

    }


    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFrostWalker(EntityBlockFormEvent event) {
        if (event.getEntity() == null || event.getEntity().getType() != EntityType.PLAYER || event.getBlock() == null)
            return;

        Player player = (Player) event.getEntity();
        Location location = event.getBlock().getLocation();

        // only notify every 10 seconds
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        boolean justCheck = fPlayer.getLastFrostwalkerMessage() + 10000 > System.currentTimeMillis();
        if (!justCheck) fPlayer.setLastFrostwalkerMessage();

        // Check if they have build permissions here. If not, block this from happening.
        if (!playerCanBuildDestroyBlock(player, location, "frostwalk", justCheck)) event.setCancelled(true);
    }

    private boolean canPistonMoveBlock(Faction pistonFaction, Location target) {
        Faction otherFaction = Board.getInstance().getFactionAt(new FLocation(target));

        if (pistonFaction == otherFaction) return true;

        if (otherFaction.isWilderness()) return !Conf.wildernessDenyBuild || Conf.worldsNoWildernessProtection.contains(target.getWorld().getName());
        else if (otherFaction.isSafeZone()) return !Conf.safeZoneDenyBuild;
        else if (otherFaction.isWarZone()) return !Conf.warZoneDenyBuild;

        Relation rel = pistonFaction.getRelationTo(otherFaction);
        return !rel.confDenyBuild(otherFaction.hasPlayersOnline());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!playerCanBuildDestroyBlock(event.getPlayer(), event.getBlock().getLocation(), "destroy", false)) {
            event.setCancelled(true);
            return;
        }
        FPlayer fme = FPlayers.getInstance().getByPlayer(event.getPlayer());
        if (fme == null || !fme.hasFaction()) {
            return;
        }
        if (event.getBlock().getType() == XMaterial.SPAWNER.parseMaterial()) {
            if (!fme.isAdminBypassing()) {
                Access access = fme.getFaction().getAccess(fme, PermissableAction.SPAWNER);
                if (access != Access.ALLOW && fme.getRole() != Role.LEADER) {
                    fme.msg(TL.GENERIC_FPERM_NOPERMISSION, "mine spawners");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onFarmLandDamage(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!playerCanBuildDestroyBlock(player, event.getBlock().getLocation(), PermissableAction.DESTROY.name(), true)) {
                FPlayer me = FPlayers.getInstance().getByPlayer(player);
                Faction otherFaction = Board.getInstance().getFactionAt(new FLocation(event.getBlock().getLocation()));
                me.msg(TL.ACTION_DENIED_OTHER, otherFaction.getTag(), "trample crops");
                event.setCancelled(true);
            }
        }
    }
}
