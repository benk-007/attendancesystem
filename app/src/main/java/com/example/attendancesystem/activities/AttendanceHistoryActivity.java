package com.example.attendancesystem.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


public class AttendanceHistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.attendancesystem.R.layout.activity_attendance_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Historique des Présences");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}