package mealplanner;

import java.sql.*;

public class MealDatabase {
    final static String DB_URL = "jdbc:postgresql:meals_db";
    final static String USER = "postgres";
    final static String PASS = "1111";
    static Connection connection;
    static Statement mealStatement, ingredientStatement, planStatement;

    public static void serverConnect() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(true);
        mealStatement = connection.createStatement();
        ingredientStatement = connection.createStatement();
        planStatement = connection.createStatement();

        mealStatement.executeUpdate("CREATE TABLE IF NOT EXISTS meals (" +
                "category VARCHAR(1024) NOT NULL," +
                "meal VARCHAR(1024) NOT NULL," +
                "meal_id INTEGER NOT NULL" +
                ");");

        ingredientStatement.executeUpdate("CREATE TABLE IF NOT EXISTS ingredients (" +
                "ingredient VARCHAR(1024) NOT NULL," +
                "ingredient_id INTEGER NOT NULL," +
                "meal_id INTEGER NOT NULL" +
                ");");

        planStatement.executeUpdate("CREATE TABLE IF NOT EXISTS plan (" +
                "category VARCHAR(1024) NOT NULL," +
                "meal VARCHAR(1024) NOT NULL," +
                "meal_id INTEGER NOT NULL" +
                ");");
    }
}