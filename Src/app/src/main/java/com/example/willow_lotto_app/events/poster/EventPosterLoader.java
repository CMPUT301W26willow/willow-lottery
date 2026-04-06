package com.example.willow_lotto_app.events.poster;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

/**
 * Loads event {@code posterUri} values into an {@link ImageView}.
 * <p>
 * Firestore posters are stored as {@code data:image/jpeg;base64,...} strings. Those are too long for
 * reliable {@link Uri#parse(String)} handling, so this path decodes to bytes and loads via Glide.
 */
public final class EventPosterLoader {

    private EventPosterLoader() {
    }

    public static void load(Context context, String posterUri, ImageView target, String eventId) {
        load(context, posterUri, target, EventPlaceholderDrawables.forEventId(eventId), false);
    }

    public static void loadWithCrossFade(FragmentActivity activity, String posterUri, ImageView target, String eventId) {
        load(activity, posterUri, target, EventPlaceholderDrawables.forEventId(eventId), true);
    }

    public static void load(Context context, String posterUri, ImageView target, @DrawableRes int fallbackRes) {
        load(context, posterUri, target, fallbackRes, false);
    }

    private static void load(Context context, String posterUri, ImageView target,
                             @DrawableRes int fallbackRes, boolean crossFade) {
        if (posterUri == null || posterUri.trim().isEmpty()) {
            Glide.with(context).clear(target);
            target.setImageResource(fallbackRes);
            return;
        }
        String s = posterUri.trim();
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(fallbackRes)
                .error(fallbackRes);

        RequestBuilder<Drawable> request;
        if (s.regionMatches(true, 0, "data:image", 0, "data:image".length())) {
            byte[] decoded = decodeDataImageBase64(s);
            if (decoded != null && decoded.length > 0) {
                request = Glide.with(context).load(decoded).apply(options);
            } else {
                Glide.with(context).clear(target);
                target.setImageResource(fallbackRes);
                return;
            }
        } else {
            try {
                request = Glide.with(context).load(Uri.parse(s)).apply(options);
            } catch (Exception e) {
                Glide.with(context).clear(target);
                target.setImageResource(fallbackRes);
                return;
            }
        }

        if (crossFade && context instanceof FragmentActivity) {
            request = request.transition(DrawableTransitionOptions.withCrossFade(200));
        }
        request.into(target);
    }

    private static byte[] decodeDataImageBase64(String dataUri) {
        int comma = dataUri.indexOf(',');
        if (comma < 0 || comma >= dataUri.length() - 1) {
            return null;
        }
        String payload = dataUri.substring(comma + 1).replace("\n", "").replace("\r", "").trim();
        try {
            return Base64.decode(payload, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
