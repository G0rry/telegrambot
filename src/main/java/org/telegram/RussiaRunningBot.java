package org.telegram;

import org.telegram.database.DatabaseManager;
import org.telegram.model.City;
import org.telegram.model.Distance;

import org.telegram.model.PriceIncrease;
import org.telegram.service.CustomTimerTask;
import org.telegram.service.TimerExecutor;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.logging.BotLogger;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gorun_pv on 17.02.2017.
 */
public class RussiaRunningBot extends TelegramLongPollingBot {

    private static final String LOGTAG = "RRBot";

    public RussiaRunningBot() {
        super();
        startExecutor();
    }

    private void startExecutor() {
        TimerExecutor.getInstance().startExecutionEveryHour(new CustomTimerTask("Update DB", -1) {
            @Override
            public void execute() {
                updateDB();
            }
        });
    }

    private void updateDB() {
        synchronized (Thread.currentThread()){
            try {
                Thread.currentThread().wait(35);
            } catch (InterruptedException e) {
                BotLogger.severe(LOGTAG, e );
            }
        }
        DatabaseManager.getInstance().init();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){

            try{
                String user_first_name = update.getMessage().getChat().getFirstName();
                String user_last_name = update.getMessage().getChat().getLastName();
                String user_username = update.getMessage().getChat().getUserName();
                StringBuilder sb = new StringBuilder();
                sb.append("["+user_first_name +"][");
                sb.append("["+user_last_name +"][");
                sb.append("["+user_username +"][");
                sb.append("["+update.getMessage().getText() +"]");

                BotLogger.info(LOGTAG, new String(sb.toString().getBytes("UTF-8")));
            } catch (Exception e){
                BotLogger.error(LOGTAG, e );
            }

            long chatId = update.getMessage().getChatId();

            switch (update.getMessage().getText()){
                case BotSettings.START : sendWelcomMessageWithKeyboard(chatId);
                    break;
                case BotSettings.GET_LIST : sendRaceList(chatId);
                    break;
                case BotSettings.GET_EXTENDED_LIST : sendExtendedList(chatId);
                    break;
                default: break;
            }

        }else if(update.hasCallbackQuery()){

            String call_data = update.getCallbackQuery().getData();
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            List<City> cities = DatabaseManager.getInstance().getCities();
            BotLogger.info(LOGTAG, call_data);
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            StringBuilder answer = getExtendedList();
            int count= 1;
            for(City city : cities){
                if (call_data.equals(String.valueOf(city.getId()))) {
                    answer.append("\n <a href=\"" + city.getLink()+"\" >"+city.getCity()+"</a> \n");
                    answer.append("\n На: <b>" + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(city.getDateUpdate())+"</b> \n");
                    List<Distance> distances = city.getDistances();
                    for(Distance distance : distances){
                        answer.append("<b>" + distance.getDistance()+"</b>");
                        answer.append(" - <b>" + distance.getPlaces()+"</b> ");
                        if ( ! distance.getChange().equals("0")  ){
                            answer.append("(<b>-" + distance.getChange() + "</b>)");
                        }
                        List<PriceIncrease> prIncreases = distance.getPriceIncreases();
                        for(PriceIncrease pr : prIncreases){
                            answer.append(" <b>" + pr.getPrice()+"</b>");
                            answer.append(" <b>" + pr.getDateStr()+"</b> ");
                        }
                        answer.append(" \n ");
                    }

                }
                if (count==6){
                    rowsInline.add(rowInline);
                    rowInline = new ArrayList<>();
                }
                rowInline.add(new InlineKeyboardButton().setText(String.valueOf(count))
                        .setCallbackData(String.valueOf(city.getId())));
                count++;
            }
            rowsInline.add(rowInline);
            inlineKeyboardMarkup.setKeyboard(rowsInline);
            EditMessageText new_message = new EditMessageText()
                    .setChatId(chat_id)
                    .setParseMode("HTML")
                    .setMessageId((int) message_id)
                    .setText(answer.toString())
                    .setReplyMarkup(inlineKeyboardMarkup);
            try {
                editMessageText(new_message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendExtendedList(long chatId) {
        List<City> cities = DatabaseManager.getInstance().getCities();
        StringBuilder sb = new StringBuilder();
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        sb.append("Список городов выберите номер: \n");
        int count = 1;
        for(City city : cities){
            sb.append("<b>"+count+"</b> "+city.getCity()+"\n");
            if (count==6){
                rowsInline.add(rowInline);
                rowInline = new ArrayList<>();
            }
            rowInline.add(new InlineKeyboardButton().setText(String.valueOf(count))
                    .setCallbackData(String.valueOf(city.getId())));
            count++;}

        rowsInline.add(rowInline);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        sendMsg(chatId, sb.toString(), inlineKeyboardMarkup);
    }

    private void sendRaceList(long chatId) {
        List<City> cities = DatabaseManager.getInstance().getCities();
        StringBuilder sb = new StringBuilder();
        sb.append("\n На: <b>" + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(cities.get(0).getDateUpdate())+"</b>");
        for(City city : cities){

            sb.append("\n <a href=\"" + city.getLink()+"\" >"+city.getCity()+"</a> \n");
            List<Distance> distances = city.getDistances();
            for(Distance distance : distances){
                sb.append("<b>" + distance.getDistance()+"</b>");
                sb.append(" - <b>" + distance.getPlaces()+"</b> ");
                if ( ! distance.getChange().equals("0")  ){
                    sb.append("(<b>-" + distance.getChange() + "</b>)");
                }
                sb.append(" - <b>" + distance.getPrice()+"</b> \n");
            }
        }
        sendMsg(chatId, sb.toString());
    }

    private void sendMsg(long chatId, String text) {
        SendMessage message =
                new SendMessage()
                        .setChatId(chatId)
                        .setParseMode("HTML")
                        .setText(text);
        getKeyboard(message);
        try {
            sendMessage(message);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void sendMsg(long chatId, String text, InlineKeyboardMarkup markupInline) {
        SendMessage message =
                new SendMessage()
                        .setChatId(chatId)
                        .setParseMode("HTML")
                        .setText(text);
        message.setReplyMarkup(markupInline);
        try {
            sendMessage(message);
        } catch (TelegramApiException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    private void sendWelcomMessageWithKeyboard(long chatId) {
        sendMsg(chatId, BotSettings.START_MESSAGE);
    }

    private void getKeyboard(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(BotSettings.GET_LIST);
        keyboard.add(row);
        row = new KeyboardRow();
        row.add(BotSettings.GET_EXTENDED_LIST);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);
    }

    @Override
    public String getBotUsername() {
        return BotSettings.BOTNAME;
    }

    @Override
    public String getBotToken() {
        return BotSettings.BOTTOKEN;
    }

    public StringBuilder getExtendedList() {
        List<City> cities = DatabaseManager.getInstance().getCities();
        StringBuilder sb = new StringBuilder();
        sb.append("Список городов выберите номер: \n");
        int count = 1;
        for(City city : cities){
            sb.append("<b>"+count+"</b> "+city.getCity()+"\n");
            count++;
        }
        return sb;
    }
}
