package com.example.willow_lotto_app;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationAdapter binds UserNotificationItem data to the RecyclerView rows
 * shown in NotificationActivity.
 *
 * Role in application:
 * - Acts as the adapter layer between the notification model and the RecyclerView UI.
 * - Inflates item_notification.xml and fills each card with notification data.
 *
 * Current limitations / outstanding issues:
 * - Notifications are display-only for now.
 * - There is no click handling yet for opening related events.
 * - Dates are displayed using the default Date.toString() format and may be reformatted later.
 */

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {




    private final List<UserNotificationItem> notifications = new ArrayList<>();

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

        holder.titleText.setText(item.getTitle() != null ? item.getTitle() : "");
        holder.messageText.setText(item.getMessage() != null ? item.getMessage() : "");
        holder.typeText.setText(item.getType() != null ? item.getType() : "");

        if (item.getCreatedAt() != null) {
            holder.dateText.setText(item.getCreatedAt().toDate().toString());
        } else {
            holder.dateText.setText("");
        }

        holder.readText.setText(item.isRead() ? "Read" : "Unread");
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView messageText;
        TextView typeText;
        TextView dateText;
        TextView readText;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.notification_title);
            messageText = itemView.findViewById(R.id.notification_message);
            typeText = itemView.findViewById(R.id.notification_type);
            dateText = itemView.findViewById(R.id.notification_date);
            readText = itemView.findViewById(R.id.notification_read_status);
        }
    }
}