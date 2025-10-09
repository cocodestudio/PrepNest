package com.cocode.prepnest;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.AddreferralBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;


public class AddreferralActivity extends AppCompatActivity {

    private AddreferralBinding binding;
    private HashMap<String, Object> map = new HashMap<>();
    private String tempid = "";
    private HashMap<String, Object> otherUserData = new HashMap<>();
    private String currentUID = "";
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
    private LogUtils logFile;
    private NetworkMonitor networkMonitor;

    private ArrayList<String> userKeys = new ArrayList<>();
    private ArrayList<String> otherUserRefers = new ArrayList<>();

    private Intent toHomePage = new Intent();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = AddreferralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        binding.skipTxt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toHomePage.setClass(AddreferralActivity.this, HomepageActivity.class);
                startActivity(toHomePage);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                finish();
            }
        });

        binding.addBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                PrepNestUtil.TransitionManager(binding.edittextContainer, 150);
                if (binding.edittext.getText().toString().trim().equals("")) {
                    binding.errorTxt.setText("Enter referral code");
                    binding.errorTxt.setVisibility(View.VISIBLE);
                } else {
                    if (validateReferralCode(binding.edittext.getText().toString().trim())) {
                        if (PrepNestUtil.isConnected(AddreferralActivity.this)) {
                            PrepNestUtil.showLoadingDialog(AddreferralActivity.this, true);
                            logFile.addLog("REFERRAL", "ADDING REFERRAL");
                            addRefer();
                        } else {
                            com.google.android.material.snackbar.Snackbar.make(binding.background, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", new OnClickListener() {
                                @Override
                                public void onClick(View _view) {

                                }
                            }).show();
                        }
                    } else {
                        binding.errorTxt.setText("Invalid referral code");
                        binding.errorTxt.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        binding.edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String _charSeq = s.toString();
                if (binding.errorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextContainer, 150);
                    binding.errorTxt.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override

            public void afterTextChanged(Editable s) {
            }
        });

    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        logFile.addActivity();
        getAllUserIDs();
        designUI();
        updateOnChangedListener();
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


    public void getAllUserIDs() {
        PrepNestUtil.showLoadingDialog(this, true);
        logFile.addLog("REFERRAL", "GETTING USER ID");
        users.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String key = userSnapshot.getKey();
                    userKeys.add(key);
                }
                PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);
                logFile.addLog("REFERRAL", "ALL USER IDs FETCHED");

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);
                logFile.addLog("REFERRAL", "FAILED TO GET DATA : ".concat(databaseError.getMessage()));
                PrepNestUtil.showToast(AddreferralActivity.this, "Database error: " + databaseError.getMessage());

            }
        });

    }


    public boolean validateReferralCode(final String _code) {
        if (FirebaseAuth.getInstance().getCurrentUser().getUid().substring((int) (0), (int) (7)).equals(_code)) {
            return false;
        } else {
            for (int i = 0; i < userKeys.size(); i++) {
                if (userKeys.get((int) (i)).substring((int) (0), (int) (7)).equals(_code)) {
                    tempid = userKeys.get((int) (i));
                    return true;
                }
            }
            return false;
        }
    }


    public void updateOnChangedListener() {
        users.child(auth.getCurrentUser().getUid()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle child added
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                logFile.addLog("REFERRAL", "REFERRAL ADDED SUCCESSFULLY");
                PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);
                toHomePage.setClass(AddreferralActivity.this, HomepageActivity.class);
                startActivity(toHomePage);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                finish();
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                // Handle child removed
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Handle child moved
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle errors
            }
        });
    }


    public void addRefer() {
        users.child(tempid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                otherUserData = dataSnapshot.getValue(new GenericTypeIndicator<HashMap<String, Object>>() {
                });
                currentUID = auth.getCurrentUser().getUid();
                if (otherUserData != null && otherUserData.containsKey("referred users")) {
                    otherUserRefers = new Gson().fromJson(otherUserData.get("referred users").toString(), new TypeToken<ArrayList<String>>() {
                    }.getType());
                } else {
                    otherUserRefers = new ArrayList<>();
                }
                if (!otherUserRefers.contains(currentUID)) {
                    otherUserRefers.add(currentUID);
                    map = new HashMap<>();
                    map.put("referred users", new Gson().toJson(otherUserRefers));
                    users.child(tempid).updateChildren(map).addOnCompleteListener(otask -> {
                        if (otask.isSuccessful()) {
                            logFile.addLog("REFERRAL", "OTHER USER DATA IS UPDATED");
                            map = new HashMap<>();
                            map.put("referred by", tempid);
                            logFile.addLog("REFERRAL", "ADDING DATA TO CURRENT USER");
                            users.child(auth.getCurrentUser().getUid()).updateChildren(map);
                        } else {
                            logFile.addLog("REFERRAL", "FAILED TO REFER : ".concat(otask.getException().toString()));
                            PrepNestUtil.showToast(AddreferralActivity.this, "Error: try again");
                        }
                    });
                } else {
                    PrepNestUtil.showToast(AddreferralActivity.this, "You have already referred this user.");
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                logFile.addLog("REFERRAL", "FAILED TO OTHER USER DATA : ".concat(databaseError.toString()));
                PrepNestUtil.showToast(AddreferralActivity.this, databaseError.toString());
            }
        });
    }
}
