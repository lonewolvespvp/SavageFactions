package com.massivecraft.factions.cmd;


import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.WarmUpUtil;
import com.massivecraft.factions.zcore.ffly.FlyParticle;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class CmdFly extends FCommand {


    public static ConcurrentHashMap<String, Boolean> flyMap = new ConcurrentHashMap<String, Boolean>();
    public static int id = -1;
    public static int flyid = -1;

    public CmdFly() {
        super();
        this.aliases.add("fly");
        this.optionalArgs.put("on/off", "flip");

        this.requirements = new CommandRequirements.Builder(Permission.FLY)
                .playerOnly()
                .memberOnly()
                .build();
    }

    public static void startParticles() {

        id = Bukkit.getScheduler().scheduleSyncRepeatingTask(SavageFactions.plugin, () -> {
            for (String name : flyMap.keySet()) {
                Player player = Bukkit.getPlayer(name);
                if (player == null) continue;
                if (!player.isFlying()) continue;
                if (!SavageFactions.plugin.mc17) {
                    if (player.getGameMode() == GameMode.SPECTATOR) continue;
                }

                FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
                if (fplayer.isVanished()) continue;

                FlyParticle selectedParticle = fplayer.getSelectedParticle();
                if (selectedParticle == null) {
                    // This is so we can gracefully accept people flying through autoenable.
                    selectedParticle = FlyParticle.WHITE_CLOUD;
                }
                selectedParticle.getData().display(player.getLocation().add(0, -0.35, 0));

            }
            if (flyMap.keySet().size() == 0) {
                Bukkit.getScheduler().cancelTask(id);
                id = -1;
            }
        }, 10L, 3L);
    }

    public static void startFlyCheck() {
        flyid = Bukkit.getScheduler().scheduleSyncRepeatingTask(SavageFactions.plugin, () -> { //threw the exception for now, until I recode fly :( Cringe.
            checkTaskState();
            if (flyMap.keySet().size() != 0) {
                for (String name : flyMap.keySet()) {
                    if (name == null) {
                        continue;
                    }
                    Player player = Bukkit.getPlayer(name);
                    if (player == null
                            || !player.isFlying()
                            || player.getGameMode() == GameMode.CREATIVE
                            || !SavageFactions.plugin.mc17 && player.getGameMode() == GameMode.SPECTATOR) {
                        continue;
                    }
                    FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                    Faction myFaction = fPlayer.getFaction();
                    if (myFaction.isWilderness()) {
                        fPlayer.setFlying(false);
                        flyMap.remove(name);
                        continue;
                    }
                    if (player.hasPermission("factions.fly.bypassnearbyenemycheck") || fPlayer.checkIfNearbyEnemies()) {
                        continue;
                    }
                    FLocation myFloc = new FLocation(player.getLocation());
                    if (Board.getInstance().getFactionAt(myFloc) != myFaction) {
                        if (!checkBypassPerms(fPlayer, player, Board.getInstance().getFactionAt(myFloc))) {
                            fPlayer.setFlying(false);
                            flyMap.remove(name);
                        }
                    }

                }
            }

        }, 20L, 20L);
    }

    private static boolean checkBypassPerms(FPlayer fme, Player me, Faction toFac) {
        if (toFac != fme.getFaction()) {
            if (!me.hasPermission("factions.fly.wilderness") && toFac.isWilderness() || !me.hasPermission("factions.fly.safezone") && toFac.isSafeZone() || !me.hasPermission("factions.fly.warzone") && toFac.isWarZone()) {
                fme.msg(TL.COMMAND_FLY_NO_ACCESS, toFac.getTag(fme));
                return false;
            }
            Access access = toFac.getAccess(fme, PermissableAction.FLY);
            if ((!(me.hasPermission("factions.fly.enemy") || access == Access.ALLOW)) && toFac.getRelationTo(fme.getFaction()) == Relation.ENEMY) {
                fme.msg(TL.COMMAND_FLY_NO_ACCESS, toFac.getTag(fme));
                return false;
            }
            if (!(me.hasPermission("factions.fly.ally") || access == Access.ALLOW) && toFac.getRelationTo(fme.getFaction()) == Relation.ALLY) {
                fme.msg(TL.COMMAND_FLY_NO_ACCESS, toFac.getTag(fme));
                return false;
            }
            if (!(me.hasPermission("factions.fly.truce") || access == Access.ALLOW) && toFac.getRelationTo(fme.getFaction()) == Relation.TRUCE) {
                fme.msg(TL.COMMAND_FLY_NO_ACCESS, toFac.getTag(fme));
                return false;
            }

            if (!(me.hasPermission("factions.fly.neutral") || access == Access.ALLOW) && toFac.getRelationTo(fme.getFaction()) == Relation.NEUTRAL && !isSystemFaction(toFac)) {
                fme.msg(TL.COMMAND_FLY_NO_ACCESS, toFac.getTag(fme));
                return false;
            }
            return me.hasPermission("factions.fly") && access != Access.DENY;
        }
        return true;
    }

    public static Boolean isSystemFaction(Faction faction) {
        return faction.isSafeZone() ||
                faction.isWarZone() ||
                faction.isWilderness();
    }

    public static void checkTaskState() {
        if (flyMap.keySet().size() == 0) {
            Bukkit.getScheduler().cancelTask(flyid);
            flyid = -1;
        }
    }

    public static void disableFlight(final FPlayer fme) {
        fme.setFlying(false);
        flyMap.remove(fme.getPlayer().getName());
    }

    public boolean isInFlightChecker(Player player) {
        return flyMap.containsKey(player.getName());
    }

    @Override
    public void perform(CommandContext context) {
        // Disabled by default.
        if (!SavageFactions.plugin.getConfig().getBoolean("enable-faction-flight", false)) {
            context.fPlayer.msg(TL.COMMAND_FLY_DISABLED);
            return;
        }

        if (Conf.enableFlyParticles && context.fPlayer.getSelectedParticle() == null) {
            context.msg(TL.COMMAND_PARTICLE_NO_SELECTED_PARTICLE);
        }

        FLocation myfloc = new FLocation(context.player.getLocation());
        Faction toFac = Board.getInstance().getFactionAt(myfloc);
        if (!checkBypassPerms(context.fPlayer, context.player, toFac)) return;
        List<Entity> entities = context.player.getNearbyEntities(16.0D, 256.0D, 16.0D);

        for (int i = 0; i <= entities.size() - 1; ++i) {
            if (entities.get(i) instanceof Player) {
                Player eplayer = (Player) entities.get(i);
                FPlayer efplayer = FPlayers.getInstance().getByPlayer(eplayer);
                if (efplayer.getRelationTo(context.fPlayer) == Relation.ENEMY && !efplayer.isStealthEnabled()) {
                    context.msg(TL.COMMAND_FLY_CHECK_ENEMY);
                    return;
                }
            }
        }


        if (context.args.size() == 0) {
            toggleFlight(context.fPlayer.isFlying(), context.fPlayer, context);
        } else if (context.args.size() == 1) {
            toggleFlight(context.argAsBool(0), context.fPlayer, context);
        }
    }

    private void toggleFlight(final boolean toggle, final FPlayer fme, CommandContext context) {
        if (toggle) {
            fme.setFlying(false);
            flyMap.remove(fme.getPlayer().getName());
            return;
        }


        if (fme.canFlyAtLocation())
            context.doWarmUp(WarmUpUtil.Warmup.FLIGHT, TL.WARMUPS_NOTIFY_FLIGHT, "Fly", () -> {
                fme.setFlying(true);
                flyMap.put(fme.getPlayer().getName(), true);
                if (id == -1) {
                    if (Conf.enableFlyParticles) {
                        startParticles();
                    }
                }
                if (flyid == -1) {
                    startFlyCheck();
                }
            }, SavageFactions.plugin.getConfig().getLong("warmups.f-fly", 0));
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_FLY_DESCRIPTION;
    }

}
