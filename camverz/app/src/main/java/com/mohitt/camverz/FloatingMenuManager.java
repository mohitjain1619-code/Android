package com.mohitt.camverz;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;

public class FloatingMenuManager {

    private PopupWindow popupWindow;
    private Context context;
    private View anchorView;

    public FloatingMenuManager(Context context, View anchorView) {
        this.context = context;
        this.anchorView = anchorView;
    }

    public void show() {
        if (popupWindow != null && popupWindow.isShowing()) {
            return;
        }

        // Inflate the menu layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View menuView = inflater.inflate(R.layout.floating_menu_popup, null);

        // Create PopupWindow
        popupWindow = new PopupWindow(menuView, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true);

        // Set background
        popupWindow.setBackgroundDrawable(context.getDrawable(R.drawable.bg_floating_menu_popup));
        popupWindow.setElevation(16);

        // Add animation
        menuView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.menu_expand));

        // Set dismiss listener to show collapse animation
        popupWindow.setOnDismissListener(this::onMenuDismissed);

        // Setup menu item click listeners
        setupMenuItemClickListeners(menuView);

        // Show popup (anchored to the floating button, positioned above it)
        popupWindow.showAsDropDown(anchorView, -220, -280, android.view.Gravity.END);
    }

    public void hide() {
        if (popupWindow != null && popupWindow.isShowing()) {
            View contentView = popupWindow.getContentView();
            if (contentView != null) {
                contentView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.menu_collapse));
            }
            popupWindow.dismiss();
        }
    }

    private void setupMenuItemClickListeners(View menuView) {
        // Home
        menuView.findViewById(R.id.menu_home).setOnClickListener(v -> {
            hide();
            context.startActivity(new Intent(context, MainScreenActivity.class));
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).finish();
            }
        });

        // Explore
        menuView.findViewById(R.id.menu_explore).setOnClickListener(v -> {
            hide();
            context.startActivity(new Intent(context, FeedActivity.class));
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).finish();
            }
        });

        // Messages
        menuView.findViewById(R.id.menu_messages).setOnClickListener(v -> {
            hide();
            context.startActivity(new Intent(context, MessagesActivity.class));
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).finish();
            }
        });

        // Profile
        menuView.findViewById(R.id.menu_profile).setOnClickListener(v -> {
            hide();
            context.startActivity(new Intent(context, ProfileActivity.class));
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).finish();
            }
        });

        // Click outside to close
        menuView.setOnClickListener(v -> hide());
    }

    private void onMenuDismissed() {
        popupWindow = null;
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }
}
