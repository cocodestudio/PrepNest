package com.cocode.prepnest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PrepNestUtil {

    private static ProgressDialog progress_dialog;

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        // Get the currently active network
        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        // Get network capabilities
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        );
    }

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void hideKeyboard(Activity activity) {
        // Get the currently focused view, which would receive soft keyboard input
        View view = activity.getCurrentFocus();
        if (view == null) {
            // If no view currently has focus, create a new one to grab a window token
            view = new View(activity);
        }

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void roundViewWithRipple(final View view, final String bgColor, final float radius, final int strokeSize, final String strokeColor, final String pressedColor) {
        android.graphics.drawable.GradientDrawable GD = new android.graphics.drawable.GradientDrawable();
        GD.setColor(Color.parseColor(bgColor));
        GD.setCornerRadius(radius);
        GD.setStroke(strokeSize, Color.parseColor((strokeColor)));

        android.graphics.drawable.RippleDrawable RE = new android.graphics.drawable.RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{Color.parseColor(pressedColor)}), GD, null);

        view.setBackground(RE);
    }

    public static void roundView(final View view, final String bgColor, final float radius, final int strokeSize, final String strokeColor) {
        view.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b, int c, int d) {
                this.setCornerRadius(a);
                this.setStroke(b, c);
                this.setColor(d);
                return this;
            }
        }.getIns((int) radius, (int) strokeSize, Color.parseColor(strokeColor), Color.parseColor(bgColor)));
    }

    public static void showLoadingDialog(final Activity activity, final boolean isShowing) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return; // Prevent BadTokenException
        }

        if (isShowing) {
            if (progress_dialog == null) {
                progress_dialog = new ProgressDialog(activity);
                progress_dialog.setCancelable(false);
                progress_dialog.setCanceledOnTouchOutside(false);

                // Remove the title bar
                progress_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                if (progress_dialog.getWindow() != null) {
                    progress_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }
            }

            if (!progress_dialog.isShowing()) {
                progress_dialog.show();
                progress_dialog.setContentView(R.layout.progress_bar);
                LinearLayout container = progress_dialog.findViewById(R.id.container);
                if (container != null) {
                    container.setBackground(new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns((int) 70, 0xFFFFFFFF));
                }
            }

        } else {
            if (progress_dialog != null && progress_dialog.isShowing()) {
                progress_dialog.dismiss();
                progress_dialog = null; // avoid leaking old Activity
            }
        }
    }


    public static void TransitionManager(final View view, final double duration) {
        LinearLayout viewgroup = (LinearLayout) view;

        android.transition.AutoTransition autoTransition = new android.transition.AutoTransition();
        autoTransition.setDuration((long) duration);
        android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
    }

    public static float getDip(Context context, int input) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, input, context.getResources().getDisplayMetrics());
    }

    public static int getDisplayWidthPixels(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDisplayHeightPixels(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }
}
