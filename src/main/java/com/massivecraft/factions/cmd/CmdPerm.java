package com.massivecraft.factions.cmd;

import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.Permissable;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.fperms.gui.PermissableActionFrame;
import com.massivecraft.factions.zcore.fperms.gui.PermissableRelationFrame;
import com.massivecraft.factions.zcore.util.TL;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CmdPerm extends FCommand {

	public CmdPerm() {
		super();
		this.aliases.add("perm");
		this.aliases.add("perms");
		this.aliases.add("permission");
		this.aliases.add("permissions");

		this.requirements = new CommandRequirements.Builder(Permission.MANAGE_PERMISSIONS)
				.playerOnly()
				.memberOnly()
				.withRole(Role.LEADER)
				.build();
	}

	@Override
	public void perform(CommandContext context) {

		if (context.args.size() == 0) {
			new PermissableRelationFrame(context.faction).buildGUI(context.fPlayer);
			return;
		} else if (context.args.size() == 1 && getPermissable(context.argAsString(0)) != null) {
			new PermissableActionFrame(context.faction).buildGUI(context.fPlayer, getPermissable(context.argAsString(0)));
			return;
		}

		// If not opening GUI, then setting the permission manually.
		if (context.args.size() != 3) {
			context.msg(TL.COMMAND_PERM_DESCRIPTION);
			return;
		}

		Set<Permissable> permissables = new HashSet<>();
		Set<PermissableAction> permissableActions = new HashSet<>();

		boolean allRelations = context.argAsString(0).equalsIgnoreCase("all");
		boolean allActions = context.argAsString(1).equalsIgnoreCase("all");

		if (allRelations) {
			permissables.addAll(context.faction.getPermissions().keySet());
		} else {
			Permissable permissable = getPermissable(context.argAsString(0));

			if (permissable == null) {
				context.msg(TL.COMMAND_PERM_INVALID_RELATION);
				return;
			}

			permissables.add(permissable);
		}

		if (allActions) {
			permissableActions.addAll(Arrays.asList(PermissableAction.values()));
		} else {
			PermissableAction permissableAction = PermissableAction.fromString(context.argAsString(1));
			if (permissableAction == null) {
				context.msg(TL.COMMAND_PERM_INVALID_ACTION);
				return;
			}

			permissableActions.add(permissableAction);
		}

		Access access = Access.fromString(context.argAsString(2));

		if (access == null) {
			context.msg(TL.COMMAND_PERM_INVALID_ACCESS);
			return;
		}

		for (Permissable permissable : permissables) {
			for (PermissableAction permissableAction : permissableActions) {
				context.faction.setPermission(permissable, permissableAction, access);
			}
		}

		context.msg(TL.COMMAND_PERM_SET, context.argAsString(1), access.name(), context.argAsString(0));
		SavageFactions.plugin.log(String.format(TL.COMMAND_PERM_SET.toString(), context.argAsString(1), access.name(), context.argAsString(0)) + " for faction " + context.fPlayer.getTag());
	}

	private Permissable getPermissable(String name) {
		if (Role.fromString(name.toUpperCase()) != null) {
			return Role.fromString(name.toUpperCase());
		} else if (Relation.fromString(name.toUpperCase()) != null) {
			return Relation.fromString(name.toUpperCase());
		} else {
			return null;
		}
	}

	@Override
	public TL getUsageTranslation() {
		return TL.COMMAND_PERM_DESCRIPTION;
	}

}