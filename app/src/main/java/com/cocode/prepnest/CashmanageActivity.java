package com.cocode.prepnest;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.CashmanageBinding;
import com.cocode.prepnest.databinding.RequestPayoutBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;


public class CashmanageActivity extends AppCompatActivity {

    private CashmanageBinding binding;
    private HashMap<String, Object> userData = new HashMap<>();
    private HashMap<String, Object> dataMap = new HashMap<>();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
    private DatabaseReference requests = FirebaseDatabase.getInstance().getReference("requests/payout_requests");
    private DatabaseReference transaction_history = FirebaseDatabase.getInstance().getReference("transactions_history");
    private double newAmount = 0;
    private LogUtils logFile;
    private String keyRequest = "";
    private String keyHistory = "";
    private NetworkMonitor networkMonitor;

    private Intent toTransactionHistory = new Intent();
    private Intent toAddCash = new Intent();
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

        binding.coinContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {

            }
        });

        binding.cashContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                binding.addCashOp.performClick();
            }
        });

        binding.addCashOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toAddCash.setClass(CashmanageActivity.this, AddcashActivity.class);
                toAddCash.putExtra("name", userData.get("name").toString());
                startActivity(toAddCash);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            }
        });

        binding.payoutOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (userData.containsKey("cash")) {
                    showRequestPayoutSheet();
                } else {
                    PrepNestUtil.showToast(CashmanageActivity.this, "Insufficient balance to payout!");
                }
            }
        });

        binding.historyOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toTransactionHistory.setClass(CashmanageActivity.this, TransactionhistoryActivity.class);
                startActivity(toTransactionHistory);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            }
        });

        binding.backIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                finish();
                overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
            }
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
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
                binding.coinAmountTxt.setText(String.valueOf((long) (money)).concat(" coins"));
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
                binding.cashAmountTxt.setText("₹ ".concat(String.valueOf((long) (money))));
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
        sheetbinding.amount1Container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                sheetbinding.amountEdittext.setText(sheetbinding.amountTxt1.getText().toString());
                ((EditText) sheetbinding.amountEdittext).setSelection((int) sheetbinding.amountEdittext.getText().toString().length());
            }
        });
        sheetbinding.amount2Container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                sheetbinding.amountEdittext.setText(sheetbinding.amountTxt2.getText().toString());
                ((EditText) sheetbinding.amountEdittext).setSelection((int) sheetbinding.amountEdittext.getText().toString().length());
            }
        });
        sheetbinding.amount3Container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                sheetbinding.amountEdittext.setText(sheetbinding.amountTxt3.getText().toString());
                ((EditText) sheetbinding.amountEdittext).setSelection((int) sheetbinding.amountEdittext.getText().toString().length());
            }
        });
        sheetbinding.sendReqBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                PrepNestUtil.TransitionManager(sheetbinding.container, 150);
                if (sheetbinding.amountEdittext.getText().toString().trim().equals("")) {
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
                dataMap.put("timestamp", String.valueOf((long) (currentTimeInMS)));
                requestRef.child(keyRequest).setValue(dataMap).addOnCompleteListener(requestTask -> {
                    if (requestTask.isSuccessful()) {
                        sendNotifications("admin", "Payout Request", "Payout request from '".concat(userData.get("name").toString().concat("'")));
                        DatabaseReference historyRef = transaction_history.child(auth.getCurrentUser().getUid());
                        keyHistory = historyRef.push().getKey();
                        dataMap = new HashMap<>();
                        dataMap.put("type", "cash_deduct");
                        dataMap.put("amount", _amount);
                        dataMap.put("timestamp", String.valueOf((long) (currentTimeInMS)));
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


    public void sendNotifications(final String topic, final String title, final String message) {
        new Thread(() -> {
            try {
                URL url = new URL("https://us-central1-prepnest-65133.cloudfunctions.net/sendNotification");

                // Open Connection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                //Create JSON Object
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("topic", topic);
                jsonParam.put("title", title);
                jsonParam.put("body", message);

                // Write JSON to request body
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Get response
                int responseCode = conn.getResponseCode();
                BufferedReader br;
                if (responseCode >= 200 && responseCode < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                br.close();

                // Show result on UI Thread
				        /*
		runOnUiThread(() -> {
            _loadingDialog(false);
			Toast.makeText(CashmanageActivity.this, "Response: " + response, Toast.LENGTH_SHORT).show();
		});
        */
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
				        /*
        _loadingDialog(false);
		runOnUiThread(() -> {
			Toast.makeText(CashmanageActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		});
        */
            }
        }).start();

    }

}
