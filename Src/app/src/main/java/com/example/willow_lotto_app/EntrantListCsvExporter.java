package com.example.willow_lotto_app;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Builds CSV file for event entrant / registration exports.
 * Author: Dev Tiwari
 */
public final class EntrantListCsvExporter {

    private static final String HEADER = "Name,Email,Phone,User ID,Registration Status,Enrolled At";

    private EntrantListCsvExporter() {
    }

    public static String escapeField(String raw) {
        if (raw == null) {
            return "";
        }
        if (raw.contains("\"") || raw.contains(",") || raw.contains("\n") || raw.contains("\r")) {
            return "\"" + raw.replace("\"", "\"\"") + "\"";
        }
        return raw;
    }

    public static String formatEnrolledAt(Timestamp ts) {
        if (ts == null) {
            return "";
        }
        Date d = ts.toDate();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(d);
    }

    public static String buildCsv(List<Row> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        for (Row r : rows) {
            sb.append(escapeField(r.name)).append(',');
            sb.append(escapeField(r.email)).append(',');
            sb.append(escapeField(r.phone)).append(',');
            sb.append(escapeField(r.userId)).append(',');
            sb.append(escapeField(r.status)).append(',');
            sb.append(escapeField(r.enrolledAt));
            sb.append('\n');
        }
        return sb.toString();
    }

    /** One exported line per registration. */
    public static final class Row {
        public final String name;
        public final String email;
        public final String phone;
        public final String userId;
        public final String status;
        public final String enrolledAt;

        public Row(String name, String email, String phone, String userId, String status, String enrolledAt) {
            this.name = name != null ? name : "";
            this.email = email != null ? email : "";
            this.phone = phone != null ? phone : "";
            this.userId = userId != null ? userId : "";
            this.status = status != null ? status : "";
            this.enrolledAt = enrolledAt != null ? enrolledAt : "";
        }
    }
}
