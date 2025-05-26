package com.example.attendancesystem.utils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.attendancesystem.fragments.DayScheduleFragment;
import com.example.attendancesystem.fragments.WeekScheduleFragment;
import com.example.attendancesystem.fragments.MonthScheduleFragment;
import com.example.attendancesystem.models.Session;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter pour ViewPager2 de l'emploi du temps
 * Gère les 3 vues : Aujourd'hui, Semaine, Mois
 */
public class SchedulePagerAdapter extends FragmentStateAdapter {

    private static final String TAG = "SchedulePagerAdapter";

    // Fragments
    private DayScheduleFragment dayFragment;
    private WeekScheduleFragment weekFragment;
    private MonthScheduleFragment monthFragment;

    // Données
    private List<Session> todaySchedule;
    private List<Session> weeklySchedule;
    private List<Session> monthlySchedule;

    public SchedulePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);

        // Initialiser les listes
        todaySchedule = new ArrayList<>();
        weeklySchedule = new ArrayList<>();
        monthlySchedule = new ArrayList<>();

        // Créer les fragments
        dayFragment = new DayScheduleFragment();
        weekFragment = new WeekScheduleFragment();
        monthFragment = new MonthScheduleFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: // Aujourd'hui
                return dayFragment;
            case 1: // Semaine
                return weekFragment;
            case 2: // Mois
                return monthFragment;
            default:
                return dayFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 3; // 3 onglets : Aujourd'hui, Semaine, Mois
    }

    /**
     * Mettre à jour l'emploi du temps d'aujourd'hui
     */
    public void updateTodaySchedule(List<Session> sessions) {
        this.todaySchedule.clear();
        if (sessions != null) {
            this.todaySchedule.addAll(sessions);
        }

        if (dayFragment != null) {
            dayFragment.updateSchedule(this.todaySchedule);
        }
    }

    /**
     * Mettre à jour l'emploi du temps de la semaine
     */
    public void updateWeeklySchedule(List<Session> sessions) {
        this.weeklySchedule.clear();
        if (sessions != null) {
            this.weeklySchedule.addAll(sessions);
        }

        if (weekFragment != null) {
            weekFragment.updateSchedule(this.weeklySchedule);
        }
    }

    /**
     * Mettre à jour l'emploi du temps du mois
     */
    public void updateMonthlySchedule(List<Session> sessions) {
        this.monthlySchedule.clear();
        if (sessions != null) {
            this.monthlySchedule.addAll(sessions);
        }

        if (monthFragment != null) {
            monthFragment.updateSchedule(this.monthlySchedule);
        }
    }

    /**
     * Vérifier s'il y a des données d'emploi du temps
     */
    public boolean hasAnyScheduleData() {
        return !todaySchedule.isEmpty() ||
                !weeklySchedule.isEmpty() ||
                !monthlySchedule.isEmpty();
    }

    /**
     * Obtenir le nombre total de sessions
     */
    public int getTotalSessionsCount() {
        return todaySchedule.size() + weeklySchedule.size() + monthlySchedule.size();
    }

    /**
     * Obtenir les sessions d'aujourd'hui
     */
    public List<Session> getTodaySchedule() {
        return new ArrayList<>(todaySchedule);
    }

    /**
     * Obtenir les sessions de la semaine
     */
    public List<Session> getWeeklySchedule() {
        return new ArrayList<>(weeklySchedule);
    }

    /**
     * Obtenir les sessions du mois
     */
    public List<Session> getMonthlySchedule() {
        return new ArrayList<>(monthlySchedule);
    }

    /**
     * Réinitialiser toutes les données
     */
    public void clearAllData() {
        todaySchedule.clear();
        weeklySchedule.clear();
        monthlySchedule.clear();

        if (dayFragment != null) {
            dayFragment.updateSchedule(new ArrayList<>());
        }
        if (weekFragment != null) {
            weekFragment.updateSchedule(new ArrayList<>());
        }
        if (monthFragment != null) {
            monthFragment.updateSchedule(new ArrayList<>());
        }
    }
}