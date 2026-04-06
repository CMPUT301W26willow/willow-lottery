package com.example.willow_lotto_app.events.poster;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Turns a picked image into a small JPEG data URI for posterUri. */
public final class PosterFirestoreCodec {

    /** Target max JPEG size before base64 (~400KB encoded) so the rest of the event document fits under 1 MiB. */
    private static final int MAX_JPEG_BYTES = 300_000;
    private static final int MAX_DECODE_EDGE_PX = 1280;
    private static final int TARGET_MAX_EDGE_PX = 1024;

    private PosterFirestoreCodec() {
    }

    /**
     * Reads an image from {@code uri}, downscales and recompresses it, returns {@code data:image/jpeg;base64,...}.
     */
    public static String encodePosterAsDataUri(ContentResolver resolver, Uri uri) throws IOException {
        if (uri == null) {
            throw new IOException("No image selected");
        }

        int rotation = readExifRotationDegrees(resolver, uri);

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                throw new IOException("Could not open image");
            }
            BitmapFactory.decodeStream(is, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Invalid image dimensions");
        }

        int sample = 1;
        int maxDim = Math.max(bounds.outWidth, bounds.outHeight);
        while (maxDim / sample > MAX_DECODE_EDGE_PX) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                throw new IOException("Could not open image");
            }
            bitmap = BitmapFactory.decodeStream(is, null, opts);
        }
        if (bitmap == null) {
            throw new IOException("Could not decode image");
        }

        bitmap = applyRotation(bitmap, rotation);
        bitmap = scaleDownLongEdge(bitmap, TARGET_MAX_EDGE_PX);
        byte[] jpeg = compressJpegUnderCap(bitmap, MAX_JPEG_BYTES);
        return "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP);
    }

    private static int readExifRotationDegrees(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) {
                return 0;
            }
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        }
    }

    private static Bitmap applyRotation(Bitmap bitmap, int degrees) {
        if (degrees == 0) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotated != bitmap) {
            bitmap.recycle();
        }
        return rotated;
    }

    private static Bitmap scaleDownLongEdge(Bitmap bitmap, int maxEdge) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int longEdge = Math.max(w, h);
        if (longEdge <= maxEdge) {
            return bitmap;
        }
        float scale = (float) maxEdge / longEdge;
        int nw = Math.max(1, Math.round(w * scale));
        int nh = Math.max(1, Math.round(h * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, nw, nh, true);
        if (scaled != bitmap) {
            bitmap.recycle();
        }
        return scaled;
    }

    /**
     * Takes ownership of {@code bitmap} and recycles it before returning.
     */
    private static byte[] compressJpegUnderCap(Bitmap bitmap, int maxBytes) throws IOException {
        Bitmap b = bitmap;
        try {
            for (int resizePass = 0; resizePass < 8; resizePass++) {
                for (int quality = 85; quality >= 38; quality -= 5) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (!b.compress(Bitmap.CompressFormat.JPEG, quality, baos)) {
                        throw new IOException("JPEG compression failed");
                    }
                    byte[] data = baos.toByteArray();
                    if (data.length <= maxBytes) {
                        return data;
                    }
                }
                int w = b.getWidth();
                int h = b.getHeight();
                int nw = Math.max(1, (int) (w * 0.72f));
                int nh = Math.max(1, (int) (h * 0.72f));
                if (nw >= w && nh >= h) {
                    throw new IOException("Image is still too large after compression. Try a smaller photo.");
                }
                Bitmap smaller = Bitmap.createScaledBitmap(b, nw, nh, true);
                if (smaller != b) {
                    b.recycle();
                    b = smaller;
                }
            }
            throw new IOException("Image is still too large after compression. Try a smaller photo.");
        } finally {
            if (b != null && !b.isRecycled()) {
                b.recycle();
            }
        }
    }
}
