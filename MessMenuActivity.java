package com.example.miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.miniproject.data.DatabaseHelper;
import com.example.miniproject.data.DayTotals;
import com.example.miniproject.data.Food;
import com.example.miniproject.data.User;
import com.example.miniproject.ui.FoodAdapter;
import com.example.miniproject.util.NutritionCalc;
import com.google.android.material.tabs.TabLayout;

import java.util.Calendar;
import java.util.List;

/** Mess menu: foods come from the `foods` table; the selected tab decides the meal. */
public class MessMenuActivity extends BaseActivity implements FoodAdapter.Listener {

    static final String[] MEALS = {"Breakfast", "Lunch", "Dinner"};

    private String currentMeal;
    private FoodAdapter adapter;
    private TextView menuSummary;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (userId < 0) return;

        setContentView(R.layout.activity_mess_menu);
        setupHeader("Mess Menu");

        menuSummary = findViewById(R.id.menuSummary);

        // Open on the meal that matches the time of day.
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int start = hour < 11 ? 0 : hour < 16 ? 1 : 2;
        if (savedInstanceState != null) {
            start = savedInstanceState.getInt("meal_index", start); // keep tab after rotation
        }
        currentMeal = MEALS[start];

        TabLayout tabs = findViewById(R.id.mealTabs);
        for (String m : MEALS) tabs.addTab(tabs.newTab().setText(m));
        TabLayout.Tab first = tabs.getTabAt(start);
        if (first != null) first.select();
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentMeal = MEALS[tab.getPosition()];
                loadFoods();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        RecyclerView list = findViewById(R.id.foodList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FoodAdapter(this);
        list.setAdapter(adapter);

        findViewById(R.id.addFoodButton).setOnClickListener(v -> showAddFoodDialog());
        findViewById(R.id.openTrackerButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, DietTrackerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("meal_index", java.util.Arrays.asList(MEALS).indexOf(currentMeal));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId >= 0 && adapter != null) {
            user = db.getUser(userId);
            loadFoods();
            updateSummary();
        }
    }

    private void loadFoods() {
        adapter.setItems(db.getFoods(currentMeal, userId),
                db.getLoggedCounts(userId, currentMeal, DatabaseHelper.todayKey()));
    }

    private void updateSummary() {
        if (user == null) return;
        int goal = NutritionCalc.dailyCalorieGoal(user);
        DayTotals t = db.getTotals(userId, DatabaseHelper.todayKey());
        double left = goal - t.calories;
        menuSummary.setText("Today: " + NutritionCalc.kcal(t.calories) + " of "
                + NutritionCalc.kcal(goal) + "  ·  "
                + (left >= 0 ? NutritionCalc.kcal(left) + " left"
                : NutritionCalc.kcal(-left) + " over"));
    }

    // ---- FoodAdapter.Listener ------------------------------------------------

    @Override
    public void onOpen(Food food) {
        Intent intent = new Intent(this, FoodDetailActivity.class);
        intent.putExtra(FoodDetailActivity.EXTRA_FOOD_ID, food.id);
        intent.putExtra(FoodDetailActivity.EXTRA_MEAL, currentMeal);
        startActivity(intent);
    }

    @Override
    public void onQuickLog(Food food) {
        db.logMeal(userId, food.id, currentMeal, 1.0);
        Toast.makeText(this, "Logged " + food.name + " (" + food.calories + " kcal)",
                Toast.LENGTH_SHORT).show();
        loadFoods();
        updateSummary();
    }

    @Override
    public void onRemove(Food food) {
        if (db.removeLatestLog(userId, food.id, currentMeal)) {
            Toast.makeText(this, "Removed " + food.name, Toast.LENGTH_SHORT).show();
        }
        loadFoods();
        updateSummary();
    }

    @Override
    public void onLongPress(Food food) {
        if (!food.custom) {
            Toast.makeText(this, "Mess menu items can't be deleted.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete " + food.name + "?")
                .setMessage("Its logged entries will be removed too.")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteCustomFood(food.id, userId);
                    loadFoods();
                    updateSummary();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---- Add custom food -----------------------------------------------------

    private EditText field(LinearLayout form, String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        if (numeric) {
            e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        form.addView(e);
        return e;
    }

    private double num(EditText e) {
        String s = e.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void showAddFoodDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(12), dp(24), 0);

        EditText name = field(form, "Food name", false);
        EditText kcal = field(form, "Calories per serving (kcal)", true);
        EditText protein = field(form, "Protein (g)", true);
        EditText carbs = field(form, "Carbs (g)", true);
        EditText fat = field(form, "Fat (g)", true);
        EditText fibre = field(form, "Fibre (g)", true);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add food to " + currentMeal)
                .setView(form)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            double k = num(kcal);
            double p = num(protein), c = num(carbs), f = num(fat), fi = num(fibre);
            if (n.isEmpty() || k <= 0 || p < 0 || c < 0 || f < 0 || fi < 0) {
                Toast.makeText(this, "Enter a name and a valid calorie value.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Food food = new Food();
            food.name = n;
            food.mealType = currentMeal;
            food.calories = (int) Math.round(k);
            food.protein = p;
            food.carbs = c;
            food.fat = f;
            food.fibre = fi;
            db.addCustomFood(food, userId);
            loadFoods();
            dialog.dismiss();
            Toast.makeText(this, n + " added to " + currentMeal, Toast.LENGTH_SHORT).show();
        });
    }
}
