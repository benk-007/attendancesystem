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
import com.example.attendancesystem.models.Attendance;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.AttendanceViewHolder> {

    private List<Attendance> attendanceList;
    private Context context;
    private OnAttendanceClickListener listener;

    // Interface pour gérer les clics
    public interface OnAttendanceClickListener {
        void onAttendanceClick(Attendance attendance);
    }

    public AttendanceHistoryAdapter(List<Attendance> attendanceList) {
        this.attendanceList = attendanceList;
    }

    public void setOnAttendanceClickListener(OnAttendanceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_attendance_history, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Attendance attendance = attendanceList.get(position);
        holder.bind(attendance);
    }

    @Override
    public int getItemCount() {
        return attendanceList != null ? attendanceList.size() : 0;
    }

    public void updateAttendanceList(List<Attendance> newAttendanceList) {
        this.attendanceList = newAttendanceList;
        notifyDataSetChanged();
    }

    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCourseName, tvDateTime, tvStatus, tvConfidence;
        private View statusIndicator;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvConfidence = itemView.findViewById(R.id.tv_confidence);
            statusIndicator = itemView.findViewById(R.id.status_indicator);

            // Listener pour les clics
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onAttendanceClick(attendanceList.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Attendance attendance) {
            // Nom du cours
            tvCourseName.setText(attendance.getCourseName());

            // Date et heure
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            if (attendance.getTimestamp() != null) {
                tvDateTime.setText(dateTimeFormat.format(attendance.getTimestamp().toDate()));
            } else {
                tvDateTime.setText("Date non disponible");
            }

            // Statut
            String status = attendance.getStatus();
            tvStatus.setText(getStatusDisplayText(status));

            int statusColor = getStatusColor(status);
            tvStatus.setTextColor(ContextCompat.getColor(context, statusColor));
            statusIndicator.setBackgroundColor(ContextCompat.getColor(context, statusColor));

            // Score de confiance (seulement pour les pointages automatiques)
            if (attendance.isManualEntry()) {
                tvConfidence.setText("Saisie manuelle");
                tvConfidence.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            } else {
                double confidence = attendance.getConfidence();
                String confidenceText = String.format(Locale.getDefault(), "Confiance: %.0f%%", confidence * 100);
                tvConfidence.setText(confidenceText);

                // Couleur selon le niveau de confiance
                int confidenceColor;
                if (confidence >= 0.8) {
                    confidenceColor = R.color.success_color;
                } else if (confidence >= 0.6) {
                    confidenceColor = R.color.warning_color;
                } else {
                    confidenceColor = R.color.error_color;
                }
                tvConfidence.setTextColor(ContextCompat.getColor(context, confidenceColor));
            }
        }

        private String getStatusDisplayText(String status) {
            switch (status) {
                case "present":
                    return "Présent";
                case "absent":
                    return "Absent";
                case "justified":
                    return "Justifié";
                default:
                    return status;
            }
        }

        private int getStatusColor(String status) {
            switch (status) {
                case "present":
                    return R.color.present_color;
                case "absent":
                    return R.color.absent_color;
                case "justified":
                    return R.color.justified_color;
                default:
                    return R.color.text_secondary;
            }
        }
    }
}