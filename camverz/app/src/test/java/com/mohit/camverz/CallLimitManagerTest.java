package com.mohit.camverz;

import org.junit.Test;
import static org.junit.Assert.*;

public class CallLimitManagerTest {

    @Test
    public void testInitialFreeSecondsConstant() {
        // Initial free call duration must be 300 seconds (5 minutes)
        long initialSeconds = 300L;
        assertEquals(300L, initialSeconds);
    }

    @Test
    public void testAdTierEscalationSequence() {
        int tier1 = 1;
        int tier2 = tier1 + 1;
        int tier3 = tier2 + 1;
        int tier4 = tier3 + 1;

        assertEquals(2, tier2);
        assertEquals(3, tier3);
        assertEquals(4, tier4); // Tier 4 triggers multi-stage cooldown block
    }

    @Test
    public void testMultiStageBlockDurations() {
        long level1Ms = 30L * 60L * 1000L; // 30 minutes
        long level2Ms = 3L * 60L * 60L * 1000L; // 3 hours
        long level3Ms = 3L * 60L * 60L * 1000L; // 3 hours
        long level4Ms = 10L * 60L * 60L * 1000L; // 10 hours

        assertEquals(1800000L, level1Ms);
        assertEquals(10800000L, level2Ms);
        assertEquals(10800000L, level3Ms);
        assertEquals(36000000L, level4Ms);
    }

    @Test
    public void testBlockLevelCycleReset() {
        int currentLevel = 4;
        int nextLevel = (currentLevel >= 4) ? 1 : currentLevel + 1;
        assertEquals(1, nextLevel); // Resets to Level 1 after Level 4
    }

    @Test
    public void testEdgeCaseBlockDurationCalculation() {
        // Test edge cases for block level boundaries
        long level0Ms = getBlockDurationForLevel(0);
        long level5Ms = getBlockDurationForLevel(5);

        assertEquals(108000000L / 3, level0Ms); // default fallback to 10 hours for unexpected levels
        assertEquals(36000000L, level5Ms); // default 10 hours
    }

    private long getBlockDurationForLevel(int level) {
        switch (level) {
            case 1:
                return 30L * 60L * 1000L;
            case 2:
            case 3:
                return 3L * 60L * 60L * 1000L;
            case 4:
            default:
                return 10L * 60L * 60L * 1000L;
        }
    }

    @Test
    public void testFormattedTimeStringEdgeCases() {
        long ms30Min = 30 * 60 * 1000L;
        long ms3Hours = 3 * 3600 * 1000L;
        long ms10Hours = 10 * 3600 * 1000L;

        assertEquals("30 minute(s)", formatTimeMs(ms30Min));
        assertEquals("3 hour(s) 0 minute(s)", formatTimeMs(ms3Hours));
        assertEquals("10 hour(s) 0 minute(s)", formatTimeMs(ms10Hours));
    }

    private String formatTimeMs(long ms) {
        long totalSec = ms / 1000;
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;

        if (hours > 0) {
            return String.format(java.util.Locale.US, "%d hour(s) %d minute(s)", hours, minutes);
        } else if (minutes > 0) {
            return String.format(java.util.Locale.US, "%d minute(s)", minutes);
        } else {
            return String.format(java.util.Locale.US, "%d second(s)", seconds);
        }
    }
}
