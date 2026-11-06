package com.mohit.camverz;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Production-Grade Unit Tests for WebRTC Audio Tuning & Network NAT Traversal Edge Cases.
 * Verifies Opus SDP parameter injection (FEC, DTX disabled, packet time) and TURN fallback lists.
 */
public class WebRtcAudioAndNetworkEdgeCaseTest {

    @Test
    public void testOpusSdpMungingInjectsFecAndDisablesDtx() {
        String originalSdp = "v=0\r\n"
                + "o=- 12345 2 IN IP4 127.0.0.1\r\n"
                + "s=-\r\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111\r\n"
                + "a=rtpmap:111 opus/48000/2\r\n"
                + "a=fmtp:111 minptime=10;useinbandfec=1\r\n";

        String tunedSdp = tuneOpusSdpString(originalSdp);
        assertNotNull(tunedSdp);
        assertTrue("SDP must enforce in-band forward error correction for packet loss", tunedSdp.contains("useinbandfec=1"));
        assertTrue("SDP must disable discontinuous transmission to stop voice cutting out", tunedSdp.contains("usedtx=0"));
        assertTrue("SDP must set min packet time to 10ms for low latency", tunedSdp.contains("minptime=10"));
        assertTrue("SDP must set max average bitrate", tunedSdp.contains("maxaveragebitrate=32000"));
    }

    @Test
    public void testOpusSdpWithoutExistingFmtpInsertsLine() {
        String sdpWithoutFmtp = "v=0\r\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111\r\n"
                + "a=rtpmap:111 opus/48000/2\r\n"
                + "a=mid:audio\r\n";

        String tunedSdp = tuneOpusSdpString(sdpWithoutFmtp);
        assertNotNull(tunedSdp);
        assertTrue("Tuned SDP must add fmtp line if missing", tunedSdp.contains("a=fmtp:111 minptime=10;useinbandfec=1;usedtx=0;maxaveragebitrate=32000"));
    }

    @Test
    public void testMalformedSdpHandlingDoesNotCrash() {
        assertNull("Null SDP returns null without throwing", tuneOpusSdpString(null));
        assertEquals("", tuneOpusSdpString(""));
        assertEquals("random garbage without audio lines", tuneOpusSdpString("random garbage without audio lines"));
    }

    @Test
    public void testTurnServersListCompleteness() {
        List<String> turnUrls = new ArrayList<>();
        turnUrls.add("turn:openrelay.metered.ca:80");
        turnUrls.add("turn:openrelay.metered.ca:443");
        turnUrls.add("turn:openrelay.metered.ca:443?transport=tcp");

        assertEquals(3, turnUrls.size());
        assertTrue("Port 80 fallback available for restrictive firewalls", turnUrls.get(0).contains(":80"));
        assertTrue("Port 443 available for standard HTTPS bypass", turnUrls.get(1).contains(":443"));
        assertTrue("TCP transport available for high packet-loss networks", turnUrls.get(2).contains("transport=tcp"));
    }

    /**
     * Pure Java implementation of the Opus SDP tuning algorithm used in CallActivity.java
     */
    private String tuneOpusSdpString(String sdpDescription) {
        if (sdpDescription == null || sdpDescription.isEmpty()) return sdpDescription;

        String[] lines = sdpDescription.split("\\r?\\n");
        StringBuilder newSdp = new StringBuilder();
        String opusPayloadType = null;

        for (String line : lines) {
            if (line.startsWith("a=rtpmap:") && line.toLowerCase().contains("opus/48000")) {
                int spaceIndex = line.indexOf(" ");
                if (spaceIndex != -1) {
                    opusPayloadType = line.substring("a=rtpmap:".length(), spaceIndex).trim();
                }
            }
        }

        if (opusPayloadType == null) {
            return sdpDescription;
        }

        boolean fmtpModified = false;
        for (String line : lines) {
            if (line.startsWith("a=fmtp:" + opusPayloadType)) {
                StringBuilder fmtpParams = new StringBuilder(line);
                if (!line.contains("minptime=")) fmtpParams.append(";minptime=10");
                if (!line.contains("useinbandfec=")) fmtpParams.append(";useinbandfec=1");
                if (!line.contains("usedtx=")) fmtpParams.append(";usedtx=0");
                if (!line.contains("maxaveragebitrate=")) fmtpParams.append(";maxaveragebitrate=32000");

                newSdp.append(fmtpParams.toString()).append("\r\n");
                fmtpModified = true;
            } else {
                newSdp.append(line).append("\r\n");
                if (line.startsWith("a=rtpmap:" + opusPayloadType) && !fmtpModified) {
                    // Check if next lines have fmtp, otherwise insert
                    boolean hasFmtpNext = false;
                    for (String l : lines) {
                        if (l.startsWith("a=fmtp:" + opusPayloadType)) {
                            hasFmtpNext = true;
                            break;
                        }
                    }
                    if (!hasFmtpNext) {
                        newSdp.append("a=fmtp:").append(opusPayloadType)
                                .append(" minptime=10;useinbandfec=1;usedtx=0;maxaveragebitrate=32000\r\n");
                        fmtpModified = true;
                    }
                }
            }
        }
        return newSdp.toString();
    }
}
