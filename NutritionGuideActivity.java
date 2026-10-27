package com.example.miniproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.miniproject.data.DatabaseHelper;
import com.example.miniproject.data.DayTotals;
import com.example.miniproject.data.MealLog;
import com.example.miniproject.data.User;
import com.example.miniproject.util.NutritionCalc;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Advice that changes with the student's profile, today's intake and the time of day. */
public class NutritionGuideActivity extends BaseActivity {

    private LinearLayout tipsContainer;
    private TextView profileSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (userId < 0) return;

        setContentView(R.layout.activity_nutrition_guide);
        setupHeader("Nutrition Guide");
        tipsContainer = findViewById(R.id.tipsContainer);
        profileSummary = findViewById(R.id.profileSummary);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId >= 0 && tipsContainer != null) build();
    }

    private void build() {
        User user = db.getUser(userId);
        if (user == null) return;

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int calGoal = NutritionCalc.dailyCalorieGoal(user);
        int proGoal = NutritionCalc.dailyProteinGoal(user);
        double bmi = NutritionCalc.bmi(user);
        String today = DatabaseHelper.todayKey();
        DayTotals t = db.getTotals(userId, today);
        List<MealLog> logs = db.getLogs(userId, today);

        profileSummary.setText(String.format(Locale.getDefault(),
                "BMI %.1f (%s)  ·  Goal: %s and %d g protein per day",
                bmi, NutritionCalc.bmiCategory(bmi), NutritionCalc.kcal(calGoal), proGoal));

        tipsContainer.removeAllViews();

        // Calories
        if (t.mealCount == 0) {
            addTip("Start your day", "Nothing logged yet. Pick a meal from the mess menu and log it.");
        } else if (t.calories > calGoal * 1.10) {
            addTip("Over your calorie goal", "You are about " + Math.round(t.calories - calGoal)
                    + " kcal above today's goal. Prefer vegetables, curd or fruit for the next meal.");
        } else {
            addTip("Calories", "You have about " + Math.max(0, Math.round(calGoal - t.calories))
                    + " kcal left today. Spread them over your remaining meals.");
        }

        // Protein
        if (t.protein < proGoal * 0.5 && hour >= 14) {
            addTip("Protein is low", "Only " + NutritionCalc.grams(t.protein) + " of " + proGoal
                    + " g so far. Add eggs, dal, curd, paneer or milk.");
        } else if (t.protein >= proGoal) {
            addTip("Protein goal reached", "You have met your " + proGoal + " g protein target. Nice!");
        } else {
            addTip("Protein", "Aim for " + proGoal + " g today. Good mess sources: eggs, dal, "
                    + "curd, milk, paneer and chicken curry.");
        }

        // Fibre
        if (t.fibre < 12 && hour >= 14) {
            addTip("Add fibre", "Fibre so far: " + NutritionCalc.grams(t.fibre)
                    + ". Include fruit and mixed vegetables in your next meal.");
        } else {
            addTip("Fibre", "Fruits, vegetables and whole grains keep you full and help digestion.");
        }

        // Skipped breakfast
        boolean hadBreakfast = false;
        for (MealLog l : logs) if ("Breakfast".equals(l.mealType)) hadBreakfast = true;
        if (!hadBreakfast && hour >= 11) {
            addTip("No breakfast logged", "Skipping breakfast often leads to overeating later. "
                    + "Try to log it tomorrow morning.");
        }

        // Hydration based on body weight
        double litres = user.weightKg * 0.035;
        addTip("Hydration", String.format(Locale.getDefault(),
                "Aim for about %.1f litres of water today. Drink regularly, not all at once.", litres));

        // BMI-based note
        String bmiNote;
        if (bmi < 18.5) bmiNote = "Your BMI is in the underweight range. Add extra servings of protein-rich foods.";
        else if (bmi < 25) bmiNote = "Your BMI is in the normal range. Keep a balanced plate: carbs, protein and vegetables.";
        else bmiNote = "Your BMI is above the normal range. Control portion size of rice and fried items.";
        addTip("About your profile", bmiNote + " (This is general guidance, not medical advice.)");
    }

    private void addTip(String title, String body) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_tip, tipsContainer, false);
        ((TextView) card.findViewById(R.id.tipTitle)).setText(title);
        ((TextView) card.findViewById(R.id.tipBody)).setText(body);
        tipsContainer.addView(card);
    }
}
