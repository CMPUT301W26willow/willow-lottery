package com.example.willow_lotto_app.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter used by the administrator profile browsing screen.
 *
 * Responsibilities:
 * - Displays organizer or entrant profile rows.
 * - Supports delete profile actions.
 * - Supports remove organizer privilege actions when the screen is in organizer mode.
 *
 * This adapter is intentionally simple so it matches the current project style.
 */


public class AdminProfileAdapter extends RecyclerView.Adapter<AdminProfileAdapter.AdminProfileViewHolder> {
    /**
     * Listener used by the hosting activity to respond to admin actions.
     */
    public interface AdminProfileActionListener {
        void onRemoveOrganizerClicked(AdminUserItem user);
        void onDeleteProfileClicked(AdminUserItem user);
    }

    private final List<AdminUserItem> users = new ArrayList<>();
    private final AdminProfileActionListener listener;
    private final String mode;

    /**
     * Creates the adapter for admin profile browsing.
     *
     * @param users initial list of users
     * @param listener callback listener for button presses
     * @param mode organizer or entrant mode
     */
    public AdminProfileAdapter(List<AdminUserItem> users,
                               AdminProfileActionListener listener,
                               String mode) {
        if (users != null) {
            this.users.addAll(users);
        }
        this.listener = listener;
        this.mode = mode;
    }

    /**
     * Replaces the displayed list and refreshes the RecyclerView.
     *
     * @param updatedUsers new list of users
     */
    public void setUsers(List<AdminUserItem> updatedUsers) {
        users.clear();
        if (updatedUsers != null) {
            users.addAll(updatedUsers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile, parent, false);
        return new AdminProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminProfileViewHolder holder, int position) {
        AdminUserItem user = users.get(position);

        holder.nameText.setText(user.getName() != null && !user.getName().trim().isEmpty()
                ? user.getName()
                : "Unnamed User");

        holder.emailText.setText(user.getEmail() != null ? user.getEmail() : "");

        holder.roleText.setText(user.isOrganizer() ? "Organizer" : "Entrant/User");

        /**
         * Delete profile button is always shown in both modes.
         */
        holder.deleteProfileButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteProfileClicked(user);
            }
        });

        /**
         * Remove organizer button only appears when browsing organizer profiles.
         */
        if (AdminBrowseProfilesActivity.MODE_ORGANIZERS.equals(mode)) {
            holder.removeOrganizerButton.setVisibility(View.VISIBLE);
            holder.removeOrganizerButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveOrganizerClicked(user);
                }
            });
        } else {
            holder.removeOrganizerButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * ViewHolder for a single admin profile row.
     */
    static class AdminProfileViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView emailText;
        TextView roleText;
        Button removeOrganizerButton;
        Button deleteProfileButton;

        AdminProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.admin_profile_name);
            emailText = itemView.findViewById(R.id.admin_profile_email);
            roleText = itemView.findViewById(R.id.admin_profile_role);
            removeOrganizerButton = itemView.findViewById(R.id.admin_remove_organizer_button);
            deleteProfileButton = itemView.findViewById(R.id.admin_delete_profile_button);
        }
    }
}
