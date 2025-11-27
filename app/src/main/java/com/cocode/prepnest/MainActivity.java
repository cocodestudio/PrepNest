package com.cocode.prepnest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.MainBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private final Timer _timer = new Timer();
    private final Intent toHomePage = new Intent();
    private final Intent toOnboarding = new Intent();
    private final Intent toLogin = new Intent();
    private final Intent toNoConnection = new Intent();
    private MainBinding binding;
    private NetworkMonitor networkMonitor;
    private HashMap<String, Object> appFirstVisit = new HashMap<>();
    private TimerTask timer;
    private SharedPreferences appFirstVisitSp;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        appFirstVisitSp = getSharedPreferences("appFirstVisit", Activity.MODE_PRIVATE);
    }

    private void initializeLogic() {
        initializeFirstVisit();
        createUI();
        networkMonitor = new NetworkMonitor(this);
        PrepNestUtil.changeNavBarColor(this, true);
        loadApp();
    }


    @Override
    public void onResume() {
        super.onResume();
        networkMonitor.register();
    }

    @Override
    public void onPause() {
        super.onPause();
        networkMonitor.unregister();
    }

    private void loadApp() {
        if (PrepNestUtil.isConnected(MainActivity.this)) {
            if ((FirebaseAuth.getInstance().getCurrentUser() != null)) {
                timer = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            toHomePage.setClass(MainActivity.this, HomepageActivity.class);
                            startActivity(toHomePage);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            timer.cancel();
                            finish();
                        });
                    }
                };
                _timer.schedule(timer, 2000);
            } else {
                timer = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            Log.d("VISIT", Objects.requireNonNull(appFirstVisit.get("onboarding")).toString());
                            if (!Boolean.parseBoolean(Objects.requireNonNull(appFirstVisit.get("onboarding")).toString())) {
                                toOnboarding.setClass(MainActivity.this, OnboardingActivity.class);
                                startActivity(toOnboarding);
                            } else {
                                toLogin.setClass(MainActivity.this, LoginActivity.class);
                                startActivity(toLogin);
                            }
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            timer.cancel();
                            finish();
                        });
                    }
                };
                _timer.schedule(timer, 3000);
            }
        } else {
            toNoConnection.setClass(MainActivity.this, NoconnectionActivity.class);
            startActivity(toNoConnection);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        }
    }

    public void createUI() {
        binding.appname.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/appiconfont.ttf"), Typeface.NORMAL);
        PrepNestUtil.setLightStatusBar(this);
    }


    public void initializeFirstVisit() {
        if (appFirstVisitSp.contains("appFirstVisit")) {
            appFirstVisit = new Gson().fromJson(appFirstVisitSp.getString("appFirstVisit", ""), new TypeToken<HashMap<String, Object>>() {
            }.getType());
        } else {
            appFirstVisit = new HashMap<>();
            appFirstVisit.put("onboarding", false);
            appFirstVisit.put("providerOverview", false);
            appFirstVisit.put("uploadGuidance", false);
            appFirstVisit.put("firstResourceView", false);
            appFirstVisitSp.edit().putString("appFirstVisit", new Gson().toJson(appFirstVisit)).apply();
        }
    }

}
