package com.mohitt.camverz;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import com.mohitt.camverz.api.TokenManager;

/**
 * Singleton handler for incoming private calls.
 * Registers listeners on the global socket so incoming calls are received
 * regardless of which Activity is currently in the foreground.
 * 
 * Handles:
 * - Showing incoming call dialog on any screen
 * - Dismissing dialog if caller cancels
 * - Preventing duplicate dialogs
 * - Preventing accept of already-cancelled calls
 * - Rejecting calls when user is busy
 */
public class IncomingCallHandler implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "IncomingCallHandler";
    private static IncomingCallHandler instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Socket socket;
    private Activity currentActivity;
    private AlertDialog currentIncomingDialog;
    private boolean isRegistered = false;

    // Pending call data
    private String pendingCallerId;
    private String pendingCallerName;
    private String pendingCallerAvatar;
    private boolean pendingIsVideo;
    private String pendingRoomName;

    private Emitter.Listener incomingCallListener;
    private Emitter.Listener callCancelledListener;
    private Emitter.Listener callFailedListener;
    private Emitter.Listener connectListener;
    
    private android.content.Context appContext;

    private IncomingCallHandler() {}

    public static synchronized IncomingCallHandler getInstance() {
        if (instance == null) {
            instance = new IncomingCallHandler();
        }
        return instance;
    }

    /**
     * Initialize the handler. Call this once from the first activity after login.
     * @param app The Application instance for activity lifecycle tracking
     * @param socket The socket instance
     */
    public void init(Application app, Socket socket) {
        if (isRegistered) {
            // Already registered, just update socket reference
            unregisterListeners();
        }
        this.appContext = app.getApplicationContext();
        this.socket = socket;
        app.registerActivityLifecycleCallbacks(this);
        registerListeners();
        isRegistered = true;
        Log.d(TAG, "IncomingCallHandler initialized");
    }

    /**
     * Clean up. Call on logout.
     */
    public void destroy(Application app) {
        unregisterListeners();
        app.unregisterActivityLifecycleCallbacks(this);
        dismissIncomingDialog();
        isRegistered = false;
        currentActivity = null;
        appContext = null;
        Log.d(TAG, "IncomingCallHandler destroyed");
    }

    private void registerListeners() {
        if (socket == null) return;

        connectListener = args -> {
            Log.d(TAG, "Socket connected/reconnected, auto-emitting register-user");
            try {
                if (appContext != null) {
                    TokenManager tokenManager = TokenManager.getInstance(appContext);
                    if (tokenManager.isLoggedIn()) {
                        JSONObject obj = new JSONObject();
                        obj.put("uid", tokenManager.getUserId());
                        socket.emit("register-user", obj);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in auto-register-user connect listener", e);
            }
        };
        socket.on(Socket.EVENT_CONNECT, connectListener);

        incomingCallListener = args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String callerId = data.getString("callerId");
                String callerName = data.optString("callerName", "Unknown");
                String callerAvatar = data.optString("callerAvatar", "");
                boolean isVideo = data.optBoolean("isVideo", true);
                String roomName = data.getString("room");

                Log.d(TAG, "Incoming private call from " + callerName + " (id: " + callerId + ")");

                // If already in a call, auto-reject
                if (CallManager.isCallActive()) {
                    Log.d(TAG, "Already in a call, auto-rejecting incoming call");
                    try {
                        JSONObject reject = new JSONObject();
                        reject.put("callerId", callerId);
                        socket.emit("reject-private-call", reject);
                    } catch (JSONException ex) {
                        Log.e(TAG, "Error emitting reject-private-call", ex);
                    }
                    return;
                }

                // If another incoming call is already pending, reject the new one
                if (CallManager.isIncomingCallPending()) {
                    Log.d(TAG, "Already have a pending incoming call, rejecting new one");
                    try {
                        JSONObject reject = new JSONObject();
                        reject.put("callerId", callerId);
                        socket.emit("reject-private-call", reject);
                    } catch (JSONException ex) {
                        Log.e(TAG, "Error emitting reject-private-call", ex);
                    }
                    return;
                }

                // Store pending call data
                pendingCallerId = callerId;
                pendingCallerName = callerName;
                pendingCallerAvatar = callerAvatar;
                pendingIsVideo = isVideo;
                pendingRoomName = roomName;
                CallManager.setIncomingCallPending(true);

                mainHandler.post(() -> showIncomingCallDialog());

            } catch (JSONException e) {
                Log.e(TAG, "Error handling incoming-private-call", e);
            }
        };
        socket.on("incoming-private-call", incomingCallListener);

        callCancelledListener = args -> {
            Log.d(TAG, "Private call cancelled by caller");
            mainHandler.post(() -> {
                dismissIncomingDialog();
                CallManager.setIncomingCallPending(false);
                pendingCallerId = null;
            });
        };
        socket.on("private-call-cancelled", callCancelledListener);
    }

    private void unregisterListeners() {
        if (socket == null) return;
        if (incomingCallListener != null) socket.off("incoming-private-call", incomingCallListener);
        if (callCancelledListener != null) socket.off("private-call-cancelled", callCancelledListener);
        if (connectListener != null) socket.off(Socket.EVENT_CONNECT, connectListener);
        incomingCallListener = null;
        callCancelledListener = null;
        connectListener = null;
    }

    private void showIncomingCallDialog() {
        if (currentActivity == null || currentActivity.isFinishing() || currentActivity.isDestroyed()) {
            Log.w(TAG, "No foreground activity to show incoming call dialog");
            // Auto-reject since we can't show UI
            rejectPendingCall();
            return;
        }

        // Don't show dialog if we're already on CallActivity
        if (currentActivity instanceof CallActivity) {
            Log.d(TAG, "Already on CallActivity, auto-rejecting new call");
            rejectPendingCall();
            return;
        }

        dismissIncomingDialog(); // Dismiss any previous dialog

        String callType = pendingIsVideo ? "Video Call" : "Voice Call";
        
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
        builder.setTitle("Incoming " + callType);
        builder.setMessage(pendingCallerName + " is calling you" + (pendingIsVideo ? " with video." : " with voice."));
        builder.setCancelable(false);
        
        builder.setPositiveButton("Accept", (dialog, which) -> {
            // Verify call is still valid before accepting
            if (!CallManager.isIncomingCallPending() || pendingCallerId == null) {
                Log.w(TAG, "Call was already cancelled, not opening CallActivity");
                return;
            }
            
            CallManager.setIncomingCallPending(false);
            
            Intent intent = new Intent(currentActivity, CallActivity.class);
            intent.putExtra("targetUserId", pendingCallerId);
            intent.putExtra("targetUserName", pendingCallerName);
            intent.putExtra("targetUserAvatar", pendingCallerAvatar);
            intent.putExtra("isVideoCall", pendingIsVideo);
            intent.putExtra("isCaller", false);
            intent.putExtra("isPrivateCall", true);
            intent.putExtra("roomName", pendingRoomName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            currentActivity.startActivity(intent);
            
            pendingCallerId = null;
        });
        
        builder.setNegativeButton("Reject", (dialog, which) -> {
            rejectPendingCall();
        });

        try {
            currentIncomingDialog = builder.create();
            currentIncomingDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing incoming call dialog", e);
            rejectPendingCall();
        }
    }

    private void rejectPendingCall() {
        if (pendingCallerId != null && socket != null) {
            try {
                JSONObject reject = new JSONObject();
                reject.put("callerId", pendingCallerId);
                socket.emit("reject-private-call", reject);
            } catch (JSONException ex) {
                Log.e(TAG, "Error emitting reject-private-call", ex);
            }
        }
        CallManager.setIncomingCallPending(false);
        pendingCallerId = null;
    }

    private void dismissIncomingDialog() {
        if (currentIncomingDialog != null && currentIncomingDialog.isShowing()) {
            try {
                currentIncomingDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing incoming call dialog", e);
            }
            currentIncomingDialog = null;
        }
    }

    // ============================================
    // Activity Lifecycle Callbacks
    // ============================================

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (currentActivity == activity) {
            // Don't null out immediately — the dialog might still need the reference
            // during activity transition
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        if (currentActivity == activity) {
            dismissIncomingDialog();
            currentActivity = null;
        }
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityStarted(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}
