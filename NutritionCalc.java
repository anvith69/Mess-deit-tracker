package com.example.miniproject.util;

import com.example.miniproject.data.User;

import java.util.Locale;

/** Pure calculation helpers (no Android classes, so they are easy to unit-test). */
public final class NutritionCalc {

    private NutritionCalc() {}

    /** Daily calorie target = BMR (Mifflin-St Jeor equation) x activity factor. */
    public static int dailyCalorieGoal(User u) {
        double bmr = 10 * u.weightKg + 6.25 * u.heightCm - 5 * u.age;
        if ("Male".equals(u.gender)) bmr += 5;
        else if ("Female".equals(u.gender)) bmr -= 161;
        else bmr -= 78; // average of the two for "Other"

        double factor;
        if ("Low".equals(u.activityLevel)) factor = 1.2;
        else if ("Light".equals(u.activityLevel)) factor = 1.375;
        else if ("High".equals(u.activityLevel)) factor = 1.725;
        else factor = 1.55; // Moderate

        return (int) (Math.round(bmr * factor / 10.0) * 10);
    }

    /** Daily protein target in grams, based on body weight and activity. */
    public static int dailyProteinGoal(User u) {
        double perKg;
        if ("Low".equals(u.activityLevel)) perKg = 0.8;
        else if ("Light".equals(u.activityLevel)) perKg = 1.0;
        else if ("High".equals(u.activityLevel)) perKg = 1.5;
        else perKg = 1.2;
        return (int) Math.round(u.weightKg * perKg);
    }

    public static double bmi(User u) {
        double m = u.heightCm / 100.0;
        return u.weightKg / (m * m);
    }

    public static String bmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Normal";
        if (bmi < 30) return "Overweight";
        return "Obese";
    }

    /** Friendly one-line status shown on the dashboard. */
    public static String statusMessage(double calories, int goal, int hourOfDay) {
        if (calories <= 0) return "Nothing logged yet. Start with a meal from the mess menu.";
        double ratio = calories / goal;
        if (ratio > 1.10) {
            return "Over your goal by " + Math.round(calories - goal) + " kcal. Go light for the rest of the day.";
        }
        if (ratio >= 0.90) return "Goal reached. Great job today!";
        if (ratio < 0.40 && hourOfDay >= 15) {
            return "Behind target. You still need about " + Math.round(goal - calories) + " kcal.";
        }
        return "On track for a balanced day.";
    }

    public static String kcal(double value) {
        return String.format(Locale.getDefault(), "%,d kcal", Math.round(value));
    }

    public static String grams(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return String.format(Locale.getDefault(), "%.0f g", value);
        }
        return String.format(Locale.getDefault(), "%.1f g", value);
    }
}
