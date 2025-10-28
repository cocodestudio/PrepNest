package com.cocode.prepnest;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.CashmanageBinding;
import com.cocode.prepnest.databinding.RequestPayoutBinding;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class CashmanageActivity extends AppCompatActivity {

    private RewardedAd rewardedAd;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
    private final DatabaseReference requests = FirebaseDatabase.getInstance().getReference("requests/payout_requests");
    private final DatabaseReference transaction_history = FirebaseDatabase.getInstance().getReference("transactions_history");
    private final Intent toTransactionHistory = new Intent();
    private final Intent toAddCash = new Intent();
    private CashmanageBinding binding;
    private HashMap<String, Object> userData = new HashMap<>();
    private HashMap<String, Object> dataMap = new HashMap<>();
    private double newAmount = 0;
    private LogUtils logFile;
    private String keyRequest = "";
    private String keyHistory = "";
    private NetworkMonitor networkMonitor;
    private com.google.android.material.bottomsheet.BottomSheetDialog request_payout_sheet;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = CashmanageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {

        binding.coinContainer.setOnClickListener(_view -> {

        });

        binding.cashContainer.setOnClickListener(_view -> binding.addCashOp.performClick());

        binding.addCashOp.setOnClickListener(_view -> {
            toAddCash.setClass(CashmanageActivity.this, AddcashActivity.class);
            toAddCash.putExtra("name", userData.get("name").toString());
            startActivity(toAddCash);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        });

        binding.payoutOp.setOnClickListener(_view -> {
            if (userData.containsKey("cash")) {
                showRequestPayoutSheet();
            } else {
                PrepNestUtil.showToast(CashmanageActivity.this, "Insufficient balance to payout!");
            }
        });

        binding.historyOp.setOnClickListener(_view -> {
            toTransactionHistory.setClass(CashmanageActivity.this, TransactionhistoryActivity.class);
            startActivity(toTransactionHistory);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        });

        binding.earnCoinsOp.setOnClickListener(_view -> {
            showRewardedAds();
        });

        binding.backIcon.setOnClickListener(_view -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
        });
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        logFile.addActivity();
        designUI();
        getMoneyData();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
        loadBannerAd();
        loadRewardedAd();
    }

    private void loadBannerAd() {
        MobileAds.initialize(this, initializationStatus -> {
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adView.loadAd(adRequest);
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(
                this,
                "ca-app-pub-3940256099942544/5224354917",
                adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        rewardedAd = null;
                        PrepNestUtil.showToast(CashmanageActivity.this, "Failed to load ad");
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        super.onAdLoaded(ad);
                        rewardedAd = ad;
                    }
                }
        );
    }

    private void showRewardedAds() {
        if (rewardedAd != null) {
            rewardedAd.show(
                    this,
                    new OnUserEarnedRewardListener() {
                        @Override
                        public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                            PrepNestUtil.showLoadingDialog(CashmanageActivity.this, true);
                            addRewardToUser();
                            loadRewardedAd();
                        }
                    }
            );
        } else {
            PrepNestUtil.showToast(CashmanageActivity.this, "Ad is not loaded, try again later");
            loadRewardedAd();
        }
    }

    public void addRewardToUser() {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            // Force refresh token to make sure it's valid
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                // Prepare JSON payload
                Map<String, Object> data = new HashMap<>();
                data.put("userId", user.getUid());
                data.put("timestamp", String.valueOf(System.currentTimeMillis()));

                // Convert to JSON string
                JSONObject json = new JSONObject(data);

                // Send HTTP POST request using OkHttp
                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

                Request request = new Request.Builder().url("https://us-central1-prepnest-65133.cloudfunctions.net/rewardUserWithCoins").post(requestBody).addHeader("Authorization", "Bearer " + idToken) // <-- add token
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("ADD_REWARD", "Failed to add reward: " + e.getMessage());
                        runOnUiThread(() -> {
                            PrepNestUtil.showLoadingDialog(CashmanageActivity.this, false);
                            PrepNestUtil.showToast(CashmanageActivity.this, "An unknown error occurred");
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        final String resp = response.body() != null ? response.body().string() : "No response";
                        Log.d("ADD_REWARD", "Response: " + resp);
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(CashmanageActivity.this, false);
                                PrepNestUtil.showToast(CashmanageActivity.this, "Reward added successfully");
                                if (userData.containsKey("coins")) {
                                    Object value = userData.get("coins");
                                    long money;

                                    if (value instanceof Long) {
                                        money = (Long) value + 5;
                                    } else if (value instanceof Double) {
                                        money = ((Double) value).longValue() + 5;
                                    } else if (value instanceof Integer) {
                                        money = ((Integer) value).longValue() + 5;
                                    } else {
                                        money = 0L; // Or handle error
                                    }
                                    userData.put("coins", money);
                                    binding.coinAmountTxt.setText(String.valueOf(money).concat(" coins"));
                                } else {
                                    binding.coinAmountTxt.setText("0 coins");
                                }

                            });
                        } else {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(CashmanageActivity.this, false);
                                PrepNestUtil.showToast(CashmanageActivity.this, "An unknown error occurred");
                            });
                        }
                    }
                });

            }).addOnFailureListener(e -> Log.e("FCM_CLIENT", "Failed to get token: " + e.getMessage()));
        } else {
            Log.e("FCM_CLIENT", "User not signed in");
        }
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
        PrepNestUtil.roundViewWithRipple(binding.coinContainer, "#FAFAFA", 30, 0, "#F5F5F5", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(binding.cashContainer, "#FAFAFA", 30, 0, "#F5F5F5", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(binding.addCashOp, "#FAFAFA", 30, 0, "#9E9E9E", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(binding.payoutOp, "#FAFAFA", 30, 0, "#9E9E9E", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(binding.historyOp, "#FAFAFA", 30, 0, "#9E9E9E", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(binding.earnCoinsOp, "#FAFAFA", 30, 0, "#9E9E9E", "#E0E0E0");
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public void getMoneyData() {
        PrepNestUtil.showLoadingDialog(this, true);
        logFile.addLog("MONEY", "GETTING MONEY DATA");
        if (getIntent().hasExtra("user")) {
            userData.clear();
            userData = new Gson().fromJson(getIntent().getStringExtra("user"), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            if (userData.containsKey("coins")) {
                Object value = userData.get("coins");
                long money;

                if (value instanceof Long) {
                    money = (Long) value;
                } else if (value instanceof Double) {
                    money = ((Double) value).longValue();
                } else if (value instanceof Integer) {
                    money = ((Integer) value).longValue();
                } else {
                    money = 0L; // Or handle error
                }
                binding.coinAmountTxt.setText(String.valueOf(money).concat(" coins"));
            } else {
                binding.coinAmountTxt.setText("0 coins");
            }
            if (userData.containsKey("cash")) {
                Object value = userData.get("cash");
                long money;

                if (value instanceof Long) {
                    money = (Long) value;
                } else if (value instanceof Double) {
                    money = ((Double) value).longValue();
                } else if (value instanceof Integer) {
                    money = ((Integer) value).longValue();
                } else {
                    money = 0L; // Or handle error
                }
                binding.cashAmountTxt.setText("₹ ".concat(String.valueOf(money)));
            } else {
                binding.cashAmountTxt.setText("₹ 0");
            }
            PrepNestUtil.showLoadingDialog(this, false);
            logFile.addLog("MONEY", "DATA LOADED SUCCESSFULLY");
        } else {
            logFile.addLog("MONEY", "FAILED TO LOAD DATA");
            PrepNestUtil.showLoadingDialog(this, false);
            PrepNestUtil.showToast(CashmanageActivity.this, "Please login again!");
            FirebaseAuth.getInstance().signOut();
            finishAffinity();
        }
    }


    public void showRequestPayoutSheet() {
        request_payout_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(CashmanageActivity.this);
        RequestPayoutBinding sheetbinding = RequestPayoutBinding.inflate(getLayoutInflater());

        request_payout_sheet.setContentView(sheetbinding.getRoot());

        request_payout_sheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });


        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetbinding.container.setBackground(gd);
        PrepNestUtil.roundViewWithRipple(sheetbinding.amount1Container, "#FAFAFA", 15, 0, "#000000", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(sheetbinding.amount2Container, "#FAFAFA", 15, 0, "#000000", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(sheetbinding.amount3Container, "#FAFAFA", 15, 0, "#000000", "#E0E0E0");
        sheetbinding.amountEdittext.setFocusableInTouchMode(true);
        sheetbinding.amount1Container.setOnClickListener(_view -> {
            sheetbinding.amountEdittext.setText(sheetbinding.amountTxt1.getText().toString());
            sheetbinding.amountEdittext.setSelection(sheetbinding.amountEdittext.getText().toString().length());
        });
        sheetbinding.amount2Container.setOnClickListener(_view -> {
            sheetbinding.amountEdittext.setText(sheetbinding.amountTxt2.getText().toString());
            sheetbinding.amountEdittext.setSelection(sheetbinding.amountEdittext.getText().toString().length());
        });
        sheetbinding.amount3Container.setOnClickListener(_view -> {
            sheetbinding.amountEdittext.setText(sheetbinding.amountTxt3.getText().toString());
            sheetbinding.amountEdittext.setSelection(sheetbinding.amountEdittext.getText().toString().length());
        });
        sheetbinding.sendReqBtn.setOnClickListener(_view -> {
            PrepNestUtil.TransitionManager(sheetbinding.container, 150);
            if (sheetbinding.amountEdittext.getText().toString().trim().isEmpty()) {
                PrepNestUtil.showToast(CashmanageActivity.this, "Enter amount");
            } else {
                if ((Double.parseDouble(sheetbinding.amountEdittext.getText().toString().trim()) > Double.parseDouble(userData.get("cash").toString())) || (Double.parseDouble(sheetbinding.amountEdittext.getText().toString().trim()) < 0)) {
                    PrepNestUtil.showToast(CashmanageActivity.this, "Insufficient balance");
                } else {
                    if (Double.parseDouble(sheetbinding.amountEdittext.getText().toString().trim()) < 10) {
                        PrepNestUtil.showToast(CashmanageActivity.this, "Enter atleast ₹10");
                    } else {
                        if (userData.containsKey("phone number")) {
                            sendPayoutRequest(Double.parseDouble(sheetbinding.amountEdittext.getText().toString().trim()));
                        } else {
                            PrepNestUtil.showToast(CashmanageActivity.this, "Link your phone number first!");
                            request_payout_sheet.dismiss();
                        }
                    }
                }
            }
        });
        request_payout_sheet.setCancelable(true);
        request_payout_sheet.show();
    }


    public void sendPayoutRequest(final double _amount) {
        PrepNestUtil.showLoadingDialog(this, true);
        long currentTimeInMS = System.currentTimeMillis();
        newAmount = (Double) userData.get("cash") - _amount;
        users.child(auth.getCurrentUser().getUid()).child("cash").setValue(newAmount).addOnCompleteListener(userUpdateTask -> {
            if (userUpdateTask.isSuccessful()) {
                binding.cashAmountTxt.setText("₹ ".concat(String.valueOf((long) (newAmount))));
                DatabaseReference requestRef = requests.child(auth.getCurrentUser().getUid());
                keyRequest = requestRef.push().getKey();
                dataMap = new HashMap<>();
                dataMap.put("amount", _amount);
                dataMap.put("timestamp", String.valueOf(currentTimeInMS));
                requestRef.child(keyRequest).setValue(dataMap).addOnCompleteListener(requestTask -> {
                    if (requestTask.isSuccessful()) {
                        DatabaseReference historyRef = transaction_history.child(auth.getCurrentUser().getUid());
                        keyHistory = historyRef.push().getKey();
                        dataMap = new HashMap<>();
                        dataMap.put("type", "cash_deduct");
                        dataMap.put("amount", _amount);
                        dataMap.put("timestamp", String.valueOf(currentTimeInMS));
                        dataMap.put("status", "pending");
                        historyRef.child(keyHistory).setValue(dataMap).addOnCompleteListener(historyTask -> {
                            if (historyTask.isSuccessful()) {
                                requestRef.child(keyRequest).child("history id").setValue(keyHistory);
                                PrepNestUtil.showToast(CashmanageActivity.this, "Request sent successfully.");
                                PrepNestUtil.showLoadingDialog(CashmanageActivity.this, false);
                                if (request_payout_sheet != null && request_payout_sheet.isShowing()) {
                                    request_payout_sheet.dismiss();
                                }
                                userData.put("cash", newAmount);
                            } else {
                                PrepNestUtil.showToast(CashmanageActivity.this, "An unknown error occurred : ".concat(historyTask.getException().toString()));
                                PrepNestUtil.showLoadingDialog(CashmanageActivity.this, false);
                            }
                        });
                    } else {
                        PrepNestUtil.showToast(CashmanageActivity.this, "Failed to send request, try again later.");
                        PrepNestUtil.showLoadingDialog(this, false);
                    }
                });
            } else {
                PrepNestUtil.showToast(CashmanageActivity.this, "An unknown error occurred : ".concat(userUpdateTask.getException().toString()));
                PrepNestUtil.showLoadingDialog(this, false);
            }
        });
    }
}
