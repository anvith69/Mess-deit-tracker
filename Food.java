package com.example.miniproject.data;

/** Plain model for a row of the `foods` table. Nutrition values are per ONE serving. */
public class Food {
    public long id;
    public String name;
    public String mealType;   // Breakfast / Lunch / Dinner
    public int calories;
    public double protein;
    public double carbs;
    public double fat;
    public double fibre;
    public boolean custom;    // true when the student added it themselves
}
