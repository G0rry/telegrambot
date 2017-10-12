package org.telegram.database;


import org.telegram.model.City;
import org.telegram.model.Distance;
import org.telegram.model.PriceIncrease;
import org.telegram.service.Parser;
import org.telegram.telegrambots.logging.BotLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gorun_pv on 15.02.2017.
 */
public class DatabaseManager {

    private static String LOGTAG = "DBLog";

    private static volatile DatabaseManager instance;
    private static volatile ConnectionDB connection;

    private DatabaseManager(){
        connection = new ConnectionDB();
    }

    public static class DatabaseManagerHolder {
        public static final DatabaseManager HOLDER_INSTANCE = new DatabaseManager();
    }

    public static DatabaseManager getInstance(){
        return  DatabaseManagerHolder.HOLDER_INSTANCE;
    }

    public void init(){
//       PreparedStatement preparedStatement;
//        try {
//            connection.initTransaction();
//            preparedStatement = connection.getPreparedStatement("DELETE CITIES");
//            preparedStatement.executeUpdate();
//            connection.commitTransaction();
//
//        } catch (SQLException e) {
//            BotLogger.error(LOGTAG, e);
//        }
        BotLogger.info(LOGTAG, "Parse data from RussiaRunning");
        Parser parser = new Parser();
        parser.parseCities();
        parser.parseDistances();

    }

    public int setCity (City city){

            City oldCity = DatabaseManager.getInstance().getCityByName(city.getCity());
            if (oldCity != null) {
                try {
                    connection.initTransaction();
                    final PreparedStatement preparedStatement =
                            connection.getPreparedStatement("UPDATE CITIES SET  DATE_UPDATE = CURRENT_TIMESTAMP() WHERE CITY = ?");
                    preparedStatement.setString(1, city.getCity());
                    preparedStatement.executeUpdate();
                    connection.commitTransaction();
                } catch (SQLException e) {
                    BotLogger.error(LOGTAG, e);
                }
                return oldCity.getId();
            }else{
                try {
                    connection.initTransaction();
                    final PreparedStatement preparedStatement =
                            connection.getPreparedStatement("INSERT INTO CITIES(CITY,LINK, DATE_UPDATE) VALUES (?,?, CURRENT_TIMESTAMP())",
                                    Statement.RETURN_GENERATED_KEYS);
                    preparedStatement.setString(1, city.getCity());
                    preparedStatement.setString(2, city.getLink());

                    int affectedRows = preparedStatement.executeUpdate();
                    connection.commitTransaction();
                    if (affectedRows == 0)
                        throw new SQLException("Creating city failed, no rows affected");
                    ResultSet returnedId = preparedStatement.getGeneratedKeys();
                    if (returnedId.next()){
                        return (int) returnedId.getLong(1);
                    }
                } catch (SQLException e) {
                    BotLogger.error(LOGTAG, e);
                }
                return 0;
            }
    }


    public List<City> getCities(){
        try {
            final PreparedStatement preparedStatement =
                    connection.getPreparedStatement("SELECT ID, CITY, LINK, DATE_UPDATE FROM CITIES");
            final ResultSet result = preparedStatement.executeQuery();
            if (!result.wasNull()){
                List<City> cities = new ArrayList<City>();
                while (result.next()) {
                    City city = new City();
                    city.setId(result.getInt("ID"));
                    city.setCity(result.getString("CITY"));
                    city.setLink(result.getString("LINK"));
                    city.setDateUpdate(result.getTimestamp("DATE_UPDATE"));

                    city.setDistances(
                            DatabaseManager.getInstance()
                                    .getDistancesByCity(city.getId()));
                    cities.add(city);

                }
                return cities;
            }
        } catch (SQLException e) {
            BotLogger.error(LOGTAG, e);
        }
        return null;
    }

    public int setDistances (int cityId, Distance distance){
        setPreviousDistanceDisable(cityId, distance.getDistance());
        try {
            connection.initTransaction();
            final PreparedStatement preparedStatement =
                    connection.getPreparedStatement("INSERT INTO DISTANCES(ID_CITY, DISTANCE, PLACES, PRICE, DATE_UPDATE) " +
                                    "VALUES (?,?,?,?,CURRENT_TIMESTAMP())",
                            Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, cityId );
            preparedStatement.setString(2, distance.getDistance());
            preparedStatement.setString(3, distance.getPlaces());
            preparedStatement.setString(4, distance.getPrice());

            int affectedRows = preparedStatement.executeUpdate();
            connection.commitTransaction();
            if (affectedRows == 0)
                throw new SQLException("Creating distance failed, no rows affected");
            ResultSet returnedId = preparedStatement.getGeneratedKeys();
            if (returnedId.next()){
                return (int) returnedId.getLong(1);
            }
        } catch (SQLException e) {
            BotLogger.error(LOGTAG, e);
        }
        return 0;
    }

