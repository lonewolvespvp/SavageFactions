package com.massivecraft.factions.cmd;

import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.listeners.FactionsBlockListener;
import com.massivecraft.factions.listeners.FactionsPlayerListener;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.WarmUpUtil;
import com.massivecraft.factions.zcore.util.TL;

public class CmdTpBanner extends FCommand {
    public CmdTpBanner() {
        super();
        this.aliases.add("tpbanner");

        this.requirements = new CommandRequirements.Builder(Permission.TPBANNER)
                .playerOnly()
                .memberOnly()
                .build();
    }

    @Override
    public void perform(CommandContext context) {
        if (!SavageFactions.plugin.getConfig().getBoolean("fbanners.Enabled")) {
            return;
        }

        final FactionsPlayerListener fpl = new FactionsPlayerListener();

        if (FactionsBlockListener.bannerLocations.containsKey(context.fPlayer.getTag())) {
            context.msg(TL.COMMAND_TPBANNER_SUCCESS);
            context.doWarmUp(WarmUpUtil.Warmup.BANNER, TL.WARMUPS_NOTIFY_TELEPORT, "Banner", () -> {
                    context.player.teleport(FactionsBlockListener.bannerLocations.get(context.fPlayer.getTag()));
            }, SavageFactions.plugin.getConfig().getLong("warmups.f-banner", 0));
        } else {
            context.msg(TL.COMMAND_TPBANNER_NOTSET);
        }

    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_TPBANNER_DESCRIPTION;
    }
}
