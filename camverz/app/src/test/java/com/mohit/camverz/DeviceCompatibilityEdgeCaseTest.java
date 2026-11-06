package com.mohit.camverz;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Production-Grade Unit Tests for Edge Cases on Low-End Phones & Network Pressure.
 * Tests memory trim handling, aspect ratio safety, low-RAM string protection, and network backoff.
 */
public class DeviceCompatibilityEdgeCaseTest {

    @Test
    public void testTrimMemoryLevelsHandling() {
        // Android OEM memory trim levels
        int runningCritical = 15; // ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        int runningLow = 10;      // ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
        int runningModerate = 5;  // ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE
        int uiHidden = 20;        // ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        int complete = 80;        // ComponentCallbacks2.TRIM_MEMORY_COMPLETE

        assertTrue("Critical trim level must trigger aggressive cache clear", shouldPerformAggressiveTrim(runningCritical));
        assertTrue("Complete trim level must trigger aggressive cache clear", shouldPerformAggressiveTrim(complete));
        assertFalse("UI hidden does not require clearing critical memory", shouldPerformAggressiveTrim(uiHidden));
        assertFalse("Running moderate is manageable", shouldPerformAggressiveTrim(runningModerate));
    }

    private boolean shouldPerformAggressiveTrim(int level) {
        return level >= 15; // TRIM_MEMORY_RUNNING_CRITICAL and above
    }

    @Test
    public void testAspectRatioBoundariesFromTabletToTallPhones() {
        // Tablets 4:3 (1.33) up to ultra-tall 21:9 (2.33)
        float tabletAspect = 4f / 3f;      // ~1.33
        float standardAspect = 16f / 9f;   // ~1.77
        float tallAspect = 20f / 9f;       // ~2.22
        float ultraTallAspect = 21f / 9f;  // ~2.33

        assertTrue("Tablet aspect ratio supported", isSupportedAspectRatio(tabletAspect));
        assertTrue("Standard aspect ratio supported", isSupportedAspectRatio(standardAspect));
        assertTrue("Tall aspect ratio supported", isSupportedAspectRatio(tallAspect));
        assertTrue("Ultra tall aspect ratio supported", isSupportedAspectRatio(ultraTallAspect));

        // Absurd / broken aspect ratio
        assertFalse("Zero aspect ratio rejected", isSupportedAspectRatio(0f));
        assertFalse("Negative aspect ratio rejected", isSupportedAspectRatio(-1.5f));
        assertFalse("Excessive aspect ratio rejected", isSupportedAspectRatio(3.5f));
    }

    private boolean isSupportedAspectRatio(float ratio) {
        return ratio >= 1.0f && ratio <= 2.5f;
    }

    @Test
    public void testLowMemoryTextTruncationSafety() {
        // Prevents OutOfMemory on 1GB RAM phones when receiving massive chat spam
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("A");
        }
        String massiveText = sb.toString();

        String safeSnippet = getSafePreviewSnippet(massiveText, 100);
        assertNotNull(safeSnippet);
        assertEquals(103, safeSnippet.length()); // 100 chars + "..."
        assertTrue(safeSnippet.endsWith("..."));

        // Null and empty safety
        assertEquals("", getSafePreviewSnippet(null, 100));
        assertEquals("", getSafePreviewSnippet("", 100));
        assertEquals("Short text", getSafePreviewSnippet("Short text", 100));
    }

    private String getSafePreviewSnippet(String text, int maxChars) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars) + "...";
    }

    @Test
    public void testExponentialBackoffOnFlakyNetworks() {
        // Simulates reconnect backoff on poor 2G/3G cellular networks
        int attempt0 = getBackoffDelayMs(0);
        int attempt1 = getBackoffDelayMs(1);
        int attempt2 = getBackoffDelayMs(2);
        int attempt3 = getBackoffDelayMs(3);
        int attemptMax = getBackoffDelayMs(10);

        assertEquals(1000, attempt0);
        assertEquals(2000, attempt1);
        assertEquals(4000, attempt2);
        assertEquals(8000, attempt3);
        assertEquals(15000, attemptMax); // Capped at 15 seconds to avoid starvation
    }

    private int getBackoffDelayMs(int attempt) {
        int baseMs = 1000;
        int maxMs = 15000;
        int delay = baseMs * (1 << Math.min(attempt, 4));
        return Math.min(delay, maxMs);
    }
}

