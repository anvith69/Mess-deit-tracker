package com.example.miniproject.data;

/** Aggregated nutrition for a single day (result of a SQL SUM query). */
public class DayTotals {
    public String date;   // yyyy-MM-dd
    public double calories;
    public double protein;
    public double carbs;
    public double fat;
    public double fibre;
    public int mealCount;
}
