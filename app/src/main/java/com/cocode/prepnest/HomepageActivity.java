package com.cocode.prepnest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.cocode.prepnest.databinding.HomepageBinding;
import com.cocode.prepnest.databinding.InAppBannerLayoutBinding;
import com.cocode.prepnest.databinding.ResourceItemShortBinding;
import com.cocode.prepnest.databinding.ShimmerLayoutBinding;
import com.cocode.prepnest.databinding.StatusViewBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;


public class HomepageActivity extends AppCompatActivity {

    private Timer _timer = new Timer();

    private HomepageBinding binding;
    private HashMap<String, Object> userData = new HashMap<>();
    private NetworkMonitor networkMonitor;
    private LogUtils logFile;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseDatabase firebase_database = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase_database.getReference("users");
    private DatabaseReference requests = firebase_database.getReference("requests/provider_requests");
    private DatabaseReference resources = firebase_database.getReference("resources");
    private DatabaseReference banners = firebase_database.getReference("other/app_banners");
    private ValueEventListener resourceListener;

    private ArrayList<HashMap<String, Object>> recentlyAddedList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> recommendedList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> bestList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> bannersList = new ArrayList<>();

    private TimerTask bannerTimer;
    private Intent toLogin = new Intent();
    private Intent toProfile = new Intent();
    private Intent toReferPage = new Intent();
    private Intent toCashManage = new Intent();
    private Intent temp = new Intent();
    private com.google.android.material.bottomsheet.BottomSheetDialog provider_verification_status_sheet;
    private Intent toBecomeProvider = new Intent();
    private Intent toAddCash = new Intent();
    private Intent toManageResources = new Intent();
    private Intent toWishlist = new Intent();
    private Intent toResources = new Intent();
    private Intent toFAQs = new Intent();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = HomepageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
//        setSupportActionBar(binding.appToolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeButtonEnabled(true);
//        binding.appToolbar.setNavigationOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View _v) {
//                onBackPressed();
//            }
//        });
//        ActionBarDrawerToggle _toggle = new ActionBarDrawerToggle(HomepageActivity.this, binding.drawerLayout, binding.appToolbar, R.string.app_name, R.string.app_name);
//        binding.drawerLayout.addDrawerListener(_toggle);
//        _toggle.syncState();


        binding.menuIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        binding.cashContainer.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View _view) {
                toAddCash.setClass(HomepageActivity.this, AddcashActivity.class);
                toAddCash.putExtra("name", userData.get("name").toString());
                startActivity(toAddCash);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                return true;
            }
        });

        binding.cashContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (binding.cashProgressbar.getVisibility() == View.GONE) {
                    toCashManage.setClass(HomepageActivity.this, CashmanageActivity.class);
                    toCashManage.putExtra("user", new Gson().toJson(userData));
                    startActivity(toCashManage);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
            }
        });

        binding.btnViewAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (binding.cashProgressbar.getVisibility() == View.GONE) {
                    toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                    toResources.putExtra("user", new Gson().toJson(userData));
                    try {
                        toResources.removeExtra("tag type");
                    } catch (Exception ignored) {

                    }
                    startActivity(toResources);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
            }
        });

        binding.seeMoreIcon1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (binding.cashProgressbar.getVisibility() == View.GONE) {
                    toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                    toResources.putExtra("tag type", "recommended");
                    toResources.putExtra("user", new Gson().toJson(userData));
                    startActivity(toResources);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
            }
        });

        binding.seeMoreIcon2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (binding.cashProgressbar.getVisibility() == View.GONE) {
                    toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                    toResources.putExtra("tag type", "best");
                    toResources.putExtra("user", new Gson().toJson(userData));
                    startActivity(toResources);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
            }
        });

        binding.drawer.profileContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toProfile.setClass(HomepageActivity.this, UserprofileActivity.class);
                startActivity(toProfile);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        binding.drawer.profileOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toProfile.setClass(HomepageActivity.this, UserprofileActivity.class);
                startActivity(toProfile);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        binding.drawer.myResourcesOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toManageResources.setClass(HomepageActivity.this, ManageresourcesActivity.class);
                startActivity(toManageResources);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        binding.drawer.wishlistOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toWishlist.setClass(HomepageActivity.this, UserWishlistActivity.class);
                toWishlist.putExtra("user", new Gson().toJson(userData));
                startActivity(toWishlist);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        binding.drawer.becomeProviderOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (userData.containsKey("provider verification status")) {
                    if (userData.get("provider verification status").toString().equals("pending")) {
                        showProviderVerificationStatusSheet(true);
                    } else {
                        if (userData.get("provider verification status").toString().equals("failed")) {
                            showProviderVerificationStatusSheet(false);
                        } else {
                            PrepNestUtil.showToast(HomepageActivity.this, "Unknown error, try login again!");
                        }
                    }
                } else {
                    toBecomeProvider.setClass(HomepageActivity.this, BecomeproviderActivity.class);
                    toBecomeProvider.putExtra("user", new Gson().toJson(userData));
                    startActivity(toBecomeProvider);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        binding.drawer.uploadResourceOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {

            }
        });

        binding.drawer.myUploadedResourcesOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {

            }
        });

        binding.drawer.referAndEarnOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toReferPage.setClass(HomepageActivity.this, ReferpageActivity.class);
                toReferPage.putExtra("user", new Gson().toJson(userData));
                startActivity(toReferPage);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        binding.drawer.reportOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {

            }
        });

        binding.drawer.faqsOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toFAQs.setClass(HomepageActivity.this, AppFaqsActivity.class);
                startActivity(toFAQs);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        });

        binding.drawer.logoutOp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all");
                FirebaseMessaging.getInstance().unsubscribeFromTopic(auth.getCurrentUser().getUid());
                FirebaseAuth.getInstance().signOut();
                toLogin.setClass(HomepageActivity.this, LoginActivity.class);
                startActivity(toLogin);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                finish();
            }
        });

