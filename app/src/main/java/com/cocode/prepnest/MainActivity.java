package com.cocode.prepnest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.MainBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    private Timer _timer = new Timer();

    private MainBinding binding;
    private HashMap<String, Object> userData = new HashMap<>();
    private NetworkMonitor networkMonitor;
    private HashMap<String, Object> features_visit_map = new HashMap<>();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
    private LogUtils logFile;

    private Intent toHomePage = new Intent();
    private TimerTask timer;
    private Intent toOnboarding = new Intent();
    private Intent toLogin = new Intent();
    private Intent toNoConnection = new Intent();
    private SharedPreferences features_visit;

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
        features_visit = getSharedPreferences("features visit", Activity.MODE_PRIVATE);
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        logFile.createLogFile();
        logFile.addActivity();
        logFile.addLog("OPEN", "APP IS OPENED");
        createUI();
        networkMonitor = new NetworkMonitor(this);
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

    @Override
    public void onStart() {
        super.onStart();
        if (PrepNestUtil.isConnected(MainActivity.this)) {
            initializeFirstVisit();
            if ((FirebaseAuth.getInstance().getCurrentUser() != null)) {
                logFile.addLog("USER", "USER IS LOGGED IN");
                logFile.addLog("USER", "USER DATA IS LOADING");
                users.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        userData = dataSnapshot.getValue(new GenericTypeIndicator<HashMap<String, Object>>() {
                        });

                        if (userData != null) {
                            if (userData.containsKey("banned")) {
                                if ((Boolean) userData.get("banned")) {
                                    PrepNestUtil.showToast(MainActivity.this, "Your account has been banned");
                                    finish();
                                } else {
                                    toHomePage.setClass(MainActivity.this, HomepageActivity.class);
                                    startActivity(toHomePage);
                                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                                    finish();
                                }
                            } else {
                                toHomePage.setClass(MainActivity.this, HomepageActivity.class);
                                startActivity(toHomePage);
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

                                finish();
                            }
                        } else {
                            PrepNestUtil.showToast(MainActivity.this, "User data not found, please login again");
                            FirebaseAuth.getInstance().signOut();
                            finish();
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                        PrepNestUtil.showToast(MainActivity.this, databaseError.toString());

                    }
                });

            } else {
                logFile.addLog("USER", "USER IS NOT LOGGED IN");
                timer = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (features_visit_map.get("onboarding").toString().equals("false")) {
                                    logFile.addLog("NAVIGATION", "NAVIGATING TO ONBOARDING");
                                    toOnboarding.setClass(MainActivity.this, OnboardingActivity.class);
                                    startActivity(toOnboarding);
                                } else {
                                    logFile.addLog("NAVIGATION", "NAVIGATING TO LOGIN");
                                    toLogin.setClass(MainActivity.this, LoginActivity.class);
                                    startActivity(toLogin);
                                }
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                timer.cancel();
                                finish();
                            }
                        });
                    }
                };
                _timer.schedule(timer, (int) (3000));
            }
        } else {
            toNoConnection.setClass(MainActivity.this, NoconnectionActivity.class);
            startActivity(toNoConnection);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        }
    }

    public void createUI() {
        binding.appname.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/appiconfont.ttf"), Typeface.NORMAL);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
    }


    public void initializeFirstVisit() {
        if (features_visit.contains("features visit")) {
            features_visit_map = new Gson().fromJson(features_visit.getString("features visit", ""), new TypeToken<HashMap<String, Object>>() {
            }.getType());
        } else {
            features_visit_map = new HashMap<>();
            features_visit_map.put("onboarding", "false");
            features_visit_map.put("provider overview", "false");
            features_visit_map.put("upload guidance", "false");
            features_visit_map.put("first resource view", "false");
            features_visit.edit().putString("features visit", new Gson().toJson(features_visit_map)).commit();
        }
    }

}
