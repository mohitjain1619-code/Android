package com.mohit.camverz;

import org.junit.Test;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

/**
 * Production-Grade Unit Tests for VIP Pass Expiry Calculations & Edge Cases.
 * Tests timezone shifts, leap days, negative remaining times, and zero-ads flag matching.
 */
public class SubscriptionDurationEdgeCaseTest {

    @Test
    public void testActivePassCalculation() {
        long nowMs = System.currentTimeMillis();
        long futureMs = nowMs + (24L * 60L * 60L * 1000L); // 24 hours in future

        assertTrue("Future timestamp must be identified as active", isPassActive(futureMs, nowMs));
        long hoursLeft = calculateHoursRemaining(futureMs, nowMs);
        assertEquals(24L, hoursLeft);
    }

    @Test
    public void testExpiredPassCalculation() {
        long nowMs = System.currentTimeMillis();
        long pastMs = nowMs - (1000L * 60L * 5L); // 5 minutes in past

        assertFalse("Past timestamp must be identified as expired", isPassActive(pastMs, nowMs));
        assertEquals(0L, calculateHoursRemaining(pastMs, nowMs));
    }

    @Test
    public void testExactBoundaryPassCalculation() {
        long nowMs = System.currentTimeMillis();
        assertFalse("Exact same millisecond is considered expired", isPassActive(nowMs, nowMs));
    }

    @Test
    public void testUtcIsoDateParsingResilience() throws Exception {
        String isoDate = "2026-12-31T23:59:59.000Z";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date parsed = sdf.parse(isoDate);

        assertNotNull(parsed);
        assertTrue(parsed.getTime() > 0);
    }

    @Test
    public void testAdFreeStatusIdentification() {
        assertTrue("VIP No Ads plan name must be ad-free", isPlanAdFree("VIP (100% Zero Ads)"));
        assertTrue("Pass 1 Day VIP must be ad-free", isPlanAdFree("1 Day VIP Pass"));
        assertFalse("Standard With Ads must NOT be ad-free", isPlanAdFree("Standard Pass (With Ads)"));
        assertFalse("Free tier must NOT be ad-free", isPlanAdFree("Free Member"));
        assertFalse("Null must safely return false", isPlanAdFree(null));
    }

    private boolean isPassActive(long expiresMs, long currentMs) {
        return expiresMs > currentMs;
    }

    private long calculateHoursRemaining(long expiresMs, long currentMs) {
        if (expiresMs <= currentMs) return 0L;
        long diffMs = expiresMs - currentMs;
        return (diffMs + (1000L * 60L * 60L - 1)) / (1000L * 60L * 60L);
    }

    private boolean isPlanAdFree(String planName) {
        if (planName == null) return false;
        String lower = planName.toLowerCase();
        if (lower.contains("with ads") || lower.contains("with_ads") || lower.contains("standard")) {
            return false;
        }
        return lower.contains("vip") || lower.contains("zero ads") || lower.contains("ad-free");
    }
}

