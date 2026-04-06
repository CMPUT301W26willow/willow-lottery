package com.example.willow_lotto_app.admin;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.willow_lotto_app.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lists user profiles for admins with remove-organizer and delete actions. */
public class AdminProfileAdapter extends RecyclerView.Adapter<AdminProfileAdapter.AdminProfileViewHolder> {

    public interface AdminProfileActionListener {
        /**
         * @param user organizer row to strip organizer role from
         */
        void onRemoveOrganizerClicked(AdminUserItem user);

        /**
         * @param user profile to soft-delete / ban
         */
        void onDeleteProfileClicked(AdminUserItem user);
    }

    private final List<AdminUserItem> users;
    private final AdminProfileActionListener listener;
    private final String mode;

    /**
     * @param users    backing list (same instance is held and mutated by {@link #setUsers(List)})
     * @param listener moderation callbacks; may be null
     * @param mode     {@link AdminBrowseProfilesActivity#MODE_ENTRANTS} or {@link AdminBrowseProfilesActivity#MODE_ORGANIZERS}
     */
    public AdminProfileAdapter(List<AdminUserItem> users,
                               AdminProfileActionListener listener,
                               String mode) {
        this.users = users != null ? users : new ArrayList<>();
        this.listener = listener;
        this.mode = mode;
    }

    /**
     * @param updatedUsers replaces rows; may be null to clear
     */
    public void setUsers(List<AdminUserItem> updatedUsers) {
        users.clear();
        if (updatedUsers != null) {
            this.users.addAll(updatedUsers);
        }
        notifyDataSetChanged();
    }

    /**
     * @param parent   parent ViewGroup
     * @param viewType unused
     * @return holder for {@code R.layout.item_admin_profile}
     */
    @NonNull
    @Override
    public AdminProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile, parent, false);
        return new AdminProfileViewHolder(view);
    }

    /**
     * @param holder   row holder
     * @param position index in the backing list
     */
    @Override
    public void onBindViewHolder(@NonNull AdminProfileViewHolder holder, int position) {
        AdminUserItem user = users.get(position);

        // Anonymous (guest) rows: fixed title + uid only
        if (user.isAnonymous()) {
            holder.titleText.setText(holder.itemView.getContext().getString(R.string.admin_profile_guest_title));
            holder.subtitleText.setText(user.getUid());
            holder.metaText.setVisibility(View.GONE);
        } else {
            String displayTitle = !user.getName().trim().isEmpty()
                    ? user.getName().trim()
                    : (!user.getDisplayName().trim().isEmpty()
                    ? user.getDisplayName().trim()
                    : holder.itemView.getContext().getString(R.string.admin_profile_unnamed));
            holder.titleText.setText(displayTitle);

            String email = user.getEmail().trim();
            holder.subtitleText.setText(email.isEmpty()
                    ? holder.itemView.getContext().getString(R.string.admin_profile_no_email)
                    : email);

            String role = user.isOrganizer()
                    ? holder.itemView.getContext().getString(R.string.admin_profile_role_organizer)
                    : holder.itemView.getContext().getString(R.string.admin_profile_role_user);
            holder.metaText.setVisibility(View.VISIBLE);
            holder.metaText.setText(holder.itemView.getContext().getString(
                    R.string.admin_profile_meta_format, role, user.getUid()));
        }

        // Avatar: Glide URL or fallback initials
        String photo = user.getProfilePhotoUrl();
        if (!photo.isEmpty()) {
            holder.initialsText.setVisibility(View.GONE);
            holder.avatarImage.setVisibility(View.VISIBLE);
            try {
                Uri uri = Uri.parse(photo);
                Glide.with(holder.itemView.getContext())
                        .load(uri)
                        .apply(new RequestOptions()
                                .centerCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(holder.avatarImage);
            } catch (Exception e) {
                Glide.with(holder.itemView.getContext()).clear(holder.avatarImage);
                holder.avatarImage.setVisibility(View.GONE);
                holder.initialsText.setVisibility(View.VISIBLE);
                holder.initialsText.setText(initialsFor(user));
            }
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.avatarImage);
            holder.avatarImage.setVisibility(View.GONE);
            holder.initialsText.setVisibility(View.VISIBLE);
            holder.initialsText.setText(initialsFor(user));
        }

        holder.deleteProfileButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteProfileClicked(user);
            }
        });

        // Remove-organizer: full list in organizer mode, or single organizer rows in entrant mode
        boolean showRemoveOrganizer = AdminBrowseProfilesActivity.MODE_ORGANIZERS.equals(mode)
                || (AdminBrowseProfilesActivity.MODE_ENTRANTS.equals(mode) && user.isOrganizer());
        if (showRemoveOrganizer) {
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

    /**
     * @param user row model
     * @return one or two letter initials for avatar fallback
     */
    private static String initialsFor(AdminUserItem user) {
        if (user.isAnonymous()) {
            return "GU";
        }
        String source = !user.getName().trim().isEmpty()
                ? user.getName().trim()
                : user.getDisplayName().trim();
        if (source.isEmpty()) {
            return "?";
        }
        String[] parts = source.split("\\s+");
        if (parts.length >= 2) {
            return (initial(parts[0]) + initial(parts[parts.length - 1])).toUpperCase(Locale.ROOT);
        }
        return source.length() >= 2
                ? source.substring(0, 2).toUpperCase(Locale.ROOT)
                : source.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    /**
     * @param word word to take first letter from
     * @return first character, or empty string if null/empty
     */
    private static String initial(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }
        return word.substring(0, 1);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class AdminProfileViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImage;
        TextView initialsText;
        TextView titleText;
        TextView subtitleText;
        TextView metaText;
        MaterialButton removeOrganizerButton;
        MaterialButton deleteProfileButton;

        AdminProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.admin_profile_avatar_image);
            initialsText = itemView.findViewById(R.id.admin_profile_initials);
            titleText = itemView.findViewById(R.id.admin_profile_title);
            subtitleText = itemView.findViewById(R.id.admin_profile_subtitle);
            metaText = itemView.findViewById(R.id.admin_profile_meta);
            removeOrganizerButton = itemView.findViewById(R.id.admin_remove_organizer_button);
            deleteProfileButton = itemView.findViewById(R.id.admin_delete_profile_button);
        }
    }
}
