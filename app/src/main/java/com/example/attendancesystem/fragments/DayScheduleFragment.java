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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fragment pour afficher l'emploi du temps d'aujourd'hui
 */
public class DayScheduleFragment extends Fragment {

    private static final String TAG = "DayScheduleFragment";

    // Views
    private RecyclerView recyclerView;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;

    // Data
    private ScheduleAdapter adapter;
    private List<Session> sessions;

    public DayScheduleFragment() {
        // Required empty public constructor
        sessions = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_day_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_day_schedule);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state_day);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message_day);
    }

    private void setupRecyclerView() {
        adapter = new ScheduleAdapter(sessions, ScheduleAdapter.VIEW_TYPE_DAY);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    /**
     * Mettre à jour l'emploi du temps du jour
     */
    public void updateSchedule(List<Session> newSessions) {
        Log.d(TAG, "Updating day schedule with " + (newSessions != null ? newSessions.size() : 0) + " sessions");

        sessions.clear();
        if (newSessions != null) {
            sessions.addAll(newSessions);

            // Trier les sessions par heure de début
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
     * Mettre à jour l'affichage de l'état vide
     */
    private void updateEmptyState() {
        boolean isEmpty = sessions.isEmpty();

        if (recyclerView != null && layoutEmptyState != null) {
            recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        if (isEmpty && tvEmptyMessage != null) {
            tvEmptyMessage.setText("Aucun cours programmé aujourd'hui\n\n" +
                    "Votre emploi du temps pour aujourd'hui est libre.\n" +
                    "Vérifiez les autres jours de la semaine.");
        }

        Log.d(TAG, "Empty state updated - isEmpty: " + isEmpty);
    }

    /**
     * Obtenir le nombre de sessions d'aujourd'hui
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * Vérifier s'il y a des sessions en cours
     */
    public boolean hasActiveSessions() {
        for (Session session : sessions) {
            if (session.isActive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtenir la prochaine session de la journée
     */
    public Session getNextSession() {
        long currentTime = System.currentTimeMillis();

        for (Session session : sessions) {
            if (session.getStartTime() != null &&
                    session.getStartTime().toDate().getTime() > currentTime) {
                return session;
            }
        }

        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Fragment resumed with " + sessions.size() + " sessions");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "Fragment view destroyed");
    }
}