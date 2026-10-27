package com.example.miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.miniproject.data.User;

/** Creates an account: validates the form, then inserts a row into SQLite. */
public class RegistrationActivity extends BaseActivity {

    private EditText nameInput, regNoInput, ageInput, heightInput, weightInput;
    private EditText emailInput, passwordInput;
    private Spinner genderSpinner, activitySpinner;
    private TextView statusText;

    @Override
    protected boolean requiresLogin() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        nameInput = findViewById(R.id.nameInput);
        regNoInput = findViewById(R.id.regNoInput);
        ageInput = findViewById(R.id.ageInput);
        heightInput = findViewById(R.id.heightInput);
        weightInput = findViewById(R.id.weightInput);
        emailInput = findViewById(R.id.registrationEmailInput);
        passwordInput = findViewById(R.id.registrationPasswordInput);
        genderSpinner = findViewById(R.id.genderSpinner);
        activitySpinner = findViewById(R.id.activitySpinner);
        statusText = findViewById(R.id.registrationStatusText);

        setSpinner(genderSpinner, new String[]{"Male", "Female", "Other"});
        setSpinner(activitySpinner, new String[]{"Low", "Light", "Moderate", "High"});
        activitySpinner.setSelection(2);

        Button registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> registerUser());
        findViewById(R.id.backToLoginText).setOnClickListener(v -> finish());
    }

    private void setSpinner(Spinner spinner, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, values);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void registerUser() {
        statusText.setVisibility(View.GONE);

        String name = nameInput.getText().toString().trim();
        String regNo = regNoInput.getText().toString().trim();
        String ageString = ageInput.getText().toString().trim();
        String heightString = heightInput.getText().toString().trim();
        String weightString = weightInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (name.isEmpty() || ageString.isEmpty() || heightString.isEmpty()
                || weightString.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showStatus("Please fill in all required fields.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password must contain at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        int age;
        double height;
        double weight;
        try {
            age = Integer.parseInt(ageString);
            height = Double.parseDouble(heightString);
            weight = Double.parseDouble(weightString);
        } catch (NumberFormatException e) {
            showStatus("Enter valid numbers for age, height and weight.");
            return;
        }
        if (age < 10 || age > 100) {
            showStatus("Age must be between 10 and 100.");
            return;
        }
        if (height < 100 || height > 230) {
            showStatus("Height must be between 100 and 230 cm.");
            return;
        }
        if (weight < 25 || weight > 250) {
            showStatus("Weight must be between 25 and 250 kg.");
            return;
        }

        User user = new User();
        user.name = name;
        user.email = email;
        user.regNo = regNo;
        user.age = age;
        user.heightCm = height;
        user.weightKg = weight;
        user.gender = genderSpinner.getSelectedItem().toString();
        user.activityLevel = activitySpinner.getSelectedItem().toString();

        long result = db.registerUser(user, password);
        if (result == -2) {
            showStatus("This email is already registered. Try logging in.");
        } else if (result == -3) {
            showStatus("This registration number is already registered.");
        } else if (result < 0) {
            showStatus("Could not create the account. Please try again.");
        } else {
            // Back to login so the student logs in explicitly.
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("prefillEmail", email);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void showStatus(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
    }
}
