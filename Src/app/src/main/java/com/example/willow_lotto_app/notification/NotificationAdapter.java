package com.example.willow_lotto_app.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * NotificationAdapter binds UserNotificationItem data to the RecyclerView rows
 * shown in NotificationActivity.
 *<p>
 * Role in application:
 * - Acts as the adapter layer between the notification model and the RecyclerView UI.
 * - Inflates item_notification.xml and fills each card with notification data.
 *<p>
 * Current limitations / outstanding issues:
 * - Notifications are display-only for now.
 * - There is no click handling yet for opening related events.
 * - Dates are displayed using the default Date.toString() format and may be reformatted later.
 * Binds {@link UserNotificationItem} rows in {@link NotificationActivity}.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(UserNotificationItem item);
    }

    private final List<UserNotificationItem> notifications = new ArrayList<>();
    private OnNotificationClickListener clickListener;

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.clickListener = listener;
    }

    public void setNotifications(List<UserNotificationItem> items) {
        notifications.clear();
        if (items != null) {
            notifications.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        UserNotificationItem item = notifications.get(position);
        String type = item.getType() != null ? item.getType() : "";

        holder.titleText.setText(item.getTitle() != null ? item.getTitle() : "");
        holder.messageText.setText(item.getMessage() != null ? item.getMessage() : "");

        if (item.getCreatedAt() != null) {
            Date d = item.getCreatedAt().toDate();
            DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
            holder.dateText.setText(fmt.format(d));
        } else {
            holder.dateText.setText("");
        }

        holder.unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
        applyTypeStyle(holder, type);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onNotificationClick(item);
            }
        });
    }

    private void applyTypeStyle(@NonNull NotificationViewHolder holder, String type) {
        int iconRes = R.drawable.ic_notification_info;
        int badgeStr = R.string.notif_badge_default;
        int badgeBg = R.drawable.bg_notif_badge_neutral;
        int badgeTextColor = R.color.textGray;

        switch (type) {
            case NotificationTypes.LOTTERY_INVITED:
            case NotificationTypes.LOTTERY_REPLACEMENT:
                iconRes = R.drawable.ic_notification_selected;
                badgeStr = R.string.notif_badge_selected;
                badgeBg = R.drawable.bg_notif_badge_success;
                badgeTextColor = R.color.notif_badge_success_text;
                break;
            case NotificationTypes.WAITLIST_JOINED:
                iconRes = R.drawable.ic_notification_waitlist;
                badgeStr = R.string.notif_badge_on_waitlist;
                badgeBg = R.drawable.bg_notif_badge_info;
                badgeTextColor = R.color.notif_badge_info_text;
                break;
            case NotificationTypes.WAITLIST_UPDATE:
                iconRes = R.drawable.ic_notification_info;
                badgeStr = R.string.notif_badge_waitlist_update;
                badgeBg = R.drawable.bg_notif_badge_info;
                badgeTextColor = R.color.notif_badge_info_text;
                break;
            case NotificationTypes.LOTTERY_CANCELLED:
                iconRes = R.drawable.ic_notification_cancelled;
                badgeStr = R.string.notif_badge_cancelled;
                badgeBg = R.drawable.bg_notif_badge_neutral;
                badgeTextColor = R.color.textGray;
                break;
            case NotificationTypes.LOTTERY_NOT_CHOSEN:
                iconRes = R.drawable.ic_notification_waitlist;
                badgeStr = R.string.notif_badge_not_chosen;
                badgeBg = R.drawable.bg_notif_badge_neutral;
                badgeTextColor = R.color.textGray;
                break;
            case NotificationTypes.CO_ORGANIZER_INVITE:
                iconRes = R.drawable.ic_notification_info;
                badgeStr = R.string.notif_badge_co_organizer_invite;
                badgeBg = R.drawable.bg_notif_badge_info;
                badgeTextColor = R.color.notif_badge_info_text;
                break;
            default:
                break;
        }

        holder.icon.setImageResource(iconRes);
        holder.typeBadge.setText(holder.itemView.getContext().getString(badgeStr));
        holder.typeBadge.setBackgroundResource(badgeBg);
        holder.typeBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), badgeTextColor));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView titleText;
        TextView messageText;
        TextView dateText;
        TextView typeBadge;
        View unreadDot;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.notification_icon);
            titleText = itemView.findViewById(R.id.notification_title);
            messageText = itemView.findViewById(R.id.notification_message);
            dateText = itemView.findViewById(R.id.notification_date);
            typeBadge = itemView.findViewById(R.id.notification_type);
            unreadDot = itemView.findViewById(R.id.notification_unread_dot);
        }
    }
}
