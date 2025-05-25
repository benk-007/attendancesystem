package com.example.attendancesystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Admin; // Import the Admin model
import com.example.attendancesystem.models.Justification;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.AdminJustificationAdapter;
import com.example.attendancesystem.utils.Utils; // Still useful for general utilities

import java.util.ArrayList;
import java.util.List;

public class AdminJustificationActivity extends AppCompatActivity implements AdminJustificationAdapter.OnJustificationActionListener {

    private static final String TAG = "AdminJustificationAct";

    private FirebaseManager firebaseManager;
    private RecyclerView rvJustifications;
    private AdminJustificationAdapter adapter;
    private List<Justification> allJustifications;
    private List<Justification> displayedJustifications;
    private ProgressBar progressBar;
    private Spinner statusFilterSpinner;

    private Admin currentAdmin; // NEW: Field to hold the current logged-in admin's data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_justification);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Gestion des Justifications");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        firebaseManager = FirebaseManager.getInstance();
        allJustifications = new ArrayList<>();
        displayedJustifications = new ArrayList<>();

        initViews();
        setupRecyclerView();
        setupFilterSpinner();
        loadAdminDataAndJustifications(); // Modified: Load admin first
    }

    private void initViews() {
        rvJustifications = findViewById(R.id.rv_admin_justifications);
        progressBar = findViewById(R.id.progress_bar_admin);
        statusFilterSpinner = findViewById(R.id.spinner_filter_status);
    }

    private void setupRecyclerView() {
        adapter = new AdminJustificationAdapter(displayedJustifications, this); // 'this' for listener
        rvJustifications.setLayoutManager(new LinearLayoutManager(this));
        rvJustifications.setAdapter(adapter);
    }

    private void setupFilterSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.justification_statuses_filter,
                android.R.layout.simple_spinner_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilterSpinner.setAdapter(spinnerAdapter);

        statusFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterJustifications();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    // NEW METHOD: Load admin data first
    private void loadAdminDataAndJustifications() {
        String adminEmail = Utils.getSavedUserEmail(this); // Get admin email from shared preferences
        if (adminEmail == null) {
            Utils.showToast(this, "Admin email not found. Please log in again.");
            finish(); // Close activity if admin email isn't available
            return;
        }

        showLoading(true);
        firebaseManager.getAdminByEmail(adminEmail, new FirebaseManager.DataCallback<Admin>() {
            @Override
            public void onSuccess(Admin admin) {
                currentAdmin = admin; // Store the admin object
                Log.d(TAG, "Admin data loaded: " + admin.getEmail());
                loadJustifications(); // Then load justifications
            }

            @Override
            public void onFailure(String error) {
                Utils.showToast(AdminJustificationActivity.this, "Failed to load admin data: " + error);
                Log.e(TAG, "Error loading admin data: " + error);
                showLoading(false); // Hide loading on failure
            }
        });
    }


    private void loadJustifications() {
        // Only proceed if admin data is loaded
        if (currentAdmin == null) {
            Log.e(TAG, "Admin data not loaded, cannot load justifications.");
            showLoading(false);
            return;
        }

        showLoading(true);
        firebaseManager.getAllJustifications(new FirebaseManager.DataCallback<List<Justification>>() {
            @Override
            public void onSuccess(List<Justification> justifications) {
                allJustifications.clear();
                allJustifications.addAll(justifications);
                filterJustifications();
                showLoading(false);
                Log.d(TAG, "Loaded " + allJustifications.size() + " total justifications.");
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminJustificationActivity.this, "Error loading justifications: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading justifications: " + error);
                showLoading(false);
            }
        });
    }

    private void filterJustifications() {
        String selectedStatus = statusFilterSpinner.getSelectedItem().toString();
        displayedJustifications.clear();

        if (selectedStatus.equals("Toutes")) {
            displayedJustifications.addAll(allJustifications);
        } else {
            for (Justification j : allJustifications) {
                if (j.getStatus().equalsIgnoreCase(selectedStatus)) {
                    displayedJustifications.add(j);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvJustifications.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    // --- AdminJustificationAdapter.OnJustificationActionListener Callbacks ---

    @Override
    public void onApproveClick(Justification justification, String comments, String reason) {
        // Use currentAdmin.getEmail() directly
        if (currentAdmin == null || currentAdmin.getEmail() == null) {
            Toast.makeText(this, "Admin data not available. Cannot approve.", Toast.LENGTH_SHORT).show();
            return;
        }

        justification.approve(currentAdmin.getEmail(), comments, reason); // Use currentAdmin.getEmail()
        firebaseManager.updateJustification(justification, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(AdminJustificationActivity.this, "Justification approuvée!", Toast.LENGTH_SHORT).show();
                loadJustifications(); // Reload to update UI
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminJustificationActivity.this, "Erreur approbation: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRejectClick(Justification justification, String comments, String reason) {
        // Use currentAdmin.getEmail() directly
        if (currentAdmin == null || currentAdmin.getEmail() == null) {
            Toast.makeText(this, "Admin data not available. Cannot reject.", Toast.LENGTH_SHORT).show();
            return;
        }

        justification.reject(currentAdmin.getEmail(), comments, reason); // Use currentAdmin.getEmail()
        firebaseManager.updateJustification(justification, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(AdminJustificationActivity.this, "Justification rejetée!", Toast.LENGTH_SHORT).show();
                loadJustifications(); // Reload to update UI
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AdminJustificationActivity.this, "Erreur rejet: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}