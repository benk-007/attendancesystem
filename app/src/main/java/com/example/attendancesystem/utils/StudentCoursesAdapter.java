package com.example.attendancesystem.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendancesystem.R;

import java.util.List;
import java.util.Map;

/**
 * Adapter pour afficher la liste des cours auxquels l'étudiant est rattaché
 */
public class StudentCoursesAdapter extends RecyclerView.Adapter<StudentCoursesAdapter.CourseViewHolder> {

    private List<Map<String, String>> courses;
    private Context context;
    private OnCourseClickListener listener;

    // Interface pour gérer les clics sur les cours
    public interface OnCourseClickListener {
        void onCourseClick(Map<String, String> course);
        void onCourseInfoClick(Map<String, String> course);
    }

    public StudentCoursesAdapter(List<Map<String, String>> courses) {
        this.courses = courses;
    }

    public void setOnCourseClickListener(OnCourseClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_student_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Map<String, String> course = courses.get(position);
        holder.bind(course);
    }

    @Override
    public int getItemCount() {
        return courses != null ? courses.size() : 0;
    }

    class CourseViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCourseName, tvCourseCode, tvTeacherName, tvDepartment, tvSchedule;
        private ImageView ivCourseIcon, ivInfoIcon;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tv_course_name);
            tvCourseCode = itemView.findViewById(R.id.tv_course_code);
            tvTeacherName = itemView.findViewById(R.id.tv_teacher_name);
            tvDepartment = itemView.findViewById(R.id.tv_department);
            tvSchedule = itemView.findViewById(R.id.tv_schedule);
            ivCourseIcon = itemView.findViewById(R.id.iv_course_icon);
            ivInfoIcon = itemView.findViewById(R.id.iv_info_icon);

            // Listeners pour les clics
            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCourseClick(courses.get(getAdapterPosition()));
                }
            });

            ivInfoIcon.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onCourseInfoClick(courses.get(getAdapterPosition()));
                }
            });
        }

        public void bind(Map<String, String> course) {
            // Nom du cours
            String courseName = course.get("name");
            tvCourseName.setText(courseName != null ? courseName : "Cours sans nom");

            // Code/ID du cours
            String courseId = course.get("id");
            tvCourseCode.setText(courseId != null ? "ID: " + courseId : "ID non défini");

            // Nom de l'enseignant
            String teacherName = course.get("teacherName");
            if (teacherName != null && !teacherName.isEmpty()) {
                tvTeacherName.setText("👨‍🏫 " + teacherName);
                tvTeacherName.setVisibility(View.VISIBLE);
            } else {
                tvTeacherName.setVisibility(View.GONE);
            }

            // Département
            String department = course.get("department");
            if (department != null && !department.isEmpty()) {
                tvDepartment.setText("🏢 " + department);
                tvDepartment.setVisibility(View.VISIBLE);
            } else {
                tvDepartment.setVisibility(View.GONE);
            }

            // Horaire (si disponible)
            String schedule = course.get("schedule");
            if (schedule != null && !schedule.isEmpty()) {
                tvSchedule.setText("🕒 " + schedule);
                tvSchedule.setVisibility(View.VISIBLE);
            } else {
                // Construire un horaire basique s'il y a des infos séparées
                String dayOfWeek = course.get("dayOfWeek");
                String timeSlot = course.get("timeSlot");
                String room = course.get("room");

                if (dayOfWeek != null || timeSlot != null || room != null) {
                    StringBuilder scheduleBuilder = new StringBuilder("🕒 ");
                    if (dayOfWeek != null) {
                        scheduleBuilder.append(capitalizeDay(dayOfWeek)).append(" ");
                    }
                    if (timeSlot != null) {
                        scheduleBuilder.append(timeSlot).append(" ");
                    }
                    if (room != null) {
                        scheduleBuilder.append("(").append(room).append(")");
                    }

                    tvSchedule.setText(scheduleBuilder.toString().trim());
                    tvSchedule.setVisibility(View.VISIBLE);
                } else {
                    tvSchedule.setVisibility(View.GONE);
                }
            }

            // Icône du cours (basée sur le département ou type)
            setCourseIcon(department);
        }

        /**
         * Définir l'icône du cours selon le département
         */
        private void setCourseIcon(String department) {
            int iconRes;
            int tintColor;

            if (department != null) {
                switch (department.toLowerCase()) {
                    case "informatique":
                    case "computer science":
                        iconRes = R.drawable.ic_badge;
                        tintColor = R.color.primary_color;
                        break;
                    case "mathématiques":
                    case "mathematics":
                        iconRes = R.drawable.ic_calendar;
                        tintColor = R.color.accent_color;
                        break;
                    case "physique":
                    case "physics":
                        iconRes = R.drawable.ic_info;
                        tintColor = R.color.info_color;
                        break;
                    default:
                        iconRes = R.drawable.ic_badge;
                        tintColor = R.color.text_secondary;
                        break;
                }
            } else {
                iconRes = R.drawable.ic_badge;
                tintColor = R.color.text_secondary;
            }

            ivCourseIcon.setImageResource(iconRes);
            ivCourseIcon.setColorFilter(context.getColor(tintColor));
        }

        /**
         * Capitaliser le nom du jour
         */
        private String capitalizeDay(String day) {
            if (day == null || day.isEmpty()) return day;

            switch (day.toLowerCase()) {
                case "monday": return "Lundi";
                case "tuesday": return "Mardi";
                case "wednesday": return "Mercredi";
                case "thursday": return "Jeudi";
                case "friday": return "Vendredi";
                case "saturday": return "Samedi";
                case "sunday": return "Dimanche";
                default: return day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase();
            }
        }
    }

    /**
     * Mettre à jour la liste des cours
     */
    public void updateCourses(List<Map<String, String>> newCourses) {
        this.courses = newCourses;
        notifyDataSetChanged();
    }

    /**
     * Obtenir un cours à une position spécifique
     */
    public Map<String, String> getCourseAt(int position) {
        if (position >= 0 && position < courses.size()) {
            return courses.get(position);
        }
        return null;
    }
}