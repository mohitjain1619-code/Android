package com.mohit.camverz;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.util.Log;

/**
 * Production-grade audio ringtone and dial-tone manager.
 * Guarantees zero ghost ringing: immediately silences ringers when a call
 * is cancelled, answered, rejected, or disconnected across all Android OEMs.
 */
public class CallRingtoneManager {
    private static final String TAG = "CallRingtoneManager";

    private static volatile CallRingtoneManager instance;
    private Ringtone incomingRingtone;
    private ToneGenerator outgoingToneGenerator;
    private Vibrator vibrator;
    private boolean isPlaying = false;

    private CallRingtoneManager() {}

    public static CallRingtoneManager getInstance() {
        if (instance == null) {
            synchronized (CallRingtoneManager.class) {
                if (instance == null) {
                    instance = new CallRingtoneManager();
                }
            }
        }
        return instance;
    }

    /**
     * Start incoming call ringing and vibration.
     */
    public synchronized void startIncomingRinging(Context context) {
        if (context == null || isPlaying) return;
        try {
            stop(); // Clean slate

            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            incomingRingtone = RingtoneManager.getRingtone(context.getApplicationContext(), ringtoneUri);
            if (incomingRingtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    incomingRingtone.setAudioAttributes(attributes);
                }
                incomingRingtone.play();
            }

            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 800, 400, 800, 400, 800};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }

            isPlaying = true;
            Log.d(TAG, "🔔 Incoming call ringtone started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting incoming ringtone: " + e.getMessage());
        }
    }

    /**
     * Start outgoing dial tone while waiting for receiver to pick up.
     */
    public synchronized void startOutgoingRinging() {
        if (isPlaying) return;
        try {
            stop();
            outgoingToneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80);
            outgoingToneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE);
            isPlaying = true;
            Log.d(TAG, "📞 Outgoing ringing tone started");
        } catch (Exception e) {
            Log.w(TAG, "Unable to start ToneGenerator: " + e.getMessage());
        }
    }

    /**
     * Instantly silence any active ringtone, vibration, or dial-tone.
     */
    public synchronized void stop() {
        try {
            if (incomingRingtone != null) {
                if (incomingRingtone.isPlaying()) {
                    incomingRingtone.stop();
                }
                incomingRingtone = null;
            }

            if (outgoingToneGenerator != null) {
                outgoingToneGenerator.stopTone();
                outgoingToneGenerator.release();
                outgoingToneGenerator = null;
            }

            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
            }

            isPlaying = false;
            Log.d(TAG, "🔕 Ringtone and vibration stopped cleanly");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping ringtone: " + e.getMessage());
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}

