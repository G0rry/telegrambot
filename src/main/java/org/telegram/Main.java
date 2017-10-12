package org.telegram;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.logging.BotLogger;
import org.telegram.telegrambots.logging.BotsFileHandler;

import java.io.IOException;
import java.util.logging.Level;


public class Main {
    private static final String LOGTAG = "MAIN";

    public static void main(String[] args) {
        BotLogger.setLevel(Level.ALL);
        try {
            BotLogger.registerLogger(new BotsFileHandler());
        } catch (IOException e) {
            e.printStackTrace();
        }

        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot( new RussiaRunningBot());
        } catch (TelegramApiRequestException e) {
            BotLogger.error(LOGTAG, e);
        }
        BotLogger.info(LOGTAG,"LoggingTestBot successfully started!");

    }

}
