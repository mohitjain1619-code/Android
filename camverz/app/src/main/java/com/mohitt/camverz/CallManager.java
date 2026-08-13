package com.mohitt.camverz;

/**
 * Singleton to track the global call state across the app.
 * Prevents duplicate call screens, tracks busy state, and manages pending incoming calls.
 */
public class CallManager {
    
    public enum CallState {
        IDLE,
        CALLING,      // Outgoing call ringing
        RINGING,      // Incoming call ringing
        CONNECTING,   // WebRTC connecting
        CONNECTED,    // Active call
        RECONNECTING, // Temporary disconnection
        ENDED         // Call ended, cleaning up
    }

    private static volatile boolean isCallActive = false;
    private static volatile CallState currentState = CallState.IDLE;
    private static volatile String currentPeerId = null;
    private static volatile String currentRoomName = null;
    private static volatile boolean isIncomingCallPending = false;

    public static boolean isCallActive() {
        return isCallActive;
    }

    public static void setCallActive(boolean active) {
        isCallActive = active;
        if (!active) {
            currentState = CallState.IDLE;
            currentPeerId = null;
            currentRoomName = null;
            isIncomingCallPending = false;
        }
    }

    public static CallState getCurrentState() {
        return currentState;
    }

    public static void setCurrentState(CallState state) {
        currentState = state;
    }

    public static String getCurrentPeerId() {
        return currentPeerId;
    }

    public static void setCurrentPeerId(String peerId) {
        currentPeerId = peerId;
    }

    public static String getCurrentRoomName() {
        return currentRoomName;
    }

    public static void setCurrentRoomName(String roomName) {
        currentRoomName = roomName;
    }

    public static boolean isIncomingCallPending() {
        return isIncomingCallPending;
    }

    public static void setIncomingCallPending(boolean pending) {
        isIncomingCallPending = pending;
    }

    /**
     * Check if the user is busy (in a call or has a pending incoming call).
     */
    public static boolean isBusy() {
        return isCallActive || isIncomingCallPending;
    }

    /**
     * Reset all state to idle. Called on logout or full cleanup.
     */
    public static void reset() {
        isCallActive = false;
        currentState = CallState.IDLE;
        currentPeerId = null;
        currentRoomName = null;
        isIncomingCallPending = false;
    }
}
