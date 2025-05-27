package com.bulka.net.tg.bots.ldrs;

import com.bulka.net.tg.bots.ldrs.chats.Chat;
import com.bulka.net.tg.bots.ldrs.chats.Trigger;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Bot extends TelegramLongPollingBot {
    public static final String SAVES_PATH = "saves";
    private String username = "";
    private static final Logger logger = Logger.getLogger(Bot.class.getName());
    private HashMap<Long, Chat> chats = new HashMap<>();
    private HashMap<String, Command> commands = new HashMap<>();
    private HashMap<Long, SettingsSession> settingsSessions = new HashMap<>();
    private HashMap<String, Timer> waitingTriggerTimers = new HashMap<>();

    public List<InlineKeyboardButton> closeSettingsKeyboardButtonRow = new ArrayList<>();
    public List<InlineKeyboardButton> backToGeneralSettingsKeyboardButtonRow = new ArrayList<>();
    public List<InlineKeyboardButton> backToTriggersSettingsKeyboardButtonRow = new ArrayList<>();
    public List<InlineKeyboardButton> backToWhitelistSettingsKeyboardButtonRow = new ArrayList<>();
    public InlineKeyboardButton arrowLeftSettingsKeyboardButton;
    public InlineKeyboardButton arrowRightSettingsKeyboardButton;
    public InlineKeyboardButton arrowLeftENDSettingsKeyboardButton;
    public InlineKeyboardButton arrowRightENDSettingsKeyboardButton;

    public InlineKeyboardButton imNotABotKeyboardButton;

    public Bot(DefaultBotOptions defaultBotOptions, String token, String username) {
        super(defaultBotOptions, token);
        this.username = username;
        try {
            load();
            save();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in bot starting", e);
            throw new RuntimeException(e);
        }

        closeSettingsKeyboardButtonRow.add(createInlineKeyboardButton("Close settings", "close_settings"));
        backToGeneralSettingsKeyboardButtonRow.add(createInlineKeyboardButton("Back to general", "back_to_general_settings"));
        backToTriggersSettingsKeyboardButtonRow.add(createInlineKeyboardButton("Back to triggers", "back_to_triggers_settings"));
        backToWhitelistSettingsKeyboardButtonRow.add(createInlineKeyboardButton("Back to whitelist", "back_to_whitelist_settings"));
        arrowLeftSettingsKeyboardButton = createInlineKeyboardButton("⬅\uFE0F", "arrow_left");
        arrowRightSettingsKeyboardButton = createInlineKeyboardButton("➡\uFE0F", "arrow_right");
        arrowLeftENDSettingsKeyboardButton = createInlineKeyboardButton("⏮\uFE0F", "arrow_left_end");
        arrowRightENDSettingsKeyboardButton = createInlineKeyboardButton("⏭\uFE0F", "arrow_right_end");

        imNotABotKeyboardButton = createInlineKeyboardButton("I`m not a bot", "im_not_a_bot");

        commands.put("start", new Command("start", false) {
            @Override
            public void execute(Update update, String arguments, Chat chat) {
                deleteMessage(chat.getId(), update.getMessage().getMessageId());
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getId());
                sendMessage.setText("Hello, World!");
                sendMessage(sendMessage);
            }
        });
        commands.put("reload", new Command("reload", false) {
            @Override
            public void execute(Update update, String arguments, Chat chat) {
                deleteMessage(chat.getId(), update.getMessage().getMessageId());
                chat.reloadAdmins();
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getId());
                sendMessage.setText("Reloaded admins!");
                sendMessage(sendMessage);
            }
        });
        commands.put("delete_chat_from_save", new Command("delete_chat_from_save", false) {
            @Override
            public void execute(Update update, String arguments, Chat chat) {
                deleteMessage(chat.getId(), update.getMessage().getMessageId());
                chats.put(chat.getId(), new Chat(chat.getId(), chat.getTitle()));
                chat = chats.get(chat.getId());
                settingsSessions.remove(chat.getId());
                chat.reloadAdmins();

                saveChat(chat);
            }
        });
        commands.put("whitelist", new Command("whitelist", false) {
            @Override
            public void execute(Update update, String arguments, Chat chat) {
                deleteMessage(chat.getId(), update.getMessage().getMessageId());
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getId());

                StringBuilder sb = new StringBuilder("Whitelist:\n");
                for (long userID : chat.getWhitelist()){
                    sb.append(userID).append(", ");
                }
                sendMessage.setText(sb.toString());
                sendMessage(sendMessage);
            }
        });
        commands.put("add_to_whitelist", new Command("add_to_whitelist", false) {
            @Override
            public void execute(Update update, String arguments, Chat chat) {
                deleteMessage(chat.getId(), update.getMessage().getMessageId());
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getId());
                long userID = 0;
                try {
                    userID = Long.parseLong(arguments);
                } catch (Exception e){
                    userID = update.getMessage().getReplyToMessage().getFrom().getId();
                }

                if(userID != 0) {
                    if(chat.getWhitelist().contains(userID)){
                        sendMessage.setText("User " + userID + " already in the whitelist");
                    } else {
                        chat.getWhitelist().add(userID);
                        sendMessage.setText("Added user " + userID + " to the whitelist");
                    }
                }
                sendMessage(sendMessage);
                saveChat(chat);
            }
        });
        commands.put("remove_from_whitelist", new Command("remove_from_whitelist", false) {
            @Override
            public void execute(Update update, String arguments, Chat chat) {
                deleteMessage(chat.getId(), update.getMessage().getMessageId());
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getId());
                long userID = 0;
                try {
                    userID = Long.parseLong(arguments);
                } catch (Exception e){
                    userID = update.getMessage().getReplyToMessage().getFrom().getId();
                }

                if(userID != 0) {
                    if(chat.getWhitelist().remove(userID)) {
                        sendMessage.setText("Removed user " + userID + " from the whitelist");
                    } else {
                        sendMessage.setText("User " + userID + " was not in the whitelist");
                    }
                }
                sendMessage(sendMessage);
                saveChat(chat);
            }
        });

        commands.put("settings", new Command("settings", true) {
            @Override
            public void execute(Update update, String arguments, Chat chat) {
                deleteMessage(chat.getId(), update.getMessage().getMessageId());
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getId());

                SettingsSession settingsSession = new SettingsSession();
                if (settingsSessions.containsKey(chat.getId())) {
                    deleteMessage(chat.getId(), settingsSessions.get(chat.getId()).getMessageID());
                }
                settingsSessions.put(chat.getId(), settingsSession);
                settingsSession.setChat(chat);
                settingsSession.setSettingsMenu(SettingsMenu.GENERAL);
                settingsSession.setPage(0);
                InlineKeyboardMarkup inlineKeyboardMarkup = getKeyboardMarkup(settingsSession);
                sendMessage.setText(settingsSession.getText());
                sendMessage.setReplyMarkup(inlineKeyboardMarkup);
                settingsSession.setMessageID(sendMessage(sendMessage));
            }
        });
        commands.put("add_trigger", new Command("add_trigger", true) {
            @Override
            public void execute(Update update, String arguments, Chat chat) {
                deleteMessage(chat.getId(), update.getMessage().getMessageId());
                saveChat(chat);
                Message replyTo = update.getMessage().getReplyToMessage();
                if(!chat.containsTrigger(replyTo.getText())){
                    chat.getTriggers().add(new Trigger(replyTo.getText(), false, false, false));
                }
                deleteMessage(replyTo.getChatId(), replyTo.getMessageId());

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chat.getId());

                SettingsSession settingsSession = new SettingsSession();
                if (settingsSessions.containsKey(chat.getId())) {
                    deleteMessage(chat.getId(), settingsSessions.get(chat.getId()).getMessageID());
                }
                settingsSessions.put(chat.getId(), settingsSession);
                settingsSession.setChat(chat);
                settingsSession.setSettingsMenu(SettingsMenu.TRIGGER);
                settingsSession.setCurrentListID(chat.getTriggers().size()-1);
                InlineKeyboardMarkup inlineKeyboardMarkup = getKeyboardMarkup(settingsSession);
                sendMessage.setText(settingsSession.getText());
                sendMessage.setReplyMarkup(inlineKeyboardMarkup);
                settingsSession.setMessageID(sendMessage(sendMessage));
                saveChat(chat);
            }
        });
    }

    public InlineKeyboardMarkup getKeyboardMarkup(SettingsSession settingsSession) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        if (settingsSession.getSettingsMenu() == SettingsMenu.NONE) {
            return inlineKeyboardMarkup;
        }
        List<InlineKeyboardButton> rowArrows = new ArrayList<>();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        Chat chat = settingsSession.getChat();
        switch (settingsSession.getSettingsMenu()) {
            case NONE: {
                break;
            }

            case GENERAL: {
                settingsSession.setText("Settings");
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                List<InlineKeyboardButton> row3 = new ArrayList<>();
                if (chat.isCanUserUseCommands()) {
                    row1.add(createInlineKeyboardButton("Commands for non-admins ✅", "deny_commands_for_members"));
                } else {
                    row1.add(createInlineKeyboardButton("Commands for non-admins ❌", "allow_commands_for_members"));
                }
                if (chat.isEnableTriggers()) {
                    row2.add(createInlineKeyboardButton("Triggers ✅", "disable_triggers"));
                } else {
                    row2.add(createInlineKeyboardButton("Triggers ❌", "enable_triggers"));
                }
                row3.add(createInlineKeyboardButton("Triggers Settings", "goto_triggers"));
                keyboard.add(row1);
                keyboard.add(row2);
                keyboard.add(row3);
                break;
            }

            case TRIGGERS: {
                int page = settingsSession.getPage();
                settingsSession.setText("Triggers\nPage: " + (page + 1) + "/" + (settingsSession.getTriggerPages()));
                if (page != 0) {
                    rowArrows.add(arrowLeftSettingsKeyboardButton);
                    rowArrows.add(arrowLeftENDSettingsKeyboardButton);
                }
                if (page != settingsSession.getTriggerPages() - 1) {
                    rowArrows.add(arrowRightENDSettingsKeyboardButton);
                    rowArrows.add(arrowRightSettingsKeyboardButton);
                }
                for (int i = 0; i < 8; i++) {
                    int triggerID = page * 8 + i;
                    if (chat.getTriggers().size() <= triggerID) {
                        break;
                    }
                    Trigger trigger = chat.getTriggers().get(triggerID);
                    StringBuilder triggerText = new StringBuilder(trigger.getText());
                    if (triggerText.length() >= 30) {
                        triggerText = new StringBuilder(trigger.getText().substring(0, 30)).append("...");
                    }
                    ArrayList<InlineKeyboardButton> row = new ArrayList<>();
                    row.add(createInlineKeyboardButton(triggerText.toString(), "selected_trigger_" + triggerID));
                    keyboard.add(row);
                }
                keyboard.add(rowArrows);
                keyboard.add(backToGeneralSettingsKeyboardButtonRow);
                break;
            }

            case TRIGGER: {
                int triggerID = settingsSession.getCurrentListID();
                Trigger trigger = settingsSession.getChat().getTriggers().get(triggerID);
                StringBuilder triggerText = new StringBuilder(trigger.getText());
                if (triggerText.length() >= 300) {
                    triggerText = new StringBuilder(trigger.getText().substring(0, 300)).append("...");
                }
                settingsSession.setText("Trigger: " + triggerText);
                List<InlineKeyboardButton> row1 = new ArrayList<>();
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                List<InlineKeyboardButton> row3 = new ArrayList<>();
                if (trigger.isStrict()) {
                    row1.add(createInlineKeyboardButton("Strict ✅", "do_trigger_not_strict_" + triggerID));
                } else {
                    row1.add(createInlineKeyboardButton("Strict ❌", "do_trigger_strict_" + triggerID));
                }
                if (trigger.isLethal()) {
                    row1.add(createInlineKeyboardButton("Lethal ✅", "do_trigger_not_lethal_" + triggerID));
                } else {
                    row1.add(createInlineKeyboardButton("Lethal ❌", "do_trigger_lethal_" + triggerID));
                }
                if (trigger.isAdvancedCheck()) {
                    row2.add(createInlineKeyboardButton("AdvancedCheck ✅", "do_trigger_not_advanced_check_" + triggerID));
                } else {
                    row2.add(createInlineKeyboardButton("AdvancedCheck ❌", "do_trigger_advanced_check_" + triggerID));
                }
                row3.add(createInlineKeyboardButton("Remove trigger", "remove_trigger_" + triggerID));
                keyboard.add(row1);
                keyboard.add(row2);
                keyboard.add(row3);
                keyboard.add(backToTriggersSettingsKeyboardButtonRow);
                keyboard.add(backToGeneralSettingsKeyboardButtonRow);
                break;
            }

            default:{

            }
        }
        keyboard.add(closeSettingsKeyboardButtonRow);

        inlineKeyboardMarkup.setKeyboard(keyboard);

        return inlineKeyboardMarkup;
    }

    public void postInit() {
        reloadAllAdmins();
    }

    public void load() {
        logger.config("Loading");
        try {
            File saves = new File(SAVES_PATH);
            if (!saves.exists()) {
                if (!saves.mkdir()) {
                    throw new RuntimeException("Can`t create saves directory");
                }
            }
            chats = new HashMap<>();
            try (Stream<Path> s_chats = Files.list(Paths.get(saves.getPath()))) {
                Iterator<Path> chatsIterator = s_chats.iterator();
                while (chatsIterator.hasNext()) {
                    Path chatPath = chatsIterator.next();
                    logger.fine("Loading " + chatPath);
                    Chat chat;
                    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(chatPath.toFile().toPath()))) {
                        chat = (Chat) in.readObject();
                    }
                    chats.put(chat.getId(), chat);
                    logger.fine("Loaded " + chat.getTitle());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in loading", e);
            throw new RuntimeException(e);
        }
        logger.config("Loaded");
    }

    public void save() {
        logger.config("Saving");
        try {
            File saves = new File("saves");
            if (!saves.exists()) {
                if (!saves.mkdir()) {
                    throw new RuntimeException("Can`t create saves directory");
                }
            }
            for (Chat chat : chats.values()) {
                saveChat(chat);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in saving", e);
            throw new RuntimeException(e);
        }
        logger.config("Saved");
    }

    public void saveChat(Chat chat) {
        logger.fine("Saving " + chat.getTitle());
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(Paths.get(SAVES_PATH + "/" + chat.getId() + ".chat")))) {
            out.writeObject(chat);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in saving chat", e);
            throw new RuntimeException(e);
        }
        logger.fine("Saved " + chat.getTitle());
    }

    public void reloadAllAdmins() {
        logger.config("Reloading admins in all chats");
        for (Chat chat : chats.values()) {
            logger.fine("Reloading admins in " + chat.getTitle());
            chat.reloadAdmins();
            logger.fine("Reloaded admins in " + chat.getTitle());
        }
        logger.config("Reloaded admins in all chats");
    }

    public boolean checkCommand(Update update, Chat chat, boolean isAdmin) {
        String input = update.getMessage().getText();
        if (input == null || input.isEmpty()) {
            return false;
        }

        input = input.trim();

        if (!input.startsWith("/")) {
            return false;
        }

        String commandPart;
        String botName = null;
        String arguments = null;

        String[] parts = input.substring(1).split(" ", 2);
        commandPart = parts[0];

        if (parts.length > 1) {
            arguments = parts[1].trim();
        }

        if (commandPart.contains("@")) {
            String[] commandParts = commandPart.split("@", 2);
            commandPart = commandParts[0];
            botName = commandParts[1];
        }

        Command command = commands.get(commandPart.toLowerCase());

        if (command == null) {
            return false;
        }

        if (botName != null && !botName.equalsIgnoreCase(username)) {
            return false;
        }

        if (command.isOnlyForAdmins() && !(isAdmin || chat.isCanUserUseCommands())) {
            return false;
        }

        command.execute(update, arguments, chat);

        return true;
    }

    @Override
    public void onUpdateReceived(Update update) {
        logger.finest("Got update");
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText()) {
                    boolean canBeSpam = true;
                    String text = message.getText();

                    long chat_id = message.getChatId();
                    long user_id = message.getFrom().getId();
                    Chat chat = chats.get(chat_id);
                    if (chat == null) {
                        String title = message.getChat().getTitle();
                        logger.config("Adding chat " + title);
                        chat = new Chat(chat_id, title);
                        chat.reloadAdmins();
                        try {
                            saveChat(chat);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Error in saving chat", e);
                            throw new RuntimeException(e);
                        }

                        chats.put(chat_id, chat);
                        logger.config("Added chat " + title);
                    }
                    boolean isAdmin = chat.isAdmin(user_id);
                    canBeSpam = !isAdmin;

                    if (text.startsWith("/")) {
                        canBeSpam = !checkCommand(update, chat, isAdmin);
                    }


                    if (canBeSpam && chat.isEnableTriggers() && !chat.getTriggers().isEmpty()) {
                        text = text.toLowerCase();
                        for (Trigger trigger : chat.getTriggers()) {
                            if (trigger.isStrict()) {
                                if (text.equals(trigger.getText())) {
                                    if(checkTriggerFinally(trigger, message, chat))
                                        break;
                                }
                            } else {
                                if (text.contains(trigger.getText())) {
                                    if(checkTriggerFinally(trigger, message, chat))
                                        break;
                                }
                            }
                        }
                    }
                }
            } else if (update.hasCallbackQuery()) {
                CallbackQuery callback = update.getCallbackQuery();
                if(Objects.equals(callback.getData(), "im_not_a_bot")){
                    waitingTriggerTimers.remove(callback.getMessage().getChatId() + "/" + callback.getMessage().getMessageId());
                    deleteMessage(callback.getMessage().getChatId(), callback.getMessage().getMessageId());
                } else {
                    settingsSessions.get(update.getCallbackQuery().getMessage().getChatId()).onGetCallback(callback);
                }
            }
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Error in handling update", e);
        }
    }

    private boolean checkTriggerFinally(Trigger trigger, Message message, Chat chat) {
        if (trigger.isAdvancedCheck()) {
            if (advancedCheck(message.getFrom())) {
                punishTrigger(chat, trigger, message);
                return true;
            }
        } else {
            punishTrigger(chat, trigger, message);
            return  true;
        }
        return  false;
    }

    public boolean advancedCheck(User user) {
        if (user.getId() > Main.ADVANCED_ID_CHECK) {
            return true;
        }

        return false;
    }

    public void punishTrigger(Chat chat, Trigger trigger, Message message) {
        if (trigger.isLethal()) {
            deleteMessage(chat.getId(), message.getMessageId());
            kick(chat.getId(), message.getFrom().getId());
        } else {
            SendMessage sendTriggerMessage = new SendMessage();
            sendTriggerMessage.setChatId(chat.getId());
            sendTriggerMessage.setReplyToMessageId(message.getMessageId());
            sendTriggerMessage.setText(chat.getTriggerText());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(imNotABotKeyboardButton);
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(row);
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(keyboard);
            sendTriggerMessage.setReplyMarkup(inlineKeyboardMarkup);
            int sednedID = sendMessage(sendTriggerMessage);
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    deleteMessage(chat.getId(), message.getMessageId());
                    deleteMessage(chat.getId(), sednedID);
                }
            };
            //TODO: Изменяемое время
            timer.schedule(timerTask, 60_000);
            waitingTriggerTimers.put(chat.getId() + "/" + message.getMessageId(), timer);
        }
    }

    public int sendMessage(SendMessage sendMessage) {
        try {
            return execute(sendMessage).getMessageId();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in sending message in chat " + sendMessage.getChatId(), e);
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(long chatID, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatID);
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in sending message in chat " + chatID, e);
            throw new RuntimeException(e);
        }
    }

    public void editMessage(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in editing message in chat " + editMessageText.getChatId(), e);
            throw new RuntimeException(e);
        }
    }

    public void editMessage(long chatID, int messageID, String text, InlineKeyboardMarkup markup) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatID);
        editMessageText.setMessageId(messageID);
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(markup);
        try {
            execute(editMessageText);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in editing message in chat " + chatID, e);
            throw new RuntimeException(e);
        }
    }

    public void deleteMessage(DeleteMessage deleteMessage) {
        try {
            execute(deleteMessage);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in deleting message in chat " + deleteMessage.getChatId(), e);
            throw new RuntimeException(e);
        }
    }

    public void deleteMessage(long chatID, int messageID) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatID);
        deleteMessage.setMessageId(messageID);
        try {
            execute(deleteMessage);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in deleting message in chat " + chatID, e);
            throw new RuntimeException(e);
        }
    }

    public void ban(BanChatMember banChatMember) {
        try {
            execute(banChatMember);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in banning in chat " + banChatMember.getChatId() + " user " + banChatMember.getUserId(), e);
            throw new RuntimeException(e);
        }
    }

    public void ban(long chatID, long userID) {
        BanChatMember banChatMember = new BanChatMember();
        banChatMember.setChatId(chatID);
        banChatMember.setUserId(userID);
        try {
            execute(banChatMember);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in banning in chat " + chatID + " user " + userID, e);
            throw new RuntimeException(e);
        }
    }

    public void kick(long chatID, long userID) {
        BanChatMember banChatMember = new BanChatMember();
        UnbanChatMember unbanChatMember = new UnbanChatMember();
        banChatMember.setChatId(chatID);
        banChatMember.setUserId(userID);
        unbanChatMember.setChatId(userID);
        unbanChatMember.setUserId(userID);
        try {
            execute(banChatMember);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in banning in chat " + chatID + " user " + userID, e);
            throw new RuntimeException(e);
        }
        try {
            execute(unbanChatMember);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in unbanning in chat " + chatID + " user " + userID, e);
            throw new RuntimeException(e);
        }
    }

    public void answerCallback(AnswerCallbackQuery answerCallbackQuery) {
        try {
            execute(answerCallbackQuery);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in sending answer callback query " + answerCallbackQuery.getCallbackQueryId(), e);
            throw new RuntimeException(e);
        }
    }

    public void answerCallback(String callbackID, String text, boolean showAlert, String url) {
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setCallbackQueryId(callbackID);
        answerCallbackQuery.setText(text);
        answerCallbackQuery.setShowAlert(showAlert);
        if (url != null && !url.isEmpty()) {
            answerCallbackQuery.setUrl(url);
        }
        try {
            execute(answerCallbackQuery);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in sending answer callback query " + callbackID, e);
            throw new RuntimeException(e);
        }
    }

    public void answerCallback(String callbackID, String text, boolean showAlert) {
        answerCallback(callbackID, text, showAlert, null);
    }

    private InlineKeyboardButton createInlineKeyboardButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    public HashMap<String, Command> getCommands() {
        return commands;
    }

    public HashMap<Long, Chat> getChats() {
        return chats;
    }

    public HashMap<Long, SettingsSession> getSettingsSessions() {
        return settingsSessions;
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }

    public List<Long> getChatAdmins(long chatID) {
        List<ChatMember> chatMembers;
        List<Long> auth = new ArrayList<>();

        try {
            chatMembers = execute(new GetChatAdministrators(String.valueOf(chatID)));
            for (ChatMember chatMember : chatMembers) {
                auth.add(chatMember.getUser().getId());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return auth;
    }

    @Override
    public String getBotUsername() {
        return username;
    }
}
