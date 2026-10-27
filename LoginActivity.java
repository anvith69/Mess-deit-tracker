package com.example.miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.miniproject.data.User;

/** Launcher screen. Logs in with email OR registration number (checked in SQLite). */
public class LoginActivity extends BaseActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private TextView errorText;

    @Override
    protected boolean requiresLogin() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Already logged in earlier? Skip straight to the dashboard.
        if (userId >= 0) {
            openHome();
            return;
        }

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        errorText = findViewById(R.id.errorText);

        String prefill = getIntent().getStringExtra("prefillEmail");
        if (prefill != null) {
            emailInput.setText(prefill);
            passwordInput.requestFocus();
            Toast.makeText(this, "Account created. Please log in.", Toast.LENGTH_LONG).show();
        }

        Button loginButton = findViewById(R.id.loginButton);
        Button createAccountButton = findViewById(R.id.createAccountButton);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);

        loginButton.setOnClickListener(v -> loginUser());
        // Pressing "Done" / Enter on the keyboard also logs in.
        passwordInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passwordInput.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginUser();
                return true;
            }
            return false;
        });
        createAccountButton.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrationActivity.class)));
        forgotPasswordText.setOnClickListener(v -> showResetDialog());
    }

    private void loginUser() {
        String identifier = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        errorText.setVisibility(View.GONE);

        if (identifier.isEmpty()) {
            emailInput.setError("Enter your email or registration number");
            emailInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Enter your password");
            passwordInput.requestFocus();
            return;
        }

        User user = db.login(identifier, password);
        if (user == null) {
            errorText.setText("Invalid login details. Check your email / registration "
                    + "number and password, or create an account.");
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        session.saveUser(user.id);
        openHome();
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Offline password reset: email and registration number must both match the account. */
    private void showResetDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(12), dp(24), 0);

        EditText email = new EditText(this);
        email.setHint("Registered email");
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText reg = new EditText(this);
        reg.setHint("Registration number");
        EditText newPass = new EditText(this);
        newPass.setHint("New password (min 6 characters)");
        newPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        form.addView(email);
        form.addView(reg);
        form.addView(newPass);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Reset password")
                .setView(form)
                .setPositiveButton("Reset", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String r = reg.getText().toString().trim();
            String p = newPass.getText().toString();
            if (e.isEmpty() || r.isEmpty() || p.length() < 6) {
                Toast.makeText(this, "Fill all fields (password min 6 characters)",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (db.resetPassword(e, r, p)) {
                Toast.makeText(this, "Password updated. You can log in now.",
                        Toast.LENGTH_LONG).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Email and registration number do not match an account.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