//        binding.headerTitle.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                throw new RuntimeException("Test crash for DebugActivity!");
//            }
//        });
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        designUI();
        attachAdaptersToRecyclerViews();
        createShimmerView();
        requestPermissions();
        getInAppBanners();
        getUserData();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finishAffinity();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        logFile.addActivity();
        checkNewVersion();
        checkAppMaintenance();
    }

    @Override
    public void onResume() {
        super.onResume();
        networkMonitor.register();
        getUserData();
    }

    @Override
    public void onPause() {
        super.onPause();
        networkMonitor.unregister();
    }


    public void designUI() {
        PrepNestUtil.roundViewWithRipple(binding.cashContainer, "#FAFAFA", 360, 0, "#FFFFFF", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(binding.drawer.profileOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.myResourcesOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.wishlistOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.becomeProviderOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.referAndEarnOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.faqsOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.logoutOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        binding.drawer.profileContainer.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 360, 0xFFFAFAFA));
        binding.mainScroll.setHorizontalScrollBarEnabled(false);
        binding.mainScroll.setVerticalScrollBarEnabled(false);
//        getSupportActionBar().hide();
//        getWindow().setStatusBarColor(0xFFFFFFFF);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setStatusBarColor(Color.WHITE);

        binding.drawerLayout.setFitsSystemWindows(false);
        binding.drawerLayout.setStatusBarBackground(null);
    }


    public String generateDefaultName(final String _name) {
        if (_name.contains(" ")) {
            int lastIndex = _name.lastIndexOf(" ");
            return _name.substring(0, 1).concat(_name.substring(lastIndex + 1, lastIndex + 2));
        } else {
            return _name.substring(0, 1);
        }
    }


    public void getUserData() {
        users.child(auth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    binding.cashAmountTxt.setVisibility(View.GONE);
                    binding.cashProgressbar.setVisibility(View.VISIBLE);
                    userData.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        userData.put(child.getKey(), child.getValue());
                    }

                    if (userData.containsKey("profile")) {
                        if (userData.get("profile").toString().equals("null")) {
                            binding.drawer.userProfilePicture.setVisibility(View.GONE);
                            binding.drawer.defaultProfileTitle.setVisibility(View.VISIBLE);
                        } else {
                            binding.drawer.defaultProfileTitle.setVisibility(View.GONE);
                            binding.drawer.userProfilePicture.setVisibility(View.VISIBLE);
                            Glide.with(getApplicationContext()).load(Uri.parse(userData.get("profile").toString())).into(binding.drawer.userProfilePicture);
                        }
                    } else {
                        binding.drawer.userProfilePicture.setVisibility(View.GONE);
                        binding.drawer.defaultProfileTitle.setVisibility(View.VISIBLE);
                    }
                    if (userData.containsKey("name")) {
                        binding.drawer.userNameTxt.setText(userData.get("name").toString());
                        binding.drawer.defaultProfileTitle.setText(generateDefaultName(userData.get("name").toString()).toUpperCase());
                    } else {
                        binding.drawer.defaultProfileTitle.setText("U");
                        binding.drawer.userNameTxt.setText("User");
                    }
                    if (userData.containsKey("cash")) {
                        Object value = userData.get("cash");
                        long money = 0;

                        if (value instanceof Long) {
                            money = (Long) value;
                        } else if (value instanceof Double) {
                            money = ((Double) value).longValue();
                        } else if (value instanceof Integer) {
                            money = ((Integer) value).longValue();
                        } else {
                            money = 0L;
                        }
                        binding.cashAmountTxt.setText(String.valueOf((long) (money)));
                    } else {
                        binding.cashAmountTxt.setText("0");
                    }
                    if (userData.containsKey("provider")) {
                        if ((Boolean) userData.get("provider")) {
                            binding.drawer.becomeProviderOp.setVisibility(View.GONE);
                        } else {
                            binding.drawer.becomeProviderOp.setVisibility(View.VISIBLE);
                        }
                    } else {
                        binding.drawer.becomeProviderOp.setVisibility(View.VISIBLE);
                    }
                    if (userData.containsKey("provider verification status")) {
                        if (userData.get("provider verification status").toString().equals("pending")) {
                            binding.drawer.providerPendingIcon.setVisibility(View.VISIBLE);
                        } else {
                            if (userData.get("provider verification status").toString().equals("failed")) {
                                binding.drawer.providerPendingIcon.setImageResource(R.drawable.icon_error);
                            } else {
                                binding.drawer.providerPendingIcon.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        binding.drawer.providerPendingIcon.setVisibility(View.GONE);
                    }
                    loadResources();
                } else {
                    binding.drawer.defaultProfileTitle.setText("U");
                    binding.drawer.userNameTxt.setText("User");
                }
                binding.cashProgressbar.setVisibility(View.GONE);
                binding.cashAmountTxt.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError error) {

                if (!isFinishing() && !isDestroyed()) {
                    FirebaseAuth.getInstance().signOut();
                    PrepNestUtil.showToast(HomepageActivity.this, "Please login again!");
                    finishAffinity();
                }
            }
        });
    }


    public void showProviderVerificationStatusSheet(final boolean isPending) {
        provider_verification_status_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(HomepageActivity.this);
        StatusViewBinding binding = StatusViewBinding.inflate(getLayoutInflater());
        provider_verification_status_sheet.setContentView(binding.getRoot());

        provider_verification_status_sheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
            }
        });

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        binding.bg.setBackground(gd);
        binding.title.setTextSize((int) 16);
        binding.subtext.setTextSize((int) 11);
        binding.imageContainer.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 360, 0xFFFAFAFA));
        if (isPending) {
            binding.btnOk.setVisibility(View.GONE);
            binding.image.setImageResource(R.drawable.icon_pending);
            PrepNestUtil.roundViewWithRipple(binding.btnCancel, "#000000", 15, 0, "#000000", "#212121");
            binding.btnCancelTxt.setTextColor(0xFFFFFFFF);
            binding.title.setText("Pending Verification!");
            binding.subtext.setText(getString(R.string.pending_provider_verification_message));
            binding.btnCancelTxt.setText("Dismiss");
        } else {
            binding.image.setImageResource(R.drawable.icon_error);
            PrepNestUtil.roundViewWithRipple(binding.btnOk, "#000000", 15, 0, "#000000", "#212121");
            PrepNestUtil.roundViewWithRipple(binding.btnCancel, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
            binding.title.setText("Verification Failed!");
            binding.subtext.setText(getString(R.string.failed_provider_verification_message));
            binding.btnOkTxt.setText("Resend request");
            binding.btnCancelTxt.setText("Cancel request");
        }
        binding.btnOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (!isPending) {
                    toBecomeProvider.setClass(HomepageActivity.this, BecomeproviderActivity.class);
                    startActivity(toBecomeProvider);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                    provider_verification_status_sheet.dismiss();
                }
            }
        });
        binding.btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (isPending) {
                    provider_verification_status_sheet.dismiss();
                } else {
                    cancelProviderRequest();
                    provider_verification_status_sheet.dismiss();
                }
            }
        });
        provider_verification_status_sheet.setCancelable(true);
        provider_verification_status_sheet.show();
    }


    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }


    public void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100
                );
            }
        }
    }


    public void cancelProviderRequest() {
        PrepNestUtil.showLoadingDialog(this, true);
        requests.child(auth.getCurrentUser().getUid()).removeValue().addOnCompleteListener(removeTask -> {
            if (removeTask.isSuccessful()) {
                users.child(auth.getCurrentUser().getUid()).child("provider verification status").removeValue().addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        PrepNestUtil.showLoadingDialog(HomepageActivity.this, false);
                        PrepNestUtil.showToast(HomepageActivity.this, "Request cancelled successfully.");
                    } else {
                        PrepNestUtil.showLoadingDialog(HomepageActivity.this, false);
                        PrepNestUtil.showToast(HomepageActivity.this, "An unknown error occurred.");
                    }
                });
            } else {
                PrepNestUtil.showLoadingDialog(HomepageActivity.this, false);
                PrepNestUtil.showToast(HomepageActivity.this, "Failed to cancel request, try again later.");
            }
        });
    }


    public class ShimmerAdapter extends RecyclerView.Adapter<ShimmerAdapter.ShimmerViewHolder> {
        private final int itemCount;

        public ShimmerAdapter(int itemCount) {
            this.itemCount = itemCount;
        }

        public class ShimmerViewHolder extends RecyclerView.ViewHolder {
            public ShimmerViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        @NonNull
        @Override
        public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shimmer_layout, parent, false);
            return new ShimmerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
            View _view = holder.itemView;
            ShimmerLayoutBinding binding = ShimmerLayoutBinding.bind(_view);

            binding.layout.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) PrepNestUtil.getDip(HomepageActivity.this, (int) (10)), 0xFFEEEEEE));

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.layout.getLayoutParams();

            int marginStart = (int) convertToDp(10);
            int marginTop = (int) convertToDp(10);
            int marginEnd = (int) convertToDp(0);
            int marginBottom = (int) convertToDp(10);

            if (position == 0) {
                marginStart = (int) convertToDp(20);
            } else if (position == itemCount - 1) {
                marginEnd = (int) convertToDp(20);
            }

            params.setMargins(marginStart, marginTop, marginEnd, marginBottom);
            binding.layout.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return itemCount;
        }
    }

    public int compareVersions(String current, String latest) {
        String[] currParts = current.replaceAll("[^0-9.]", "").split("\\.");
        String[] latestParts = latest.replaceAll("[^0-9.]", "").split("\\.");

        int length = Math.max(currParts.length, latestParts.length);

        for (int i = 0; i < length; i++) {
            int curr = i < currParts.length ? Integer.parseInt(currParts[i]) : 0;
            int latestVal = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (curr != latestVal) {
                return Integer.compare(curr, latestVal);
            }
        }
        return 0;
    }

    public class Banner_viewpagerAdapter extends PagerAdapter {

        Context _context;
        ArrayList<HashMap<String, Object>> _data;

        public Banner_viewpagerAdapter(Context _ctx, ArrayList<HashMap<String, Object>> _arr) {
            _context = _ctx;
            _data = _arr;
        }

        public Banner_viewpagerAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _context = HomepageActivity.this;
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
            InAppBannerLayoutBinding binding = InAppBannerLayoutBinding.inflate(LayoutInflater.from(_context), _container, false);

            Glide.with(getApplicationContext()).load(Uri.parse(_data.get((int) _position).get("image url").toString())).into(binding.image);

            View _view = binding.getRoot();
            _container.addView(_view);
            return _view;
        }
    }


    public void createShimmerView() {
        binding.shimmerrecyclerview1.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.shimmerrecyclerview2.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.shimmerrecyclerview3.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.shimmerrecyclerview1.setAdapter(new ShimmerAdapter(3));
        binding.shimmerView1.startShimmer();
        binding.shimmerrecyclerview2.setAdapter(new ShimmerAdapter(3));
        binding.shimmerView2.startShimmer();
        binding.shimmerrecyclerview3.setAdapter(new ShimmerAdapter(3));
        binding.shimmerView3.startShimmer();
    }


    public void loadResources() {
        if (resourceListener != null) {
            resources.removeEventListener(resourceListener);
        }
        logFile.addLog("RESOURCES", "LOADING RESOURCES");
        resourceListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    toggleAllEmptyState();
                    return;
                }

                recentlyAddedList.clear();
                recommendedList.clear();
                bestList.clear();
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
                toggleAllEmptyState();
            }
        };

        resources.addValueEventListener(resourceListener);
    }


    public void toggleRecentListEmptyState(final boolean isVisible) {
        binding.shimmerView1.stopShimmer();
        binding.recentRealItemsContainer.setVisibility(View.VISIBLE);
        binding.shimmerView1.setVisibility(View.GONE);
        if (isVisible) {
            binding.noRecentResourcesMessage.setVisibility(View.GONE);
            binding.recentlyAddedList.setVisibility(View.VISIBLE);
        } else {
            binding.recentlyAddedList.setVisibility(View.GONE);
            binding.noRecentResourcesMessage.setVisibility(View.VISIBLE);
            binding.recentRealItemsContainer.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams paramsrecentRealItemsContainer = (LinearLayout.LayoutParams) binding.recentRealItemsContainer.getLayoutParams();

            paramsrecentRealItemsContainer.width = LinearLayout.LayoutParams.MATCH_PARENT;

            paramsrecentRealItemsContainer.height = (int) convertToDp(130);

            binding.recentRealItemsContainer.setLayoutParams(paramsrecentRealItemsContainer);

        }
    }


    public void toggleRecommendedListEmptyState(final boolean isVisible) {
        binding.shimmerView2.stopShimmer();
        binding.recommendedRealItemsContainer.setVisibility(View.VISIBLE);
        binding.shimmerView2.setVisibility(View.GONE);
        if (isVisible) {
            binding.noRecommendedResourcesMessage.setVisibility(View.GONE);
            binding.recommendedList.setVisibility(View.VISIBLE);
        } else {
            binding.recommendedList.setVisibility(View.GONE);
            binding.noRecommendedResourcesMessage.setVisibility(View.VISIBLE);
            binding.recommendedRealItemsContainer.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams paramsrecommendedRealItemsContainer = (LinearLayout.LayoutParams) binding.recommendedRealItemsContainer.getLayoutParams();

            paramsrecommendedRealItemsContainer.width = LinearLayout.LayoutParams.MATCH_PARENT;

            paramsrecommendedRealItemsContainer.height = (int) convertToDp(130);

            binding.recommendedRealItemsContainer.setLayoutParams(paramsrecommendedRealItemsContainer);

        }
    }


    public void toggleBestListEmptyState(final boolean isVisible) {
        binding.shimmerView3.stopShimmer();
        binding.bestRealItemsContainer.setVisibility(View.VISIBLE);
        binding.shimmerView3.setVisibility(View.GONE);
        if (isVisible) {
            binding.noBestResourcesMessage.setVisibility(View.GONE);
            binding.bestList.setVisibility(View.VISIBLE);
        } else {
            binding.bestList.setVisibility(View.GONE);
            binding.noBestResourcesMessage.setVisibility(View.VISIBLE);
            binding.bestRealItemsContainer.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams paramsbestRealItemsContainer = (LinearLayout.LayoutParams) binding.bestRealItemsContainer.getLayoutParams();

            paramsbestRealItemsContainer.width = LinearLayout.LayoutParams.MATCH_PARENT;

            paramsbestRealItemsContainer.height = (int) convertToDp(130);

            binding.bestRealItemsContainer.setLayoutParams(paramsbestRealItemsContainer);

        }
    }


    public void toggleAllEmptyState() {
        toggleRecentListEmptyState(false);
        toggleRecommendedListEmptyState(false);
        toggleBestListEmptyState(false);
    }


    public boolean getRequiredResources(final HashMap<String, Object> _item) {
        boolean matchesCourse = _item.get("course id").toString().equals(userData.get("course id").toString());
        boolean matchesSemester = ((Number) (_item.get("semester"))).intValue() <= (((Number) (userData.get("semester"))).intValue() + 2);
        boolean data = matchesCourse && matchesSemester;
        boolean isActive = !_item.containsKey("discontinue") || Boolean.FALSE.equals(_item.get("discontinue"));
        return data && isActive;
    }


    public void filterResources(final ArrayList<HashMap<String, Object>> _list) {
        ArrayList<HashMap<String, Object>> sortedList = new ArrayList<>(_list);
        ListMapUtils.sortListByKey(sortedList, "date of verification", true, ListMapUtils.SortType.TIMESTAMP_STRING);
        recentlyAddedList = new ArrayList<>(sortedList.stream().limit(3).collect(Collectors.toList()));
        binding.recentlyAddedList.setAdapter(new Recently_added_listAdapter(recentlyAddedList));
        if (recentlyAddedList.size() == 0) {
            toggleRecentListEmptyState(false);
        } else {
            toggleRecentListEmptyState(true);
        }
        recommendedList = new ArrayList<>(
                sortedList.stream()
                        .filter(item -> {
                            Object value = item.get("recommended");
                            return Boolean.TRUE.equals(value);
                        })
                        .limit(3)
                        .collect(Collectors.toList())
        );
        if (recommendedList.size() == 0) {
            toggleRecommendedListEmptyState(false);
        } else {
            toggleRecommendedListEmptyState(true);
        }
        binding.recommendedList.setAdapter(new Recommended_listAdapter(recommendedList));
        bestList = new ArrayList<>(
                sortedList.stream()
                        .filter(item -> {
                            Object value = item.get("best choice");
                            return Boolean.TRUE.equals(value);
                        })
                        .limit(3)
                        .collect(Collectors.toList())
        );
        if (bestList.size() == 0) {
            toggleBestListEmptyState(false);
        } else {
            toggleBestListEmptyState(true);
        }
        binding.bestList.setAdapter(new Best_listAdapter(bestList));
        logFile.addLog("RESOURCES", "LOADED SUCCESSFULLY");
    }


    public void attachAdaptersToRecyclerViews() {
        binding.recentlyAddedList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recommendedList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.bestList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }


    public String getPrettyResourceTitle(final String title) {
        if (title == null || title.trim().isEmpty()) {
            return title;
        }

        String[] words = title.trim().split("\\s+");
        if (words.length <= 1) {
            return title;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length - 1; i++) {
            result.append(words[i]);
            if (i < words.length - 2) {
                result.append(" ");
            }
        }

        result.append("\n").append(words[words.length - 1]);
        return result.toString();

    }


    public void checkNewVersion() {
        DatabaseReference newVersion = firebase_database.getReference("other/new_update");

        newVersion.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, Object> data = (HashMap<String, Object>) dataSnapshot.getValue();

                String latestVersion = data.get("latest_version").toString();

                try {
                    PackageManager pm = getPackageManager();
                    PackageInfo pInfo = pm.getPackageInfo(getPackageName(), 0);
                    String currentVersion = pInfo.versionName;

                    int result = compareVersions(currentVersion, latestVersion);

                    if (result < 0) {
                        Log.d("AppUpdateCheck", "Update available: " + latestVersion);
                        // Prompt update here
                        Intent toAppUpdate = new Intent();
                        toAppUpdate.setClass(HomepageActivity.this, NewappupdateActivity.class);
                        toAppUpdate.putExtra("data", new Gson().toJson(data));
                        startActivity(toAppUpdate);
                        overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                        finish();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

    }


    public void checkAppMaintenance() {
        DatabaseReference appMaintenance = firebase_database.getReference("other/app_maintenance/closed");

        appMaintenance.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isAppClosed = dataSnapshot.getValue(Boolean.class);

                if (isAppClosed) {
                    Intent toAppMaintenance = new Intent();
                    toAppMaintenance.setClass(HomepageActivity.this, AppmaintenanceActivity.class);
                    startActivity(toAppMaintenance);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }


    public void getInAppBanners() {
        banners.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    binding.bannersContainer.setVisibility(View.INVISIBLE);
                    return;
                }

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    HashMap<String, Object> bannerMap = new HashMap<>();
                    String imageURL = childSnapshot.child("image url").getValue(String.class);
                    bannerMap.put("image url", imageURL);
                    bannerMap.put("id", childSnapshot.getKey());
                    bannersList.add(bannerMap);
                }

                binding.bannerViewpager.setAdapter(new Banner_viewpagerAdapter(bannersList));
                automateBanner();
                binding.bannerViewpager.setVisibility(View.VISIBLE);
                binding.bannersProgressbar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                binding.bannersContainer.setVisibility(View.INVISIBLE);
            }
        });

    }


    public void automateBanner() {
        bannerTimer = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (binding.bannerViewpager.getCurrentItem() == (bannersList.size() - 1)) {
                            binding.bannerViewpager.setCurrentItem((int) 0);
                        } else {
                            binding.bannerViewpager.setCurrentItem((int) binding.bannerViewpager.getCurrentItem() + 1);
                        }
                    }
                });
            }
        };
        _timer.scheduleAtFixedRate(bannerTimer, (int) (2000), (int) (5000));
    }

    public class Recently_added_listAdapter extends RecyclerView.Adapter<Recently_added_listAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public Recently_added_listAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.resource_item_short, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            ResourceItemShortBinding binding = ResourceItemShortBinding.bind(_view);

            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _view.setLayoutParams(_lp);
            PrepNestUtil.roundViewWithRipple(binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            if (_data.get((int) _position).containsKey("resource title")) {
                binding.title.setText(getPrettyResourceTitle(_data.get((int) _position).get("resource title").toString()));
            } else {
                binding.title.setText("No title");
            }
            if (_data.get((int) _position).containsKey("subject")) {
                binding.subjectNameTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.subjectNameTxt.setText(_data.get((int) _position).get("subject").toString());
                binding.subjectNameTxt.setVisibility(View.VISIBLE);
            } else {
                binding.subjectNameTxt.setVisibility(View.GONE);
            }
            if (_data.get((int) _position).containsKey("session")) {
                binding.sessionTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.sessionTxt.setText(_data.get((int) _position).get("session").toString());
                binding.sessionTxt.setVisibility(View.VISIBLE);
            } else {
                binding.sessionTxt.setVisibility(View.GONE);
            }
            if (_data.get((int) _position).containsKey("type")) {
                if (_data.get((int) _position).get("type").toString().equals("paper")) {
                    binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    if (_data.get((int) _position).get("type").toString().equals("notes")) {
                        binding.image.setImageResource(R.drawable.short_notes);
                    } else {
                        binding.image.setVisibility(View.INVISIBLE);
                    }
                }
            } else {
                binding.image.setVisibility(View.INVISIBLE);
            }
            LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) binding.container.getLayoutParams();
            if (_position == 0) {
                paramscontainer.setMargins((int) convertToDp(20), (int) convertToDp(10), (int) 0, (int) convertToDp(10));
            } else {
                if (_position == (_data.size() - 1)) {
                    paramscontainer.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) convertToDp(20), (int) convertToDp(10));
                } else {
                    paramscontainer.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) 0, (int) convertToDp(10));
                }
            }
            binding.container.setLayoutParams(paramscontainer);

            binding.container.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View _view) {
                    if (_data.get((int) _position).containsKey("type")) {
                        toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                        toResources.putExtra("user", new Gson().toJson(userData));
                        try {
                            toResources.removeExtra("tag type");
                        } catch (Exception ignored) {

                        }
                        startActivity(toResources);
                        overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                    }
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

    public class Recommended_listAdapter extends RecyclerView.Adapter<Recommended_listAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public Recommended_listAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.resource_item_short, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            ResourceItemShortBinding binding = ResourceItemShortBinding.bind(_view);

            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _view.setLayoutParams(_lp);
            PrepNestUtil.roundViewWithRipple(binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            if (_data.get((int) _position).containsKey("resource title")) {
                binding.title.setText(getPrettyResourceTitle(_data.get((int) _position).get("resource title").toString()));
            } else {
                binding.title.setText("No title");
            }
            if (_data.get((int) _position).containsKey("subject")) {
                binding.subjectNameTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.subjectNameTxt.setText(_data.get((int) _position).get("subject").toString());
                binding.subjectNameTxt.setVisibility(View.VISIBLE);
            } else {
                binding.subjectNameTxt.setVisibility(View.GONE);
            }
            if (_data.get((int) _position).containsKey("session")) {
                binding.sessionTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.sessionTxt.setText(_data.get((int) _position).get("session").toString());
                binding.sessionTxt.setVisibility(View.VISIBLE);
            } else {
                binding.sessionTxt.setVisibility(View.GONE);
            }
            if (_data.get((int) _position).containsKey("type")) {
                if (_data.get((int) _position).get("type").toString().equals("paper")) {
                    binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    if (_data.get((int) _position).get("type").toString().equals("notes")) {
                        binding.image.setImageResource(R.drawable.short_notes);
                    } else {
                        binding.image.setVisibility(View.INVISIBLE);
                    }
                }
            } else {
                binding.image.setVisibility(View.INVISIBLE);
            }
            LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) binding.container.getLayoutParams();
            if (_position == 0) {
                paramscontainer.setMargins((int) convertToDp(20), (int) convertToDp(10), (int) 0, (int) convertToDp(10));
            } else {
                if (_position == (_data.size() - 1)) {
                    paramscontainer.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) convertToDp(20), (int) convertToDp(10));
                } else {
                    paramscontainer.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) 0, (int) convertToDp(10));
                }
            }
            binding.container.setLayoutParams(paramscontainer);
            binding.container.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View _view) {
                    if (_data.get((int) _position).containsKey("type")) {
                        toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                        toResources.putExtra("user", new Gson().toJson(userData));
                        try {
                            toResources.removeExtra("tag type");
                        } catch (Exception ignored) {

                        }
                        startActivity(toResources);
                        overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                    }
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

    public class Best_listAdapter extends RecyclerView.Adapter<Best_listAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public Best_listAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.resource_item_short, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            ResourceItemShortBinding binding = ResourceItemShortBinding.bind(_view);

            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _view.setLayoutParams(_lp);
            PrepNestUtil.roundViewWithRipple(binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            if (_data.get((int) _position).containsKey("resource title")) {
                binding.title.setText(getPrettyResourceTitle(_data.get((int) _position).get("resource title").toString()));
            } else {
                binding.title.setText("No title");
            }
            if (_data.get((int) _position).containsKey("subject")) {
                binding.subjectNameTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.subjectNameTxt.setText(_data.get((int) _position).get("subject").toString());
                binding.subjectNameTxt.setVisibility(View.VISIBLE);
            } else {
                binding.subjectNameTxt.setVisibility(View.GONE);
            }
            if (_data.get((int) _position).containsKey("session")) {
                binding.sessionTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.sessionTxt.setText(_data.get((int) _position).get("session").toString());
                binding.sessionTxt.setVisibility(View.VISIBLE);
            } else {
                binding.sessionTxt.setVisibility(View.GONE);
            }
            if (_data.get((int) _position).containsKey("type")) {
                if (_data.get((int) _position).get("type").toString().equals("paper")) {
                    binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    if (_data.get((int) _position).get("type").toString().equals("notes")) {
                        binding.image.setImageResource(R.drawable.short_notes);
                    } else {
                        binding.image.setVisibility(View.INVISIBLE);
                    }
                }
            } else {
                binding.image.setVisibility(View.INVISIBLE);
            }
            LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) binding.container.getLayoutParams();

            if (_position == 0) {
                paramscontainer.setMargins((int) convertToDp(20), (int) convertToDp(10), (int) 0, (int) convertToDp(10));
            } else {
                if (_position == (_data.size() - 1)) {
                    paramscontainer.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) convertToDp(20), (int) convertToDp(10));
                } else {
                    paramscontainer.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) 0, (int) convertToDp(10));
                }
            }
            binding.container.setLayoutParams(paramscontainer);

            binding.container.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View _view) {
                    if (_data.get((int) _position).containsKey("type")) {
                        toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                        toResources.putExtra("user", new Gson().toJson(userData));
                        try {
                            toResources.removeExtra("tag type");
                        } catch (Exception e) {

                        }
                        startActivity(toResources);
                        overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                    }
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