    private void setPreviousDistanceDisable(int cityId, String distance) {
        try {
            connection.initTransaction();
            final PreparedStatement preparedStatement =
                    connection.getPreparedStatement("UPDATE DISTANCES " +
                                    "SET LAST_UPDATE = 0 WHERE ID_CITY= ? AND DISTANCE = ?");
             preparedStatement.setInt(1, cityId );
            preparedStatement.setString(2, distance);
             preparedStatement.executeUpdate();
            connection.commitTransaction();
        } catch (SQLException e) {
            BotLogger.error(LOGTAG, e);
        }
    }

    public List<Distance> getDistancesByCity(int cityId){
        try {
            final PreparedStatement preparedStatement =
                    connection.getPreparedStatement("SELECT D.ID, D.DISTANCE, D.PLACES, D.PRICE, D.DATE_UPDATE, f.max, f.min\n" +
                            "FROM DISTANCES D,\n" +
                            "(SELECT DISTANCE, MAX(PLACES) as max , min(PLACES) as min\n" +
                            " FROM DISTANCES\n" +
                            "  WHERE ID_CITY = ?\n" +
                            "   AND DATE_UPDATE BETWEEN  current_date() AND current_timestamp() \n" +
                            "   GROUP BY DISTANCE) f\n" +
                            "WHERE D.ID_CITY = ?\n" +
                            "AND D.DISTANCE = F.DISTANCE\n" +
                            "AND LAST_UPDATE = 1");


            preparedStatement.setInt(1, cityId);
            preparedStatement.setInt(2, cityId);
            final ResultSet result = preparedStatement.executeQuery();
            if (!result.wasNull()){
                List<Distance> distances = new ArrayList<Distance>();
                while (result.next()) {
                    Distance distance = new Distance();
                    distance.setId(result.getInt("ID"));
                    distance.setDistance(result.getString("DISTANCE"));
                    distance.setPlaces(result.getString("PLACES"));
                    distance.setPrice(result.getString("PRICE"));
                    distance.setDateUpdate(result.getTimestamp("DATE_UPDATE"));
                    distance.setChange(String.valueOf(result.getInt("MAX") - result.getInt("MIN")));
                    distance.setPriceIncreases(getPriceIncreaseByDistance(distance.getId()));
                    distances.add(distance);
                }
                return distances;
            }
        } catch (SQLException e) {
            BotLogger.error(LOGTAG, e);
        }
        return null;
    }

    public int setPriceIncrease (int distanceId, PriceIncrease priceIncrease){
        try {
            final PreparedStatement preparedStatement =
                    connection.getPreparedStatement("INSERT INTO PRICE_INCREASE(ID_DISTANCE, DATE_STR, PRICE) " +
                                    "VALUES (?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setInt(1, distanceId );
            preparedStatement.setString(2, priceIncrease.getDateStr());
            preparedStatement.setString(3, priceIncrease.getPrice());

            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0)
                throw new SQLException("Creating PRICE_INCREASE failed, no rows affected");
            ResultSet returnedId = preparedStatement.getGeneratedKeys();
            if (returnedId.next()){
                System.out.println(returnedId.getLong(1));
                return (int) returnedId.getLong(1);
            }
        } catch (SQLException e) {
            BotLogger.error(LOGTAG, e);
        }
        return 0;
    }

    public List<PriceIncrease> getPriceIncreaseByDistance(int distanceId){
        try {
            final PreparedStatement preparedStatement =
                    connection.getPreparedStatement("SELECT ID, DATE_STR, PRICE " +
                            "FROM PRICE_INCREASE WHERE ID_DISTANCE = ?");
            preparedStatement.setInt(1, distanceId);
            final ResultSet result = preparedStatement.executeQuery();
            if (!result.wasNull()){
                List<PriceIncrease> priceIncreases = new ArrayList<PriceIncrease>();
                while (result.next()) {
                    PriceIncrease priceIncrease = new PriceIncrease();
                    priceIncrease.setId(result.getInt("ID"));
                    priceIncrease.setDistanceId(distanceId);
                    priceIncrease.setDateStr(result.getString("DATE_STR"));
                    priceIncrease.setPrice(result.getString("PRICE"));
                    priceIncreases.add(priceIncrease);
                }
                return priceIncreases;
            }
        } catch (SQLException e) {
            BotLogger.error(LOGTAG, e);
        }
        return null;
    }

    public City getCityByName(String cityName) {
        final PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.getPreparedStatement("SELECT ID, CITY, LINK, DATE_UPDATE FROM CITIES WHERE CITY = ?");
            preparedStatement.setString(1, cityName);
            final ResultSet result = preparedStatement.executeQuery();
            if (result.next()){
                City city = new City();
                city.setId(result.getInt("ID"));
                city.setCity(result.getString("CITY"));
                city.setLink(result.getString("LINK"));
                city.setDateUpdate(result.getTimestamp("DATE_UPDATE"));
                return city;
            }
        } catch (SQLException e) {
            BotLogger.error(LOGTAG, e);
        }
        return null;
    }
}
