package com.bulka.net.tg.bots.ldrs.chats;

import com.bulka.net.tg.bots.ldrs.Bot;
import com.bulka.net.tg.bots.ldrs.Main;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Chat implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id = 0;
    private String title = "";
    private List<Long> whitelist = new ArrayList<>();
    private List<Trigger> triggers = new ArrayList<>();
    private String triggerText = "Your text may contain prohibited text. If you are not a bot, click the button below.";
    private boolean canUserUseCommands = false;
    private boolean enableTriggers = true;
    private transient List<Long> admins = new ArrayList<>();

    public Chat(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public void reloadAdmins(){
        admins = Main.getBot().getChatAdmins(id);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public boolean isAdmin(long id){
        return admins.contains(id);
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Long> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<Long> whitelist) {
        this.whitelist = whitelist;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    public List<Long> getAdmins() {
        return admins;
    }

    public void setAdmins(List<Long> admins) {
        this.admins = admins;
    }

    public boolean isCanUserUseCommands() {
        return canUserUseCommands;
    }

    public void setCanUserUseCommands(boolean canUserUseCommands) {
        this.canUserUseCommands = canUserUseCommands;
    }

    public boolean isEnableTriggers() {
        return enableTriggers;
    }

    public void setEnableTriggers(boolean enableTriggers) {
        this.enableTriggers = enableTriggers;
    }

    public String getTriggerText() {
        return triggerText;
    }

    public void setTriggerText(String triggerText) {
        this.triggerText = triggerText;
    }
}
