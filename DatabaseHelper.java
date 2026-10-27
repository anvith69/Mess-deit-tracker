package com.example.miniproject.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.miniproject.util.PasswordUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * The whole "backend" of the app: a local SQLite database stored on the phone.
 *
 * Tables
 *   users      - registered students (password stored as salted hash)
 *   foods      - mess menu items (seeded on first run) + custom foods
 *   meal_logs  - one row each time a student logs a food (user, food, servings, date)
 *
 * Singleton: use DatabaseHelper.getInstance(context) everywhere.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "mess_diet.db";
    private static final int DB_VERSION = 1;

    private static final String USER_COLS =
            "id, name, email, reg_no, age, height_cm, weight_kg, gender, activity_level";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ------------------------------------------------------------------ schema

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true); // deleting a food removes its logs
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "email TEXT NOT NULL UNIQUE, "
                + "reg_no TEXT, "
                + "password_hash TEXT NOT NULL, "
                + "salt TEXT NOT NULL, "
                + "age INTEGER, height_cm REAL, weight_kg REAL, "
                + "gender TEXT, activity_level TEXT, "
                + "created_at INTEGER)");

        db.execSQL("CREATE TABLE foods ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "meal_type TEXT NOT NULL, "
                + "calories INTEGER NOT NULL, "
                + "protein REAL, carbs REAL, fat REAL, fibre REAL, "
                + "is_custom INTEGER NOT NULL DEFAULT 0, "
                + "owner_id INTEGER)");

        db.execSQL("CREATE TABLE meal_logs ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "food_id INTEGER NOT NULL, "
                + "meal_type TEXT NOT NULL, "
                + "servings REAL NOT NULL, "
                + "log_date TEXT NOT NULL, "
                + "logged_at INTEGER NOT NULL, "
                + "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, "
                + "FOREIGN KEY(food_id) REFERENCES foods(id) ON DELETE CASCADE)");

        db.execSQL("CREATE INDEX idx_logs_user_date ON meal_logs(user_id, log_date)");

        seedFoods(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS meal_logs");
        db.execSQL("DROP TABLE IF EXISTS foods");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    /** Default mess menu (approximate values per serving). */
    private void seedFoods(SQLiteDatabase db) {
        // name, meal, kcal, protein, carbs, fat, fibre
        seed(db, "Idli and sambar", "Breakfast", 180, 6, 32, 3, 2);
        seed(db, "Boiled eggs (2)", "Breakfast", 155, 13, 1, 11, 0);
        seed(db, "Banana", "Breakfast", 105, 1.3, 27, 0.4, 3);
        seed(db, "Filter coffee", "Breakfast", 70, 2, 9, 3, 0);
        seed(db, "Masala dosa", "Breakfast", 250, 5, 38, 8, 2);
        seed(db, "Poha", "Breakfast", 250, 5, 45, 6, 3);
        seed(db, "Upma", "Breakfast", 210, 5, 35, 6, 2);

        seed(db, "Rice and dal", "Lunch", 350, 12, 62, 5, 6);
        seed(db, "Vegetable curry", "Lunch", 150, 4, 16, 8, 4);
        seed(db, "Curd", "Lunch", 100, 5, 8, 5, 0);
        seed(db, "Seasonal fruit", "Lunch", 90, 1, 22, 0.3, 3);
        seed(db, "Chapati (2)", "Lunch", 200, 6, 36, 4, 4);
        seed(db, "Paneer curry", "Lunch", 280, 14, 9, 21, 2);
        seed(db, "Sambar rice", "Lunch", 320, 9, 56, 6, 5);

        seed(db, "Chapati and curry", "Dinner", 300, 9, 45, 9, 5);
        seed(db, "Rice", "Dinner", 250, 5, 55, 0.5, 1);
        seed(db, "Mixed vegetables", "Dinner", 120, 4, 14, 6, 5);
        seed(db, "Milk", "Dinner", 120, 6, 10, 5, 0);
        seed(db, "Egg curry", "Dinner", 220, 13, 6, 16, 2);
        seed(db, "Khichdi", "Dinner", 280, 10, 48, 5, 4);
        seed(db, "Chicken curry", "Dinner", 260, 22, 6, 16, 1);
    }

    private void seed(SQLiteDatabase db, String name, String meal, int kcal,
                      double p, double c, double f, double fi) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("meal_type", meal);
        v.put("calories", kcal);
        v.put("protein", p);
        v.put("carbs", c);
        v.put("fat", f);
        v.put("fibre", fi);
        v.put("is_custom", 0);
        db.insert("foods", null, v);
    }

    // ------------------------------------------------------------ date helpers

    /** yyyy-MM-dd key used to group logs by day. */
    public static String dateKeyDaysAgo(int daysAgo) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    public static String todayKey() {
        return dateKeyDaysAgo(0);
    }

    // ------------------------------------------------------------------- users

    public boolean emailExists(String email) {
        return exists("SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)", email);
    }

    public boolean regNoExists(String regNo) {
        return exists("SELECT 1 FROM users WHERE LOWER(reg_no) = LOWER(?)", regNo);
    }

    private boolean exists(String sql, String arg) {
        Cursor c = getReadableDatabase().rawQuery(sql, new String[]{arg});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    /**
     * Creates a new account.
     * @return new user id, -2 if the email is already registered, -3 if the registration number is taken.
     */
    public long registerUser(User u, String password) {
        if (emailExists(u.email)) return -2;
        if (u.regNo != null && !u.regNo.isEmpty() && regNoExists(u.regNo)) return -3;

        String salt = PasswordUtil.newSalt();
        ContentValues v = new ContentValues();
        v.put("name", u.name);
        v.put("email", u.email.toLowerCase(Locale.ROOT));
        if (u.regNo == null || u.regNo.trim().isEmpty()) v.putNull("reg_no");
        else v.put("reg_no", u.regNo.trim());
        v.put("password_hash", PasswordUtil.hash(password, salt));
        v.put("salt", salt);
        v.put("age", u.age);
        v.put("height_cm", u.heightCm);
        v.put("weight_kg", u.weightKg);
        v.put("gender", u.gender);
        v.put("activity_level", u.activityLevel);
        v.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("users", null, v);
    }

    /** @param identifier email OR registration number. @return the user, or null if credentials are wrong. */
    public User login(String identifier, String password) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + USER_COLS + ", password_hash, salt FROM users "
                        + "WHERE LOWER(email) = LOWER(?) OR LOWER(reg_no) = LOWER(?)",
                new String[]{identifier, identifier});
        try {
            if (c.moveToFirst() && PasswordUtil.verify(password, c.getString(10), c.getString(9))) {
                return readUser(c);
            }
            return null;
        } finally {
            c.close();
        }
    }

    public User getUser(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT " + USER_COLS + " FROM users WHERE id = ?",
                new String[]{String.valueOf(id)});
        try {
            return c.moveToFirst() ? readUser(c) : null;
        } finally {
            c.close();
        }
    }

    private User readUser(Cursor c) {
        User u = new User();
        u.id = c.getLong(0);
        u.name = c.getString(1);
        u.email = c.getString(2);
        u.regNo = c.getString(3);
        u.age = c.getInt(4);
        u.heightCm = c.getDouble(5);
        u.weightKg = c.getDouble(6);
        u.gender = c.getString(7);
        u.activityLevel = c.getString(8);
        return u;
    }

    /** Offline "forgot password": email and registration number must both match. */
    public boolean resetPassword(String email, String regNo, String newPassword) {
        String salt = PasswordUtil.newSalt();
        ContentValues v = new ContentValues();
        v.put("password_hash", PasswordUtil.hash(newPassword, salt));
        v.put("salt", salt);
        int rows = getWritableDatabase().update("users", v,
                "LOWER(email) = LOWER(?) AND reg_no IS NOT NULL AND LOWER(reg_no) = LOWER(?)",
                new String[]{email, regNo});
        return rows > 0;
    }

    // ------------------------------------------------------------------- foods

    /** Seeded mess items for the meal + custom foods this user added. */
    public List<Food> getFoods(String mealType, long userId) {
        List<Food> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, name, meal_type, calories, protein, carbs, fat, fibre, is_custom "
                        + "FROM foods WHERE meal_type = ? AND (is_custom = 0 OR owner_id = ?) "
                        + "ORDER BY is_custom, id",
                new String[]{mealType, String.valueOf(userId)});
        try {
            while (c.moveToNext()) list.add(readFood(c));
        } finally {
            c.close();
        }
        return list;
    }

    public Food getFood(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, name, meal_type, calories, protein, carbs, fat, fibre, is_custom "
                        + "FROM foods WHERE id = ?", new String[]{String.valueOf(id)});
        try {
            return c.moveToFirst() ? readFood(c) : null;
        } finally {
            c.close();
        }
    }

    private Food readFood(Cursor c) {
        Food f = new Food();
        f.id = c.getLong(0);
        f.name = c.getString(1);
        f.mealType = c.getString(2);
        f.calories = c.getInt(3);
        f.protein = c.getDouble(4);
        f.carbs = c.getDouble(5);
        f.fat = c.getDouble(6);
        f.fibre = c.getDouble(7);
        f.custom = c.getInt(8) == 1;
        return f;
    }

    public long addCustomFood(Food f, long userId) {
        ContentValues v = new ContentValues();
        v.put("name", f.name);
        v.put("meal_type", f.mealType);
        v.put("calories", f.calories);
        v.put("protein", f.protein);
        v.put("carbs", f.carbs);
        v.put("fat", f.fat);
        v.put("fibre", f.fibre);
        v.put("is_custom", 1);
        v.put("owner_id", userId);
        return getWritableDatabase().insert("foods", null, v);
    }

    /** Only the owner can delete a custom food; seeded foods are protected. */
    public boolean deleteCustomFood(long foodId, long userId) {
        return getWritableDatabase().delete("foods",
                "id = ? AND is_custom = 1 AND owner_id = ?",
                new String[]{String.valueOf(foodId), String.valueOf(userId)}) > 0;
    }

    // -------------------------------------------------------------- meal logs

    public long logMeal(long userId, long foodId, String mealType, double servings) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("food_id", foodId);
        v.put("meal_type", mealType);
        v.put("servings", servings);
        v.put("log_date", todayKey());
        v.put("logged_at", System.currentTimeMillis());
        return getWritableDatabase().insert("meal_logs", null, v);
    }

    /** How many times each food was logged today for one meal (food id -> count). */
    public Map<Long, Integer> getLoggedCounts(long userId, String mealType, String date) {
        Map<Long, Integer> counts = new HashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT food_id, COUNT(*) FROM meal_logs "
                        + "WHERE user_id = ? AND meal_type = ? AND log_date = ? GROUP BY food_id",
                new String[]{String.valueOf(userId), mealType, date});
        try {
            while (c.moveToNext()) counts.put(c.getLong(0), c.getInt(1));
        } finally {
            c.close();
        }
        return counts;
    }

    /** Undo: removes the most recent entry of this food for this meal today. */
    public boolean removeLatestLog(long userId, long foodId, String mealType) {
        return getWritableDatabase().delete("meal_logs",
                "id = (SELECT id FROM meal_logs WHERE user_id = ? AND food_id = ? "
                        + "AND meal_type = ? AND log_date = ? "
                        + "ORDER BY logged_at DESC, id DESC LIMIT 1)",
                new String[]{String.valueOf(userId), String.valueOf(foodId),
                        mealType, todayKey()}) > 0;
    }

    public boolean deleteLog(long logId, long userId) {
        return getWritableDatabase().delete("meal_logs", "id = ? AND user_id = ?",
                new String[]{String.valueOf(logId), String.valueOf(userId)}) > 0;
    }

    /** All entries of one day, oldest first (JOIN with foods to get nutrition). */
    public List<MealLog> getLogs(long userId, String date) {
        List<MealLog> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT l.id, l.food_id, l.meal_type, l.servings, l.logged_at, "
                        + "f.name, f.calories, f.protein, f.carbs, f.fat, f.fibre "
                        + "FROM meal_logs l JOIN foods f ON f.id = l.food_id "
                        + "WHERE l.user_id = ? AND l.log_date = ? ORDER BY l.logged_at",
                new String[]{String.valueOf(userId), date});
        try {
            while (c.moveToNext()) {
                MealLog m = new MealLog();
                m.id = c.getLong(0);
                m.foodId = c.getLong(1);
                m.mealType = c.getString(2);
                m.servings = c.getDouble(3);
                m.loggedAt = c.getLong(4);
                m.foodName = c.getString(5);
                m.calories = c.getInt(6) * m.servings;
                m.protein = c.getDouble(7) * m.servings;
                m.carbs = c.getDouble(8) * m.servings;
                m.fat = c.getDouble(9) * m.servings;
                m.fibre = c.getDouble(10) * m.servings;
                list.add(m);
            }
        } finally {
            c.close();
        }
        return list;
    }

    /** SUM() of every nutrient for one day, computed by SQLite. */
    public DayTotals getTotals(long userId, String date) {
        DayTotals t = new DayTotals();
        t.date = date;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(f.calories * l.servings), 0), "
                        + "COALESCE(SUM(f.protein * l.servings), 0), "
                        + "COALESCE(SUM(f.carbs * l.servings), 0), "
                        + "COALESCE(SUM(f.fat * l.servings), 0), "
                        + "COALESCE(SUM(f.fibre * l.servings), 0), "
                        + "COUNT(l.id) "
                        + "FROM meal_logs l JOIN foods f ON f.id = l.food_id "
                        + "WHERE l.user_id = ? AND l.log_date = ?",
                new String[]{String.valueOf(userId), date});
        try {
            if (c.moveToFirst()) {
                t.calories = c.getDouble(0);
                t.protein = c.getDouble(1);
                t.carbs = c.getDouble(2);
                t.fat = c.getDouble(3);
                t.fibre = c.getDouble(4);
                t.mealCount = c.getInt(5);
            }
        } finally {
            c.close();
        }
        return t;
    }

    /** Totals for the last `days` days, oldest first (used by the weekly report). */
    public List<DayTotals> getDailyTotals(long userId, int days) {
        List<DayTotals> list = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            list.add(getTotals(userId, dateKeyDaysAgo(i)));
        }
        return list;
    }

    /** Number of consecutive days (ending today or yesterday) with at least one logged meal. */
    public int getStreak(long userId) {
        Set<String> dates = new HashSet<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT log_date FROM meal_logs WHERE user_id = ?",
                new String[]{String.valueOf(userId)});
        try {
            while (c.moveToNext()) dates.add(c.getString(0));
        } finally {
            c.close();
        }
        int streak = 0;
        for (int i = 0; i < 400; i++) {
            if (dates.contains(dateKeyDaysAgo(i))) {
                streak++;
            } else if (i > 0) {
                break; // today may still be empty; any other gap ends the streak
            }
        }
        return streak;
    }
}
