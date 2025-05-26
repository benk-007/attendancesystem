package com.example.attendancesystem.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Session;
import com.example.attendancesystem.utils.ScheduleAdapter;
import com.example.attendancesystem.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment pour afficher l'emploi du temps du mois
 */
public class MonthScheduleFragment extends Fragment {

    private static final String TAG = "MonthScheduleFragment";

    // Views
    private RecyclerView recyclerView;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    private TextView tvMonthInfo;

    // Data
    private ScheduleAdapter adapter;
    private List<Session> sessions;
    private Map<String, List<Session>> sessionsByWeek;

    public MonthScheduleFragment() {
        // Required empty public constructor
        sessions = new ArrayList<>();
        sessionsByWeek = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_month_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        updateMonthInfo();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_month_schedule);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state_month);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message_month);
        tvMonthInfo = view.findViewById(R.id.tv_month_info);
    }

    private void setupRecyclerView() {
        adapter = new ScheduleAdapter(sessions, ScheduleAdapter.VIEW_TYPE_MONTH);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    private void updateMonthInfo() {
        if (tvMonthInfo != null) {
            Calendar calendar = Calendar.getInstance();
            String monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.FRENCH);
            int year = calendar.get(Calendar.YEAR);

            String monthInfo = String.format("%s %d",
                    monthName != null ? monthName : "Mois", year);
            tvMonthInfo.setText(monthInfo);
        }
    }

    /**
     * Mettre à jour l'emploi du temps du mois
     */
    public void updateSchedule(List<Session> newSessions) {
        Log.d(TAG, "Updating month schedule with " + (newSessions != null ? newSessions.size() : 0) + " sessions");

        sessions.clear();
        sessionsByWeek.clear();

        if (newSessions != null) {
            sessions.addAll(newSessions);

            // Grouper les sessions par semaine
            groupSessionsByWeek();

            // Trier les sessions par date
            Collections.sort(sessions, new Comparator<Session>() {
                @Override
                public int compare(Session s1, Session s2) {
                    if (s1.getStartTime() == null) return 1;
                    if (s2.getStartTime() == null) return -1;
                    return s1.getStartTime().compareTo(s2.getStartTime());
                }
            });
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        updateEmptyState();
    }

    /**
     * Grouper les sessions par semaine du mois
     */
    private void groupSessionsByWeek() {
        for (Session session : sessions) {
            if (session.getStartTime() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(session.getStartTime().toDate());

                int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH);
                String weekKey = "Semaine " + weekOfMonth;

                if (!sessionsByWeek.containsKey(weekKey)) {
                    sessionsByWeek.put(weekKey, new ArrayList<>());
                }

                sessionsByWeek.get(weekKey).add(session);
            }
        }
    }

    /**
     * Mettre à jour l'affichage de l'état vide
     */
    private void updateEmptyState() {
        boolean isEmpty = sessions.isEmpty();

        if (recyclerView != null && layoutEmptyState != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        if (isEmpty && tvEmptyMessage != null) {
            Calendar calendar = Calendar.getInstance();
            String monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.FRENCH);

            tvEmptyMessage.setText(String.format(
                    "Aucun cours programmé en %s\n\n" +
                            "Votre emploi du temps pour ce mois est libre.\n" +
                            "Consultez les autres mois ou vérifiez avec l'administration.",
                    monthName != null ? monthName : "ce mois"));
        }

        Log.d(TAG, "Empty state updated - isEmpty: " + isEmpty);
    }

    /**
     * Obtenir le nombre de sessions du mois
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * Obtenir les sessions d'une semaine spécifique
     */
    public List<Session> getSessionsForWeek(String weekKey) {
        return sessionsByWeek.get(weekKey);
    }

    /**
     * Obtenir le nombre de semaines avec des cours
     */
    public int getActiveWeeksCount() {
        return sessionsByWeek.size();
    }

    /**
     * Obtenir des statistiques du mois
     */
    public String getMonthStats() {
        int totalSessions = sessions.size();
        int activeWeeks = getActiveWeeksCount();

        Map<String, Integer> courseCount = new HashMap<>();
        for (Session session : sessions) {
            String courseName = session.getCourseName();
            courseCount.put(courseName, courseCount.getOrDefault(courseName, 0) + 1);
        }

        return String.format("📅 %d cours sur %d semaines • %d matières",
                totalSessions, activeWeeks, courseCount.size());
    }

    /**
     * Obtenir la répartition des cours par matière
     */
    public Map<String, Integer> getCourseDistribution() {
        Map<String, Integer> distribution = new HashMap<>();

        for (Session session : sessions) {
            String courseName = session.getCourseName();
            distribution.put(courseName, distribution.getOrDefault(courseName, 0) + 1);
        }

        return distribution;
    }

    /**
     * Obtenir les prochaines sessions du mois
     */
    public List<Session> getUpcomingSessions() {
        List<Session> upcoming = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (Session session : sessions) {
            if (session.getStartTime() != null &&
                    session.getStartTime().toDate().getTime() > currentTime) {
                upcoming.add(session);
            }
        }

        // Limiter aux 5 prochaines sessions
        if (upcoming.size() > 5) {
            upcoming = upcoming.subList(0, 5);
        }

        return upcoming;
    }

    /**
     * Vérifier s'il y a des sessions aujourd'hui
     */
    public boolean hasSessionsToday() {
        Calendar today = Calendar.getInstance();

        for (Session session : sessions) {
            if (session.getStartTime() != null) {
                Calendar sessionCal = Calendar.getInstance();
                sessionCal.setTime(session.getStartTime().toDate());

                if (today.get(Calendar.YEAR) == sessionCal.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == sessionCal.get(Calendar.DAY_OF_YEAR)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed with " + sessions.size() + " sessions");
        updateMonthInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Fragment view destroyed");
    }
}