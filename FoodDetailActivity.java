package com.example.miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.miniproject.data.Food;
import com.example.miniproject.data.User;
import com.example.miniproject.util.NutritionCalc;

/** Nutrition details of one food; the stepper recalculates every value live. */
public class FoodDetailActivity extends BaseActivity {

    public static final String EXTRA_FOOD_ID = "food_id";
    public static final String EXTRA_MEAL = "meal";

    private Food food;
    private User user;
    private double servings = 1.0;

    private TextView servingsText, detailCalories, detailProtein, detailCarbs;
    private TextView detailFat, detailFibre, detailGoalText;
    private Spinner mealSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (userId < 0) return;

        food = db.getFood(getIntent().getLongExtra(EXTRA_FOOD_ID, -1));
        user = db.getUser(userId);
        if (food == null || user == null) {
            Toast.makeText(this, "Food not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (savedInstanceState != null) {
            servings = savedInstanceState.getDouble("servings", 1.0);
        }

        setContentView(R.layout.activity_food_detail);
        setupHeader(food.name);

        servingsText = findViewById(R.id.servingsText);
        detailCalories = findViewById(R.id.detailCalories);
        detailProtein = findViewById(R.id.detailProtein);
        detailCarbs = findViewById(R.id.detailCarbs);
        detailFat = findViewById(R.id.detailFat);
        detailFibre = findViewById(R.id.detailFibre);
        detailGoalText = findViewById(R.id.detailGoalText);
        mealSpinner = findViewById(R.id.mealSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, MessMenuActivity.MEALS);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mealSpinner.setAdapter(adapter);
        String meal = getIntent().getStringExtra(EXTRA_MEAL);
        for (int i = 0; i < MessMenuActivity.MEALS.length; i++) {
            if (MessMenuActivity.MEALS[i].equals(meal)) mealSpinner.setSelection(i);
        }

        findViewById(R.id.servingMinus).setOnClickListener(v -> changeServings(-0.5));
        findViewById(R.id.servingPlus).setOnClickListener(v -> changeServings(0.5));
        findViewById(R.id.logButton).setOnClickListener(v -> logMeal());

        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putDouble("servings", servings);
    }

    private void changeServings(double delta) {
        double next = servings + delta;
        if (next < 0.5 || next > 5.0) return;
        servings = next;
        render();
    }

    private void render() {
        servingsText.setText(HomeActivity.servingsLabel(servings).split(" ")[0]);
        ((TextView) findViewById(R.id.detailNote)).setText(
                food.mealType + (food.custom ? " · added by you" : " · mess menu")
                        + " · approximate values for " + HomeActivity.servingsLabel(servings));

        double kcal = food.calories * servings;
        detailCalories.setText("Calories: " + NutritionCalc.kcal(kcal));
        detailProtein.setText("Protein: " + NutritionCalc.grams(food.protein * servings));
        detailCarbs.setText("Carbohydrates: " + NutritionCalc.grams(food.carbs * servings));
        detailFat.setText("Fat: " + NutritionCalc.grams(food.fat * servings));
        detailFibre.setText("Fibre: " + NutritionCalc.grams(food.fibre * servings));

        int goal = NutritionCalc.dailyCalorieGoal(user);
        detailGoalText.setText("This is about " + Math.round(kcal * 100.0 / goal)
                + "% of your daily goal (" + NutritionCalc.kcal(goal) + ").");
    }

    private void logMeal() {
        String meal = mealSpinner.getSelectedItem().toString();
        db.logMeal(userId, food.id, meal, servings);
        Toast.makeText(this, "Logged " + food.name, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, DietTrackerActivity.class));
        finish();
    }
}
