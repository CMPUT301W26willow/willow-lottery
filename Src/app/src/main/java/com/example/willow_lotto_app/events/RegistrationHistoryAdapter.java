package com.example.willow_lotto_app.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for registration history cards on the Events tab.
 */
public class RegistrationHistoryAdapter extends RecyclerView.Adapter<RegistrationHistoryAdapter.Holder> {

    public interface Listener {
        void onItemClick(@NonNull RegistrationHistoryItem item);
    }

    private final List<RegistrationHistoryItem> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<RegistrationHistoryItem> next) {
        items.clear();
        if (next != null) {
            items.addAll(next);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registration_history, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        RegistrationHistoryItem item = items.get(position);
        h.badge.setText(item.getBadgeLabel());
        h.badge.setBackgroundResource(item.getBadgeBackgroundRes());
        h.badge.setTextColor(h.itemView.getContext().getColor(item.getBadgeTextColorRes()));
        h.title.setText(item.getEventTitle());
        h.registered.setText(item.getRegisteredDateLine());
        h.eventDate.setText(item.getEventDateLine());

        switch (item.getOutcome()) {
            case POSITIVE:
                h.outcome.setVisibility(View.VISIBLE);
                h.outcome.setBackgroundResource(R.drawable.bg_reg_history_outcome_positive);
                h.outcomeIcon.setImageResource(R.drawable.ic_notification_selected);
                h.outcomeText.setText(h.outcomeText.getContext().getString(item.getOutcomeMessageRes()));
                h.outcomeText.setTextColor(h.outcomeText.getContext().getColor(R.color.reg_history_outcome_positive_text));
                break;
            case NEGATIVE:
                h.outcome.setVisibility(View.VISIBLE);
                h.outcome.setBackgroundResource(R.drawable.bg_reg_history_outcome_negative);
                h.outcomeIcon.setImageResource(R.drawable.ic_reg_history_negative);
                h.outcomeText.setText(h.outcomeText.getContext().getString(item.getOutcomeMessageRes()));
                h.outcomeText.setTextColor(h.outcomeText.getContext().getColor(R.color.reg_history_outcome_negative_text));
                break;
            default:
                h.outcome.setVisibility(View.GONE);
                break;
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView badge;
        final TextView title;
        final TextView registered;
        final TextView eventDate;
        final LinearLayout outcome;
        final ImageView outcomeIcon;
        final TextView outcomeText;

        Holder(@NonNull View itemView) {
            super(itemView);
            badge = itemView.findViewById(R.id.reg_hist_badge);
            title = itemView.findViewById(R.id.reg_hist_title);
            registered = itemView.findViewById(R.id.reg_hist_registered);
            eventDate = itemView.findViewById(R.id.reg_hist_event_date);
            outcome = itemView.findViewById(R.id.reg_hist_outcome);
            outcomeIcon = itemView.findViewById(R.id.reg_hist_outcome_icon);
            outcomeText = itemView.findViewById(R.id.reg_hist_outcome_text);
        }
    }
}
