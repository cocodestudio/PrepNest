package com.cocode.prepnest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.cocode.prepnest.databinding.BecomeproviderBinding;
import com.cocode.prepnest.databinding.EligibilityIssuesSheetviewBinding;
import com.cocode.prepnest.databinding.OverviewItemBinding;
import com.cocode.prepnest.databinding.OverviewSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;


public class BecomeproviderActivity extends AppCompatActivity {

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

    private BecomeproviderBinding binding;
    private HashMap<String, Object> userData = new HashMap<>();
    private boolean hasProfile = false;
    private boolean hasPhnNumber = false;
    private boolean isEmailVerified = false;
    private boolean isPhnVerified = false;
    private boolean isEligible = false;
    private HashMap<String, Object> features_visit_map = new HashMap<>();
    private HashMap<String, Object> add_items = new HashMap<>();
    private HashMap<String, Object> requestMap = new HashMap<>();
    private LogUtils logFile;
    private NetworkMonitor networkMonitor;

    private ArrayList<HashMap<String, Object>> provider_overview_list = new ArrayList<>();

    private com.google.android.material.bottomsheet.BottomSheetDialog eligibility_issues_sheet;
    private com.google.android.material.bottomsheet.BottomSheetDialog provider_overview_sheet;
    private SharedPreferences features_visit;
    private FirebaseAuth auth;
    private Intent toProfile = new Intent();
    private DatabaseReference users = _firebase.getReference("users");
    private ChildEventListener _users_child_listener;
    private DatabaseReference requests = _firebase.getReference("requests/provider_requests");
    private ChildEventListener _requests_child_listener;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = BecomeproviderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        features_visit = getSharedPreferences("features visit", Activity.MODE_PRIVATE);
        auth = FirebaseAuth.getInstance();

