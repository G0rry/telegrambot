package org.telegram.service;

import org.telegram.BotSettings;
import org.telegram.model.City;
import org.telegram.database.DatabaseManager;
import org.telegram.model.Distance;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.model.PriceIncrease;

import java.io.IOException;



/**
 * Created by gorun_pv on 17.02.2017.
 */
public class Parser {

    private static final String LOGTAG = "PARSER";
    private static final String MARATHON = "42 км";
    private static final String HALFMARATHON = "21 км";
    private static final String TEN = "10 км";

    public static void parseCities(){
        Document doc = null;
        Document prInc= null;
        try {
            doc = Jsoup.connect(BotSettings.URL).get();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Elements links = doc.select("a[class^=square-wrap calendar-item]");

        for (Element link : links) {
            City city = new City()
                    .setCity(link.text())
                    .setLink(link.attr("abs:href"));
            int idCity = DatabaseManager.getInstance().setCity(city);

            try {
                prInc= Jsoup.connect(link.attr("abs:href")).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements distancesAttr = prInc.select("div.package-cost");
            for (Element distanceAttr : distancesAttr) {
                Element dist = distanceAttr.select("p.subtitle").first();

                switch (dist.text()){
                    case MARATHON:
                    case HALFMARATHON:
                    case TEN:
                        Elements costsAttr = distanceAttr.select("div.costs div.costs-wrap");

                        for (Element costAttr : costsAttr) {
                            Element cost = costAttr.select("span.cost").first();
                            Element date = costAttr.select("span.date").first();
                            PriceIncrease increase = new PriceIncrease();
                            increase.setDateStr(date.text());
                            increase.setPrice(cost.text());
                            DatabaseManager.getInstance().setPriceIncrease(idCity,increase);
                        }

                        break;
                    default:
                        break;
                }
            }
        }
    }

    public static void parseDistances(){

        Document doc = null;
        try {
            doc = Jsoup.connect(BotSettings.URL).get();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        Elements links = doc.select("a[class^=square-wrap calendar-item]");

        for (Element link : links) {

            try {
                doc = Jsoup.connect(link.attr("abs:href")).get();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Element registerLink = doc.select("a[class*=register]").first();

            Document registration = null;
            try {
                registration = Jsoup.connect(registerLink.attr("abs:href")).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements regAttr = registration.select("div.registration div.info");
            for (Element reg : regAttr) {
                Elements ds = reg.select("p");
                switch (ds.text()){
                    case MARATHON:
                    case HALFMARATHON:
                    case TEN:
                        Elements row = reg.select("div.data-row");
                        Element valuePlace = row.select("div.data-value").first();
                        Element valuePrice = row.select("div.data-value").last();
                        City city = DatabaseManager.getInstance().getCityByName(link.text());
                        Distance distance = new Distance()
                                .setDistance(ds.text())
                                .setPlaces(valuePlace.text())
                                .setPrice(valuePrice.text());

                        DatabaseManager.getInstance().setDistances(city.getId(),distance);

                        break;
                    default:
                        break;
                }

            }
        }
    }
}
