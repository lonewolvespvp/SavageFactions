package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

public class CmdVault extends FCommand {

    public CmdVault() {
        this.aliases.add("vault");

        this.requirements = new CommandRequirements.Builder(Permission.VAULT)
                .playerOnly()
                .memberOnly()
                .withAction(PermissableAction.VAULT)
                .build();
    }

    @Override
    public void perform(CommandContext context) {

        if (!SavageFactions.plugin.getConfig().getBoolean("fvault.Enabled")) {
            context.fPlayer.sendMessage("This command is disabled!");
            return;
        }

        if (context.fPlayer.isInVault()) {
            context.player.closeInventory();
            return;
        }

        context.fPlayer.setInVault(true);
        Location vaultLocation = context.faction.getVault();
        if (vaultLocation == null) {
            context.msg(TL.COMMAND_VAULT_INVALID);
            return;
        }
        FLocation vaultFLocation = new FLocation(vaultLocation);
        if (Board.getInstance().getFactionAt(vaultFLocation) != context.faction) {
            context.faction.setVault(null);
            context.msg(TL.COMMAND_VAULT_INVALID);
            return;
        }
        if (vaultLocation.getBlock().getType() != Material.CHEST) {
            context.faction.setVault(null);
            context.msg(TL.COMMAND_VAULT_INVALID);
            return;
        }
        Chest chest = (Chest) vaultLocation.getBlock().getState();
        Inventory chestInv = chest.getBlockInventory();
        context.msg(TL.COMMAND_VAULT_OPENING);
        context.player.openInventory(chestInv);


    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_VAULT_DESCRIPTION;
    }

}
