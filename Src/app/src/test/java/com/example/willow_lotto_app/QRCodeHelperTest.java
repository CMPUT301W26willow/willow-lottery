package com.example.willow_lotto_app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link QRCodeHelper} payload logic (no Android APIs).
 * Bitmap generation is covered by UI/instrumentation or manual testing.
 */
public class QRCodeHelperTest {

    @Test
    public void getEventQrPayload_nullId_usesEmptySuffix() {
        String payload = QRCodeHelper.getEventQrPayload(null);
        assertEquals("Scheme should end with slash when id is null",
                QRCodeHelper.SCHEME, payload);
    }

    @Test
    public void getEventQrPayload_nonNullId_appendsId() {
        String payload = QRCodeHelper.getEventQrPayload("abc123");
        assertEquals("willow-lottery://event/abc123", payload);
    }

    @Test
    public void getEventQrPayload_emptyString_appendsNothing() {
        String payload = QRCodeHelper.getEventQrPayload("");
        assertEquals(QRCodeHelper.SCHEME, payload);
    }
}

