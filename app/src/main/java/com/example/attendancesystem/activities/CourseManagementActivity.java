package com.example.attendancesystem.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


public class CourseManagementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.attendancesystem.R.layout.activity_course_management);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestion des Cours");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}