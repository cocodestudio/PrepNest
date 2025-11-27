package com.cocode.prepnest;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.AddreferralBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AddreferralActivity extends AppCompatActivity {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
    private final ArrayList<String> userKeys = new ArrayList<>();
    private final Intent toHomePage = new Intent();
    private AddreferralBinding binding;
    private HashMap<String, Object> map = new HashMap<>();
    private String tempId = "";
    private HashMap<String, Object> otherUserData = new HashMap<>();
    private String currentUID = "";
    private NetworkMonitor networkMonitor;
    private ArrayList<String> otherUserRefers = new ArrayList<>();

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
        binding.skipTxt.setOnClickListener(_view -> {
            toHomePage.setClass(AddreferralActivity.this, HomepageActivity.class);
            startActivity(toHomePage);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            finish();
        });

        binding.addBtn.setOnClickListener(_view -> {
            PrepNestUtil.TransitionManager(binding.wrapperLayout, 150);
            if (binding.edittext.getText().toString().trim().isEmpty()) {
                binding.errorTxt.setText(getString(R.string.enter_referral_code));
                binding.errorTxt.setVisibility(View.VISIBLE);
            } else {
                if (validateReferralCode(binding.edittext.getText().toString().trim())) {
                    if (PrepNestUtil.isConnected(AddreferralActivity.this)) {
                        PrepNestUtil.showLoadingDialog(AddreferralActivity.this, true);
                        assert auth.getCurrentUser() != null;
                        addReferCF(auth.getCurrentUser().getUid(), tempId);
                    } else {
                        com.google.android.material.snackbar.Snackbar.make(binding.wrapperLayout, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", _view1 -> {

                        }).show();
                    }
                } else {
                    binding.errorTxt.setText(getString(R.string.invalid_referral_code));
                    binding.errorTxt.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override

            public void afterTextChanged(Editable s) {
                if (binding.errorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.wrapperLayout, 150);
                    binding.errorTxt.setVisibility(View.INVISIBLE);
                }
            }
        });

    }

    private void initializeLogic() {
        networkMonitor = new NetworkMonitor(this);
        getAllUserIDs();
        designUI();
//        updateOnChangedListener();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (view instanceof EditText) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                float x = ev.getRawX() + view.getLeft() - location[0];
                float y = ev.getRawY() + view.getTop() - location[1];

                if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom()) {
                    PrepNestUtil.hideKeyboard(AddreferralActivity.this);
                    view.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
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
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public void getAllUserIDs() {
        PrepNestUtil.showLoadingDialog(this, true);
        users
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String key = userSnapshot.getKey();
                            userKeys.add(key);
                        }
                        PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                        PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);
                        PrepNestUtil.showToast(AddreferralActivity.this, "Database error: " + databaseError.getMessage());

                    }
                });

    }


    public boolean validateReferralCode(final String _code) {
        if (!Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid().substring(0, 7).equals(_code)) {
            for (int i = 0; i < userKeys.size(); i++) {
                if (userKeys.get(i).substring(0, 7).equals(_code)) {
                    tempId = userKeys.get(i);
                    return true;
                }
            }
        }
        return false;
    }


    public void updateOnChangedListener() {
        users.child(auth.getCurrentUser().getUid()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // Handle child added
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);
                toHomePage.setClass(AddreferralActivity.this, HomepageActivity.class);
                startActivity(toHomePage);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                finish();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // Handle child removed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String previousChildName) {
                // Handle child moved
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors
            }
        });
    }

    public void addReferCF(final String newUserID, final String otherUserID) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Force refresh token to make sure it's valid
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                // Prepare JSON payload
                Map<String, Object> data = new HashMap<>();
                data.put("newUserID", newUserID);
                data.put("otherUserID", otherUserID);

                // Convert to JSON string
                JSONObject json = new JSONObject(data);

                // Send HTTP POST request using OkHttp
                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

                Request request = new Request.Builder().url("https://us-central1-prepnest-65133.cloudfunctions.net/addRefer").post(requestBody).addHeader("Authorization", "Bearer " + idToken) // <-- add token
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("ADD_REFER", "Failed to add referral: " + e.getMessage());
                        runOnUiThread(() -> {
                            PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);
                            PrepNestUtil.showToast(AddreferralActivity.this, "An unknown error occurred");
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        final String resp = response.body() != null ? response.body().string() : "No response";
                        Log.d("ADD_REFER", "Response: " + resp);
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);
                                toHomePage.setClass(AddreferralActivity.this, HomepageActivity.class);
                                startActivity(toHomePage);
                                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(AddreferralActivity.this, false);
                                PrepNestUtil.showToast(AddreferralActivity.this, "An unknown error occurred");
                            });
                        }
                    }
                });

            }).addOnFailureListener(e -> Log.e("FCM_CLIENT", "Failed to get token: " + e.getMessage()));
        } else {
            Log.e("FCM_CLIENT", "User not signed in");
        }
    }


    public void addRefer() {
        users.child(tempId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                otherUserData = dataSnapshot.getValue(new GenericTypeIndicator<>() {
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
                    users.child(tempId).updateChildren(map).addOnCompleteListener(otask -> {
                        if (otask.isSuccessful()) {
                            map = new HashMap<>();
                            map.put("referred by", tempId);
                            users.child(auth.getCurrentUser().getUid()).updateChildren(map);
                        } else {
                            PrepNestUtil.showToast(AddreferralActivity.this, "Error: try again");
                        }
                    });
                } else {
                    PrepNestUtil.showToast(AddreferralActivity.this, "You have already referred this user.");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                PrepNestUtil.showToast(AddreferralActivity.this, databaseError.toString());
            }
        });
    }
}
