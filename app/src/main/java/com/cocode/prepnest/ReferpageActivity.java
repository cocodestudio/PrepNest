package com.cocode.prepnest;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.ReferpageBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;


public class ReferpageActivity extends AppCompatActivity {

    private ReferpageBinding binding;
    private HashMap<String, Object> userData = new HashMap<>();
    private LogUtils logFile;
    private NetworkMonitor networkMonitor;

    private ArrayList<String> referredUsers = new ArrayList<>();


    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = ReferpageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        binding.backIcon.setOnClickListener(_view -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
        });

        binding.referCodeMain.setOnClickListener(_view -> {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", binding.referCode.getText().toString()));
            PrepNestUtil.showToast(ReferpageActivity.this, "Copied!");
        });

        binding.referBtn.setOnClickListener(_view -> sendReferLink());
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        logFile.addActivity();
        designUI();
        if ((FirebaseAuth.getInstance().getCurrentUser() != null)) {
            logFile.addLog("USER", "USER IS LOGGED IN");
            binding.referCode.setText(FirebaseAuth.getInstance().getCurrentUser().getUid().substring(0, 7));
            fetchReferData();
        } else {
            logFile.addLog("USER", "USER IS NOT LOGGED IN");
            PrepNestUtil.showToast(ReferpageActivity.this, "Error: please login again");
            FirebaseAuth.getInstance().signOut();
            finishAffinity();
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
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

    public void designUI() {
        PrepNestUtil.roundViewWithRipple(binding.referCodeMain, "#F5F5F5", 30, 0, "#000000", "#E0E0E0");
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public void fetchReferData() {
        userData.clear();
        PrepNestUtil.showLoadingDialog(this, true);
        logFile.addLog("REFERRAL", "GETTING REFERRAL DATA");
        if (getIntent().hasExtra("user")) {
            userData = new Gson().fromJson(getIntent().getStringExtra("user"), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            assert userData != null;
            if (userData.containsKey("referred users")) {
                if (!userData.get("referred users").toString().isEmpty()) {
                    referredUsers = new Gson().fromJson(userData.get("referred users").toString(), new TypeToken<ArrayList<String>>() {
                    }.getType());
                }
                if (referredUsers != null && !referredUsers.isEmpty()) {
                    binding.totalReferredAmountTxt.setText(String.valueOf((long) (referredUsers.size())));
                } else {
                    binding.totalReferredAmountTxt.setText("0");

                }
            } else {
                binding.totalReferredAmountTxt.setText("0");
            }
            PrepNestUtil.showLoadingDialog(this, false);
            logFile.addLog("REFERRAL", "REFERRAL DATA LOADED");
        } else {
            logFile.addLog("REFERRAL", "FAILED TO LOAD REFERRAL DATA");
            PrepNestUtil.showToast(ReferpageActivity.this, "Please login again");
            PrepNestUtil.showLoadingDialog(this, false);
            FirebaseAuth.getInstance().signOut();
            finishAffinity();
        }
    }


    public void sendReferLink() {
        String appPackageName = getPackageName();
        String appLink = "https://play.google.com/store/apps/details?id=" + appPackageName;

        String message = "Prep smarter, not harder - with PrepNest!\nUse my referral code: `" + binding.referCode.getText().toString() + "`\nGet the app now:\n" + appLink;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}
