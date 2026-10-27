package com.example.miniproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.miniproject.data.DatabaseHelper;
import com.example.miniproject.util.SessionManager;

/**
 * Parent class of every screen. It gives each activity the database, the
 * session and the id of the logged-in user, and sends the student back to the
 * login screen if nobody is logged in (a simple "auth guard").
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected DatabaseHelper db;
    protected SessionManager session;
    protected long userId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = DatabaseHelper.getInstance(this);
        session = new SessionManager(this);
        userId = session.getUserId();

        if (requiresLogin() && userId < 0) {
            goToLogin();
        }
    }

    /**
     * Android 15+ draws apps edge-to-edge (under the status bar, navigation bar and
     * keyboard). We pad the whole screen by the system-bar and keyboard insets so no
     * content is hidden.
     */
    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Login and Registration override this to return false. */
    protected boolean requiresLogin() {
        return true;
    }

    /** Fills the shared header (back arrow + title). */
    protected void setupHeader(String title) {
        ((TextView) findViewById(R.id.headerTitle)).setText(title);
        findViewById(R.id.headerBack).setOnClickListener(v -> finish());
    }

    protected void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
