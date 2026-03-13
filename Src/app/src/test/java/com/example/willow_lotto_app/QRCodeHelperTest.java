package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link QRCodeHelper}.
 * Only tests string payload generation (no Android Bitmap).
 */
public class QRCodeHelperTest {

    @Test
    public void getEventQrPayload_nullId_usesEmptySuffix() {
        String payload = QRCodeHelper.getEventQrPayload(null);
        assertEquals("willow-lottery://event/", payload);
    }

    @Test
    public void getEventQrPayload_nonNullId_appendsId() {
        String payload = QRCodeHelper.getEventQrPayload("abc123");
        assertEquals("willow-lottery://event/abc123", payload);
    }

    @Test
    public void getEventQrPayload_emptyString_addsNothing() {
        String payload = QRCodeHelper.getEventQrPayload("");
        assertEquals("willow-lottery://event/", payload);
    }
}

