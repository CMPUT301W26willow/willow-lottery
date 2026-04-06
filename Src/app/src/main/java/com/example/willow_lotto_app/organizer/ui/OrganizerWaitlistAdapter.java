package com.example.willow_lotto_app.organizer.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.registration.Registration;

import java.util.ArrayList;
import java.util.List;

/**
 * Waitlist rows with organizer actions: invite (lottery slot) or remove from waitlist.
 */
public class OrganizerWaitlistAdapter extends BaseAdapter {

    public interface Listener {
        void onInvite(Registration registration);

        void onRemove(Registration registration);
    }

    public static final class Row {
        public final Registration registration;
        public String displayName;

        public Row(Registration registration, String displayName) {
            this.registration = registration;
            this.displayName = displayName;
        }
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final List<Row> rows = new ArrayList<>();
    private Listener listener;

    public OrganizerWaitlistAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setRows(List<Row> newRows) {
        rows.clear();
        if (newRows != null) {
            rows.addAll(newRows);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Row getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = inflater.inflate(R.layout.item_organizer_waitlist_row, parent, false);
        }
        Row row = rows.get(position);
        TextView name = v.findViewById(R.id.waitlist_row_name);
        Button invite = v.findViewById(R.id.waitlist_row_invite);
        Button remove = v.findViewById(R.id.waitlist_row_remove);

        name.setText(row.displayName != null && !row.displayName.isEmpty()
                ? row.displayName
                : row.registration.getUserId());

        invite.setOnClickListener(x -> {
            if (listener != null && row.registration != null) {
                listener.onInvite(row.registration);
            }
        });
        remove.setOnClickListener(x -> {
            if (listener != null && row.registration != null) {
                listener.onRemove(row.registration);
            }
        });
        return v;
    }
}
