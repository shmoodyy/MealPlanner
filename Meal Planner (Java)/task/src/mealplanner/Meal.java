package mealplanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;

import java.util.*;

public class Meal extends MealDatabase {
    Scanner scanner = new Scanner(System.in);
    private String mealCategory;
    private String mealName;
    private String mealIngredients;
    static boolean finished;
    static String[] daysOfTheWeek = new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
            , "Saturday", "Sunday"};

    Meal() throws SQLException {
        serverConnect();
        while (!finished) {
            actionMenu();
        }
        mealStatement.close();
        ingredientStatement.close();
        planStatement.close();
        connection.close();
        System.out.println("Bye!");
    }

    public String getMealCategory() {
        return mealCategory;
    }

    public void setMealCategory(String mealCategory) {
        this.mealCategory = mealCategory;
    }

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public String getMealIngredients() {
        return mealIngredients;
    }

    public void setMealIngredients(String mealIngredients) {
        this.mealIngredients = mealIngredients;
    }

    public void actionMenu() throws SQLException {
        boolean wrongInput;
        do {
            System.out.println("What would you like to do (add, show, plan, save, exit)?");
            String action = scanner.nextLine().toLowerCase();
            System.out.println();
            wrongInput = false;
            switch (action) {
                case "add"  -> addMeal();
                case "show" -> showMeal();
                case "plan" -> planMeal();
                case "save" -> {
                    ResultSet rsP = planStatement.executeQuery("SELECT COUNT(*) as total FROM plan;");
                    rsP.next();
                    if (rsP.getInt("total") == 21) {
                        saveMeal();
                    } else {
                        System.out.println("Unable to save. Plan your meals first.");
                        wrongInput = true;
                    }
                }
                case "exit" -> finished = true;
                default     -> wrongInput = true;
            }
        } while (wrongInput);
    }

    public void saveMeal() throws SQLException {
        System.out.println("Input a filename:");
        String filename = scanner.nextLine();
        File file = new File(filename);
        Map<String, Integer> ingredientMap = new HashMap<>();
        int ingredientCount;
        try (FileWriter writer = new FileWriter(filename)) {
            boolean createdNew = file.createNewFile();
            ResultSet rsP = planStatement.executeQuery("SELECT * FROM plan;");
            while (rsP.next()) {
                int mealMealID = rsP.getInt("meal_id");
                ResultSet rsI = ingredientStatement.executeQuery("SELECT * FROM ingredients " +
                        "WHERE meal_id = " + mealMealID + ";");
                while (rsI.next()) {
                    String currentIngredient = rsI.getString("ingredient");
                    ingredientCount = ingredientMap.get(currentIngredient) != null
                            ? ingredientMap.get(currentIngredient) + 1 : 1;
                    ingredientMap.put(currentIngredient, ingredientCount);
                }
            }

            for (var entry : ingredientMap.entrySet()) {
                int count = entry.getValue();
                writer.write(String.format("%s%s%n", entry.getKey()
                        , ((count > 1) ? " x" + count : "")));
            }

            System.out.println("Saved!");
        } catch (IOException e) {
            System.out.println("Cannot create the file: " + file.getPath());
        }
    }

    public void addMeal() throws SQLException {
        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        addMealCategory();
        System.out.println("Input the meal's name:");
        addMealName();
        System.out.println("Input the ingredients:");
        addMealIngredients();

        ResultSet rsM = mealStatement.executeQuery("SELECT MAX(meal_id) AS latest_meal_id FROM meals;");
        rsM.next();
        int currentMealID = rsM.getInt("latest_meal_id") + 1;
        mealStatement.executeUpdate("INSERT INTO meals (category, meal, meal_id) " +
                "values ('" + getMealCategory() + "', '" + getMealName() + "', " + currentMealID + ");");


        String[] ingredientsArray = getMealIngredients().split(",\\s*");
        for (String element : ingredientsArray) {
            ResultSet rsI = ingredientStatement.executeQuery("SELECT MAX(ingredient_id) " +
                    "AS latest_ingredient_id FROM ingredients;");
            rsI.next();
            int currentIngredientID = rsI.getInt("latest_ingredient_id") + 1;
            ingredientStatement.executeUpdate("INSERT INTO ingredients (ingredient, ingredient_id, meal_id) " +
                    "values ('" + element + "', " + currentIngredientID + ", " + currentMealID + ");");
        }
        System.out.println("The meal has been added!");
    }

    public void addMealCategory() {
        String category = scanner.nextLine().toLowerCase();
        while (!category.matches("\\s*(breakfast|lunch|dinner)\\s*")) {
            System.out.println("Wrong meal category! Choose FROM: breakfast, lunch, dinner.");
            category = scanner.nextLine().trim();
        }
        setMealCategory(category);
    }

    public void addMealName() {
        String name = scanner.nextLine();
        while (!name.matches("\\s*[A-Za-z]+[A-Za-z\\s]*\\s*")) {
            System.out.println("Wrong format. Use letters only!");
            name = scanner.nextLine();
        }
        setMealName(name);
    }

    public void addMealIngredients() {
        String ingredients = scanner.nextLine();
        while (!ingredients.matches("^[a-zA-Z]+(\\s+[a-zA-Z]+)*\\s*(,\\s*[a-zA-Z]+(\\s+[a-zA-Z]+)*)*$")) {
            System.out.println("Wrong format. Use letters only!");
            ingredients = scanner.nextLine();
        }
        setMealIngredients(ingredients);
    }

    public void showMeal() throws SQLException {
        System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
        setMealCategory(scanner.nextLine().toLowerCase().trim());
        while (!getMealCategory().matches("\\s*(breakfast|lunch|dinner)\\s*")) {
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            setMealCategory(scanner.nextLine().toLowerCase().trim());
        }
        toShow(getMealCategory());
    }
    
    public void toShow(String category) throws SQLException {
        ResultSet rsMeals = mealStatement.executeQuery(String.format("SELECT * FROM meals WHERE category = '%s';"
                , category));
        if (rsMeals.next()) {
            System.out.println("Category: " + category + "\n");
            do {
                ResultSet rsIngredients = ingredientStatement.executeQuery(String.format("SELECT * FROM ingredients" +
                        " WHERE meal_id = %d;", rsMeals.getInt("meal_id")));
                System.out.printf("""
                         Name: %s
                         Ingredients:
                         """, rsMeals.getString("meal"));
                if (rsIngredients.next()) {
                    do {
                        if (rsIngredients.getInt("meal_id") == rsMeals.getInt("meal_id")) {
                            System.out.println(rsIngredients.getString("ingredient"));
                        } else break;
                    } while (rsIngredients.next());
                }
                System.out.println();
            } while (rsMeals.next());
        } else {
            System.out.println("No meals found.");
        }
    }

    public void planMeal() throws SQLException {
        createPlanDB();
        List<String> mealList = new ArrayList<>();
        for (String day : daysOfTheWeek) {
            System.out.println(day);
            ResultSet rsBreakfast = mealStatement.executeQuery("SELECT meal, meal_id FROM meals " +
                    "WHERE category = 'breakfast' ORDER BY meal ASC;");
            toPlan(day, mealList, rsBreakfast, "breakfast");
            ResultSet rsLunch = mealStatement.executeQuery("SELECT meal, meal_id FROM meals " +
                    "WHERE category = 'lunch' ORDER BY meal ASC;");
            toPlan(day, mealList, rsLunch, "lunch");
            ResultSet rsDinner = mealStatement.executeQuery("SELECT meal, meal_id FROM meals " +
                    "WHERE category = 'dinner' ORDER BY meal ASC;");
            toPlan(day, mealList, rsDinner, "dinner");
            System.out.println("Yeah! We planned the meals for " + day +  ".\n");
        }

        ResultSet rsPlan = planStatement.executeQuery("SELECT meal FROM plan");
        for (String day : daysOfTheWeek) {
            System.out.println(day);
            rsPlan.next();
            System.out.println("Breakfast: " + rsPlan.getString("meal"));
            rsPlan.next();
            System.out.println("Lunch: " + rsPlan.getString("meal"));
            rsPlan.next();
            System.out.println("Dinner: " + rsPlan.getString("meal") + "\n");
        }
    }

    public void createPlanDB() throws SQLException {
        planStatement.executeUpdate("DROP TABLE IF EXISTS plan;");
        planStatement.executeUpdate("CREATE TABLE plan (" +
                "category VARCHAR(1024) NOT NULL," +
                "meal VARCHAR(1024) NOT NULL," +
                "meal_id INTEGER NOT NULL" +
                ");");
    }

    public void toPlan (String day, List<String> list, ResultSet rs, String category) throws SQLException {
        while (rs.next()) {
            String mealOption = rs.getString("meal");
            list.add(mealOption);
            System.out.println(mealOption);
        }
        System.out.println("Choose the " + category + " for " + day + " from the list above:");
        String mealChoice = scanner.nextLine();
        while (!list.contains(mealChoice)) {
            System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
            mealChoice = scanner.nextLine();
        }
        ResultSet rsMID = mealStatement.executeQuery("SELECT meal_id AS specific_meal_id FROM meals " +
                "WHERE meal = '" + mealChoice +  "';");
        rsMID.next();
        int specificMealID = rsMID.getInt("specific_meal_id");
        planStatement.executeUpdate("INSERT INTO plan (category, meal, meal_id) " +
                "values ('" + category + "', '" + mealChoice + "', " + specificMealID + ");");
    }
}