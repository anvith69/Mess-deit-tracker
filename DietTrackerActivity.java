package com.example.miniproject;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.miniproject.data.DatabaseHelper;
import com.example.miniproject.data.DayTotals;
import com.example.miniproject.data.MealLog;
import com.example.miniproject.data.User;
import com.example.miniproject.util.NutritionCalc;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Shows the saved log of any day (browse with the arrows) and lets you delete entries. */
public class DietTrackerActivity extends BaseActivity {

    private int dayOffset = 0; // 0 = today, 1 = yesterday ...
    private User user;

    private TextView dateLabel, trackerCalories, trackerMacros, emptyText, nextDay;
    private ProgressBar trackerProgress;
    private LinearLayout mealsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (userId < 0) return;

        setContentView(R.layout.activity_diet_tracker);
        setupHeader("Diet Tracker");

        if (savedInstanceState != null) {
            dayOffset = savedInstanceState.getInt("day_offset", 0);
        }

        dateLabel = findViewById(R.id.dateLabel);
        trackerCalories = findViewById(R.id.trackerCalories);
        trackerMacros = findViewById(R.id.trackerMacros);
        trackerProgress = findViewById(R.id.trackerProgress);
        mealsContainer = findViewById(R.id.mealsContainer);
        emptyText = findViewById(R.id.emptyText);
        nextDay = findViewById(R.id.nextDay);

        findViewById(R.id.prevDay).setOnClickListener(v -> {
            dayOffset++;
            refresh();
        });
        nextDay.setOnClickListener(v -> {
            if (dayOffset > 0) {
                dayOffset--;
                refresh();
            }
        });
        findViewById(R.id.saveMealButton).setOnClickListener(v -> saveAndGoHome());
        findViewById(R.id.addMealButton).setOnClickListener(v -> {
            // Reuse the menu screen if it is already open (keeps the back stack short).
            Intent intent = new Intent(this, MessMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        findViewById(R.id.guideButton).setOnClickListener(v ->
                startActivity(new Intent(this, NutritionGuideActivity.class)));
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("day_offset", dayOffset);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId >= 0 && mealsContainer != null) {
            user = db.getUser(userId);
            refresh();
        }
    }

    /** Meals are written to SQLite when logged; this confirms them and returns to the dashboard. */
    private void saveAndGoHome() {
        DayTotals today = db.getTotals(userId, DatabaseHelper.todayKey());
        if (today.mealCount == 0) {
            Toast.makeText(this, "Log at least one meal first.", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Meals saved (" + today.mealCount + " items, "
                + NutritionCalc.kcal(today.calories) + ")", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void refresh() {
        if (user == null) return;
        String key = DatabaseHelper.dateKeyDaysAgo(dayOffset);

        if (dayOffset == 0) {
            dateLabel.setText("Today");
        } else if (dayOffset == 1) {
            dateLabel.setText("Yesterday");
        } else {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, -dayOffset);
            dateLabel.setText(new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
                    .format(c.getTime()));
        }
        nextDay.setAlpha(dayOffset == 0 ? 0.3f : 1f);

        int goal = NutritionCalc.dailyCalorieGoal(user);
        DayTotals t = db.getTotals(userId, key);
        trackerCalories.setText(NutritionCalc.kcal(t.calories) + " / " + NutritionCalc.kcal(goal));
        trackerProgress.setProgress((int) Math.min(100, Math.round(t.calories * 100.0 / goal)));
        trackerProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                t.calories > goal * 1.10 ? R.color.warn_amber : R.color.brand_green)));
        trackerMacros.setText("Protein " + NutritionCalc.grams(t.protein)
                + "  ·  Carbs " + NutritionCalc.grams(t.carbs)
                + "  ·  Fat " + NutritionCalc.grams(t.fat)
                + "  ·  Fibre " + NutritionCalc.grams(t.fibre));

        mealsContainer.removeAllViews();
        List<MealLog> logs = db.getLogs(userId, key);
        emptyText.setVisibility(logs.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (String meal : MessMenuActivity.MEALS) {
            double subtotal = 0;
            boolean any = false;
            for (MealLog l : logs) {
                if (l.mealType.equals(meal)) { subtotal += l.calories; any = true; }
            }
            if (!any) continue;

            TextView heading = new TextView(this);
            heading.setText(meal + "  ·  " + NutritionCalc.kcal(subtotal));
            heading.setTextSize(16);
            heading.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            heading.setTypeface(heading.getTypeface(), android.graphics.Typeface.BOLD);
            heading.setPadding(dp(4), dp(10), 0, dp(6));
            mealsContainer.addView(heading);

            for (MealLog l : logs) {
                if (!l.mealType.equals(meal)) continue;
                View row = inflater.inflate(R.layout.item_log, mealsContainer, false);
                ((TextView) row.findViewById(R.id.logTitle)).setText(l.foodName);
                ((TextView) row.findViewById(R.id.logSub)).setText(
                        HomeActivity.servingsLabel(l.servings) + "  ·  "
                                + NutritionCalc.grams(l.protein) + " protein");
                ((TextView) row.findViewById(R.id.logKcal)).setText(NutritionCalc.kcal(l.calories));
                row.findViewById(R.id.logDelete).setVisibility(View.GONE); // removal is done in the mess menu
                mealsContainer.addView(row);
            }
        }
    }
}
