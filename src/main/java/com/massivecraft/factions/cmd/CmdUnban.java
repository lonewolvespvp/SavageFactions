package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;

public class CmdUnban extends FCommand {

    public CmdUnban() {
        super();
        this.aliases.add("unban");
        this.requiredArgs.add("target");

        this.requirements = new CommandRequirements.Builder(Permission.BAN)
                .playerOnly()
                .memberOnly()
                .withAction(PermissableAction.BAN)
                .build();
    }

    @Override
    public void perform(CommandContext context) {
        // Good on permission checks. Now lets just ban the player.
        FPlayer target = context.argAsFPlayer(0);
        if (target == null) {
            return; // the above method sends a message if fails to find someone.
        }

        if (target.getFaction() != context.fPlayer.getFaction()) {
            if (target.getFaction().getAccess(context.fPlayer, PermissableAction.BAN) != Access.ALLOW) {
                if (!context.fPlayer.isAdminBypassing()) {
                    context.fPlayer.msg(TL.COMMAND_UNBAN_TARGET_IN_OTHER_FACTION);
                }
            }
        }

        if (!context.faction.isBanned(target)) {
            context.msg(TL.COMMAND_UNBAN_NOTBANNED, target.getName());
            return;
        }

        context.faction.unban(target);

        context.msg(TL.COMMAND_UNBAN_UNBANNED, context.fPlayer.getName(), target.getName());
        target.msg(TL.COMMAND_UNBAN_TARGET, context.faction.getTag(target));
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_UNBAN_DESCRIPTION;
    }
}
