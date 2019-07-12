package com.massivecraft.factions.zcore.chatconfig;

import com.massivecraft.factions.Conf;
import me.tom.sparse.spigot.chat.menu.ChatMenu;
import me.tom.sparse.spigot.chat.menu.element.Element;
import me.tom.sparse.spigot.chat.menu.element.IncrementalElement;
import me.tom.sparse.spigot.chat.menu.element.InputElement;


import java.lang.reflect.Field;

public class ChatConfig {

    private Field[] fields;

    // Options per page.
    private int fieldsPerPage = 5;

    public ChatConfig(Class config) {
        this.fields = config.getFields();
    }

    public void showPage(int page) {
        ChatMenu chatMenu = new ChatMenu();
        for (int i = fieldsPerPage * (page - 1); i < fieldsPerPage * page; i++) {
            chatMenu.add(getAppropriateElement(fields[i]));
        }
    }

    private Element getAppropriateElement(Field field) {
        if (field.getType().isAssignableFrom(String.class))
            return new InputElement(10, 10, 10, field.getName());
        else if (field.getType().isAssignableFrom(int.class))
            return new IncrementalElement(10, 10, field.get())


    }


}
