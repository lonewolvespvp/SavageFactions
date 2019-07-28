package com.massivecraft.factions.addon;

import com.massivecraft.factions.cmd.FCommand;

import java.util.List;

public abstract class BaseAddon {

    protected static List<Object> listeners;
    protected static List<FCommand> fCommands;
    protected String addonIdentifier;

    public abstract List<Object> getListeners();

    public abstract List<FCommand> getfCommands();
}