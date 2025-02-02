package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;

public class CmdInviteAlt extends FCommand {

    public CmdInviteAlt() {
        super();
        this.aliases.add("invite");
        this.requiredArgs.add("player name");

        this.requirements = new CommandRequirements.Builder(Permission.INVITE)
                .playerOnly()
                .memberOnly()
                .build();
    }

    @Override
    public void perform(CommandContext context) {
        if(!Conf.enableFactionAlts){
            context.fPlayer.msg(TL.GENERIC_DISABLED);
            return;
        }

        FPlayer target = context.argAsBestFPlayerMatch(0);
        if (target == null) {
            return;
        }

        if (target.getFaction() == context.faction) {
            context.msg(TL.COMMAND_INVITE_ALREADYMEMBER, target.getName(), context.faction.getTag());
            return;
        }

        // if economy is enabled, they're not on the bypass list, and this
        // command has a cost set, make 'em pay
        if (!context.payForCommand(Conf.econCostInvite, TL.COMMAND_INVITE_TOINVITE.toString(), TL.COMMAND_INVITE_FORINVITE.toString())) {
            return;
        }

        if (!context.fPlayer.isAdminBypassing()) {
            Access access = context.faction.getAccess(context.fPlayer, PermissableAction.INVITE);
            if (access != Access.ALLOW && context.fPlayer.getRole() != Role.LEADER) {
                context.msg(TL.GENERIC_FPERM_NOPERMISSION, "manage invites");
                return;
            }
        }

        if (context.faction.isBanned(target)) {
            context.msg(TL.COMMAND_INVITE_BANNED, target.getName());
            return;
        }

        context.faction.deinvite(target);
        context.faction.altInvite(target);
        if (!target.isOnline()) {
            return;
        }

        FancyMessage message = new FancyMessage(context.fPlayer.describeTo(target, true))
                .tooltip(TL.COMMAND_INVITE_CLICKTOJOIN.toString())
                .command("/" + Conf.baseCommandAliases.get(0) + " join " + context.faction.getTag())
                .then(TL.COMMAND_INVITE_INVITEDYOU.toString())
                .color(ChatColor.YELLOW)
                .tooltip(TL.COMMAND_INVITE_CLICKTOJOIN.toString())
                .command("/" + Conf.baseCommandAliases.get(0) + " join " + context.faction.getTag())
                .then(context.faction.describeTo(target)).tooltip(TL.COMMAND_INVITE_CLICKTOJOIN.toString())
                .command("/" + Conf.baseCommandAliases.get(0) + " join " + context.faction.getTag());

        message.send(target.getPlayer());

        context.faction.msg(TL.COMMAND_ALTINVITE_INVITED_ALT, context.fPlayer.describeTo(context.faction, true), target.describeTo(context.faction));
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_ALTINVITE_DESCRIPTION;
    }
}

