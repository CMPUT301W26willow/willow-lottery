package com.example.willow_lotto_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.willow_lotto_app.events.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists events owned by the signed-in organizer with actions to open the dashboard or export CSV.
 */
public class OrganizerMyEventsAdapter extends RecyclerView.Adapter<OrganizerMyEventsAdapter.Holder> {

    public interface Listener {
        void onOpenDashboard(Event event);

        void onExportCsv(Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final Listener listener;

    public OrganizerMyEventsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setEvents(List<Event> updated) {
        events.clear();
        if (updated != null) {
            events.addAll(updated);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_my_event, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Event event = events.get(position);
        holder.name.setText(event.getName() != null ? event.getName() : "");
        String date = event.getDate();
        holder.date.setText(date != null ? date : "");
        holder.pastBadge.setVisibility(event.isPastByEventDate() ? View.VISIBLE : View.GONE);
        holder.openDashboard.setOnClickListener(v -> listener.onOpenDashboard(event));
        holder.exportCsv.setOnClickListener(v -> listener.onExportCsv(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView date;
        final TextView pastBadge;
        final Button openDashboard;
        final Button exportCsv;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_organizer_event_name);
            date = itemView.findViewById(R.id.item_organizer_event_date);
            pastBadge = itemView.findViewById(R.id.item_organizer_event_past_badge);
            openDashboard = itemView.findViewById(R.id.item_organizer_open_dashboard);
            exportCsv = itemView.findViewById(R.id.item_organizer_export_csv);
        }
    }
}
