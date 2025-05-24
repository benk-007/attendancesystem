package com.example.attendancesystem.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Justification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class JustificationAdapter extends RecyclerView.Adapter<JustificationAdapter.JustificationViewHolder> {

    private List<Justification> justificationsList;
    private boolean isAdminView; // Flag to determine if it's an admin view

    // Date formatter for display
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());


    // Constructor
    public JustificationAdapter(List<Justification> justificationsList, boolean isAdminView) {
        this.justificationsList = justificationsList;
        this.isAdminView = isAdminView;
    }

    @NonNull
    @Override
    public JustificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for a single item in the RecyclerView
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_justification, parent, false); // You'll create this layout
        return new JustificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JustificationViewHolder holder, int position) {
        Justification justification = justificationsList.get(position);

        holder.tvCourseName.setText(justification.getCourseName());
        holder.tvJustificationReason.setText(justification.getReason());
        holder.tvJustificationDescription.setText(justification.getDescription());
        holder.tvSubmittedDate.setText("Soumis le: " + dateTimeFormatter.format(justification.getSubmittedAt().toDate()));
        holder.tvJustificationAbsenceDate.setText("Date d'absence: " + dateFormatter.format(justification.getJustificationDate()));

        // Set status text and color
        String status = justification.getStatus();
        holder.tvStatus.setText(status);
        switch (status) {
            case "approved":
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green_approved)); // Define this color
                break;
            case "rejected":
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_rejected)); // Define this color
                break;
            case "submitted":
            case "under_review":
            default:
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue_pending)); // Define this color
                break;
        }

        // Show/hide admin-specific fields
        if (isAdminView) {
            holder.tvStudentName.setVisibility(View.VISIBLE);
            holder.tvStudentName.setText("Étudiant: " + justification.getStudentName());
            holder.tvReviewedBy.setVisibility(View.VISIBLE);
            holder.tvReviewedAt.setVisibility(View.VISIBLE);
            holder.tvReviewComments.setVisibility(View.VISIBLE);
            holder.tvApprovalReason.setVisibility(View.VISIBLE);

            if (justification.getReviewedBy() != null && !justification.getReviewedBy().isEmpty()) {
                holder.tvReviewedBy.setText("Révisé par: " + justification.getReviewedBy());
                holder.tvReviewedAt.setText("Le: " + dateTimeFormatter.format(justification.getReviewedAt().toDate()));
                holder.tvReviewComments.setText("Commentaires: " + (justification.getReviewComments() != null ? justification.getReviewComments() : "N/A"));
                holder.tvApprovalReason.setText("Raison décision: " + (justification.getApprovalReason() != null ? justification.getApprovalReason() : "N/A"));
            } else {
                holder.tvReviewedBy.setText("Pas encore révisé");
                holder.tvReviewedAt.setText("");
                holder.tvReviewComments.setText("");
                holder.tvApprovalReason.setText("");
            }
        } else {
            // Hide for student view
            holder.tvStudentName.setVisibility(View.GONE);
            holder.tvReviewedBy.setVisibility(View.GONE);
            holder.tvReviewedAt.setVisibility(View.GONE);
            holder.tvReviewComments.setVisibility(View.GONE);
            holder.tvApprovalReason.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return justificationsList.size();
    }

    // ViewHolder class
    public static class JustificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName;
        TextView tvJustificationReason;
        TextView tvJustificationDescription;
        TextView tvSubmittedDate;
        TextView tvJustificationAbsenceDate;
        TextView tvStatus;
        TextView tvStudentName; // Only for admin
        TextView tvReviewedBy;  // Only for admin
        TextView tvReviewedAt;  // Only for admin
        TextView tvReviewComments; // Only for admin
        TextView tvApprovalReason; // Only for admin


        public JustificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_justification_course_name);
            tvJustificationReason = itemView.findViewById(R.id.tv_justification_reason);
            tvJustificationDescription = itemView.findViewById(R.id.tv_justification_description);
            tvSubmittedDate = itemView.findViewById(R.id.tv_justification_submitted_date);
            tvJustificationAbsenceDate = itemView.findViewById(R.id.tv_justification_absence_date);
            tvStatus = itemView.findViewById(R.id.tv_justification_status);

            // Admin-specific views (initialized but visibility controlled in onBindViewHolder)
            tvStudentName = itemView.findViewById(R.id.tv_justification_student_name);
            tvReviewedBy = itemView.findViewById(R.id.tv_justification_reviewed_by);
            tvReviewedAt = itemView.findViewById(R.id.tv_justification_reviewed_at);
            tvReviewComments = itemView.findViewById(R.id.tv_justification_review_comments);
            tvApprovalReason = itemView.findViewById(R.id.tv_justification_approval_reason);
        }
    }

    // You can add methods here to update the list if needed, e.g.:
    public void updateJustifications(List<Justification> newJustifications) {
        this.justificationsList.clear();
        this.justificationsList.addAll(newJustifications);
        notifyDataSetChanged();
    }
}
