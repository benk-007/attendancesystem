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
import java.util.Map;

/**
 * Fragment pour afficher l'emploi du temps de la semaine
 */
public class WeekScheduleFragment extends Fragment {

    private static final String TAG = "WeekScheduleFragment";

    // Views
    private RecyclerView recyclerView;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    private TextView tvWeekInfo;

    // Data
    private ScheduleAdapter adapter;
    private List<Session> sessions;
    private Map<String, List<Session>> sessionsByDay;

    public WeekScheduleFragment() {
        // Required empty public constructor
        sessions = new ArrayList<>();
        sessionsByDay = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_week_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        updateWeekInfo();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_week_schedule);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state_week);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message_week);
        tvWeekInfo = view.findViewById(R.id.tv_week_info);
    }

    private void setupRecyclerView() {
        adapter = new ScheduleAdapter(sessions, ScheduleAdapter.VIEW_TYPE_WEEK);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    private void updateWeekInfo() {
        if (tvWeekInfo != null) {
            Calendar calendar = Calendar.getInstance();

            // Début de la semaine (lundi)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            String startWeek = Utils.formatDate(new com.google.firebase.Timestamp(calendar.getTime()));

            // Fin de la semaine (dimanche)
            calendar.add(Calendar.DAY_OF_WEEK, 6);
            String endWeek = Utils.formatDate(new com.google.firebase.Timestamp(calendar.getTime()));

            String weekInfo = String.format("Semaine du %s au %s", startWeek, endWeek);
            tvWeekInfo.setText(weekInfo);
        }
    }

    /**
     * Mettre à jour l'emploi du temps de la semaine
     */
    public void updateSchedule(List<Session> newSessions) {
        Log.d(TAG, "Updating week schedule with " + (newSessions != null ? newSessions.size() : 0) + " sessions");

        sessions.clear();
        sessionsByDay.clear();

        if (newSessions != null) {
            sessions.addAll(newSessions);

            // Grouper les sessions par jour
            groupSessionsByDay();

            // Trier les sessions par jour puis par heure
            Collections.sort(sessions, new Comparator<Session>() {
                @Override
                public int compare(Session s1, Session s2) {
                    if (s1.getStartTime() == null) return 1;
                    if (s2.getStartTime() == null) return -1;

                    // Comparer d'abord par jour
                    Calendar cal1 = Calendar.getInstance();
                    Calendar cal2 = Calendar.getInstance();
                    cal1.setTime(s1.getStartTime().toDate());
                    cal2.setTime(s2.getStartTime().toDate());

                    int dayCompare = Integer.compare(cal1.get(Calendar.DAY_OF_YEAR),
                            cal2.get(Calendar.DAY_OF_YEAR));
                    if (dayCompare != 0) {
                        return dayCompare;
                    }

                    // Puis par heure
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
     * Grouper les sessions par jour de la semaine
     */
    private void groupSessionsByDay() {
        String[] daysOfWeek = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};

        for (String day : daysOfWeek) {
            sessionsByDay.put(day, new ArrayList<>());
        }

        for (Session session : sessions) {
            if (session.getStartTime() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(session.getStartTime().toDate());

                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                String dayName;

                switch (dayOfWeek) {
                    case Calendar.MONDAY: dayName = "Lundi"; break;
                    case Calendar.TUESDAY: dayName = "Mardi"; break;
                    case Calendar.WEDNESDAY: dayName = "Mercredi"; break;
                    case Calendar.THURSDAY: dayName = "Jeudi"; break;
                    case Calendar.FRIDAY: dayName = "Vendredi"; break;
                    case Calendar.SATURDAY: dayName = "Samedi"; break;
                    case Calendar.SUNDAY: dayName = "Dimanche"; break;
                    default: dayName = "Lundi"; break;
                }

                sessionsByDay.get(dayName).add(session);
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
            tvEmptyMessage.setText("Aucun cours programmé cette semaine\n\n" +
                    "Votre emploi du temps pour cette semaine est libre.\n" +
                    "Consultez les autres semaines ou contactez l'administration.");
        }

        Log.d(TAG, "Empty state updated - isEmpty: " + isEmpty);
    }

    /**
     * Obtenir le nombre de sessions de la semaine
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * Obtenir les sessions d'un jour spécifique
     */
    public List<Session> getSessionsForDay(String dayName) {
        return sessionsByDay.get(dayName);
    }

    /**
     * Obtenir le nombre de jours avec des cours
     */
    public int getActiveDaysCount() {
        int count = 0;
        for (List<Session> daySessions : sessionsByDay.values()) {
            if (!daySessions.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Obtenir des statistiques de la semaine
     */
    public String getWeekStats() {
        int totalSessions = sessions.size();
        int activeDays = getActiveDaysCount();

        return String.format("📅 %d cours sur %d jours", totalSessions, activeDays);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed with " + sessions.size() + " sessions");
        updateWeekInfo();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Fragment view destroyed");
    }
}