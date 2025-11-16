package com.cocode.prepnest;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

public class PrepNest extends Application {
    private static FirebaseAnalytics firebaseAnalytics;

    public static FirebaseAnalytics getFirebaseAnalytics() {
        return firebaseAnalytics;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        );

        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e("PrepNest", "Uncaught Exception", throwable);

                // Prepare the intent for DebugActivity
                Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
                intent.putExtra("error", Log.getStackTraceString(throwable));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // Launch DebugActivity on a separate thread so it doesn’t freeze
                new Thread(() -> {
                    try {
                        Thread.sleep(200); // brief pause for stability
                        getApplicationContext().startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                // Wait a moment before shutting down
                Thread.sleep(800);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Kill the app process cleanly
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }
}
