package com.cocode.prepnest;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.ResourceItemCardFullBinding;
import com.cocode.prepnest.databinding.ResourcePurchaseSheetLayoutBinding;
import com.cocode.prepnest.databinding.UserWishlistBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class UserWishlistActivity extends AppCompatActivity {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase firebase_database = FirebaseDatabase.getInstance();
    private final DatabaseReference users = firebase_database.getReference("users");
    private final DatabaseReference resources = firebase_database.getReference("resources");
    private final ArrayList<HashMap<String, Object>> wishlistedResources = new ArrayList<>();
    private HashMap<String, Object> userData = new HashMap<>();
    private UserWishlistBinding binding;
    private LogUtils logFile;
    private NetworkMonitor networkMonitor;
    private ArrayList<String> resourceIDs = new ArrayList<>();
    private Items_listAdapter listAdapter;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = UserWishlistBinding.inflate(getLayoutInflater());
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
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        logFile.addActivity();
        designUI();
        listAdapter = new Items_listAdapter(wishlistedResources);
        binding.itemsList.setAdapter(listAdapter);
        binding.itemsList.setLayoutManager(new LinearLayoutManager(this));
        getUserData();
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
//        binding.stateImg.setBackground(new GradientDrawable() {
//            public GradientDrawable getIns(int a, int b) {
//                this.setCornerRadius(a);
//                this.setColor(b);
//                return this;
//            }
//        }.getIns((int) 360, 0xFFFAFAFA));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);

        PrepNestUtil.changeNavBarColor(this, true);
    }

    public void getUserData() {
        logFile.addLog("USER", "LOADING USER DATA");
        if (getIntent().hasExtra("user")) {
            userData = new Gson().fromJson(getIntent().getStringExtra("user"), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            logFile.addLog("USER", "DATA LOADED SUCCESSFULLY");
            if (userData.containsKey("wishlist")) {
                resourceIDs = new Gson().fromJson(userData.get("wishlist").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
            } else {
                resourceIDs = new ArrayList<>();
            }
            getResources(resourceIDs);
        } else {
            logFile.addLog("USER", "FAILED TO GET DATA");
            auth.signOut();
            PrepNestUtil.showToast(this, "An unknown error occurred, please login again");
            finishAffinity();
        }
    }


    public void getWishlistedResources() {
        logFile.addLog("WISHLIST", "GETTING USER WISHLISTED RESOURCES IDs");
        DatabaseReference dataRef = users.child(auth.getCurrentUser().getUid()).child("wishlist");
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && (dataSnapshot.getValue() != null || !dataSnapshot.getValue(String.class).isEmpty())) {
                    logFile.addLog("WISHLIST", "IDs LOADED SUCCESSFULLY");
                    resourceIDs = new Gson().fromJson(dataSnapshot.getValue(String.class), new TypeToken<ArrayList<String>>() {
                    }.getType());
                } else {
                    resourceIDs = new ArrayList<>();
                }
                getResources(resourceIDs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                logFile.addLog("WISHLIST", "FAILED TO LOAD IDs: ".concat(databaseError.toString()));
                PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred : ".concat(databaseError.toString()));
            }
        });
    }

    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }


    public void toggleEmptyState(final boolean _state) {
        binding.progressLayout.setVisibility(View.GONE);
        if (_state) {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.listLayout.setVisibility(View.VISIBLE);
        } else {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.listLayout.setVisibility(View.GONE);
        }
    }


    public void getResources(final ArrayList<String> _list) {
        if (_list.isEmpty()) {
            toggleEmptyState(false);
            return;
        }
        AtomicInteger loadedCount = new AtomicInteger(0);
        for (String id : _list) {
            logFile.addLog("WISHLIST RESOURCES", "LOADING RESOURCE: ".concat(id));
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> item = (Map<String, Object>) dataSnapshot.getValue();

                        if (item != null && !item.containsKey("discontinue") || Boolean.FALSE.equals(item.get("discontinue"))) {
                            item.put("id", id);
                            logFile.addLog("WISHLIST RESOURCES", "RESOURCE LOADED SUCCESSFULLY: ".concat(id));
                            wishlistedResources.add(0, new HashMap<>(item));
                        }
                    }

                    if (loadedCount.incrementAndGet() == _list.size()) {
                        toggleEmptyState(!wishlistedResources.isEmpty());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    logFile.addLog("WISHLIST RESOURCES", "FAILED LOADING RESOURCE: " + id);
                    toggleEmptyState(false);
                    PrepNestUtil.showToast(UserWishlistActivity.this, "Failed loading resources: " + databaseError.getMessage());
                }
            };
            resources.child(id).addListenerForSingleValueEvent(listener);
        }
    }

    public void removeWishlist(final String _ID, final int position) {
        for (int i = 0; i < resourceIDs.size(); i++) {
            if (_ID.equals(resourceIDs.get(i))) {
                resourceIDs.remove(i);
                break;
            }
        }
        String newList = new Gson().toJson(resourceIDs);
        users.child(auth.getCurrentUser().getUid()).child("wishlist").setValue(newList).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                wishlistedResources.remove(position);
                listAdapter.notifyItemRemoved(position);
                toggleEmptyState(!wishlistedResources.isEmpty());
//                PrepNestUtil.showToast(UserWishlistActivity.this, String.valueOf(wishlistedResources.isEmpty()));
                PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, false);
                PrepNestUtil.showToast(UserWishlistActivity.this, "Removed from wishlist");
            } else {
                logFile.addLog("WISHLIST", "FAILED TO UPDATE WISHLIST: " + task.getException().toString());
                PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred: " + task.getException().toString());
            }
        });
    }

    public void purchaseResourceCF(final HashMap<String, Object> resourceItem, final boolean isCash, final int position) {
        if (isCash) {
            if ((((Number) userData.get("cash")).longValue()) < (((Number) resourceItem.get("price")).longValue())) {
                PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, false);
                PrepNestUtil.showToast(UserWishlistActivity.this, "Insufficient balance!");
                return;
            }
        } else {
            if ((((Number) userData.get("coins")).longValue()) < (((Number) resourceItem.get("price")).longValue() * 5)) {
                PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, false);
                PrepNestUtil.showToast(UserWishlistActivity.this, "Insufficient balance!");
                return;
            }
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            // Force refresh token to make sure it's valid
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                // Prepare JSON payload
                Map<String, Object> data = new HashMap<>();
                data.put("buyerId", user.getUid());
                data.put("ownerId", resourceItem.get("uploader uid").toString());
                data.put("modeOfPayment", isCash ? "cash" : "coins");
                data.put("resourceId", resourceItem.get("id").toString());
                data.put("timestamp", String.valueOf(System.currentTimeMillis()));

                // Convert to JSON string
                JSONObject json = new JSONObject(data);
                // Send HTTP POST request using OkHttp
                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder().url("https://us-central1-prepnest-65133.cloudfunctions.net/purchaseResource").post(requestBody).addHeader("Authorization", "Bearer " + idToken) // <-- add token
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        runOnUiThread(() -> {
                            PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, false);
                            PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred");
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        final String resp = response.body() != null ? response.body().string() : "No response";
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                PrepNestUtil.showToast(UserWishlistActivity.this, "Purchased successfully");
                                removeWishlist(resourceItem.get("id").toString(), position);
                            });
                        } else {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, false);
                                PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred");
                            });
                        }
                    }
                });

            }).addOnFailureListener(e -> Log.e("FCM_CLIENT", "Failed to get token: " + e.getMessage()));
        } else {
            Log.e("FCM_CLIENT", "User not signed in");
        }
    }

    public void showPurchaseSheet(final HashMap<String, Object> resourceItem, final int position) {
        com.google.android.material.bottomsheet.BottomSheetDialog resource_purchase_sheet;
        resource_purchase_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UserWishlistActivity.this);
        ResourcePurchaseSheetLayoutBinding sheetbinding = ResourcePurchaseSheetLayoutBinding.inflate(getLayoutInflater());

        resource_purchase_sheet.setContentView(sheetbinding.getRoot());

        resource_purchase_sheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        sheetbinding.radiogroup.setOnCheckedChangeListener((group, checkedId) -> {
            PrepNestUtil.TransitionManager(sheetbinding.container, 150);

            if (checkedId == sheetbinding.cashRadiobutton.getId()) {
                if (userData.containsKey("cash")) {
                    sheetbinding.cashRadiobutton.setText("Cash (₹".concat(String.valueOf(((Number) userData.get("cash")).longValue()).concat(")")));
                    if (resourceItem.containsKey("price")) {
                        sheetbinding.btnBuy.setText("Buy for ₹".concat(String.valueOf(((Number) resourceItem.get("price")).longValue())));
                        sheetbinding.btnBuy.setEnabled(true);
                        sheetbinding.btnBuy.setAlpha(1f);
                    } else {
                        resource_purchase_sheet.dismiss();
                        PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred, please login again!");
                        auth.signOut();
                        finishAffinity();
                    }
                } else {
                    sheetbinding.cashRadiobutton.setText("Cash (₹0)");
                    sheetbinding.btnBuy.setText("Buy");
                    sheetbinding.btnBuy.setEnabled(false);
                    sheetbinding.btnBuy.setAlpha(0.7f);
                }
            } else if (checkedId == sheetbinding.coinsRadiobutton.getId()) {
                if (userData.containsKey("coins")) {
                    sheetbinding.coinsRadiobutton.setText("Coins (".concat(String.valueOf(((Number) userData.get("coins")).longValue()).concat(")")));
                    if (resourceItem.containsKey("price")) {
                        sheetbinding.btnBuy.setText("Buy for ".concat(String.valueOf(((Number) resourceItem.get("price")).longValue() * 5)).concat(" coins"));
                        sheetbinding.btnBuy.setEnabled(true);
                        sheetbinding.btnBuy.setAlpha(1f);
                    } else {
                        resource_purchase_sheet.dismiss();
                        PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred, please login again!");
                        auth.signOut();
                        finishAffinity();
                    }
                } else {
                    sheetbinding.coinsRadiobutton.setText("Coins (0)");
                    sheetbinding.btnBuy.setText("Buy");
                    sheetbinding.btnBuy.setEnabled(false);
                    sheetbinding.btnBuy.setAlpha(0.7f);
                }
            }
        });

        final boolean[] wishlisted = {true};
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetbinding.container.setBackground(gd);
        PrepNestUtil.roundViewWithRipple(sheetbinding.btnWishlist, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(sheetbinding.btnBuy, "#000000", 15, 0, "#000000", "#212121");
        if (resourceItem.containsKey("resource title")) {
            sheetbinding.resourceTitle.setText(resourceItem.get("resource title").toString());
        } else {
            sheetbinding.resourceTitle.setText("No title");
        }
		/*
if (_item.containsKey("rating")) {
sheetbinding.ratingTxt.setText(_item.get("rating").toString());
sheetbinding.ratingContainer.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFFFF3E0));
sheetbinding.ratingContainer.setVisibility(View.VISIBLE);
} else {
sheetbinding.ratingContainer.setVisibility(View.GONE);
}
*/
        if (resourceItem.containsKey("subject")) {
            sheetbinding.subjectNameTxt.setText(resourceItem.get("subject").toString());
            sheetbinding.subjectNameTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetbinding.subjectNameTxt.setVisibility(View.VISIBLE);
        } else {
            sheetbinding.subjectNameTxt.setVisibility(View.GONE);
        }
        if (resourceItem.containsKey("session")) {
            sheetbinding.sessionTxt.setText(resourceItem.get("session").toString());
            sheetbinding.sessionTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetbinding.sessionTxt.setVisibility(View.VISIBLE);
        } else {
            sheetbinding.sessionTxt.setVisibility(View.GONE);
        }
        if (resourceItem.containsKey("price")) {
            sheetbinding.btnBuy.setText("Buy for ₹".concat(resourceItem.get("price").toString()));
        } else {
            resource_purchase_sheet.dismiss();
            PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred, please login again!");
            auth.signOut();
            finishAffinity();
        }
        if (userData.containsKey("cash")) {
            sheetbinding.cashRadiobutton.setText("Cash (₹".concat(String.valueOf(((Number) userData.get("cash")).longValue()).concat(")")));
        } else {
            userData.put("cash", 0);
            sheetbinding.cashRadiobutton.setText("Cash (₹0)");
        }
        if (userData.containsKey("coins")) {
            sheetbinding.coinsRadiobutton.setText("Coins (".concat(String.valueOf(((Number) userData.get("coins")).longValue()).concat(")")));
        } else {
            userData.put("coins", 0);
            sheetbinding.coinsRadiobutton.setText("Coins (0)");
        }
        if (resourceItem.containsKey("id")) {
            if (resourceIDs.contains(resourceItem.get("id").toString())) {
                sheetbinding.btnWishlistTxt.setText("Remove from wishlist");
                sheetbinding.iconWishlist.setImageResource(R.drawable.icon_heart_filled);
            } else {
                sheetbinding.btnWishlistTxt.setText("Add to wishlist");
                sheetbinding.iconWishlist.setImageResource(R.drawable.icon_heart_hollow);
                wishlisted[0] = false;
            }
        }
        sheetbinding.btnWishlist.setOnClickListener(_view -> {
            PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, true);
            removeWishlist(resourceItem.get("id").toString(), position);
            sheetbinding.iconWishlist.setImageResource(R.drawable.icon_heart_hollow);
            resource_purchase_sheet.dismiss();
        });
        sheetbinding.btnBuy.setOnClickListener(_view -> {
            PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, true);
            purchaseResourceCF(resourceItem, sheetbinding.cashRadiobutton.isChecked(), position);
            resource_purchase_sheet.dismiss();
        });
        resource_purchase_sheet.show();
    }

    public class Items_listAdapter extends RecyclerView.Adapter<Items_listAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public Items_listAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.resource_item_card_full, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            ResourceItemCardFullBinding binding = ResourceItemCardFullBinding.bind(_view);

            PrepNestUtil.roundViewWithRipple(binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            if (_data.get(_position).containsKey("resource title")) {
                binding.title.setText(_data.get(_position).get("resource title").toString());
            } else {
                binding.title.setText("No title");
            }
            if (_data.get(_position).containsKey("subject")) {
                binding.subjectNameTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.subjectNameTxt.setText(_data.get(_position).get("subject").toString());
                binding.subjectNameTxt.setVisibility(View.VISIBLE);
            } else {
                binding.subjectNameTxt.setVisibility(View.GONE);
            }
            if (_data.get(_position).containsKey("session")) {
                binding.sessionTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.sessionTxt.setText(_data.get(_position).get("session").toString());
                binding.sessionTxt.setVisibility(View.VISIBLE);

                ViewGroup.MarginLayoutParams sessionTxtParams =
                        (ViewGroup.MarginLayoutParams) binding.sessionTxt.getLayoutParams();

                if (_data.get(_position).get("subject").toString().length() >= 20) {
                    binding.subAndSessionContainer.setOrientation(LinearLayout.VERTICAL);
                    sessionTxtParams.setMargins(0, (int)convertToDp(8), 0, 0);
                } else {
                    sessionTxtParams.setMargins(0, 0, 0, 0);
                    binding.subAndSessionContainer.setOrientation(LinearLayout.HORIZONTAL);
                }
                binding.sessionTxt.setLayoutParams(sessionTxtParams);
            } else {
                binding.sessionTxt.setVisibility(View.GONE);
            }
            if (_data.get(_position).containsKey("best choice")) {
                if (_data.get(_position).get("best choice").toString().equals("true")) {
                    binding.bestChoiceTag.setBackground(new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns((int) 360, 0xFF000000));
                    binding.bestChoiceTag.setVisibility(View.VISIBLE);
                } else {
                    binding.bestChoiceTag.setVisibility(View.GONE);
                }
            } else {
                binding.bestChoiceTag.setVisibility(View.GONE);
            }
            if (_data.get(_position).containsKey("recommended")) {
                if (_data.get(_position).get("recommended").toString().equals("true")) {
                    binding.recommendedTag.setBackground(new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns((int) 360, 0xFF000000));
                    binding.recommendedTag.setVisibility(View.VISIBLE);
                } else {
                    binding.recommendedTag.setVisibility(View.GONE);
                }
            } else {
                binding.recommendedTag.setVisibility(View.GONE);
            }
            if (_data.get(_position).containsKey("type")) {
                if (_data.get(_position).get("type").toString().equals("paper")) {
                    binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    if (_data.get(_position).get("type").toString().equals("notes")) {
                        binding.image.setImageResource(R.drawable.short_notes);
                    } else {
                        binding.image.setVisibility(View.INVISIBLE);
                        binding.image.setEnabled(false);
                    }
                }
            } else {
                binding.image.setVisibility(View.INVISIBLE);
                binding.image.setEnabled(false);
            }
            ViewGroup.LayoutParams rawParams = binding.container.getLayoutParams();
            ViewGroup.MarginLayoutParams paramscontainer;

            if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                paramscontainer = (ViewGroup.MarginLayoutParams) rawParams;
            } else {
                // fallback if getLayoutParams() is null or not a MarginLayoutParams
                paramscontainer = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }

            if (_position == (_data.size() - 1)) {
                paramscontainer.setMargins(
                        (int) convertToDp(20),
                        (int) convertToDp(10),
                        (int) convertToDp(20),
                        (int) convertToDp(10)
                );
            } else {
                paramscontainer.setMargins(
                        (int) convertToDp(20),
                        (int) convertToDp(10),
                        (int) convertToDp(20),
                        0
                );
            }

            paramscontainer.width = ViewGroup.LayoutParams.MATCH_PARENT;
            paramscontainer.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            binding.container.setLayoutParams(paramscontainer);

            binding.container.setOnClickListener(_view1 -> {
                int pos = _holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    showPurchaseSheet(_data.get(pos), pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return _data.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }
}
