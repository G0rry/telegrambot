package org.telegram.database;

import java.sql.*;

/**
 * Created by pgorun on 14.02.2017.
 */
public class ConnectionDB {
    private Connection currentConnection;
    private static final String DB_DRIVER = "org.h2.Driver";
    private static final String DB_CONNECTION = "jdbc:h2:~/test";
    private static final String DB_USER = "asd";
    private static final String DB_PASSWORD = "qaz";
    
    public ConnectionDB() {
        this.currentConnection = openConection();
    }

    public Connection openConection() {
        Connection connection = null;
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return  connection;
    }

    public void closeConnection() {
        try{
            this.currentConnection.close();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public ResultSet runSqlQuery (String query) throws SQLException{
        final Statement statement;
        statement = this.currentConnection.createStatement();
        return statement.executeQuery(query);
    }

    public PreparedStatement getPreparedStatement(String query) throws SQLException{
        return this.currentConnection.prepareStatement(query);
    }

    public PreparedStatement getPreparedStatement(String query, int flag) throws SQLException{
        return this.currentConnection.prepareStatement(query, flag);
    }

    public void initTransaction() throws SQLException {
        this.currentConnection.setAutoCommit(false);
    }

    public void commitTransaction() throws SQLException{
        try{
            this.currentConnection.commit();
        } catch (SQLException e){
            if (this.currentConnection != null){
                this.currentConnection.rollback();
            }
        } finally {
            this.currentConnection.setAutoCommit(false);
        }
    }


}