        binding.backIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                finish();
                overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
            }
        });

        binding.btnVerification.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (isEligible) {
                    sendRequest();
                } else {
                    showEligibilityIssuesSheet();
                }
            }
        });
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        logFile.addActivity();
        designUI();
        showOverviewSheet();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        getUserData();
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
    }


    public void getUserData() {
        PrepNestUtil.showLoadingDialog(this, true);
        logFile.addLog("USER", "GETTING USER DATA");
        users.child(auth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userData.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        userData.put(child.getKey(), child.getValue());
                    }
                    if (userData.containsKey("profile") && !userData.get("profile").toString().equals("null")) {
                        hasProfile = true;
                    }
                    if (userData.containsKey("phone number")) {
                        hasPhnNumber = true;
						/*
if (userData.containsKey("phone verified") && userData.get("phone verified").toString().equals("true")) {
isPhnVerified = true;
}
*/
                    }
                    if (auth.getCurrentUser() != null) {
                        auth.getCurrentUser().reload().addOnCompleteListener(checkStatus -> {
                            if (auth.getCurrentUser().isEmailVerified()) {
                                isEmailVerified = true;
                            }
                            if (hasProfile && (hasPhnNumber && isEmailVerified)) {
                                isEligible = true;
                                binding.title.setText("Eligible");
                                binding.subtext.setText(getString(R.string.provider_eligible_message));
                                binding.btnVerificationTxt.setText(getString(R.string.send_request));
                                binding.image.setImageResource(R.drawable.provider_eligible_illus);
                            } else {
                                isEligible = false;
                                binding.title.setText("Not eligible");
                                binding.subtext.setText(getString(R.string.provider_not_eligible_message));
                                binding.btnVerificationTxt.setText(getString(R.string.see_eligibility_issues));
                                binding.image.setImageResource(R.drawable.provider_not_eligible_illus);
                            }
                        });
                    }
                    PrepNestUtil.showLoadingDialog(BecomeproviderActivity.this, false);
                    logFile.addLog("USER", "USER DATA LOADED SUCCESSFULLY");
                } else {
                    PrepNestUtil.showLoadingDialog(BecomeproviderActivity.this, false);
                    logFile.addLog("USER", "FAILED TO LOAD USER DATA");
                    PrepNestUtil.showToast(BecomeproviderActivity.this, "Please login again!");
                    FirebaseAuth.getInstance().signOut();
                    finishAffinity();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

                if (!isFinishing() && !isDestroyed()) {
                    logFile.addLog("USER", "FAILED TO LOAD USER DATA : ".concat(error.toString()));
                    PrepNestUtil.showLoadingDialog(BecomeproviderActivity.this, false);
                    FirebaseAuth.getInstance().signOut();
                    PrepNestUtil.showToast(BecomeproviderActivity.this, "Please login again!");
                    finishAffinity();
                }
            }
        });
    }


    public void showOverviewSheet() {
        if (features_visit.contains("features visit")) {
            features_visit_map = new Gson().fromJson(features_visit.getString("features visit", ""), new TypeToken<HashMap<String, Object>>() {
            }.getType());
        }
        if (features_visit_map.containsKey("provider overview")) {
            if (features_visit_map.get("provider overview").toString().equals("false")) {
                addProviderOverviewItems();
                provider_overview_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(BecomeproviderActivity.this);
                OverviewSheetBinding sheetbinding = OverviewSheetBinding.inflate(getLayoutInflater());

                provider_overview_sheet.setContentView(sheetbinding.getRoot());

                provider_overview_sheet.setOnShowListener(dialog -> {
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
                sheetbinding.viewpager.addOnPageChangeListener(new OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int _position, float _positionOffset, int _positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int _position) {
                        sheetbinding.dot1.setBackground(new GradientDrawable() {
                            public GradientDrawable getIns(int a, int b) {
                                this.setCornerRadius(a);
                                this.setColor(b);
                                return this;
                            }
                        }.getIns((int) 360, 0xFFF5F5F5));
                        sheetbinding.dot2.setBackground(new GradientDrawable() {
                            public GradientDrawable getIns(int a, int b) {
                                this.setCornerRadius(a);
                                this.setColor(b);
                                return this;
                            }
                        }.getIns((int) 360, 0xFFF5F5F5));
                        sheetbinding.dot3.setBackground(new GradientDrawable() {
                            public GradientDrawable getIns(int a, int b) {
                                this.setCornerRadius(a);
                                this.setColor(b);
                                return this;
                            }
                        }.getIns((int) 360, 0xFFF5F5F5));
                        if (_position == 0) {
                            sheetbinding.dot1.setBackground(new GradientDrawable() {
                                public GradientDrawable getIns(int a, int b) {
                                    this.setCornerRadius(a);
                                    this.setColor(b);
                                    return this;
                                }
                            }.getIns((int) 360, 0xFF000000));
                            sheetbinding.btnNext.setText("Next");
                        } else {
                            if (_position == 1) {
                                sheetbinding.dot2.setBackground(new GradientDrawable() {
                                    public GradientDrawable getIns(int a, int b) {
                                        this.setCornerRadius(a);
                                        this.setColor(b);
                                        return this;
                                    }
                                }.getIns((int) 360, 0xFF000000));
                                sheetbinding.btnNext.setText("Next");
                            } else {
                                if (_position == 2) {
                                    sheetbinding.dot3.setBackground(new GradientDrawable() {
                                        public GradientDrawable getIns(int a, int b) {
                                            this.setCornerRadius(a);
                                            this.setColor(b);
                                            return this;
                                        }
                                    }.getIns((int) 360, 0xFF000000));
                                    sheetbinding.btnNext.setText("Done");
                                } else {

                                }
                            }
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int _scrollState) {

                    }
                });

                sheetbinding.dot1.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFF000000));
                sheetbinding.dot2.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                sheetbinding.dot3.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                sheetbinding.viewpager.setAdapter(new ViewpagerAdapter(provider_overview_list));
                ((PagerAdapter) sheetbinding.viewpager.getAdapter()).notifyDataSetChanged();
                sheetbinding.btnNext.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View _view) {
                        if (sheetbinding.viewpager.getCurrentItem() == 0) {
                            sheetbinding.viewpager.setCurrentItem((int) 1);
                        } else {
                            if (sheetbinding.viewpager.getCurrentItem() == 1) {
                                sheetbinding.viewpager.setCurrentItem((int) 2);
                            } else {
                                features_visit_map.put("provider overview", "true");
                                features_visit.edit().putString("features visit", new Gson().toJson(features_visit_map)).commit();
                                provider_overview_sheet.dismiss();
                            }
                        }
                    }
                });
                provider_overview_sheet.setCancelable(false);
                provider_overview_sheet.show();
            }
        }
    }


    public class ViewpagerAdapter extends PagerAdapter {

        Context _context;
        ArrayList<HashMap<String, Object>> _data;

        public ViewpagerAdapter(Context _ctx, ArrayList<HashMap<String, Object>> _arr) {
            _context = _ctx;
            _data = _arr;
        }

        public ViewpagerAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _context = BecomeproviderActivity.this;
            _data = _arr;
        }

        @Override
        public int getCount() {
            return _data.size();
        }

        @Override
        public boolean isViewFromObject(View _view, Object _object) {
            return _view == _object;
        }

        @Override
        public void destroyItem(ViewGroup _container, int _position, Object _object) {
            _container.removeView((View) _object);
        }

        @Override
        public int getItemPosition(Object _object) {
            return super.getItemPosition(_object);
        }

        @Override
        public CharSequence getPageTitle(int pos) {
            // Use the Activity Event (onTabLayoutNewTabAdded) in order to use this method
            return "page " + String.valueOf(pos);
        }

        @Override
        public Object instantiateItem(ViewGroup _container, final int _position) {
            OverviewItemBinding binding = OverviewItemBinding.inflate(LayoutInflater.from(_context), _container, false);


            binding.title.setText(_data.get(_position).get("title").toString());

            binding.subtext.setText(_data.get(_position).get("subtext").toString());

            View _view = binding.getRoot();
            _container.addView(_view);
            return _view;
        }
    }

    {
    }


    public void addProviderOverviewItems() {
        add_items = new HashMap<>();
        add_items.put("title", "What is provider?");
        add_items.put("subtext", "A Provider is a special type of user on PrepNest who can upload and share educational resources within the app. These resources are made available for other users to purchase. Becoming a provider gives you exclusive access to upload your own content and earn from it.");
        provider_overview_list.add(add_items);
        add_items = new HashMap<>();
        add_items.put("title", "Eligibility Criteria");
        add_items.put("subtext", "To become a provider, your account must have a valid and authentic profile picture, a verified email address, and a phone number that is both linked to your account and verified.");
        provider_overview_list.add(add_items);
        add_items = new HashMap<>();
        add_items.put("title", "Benefits");
        add_items.put("subtext", "As a provider, you’ll earn 30% of the amount each time a user purchases your resources. The earnings can be in cash or coins, depending on which payment method the user chooses during the transaction.");
        provider_overview_list.add(add_items);
    }


    public void showEligibilityIssuesSheet() {
        eligibility_issues_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(BecomeproviderActivity.this);
        EligibilityIssuesSheetviewBinding sheetbinding = EligibilityIssuesSheetviewBinding.inflate(getLayoutInflater());

        eligibility_issues_sheet.setContentView(sheetbinding.getRoot());

        eligibility_issues_sheet.setOnShowListener(dialog -> {
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
        if (hasProfile) {
            sheetbinding.iconStatusProfile.setImageResource(R.drawable.icon_check_circle_round);
            sheetbinding.profileStatusTxt.setTextColor(0xFF4CAF50);
            sheetbinding.iconStatusProfile.setColorFilter(0xFF4CAF50);
            sheetbinding.profileStatusTxt.setText("Profile picture is uploaded");
        } else {
            sheetbinding.iconStatusProfile.setImageResource(R.drawable.icon_cancel);
            sheetbinding.profileStatusTxt.setTextColor(0xFFD32F2F);
            sheetbinding.iconStatusProfile.setColorFilter(0xFFD32F2F);
            sheetbinding.profileStatusTxt.setText("Profile picture is not uploaded");
        }
        if (hasPhnNumber) {
            sheetbinding.iconStatusPhone.setImageResource(R.drawable.icon_check_circle_round);
            sheetbinding.phoneStatusTxt.setTextColor(0xFF4CAF50);
            sheetbinding.iconStatusPhone.setColorFilter(0xFF4CAF50);
            sheetbinding.phoneStatusTxt.setText("Phone number is linked");
			/*
if (isPhnVerified) {

} else {
icon_status_phone.setImageResource(R.drawable.icon_cancel);
phone_status_txt.setTextColor(0xFFD32F2F);
icon_status_phone.setColorFilter(0xFFD32F2F);
phone_status_txt.setText("Phone number is not verified");
}
*/
        } else {
            sheetbinding.iconStatusPhone.setImageResource(R.drawable.icon_cancel);
            sheetbinding.phoneStatusTxt.setTextColor(0xFFD32F2F);
            sheetbinding.iconStatusPhone.setColorFilter(0xFFD32F2F);
            sheetbinding.phoneStatusTxt.setText("Phone number is not linked");
        }
        if (isEmailVerified) {
            sheetbinding.iconStatusEmail.setImageResource(R.drawable.icon_check_circle_round);
            sheetbinding.emailStatusTxt.setTextColor(0xFF4CAF50);
            sheetbinding.iconStatusEmail.setColorFilter(0xFF4CAF50);
            sheetbinding.emailStatusTxt.setText("Email address is verified");
        } else {
            sheetbinding.iconStatusEmail.setImageResource(R.drawable.icon_cancel);
            sheetbinding.emailStatusTxt.setTextColor(0xFFD32F2F);
            sheetbinding.iconStatusEmail.setColorFilter(0xFFD32F2F);
            sheetbinding.emailStatusTxt.setText("Email address is not verified");
        }
        sheetbinding.btnFix.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toProfile.setClass(BecomeproviderActivity.this, UserprofileActivity.class);
                startActivity(toProfile);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                eligibility_issues_sheet.dismiss();
            }
        });
        eligibility_issues_sheet.setCancelable(true);
        eligibility_issues_sheet.show();
    }


    public void sendRequest() {
        PrepNestUtil.showLoadingDialog(this, true);
        logFile.addLog("PROVIDER", "SENDING REQUEST");
        long currentTimeInMS = System.currentTimeMillis();
        requestMap = new HashMap<>();
        requestMap.put("timestamp", String.valueOf((long) (currentTimeInMS)));
        requests.child(auth.getCurrentUser().getUid()).updateChildren(requestMap).addOnCompleteListener(addRequest -> {
            if (addRequest.isSuccessful()) {
                users.child(auth.getCurrentUser().getUid()).child("provider verification status").setValue("pending").addOnCompleteListener(updateRequest -> {
                    if (updateRequest.isSuccessful()) {
                        sendNotifications("admin", "Provider Request", "New provider request from '".concat(userData.get("name").toString().concat("'")));
                        logFile.addLog("PROVIDER", "REQUEST SENT SUCCESSFULLY");
                        PrepNestUtil.showToast(BecomeproviderActivity.this, "Request is successfully sent.");
                        binding.backIcon.performClick();
                        PrepNestUtil.showLoadingDialog(BecomeproviderActivity.this, false);
                    } else {
                        logFile.addLog("PROVIDER", "FAILED TO SEND : ".concat(updateRequest.getException().toString()));
                        PrepNestUtil.showToast(BecomeproviderActivity.this, "An unknown error occured.");
                        PrepNestUtil.showLoadingDialog(BecomeproviderActivity.this, false);
                    }
                });
            } else {
                PrepNestUtil.showToast(BecomeproviderActivity.this, "Failed to send request, try again later.");
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
			Toast.makeText(BecomeproviderActivity.this, "Response: " + response, Toast.LENGTH_SHORT).show();
		});
        */
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
				        /*
        _loadingDialog(false);
		runOnUiThread(() -> {
			Toast.makeText(BecomeproviderActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		});
        */
            }
        }).start();

    }

}
