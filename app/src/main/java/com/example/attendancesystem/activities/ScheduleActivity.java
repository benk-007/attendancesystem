package com.example.attendancesystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Session;
import com.example.attendancesystem.models.Student;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.SchedulePagerAdapter;
import com.example.attendancesystem.utils.Utils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Activité pour consulter l'emploi du temps de l'étudiant
 * Affichage par jour, semaine et mois avec navigation
 */
public class ScheduleActivity extends AppCompatActivity {

    private static final String TAG = "Schedule";

    // Views
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TextView tvCurrentPeriod, tvScheduleInfo;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private Button btnPreviousPeriod, btnToday, btnNextPeriod;

    // Data
    private FirebaseManager firebaseManager;
    private Student currentStudent;
    private SchedulePagerAdapter pagerAdapter;

    // Calendar pour navigation
    private Calendar currentCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // Configurer la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mon Emploi du Temps");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialiser Firebase et calendar
        firebaseManager = FirebaseManager.getInstance();
        currentCalendar = Calendar.getInstance();

        // Initialiser les views
        initViews();
        setupViewPager();
        setupNavigationButtons();

        // Charger les données utilisateur
        loadUserData();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout_schedule);
        viewPager = findViewById(R.id.view_pager_schedule);
        tvCurrentPeriod = findViewById(R.id.tv_current_period);
        tvScheduleInfo = findViewById(R.id.tv_schedule_info);
        progressBar = findViewById(R.id.progress_bar_schedule);
        layoutEmptyState = findViewById(R.id.layout_empty_state_schedule);

        // Boutons de navigation
        btnPreviousPeriod = findViewById(R.id.btn_previous_period);
        btnToday = findViewById(R.id.btn_today);
        btnNextPeriod = findViewById(R.id.btn_next_period);

        // Afficher la période actuelle
        updateCurrentPeriodDisplay();
    }

    private void setupViewPager() {
        pagerAdapter = new SchedulePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Configurer les tabs
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Aujourd'hui");
                            tab.setIcon(R.drawable.ic_calendar);
                            break;
                        case 1:
                            tab.setText("Semaine");
                            tab.setIcon(R.drawable.ic_badge);
                            break;
                        case 2:
                            tab.setText("Mois");
                            tab.setIcon(R.drawable.ic_email);
                            break;
                    }
                }
        ).attach();
    }

    private void setupNavigationButtons() {
        btnPreviousPeriod.setOnClickListener(v -> navigatePrevious());
        btnToday.setOnClickListener(v -> goToToday());
        btnNextPeriod.setOnClickListener(v -> navigateNext());
    }

    private void loadUserData() {
        String userEmail = Utils.getSavedUserEmail(this);
        if (userEmail == null) {
            Utils.showToast(this, "Erreur: Utilisateur non connecté");
            finish();
            return;
        }

        showLoading(true);

        firebaseManager.getStudentByEmail(userEmail, new FirebaseManager.DataCallback<Student>() {
            @Override
            public void onSuccess(Student student) {
                currentStudent = student;
                Log.d(TAG, "Student loaded: " + student.getFullName());

                updateScheduleInfo();
                loadScheduleData();
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Error loading student data: " + error);
                showError("Erreur lors du chargement des données: " + error);
            }
        });
    }

    /**
     * Mettre à jour les informations de l'emploi du temps
     */
    private void updateScheduleInfo() {
        if (currentStudent != null) {
            String info = String.format("Emploi du temps pour %s - %s %s",
                    currentStudent.getField() != null ? currentStudent.getField() : "Filière",
                    currentStudent.getYear(),
                    currentStudent.getDepartment()
            );
            tvScheduleInfo.setText(info);
        }
    }

    /**
     * Mettre à jour l'affichage de la période actuelle
     */
    private void updateCurrentPeriodDisplay() {
        String currentDate = Utils.formatDate(Timestamp.now());
        tvCurrentPeriod.setText("📅 " + currentDate);
    }

    /**
     * Charger les données de l'emploi du temps
     */
    private void loadScheduleData() {
        if (currentStudent == null) {
            showError("Données étudiant non disponibles");
            return;
        }

        Log.d(TAG, "Loading schedule data for: " + currentStudent.getEmail());

        // Charger les sessions d'aujourd'hui
        loadTodaySchedule();

        // Charger les sessions de la semaine
        loadWeeklySchedule();

        // Charger les sessions du mois
        loadMonthlySchedule();
    }

    /**
     * Charger l'emploi du temps d'aujourd'hui
     */
    private void loadTodaySchedule() {
        firebaseManager.getTodaySessionsForStudent(
                currentStudent.getEmail(),
                currentStudent.getDepartment(),
                currentStudent.getField() != null ? currentStudent.getField() : "",
                currentStudent.getYear(),
                new FirebaseManager.DataCallback<List<Session>>() {
                    @Override
                    public void onSuccess(List<Session> sessions) {
                        Log.d(TAG, "Today schedule loaded: " + sessions.size() + " sessions");
                        pagerAdapter.updateTodaySchedule(sessions);
                        checkEmptyState();
                        showLoading(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading today schedule: " + error);
                        pagerAdapter.updateTodaySchedule(new ArrayList<>());
                        showLoading(false);
                    }
                }
        );
    }

    /**
     * Charger l'emploi du temps de la semaine
     */
    private void loadWeeklySchedule() {
        // Calculer le début de la semaine
        Calendar weekStart = Calendar.getInstance();
        weekStart.setTime(currentCalendar.getTime());
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);

        Timestamp weekStartTimestamp = new Timestamp(weekStart.getTime());

        firebaseManager.getWeeklySessionsForField(
                currentStudent.getDepartment(),
                currentStudent.getField() != null ? currentStudent.getField() : "",
                currentStudent.getYear(),
                weekStartTimestamp,
                new FirebaseManager.DataCallback<List<Session>>() {
                    @Override
                    public void onSuccess(List<Session> sessions) {
                        Log.d(TAG, "Weekly schedule loaded: " + sessions.size() + " sessions");
                        pagerAdapter.updateWeeklySchedule(sessions);
                        checkEmptyState();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading weekly schedule: " + error);
                        pagerAdapter.updateWeeklySchedule(new ArrayList<>());
                    }
                }
        );
    }

    /**
     * Charger l'emploi du temps du mois
     */
    private void loadMonthlySchedule() {
        // Calculer le début du mois
        Calendar monthStart = Calendar.getInstance();
        monthStart.setTime(currentCalendar.getTime());
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);

        Timestamp monthStartTimestamp = new Timestamp(monthStart.getTime());

        // Pour le mois, on charge plusieurs semaines
        firebaseManager.getWeeklySessionsForField(
                currentStudent.getDepartment(),
                currentStudent.getField() != null ? currentStudent.getField() : "",
                currentStudent.getYear(),
                monthStartTimestamp,
                new FirebaseManager.DataCallback<List<Session>>() {
                    @Override
                    public void onSuccess(List<Session> sessions) {
                        Log.d(TAG, "Monthly schedule loaded: " + sessions.size() + " sessions");
                        pagerAdapter.updateMonthlySchedule(sessions);
                        checkEmptyState();
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Error loading monthly schedule: " + error);
                        pagerAdapter.updateMonthlySchedule(new ArrayList<>());
                    }
                }
        );
    }

    /**
     * Navigation - Période précédente
     */
    private void navigatePrevious() {
        int currentTab = viewPager.getCurrentItem();

        switch (currentTab) {
            case 0: // Jour
                currentCalendar.add(Calendar.DAY_OF_MONTH, -1);
                loadTodaySchedule();
                break;
            case 1: // Semaine
                currentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
                loadWeeklySchedule();
                break;
            case 2: // Mois
                currentCalendar.add(Calendar.MONTH, -1);
                loadMonthlySchedule();
                break;
        }

        updateCurrentPeriodDisplay();
    }

    /**
     * Navigation - Période suivante
     */
    private void navigateNext() {
        int currentTab = viewPager.getCurrentItem();

        switch (currentTab) {
            case 0: // Jour
                currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
                loadTodaySchedule();
                break;
            case 1: // Semaine
                currentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
                loadWeeklySchedule();
                break;
            case 2: // Mois
                currentCalendar.add(Calendar.MONTH, 1);
                loadMonthlySchedule();
                break;
        }

        updateCurrentPeriodDisplay();
    }

    /**
     * Revenir à aujourd'hui
     */
    public void goToToday() {
        currentCalendar = Calendar.getInstance();
        updateCurrentPeriodDisplay();
        loadScheduleData();
    }

    /**
     * Vérifier s'il faut afficher l'état vide
     */
    private void checkEmptyState() {
        boolean hasSchedule = pagerAdapter.hasAnyScheduleData();
        showEmptyState(!hasSchedule);
    }

    /**
     * Afficher/masquer l'état vide
     */
    private void showEmptyState(boolean show) {
        if (show) {
            viewPager.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            viewPager.setVisibility(View.VISIBLE);
            tabLayout.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    /**
     * Afficher l'état de chargement
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            // Les autres vues seront gérées par checkEmptyState()
        }
    }

    /**
     * Afficher une erreur
     */
    private void showError(String message) {
        Utils.showToast(this, message);
        Log.e(TAG, message);
        showEmptyState(true);
        showLoading(false);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger l'emploi du temps au retour sur l'activité
        if (currentStudent != null) {
            loadScheduleData();
        }
    }
}