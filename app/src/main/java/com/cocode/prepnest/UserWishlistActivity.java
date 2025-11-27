package com.cocode.prepnest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class UserWishlistActivity extends AppCompatActivity {

    private final Timer timer = new Timer();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference users = database.getReference("users");
    private final DatabaseReference resources = database.getReference("resources");
    private final ArrayList<HashMap<String, Object>> wishlistedResources = new ArrayList<>();
    private final Gson gson = new Gson();
    private TimerTask timerTask;
    private HashMap<String, Object> userData = new HashMap<>();
    private UserWishlistBinding binding;
    private NetworkMonitor networkMonitor;
    private ArrayList<String> resourceIDs = new ArrayList<>();
    private ItemsListAdapter listAdapter;
    private SharedPreferences cachedData;

    public static <T> T getValue(HashMap<String, Object> map, String key) {
        return (T) map.get(key);
    }

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
        cachedData = getSharedPreferences("userCachedData", Activity.MODE_PRIVATE);

        binding.backIcon.setOnClickListener(_view -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
        });
    }

    private void initializeLogic() {
        networkMonitor = new NetworkMonitor(this);
        designUI();
        listAdapter = new ItemsListAdapter(wishlistedResources);
        binding.itemsList.setAdapter(listAdapter);
        binding.itemsList.setLayoutManager(new LinearLayoutManager(this));
        loadUserDataFromSP();
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
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }

    public void loadUserDataFromSP() {
        if (cachedData.contains("userData")) {
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, Object>>() {
            }.getType();

            userData = gson.fromJson(cachedData.getString("userData", "{}"), type);

            if (userData.containsKey("wishlist")) {
                resourceIDs = getValue(userData, "wishlist");
            } else {
                resourceIDs = new ArrayList<>();
            }
            getResources(resourceIDs);
        } else {
            PrepNestUtil.showToast(this, "An unknown error occurred, try again!");
            finish();
        }
    }

    public void getUserData() {
        if (getIntent().hasExtra("user")) {
            userData = new Gson().fromJson(getIntent().getStringExtra("user"), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            assert userData != null;
            if (userData.containsKey("wishlist")) {
                resourceIDs = new Gson().fromJson(Objects.requireNonNull(userData.get("wishlist")).toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
            } else {
                resourceIDs = new ArrayList<>();
            }
            getResources(resourceIDs);
        } else {
            auth.signOut();
            PrepNestUtil.showToast(this, "An unknown error occurred, please login again");
            finishAffinity();
        }
    }


    public void getWishlistedResources() {
        assert auth.getCurrentUser() != null;
        DatabaseReference dataRef = users.child(auth.getCurrentUser().getUid()).child("wishlist");
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && (dataSnapshot.getValue() != null || !Objects.requireNonNull(dataSnapshot.getValue(String.class)).isEmpty())) {
                    resourceIDs = new Gson().fromJson(dataSnapshot.getValue(String.class), new TypeToken<ArrayList<String>>() {
                    }.getType());
                } else {
                    resourceIDs = new ArrayList<>();
                }
                getResources(resourceIDs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
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
        // Case: empty wishlist
        if (_list == null || _list.isEmpty()) {
            toggleEmptyState(false);
            return;
        }


        AtomicInteger loadedCount = new AtomicInteger(0);
        wishlistedResources.clear();

        for (String id : _list) {
            final String finalId = id;

            DatabaseReference resourceRef = resources.child(finalId);

            resourceRef.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot resourceSnap) {

                    synchronized (wishlistedResources) {
                        // Remove old entry with same ID
                        wishlistedResources.removeIf(map ->
                                finalId.equals(map.get("id")));

                        if (resourceSnap.exists()) {
                            Map<String, Object> item =
                                    (Map<String, Object>) resourceSnap.getValue();

                            if (item != null) {
                                // Check isDiscontinue flag safely
                                boolean isDiscontinued =
                                        Boolean.TRUE.equals(item.get("isDiscontinue"));

                                if (!isDiscontinued) {
                                    int slashIndex = finalId.lastIndexOf("/");
                                    if (slashIndex != -1) {
                                        String resId = finalId.substring(slashIndex + 1);
                                        item.put("id", resId);
                                    } else {
                                        item.put("id", finalId);
                                    }

                                    wishlistedResources.add(0, new HashMap<>(item));
                                }
                            }
                        }
                    }

                    // Count only once per ID
                    if (loadedCount.incrementAndGet() == _list.size()) {
                        listAdapter.notifyDataSetChanged();
                        toggleEmptyState(!wishlistedResources.isEmpty());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    PrepNestUtil.showToast(
                            UserWishlistActivity.this,
                            "Failed loading resources: " + error.getMessage()
                    );

                    // Count load anyway
                    if (loadedCount.incrementAndGet() == _list.size()) {
                        toggleEmptyState(!wishlistedResources.isEmpty());
                    }
                }
            });
        }
    }


    public void removeWishlist(final String _ID, final int position) {
        for (int i = 0; i < resourceIDs.size(); i++) {
            if (_ID.equals(resourceIDs.get(i))) {
                resourceIDs.remove(i);
                break;
            }
        }
//        String newList = new Gson().toJson(resourceIDs);
        assert auth.getCurrentUser() != null;
        users.child(auth.getCurrentUser().getUid()).child("wishlist").setValue(resourceIDs).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                wishlistedResources.remove(position);
                listAdapter.notifyItemRemoved(position);
                toggleEmptyState(!wishlistedResources.isEmpty());
//                PrepNestUtil.showToast(UserWishlistActivity.this, String.valueOf(wishlistedResources.isEmpty()));
                PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, false);
                PrepNestUtil.showToast(UserWishlistActivity.this, "Removed from wishlist");
                userData.put("wishlist", resourceIDs);
                cachedData.edit().putString("userData", gson.toJson(userData)).apply();
            } else {
                PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred: " + Objects.requireNonNull(task.getException()));
            }
        });
    }

    public void purchaseResourceCF(final HashMap<String, Object> resourceItem, final boolean isCash, final int position) {
        if (isCash) {
            if ((((Number) Objects.requireNonNull(userData.get("cash"))).longValue()) < (((Number) Objects.requireNonNull(resourceItem.get("price"))).longValue())) {
                PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, false);
                PrepNestUtil.showToast(UserWishlistActivity.this, "Insufficient balance!");
                return;
            }
        } else {
            if ((((Number) Objects.requireNonNull(userData.get("coins"))).longValue()) < (((Number) Objects.requireNonNull(resourceItem.get("price"))).longValue() * 5)) {
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
                data.put("ownerId", Objects.requireNonNull(resourceItem.get("uploaderId")).toString());
                data.put("modeOfPayment", isCash ? "cash" : "coins");
                data.put("resourceId", Objects.requireNonNull(resourceItem.get("id")).toString());
                Log.d("resourceId", Objects.requireNonNull(resourceItem.get("id")).toString());
                data.put("courseId", Objects.requireNonNull(resourceItem.get("courseId")).toString());
                Log.d("courseId", Objects.requireNonNull(resourceItem.get("courseId")).toString());
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
                            Log.d("ERROR", e.toString());
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        final String resp = response.body() != null ? response.body().string() : "No response";
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                PrepNestUtil.showToast(UserWishlistActivity.this, "Purchased successfully");
                                final String resourceIdPath = Objects
                                        .requireNonNull(resourceItem.get("courseId")).toString()
                                        .concat("/")
                                        .concat(Objects.requireNonNull(resourceItem.get("id")).toString());
                                removeWishlist(resourceIdPath, position);
                            });
                        } else {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, false);
                                PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred");
                                Log.d("SUCCESS FAILED", resp);
                            });
                        }
                    }
                });

            }).addOnFailureListener(e -> Log.e("FCM_CLIENT", "Failed to get token: " + e.getMessage()));
        } else {
            Log.e("FCM_CLIENT", "User not signed in");
        }
    }

    @SuppressLint("SetTextI18n")
    public void showPurchaseSheet(final HashMap<String, Object> resourceItem, final int position) {
        com.google.android.material.bottomsheet.BottomSheetDialog resource_purchase_sheet;
        resource_purchase_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UserWishlistActivity.this);
        ResourcePurchaseSheetLayoutBinding sheetBinding = ResourcePurchaseSheetLayoutBinding.inflate(getLayoutInflater());

        resource_purchase_sheet.setContentView(sheetBinding.getRoot());

        resource_purchase_sheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        sheetBinding.radiogroup.setOnCheckedChangeListener((group, checkedId) -> {
            PrepNestUtil.TransitionManager(sheetBinding.container, 150);

            if (checkedId == sheetBinding.cashRadiobutton.getId()) {
                if (userData.containsKey("cash")) {
                    sheetBinding.cashRadiobutton.setText("Cash (₹".concat(String.valueOf(((Number) Objects.requireNonNull(userData.get("cash"))).longValue()).concat(")")));
                    if (resourceItem.containsKey("price")) {
                        sheetBinding.btnBuy.setText("Buy for ₹".concat(String.valueOf(((Number) Objects.requireNonNull(resourceItem.get("price"))).longValue())));
                        sheetBinding.btnBuy.setEnabled(true);
                        sheetBinding.btnBuy.setAlpha(1f);
                    } else {
                        resource_purchase_sheet.dismiss();
                        PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred, please login again!");
                        auth.signOut();
                        finishAffinity();
                    }
                } else {
                    sheetBinding.cashRadiobutton.setText("Cash (₹0)");
                    sheetBinding.btnBuy.setText("Buy");
                    sheetBinding.btnBuy.setEnabled(false);
                    sheetBinding.btnBuy.setAlpha(0.7f);
                }
            } else if (checkedId == sheetBinding.coinsRadiobutton.getId()) {
                if (userData.containsKey("coins")) {
                    sheetBinding.coinsRadiobutton.setText("Coins (".concat(String.valueOf(((Number) Objects.requireNonNull(userData.get("coins"))).longValue()).concat(")")));
                    if (resourceItem.containsKey("price")) {
                        sheetBinding.btnBuy.setText("Buy for ".concat(String.valueOf(((Number) Objects.requireNonNull(resourceItem.get("price"))).longValue() * 5)).concat(" coins"));
                        sheetBinding.btnBuy.setEnabled(true);
                        sheetBinding.btnBuy.setAlpha(1f);
                    } else {
                        resource_purchase_sheet.dismiss();
                        PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred, please login again!");
                        auth.signOut();
                        finishAffinity();
                    }
                } else {
                    sheetBinding.coinsRadiobutton.setText("Coins (0)");
                    sheetBinding.btnBuy.setText("Buy");
                    sheetBinding.btnBuy.setEnabled(false);
                    sheetBinding.btnBuy.setAlpha(0.7f);
                }
            }
        });

        final String resourcePathId = Objects
                .requireNonNull(resourceItem.get("courseId")).toString()
                .concat("/")
                .concat(Objects.requireNonNull(resourceItem.get("id")).toString());
        final boolean[] wishlisted = {true};
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetBinding.container.setBackground(gd);
        PrepNestUtil.roundViewWithRipple(sheetBinding.btnWishlist, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(sheetBinding.btnBuy, "#000000", 15, 0, "#000000", "#212121");
        if (resourceItem.containsKey("resourceTitle")) {
            sheetBinding.resourceTitle.setText(Objects.requireNonNull(resourceItem.get("resourceTitle")).toString());
        } else {
            sheetBinding.resourceTitle.setText("No title");
        }
		/*
if (_item.containsKey("rating")) {
sheetBinding.ratingTxt.setText(_item.get("rating").toString());
sheetBinding.ratingContainer.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFFFF3E0));
sheetBinding.ratingContainer.setVisibility(View.VISIBLE);
} else {
sheetBinding.ratingContainer.setVisibility(View.GONE);
}
*/
        if (resourceItem.containsKey("subject")) {
            sheetBinding.subjectNameTxt.setText(Objects.requireNonNull(resourceItem.get("subject")).toString());
            sheetBinding.subjectNameTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetBinding.subjectNameTxt.setVisibility(View.VISIBLE);
        } else {
            sheetBinding.subjectNameTxt.setVisibility(View.GONE);
        }
        if (resourceItem.containsKey("session")) {
            sheetBinding.sessionTxt.setText(Objects.requireNonNull(resourceItem.get("session")).toString());
            sheetBinding.sessionTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetBinding.sessionTxt.setVisibility(View.VISIBLE);
        } else {
            sheetBinding.sessionTxt.setVisibility(View.GONE);
        }
        if (resourceItem.containsKey("price")) {
            sheetBinding.btnBuy.setText("Buy for ₹".concat(Objects.requireNonNull(resourceItem.get("price")).toString()));
        } else {
            resource_purchase_sheet.dismiss();
            PrepNestUtil.showToast(UserWishlistActivity.this, "An unknown error occurred, please login again!");
            auth.signOut();
            finishAffinity();
        }
        if (userData.containsKey("cash")) {
            sheetBinding.cashRadiobutton.setText("Cash (₹".concat(String.valueOf(((Number) Objects.requireNonNull(userData.get("cash"))).longValue()).concat(")")));
        } else {
            userData.put("cash", 0);
            sheetBinding.cashRadiobutton.setText("Cash (₹0)");
        }
        if (userData.containsKey("coins")) {
            sheetBinding.coinsRadiobutton.setText("Coins (".concat(String.valueOf(((Number) Objects.requireNonNull(userData.get("coins"))).longValue()).concat(")")));
        } else {
            userData.put("coins", 0);
            sheetBinding.coinsRadiobutton.setText("Coins (0)");
        }
        if (resourceItem.containsKey("id")) {
            if (resourceIDs.contains(resourcePathId)) {
                sheetBinding.btnWishlistTxt.setText("Remove from wishlist");
                sheetBinding.iconWishlist.setImageResource(R.drawable.icon_heart_filled);
            } else {
                sheetBinding.btnWishlistTxt.setText("Add to wishlist");
                sheetBinding.iconWishlist.setImageResource(R.drawable.icon_heart_hollow);
                wishlisted[0] = false;
            }
        }
        sheetBinding.btnWishlist.setOnClickListener(_view -> {
            PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, true);
            removeWishlist(resourcePathId, position);
            sheetBinding.iconWishlist.setImageResource(R.drawable.icon_heart_hollow);
            resource_purchase_sheet.dismiss();
        });
        sheetBinding.btnBuy.setOnClickListener(_view -> {
            PrepNestUtil.showLoadingDialog(UserWishlistActivity.this, true);
            purchaseResourceCF(resourceItem, sheetBinding.cashRadiobutton.isChecked(), position);
            resource_purchase_sheet.dismiss();
        });
        resource_purchase_sheet.show();
    }

    public class ItemsListAdapter extends RecyclerView.Adapter<ItemsListAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> list;

        public ItemsListAdapter(ArrayList<HashMap<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ResourceItemCardFullBinding resourceItemCardFullBinding = ResourceItemCardFullBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );

            return new ViewHolder(resourceItemCardFullBinding);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            PrepNestUtil.roundViewWithRipple(holder.binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            if (list.get(position).containsKey("resourceTitle")) {
                holder.binding.title.setText(Objects.requireNonNull(list.get(position).get("resourceTitle")).toString());
            } else {
                holder.binding.title.setText("No title");
            }
            if (list.get(position).containsKey("subject")) {
                holder.binding.subjectNameTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                holder.binding.subjectNameTxt.setText(Objects.requireNonNull(list.get(position).get("subject")).toString());
                holder.binding.subjectNameTxt.setVisibility(View.VISIBLE);
            } else {
                holder.binding.subjectNameTxt.setVisibility(View.GONE);
            }
            if (list.get(position).containsKey("session")) {
                holder.binding.sessionTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                holder.binding.sessionTxt.setText(Objects.requireNonNull(list.get(position).get("session")).toString());
                holder.binding.sessionTxt.setVisibility(View.VISIBLE);

                ViewGroup.MarginLayoutParams sessionTxtParams =
                        (ViewGroup.MarginLayoutParams) holder.binding.sessionTxt.getLayoutParams();

                if (Objects.requireNonNull(list.get(position).get("subject")).toString().length() >= 20) {
                    holder.binding.subAndSessionContainer.setOrientation(LinearLayout.VERTICAL);
                    sessionTxtParams.setMargins(0, (int) convertToDp(8), 0, 0);
                } else {
                    sessionTxtParams.setMargins(0, 0, 0, 0);
                    holder.binding.subAndSessionContainer.setOrientation(LinearLayout.HORIZONTAL);
                }
                holder.binding.sessionTxt.setLayoutParams(sessionTxtParams);
            } else {
                holder.binding.sessionTxt.setVisibility(View.GONE);
            }
            if (list.get(position).containsKey("isBestChoice")) {
                if (Boolean.parseBoolean(Objects.requireNonNull(list.get(position).get("isBestChoice")).toString())) {
                    holder.binding.bestChoiceTag.setBackground(new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns((int) 360, 0xFF000000));
                    holder.binding.bestChoiceTag.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.bestChoiceTag.setVisibility(View.GONE);
                }
            } else {
                holder.binding.bestChoiceTag.setVisibility(View.GONE);
            }
            if (list.get(position).containsKey("isRecommended")) {
                if (Boolean.parseBoolean(Objects.requireNonNull(list.get(position).get("isRecommended")).toString())) {
                    holder.binding.recommendedTag.setBackground(new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns((int) 360, 0xFF000000));
                    holder.binding.recommendedTag.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.recommendedTag.setVisibility(View.GONE);
                }
            } else {
                holder.binding.recommendedTag.setVisibility(View.GONE);
            }
            if (list.get(position).containsKey("type")) {
                if (Objects.requireNonNull(list.get(position).get("type")).toString().equals("paper")) {
                    holder.binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    if (Objects.requireNonNull(list.get(position).get("type")).toString().equals("notes")) {
                        holder.binding.image.setImageResource(R.drawable.short_notes);
                    } else {
                        holder.binding.image.setVisibility(View.INVISIBLE);
                        holder.binding.image.setEnabled(false);
                    }
                }
            } else {
                holder.binding.image.setVisibility(View.INVISIBLE);
                holder.binding.image.setEnabled(false);
            }
            ViewGroup.MarginLayoutParams paramsContainer = getLayoutParams(holder);

            if (position == (list.size() - 1)) {
                paramsContainer.setMargins(
                        (int) convertToDp(20),
                        (int) convertToDp(10),
                        (int) convertToDp(20),
                        (int) convertToDp(10)
                );
            } else {
                paramsContainer.setMargins(
                        (int) convertToDp(20),
                        (int) convertToDp(10),
                        (int) convertToDp(20),
                        0
                );
            }

            paramsContainer.width = ViewGroup.LayoutParams.MATCH_PARENT;
            paramsContainer.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            holder.binding.container.setLayoutParams(paramsContainer);

            holder.binding.container.setOnClickListener(_view1 -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    showPurchaseSheet(list.get(pos), pos);
                }
            });
        }

        private ViewGroup.MarginLayoutParams getLayoutParams(@NonNull ViewHolder holder) {
            ViewGroup.LayoutParams rawParams = holder.binding.container.getLayoutParams();
            ViewGroup.MarginLayoutParams paramsContainer;

            if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                paramsContainer = (ViewGroup.MarginLayoutParams) rawParams;
            } else {
                // fallback if getLayoutParams() is null or not a MarginLayoutParams
                paramsContainer = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
            }
            return paramsContainer;
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ResourceItemCardFullBinding binding;

            public ViewHolder(ResourceItemCardFullBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
