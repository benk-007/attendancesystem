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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.app.AlertDialog;
import android.content.Context;

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
            String statusIcon = getStatusIcon(attendance.getStatus());
            tvCourseName.setText(attendance.getCourseName());

            // Date et heure DÉTAILLÉE
            if (attendance.getTimestamp() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH);
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.FRENCH);

                Date attendanceDate = attendance.getTimestamp().toDate();
                String dateStr = dateFormat.format(attendanceDate);
                String timeStr = timeFormat.format(attendanceDate);

                tvDateTime.setText(String.format("%s\n🕐 %s",
                        capitalizeFirst(dateStr), timeStr));
            } else {
                tvDateTime.setText("Date non disponible");
            }

            // Statut
            String status = attendance.getStatus();
            String statusText = getStatusDisplayText(status);
            tvStatus.setText(getStatusDisplayText(status));

            int statusColor = getStatusColor(status);
            tvStatus.setTextColor(ContextCompat.getColor(context, statusColor));
            statusIndicator.setBackgroundColor(ContextCompat.getColor(context, statusColor));

            // Informations détaillées de confiance
            if (attendance.isManualEntry()) {
                tvConfidence.setText("✏️ Saisie manuelle");
                tvConfidence.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            } else {
                double confidence = attendance.getConfidence();
                String confidenceText = String.format(Locale.getDefault(),
                        "🤖 Reconnaissance: %.0f%%", confidence * 100);
                tvConfidence.setText(confidenceText);

                // Couleur selon le niveau de confiance
                int confidenceColor;
                if (confidence >= 0.9) {
                    confidenceColor = R.color.success_color;
                } else if (confidence >= 0.7) {
                    confidenceColor = R.color.warning_color;
                } else {
                    confidenceColor = R.color.error_color;
                }
                tvConfidence.setTextColor(ContextCompat.getColor(context, confidenceColor));
            }
            // NOUVEAU: Afficher les détails supplémentaires au clic
            itemView.setOnClickListener(v -> showAttendanceDetails(attendance));
        }

        // Méthodes helper
        private String getStatusIcon(String status) {
            switch (status) {
                case "present": return "✅";
                case "absent": return "❌";
                case "justified": return "📋";
                default: return "❓";
            }
        }

        private String capitalizeFirst(String text) {
            if (text == null || text.isEmpty()) return text;
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }

        // NOUVEAU: Dialog avec détails complets
        private void showAttendanceDetails(Attendance attendance) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            // Créer le contenu détaillé
            StringBuilder details = new StringBuilder();
            details.append("📚 Cours: ").append(attendance.getCourseName()).append("\n\n");

            if (attendance.getTimestamp() != null) {
                SimpleDateFormat fullFormat = new SimpleDateFormat("EEEE dd MMMM yyyy 'à' HH:mm:ss", Locale.FRENCH);
                details.append("📅 Date/Heure: ").append(fullFormat.format(attendance.getTimestamp().toDate())).append("\n\n");
            }

            details.append("📊 Statut: ").append(getStatusDisplayText(attendance.getStatus())).append("\n\n");

            if (!attendance.isManualEntry()) {
                details.append("🤖 Confiance: ").append(String.format("%.1f%%", attendance.getConfidence() * 100)).append("\n\n");

                if (attendance.getAttendanceDetails() != null) {
                    Attendance.AttendanceDetails ad = attendance.getAttendanceDetails();
                    details.append("⚙️ Détails techniques:\n");
                    details.append("  • Temps de traitement: ").append(ad.getProcessingTime()).append("ms\n");
                    details.append("  • Tentatives: ").append(ad.getRetryCount()).append("\n");
                    details.append("  • Lieu: ").append(ad.getLocation()).append("\n\n");
                }
            } else {
                details.append("✏️ Saisie manuelle\n");
                if (attendance.getModifiedBy() != null) {
                    details.append("👤 Modifié par: ").append(attendance.getModifiedBy()).append("\n");
                }
                if (attendance.getModificationReason() != null) {
                    details.append("📝 Raison: ").append(attendance.getModificationReason()).append("\n");
                }
                details.append("\n");
            }

            if (attendance.getSessionId() != null) {
                details.append("🔗 ID Session: ").append(attendance.getSessionId()).append("\n");
            }

            builder.setTitle("Détails de la Présence")
                    .setMessage(details.toString())
                    .setPositiveButton("Fermer", null)
                    .show();
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