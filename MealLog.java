package com.example.miniproject.data;

/** One logged meal entry. Nutrition fields are already multiplied by `servings`. */
public class MealLog {
    public long id;
    public long foodId;
    public String foodName;
    public String mealType;
    public double servings;
    public long loggedAt;
    public double calories;
    public double protein;
    public double carbs;
    public double fat;
    public double fibre;
}
