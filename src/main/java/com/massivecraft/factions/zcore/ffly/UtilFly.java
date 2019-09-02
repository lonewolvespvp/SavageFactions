package com.massivecraft.factions.zcore.ffly;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;

public class UtilFly {


    public static void run() {
        if (!SavageFactions.plugin.getConfig().getBoolean("enable-faction-flight"))
            return;

        Bukkit.getScheduler().scheduleSyncRepeatingTask(SavageFactions.plugin, () -> {
            for (FPlayer fp : FPlayers.getInstance().getOnlinePlayers()) {
                if (fp.isFlying()) fp.checkIfNearbyEnemies();
            }
        }, 0, SavageFactions.plugin.getConfig().getInt("fly-task-interval", 10));
    }

    public static void setFly(FPlayer fp, boolean fly, boolean silent, boolean damage) {
        if (!SavageFactions.plugin.getConfig().getBoolean("enable-faction-flight"))
            return;

        fp.getPlayer().setAllowFlight(fly);
        fp.getPlayer().setFlying(fly);
        fp.setFlying(fly);


        if (!silent) {
            if (!damage) {
                fp.msg(TL.COMMAND_FLY_CHANGE, fly ? TL.GENERIC_ENABLED : TL.GENERIC_DISABLED);
            } else {
                fp.msg(TL.COMMAND_FLY_DAMAGE);
            }
        }

        setFallDamage(fp, fly, damage);
    }

    public static void checkFly(FPlayer me, Faction factionTo) {
        if (!SavageFactions.plugin.getConfig().getBoolean("enable-faction-flight"))
            return;

        if (me.isAdminBypassing() && me.isFlying())
            return;

        if (!me.isFlying()) {
            if (me.isAdminBypassing()) {
                UtilFly.setFly(me, true, false, false);
                return;
            }

            if (factionTo == me.getFaction() && me.getPlayer().hasPermission("factions.fly")) {
                UtilFly.setFly(me, true, false, false);
            } else {
                Relation relationTo = factionTo.getRelationTo(me);
                if ((factionTo.isWilderness() && me.canflyinWilderness()) || (factionTo.isWarZone() && me.canflyinWarzone())
                        || (factionTo.isSafeZone() && me.canflyinSafezone()) || (relationTo == Relation.ENEMY && me.canflyinEnemy())
                        || (relationTo == Relation.ALLY && me.canflyinAlly()) || (relationTo == Relation.TRUCE && me.canflyinTruce())
                        || (relationTo == Relation.NEUTRAL && me.canflyinNeutral())) {
                    UtilFly.setFly(me, true, false, false);
                }
            }
        } else {
            Relation relationTo = factionTo.getRelationTo(me);
            if ((factionTo.equals(me.getFaction()) && !me.getPlayer().hasPermission("factions.fly"))
                    || (factionTo.isWilderness() && !me.canflyinWilderness()) || (factionTo.isWarZone() && !me.canflyinWarzone())
                    || (factionTo.isSafeZone() && !me.canflyinSafezone()) || (relationTo == Relation.ENEMY && !me.canflyinEnemy())
                    || (relationTo == Relation.ALLY && !me.canflyinAlly()) || (relationTo == Relation.TRUCE && !me.canflyinTruce())
                    || (relationTo == Relation.NEUTRAL && !me.canflyinNeutral())) {
                UtilFly.setFly(me, false, false, false);
            }
        }
    }

    public static void setFallDamage(FPlayer fp, boolean fly, boolean damage) {
        if (!fly) {
            if (!damage) {
                fp.sendMessage(TL.COMMAND_FLY_COOLDOWN.toString().replace("{amount}", SavageFactions.plugin.getConfig().getInt("fly-falldamage-cooldown", 3) + ""));
            }

            int cooldown = SavageFactions.plugin.getConfig().getInt("fly-falldamage-cooldown", 3);
            if (cooldown > 0) {
                fp.setTakeFallDamage(false);
                Bukkit.getScheduler().runTaskLater(SavageFactions.plugin, () -> fp.setTakeFallDamage(true), 20L * cooldown);
            }
        }
    }
}


