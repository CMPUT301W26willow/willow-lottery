package com.example.willow_lotto_app;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QRCodeHelper:
 * This class is used to generate QR codes for events.
 * 
 * Features:
 * - Generate QR codes for events
 * - Get the payload for the QR code
 * - Generate the QR code bitmap
 * 
 * Dependencies:

 * Flow:
 * 1. The user creates an event
 * 2. The event is stored in the database
 * 3. The event ID is passed to the QRCodeHelper class
 * 4. The QRCodeHelper class generates a QR code for the event
 * 5. The QR code is displayed to the user
 * 
  
*/

public final class QRCodeHelper {

    public static final String SCHEME = "willow-lottery://event/";

    public static String getEventQrPayload(String eventId) {
        if (eventId == null) eventId = "";
        return SCHEME + eventId;
    }

    // Generate the QR code bitmap
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

    // Convert the bit matrix to a bitmap
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
        // create a new bitmap with the width and height of the matrix
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        // set the pixels of the bitmap to the pixels of the matrix
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }
}
