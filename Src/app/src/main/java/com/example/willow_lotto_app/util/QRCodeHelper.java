package com.example.willow_lotto_app.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public final class QRCodeHelper {

    public static final String SCHEME = "willow-lottery://event/";

    public static String getEventQrPayload(String eventId) {
        if (eventId == null) eventId = "";
        return SCHEME + eventId;
    }

    public static Bitmap generateEventQrBitmap(String eventId, int sizePx) {
        String content = getEventQrPayload(eventId);
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
            return bitMatrixToBitmap(bitMatrix);
        } catch (WriterException e) {
            return null;
        }
    }

    private static Bitmap bitMatrixToBitmap(BitMatrix matrix) {
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }
}
