package com.example.miniproject.data;

/** Plain model for a row of the `users` table (password hash is never kept here). */
public class User {
    public long id;
    public String name;
    public String email;
    public String regNo;
    public String gender;        // Male / Female / Other
    public String activityLevel; // Low / Light / Moderate / High
    public int age;
    public double heightCm;
    public double weightKg;

    public String firstName() {
        if (name == null || name.trim().isEmpty()) return "there";
        return name.trim().split("\\s+")[0];
    }
}
