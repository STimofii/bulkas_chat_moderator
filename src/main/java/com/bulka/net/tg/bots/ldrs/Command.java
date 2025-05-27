package com.bulka.net.tg.bots.ldrs;

import com.bulka.net.tg.bots.ldrs.chats.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public abstract class Command {
    private String name = "";
    private boolean onlyForAdmins = false;
    public abstract void execute(Update update, String arguments, Chat chat);

    public Command() {
    }

    public Command(String name) {
        this.name = name;
    }

    public Command(String name, boolean onlyForAdmins) {
        this.name = name;
        this.onlyForAdmins = onlyForAdmins;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isOnlyForAdmins() {
        return onlyForAdmins;
    }

    public void setOnlyForAdmins(boolean onlyForAdmins) {
        this.onlyForAdmins = onlyForAdmins;
    }
}
