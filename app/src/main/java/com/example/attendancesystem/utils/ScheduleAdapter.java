package com.example.attendancesystem.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Session;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Adapter pour afficher les sessions dans l'emploi du temps
 * Supporte différents types d'affichage : jour, semaine, mois
 */
public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private static final String TAG = "ScheduleAdapter";

    // Types d'affichage
    public static final int VIEW_TYPE_DAY = 1;
    public static final int VIEW_TYPE_WEEK = 2;
    public static final int VIEW_TYPE_MONTH = 3;

    private List<Session> sessions;
    private Context context;
    private int viewType;
    private OnSessionClickListener listener;

    // Formatters de date
    private SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.FRENCH);
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM", Locale.FRENCH);
    private SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE", Locale.FRENCH);

    /**
     * Interface pour gérer les clics sur les sessions
     */
    public interface OnSessionClickListener {
        void onSessionClick(Session session);
        void onSessionLongClick(Session session);
    }

    public ScheduleAdapter(List<Session> sessions, int viewType) {
        this.sessions = sessions;
        this.viewType = viewType;
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule_session, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Session session = sessions.get(position);
        holder.bind(session, viewType);
    }

    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * ViewHolder pour les sessions
     */
    class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCourseName;
        private TextView tvTeacherName;
        private TextView tvTime;
        private TextView tvRoom;
        private TextView tvStatus;
        private TextView tvDayInfo;
        private View statusIndicator;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCourseName = itemView.findViewById(R.id.tv_course_name_schedule);
            tvTeacherName = itemView.findViewById(R.id.tv_teacher_name_schedule);
            tvTime = itemView.findViewById(R.id.tv_time_schedule);
            tvRoom = itemView.findViewById(R.id.tv_room_schedule);
            tvStatus = itemView.findViewById(R.id.tv_status_schedule);
            tvDayInfo = itemView.findViewById(R.id.tv_day_info_schedule);
            statusIndicator = itemView.findViewById(R.id.status_indicator_schedule);

            // Listeners pour les clics
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onSessionClick(sessions.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onSessionLongClick(sessions.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }

        public void bind(Session session, int displayType) {
            // Nom du cours
            tvCourseName.setText(session.getCourseName());

            // Nom de l'enseignant
            tvTeacherName.setText(session.getTeacherName());

            // Salle
            tvRoom.setText(session.getRoom() != null ? session.getRoom() : "Salle TBD");

            // Affichage du temps selon le type
            displayTimeInfo(session, displayType);

            // Statut de la session
            displayStatus(session);

            // Informations sur le jour (pour vue semaine/mois)
            displayDayInfo(session, displayType);
        }

        private void displayTimeInfo(Session session, int displayType) {
            if (session.getStartTime() == null || session.getEndTime() == null) {
                tvTime.setText("Horaire à définir");
                return;
            }

            String startTime = timeFormatter.format(session.getStartTime().toDate());
            String endTime = timeFormatter.format(session.getEndTime().toDate());

            switch (displayType) {
                case VIEW_TYPE_DAY:
                    // Pour le jour : juste l'heure
                    tvTime.setText(String.format("%s - %s", startTime, endTime));
                    break;

                case VIEW_TYPE_WEEK:
                    // Pour la semaine : jour + heure
                    String dayName = dayFormatter.format(session.getStartTime().toDate());
                    tvTime.setText(String.format("%s %s-%s",
                            capitalizeFirst(dayName), startTime, endTime));
                    break;

                case VIEW_TYPE_MONTH:
                    // Pour le mois : date + heure
                    String date = dateFormatter.format(session.getStartTime().toDate());
                    tvTime.setText(String.format("%s %s-%s", date, startTime, endTime));
                    break;
            }
        }

        private void displayStatus(Session session) {
            String status = session.getStatus();
            String statusText;
            int statusColor;

            switch (status) {
                case "active":
                    statusText = "En cours";
                    statusColor = R.color.success_color;
                    break;
                case "completed":
                    statusText = "Terminé";
                    statusColor = R.color.text_secondary;
                    break;
                case "cancelled":
                    statusText = "Annulé";
                    statusColor = R.color.error_color;
                    break;
                case "scheduled":
                default:
                    statusText = "Programmé";
                    statusColor = R.color.primary_color;
                    break;
            }

            tvStatus.setText(statusText);
            tvStatus.setTextColor(ContextCompat.getColor(context, statusColor));
            statusIndicator.setBackgroundColor(ContextCompat.getColor(context, statusColor));
        }

        private void displayDayInfo(Session session, int displayType) {
            if (displayType == VIEW_TYPE_DAY) {
                // Pour la vue jour, pas besoin d'info supplémentaire
                tvDayInfo.setVisibility(View.GONE);
            } else {
                tvDayInfo.setVisibility(View.VISIBLE);

                if (session.getField() != null && session.getDepartment() != null) {
                    String info = String.format("%s • %s",
                            session.getField(), session.getDepartment());
                    tvDayInfo.setText(info);
                } else {
                    tvDayInfo.setText(session.getDepartment());
                }
            }
        }

        private String capitalizeFirst(String text) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        }
    }

    /**
     * Mettre à jour la liste des sessions
     */
    public void updateSessions(List<Session> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }

    /**
     * Obtenir une session à une position donnée
     */
    public Session getSession(int position) {
        if (position >= 0 && position < sessions.size()) {
            return sessions.get(position);
        }
        return null;
    }

    /**
     * Vérifier s'il y a des sessions actives
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
     * Obtenir le nombre de sessions par statut
     */
    public int getSessionCountByStatus(String status) {
        int count = 0;
        for (Session session : sessions) {
            if (status.equals(session.getStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Vérifier si une session est aujourd'hui
     */
    private boolean isToday(Session session) {
        if (session.getStartTime() == null) return false;

        Calendar today = Calendar.getInstance();
        Calendar sessionCal = Calendar.getInstance();
        sessionCal.setTime(session.getStartTime().toDate());

        return today.get(Calendar.YEAR) == sessionCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == sessionCal.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Vérifier si une session est en cours
     */
    private boolean isCurrentlyActive(Session session) {
        if (session.getStartTime() == null || session.getEndTime() == null) return false;

        long currentTime = System.currentTimeMillis();
        long startTime = session.getStartTime().toDate().getTime();
        long endTime = session.getEndTime().toDate().getTime();

        return currentTime >= startTime && currentTime <= endTime;
    }
}