package com.cocode.prepnest;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.ResourceItemCardFullBinding;
import com.cocode.prepnest.databinding.ResourcePurchaseSheetLayoutBinding;
import com.cocode.prepnest.databinding.ResourcesBinding;
import com.cocode.prepnest.databinding.ResourcesFilterSheetBinding;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ResourcesActivity extends AppCompatActivity {

    private final FirebaseDatabase firebase_database = FirebaseDatabase.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final DatabaseReference resources = firebase_database.getReference("resources");
    private final DatabaseReference users = firebase_database.getReference("users");
    private final ArrayList<HashMap<String, Object>> resourcesList = new ArrayList<>();
    private final Intent toManageResources = new Intent();
    private final Gson gson = new Gson();
    private ResourcesBinding binding;
    private HashMap<String, Object> filterMap = new HashMap<>();
    private HashMap<String, Object> userData = new HashMap<>();
    private ItemsListAdapter listAdapter;
    private NetworkMonitor networkMonitor;
    private ArrayList<String> wishlistedResources = new ArrayList<>();
    private ArrayList<String> purchasedResources = new ArrayList<>();
    private ArrayList<String> uploadedResources = new ArrayList<>();
    private SharedPreferences cachedData;

    @SuppressWarnings("unchecked")
    public static <T> T getValue(HashMap<String, Object> map, String key) {
        return (T) map.get(key);
    }

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = ResourcesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize() {
        cachedData = getSharedPreferences("userCachedData", Activity.MODE_PRIVATE);

        binding.backIcon.setOnClickListener(_view -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
        });

        binding.filterIcon.setOnClickListener(_view -> showFilterSheet());
    }

    private void initializeLogic() {
        networkMonitor = new NetworkMonitor(this);
        designUI();
        attachAdapterToRecyclerView();
        loadUserDataFromSP();
        setDefaultFilters();
        loadResourcesFromSP();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
        loadBannerAd();
    }

    private void loadBannerAd() {
        MobileAds.initialize(this, initializationStatus -> {
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adView.loadAd(adRequest);
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
        binding.itemsList.setHorizontalScrollBarEnabled(false);
        binding.itemsList.setVerticalScrollBarEnabled(false);
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }

    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }

    public void showFilterSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog filterSheet;
        filterSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(ResourcesActivity.this);
        ResourcesFilterSheetBinding sheetBinding = ResourcesFilterSheetBinding.inflate(getLayoutInflater());

        filterSheet.setContentView(sheetBinding.getRoot());

        filterSheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });


        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetBinding.container.setBackground(gd);
        if (filterMap.containsKey("date")) {
            if (Objects.requireNonNull(filterMap.get("date")).toString().equals("recent")) {
                setFilterOption(sheetBinding.dateSortRecentTxt, sheetBinding.dateSortOldTxt, null);
            } else {
                if (Objects.requireNonNull(filterMap.get("date")).toString().equals("old")) {
                    setFilterOption(sheetBinding.dateSortOldTxt, sheetBinding.dateSortRecentTxt, null);
                } else {
                    filterMap.put("date", "recent");
                    setFilterOption(sheetBinding.dateSortRecentTxt, sheetBinding.dateSortOldTxt, null);
                }
            }
        } else {
            filterMap.put("date", "recent");
            setFilterOption(sheetBinding.dateSortRecentTxt, sheetBinding.dateSortOldTxt, null);
        }
        if (filterMap.containsKey("type")) {
            if (Objects.requireNonNull(filterMap.get("type")).toString().equals("both")) {
                setFilterOption(sheetBinding.typeSortBothTxt, sheetBinding.typeSortMidTxt, sheetBinding.typeSortSemTxt);
            } else {
                if (Objects.requireNonNull(filterMap.get("type")).toString().equals("sem")) {
                    setFilterOption(sheetBinding.typeSortSemTxt, sheetBinding.typeSortMidTxt, sheetBinding.typeSortBothTxt);
                } else {
                    if (Objects.requireNonNull(filterMap.get("type")).toString().equals("mid")) {
                        setFilterOption(sheetBinding.typeSortMidTxt, sheetBinding.typeSortSemTxt, sheetBinding.typeSortBothTxt);
                    } else {
                        filterMap.put("type", "both");
                        setFilterOption(sheetBinding.typeSortBothTxt, sheetBinding.typeSortMidTxt, sheetBinding.typeSortSemTxt);
                    }
                }
            }
        } else {
            filterMap.put("type", "both");
            setFilterOption(sheetBinding.typeSortBothTxt, sheetBinding.typeSortMidTxt, sheetBinding.typeSortSemTxt);
        }
		/*
if (filterMap.containsKey("rating")) {
if (filterMap.get("rating").toString().equals("high")) {
setFilterOption(sheetBinding.ratingSortHighTxt, sheetBinding.ratingSortLowTxt, null);
} else {
if (filterMap.get("rating").toString().equals("low")) {
setFilterOption(sheetBinding.ratingSortLowTxt, sheetBinding.ratingSortHighTxt, null);
} else {
filterMap.put("rating", "null");
setFilterOption(sheetBinding.ratingSortHighTxt, sheetBinding.ratingSortLowTxt, sheetBinding.ratingSortHighTxt);
}
}
} else {
filterMap.put("rating", "null");
setFilterOption(sheetBinding.ratingSortHighTxt, sheetBinding.ratingSortLowTxt, sheetBinding.ratingSortHighTxt);
}
*/
        PrepNestUtil.roundViewWithRipple(sheetBinding.btnApply, "#000000", 15, 0, "#000000", "#212121");
        PrepNestUtil.roundViewWithRipple(sheetBinding.btnReset, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
        sheetBinding.dateSortRecentTxt.setOnClickListener(_view -> {
            filterMap.put("date", "recent");
            setFilterOption(sheetBinding.dateSortRecentTxt, sheetBinding.dateSortOldTxt, null);
        });
        sheetBinding.dateSortOldTxt.setOnClickListener(_view -> {
            filterMap.put("date", "old");
            setFilterOption(sheetBinding.dateSortOldTxt, sheetBinding.dateSortRecentTxt, null);
        });
        sheetBinding.typeSortSemTxt.setOnClickListener(_view -> {
            filterMap.put("type", "sem");
            setFilterOption(sheetBinding.typeSortSemTxt, sheetBinding.typeSortMidTxt, sheetBinding.typeSortBothTxt);
        });
        sheetBinding.typeSortMidTxt.setOnClickListener(_view -> {
            filterMap.put("type", "mid");
            setFilterOption(sheetBinding.typeSortMidTxt, sheetBinding.typeSortSemTxt, sheetBinding.typeSortBothTxt);
        });
        sheetBinding.typeSortBothTxt.setOnClickListener(_view -> {
            setFilterOption(sheetBinding.typeSortBothTxt, sheetBinding.typeSortSemTxt, sheetBinding.typeSortMidTxt);
            filterMap.put("type", "both");
        });
		/*
sheetBinding.ratingSortHighTxt.setOnClickListener(new View.OnClickListener() {
@Override
public void onClick(View _view) {
filterMap.put("rating", "high");
setFilterOption(sheetBinding.ratingSortHighTxt, sheetBinding.ratingSortLowTxt, null);
}
});
sheetBinding.ratingSortLowTxt.setOnClickListener(new View.OnClickListener() {
@Override
public void onClick(View _view) {
filterMap.put("rating", "low");
setFilterOption(sheetBinding.ratingSortLowTxt, sheetBinding.ratingSortHighTxt, null);
}
});
*/
        sheetBinding.btnApply.setOnClickListener(_view -> {
            binding.progressLinear.setVisibility(View.VISIBLE);
            binding.emptyStateLinear.setVisibility(View.GONE);
            binding.itemsList.setVisibility(View.GONE);
            loadResourcesFromSP();
            filterSheet.dismiss();
        });
        sheetBinding.btnReset.setOnClickListener(_view -> {
            setDefaultFilters();
            binding.progressLinear.setVisibility(View.VISIBLE);
            binding.emptyStateLinear.setVisibility(View.GONE);
            binding.itemsList.setVisibility(View.GONE);
            loadResourcesFromSP();
            filterSheet.dismiss();
        });
        filterSheet.setCancelable(true);
        filterSheet.show();
    }

    public void setFilterOption(final TextView op1, final TextView op2, final TextView op3) {
        PrepNestUtil.roundViewWithRipple(op1, "#000000", 360, 0, "#000000", "#212121");
        PrepNestUtil.roundViewWithRipple(op2, "#F5F5F5", 360, 0, "#000000", "#E0E0E0");
        op1.setTextColor(0xFFFFFFFF);
        op2.setTextColor(0xFF000000);
        if (op3 != null) {
            PrepNestUtil.roundViewWithRipple(op3, "#F5F5F5", 360, 0, "#000000", "#E0E0E0");
            op3.setTextColor(0xFF000000);
        }
    }

    public void setDefaultFilters() {
        filterMap = new HashMap<>();
        filterMap.put("date", "recent");
        filterMap.put("type", "both");
        filterMap.put("rating", "null");
    }

    @SuppressWarnings("unchecked")
    public void loadResources() {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    toggleEmptyState(false);
                    return;
                }

                resourcesList.clear();
                ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();

                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Map<String, Object> item = (Map<String, Object>) child.getValue();

                    if (item != null) {
                        if (getRequiredResources(new HashMap<>(item))) {
                            item.put("id", child.getKey());
                            tempList.add(new HashMap<>(item));
                        }
                    }
                }

                filterResources(tempList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.emptyStateLinear.setVisibility(View.VISIBLE);
                binding.progressLinear.setVisibility(View.GONE);
                binding.itemsList.setVisibility(View.GONE);
            }
        };

        resources
                .child(Objects.requireNonNull(userData.get("courseId")).toString())
                .addListenerForSingleValueEvent(listener);
    }

    public void loadUserDataFromSP() {
        if (cachedData.contains("userData")) {
            Type type = new TypeToken<HashMap<String, Object>>() {
            }.getType();
            userData = gson.fromJson(cachedData.getString("userData", "{}"), type);
            loadUserDataToUI();
        } else {
            PrepNestUtil.showToast(this, "An unknown error occurred, restart the app");
            finishAffinity();
        }
    }

    public void loadUserDataToUI() {
        if (userData.containsKey("wishlist")) {
            wishlistedResources = getValue(userData, "wishlist");
        } else {
            wishlistedResources = new ArrayList<>();
        }
        if (userData.containsKey("purchasedResources")) {
            purchasedResources = getValue(userData, "purchasedResources");
        } else {
            purchasedResources = new ArrayList<>();
        }
        if (userData.containsKey("uploadedResources")) {
            uploadedResources = getValue(userData, "uploadedResources");
        } else {
            uploadedResources = new ArrayList<>();
        }
    }

    public void loadResourcesFromSP() {
        if (cachedData.contains("resources")) {
            resourcesList.clear();

            Type type = new TypeToken<ArrayList<HashMap<String, Object>>>() {
            }.getType();
            ArrayList<HashMap<String, Object>> tempList = gson.fromJson(cachedData.getString("resources", "[]"), type);

            tempList.removeIf(item -> !getTaggedResource(item));
            filterResources(tempList);
        } else {
            loadResources();
        }
    }

    public void filterResources(final ArrayList<HashMap<String, Object>> list) {
        if (list.isEmpty()) {
            toggleEmptyState(false);
        } else {
            for (HashMap<String, Object> resource : list) {
                if (filterMap.containsKey("type")) {
                    if (resource.containsKey("type")) {
                        if (Objects.requireNonNull(filterMap.get("type")).toString().equals("both")) {
                            resourcesList.add(resource);
                        } else {
                            if (Objects.requireNonNull(filterMap.get("type")).toString().equals("mid")) {
                                if (Objects.requireNonNull(resource.get("subtype")).toString().equals("midterm")) {
                                    resourcesList.add(resource);
                                }
                            } else {
                                if (Objects.requireNonNull(filterMap.get("type")).toString().equals("sem")) {
                                    if (Objects.requireNonNull(resource.get("subtype")).toString().equals("semester")) {
                                        resourcesList.add(resource);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            boolean recentFirst = true;
            if (filterMap.containsKey("date")) {
                if (!(Objects.requireNonNull(filterMap.get("date")).toString().equals("recent"))) {
                    recentFirst = false;
                }
            }
            ListMapUtils.sortListByKey(resourcesList, "session", recentFirst, ListMapUtils.SortType.SESSION);
			/*
if (getIntent().hasExtra("type")) {
if (!getIntent().getStringExtra("type").equals("paper")) {
if (filterMap.containsKey("rating")) {
if (filterMap.get("rating").toString().equals("high")) {
ListMapUtils.sortListByKey(resourcesList, "rating", false, ListMapUtils.SortType.NUMBER);
} else {
if (filterMap.get("rating").toString().equals("low")) {
ListMapUtils.sortListByKey(resourcesList, "rating", true, ListMapUtils.SortType.NUMBER);
}
}
}
}
}
*/
        }
        listAdapter.notifyDataSetChanged();
        toggleEmptyState(!resourcesList.isEmpty());
    }


    public boolean getRequiredResources(final HashMap<String, Object> item) {
        boolean matchesSemester = ((Number) (Objects.requireNonNull(item.get("semester")))).intValue() <= (((Number) (Objects.requireNonNull(userData.get("semester")))).intValue() + 2);
        boolean isActive = !item.containsKey("isDiscontinue") || Boolean.FALSE.equals(item.get("isDiscontinue"));

        return matchesSemester && isActive && getTaggedResource(item);
    }

    public boolean getTaggedResource(final HashMap<String, Object> item) {
        boolean tagMatches = true;
        if (getIntent().hasExtra("tagType")) {
            if (Objects.equals(getIntent().getStringExtra("tagType"), "isRecommended")) {
                tagMatches = item.containsKey("isRecommended") && Boolean.parseBoolean(Objects.requireNonNull(item.get("isRecommended")).toString());
            } else if (Objects.equals(getIntent().getStringExtra("tagType"), "isBestChoice")) {
                tagMatches = item.containsKey("isBestChoice") && Boolean.parseBoolean(Objects.requireNonNull(item.get("isBestChoice")).toString());
            }
        }

        return tagMatches;
    }

    public void toggleEmptyState(final boolean _state) {
        binding.progressLinear.setVisibility(View.GONE);
        if (_state) {
            binding.emptyStateLinear.setVisibility(View.GONE);
            binding.itemsList.setVisibility(View.VISIBLE);
        } else {
            binding.emptyStateLinear.setVisibility(View.VISIBLE);
            binding.itemsList.setVisibility(View.GONE);
        }
    }


    public void attachAdapterToRecyclerView() {
        binding.itemsList.setLayoutManager(new LinearLayoutManager(this));
        listAdapter = new ItemsListAdapter(resourcesList);
        binding.itemsList.setAdapter(listAdapter);
    }


    public void showPurchaseSheet(final HashMap<String, Object> resourceItem) {
        com.google.android.material.bottomsheet.BottomSheetDialog resourcePurchaseSheet;
        resourcePurchaseSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(ResourcesActivity.this);
        ResourcePurchaseSheetLayoutBinding sheetBinding = ResourcePurchaseSheetLayoutBinding.inflate(getLayoutInflater());

        resourcePurchaseSheet.setContentView(sheetBinding.getRoot());

        resourcePurchaseSheet.setOnShowListener(dialog -> {
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
                        resourcePurchaseSheet.dismiss();
                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred, please login again!");
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
                        resourcePurchaseSheet.dismiss();
                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred, please login again!");
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
            sheetBinding.btnBuy.setText("Buy for ₹".concat(String.valueOf(((Number) Objects.requireNonNull(resourceItem.get("price"))).longValue())));
        } else {
            resourcePurchaseSheet.dismiss();
            PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred, please login again!");
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
            if (wishlistedResources.contains(resourcePathId)) {
                sheetBinding.btnWishlistTxt.setText("Remove from wishlist");
                sheetBinding.iconWishlist.setImageResource(R.drawable.icon_heart_filled);
            } else {
                sheetBinding.btnWishlistTxt.setText("Add to wishlist");
                sheetBinding.iconWishlist.setImageResource(R.drawable.icon_heart_hollow);
                wishlisted[0] = false;
            }
        }
        sheetBinding.btnWishlist.setOnClickListener(_view -> {
            if (wishlisted[0]) {
                updateUserWishlist(resourcePathId, true);
                sheetBinding.iconWishlist.setImageResource(R.drawable.icon_heart_hollow);
                sheetBinding.btnWishlistTxt.setText("Add to wishlist");
            } else {
                updateUserWishlist(resourcePathId, false);
                sheetBinding.iconWishlist.setImageResource(R.drawable.icon_heart_filled);
                sheetBinding.btnWishlistTxt.setText("Remove from wishlist");
            }
            wishlisted[0] = !wishlisted[0];
        });
        sheetBinding.btnBuy.setOnClickListener(_view -> {
            PrepNestUtil.showLoadingDialog(ResourcesActivity.this, true);
            purchaseResourceCF(resourceItem, sheetBinding.cashRadiobutton.isChecked());
            resourcePurchaseSheet.dismiss();
        });
        resourcePurchaseSheet.show();
    }


    public void checkResource(final HashMap<String, Object> _item) {
        boolean canPurchase = true;
        if (_item.containsKey("id")) {
            if (uploadedResources.contains(Objects.requireNonNull(_item.get("id")).toString())) {
                canPurchase = false;
                toManageResources.setClass(ResourcesActivity.this, ManageresourcesActivity.class);
                toManageResources.putExtra("navigationType", "owner");
                startActivity(toManageResources);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                finish();
            }
            if (purchasedResources.contains(Objects.requireNonNull(_item.get("id")).toString())) {
                canPurchase = false;
                toManageResources.setClass(ResourcesActivity.this, ManageresourcesActivity.class);
                toManageResources.putExtra("navigationType", "purchased");
                startActivity(toManageResources);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                finish();
            }
        }
        if (canPurchase) {
            showPurchaseSheet(_item);
        }
    }


    public void updateUserWishlist(final String _ID, final boolean _remove) {
        String message;
        if (_remove) {
            message = "Removed from wishlist";
            for (int i = 0; i < wishlistedResources.size(); i++) {
                if (_ID.equals(wishlistedResources.get(i))) {
                    wishlistedResources.remove(i);
                    break;
                }
            }
        } else {
            wishlistedResources.add(_ID);
            message = "Added to wishlist";
        }
//        String newList = new Gson().toJson(wishlistedResources);
        assert auth.getCurrentUser() != null;
        users
                .child(auth.getCurrentUser().getUid())
                .child("wishlist")
                .setValue(wishlistedResources).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        PrepNestUtil.showToast(ResourcesActivity.this, message);
                        userData.put("wishlist", wishlistedResources);
                        cachedData.edit().putString("userData", gson.toJson(userData)).apply();
                    } else {
                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred: " + Objects.requireNonNull(task.getException()));
                    }
                });
    }

    public void purchaseResourceCF(final HashMap<String, Object> resourceItem, final boolean isCash) {
        if (isCash) {
            if ((((Number) Objects.requireNonNull(userData.get("cash"))).longValue()) < (((Number) Objects.requireNonNull(resourceItem.get("price"))).longValue())) {
                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                PrepNestUtil.showToast(ResourcesActivity.this, "Insufficient balance!");
                return;
            }
        } else {
            if ((((Number) Objects.requireNonNull(userData.get("coins"))).longValue()) >= (((Number) Objects.requireNonNull(resourceItem.get("price"))).longValue() * 5)) {
                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                PrepNestUtil.showToast(ResourcesActivity.this, "Insufficient balance!");
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
                data.put("courseId", Objects.requireNonNull(resourceItem.get("courseId")).toString());
                data.put("timestamp", System.currentTimeMillis());

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
                            PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                            PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred");
                            Log.d("ERROR", e.toString());
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        final String resp = response.body() != null ? response.body().string() : "No response";
                        if (response.isSuccessful()) {
                            final String resourcePathId = Objects
                                    .requireNonNull(resourceItem.get("courseId")).toString()
                                    .concat("/")
                                    .concat(Objects.requireNonNull(resourceItem.get("id")).toString());
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                PrepNestUtil.showToast(ResourcesActivity.this, "Purchased successfully");
                                purchasedResources.add(Objects.requireNonNull(resourceItem.get("id")).toString());
                                if (wishlistedResources.contains(resourcePathId)) {
                                    updateUserWishlist(resourcePathId, true);
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred");
                                Log.d("ERROR IN SUCCESS", resp);
                            });
                        }
                    }
                });

            }).addOnFailureListener(e -> Log.e("FCM_CLIENT", "Failed to get token: " + e.getMessage()));
        } else {
            Log.e("FCM_CLIENT", "User not signed in");
        }
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
                    }
                }
            } else {
                holder.binding.image.setVisibility(View.INVISIBLE);
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
                    checkResource(list.get(pos));
                }
            });
        }

        private ViewGroup.MarginLayoutParams getLayoutParams(@NonNull ViewHolder holder) {
            ViewGroup.LayoutParams rawParams = holder.binding.container.getLayoutParams();
            ViewGroup.MarginLayoutParams paramsContainer;

            if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                paramsContainer = (ViewGroup.MarginLayoutParams) rawParams;
            } else {
                // Fallback if getLayoutParams() is null or not a MarginLayoutParams
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
