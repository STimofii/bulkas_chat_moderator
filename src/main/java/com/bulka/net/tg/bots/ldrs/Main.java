package com.bulka.net.tg.bots.ldrs;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static Bot bot;
    public static long ADVANCED_ID_CHECK = 7000000000L;
    public static void main(String[] args) throws Exception{
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Starting!");
        Properties properties = new Properties();
        try {
            logger.config("Loading bot.properties");
            properties.load(Files.newInputStream(Paths.get("bot.properties")));
            logger.config("Loaded bot.properties file");
            if(properties.get("username") == null || properties.get("token") == null) {
                throw new IOException("bot.properties must contains username and token");
            }
            ADVANCED_ID_CHECK = Long.parseLong(properties.getProperty("check_id", String.valueOf(ADVANCED_ID_CHECK)));
        } catch (Exception e){
            logger.log(Level.SEVERE, "!!! CAN`T LOAD bot.properties!!! \nIT MUST BE IN THE ROOT DIR AND CONTAINS \nusername=r4ijefnsdvi \ntoken=3449895:jnjvdnwsjcbw", e);
            System.exit(-1);
        }
        logger.config("Successful loaded bot.properties file and checked values");

        logger.config("Starting bot");
        try {
            bot = new Bot(new DefaultBotOptions(), properties.getProperty("token"), properties.getProperty("username"));
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            logger.config("Got botsAPI");
            logger.config("Registration bot");
            botsApi.registerBot(bot);
            logger.config("Successful Registered bot");
            bot.postInit();
            logger.config("Successful PostInit bot");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "!!! CAN`T REGISTER BOT!!!", e);
            throw e;
        }
    }

    public static Bot getBot() {
        return bot;
    }
}