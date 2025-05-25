package com.example.attendancesystem.utils;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;
import com.example.attendancesystem.models.Justification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminJustificationAdapter extends RecyclerView.Adapter<AdminJustificationAdapter.JustificationViewHolder> {

    private List<Justification> justifications;
    private OnJustificationActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnJustificationActionListener {
        void onApproveClick(Justification justification, String comments, String reason);
        void onRejectClick(Justification justification, String comments, String reason);
    }

    public AdminJustificationAdapter(List<Justification> justifications, OnJustificationActionListener listener) {
        this.justifications = justifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JustificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_justification_admin, parent, false);
        return new JustificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JustificationViewHolder holder, int position) {
        Justification justification = justifications.get(position);
        holder.bind(justification);
    }

    @Override
    public int getItemCount() {
        return justifications.size();
    }

    public class JustificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName, tvCourseName, tvJustificationDate, tvReason, tvDescription, tvStatus;
        Button btnApprove, btnReject, btnConfirmAction;
        LinearLayout layoutAdminActions, layoutAdminInput;
        EditText etAdminComments, etAdminReason;

        public JustificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tv_student_name_admin);
            tvCourseName = itemView.findViewById(R.id.tv_course_name_admin);
            tvJustificationDate = itemView.findViewById(R.id.tv_justification_date_admin);
            tvReason = itemView.findViewById(R.id.tv_reason_admin);
            tvDescription = itemView.findViewById(R.id.tv_description_admin);
            tvStatus = itemView.findViewById(R.id.tv_status_admin);
            btnApprove = itemView.findViewById(R.id.btn_approve);
            btnReject = itemView.findViewById(R.id.btn_reject);
            btnConfirmAction = itemView.findViewById(R.id.btn_confirm_action);
            layoutAdminActions = itemView.findViewById(R.id.layout_admin_actions);
            layoutAdminInput = itemView.findViewById(R.id.layout_admin_input);
            etAdminComments = itemView.findViewById(R.id.et_admin_comments);
            etAdminReason = itemView.findViewById(R.id.et_admin_reason);

            // Toggle description visibility
            tvDescription.setOnClickListener(v -> {
                if (tvDescription.getMaxLines() == 2) {
                    tvDescription.setMaxLines(Integer.MAX_VALUE);
                    tvDescription.setEllipsize(null);
                } else {
                    tvDescription.setMaxLines(2);
                    tvDescription.setEllipsize(TextUtils.TruncateAt.END);
                }
            });
        }

        public void bind(Justification justification) {
            tvStudentName.setText("Étudiant: " + justification.getStudentName());
            tvCourseName.setText("Cours: " + justification.getCourseName());
            tvJustificationDate.setText("Date: " + dateFormat.format(justification.getJustificationDate()));
            tvReason.setText("Raison: " + justification.getReason());
            tvDescription.setText("Description: " + justification.getDescription());
            tvStatus.setText("Statut: " + justification.getStatus());

            // Set status text color
            switch (justification.getStatus()) {
                case "submitted":
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.orange_500));
                    break;
                case "approved":
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.green_700));
                    break;
                case "rejected":
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.red_700));
                    break;
                case "under_review":
                    tvStatus.setTextColor(itemView.getContext().getColor(R.color.blue_500));
                    break;
                default:
                    tvStatus.setTextColor(itemView.getContext().getColor(android.R.color.black));
                    break;
            }

            // Show/Hide action buttons based on status
            if (justification.checkStatusIsSubmitted() || justification.checkStatusIsUnderReview()) {
                layoutAdminActions.setVisibility(View.VISIBLE);
                layoutAdminInput.setVisibility(View.GONE); // Hide input fields initially
                etAdminComments.setText(""); // Clear fields on re-bind
                etAdminReason.setText("");
            } else {
                layoutAdminActions.setVisibility(View.GONE);
                layoutAdminInput.setVisibility(View.GONE);
            }

            // Set up click listeners for action buttons
            btnApprove.setOnClickListener(v -> {
                // Show input fields for comments/reason, hide action buttons
                layoutAdminActions.setVisibility(View.GONE);
                layoutAdminInput.setVisibility(View.VISIBLE);
                btnConfirmAction.setOnClickListener(confirmView -> {
                    String comments = etAdminComments.getText().toString();
                    String approvalReason = etAdminReason.getText().toString();
                    if (approvalReason.isEmpty()) {
                        Toast.makeText(itemView.getContext(), "Veuillez fournir une raison d'approbation.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (listener != null) {
                        listener.onApproveClick(justification, comments, approvalReason);
                    }
                    layoutAdminInput.setVisibility(View.GONE); // Hide input after action
                });
            });

            btnReject.setOnClickListener(v -> {
                // Show input fields for comments/reason, hide action buttons
                layoutAdminActions.setVisibility(View.GONE);
                layoutAdminInput.setVisibility(View.VISIBLE);
                btnConfirmAction.setOnClickListener(confirmView -> {
                    String comments = etAdminComments.getText().toString();
                    String rejectReason = etAdminReason.getText().toString();
                    if (rejectReason.isEmpty()) {
                        Toast.makeText(itemView.getContext(), "Veuillez fournir une raison de rejet.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (listener != null) {
                        listener.onRejectClick(justification, comments, rejectReason);
                    }
                    layoutAdminInput.setVisibility(View.GONE); // Hide input after action
                });
            });
        }
    }
}