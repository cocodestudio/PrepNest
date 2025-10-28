package com.cocode.prepnest;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
    private ResourcesBinding binding;
    private HashMap<String, Object> filterMap = new HashMap<>();
    private LogUtils logFile;
    private HashMap<String, Object> userData = new HashMap<>();
    private Items_listAdapter listAdapter;
    private NetworkMonitor networkMonitor;
    private ArrayList<String> wishlistedResources = new ArrayList<>();
    private ArrayList<String> purchasedResources = new ArrayList<>();
    private ArrayList<String> uploadedResources = new ArrayList<>();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = ResourcesBinding.inflate(getLayoutInflater());
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

        binding.filterIcon.setOnClickListener(_view -> showFilterSheet());
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        logFile.addActivity();
        designUI();
        attachAdapterToRecyclerView();
        getUserData();
        setDefaultFilters();
        loadResources();
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }


    public void showFilterSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog filter_sheet;
        filter_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(ResourcesActivity.this);
        ResourcesFilterSheetBinding sheetbinding = ResourcesFilterSheetBinding.inflate(getLayoutInflater());

        filter_sheet.setContentView(sheetbinding.getRoot());

        filter_sheet.setOnShowListener(dialog -> {
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
        if (filterMap.containsKey("date")) {
            if (filterMap.get("date").toString().equals("recent")) {
                setFilterOption(sheetbinding.dateSortRecentTxt, sheetbinding.dateSortOldTxt, null);
            } else {
                if (filterMap.get("date").toString().equals("old")) {
                    setFilterOption(sheetbinding.dateSortOldTxt, sheetbinding.dateSortRecentTxt, null);
                } else {
                    filterMap.put("date", "recent");
                    setFilterOption(sheetbinding.dateSortRecentTxt, sheetbinding.dateSortOldTxt, null);
                }
            }
        } else {
            filterMap.put("date", "recent");
            setFilterOption(sheetbinding.dateSortRecentTxt, sheetbinding.dateSortOldTxt, null);
        }
        if (filterMap.containsKey("type")) {
            if (filterMap.get("type").toString().equals("both")) {
                setFilterOption(sheetbinding.typeSortBothTxt, sheetbinding.typeSortMidTxt, sheetbinding.typeSortSemTxt);
            } else {
                if (filterMap.get("type").toString().equals("sem")) {
                    setFilterOption(sheetbinding.typeSortSemTxt, sheetbinding.typeSortMidTxt, sheetbinding.typeSortBothTxt);
                } else {
                    if (filterMap.get("type").toString().equals("mid")) {
                        setFilterOption(sheetbinding.typeSortMidTxt, sheetbinding.typeSortSemTxt, sheetbinding.typeSortBothTxt);
                    } else {
                        filterMap.put("type", "both");
                        setFilterOption(sheetbinding.typeSortBothTxt, sheetbinding.typeSortMidTxt, sheetbinding.typeSortSemTxt);
                    }
                }
            }
        } else {
            filterMap.put("type", "both");
            setFilterOption(sheetbinding.typeSortBothTxt, sheetbinding.typeSortMidTxt, sheetbinding.typeSortSemTxt);
        }
		/*
if (filterMap.containsKey("rating")) {
if (filterMap.get("rating").toString().equals("high")) {
setFilterOption(sheetbinding.ratingSortHighTxt, sheetbinding.ratingSortLowTxt, null);
} else {
if (filterMap.get("rating").toString().equals("low")) {
setFilterOption(sheetbinding.ratingSortLowTxt, sheetbinding.ratingSortHighTxt, null);
} else {
filterMap.put("rating", "null");
setFilterOption(sheetbinding.ratingSortHighTxt, sheetbinding.ratingSortLowTxt, sheetbinding.ratingSortHighTxt);
}
}
} else {
filterMap.put("rating", "null");
setFilterOption(sheetbinding.ratingSortHighTxt, sheetbinding.ratingSortLowTxt, sheetbinding.ratingSortHighTxt);
}
*/
        PrepNestUtil.roundViewWithRipple(sheetbinding.btnApply, "#000000", 15, 0, "#000000", "#212121");
        PrepNestUtil.roundViewWithRipple(sheetbinding.btnReset, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
        sheetbinding.dateSortRecentTxt.setOnClickListener(_view -> {
            filterMap.put("date", "recent");
            setFilterOption(sheetbinding.dateSortRecentTxt, sheetbinding.dateSortOldTxt, null);
        });
        sheetbinding.dateSortOldTxt.setOnClickListener(_view -> {
            filterMap.put("date", "old");
            setFilterOption(sheetbinding.dateSortOldTxt, sheetbinding.dateSortRecentTxt, null);
        });
        sheetbinding.typeSortSemTxt.setOnClickListener(_view -> {
            filterMap.put("type", "sem");
            setFilterOption(sheetbinding.typeSortSemTxt, sheetbinding.typeSortMidTxt, sheetbinding.typeSortBothTxt);
        });
        sheetbinding.typeSortMidTxt.setOnClickListener(_view -> {
            filterMap.put("type", "mid");
            setFilterOption(sheetbinding.typeSortMidTxt, sheetbinding.typeSortSemTxt, sheetbinding.typeSortBothTxt);
        });
        sheetbinding.typeSortBothTxt.setOnClickListener(_view -> {
            setFilterOption(sheetbinding.typeSortBothTxt, sheetbinding.typeSortSemTxt, sheetbinding.typeSortMidTxt);
            filterMap.put("type", "both");
        });
		/*
sheetbinding.ratingSortHighTxt.setOnClickListener(new View.OnClickListener() {
@Override
public void onClick(View _view) {
filterMap.put("rating", "high");
setFilterOption(sheetbinding.ratingSortHighTxt, sheetbinding.ratingSortLowTxt, null);
}
});
sheetbinding.ratingSortLowTxt.setOnClickListener(new View.OnClickListener() {
@Override
public void onClick(View _view) {
filterMap.put("rating", "low");
setFilterOption(sheetbinding.ratingSortLowTxt, sheetbinding.ratingSortHighTxt, null);
}
});
*/
        sheetbinding.btnApply.setOnClickListener(_view -> {
            binding.progressLinear.setVisibility(View.VISIBLE);
            binding.emptyStateLinear.setVisibility(View.GONE);
            binding.itemsList.setVisibility(View.GONE);
            loadResources();
            filter_sheet.dismiss();
        });
        sheetbinding.btnReset.setOnClickListener(_view -> {
            setDefaultFilters();
            binding.progressLinear.setVisibility(View.VISIBLE);
            binding.emptyStateLinear.setVisibility(View.GONE);
            binding.itemsList.setVisibility(View.GONE);
            loadResources();
            filter_sheet.dismiss();
        });
        filter_sheet.setCancelable(true);
        filter_sheet.show();
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


    public void loadResources() {
        logFile.addLog("RESOURCES", "LOADING RESOURCES");
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
            public void onCancelled(DatabaseError error) {
                logFile.addLog("RESOURCES", "FAILED TO LOAD RESOURCES: " + error.toString());
                binding.emptyStateLinear.setVisibility(View.VISIBLE);
                binding.progressLinear.setVisibility(View.GONE);
                binding.itemsList.setVisibility(View.GONE);
            }
        };

        resources.addListenerForSingleValueEvent(listener);
    }


    public void getUserData() {
        logFile.addLog("USER", "LOADING USER DATA");
        if (getIntent().hasExtra("user")) {
            userData = new Gson().fromJson(getIntent().getStringExtra("user"), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            logFile.addLog("USER", "DATA LOADED SUCCESSFULLY");
            if (userData.containsKey("wishlist")) {
                wishlistedResources = new Gson().fromJson(userData.get("wishlist").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
            } else {
                wishlistedResources = new ArrayList<>();
            }
            if (userData.containsKey("purchased resources")) {
                purchasedResources = new Gson().fromJson(userData.get("purchased resources").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
            } else {
                purchasedResources = new ArrayList<>();
            }
            if (userData.containsKey("uploaded resources")) {
                uploadedResources = new Gson().fromJson(userData.get("uploaded resources").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
            } else {
                uploadedResources = new ArrayList<>();
            }
        } else {
            logFile.addLog("USER", "FAILED TO GET DATA");
            auth.signOut();
            PrepNestUtil.showToast(this, "An unknown error occurred, please login again");
            finishAffinity();
        }
    }


    public void filterResources(final ArrayList<HashMap<String, Object>> list) {
        if (list.isEmpty()) {
            toggleEmptyState(false);
        } else {
            for (HashMap<String, Object> resource : list) {
                if (filterMap.containsKey("type")) {
                    if (resource.containsKey("type")) {
                        if (filterMap.get("type").toString().equals("both")) {
                            resourcesList.add(resource);
                        } else {
                            if (filterMap.get("type").toString().equals("mid")) {
                                if (resource.get("subtype").toString().equals("midterm")) {
                                    resourcesList.add(resource);
                                }
                            } else {
                                if (filterMap.get("type").toString().equals("sem")) {
                                    if (resource.get("subtype").toString().equals("semester")) {
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
                if (!filterMap.get("date").toString().equals("recent")) {
                    recentFirst = false;
                }
            }
            ListMapUtils.sortListByKey(resourcesList, "date of verification", recentFirst, ListMapUtils.SortType.TIMESTAMP_STRING);
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


    public boolean getRequiredResources(final HashMap<String, Object> _item) {
        boolean matchesCourse = _item.get("course id").toString().equals(userData.get("course id").toString());
        boolean matchesSemester = ((Number) (_item.get("semester"))).intValue() <= (((Number) (userData.get("semester"))).intValue() + 2);
        boolean data = matchesCourse && matchesSemester;
        boolean isActive = !_item.containsKey("discontinue") || Boolean.FALSE.equals(_item.get("discontinue"));
        boolean tagMatches = true;
        if (getIntent().hasExtra("tag type")) {
            if (getIntent().getStringExtra("tag type").equals("recommended")) {
                tagMatches = _item.containsKey("recommended") && Boolean.parseBoolean(_item.get("recommended").toString());
            } else if (getIntent().getStringExtra("tag type").equals("best")) {
                tagMatches = _item.containsKey("best choice") && Boolean.parseBoolean(_item.get("best choice").toString());
            }
        }
        return data && isActive && tagMatches;
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
        listAdapter = new Items_listAdapter(resourcesList);
        binding.itemsList.setAdapter(listAdapter);
    }


    public void showPurchaseSheet(final HashMap<String, Object> resourceItem) {
        com.google.android.material.bottomsheet.BottomSheetDialog resource_purchase_sheet;
        resource_purchase_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(ResourcesActivity.this);
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
                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred, please login again!");
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
                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred, please login again!");
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
            PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred, please login again!");
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
            if (wishlistedResources.contains(resourceItem.get("id").toString())) {
                sheetbinding.btnWishlistTxt.setText("Remove from wishlist");
                sheetbinding.iconWishlist.setImageResource(R.drawable.icon_heart_filled);
            } else {
                sheetbinding.btnWishlistTxt.setText("Add to wishlist");
                sheetbinding.iconWishlist.setImageResource(R.drawable.icon_heart_hollow);
                wishlisted[0] = false;
            }
        }
        sheetbinding.btnWishlist.setOnClickListener(_view -> {
            if (wishlisted[0]) {
                updateUserWishlist(resourceItem.get("id").toString(), true);
                sheetbinding.iconWishlist.setImageResource(R.drawable.icon_heart_hollow);
                sheetbinding.btnWishlistTxt.setText("Add to wishlist");
            } else {
                updateUserWishlist(resourceItem.get("id").toString(), false);
                sheetbinding.iconWishlist.setImageResource(R.drawable.icon_heart_filled);
                sheetbinding.btnWishlistTxt.setText("Remove from wishlist");
            }
            wishlisted[0] = !wishlisted[0];
        });
        sheetbinding.btnBuy.setOnClickListener(_view -> {
            PrepNestUtil.showLoadingDialog(ResourcesActivity.this, true);
            purchaseResourceCF(resourceItem, sheetbinding.cashRadiobutton.isChecked());
            resource_purchase_sheet.dismiss();
        });
        resource_purchase_sheet.show();
    }


    public void checkResource(final HashMap<String, Object> _item) {
        boolean canPurchase = true;
        if (_item.containsKey("id")) {
            if (uploadedResources.contains(_item.get("id").toString())) {
                canPurchase = false;
                toManageResources.setClass(ResourcesActivity.this, ManageresourcesActivity.class);
                toManageResources.putExtra("navigation type", "owner");
                startActivity(toManageResources);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                finish();
            }
            if (purchasedResources.contains(_item.get("id").toString())) {
                canPurchase = false;
                toManageResources.setClass(ResourcesActivity.this, ManageresourcesActivity.class);
                toManageResources.putExtra("navigation type", "purchased");
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
        String newList = new Gson().toJson(wishlistedResources);
        users.child(auth.getCurrentUser().getUid()).child("wishlist").setValue(newList).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                PrepNestUtil.showToast(ResourcesActivity.this, message);
            } else {
                logFile.addLog("WISHLIST", "FAILED TO UPDATE WISHLIST: " + task.getException().toString());
                PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred: " + task.getException().toString());
            }
        });
    }

    public void purchaseResourceCF(final HashMap<String, Object> resourceItem, final boolean isCash) {
        if (isCash) {
            if ((((Number) userData.get("cash")).longValue()) < (((Number) resourceItem.get("price")).longValue())) {
                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                PrepNestUtil.showToast(ResourcesActivity.this, "Insufficient balance!");
                return;
            }
        } else {
            if ((((Number) userData.get("coins")).longValue()) >= (((Number) resourceItem.get("price")).longValue() * 5)) {
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
                            PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                            PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred");
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        final String resp = response.body() != null ? response.body().string() : "No response";
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                PrepNestUtil.showToast(ResourcesActivity.this, "Purchased successfully");
                                purchasedResources.add(resourceItem.get("id").toString());
                                if (wishlistedResources.contains(resourceItem.get("id").toString())) {
                                    updateUserWishlist(resourceItem.get("id").toString(), true);
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred");
                            });
                        }
                    }
                });

            }).addOnFailureListener(e -> Log.e("FCM_CLIENT", "Failed to get token: " + e.getMessage()));
        } else {
            Log.e("FCM_CLIENT", "User not signed in");
        }
    }

    public void purchaseResource(final HashMap<String, Object> _item, final boolean _isCash) {
        // add to history after successful add
        DatabaseReference currentUser = users.child(auth.getCurrentUser().getUid());
        DatabaseReference history_db = firebase_database.getReference("transactions_history");
        purchasedResources.add(_item.get("id").toString());
        String newList = new Gson().toJson(purchasedResources);
        if (_isCash) {
            if ((((Number) userData.get("cash")).longValue()) >= (((Number) _item.get("price")).longValue())) {
                double newAmount = (((Number) userData.get("cash")).longValue()) - (((Number) _item.get("price")).longValue());
                currentUser.child("cash").setValue(newAmount).addOnCompleteListener(deductCash -> {
                    if (deductCash.isSuccessful()) {
                        userData.put("cash", newAmount);
                        // add id to purchased resources
                        currentUser.child("purchased resources").setValue(newList).addOnCompleteListener(addItem -> {
                            if (addItem.isSuccessful()) {
                                HashMap<String, Object> history = new HashMap<>();
                                String key = history_db.child(auth.getCurrentUser().getUid()).push().getKey();
                                history.put("type", "purchase");
                                history.put("amount", ((Number) _item.get("price")).longValue());
                                history.put("amount type", "cash");
                                history.put("timestamp", String.valueOf(System.currentTimeMillis()));
                                history_db.child(auth.getCurrentUser().getUid()).child(key).setValue(history).addOnCompleteListener(addHistory -> {
                                    if (addHistory.isSuccessful()) {
                                        PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                        PrepNestUtil.showToast(ResourcesActivity.this, "Successfully purchased!");
                                    } else {
                                        PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred!");
                                    }
                                });
                                addReferralReward(_item.get("uploader uid").toString(), currentUser, history_db);
                                addPurchaseReward(_item, currentUser, history_db);
                                addResourceSoldData(_item, users, history_db, true);
                                if (wishlistedResources.contains(_item.get("id").toString())) {
                                    updateUserWishlist(_item.get("id").toString(), true);
                                }
                            } else {
                                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred!");
                                double previousAmount = (((Number) userData.get("cash")).longValue()) + (((Number) _item.get("price")).longValue());
                                userData.put("cash", previousAmount);
                                currentUser.child("cash").setValue(previousAmount);
                            }
                        });
                    } else {
                        PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred!");
                        double previousAmount = (((Number) userData.get("cash")).longValue()) + (((Number) _item.get("price")).longValue());
                        userData.put("cash", previousAmount);
                        currentUser.child("cash").setValue(previousAmount);
                    }
                });

            } else {
                purchasedResources.remove(purchasedResources.size() - 1);
                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                PrepNestUtil.showToast(ResourcesActivity.this, "Insufficient balance!");
            }
        } else {
            if ((((Number) userData.get("coins")).longValue()) >= (((Number) _item.get("price")).longValue() * 5)) {
                double newAmount = (((Number) userData.get("coins")).longValue()) - (((Number) _item.get("price")).longValue() * 5);
                currentUser.child("coins").setValue(newAmount).addOnCompleteListener(deductCash -> {
                    if (deductCash.isSuccessful()) {
                        userData.put("coins", newAmount);
                        // add id to purchased resources
                        currentUser.child("purchased resources").setValue(newList).addOnCompleteListener(addItem -> {
                            if (addItem.isSuccessful()) {
                                HashMap<String, Object> history = new HashMap<>();
                                String key = history_db.child(auth.getCurrentUser().getUid()).push().getKey();
                                history.put("type", "purchase");
                                history.put("amount", ((Number) _item.get("price")).longValue() * 5);
                                history.put("amount type", "coins");
                                history.put("timestamp", String.valueOf(System.currentTimeMillis()));
                                history_db.child(auth.getCurrentUser().getUid()).child(key).setValue(history).addOnCompleteListener(addHistory -> {
                                    if (addHistory.isSuccessful()) {
                                        PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                        PrepNestUtil.showToast(ResourcesActivity.this, "Successfully purchased!");
                                    } else {
                                        PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred!");
                                    }
                                });

                                addReferralReward(_item.get("uploader uid").toString(), currentUser, history_db);
                                addPurchaseReward(_item, currentUser, history_db);
                                addResourceSoldData(_item, users, history_db, false);
                                if (wishlistedResources.contains(_item.get("id").toString())) {
                                    updateUserWishlist(_item.get("id").toString(), true);
                                }
                            } else {
                                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred!");
                                double previousAmount = (((Number) userData.get("coins")).longValue()) + (((Number) _item.get("price")).longValue() * 5);
                                userData.put("coins", previousAmount);
                                currentUser.child("coins").setValue(previousAmount);
                            }
                        });
                    } else {
                        PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                        PrepNestUtil.showToast(ResourcesActivity.this, "An unknown error occurred!");
                        double previousAmount = (((Number) userData.get("coins")).longValue()) + (((Number) _item.get("price")).longValue() * 5);
                        userData.put("coins", previousAmount);
                        currentUser.child("coins").setValue(previousAmount);
                    }
                });

            } else {
                purchasedResources.remove(purchasedResources.size() - 1);
                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                PrepNestUtil.showToast(ResourcesActivity.this, "Insufficient balance!");
            }
        }
    }


    public void addReferralReward(final String _otherUserID, final DatabaseReference _currentUser, final DatabaseReference _history_db) {
        checkFirstItem(_currentUser, result -> {
            if (result) {
                final long[] totalAmount = {((Number) userData.get("coins")).longValue() + 10};
                _currentUser.child("coins").setValue(totalAmount[0]).addOnCompleteListener(updatedAmount -> {
                    if (updatedAmount.isSuccessful()) {
                        userData.put("coins", totalAmount[0]);
                        _currentUser.child("first purchase").setValue(true).addOnCompleteListener(purchase -> {
                            if (purchase.isSuccessful()) {
                                AtomicReference<HashMap<String, Object>> historyRef = new AtomicReference<>(new HashMap<>());
                                AtomicReference<String> keyRef = new AtomicReference<>(_history_db.child(auth.getCurrentUser().getUid()).push().getKey());
                                historyRef.get().put("type", "refer_reward");
                                historyRef.get().put("amount", 10);
                                historyRef.get().put("timestamp", String.valueOf(System.currentTimeMillis()));
                                _history_db.child(auth.getCurrentUser().getUid()).child(keyRef.get()).setValue(historyRef.get());
                                users.child(_otherUserID).child("coins").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            Long coinsAmount = dataSnapshot.getValue(Long.class);

                                            users.child(_otherUserID).child("coins").setValue(coinsAmount + 50).addOnCompleteListener(otherUser -> {
                                                if (otherUser.isSuccessful()) {
                                                    sendNotifications(_otherUserID, "Referral reward 💰", "50 coins has been successfully credited to your account for a successful refer 💰.");
                                                    historyRef.set(new HashMap<>());
                                                    keyRef.set(_history_db.child(_otherUserID).push().getKey());

                                                    historyRef.get().put("type", "refer_success");
                                                    historyRef.get().put("amount", 50);
                                                    historyRef.get().put("timestamp", String.valueOf(System.currentTimeMillis()));

                                                    _history_db.child(_otherUserID).child(keyRef.get()).setValue(historyRef.get());
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            } else {
                                totalAmount[0] -= 10;
                                userData.put("coins", totalAmount[0]);
                                _currentUser.child("coins").setValue(totalAmount[0]);
                            }
                        });
                    } else {
                        totalAmount[0] -= 10;
                        userData.put("coins", totalAmount[0]);
                        _currentUser.child("coins").setValue(totalAmount[0]);
                    }
                });

            }
        });

    }


    public void checkFirstItem(final DatabaseReference _currentUser, final BooleanCallback _callback) {
        _currentUser.child("first purchase").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    boolean value = Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class));
                    _callback.onResult(!value);
                } else {
                    _callback.onResult(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                _callback.onResult(false);
            }
        });

    }

    public void addPurchaseReward(final HashMap<String, Object> _item, final DatabaseReference _currentUser, final DatabaseReference _history_db) {
        double resourcePrice = ((Number) _item.get("price")).doubleValue();
        long userCoinsAmount = ((Number) userData.get("coins")).longValue();
        final long[] rewardedAmount = {(long) (resourcePrice * 0.7)};
        _currentUser.child("coins").setValue(userCoinsAmount + rewardedAmount[0]).addOnSuccessListener(unused -> {
            HashMap<String, Object> history = new HashMap<>();
            String key = _history_db.child(auth.getCurrentUser().getUid()).push().getKey();
            history.put("amount", rewardedAmount[0]);
            history.put("type", "purchase_reward");
            history.put("timestamp", String.valueOf(System.currentTimeMillis()));
            _history_db.child(auth.getCurrentUser().getUid()).child(key).setValue(history);
        });
    }

    public void addResourceSoldData(final HashMap<String, Object> _item, final DatabaseReference _users, final DatabaseReference _history_db, final boolean _isCash) {
        final long[] amount = {((Number) _item.get("price")).longValue()};
        if (_isCash) amount[0] *= 0.3;
        else amount[0] *= 1.5;
        DatabaseReference otherUser = _users.child(_item.get("uploader uid").toString()).child(_isCash ? "cash" : "coins");

        otherUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Long userAmount = dataSnapshot.getValue(Long.class);
                otherUser.setValue(userAmount + amount[0]).addOnSuccessListener(unused -> {
                    if (_isCash) {
                        sendNotifications(_item.get("uploader uid").toString(), "New Purchase 💰", "₹".concat(String.valueOf(amount[0]).concat(" has been credited to your account 💰.")));
                    } else {
                        sendNotifications(_item.get("uploader uid").toString(), "New Purchase 💰", String.valueOf(amount[0]).concat(" coins has been successfully credited to your account 💰."));
                    }
                    HashMap<String, Object> history = new HashMap<>();
                    String key = _history_db.child(auth.getCurrentUser().getUid()).push().getKey();

                    history.put("type", "resource_sold");
                    history.put("amount type", _isCash ? "cash" : "coins");
                    history.put("amount", amount[0]);
                    history.put("timestamp", String.valueOf(System.currentTimeMillis()));
                    _history_db.child(_item.get("uploader uid").toString()).child(key).setValue(history);
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

    }

    public void sendNotifications(final String topic, final String title, final String message) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            // Force refresh token to make sure it's valid
            user.getIdToken(true).addOnSuccessListener(result -> {
                String idToken = result.getToken();

                // Prepare JSON payload
                Map<String, Object> data = new HashMap<>();
                data.put("topic", topic);
                data.put("title", title);
                data.put("body", message);

                // Convert to JSON string
                JSONObject json = new JSONObject(data);

                // Send HTTP POST request using OkHttp
                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));

                Request request = new Request.Builder().url("https://us-central1-prepnest-65133.cloudfunctions.net/sendNotification").post(requestBody).addHeader("Authorization", "Bearer " + idToken) // <-- add token
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("FCM_CLIENT", "Failed to send notification: " + e.getMessage());
                        runOnUiThread(() -> {
                            PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                            PrepNestUtil.showToast(ResourcesActivity.this, "Failed to send notification");
                        });
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        final String resp = response.body() != null ? response.body().string() : "No response";
                        Log.d("FCM_CLIENT", "Response: " + resp);

                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                PrepNestUtil.showToast(ResourcesActivity.this, "Sent successfully");
                            });
                        } else {
                            runOnUiThread(() -> {
                                PrepNestUtil.showLoadingDialog(ResourcesActivity.this, false);
                                PrepNestUtil.showToast(ResourcesActivity.this, "Failed to send notification");
                            });
                        }
                    }
                });

            }).addOnFailureListener(e -> Log.e("FCM_CLIENT", "Failed to get token: " + e.getMessage()));
        } else {
            Log.e("FCM_CLIENT", "User not signed in");
        }
    }


    public interface BooleanCallback {
        void onResult(boolean result);
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
                    }
                }
            } else {
                binding.image.setVisibility(View.INVISIBLE);
            }
            ViewGroup.LayoutParams rawParams = binding.container.getLayoutParams();
            ViewGroup.MarginLayoutParams paramscontainer;

            if (rawParams instanceof ViewGroup.MarginLayoutParams) {
                paramscontainer = (ViewGroup.MarginLayoutParams) rawParams;
            } else {
                // Fallback if getLayoutParams() is null or not a MarginLayoutParams
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
                    checkResource(_data.get(pos));
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
