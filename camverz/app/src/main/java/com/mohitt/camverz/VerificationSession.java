package com.mohitt.camverz;

import android.graphics.Bitmap;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Clean holder for captured verification image(s).
 * Current flow uses only front image.
 */
public class VerificationSession {
    private static final String TAG = "VerificationSession";
    
    private Bitmap front;
    private File sessionDir;

    public VerificationSession(File sessionDir) {
        this.sessionDir = sessionDir;
        if (!sessionDir.exists()) {
            sessionDir.mkdirs();
        }
    }

    /**
     * Save bitmap as JPEG and store reference
     */
    public void setFront(Bitmap bitmap) {
        this.front = bitmap;
        saveImage(bitmap, "front");
    }

    /**
     * Save bitmap to file
     * front.jpg
     */
    private void saveImage(Bitmap bitmap, String name) {
        if (bitmap == null) {
            Log.e(TAG, "Cannot save: bitmap is null");
            return;
        }

        try {
            File file = new File(sessionDir, name + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            Log.d(TAG, "✅ Saved: " + file.getAbsolutePath() + " (" + (file.length() / 1024) + " KB)");
        } catch (Exception e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
        }
    }

    /**
     * Get bitmap for verification
     */
    public Bitmap getFront() { return front; }

    /**
     * Get file path
     */
    public File getFrontFile() { return new File(sessionDir, "front.jpg"); }

    public File getSessionDir() { return sessionDir; }

    public void saveDebugImage(Bitmap bitmap, String name) {
        saveImage(bitmap, name);
    }

    /**
     * Clear session
     */
    public void clear() {
        front = null;
    }

    /**
     * Delete all files
     */
    public void deleteFiles() {
        try {
            getFrontFile().delete();
            Log.d(TAG, "🗑️ Cleaned up session files");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning files: " + e.getMessage());
        }
    }
}
