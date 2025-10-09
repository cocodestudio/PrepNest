package com.cocode.prepnest;

import android.app.Activity;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.cocode.prepnest.databinding.CustomSnackbarBinding;
import com.google.android.material.snackbar.Snackbar;

public class NetworkMonitor {

    private final Activity activity;
    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback;
    private boolean wasOffline = false;

    public NetworkMonitor(Activity activity) {
        this.activity = activity;
        connectivityManager = (ConnectivityManager) activity.getSystemService(Activity.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (wasOffline) {
                    showCustomSnackbar("Back online");
                    wasOffline = false;
                }
            }

            @Override
            public void onLost(Network network) {
                showCustomSnackbar("You are offline!");
                wasOffline = true;
            }
        };
    }

    public void register() {
        NetworkRequest request = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    public void unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    public void showCustomSnackbar(String message) {
        View parentLayout = activity.findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(parentLayout, "", Snackbar.LENGTH_INDEFINITE);

        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
        snackbarLayout.setPadding(0, 0, 0, 0);

        LayoutInflater inflater = LayoutInflater.from(activity);
        CustomSnackbarBinding binding = CustomSnackbarBinding.inflate(inflater);

        binding.message.setText(message);
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        binding.getRoot().setLayoutParams(params);

        snackbarLayout.addView(binding.getRoot(), 0);

        snackbar.show();
        // Animate slide-in from bottom
        binding.getRoot().setTranslationY(200); // start below screen
        binding.getRoot().animate()
                .translationY(0)
                .setDuration(300)
                .start();

        new Handler().postDelayed(() -> {
            binding.getRoot().animate()
                    .translationY(200)
                    .setDuration(300)
                    .start();
            snackbar.dismiss();
        }, 3000); // match Snackbar.LENGTH_LONG (~3s)
    }
}
