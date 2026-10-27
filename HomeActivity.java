package com.example.miniproject;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.miniproject.data.DatabaseHelper;
import com.example.miniproject.data.DayTotals;
import com.example.miniproject.data.MealLog;
import com.example.miniproject.data.User;
import com.example.miniproject.util.NutritionCalc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Dashboard. Everything on it is read from SQLite every time the screen is shown. */
public class HomeActivity extends BaseActivity {

    private TextView dateText, greetingText, streakText;
    private TextView caloriesText, caloriesSub, proteinText, statusText, emptyMealsText;
    private ProgressBar caloriesProgress, proteinProgress;
    private LinearLayout todayMealsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (userId < 0) return; // BaseActivity already redirected to login

        setContentView(R.layout.activity_home);

        dateText = findViewById(R.id.dateText);
        greetingText = findViewById(R.id.greetingText);
        streakText = findViewById(R.id.streakText);
        caloriesText = findViewById(R.id.caloriesText);
        caloriesSub = findViewById(R.id.caloriesSub);
        caloriesProgress = findViewById(R.id.caloriesProgress);
        proteinText = findViewById(R.id.proteinText);
        proteinProgress = findViewById(R.id.proteinProgress);
        statusText = findViewById(R.id.statusText);
        todayMealsContainer = findViewById(R.id.todayMealsContainer);
        emptyMealsText = findViewById(R.id.emptyMealsText);

        findViewById(R.id.viewMenuButton).setOnClickListener(v ->
                startActivity(new Intent(this, MessMenuActivity.class)));
        findViewById(R.id.trackerButton).setOnClickListener(v ->
                startActivity(new Intent(this, DietTrackerActivity.class)));
        findViewById(R.id.weeklyReportButton).setOnClickListener(v ->
                startActivity(new Intent(this, WeeklyReportActivity.class)));
        findViewById(R.id.guideButton).setOnClickListener(v ->
                startActivity(new Intent(this, NutritionGuideActivity.class)));
        findViewById(R.id.logoutText).setOnClickListener(v -> {
            session.clear();
            goToLogin();
        });
    }

    /** onResume runs every time we come back (e.g. after logging a meal) so the numbers stay fresh. */
    @Override
    protected void onResume() {
        super.onResume();
        if (userId >= 0 && todayMealsContainer != null) refresh();
    }

    private void refresh() {
        User user = db.getUser(userId);
        if (user == null) { // account vanished (e.g. app data cleared)
            session.clear();
            goToLogin();
            return;
        }

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        greetingText.setText(greet + ", " + user.firstName());
        dateText.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()));

        int streak = db.getStreak(userId);
        streakText.setText(streak == 0 ? "Log a meal today to start your streak"
                : streak + "-day logging streak");

        int calGoal = NutritionCalc.dailyCalorieGoal(user);
        int proGoal = NutritionCalc.dailyProteinGoal(user);
        String today = DatabaseHelper.todayKey();
        DayTotals totals = db.getTotals(userId, today);

        caloriesText.setText(NutritionCalc.kcal(totals.calories) + " / " + NutritionCalc.kcal(calGoal));
        caloriesProgress.setProgress(percent(totals.calories, calGoal));
        int amber = ContextCompat.getColor(this, R.color.warn_amber);
        int green = ContextCompat.getColor(this, R.color.brand_green);
        caloriesProgress.setProgressTintList(ColorStateList.valueOf(
                totals.calories > calGoal * 1.10 ? amber : green));
        double remaining = calGoal - totals.calories;
        caloriesSub.setText(remaining >= 0
                ? NutritionCalc.kcal(remaining) + " remaining"
                : NutritionCalc.kcal(-remaining) + " over your goal");

        proteinText.setText(NutritionCalc.grams(totals.protein) + " / " + proGoal + " g");
        proteinProgress.setProgress(percent(totals.protein, proGoal));

        statusText.setText(NutritionCalc.statusMessage(totals.calories, calGoal, hour));

        // Today's meals list
        todayMealsContainer.removeAllViews();
        List<MealLog> logs = db.getLogs(userId, today);
        emptyMealsText.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (MealLog log : logs) {
            View row = inflater.inflate(R.layout.item_log, todayMealsContainer, false);
            ((TextView) row.findViewById(R.id.logTitle)).setText(log.foodName);
            ((TextView) row.findViewById(R.id.logSub)).setText(
                    log.mealType + "  ·  " + servingsLabel(log.servings));
            ((TextView) row.findViewById(R.id.logKcal)).setText(NutritionCalc.kcal(log.calories));
            row.findViewById(R.id.logDelete).setVisibility(View.GONE);
            todayMealsContainer.addView(row);
        }
    }

    private int percent(double value, double goal) {
        if (goal <= 0) return 0;
        return (int) Math.min(100, Math.round(value * 100.0 / goal));
    }

    static String servingsLabel(double servings) {
        return (servings == Math.rint(servings)
                ? String.valueOf((int) servings) : String.valueOf(servings))
                + (servings == 1.0 ? " serving" : " servings");
    }
}
