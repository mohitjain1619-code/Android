package com.mohit.camverz;

import android.os.Build;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Window.Callback wrapper that intercepts and consumes all BACK key events
 * to prevent users from bypassing or cancelling ads or active upload flows.
 */
public class WindowCallbackWrapper implements Window.Callback {
    private static final String TAG = "WindowCallbackWrapper";
    private final Window.Callback delegate;

    public WindowCallbackWrapper(Window.Callback delegate) {
        this.delegate = delegate;
    }

    public Window.Callback getDelegate() {
        return delegate;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (BaseActivity.isAdShowing) {
                Log.d(TAG, "🚫 KEYCODE_BACK blocked in WindowCallbackWrapper while ad is showing");
                return true; // Consume event completely only while ad is showing
            }
        }
        return delegate != null && delegate.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        return delegate != null && delegate.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return delegate != null && delegate.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        return delegate != null && delegate.dispatchTrackballEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        return delegate != null && delegate.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        return delegate != null && delegate.dispatchPopulateAccessibilityEvent(event);
    }

    @Nullable
    @Override
    public View onCreatePanelView(int featureId) {
        return delegate != null ? delegate.onCreatePanelView(featureId) : null;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        return delegate != null && delegate.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, @Nullable View view, @NonNull Menu menu) {
        return delegate != null && delegate.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, @NonNull Menu menu) {
        return delegate != null && delegate.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, @NonNull MenuItem item) {
        return delegate != null && delegate.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
        if (delegate != null) delegate.onWindowAttributesChanged(attrs);
    }

    @Override
    public void onContentChanged() {
        if (delegate != null) delegate.onContentChanged();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (delegate != null) delegate.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onAttachedToWindow() {
        if (delegate != null) delegate.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        if (delegate != null) delegate.onDetachedFromWindow();
    }

    @Override
    public void onPanelClosed(int featureId, @NonNull Menu menu) {
        if (delegate != null) delegate.onPanelClosed(featureId, menu);
    }

    @Override
    public boolean onSearchRequested() {
        return delegate != null && delegate.onSearchRequested();
    }

    @Override
    public boolean onSearchRequested(SearchEvent searchEvent) {
        return delegate != null && delegate.onSearchRequested(searchEvent);
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return delegate != null ? delegate.onWindowStartingActionMode(callback) : null;
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
        return delegate != null ? delegate.onWindowStartingActionMode(callback, type) : null;
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        if (delegate != null) delegate.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        if (delegate != null) delegate.onActionModeFinished(mode);
    }

    @Override
    public void onProvideKeyboardShortcuts(List<android.view.KeyboardShortcutGroup> data, @Nullable Menu menu, int deviceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && delegate != null) {
            delegate.onProvideKeyboardShortcuts(data, menu, deviceId);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && delegate != null) {
            delegate.onPointerCaptureChanged(hasCapture);
        }
    }
}
