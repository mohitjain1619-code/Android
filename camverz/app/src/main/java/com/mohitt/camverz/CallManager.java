package com.mohitt.camverz;

public class CallManager {
    private static boolean isCallActive = false;

    public static boolean isCallActive() {
        return isCallActive;
    }

    public static void setCallActive(boolean isCallActive) {
        CallManager.isCallActive = isCallActive;
    }
}
