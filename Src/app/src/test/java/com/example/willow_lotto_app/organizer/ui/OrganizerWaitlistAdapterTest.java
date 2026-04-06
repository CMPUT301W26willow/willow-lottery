package com.example.willow_lotto_app.organizer.ui;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.example.willow_lotto_app.R;
import com.example.willow_lotto_app.registration.Registration;
import com.example.willow_lotto_app.registration.RegistrationStatus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class OrganizerWaitlistAdapterTest {

    @Test
    public void setRows_reflectsCountAndDisplayNames() {
        Context ctx = RuntimeEnvironment.getApplication();
        OrganizerWaitlistAdapter adapter = new OrganizerWaitlistAdapter(ctx);

        Registration r1 = new Registration("e1", "u1", RegistrationStatus.WAITLISTED.getValue());
        r1.setId("doc1");
        List<OrganizerWaitlistAdapter.Row> rows = Arrays.asList(
                new OrganizerWaitlistAdapter.Row(r1, "Pat"),
                new OrganizerWaitlistAdapter.Row(r1, "Pat duplicate row"));
        adapter.setRows(rows);

        assertEquals(2, adapter.getCount());
        assertEquals("Pat", adapter.getItem(0).displayName);
        assertEquals(r1, adapter.getItem(0).registration);
    }

    @Test
    public void setRows_nullClearsAdapter() {
        Context ctx = RuntimeEnvironment.getApplication();
        OrganizerWaitlistAdapter adapter = new OrganizerWaitlistAdapter(ctx);
        adapter.setRows(Collections.singletonList(
                new OrganizerWaitlistAdapter.Row(
                        new Registration("e", "u", RegistrationStatus.WAITLISTED.getValue()),
                        "X")));
        adapter.setRows(null);
        assertEquals(0, adapter.getCount());
    }

    @Test
    public void listener_inviteAndRemove_fireWithRegistration() {
        Context ctx = RuntimeEnvironment.getApplication();
        OrganizerWaitlistAdapter adapter = new OrganizerWaitlistAdapter(ctx);
        Registration r = new Registration("e1", "u1", RegistrationStatus.WAITLISTED.getValue());
        r.setId("docZ");
        adapter.setRows(Collections.singletonList(new OrganizerWaitlistAdapter.Row(r, "Q")));

        AtomicInteger invites = new AtomicInteger();
        AtomicInteger removes = new AtomicInteger();
        adapter.setListener(new OrganizerWaitlistAdapter.Listener() {
            @Override
            public void onInvite(Registration registration) {
                if (registration.getId().equals("docZ")) {
                    invites.incrementAndGet();
                }
            }

            @Override
            public void onRemove(Registration registration) {
                if (registration.getId().equals("docZ")) {
                    removes.incrementAndGet();
                }
            }
        });

        FrameLayout parent = new FrameLayout(ctx);
        View rowView = adapter.getView(0, null, parent);
        rowView.findViewById(R.id.waitlist_row_invite).performClick();
        rowView.findViewById(R.id.waitlist_row_remove).performClick();

        assertEquals(1, invites.get());
        assertEquals(1, removes.get());
    }
}
