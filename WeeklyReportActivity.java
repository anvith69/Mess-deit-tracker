package com.example.miniproject;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.miniproject.data.DayTotals;
import com.example.miniproject.data.User;
import com.example.miniproject.util.NutritionCalc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Report built from the saved logs of the last 7 or 14 days (SQL SUM per day). */
public class WeeklyReportActivity extends BaseActivity {

    private int rangeDays = 7;
    private User user;

    private TextView statAvg, statProtein, statOnTarget, statBest, chartTitle;
    private LinearLayout barsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (userId < 0) return;

        setContentView(R.layout.activity_weekly_report);
        setupHeader("Weekly Report");

        statAvg = findViewById(R.id.statAvg);
        statProtein = findViewById(R.id.statProtein);
        statOnTarget = findViewById(R.id.statOnTarget);
        statBest = findViewById(R.id.statBest);
        chartTitle = findViewById(R.id.chartTitle);
        barsContainer = findViewById(R.id.barsContainer);

        if (savedInstanceState != null) {
            rangeDays = savedInstanceState.getInt("range_days", 7);
        }

        findViewById(R.id.range7).setOnClickListener(v -> { rangeDays = 7; refresh(); });
        findViewById(R.id.range14).setOnClickListener(v -> { rangeDays = 14; refresh(); });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId >= 0 && barsContainer != null) {
            user = db.getUser(userId);
            refresh();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("range_days", rangeDays);
    }

    private void styleRange(int id, boolean selected) {
        Button b = findViewById(id);
        b.setBackgroundResource(selected ? R.drawable.primary_button : R.drawable.outline_button);
        b.setTextColor(ContextCompat.getColor(this,
                selected ? R.color.white : R.color.brand_green));
    }

    private void refresh() {
        if (user == null) return;
        styleRange(R.id.range7, rangeDays == 7);
        styleRange(R.id.range14, rangeDays == 14);
        int goal = NutritionCalc.dailyCalorieGoal(user);
        int proteinGoal = NutritionCalc.dailyProteinGoal(user);
        List<DayTotals> days = db.getDailyTotals(userId, rangeDays);

        double sumCal = 0, sumPro = 0, maxCal = goal;
        int logged = 0, onTarget = 0;
        DayTotals best = null;
        for (DayTotals t : days) {
            maxCal = Math.max(maxCal, t.calories);
            if (t.mealCount == 0) continue;
            logged++;
            sumCal += t.calories;
            sumPro += t.protein;
            double ratio = t.calories / goal;
            if (ratio >= 0.85 && ratio <= 1.10) onTarget++;
            if (best == null || Math.abs(t.calories - goal) < Math.abs(best.calories - goal)) {
                best = t;
            }
        }

        if (logged == 0) {
            statAvg.setText("No meals logged in the last " + rangeDays + " days.");
            statProtein.setText("Log meals from the mess menu to see your report.");
            statOnTarget.setText("");
            statBest.setText("");
        } else {
            statAvg.setText("Average: " + NutritionCalc.kcal(sumCal / logged)
                    + " per logged day (goal " + NutritionCalc.kcal(goal) + ")");
            statProtein.setText("Average protein: " + NutritionCalc.grams(sumPro / logged)
                    + " (goal " + proteinGoal + " g)");
            statOnTarget.setText("Days on target (85-110% of goal): " + onTarget + " of " + logged);
            statBest.setText("Closest to goal: " + friendlyDay(best.date, "EEE, d MMM")
                    + " (" + NutritionCalc.kcal(best.calories) + ")");
        }

        chartTitle.setText("Calories per day (last " + rangeDays + " days)");
        barsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int green = ContextCompat.getColor(this, R.color.brand_green);
        int amber = ContextCompat.getColor(this, R.color.warn_amber);
        for (int i = days.size() - 1; i >= 0; i--) { // newest day on top
            DayTotals t = days.get(i);
            View row = inflater.inflate(R.layout.item_bar, barsContainer, false);
            ((TextView) row.findViewById(R.id.barLabel)).setText(friendlyDay(t.date, "EEE d"));
            ProgressBar bar = row.findViewById(R.id.barProgress);
            bar.setProgress((int) Math.round(t.calories * 100.0 / maxCal));
            bar.setProgressTintList(ColorStateList.valueOf(
                    t.calories > goal * 1.10 ? amber : green));
            ((TextView) row.findViewById(R.id.barValue)).setText(
                    t.mealCount == 0 ? "-" : String.format(Locale.getDefault(), "%,d", Math.round(t.calories)));
            barsContainer.addView(row);
        }
    }

    private String friendlyDay(String key, String pattern) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key);
            return new SimpleDateFormat(pattern, Locale.getDefault()).format(d);
        } catch (ParseException e) {
            return key;
        }
    }
}
