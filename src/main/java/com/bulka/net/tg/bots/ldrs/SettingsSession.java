package com.bulka.net.tg.bots.ldrs;

import com.bulka.net.tg.bots.ldrs.chats.Chat;
import com.bulka.net.tg.bots.ldrs.chats.Trigger;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public class SettingsSession {
    private SettingsMenu settingsMenu = SettingsMenu.NONE;
    private int page = 0;
    private int currentListID = 0;
    private int pages = 0;
    private Chat chat;
    private int messageID;
    private String text = "Settings";

    public SettingsSession() {
    }

    public SettingsSession(SettingsMenu settingsMenu, int page, Chat chat) {
        this.settingsMenu = settingsMenu;
        this.page = page;
        this.chat = chat;
    }

    public SettingsSession(SettingsMenu settingsMenu, int page, Chat chat, int messageID) {
        this.settingsMenu = settingsMenu;
        this.page = page;
        this.chat = chat;
        this.messageID = messageID;
    }

    public void onGetCallback(CallbackQuery callback) {
        pages = (int) Math.ceil((double) chat.getTriggers().size() / 8);
        long userID = callback.getFrom().getId();
        boolean isAdmin = chat.isAdmin(userID);
        if ((isAdmin || chat.isCanUserUseCommands())) {
            String callbackData = callback.getData();
            switch (callbackData) {
                case "allow_commands_for_members":
                    chat.setCanUserUseCommands(true);
                    Main.getBot().saveChat(chat);
                    updateSettingsMenu();
                    break;
                case "deny_commands_for_members":
                    chat.setCanUserUseCommands(false);
                    Main.getBot().saveChat(chat);
                    updateSettingsMenu();
                    break;

                case "disable_triggers":
                    chat.setEnableTriggers(false);
                    Main.getBot().saveChat(chat);
                    updateSettingsMenu();
                    break;
                case "enable_triggers":
                    chat.setEnableTriggers(true);
                    Main.getBot().saveChat(chat);
                    updateSettingsMenu();
                    break;

                case "goto_triggers":
                case "back_to_triggers_settings":
                    settingsMenu = SettingsMenu.TRIGGERS;
                    updateSettingsMenu();
                    currentListID = 0;
                    break;

                case "arrow_left":
                   if(page >= 1)
                       page--;
                    updateSettingsMenu();
                    break;
                case "arrow_right":
                    if(page < pages)
                        page++;
                    updateSettingsMenu();
                    break;

                case "back_to_general_settings":
                    settingsMenu = SettingsMenu.GENERAL;
                    currentListID = 0;
                    page = 0;
                    updateSettingsMenu();
                    break;
                case "close_settings":
                    currentListID = 0;
                    page = 0;
                    Main.getBot().deleteMessage(chat.getId(), messageID);
                    Main.getBot().getSettingsSessions().remove(chat.getId());
                    break;

                default:
                    if(callbackData.startsWith("selected_trigger_")){
                        settingsMenu = SettingsMenu.TRIGGER;
                        currentListID = Integer.parseInt(callbackData.replace("selected_trigger_", ""));
                        updateSettingsMenu();
                    }
                    if(callbackData.startsWith("do_trigger_not_strict_")){
                        int triggerID = Integer.parseInt(callbackData.replace("do_trigger_not_strict_", ""));
                        Trigger trigger = chat.getTriggers().get(triggerID);
                        trigger.setStrict(false);
                        Main.getBot().saveChat(chat);
                        updateSettingsMenu();
                    }
                    if(callbackData.startsWith("do_trigger_strict_")){
                        int triggerID = Integer.parseInt(callbackData.replace("do_trigger_strict_", ""));
                        Trigger trigger = chat.getTriggers().get(triggerID);
                        trigger.setStrict(true);
                        Main.getBot().saveChat(chat);
                        updateSettingsMenu();
                    }
                    if(callbackData.startsWith("do_trigger_not_lethal_")){
                        int triggerID = Integer.parseInt(callbackData.replace("do_trigger_not_lethal_", ""));
                        Trigger trigger = chat.getTriggers().get(triggerID);
                        trigger.setLethal(false);
                        Main.getBot().saveChat(chat);
                        updateSettingsMenu();
                    }
                    if(callbackData.startsWith("do_trigger_lethal_")){
                        int triggerID = Integer.parseInt(callbackData.replace("do_trigger_lethal_", ""));
                        Trigger trigger = chat.getTriggers().get(triggerID);
                        trigger.setLethal(true);
                        Main.getBot().saveChat(chat);
                        updateSettingsMenu();
                    }
                    if(callbackData.startsWith("do_trigger_not_advanced_check_")){
                        int triggerID = Integer.parseInt(callbackData.replace("do_trigger_not_advanced_check_", ""));
                        Trigger trigger = chat.getTriggers().get(triggerID);
                        trigger.setAdvancedCheck(false);
                        Main.getBot().saveChat(chat);
                        updateSettingsMenu();
                    }
                    if(callbackData.startsWith("do_trigger_advanced_check_")){
                        int triggerID = Integer.parseInt(callbackData.replace("do_trigger_advanced_check_", ""));
                        Trigger trigger = chat.getTriggers().get(triggerID);
                        trigger.setAdvancedCheck(true);
                        Main.getBot().saveChat(chat);
                        updateSettingsMenu();
                    }
                    break;
                }
        } else {
            Main.getBot().answerCallback(callback.getId(), "You must be an admin", true);
        }

    }

    public void updateSettingsMenu() {
        try {
            InlineKeyboardMarkup inlineKeyboardMarkup = Main.getBot().getKeyboardMarkup(this);
            Main.getBot().editMessage(chat.getId(), messageID, text, inlineKeyboardMarkup);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public SettingsMenu getSettingsMenu() {
        return settingsMenu;
    }

    public void setSettingsMenu(SettingsMenu settingsMenu) {
        this.settingsMenu = settingsMenu;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public int getMessageID() {
        return messageID;
    }

    public void setMessageID(int messageID) {
        this.messageID = messageID;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPages() {
        return pages;
    }

    public int getCurrentListID() {
        return currentListID;
    }

    public void setCurrentListID(int currentListID) {
        this.currentListID = currentListID;
    }
}
