package com.cocode.prepnest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cocode.prepnest.databinding.HomepageBinding;
import com.cocode.prepnest.databinding.InAppBannerLayoutBinding;
import com.cocode.prepnest.databinding.ResourceItemShortBinding;
import com.cocode.prepnest.databinding.ShimmerLayoutBinding;
import com.cocode.prepnest.databinding.StatusViewBinding;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.takusemba.spotlight.Spotlight;
import com.takusemba.spotlight.Target;
import com.takusemba.spotlight.shape.Circle;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class HomepageActivity extends AppCompatActivity {

    private static final int UPDATE_REQUEST_CODE = 123;
    private final Timer _timer = new Timer();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference users = database.getReference("users");
    private final DatabaseReference requests = database.getReference("requests/providerRequests");
    private final DatabaseReference resources = database.getReference("resources");
    private final DatabaseReference banners = database.getReference("other/appBanners");
    private final ArrayList<HashMap<String, Object>> bannersList = new ArrayList<>();
    private final Intent toLogin = new Intent();
    private final Intent toProfile = new Intent();
    private final Intent toReferPage = new Intent();
    private final Intent toCashManage = new Intent();
    private final Intent toBecomeProvider = new Intent();
    private final Intent toAddCash = new Intent();
    private final Intent toManageResources = new Intent();
    private final Intent toWishlist = new Intent();
    private final Intent toResources = new Intent();
    private final Intent toReport = new Intent();
    private final Intent toFAQs = new Intent();
    private final ArrayList<HashMap<String, Object>> recentlyAddedList = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> recommendedList = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> bestList = new ArrayList<>();
    private String jsonCourseData = null;
    private HashMap<String, Object> userData = new HashMap<>();
    private HomepageBinding binding;
    private NetworkMonitor networkMonitor;
    private com.google.android.material.bottomsheet.BottomSheetDialog providerVerificationStatusSheet;
    private AppUpdateManager appUpdateManager;
    private SharedPreferences cachedData;
    private Spotlight spotlight;
    private View overlay;
    private RecentlyAddedListAdapter recentlyAddedListAdapter;
    private RecommendedListAdapter recommendedListAdapter;
    private BestListAdapter bestListAdapter;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = HomepageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
        FirebaseApp.initializeApp(this);

        initializeLogic();
    }

    private void initialize() {
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

        cachedData = getSharedPreferences("userCachedData", Activity.MODE_PRIVATE);

        binding.menuIcon.setOnClickListener(_view -> binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.cashContainer.setOnLongClickListener(_view -> {
            toAddCash.setClass(HomepageActivity.this, AddcashActivity.class);
            toAddCash.putExtra("name", Objects.requireNonNull(userData.get("name")).toString());
            startActivity(toAddCash);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            return true;
        });

        binding.cashContainer.setOnClickListener(_view -> {
            if (binding.cashProgressbar.getVisibility() == View.GONE) {
                toCashManage.setClass(HomepageActivity.this, CashmanageActivity.class);
                toCashManage.putExtra("user", new Gson().toJson(userData));
                startActivity(toCashManage);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            }
        });

        binding.btnViewAll.setOnClickListener(_view -> {
            if (binding.cashProgressbar.getVisibility() == View.GONE) {
                toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                toResources.putExtra("user", new Gson().toJson(userData));
                try {
                    toResources.removeExtra("tagType");
                } catch (Exception ignored) {

                }
                startActivity(toResources);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            }
        });

        binding.seeMoreIcon1.setOnClickListener(_view -> {
            if (binding.cashProgressbar.getVisibility() == View.GONE) {
                toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                toResources.putExtra("tagType", "isRecommended");
                toResources.putExtra("user", new Gson().toJson(userData));
                startActivity(toResources);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            }
        });

        binding.seeMoreIcon2.setOnClickListener(_view -> {
            if (binding.cashProgressbar.getVisibility() == View.GONE) {
                toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                toResources.putExtra("tagType", "isBestChoice");
                toResources.putExtra("user", new Gson().toJson(userData));
                startActivity(toResources);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            }
        });

        binding.drawer.profileContainer.setOnClickListener(_view -> {
            toProfile.setClass(HomepageActivity.this, UserprofileActivity.class);
            startActivity(toProfile);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

        binding.drawer.profileOp.setOnClickListener(_view -> {
            toProfile.setClass(HomepageActivity.this, UserprofileActivity.class);
            startActivity(toProfile);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

        binding.drawer.myResourcesOp.setOnClickListener(_view -> {
            toManageResources.setClass(HomepageActivity.this, ManageresourcesActivity.class);
            startActivity(toManageResources);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

        binding.drawer.wishlistOp.setOnClickListener(_view -> {
            toWishlist.setClass(HomepageActivity.this, UserWishlistActivity.class);
            toWishlist.putExtra("user", new Gson().toJson(userData));
            startActivity(toWishlist);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

        binding.drawer.becomeProviderOp.setOnClickListener(_view -> {
            if (userData.containsKey("providerVerificationStatus")) {
                if (Objects.requireNonNull(userData.get("providerVerificationStatus")).toString().equals("pending")) {
                    showProviderVerificationStatusSheet(true);
                } else {
                    if (Objects.requireNonNull(userData.get("providerVerificationStatus")).toString().equals("failed")) {
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
        });

        binding.drawer.referAndEarnOp.setOnClickListener(_view -> {
            toReferPage.setClass(HomepageActivity.this, ReferpageActivity.class);
            toReferPage.putExtra("user", new Gson().toJson(userData));
            startActivity(toReferPage);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

        binding.drawer.reportOp.setOnClickListener(_view -> {
            toReport.setClass(HomepageActivity.this, ReportIssueActivity.class);
            startActivity(toReport);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

        binding.drawer.faqsOp.setOnClickListener(_view -> {
            toFAQs.setClass(HomepageActivity.this, AppFaqsActivity.class);
            startActivity(toFAQs);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        });

        binding.drawer.logoutOp.setOnClickListener(_view -> {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("all");
            cachedData.edit().remove("userData").apply();
            cachedData.edit().remove("recentlyAddedResources").apply();
            cachedData.edit().remove("recommendedResources").apply();
            cachedData.edit().remove("bestResources").apply();

            assert auth.getCurrentUser() != null;
            FirebaseMessaging.getInstance().unsubscribeFromTopic(auth.getCurrentUser().getUid());
            FirebaseAuth.getInstance().signOut();
            toLogin.setClass(HomepageActivity.this, LoginActivity.class);
            startActivity(toLogin);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            finish();
        });

        binding.headerTitle.setOnClickListener(_view -> {
//            startTutorialOnViewReady();
            Intent temp = new Intent();
            temp.setClass(HomepageActivity.this, TestActivity.class);
            startActivity(temp);
        });

        binding.streakContainer.setOnClickListener(_view -> {

        });
    }

    private void initializeLogic() {
        networkMonitor = new NetworkMonitor(this);
        designUI();
        attachAdaptersToRecyclerViews();
        createShimmerView();
        requestPermissions();
        getInAppBanners();
//        getUserData();
        loadCourses();
        loadUserDataFromSP();
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

        appUpdateManager = AppUpdateManagerFactory.create(this);
        checkForAppUpdate();
        loadBannerAd();
    }

    private void startTutorialOnViewReady() {
        View rootView = binding.getRoot();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Layout is now ready
                createTutorial();
            }
        });
    }

    private void createTutorial() {
        // --- Target 1: The Welcome Text ---
        // 1. Inflate the custom overlay layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View firstOverlay = inflater.inflate(R.layout.layout_spotlight, null);

        // 2. Find views in the overlay and customize them
        TextView title1 = firstOverlay.findViewById(R.id.spotlight_title);
        TextView desc1 = firstOverlay.findViewById(R.id.spotlight_description);
        Button nextButton1 = firstOverlay.findViewById(R.id.spotlight_next_button);

        title1.setText("Welcome!");
        desc1.setText("This is the main welcome message.");
        nextButton1.setText("Got it");
        nextButton1.setOnClickListener(v -> spotlight.next());

        // 3. Create the Target
        Target firstTarget = new Target.Builder()
                .setAnchor(binding.cashContainer) // The view to highlight
                .setShape(new Circle(200f)) // Radius of the circle
                .setOverlay(firstOverlay) // Set the custom inflated view
                .build();


        // --- Target 2: The Profile Button ---
        View secondOverlay = inflater.inflate(R.layout.layout_spotlight, null);

        TextView title2 = secondOverlay.findViewById(R.id.spotlight_title);
        TextView desc2 = secondOverlay.findViewById(R.id.spotlight_description);
        Button nextButton2 = secondOverlay.findViewById(R.id.spotlight_next_button);

        title2.setText("Your Profile");
        desc2.setText("Click here to see your profile and settings.");
        nextButton2.setText("Finish");
        nextButton2.setOnClickListener(v -> spotlight.finish());

        // 3. Create the Target
        Target secondTarget = new Target.Builder()
                .setAnchor(binding.menuIcon)
                .setShape(new Circle(150f))
                .setOverlay(secondOverlay)
                .build();

        // --- Build and Start Spotlight ---
        ArrayList<Target> targets = new ArrayList<>();
        targets.add(firstTarget);
        targets.add(secondTarget);

        spotlight = new Spotlight.Builder(this)
                .setTargets(targets)
                .setBackgroundColor(Color.parseColor("#80000000")) // e.g., #80000000
                .setDuration(1000L)
                .setAnimation(new DecelerateInterpolator(2f))
                .setOnSpotlightListener(new com.takusemba.spotlight.OnSpotlightListener() {
                    @Override
                    public void onStarted() {
                    }

                    @Override
                    public void onEnded() {
                    }
                })
                .build();

        spotlight.start();
    }

    private void loadBannerAd() {
        MobileAds.initialize(this, initializationStatus -> {
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adView.loadAd(adRequest);
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAppMaintenance();
    }

    @Override
    public void onResume() {
        super.onResume();
        networkMonitor.register();

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdate(appUpdateInfo);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPDATE_REQUEST_CODE && resultCode != RESULT_OK) {
            PrepNestUtil.showToast(getApplicationContext(), "Update failed, try again");
//            Log.d("InAppUpdate", "Update flow failed! Code: " + resultCode);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        networkMonitor.unregister();
    }


    public void designUI() {
        PrepNestUtil.roundViewWithRipple(binding.cashContainer, "#FAFAFA", 360, 0, "#FFFFFF", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(binding.streakContainer, "#FAFAFA", 360, 0, "#FFFFFF", "#E0E0E0");
        PrepNestUtil.roundViewWithRipple(binding.drawer.profileOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.myResourcesOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.wishlistOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.becomeProviderOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.referAndEarnOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(binding.drawer.reportOp, "#FFFFFF", 0, 0, "#FFFFFF", "#EEEEEE");
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
        PrepNestUtil.setLightStatusBar(this);

        binding.drawerLayout.setFitsSystemWindows(false);
        binding.drawerLayout.setStatusBarBackground(null);

        PrepNestUtil.changeNavBarColor(this, true);
    }


    public String generateDefaultName(final String _name) {
        if (_name.contains(" ")) {
            int lastIndex = _name.lastIndexOf(" ");
            return _name.substring(0, 1).concat(_name.substring(lastIndex + 1, lastIndex + 2));
        } else {
            return _name.substring(0, 1);
        }
    }

    private void checkForAppUpdate() {
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {

            // Check if update is available
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdate(appUpdateInfo);
            }
        });
    }

    private void startUpdate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void getUserData() {
        assert auth.getCurrentUser() != null;
        users.child(auth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FirebaseMessaging.getInstance().subscribeToTopic("all");
                if (!isFinishing() || !isDestroyed())
                    FirebaseMessaging.getInstance().subscribeToTopic(auth.getCurrentUser().getUid());
                if (snapshot.exists()) {
                    binding.cashAmountTxt.setVisibility(View.GONE);
                    binding.cashProgressbar.setVisibility(View.VISIBLE);
                    userData.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        userData.put(child.getKey(), child.getValue());
                    }

                    loadUserDataToUI();
                } else {
                    binding.drawer.defaultProfileTitle.setText("U");
                    binding.drawer.userNameTxt.setText("User");
                }
                binding.cashProgressbar.setVisibility(View.GONE);
                binding.cashAmountTxt.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                if (!isFinishing() && !isDestroyed()) {
                    FirebaseAuth.getInstance().signOut();
                    PrepNestUtil.showToast(HomepageActivity.this, "Please login again!");
                    finishAffinity();
                }
            }
        });
    }

    public void loadUserDataFromSP() {
        if (cachedData.contains("userData")) {
            userData = new Gson()
                    .fromJson(cachedData.getString("userData", ""),
                            new TypeToken<HashMap<String, Object>>() {
                            }
                                    .getType()
                    );

            binding.cashProgressbar.setVisibility(View.GONE);
            binding.cashAmountTxt.setVisibility(View.VISIBLE);
            loadUserDataToUI();
//            Log.d("USER DATA", "DATA LOADED FROM SP");
            getUserData();
        } else {
            getUserData();
        }
    }

    public void loadUserDataToUI() {
        if (Boolean.parseBoolean(Objects.requireNonNull(userData.getOrDefault("isBanned", false)).toString())) {
            PrepNestUtil.showToast(this, "Your account has been banned.");
            finishAffinity();
        }
        if (userData.containsKey("courseId")) {
            FirebaseMessaging.getInstance().subscribeToTopic(Objects.requireNonNull(userData.get("courseId")).toString());
        }

        if (userData.containsKey("profile")) {
            if (Objects.requireNonNull(userData.get("profile")).toString().equals("null")) {
                binding.drawer.userProfilePicture.setVisibility(View.GONE);
                binding.drawer.defaultProfileTitle.setVisibility(View.VISIBLE);
            } else {
                binding.drawer.defaultProfileTitle.setVisibility(View.GONE);
                binding.drawer.userProfilePicture.setVisibility(View.VISIBLE);
                Glide.with(getApplicationContext())
                        .load(Uri.parse(Objects.requireNonNull(userData.get("profile")).toString()))
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(binding.drawer.userProfilePicture);
            }
        } else {
            binding.drawer.userProfilePicture.setVisibility(View.GONE);
            binding.drawer.defaultProfileTitle.setVisibility(View.VISIBLE);
        }
        if (userData.containsKey("name")) {
            binding.drawer.userNameTxt.setText(Objects.requireNonNull(userData.get("name")).toString());
            binding.drawer.defaultProfileTitle.setText(generateDefaultName(Objects.requireNonNull(userData.get("name")).toString()).toUpperCase());
        } else {
            binding.drawer.defaultProfileTitle.setText("U");
            binding.drawer.userNameTxt.setText("User");
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
                money = 0L;
            }
            binding.cashAmountTxt.setText(String.valueOf(money));
        } else {
            binding.cashAmountTxt.setText("0");
        }
        if (userData.containsKey("isProvider")) {
            if (Boolean.parseBoolean(Objects.requireNonNull(userData.get("isProvider")).toString())) {
                binding.drawer.becomeProviderOp.setVisibility(View.GONE);
            } else {
                binding.drawer.becomeProviderOp.setVisibility(View.VISIBLE);
            }
        } else {
            binding.drawer.becomeProviderOp.setVisibility(View.VISIBLE);
        }
        if (userData.containsKey("providerVerificationStatus")) {
            if (Objects.requireNonNull(userData.get("providerVerificationStatus")).toString().equals("pending")) {
                binding.drawer.providerPendingIcon.setVisibility(View.VISIBLE);
            } else {
                if (Objects.requireNonNull(userData.get("providerVerificationStatus")).toString().equals("failed")) {
                    binding.drawer.providerPendingIcon.setImageResource(R.drawable.icon_error);
                } else {
                    binding.drawer.providerPendingIcon.setVisibility(View.GONE);
                }
            }
        } else {
            binding.drawer.providerPendingIcon.setVisibility(View.GONE);
        }
//        loadResources();
        cachedData.edit().putString("userData", new Gson().toJson(userData)).apply();
//        Log.d("USER DATA", "DATA SAVED TO SP");
//        Log.d("USER DATA", cachedData.getString("userData", ""));
        loadResourcesFromSP();
    }

    public void loadCourses() {
        if (jsonCourseData == null || jsonCourseData.trim().isEmpty()) {
            try {
                InputStream is = getAssets().open("courses.json");
                ByteArrayOutputStream bos = new ByteArrayOutputStream();

                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, length);
                }

                jsonCourseData = bos.toString(StandardCharsets.UTF_8);

//                Log.d("COURSE_JSON", jsonCourseData);

                bos.close();
                is.close();
            } catch (IOException ex) {
                Log.e("ERROR", ex.toString());
            }
        }
    }

    public int getCourseMaxSemesters(final String courseID) {
        try {
            JSONObject allCourses = new JSONObject(jsonCourseData);
            JSONObject course = allCourses.getJSONObject(courseID);
            return course.getInt("duration") * 2;
        } catch (Exception e) {
            Log.e("ERROR", e.toString());
//            PrepNestUtil.showToast(getApplicationContext(), e.toString());
        }
        return 0;
    }

    public void loadAllResources() {
        ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();

        final int currentSemester = (int) Float.parseFloat(Objects.requireNonNull(userData.get("semester")).toString());
        final int maxSemester = getCourseMaxSemesters(Objects.requireNonNull(userData.get("courseId")).toString());

        int difference = maxSemester - currentSemester;

        if (difference <= 0) {
            difference = 1; // load at least 1 semester
        }

        final int MAX = Math.min(difference, 3);
//        Log.d("MAX", String.valueOf(MAX));
        AtomicInteger loadedCount = new AtomicInteger(0);

        for (int semValue = currentSemester; semValue < (currentSemester + MAX); semValue++) {
//            Log.d("SEM VALUE", String.valueOf(semValue));
//            Log.d("SEM MAX", String.valueOf(currentSemester + MAX));
            resources
                    .child(Objects.requireNonNull(userData.get("courseId")).toString())
                    .orderByChild("semester")
                    .equalTo(semValue)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                if (loadedCount.incrementAndGet() == MAX) {
                                    filterResources(tempList);
                                }
                            } else {

                                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                                    HashMap<String, Object> item = (HashMap<String, Object>) childSnapshot.getValue();
                                    assert item != null;
                                    item.put("id", childSnapshot.getKey());
                                    tempList.add(item);
                                }

                                if (loadedCount.incrementAndGet() == MAX) {
                                    filterResources(tempList);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            PrepNestUtil.showToast(HomepageActivity.this, error.getMessage());
                        }
                    });
        }

    }

    public void loadResourcesFromSP() {
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<HashMap<String, Object>>>() {
        }.getType();
        ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();

        if (cachedData.contains("recentlyAddedResources")) {
            tempList = gson.fromJson(cachedData.getString("recentlyAddedResources", "[]"), type);
            recentlyAddedList.clear();
            recentlyAddedList.addAll(tempList);
            toggleRecentListEmptyState(!recentlyAddedList.isEmpty());
            recentlyAddedListAdapter.updateData(new ArrayList<>(recentlyAddedList));
        }

        tempList = new ArrayList<>();

        if (cachedData.contains("recommendedResources")) {
            tempList = gson.fromJson(cachedData.getString("recommendedResources", "[]"), type);
            recommendedList.clear();
            recommendedList.addAll(tempList);
            toggleRecommendedListEmptyState(!recommendedList.isEmpty());
            recommendedListAdapter.updateData(new ArrayList<>(recommendedList));
        }

        tempList = new ArrayList<>();

        if (cachedData.contains("bestResources")) {
            tempList = gson.fromJson(cachedData.getString("bestResources", "[]"), type);
            bestList.clear();
            bestList.addAll(tempList);
            toggleBestListEmptyState(!bestList.isEmpty());
            bestListAdapter.updateData(new ArrayList<>(bestList));
        }

        loadAllResources();
    }


    public void showProviderVerificationStatusSheet(final boolean isPending) {
        providerVerificationStatusSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(HomepageActivity.this);
        StatusViewBinding binding = StatusViewBinding.inflate(getLayoutInflater());
        providerVerificationStatusSheet.setContentView(binding.getRoot());

        providerVerificationStatusSheet.setOnShowListener(dialog -> {
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
        binding.title.setTextSize(16);
        binding.subtext.setTextSize(11);
//        binding.imageContainer.setBackground(new GradientDrawable() {
//            public GradientDrawable getIns(int a, int b) {
//                this.setCornerRadius(a);
//                this.setColor(b);
//                return this;
//            }
//        }.getIns((int) 360, 0xFFFAFAFA));
        if (isPending) {
            binding.btnOk.setVisibility(View.GONE);
            binding.image.setImageResource(R.drawable.icon_hourglass);
            PrepNestUtil.roundViewWithRipple(binding.btnCancel, "#000000", 15, 0, "#000000", "#212121");
            binding.btnCancelTxt.setTextColor(0xFFFFFFFF);
            binding.title.setText("Pending Verification!");
            binding.subtext.setText(getString(R.string.pending_provider_verification_message));
            binding.btnCancelTxt.setText("Dismiss");
        } else {
            binding.image.setImageResource(R.drawable.icon_failed_error);
            PrepNestUtil.roundViewWithRipple(binding.btnOk, "#000000", 15, 0, "#000000", "#212121");
            PrepNestUtil.roundViewWithRipple(binding.btnCancel, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
            binding.title.setText("Verification Failed!");
            binding.subtext.setText(getString(R.string.failed_provider_verification_message));
            binding.btnOkTxt.setText("Resend request");
            binding.btnCancelTxt.setText("Cancel request");
        }
        binding.btnOk.setOnClickListener(_view -> {
            if (!isPending) {
                toBecomeProvider.setClass(HomepageActivity.this, BecomeproviderActivity.class);
                startActivity(toBecomeProvider);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                providerVerificationStatusSheet.dismiss();
            }
        });
        binding.btnCancel.setOnClickListener(_view -> {
            if (isPending) {
                providerVerificationStatusSheet.dismiss();
            } else {
                cancelProviderRequest();
                providerVerificationStatusSheet.dismiss();
            }
        });
        providerVerificationStatusSheet.setCancelable(true);
        providerVerificationStatusSheet.show();
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
        assert auth.getCurrentUser() != null;
        requests.child(auth.getCurrentUser().getUid()).removeValue().addOnCompleteListener(removeTask -> {
            if (removeTask.isSuccessful()) {
                users.child(auth.getCurrentUser().getUid()).child("providerVerificationStatus").removeValue().addOnCompleteListener(updateTask -> {
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

    public void createShimmerView() {
        binding.shimmerrecyclerview1.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.shimmerrecyclerview2.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.shimmerrecyclerview3.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.shimmerrecyclerview1.setAdapter(new ShimmerAdapter(this, 3));
        binding.shimmerView1.startShimmer();
        binding.shimmerrecyclerview2.setAdapter(new ShimmerAdapter(this, 3));
        binding.shimmerView2.startShimmer();
        binding.shimmerrecyclerview3.setAdapter(new ShimmerAdapter(this, 3));
        binding.shimmerView3.startShimmer();
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
            LinearLayout.LayoutParams paramsRecentRealItemsContainer = (LinearLayout.LayoutParams) binding.recentRealItemsContainer.getLayoutParams();

            paramsRecentRealItemsContainer.width = LinearLayout.LayoutParams.MATCH_PARENT;

            paramsRecentRealItemsContainer.height = (int) convertToDp(130);

            binding.recentRealItemsContainer.setLayoutParams(paramsRecentRealItemsContainer);

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
            LinearLayout.LayoutParams paramsRecommendedRealItemsContainer = (LinearLayout.LayoutParams) binding.recommendedRealItemsContainer.getLayoutParams();

            paramsRecommendedRealItemsContainer.width = LinearLayout.LayoutParams.MATCH_PARENT;

            paramsRecommendedRealItemsContainer.height = (int) convertToDp(130);

            binding.recommendedRealItemsContainer.setLayoutParams(paramsRecommendedRealItemsContainer);

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
            LinearLayout.LayoutParams paramsBestRealItemsContainer = (LinearLayout.LayoutParams) binding.bestRealItemsContainer.getLayoutParams();

            paramsBestRealItemsContainer.width = LinearLayout.LayoutParams.MATCH_PARENT;

            paramsBestRealItemsContainer.height = (int) convertToDp(130);

            binding.bestRealItemsContainer.setLayoutParams(paramsBestRealItemsContainer);

        }
    }

    public void filterResources(final ArrayList<HashMap<String, Object>> _list) {
//        Log.d("LIST SIZE", String.valueOf(_list.size()));
        ArrayList<HashMap<String, Object>> sortedList = new ArrayList<>(_list);
        Gson gson = new Gson();

        sortedList.removeIf(item -> Boolean.TRUE.equals(item.get("isDiscontinue")));

        ListMapUtils.sortListByKey(
                sortedList,
                "dateOfVerification",
                false,
                ListMapUtils.SortType.NUMBER
        );


        cachedData.edit().putString("resources", gson.toJson(sortedList)).apply();

//        Log.d("RESOURCES", sortedList.toString());

        recentlyAddedList.clear();
        recentlyAddedList.addAll(
                sortedList
                        .stream()
                        .limit(3)
                        .collect(Collectors.toCollection(ArrayList::new))
        );

        cachedData.edit().putString("recentlyAddedResources", new Gson().toJson(recentlyAddedList)).apply();
//        Log.d("RECENT", cachedData.getString("recentlyAddedResources", ""));
        toggleRecentListEmptyState(!recentlyAddedList.isEmpty());
        recentlyAddedListAdapter.updateData(new ArrayList<>(recentlyAddedList));

        recommendedList.clear();
        recommendedList.addAll(sortedList.stream()
                .filter(item -> {
                    Object value = item.get("isRecommended");
                    return Boolean.TRUE.equals(value);
                })
                .limit(3)
                .collect(Collectors.toCollection(ArrayList::new))
        );

        cachedData.edit().putString("recommendedResources", new Gson().toJson(recommendedList)).apply();
        toggleRecommendedListEmptyState(!recommendedList.isEmpty());
        recommendedListAdapter.updateData(new ArrayList<>(recommendedList));

        bestList.clear();
        bestList.addAll(sortedList.stream()
                .filter(item -> {
                    Object value = item.get("isBestChoice");
                    return Boolean.TRUE.equals(value);
                })
                .limit(3)
                .collect(Collectors.toCollection(ArrayList::new))
        );

        cachedData.edit().putString("bestResources", new Gson().toJson(bestList)).apply();
        toggleBestListEmptyState(!bestList.isEmpty());
        bestListAdapter.updateData(new ArrayList<>(bestList));
    }

    public void attachAdaptersToRecyclerViews() {
        recentlyAddedListAdapter = new RecentlyAddedListAdapter(recentlyAddedList);
        recommendedListAdapter = new RecommendedListAdapter(recommendedList);
        bestListAdapter = new BestListAdapter(bestList);

        binding.recentlyAddedList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recommendedList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.bestList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        binding.recentlyAddedList.setAdapter(recentlyAddedListAdapter);
        binding.recommendedList.setAdapter(recommendedListAdapter);
        binding.bestList.setAdapter(bestListAdapter);
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

    public void checkAppMaintenance() {
        DatabaseReference appMaintenance = database.getReference("other/appMaintenance/isClosed");

        appMaintenance.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean isAppClosed = Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class));

                if (isAppClosed) {
                    Intent toAppMaintenance = new Intent();
                    toAppMaintenance.setClass(HomepageActivity.this, AppmaintenanceActivity.class);
                    startActivity(toAppMaintenance);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void getInAppBanners() {
        banners.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    binding.bannersContainer.setVisibility(View.INVISIBLE);
                    return;
                }

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    HashMap<String, Object> bannerMap = new HashMap<>();
                    String imageURL = childSnapshot.child("imageUrl").getValue(String.class);
                    bannerMap.put("imageUrl", imageURL);
                    bannerMap.put("id", childSnapshot.getKey());
                    bannersList.add(bannerMap);
                }

                binding.bannerViewpager.setAdapter(new InAppBannerAdapter(bannersList));
                automateBanner();
                binding.bannerViewpager.setVisibility(View.VISIBLE);
                binding.bannersProgressbar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.bannersContainer.setVisibility(View.INVISIBLE);
            }
        });

    }

    public void automateBanner() {
        TimerTask bannerTimer = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (binding.bannerViewpager.getCurrentItem() == (bannersList.size() - 1)) {
                        binding.bannerViewpager.setCurrentItem(0);
                    } else {
                        binding.bannerViewpager.setCurrentItem(binding.bannerViewpager.getCurrentItem() + 1);
                    }
                });
            }
        };
        _timer.scheduleAtFixedRate(bannerTimer, 2000, 5000);
    }

    public class ShimmerAdapter extends RecyclerView.Adapter<ShimmerAdapter.ShimmerViewHolder> {

        private final int itemCount;
        private final Context context;

        public ShimmerAdapter(Context context, int itemCount) {
            this.context = context;
            this.itemCount = itemCount;
        }

        @NonNull
        @Override
        public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            ShimmerLayoutBinding binding = ShimmerLayoutBinding.inflate(inflater, parent, false);
            return new ShimmerViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {

            // Background
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(10));
            bg.setColor(0xFFEEEEEE);
            holder.binding.layout.setBackground(bg);

            // Margins
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) holder.binding.layout.getLayoutParams();

            int marginStart = dp(10);
            int marginEnd = dp(0);

            if (position == 0) marginStart = dp(20);
            else if (position == itemCount - 1) marginEnd = dp(20);

            params.setMargins(marginStart, dp(10), marginEnd, dp(10));
            holder.binding.layout.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return itemCount;
        }

        private int dp(int value) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    value,
                    context.getResources().getDisplayMetrics()
            );
        }

        public class ShimmerViewHolder extends RecyclerView.ViewHolder {
            ShimmerLayoutBinding binding;

            public ShimmerViewHolder(@NonNull ShimmerLayoutBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }


    public class InAppBannerAdapter extends PagerAdapter {

        private final Context context;
        private final ArrayList<HashMap<String, Object>> data;

        public InAppBannerAdapter(Context context, ArrayList<HashMap<String, Object>> data) {
            this.context = context;
            this.data = data;
        }

        // If you must keep this constructor:
        public InAppBannerAdapter(ArrayList<HashMap<String, Object>> data) {
            this.context = HomepageActivity.this; // Works only inside the Activity
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "page " + position;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {

            InAppBannerLayoutBinding binding = InAppBannerLayoutBinding.inflate(
                    LayoutInflater.from(context),
                    container,
                    false
            );

            // Load image safely
            Object urlObj = data.get(position).get("imageUrl");
            String imageUrl = urlObj != null ? urlObj.toString() : "";

            Glide.with(context)
                    .load(Uri.parse(imageUrl))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .onlyRetrieveFromCache(true)
                    .into(binding.image);

            View root = binding.getRoot();
            container.addView(root);

            return root;
        }
    }

    public class RecentlyAddedListAdapter extends RecyclerView.Adapter<RecentlyAddedListAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> list;

        public RecentlyAddedListAdapter(ArrayList<HashMap<String, Object>> list) {
            this.list = list;
        }

        public void updateData(List<HashMap<String, Object>> newData) {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ResourceDiffCallback(list, newData));
            list.clear();
            list.addAll(newData);
            result.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ResourceItemShortBinding resourceItemShortBinding = ResourceItemShortBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );

            return new ViewHolder(resourceItemShortBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            PrepNestUtil.roundViewWithRipple(holder.binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            if (list.get(position).containsKey("resourceTitle")) {
                holder.binding.title.setText(getPrettyResourceTitle(Objects.requireNonNull(list.get(position).get("resourceTitle")).toString()));
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
            } else {
                holder.binding.sessionTxt.setVisibility(View.GONE);
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
//            LinearLayout.LayoutParams paramsContainer = (LinearLayout.LayoutParams) holder.binding.container.getLayoutParams();
            ViewGroup.LayoutParams params = holder.binding.container.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
                if (position == 0) {
                    marginParams.setMargins((int) convertToDp(20), (int) convertToDp(10), 0, (int) convertToDp(10));
                } else if (position == (list.size() - 1)) {
                    marginParams.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) convertToDp(20), (int) convertToDp(10));
                } else {
                    marginParams.setMargins((int) convertToDp(10), (int) convertToDp(10), 0, (int) convertToDp(10));
                }
                holder.binding.container.setLayoutParams(marginParams);
            }

            holder.binding.container.setOnClickListener(_view1 -> {
                if (list.get(position).containsKey("type")) {
                    toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                    toResources.putExtra("user", new Gson().toJson(userData));
                    try {
                        toResources.removeExtra("tagType");
                    } catch (Exception ignored) {

                    }
                    startActivity(toResources);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ResourceItemShortBinding binding;

            public ViewHolder(ResourceItemShortBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    public class RecommendedListAdapter extends RecyclerView.Adapter<RecommendedListAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> list;

        public RecommendedListAdapter(ArrayList<HashMap<String, Object>> list) {
            this.list = list;
        }

        public void updateData(List<HashMap<String, Object>> newData) {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ResourceDiffCallback(list, newData));
            list.clear();
            list.addAll(newData);
            result.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ResourceItemShortBinding resourceItemShortBinding = ResourceItemShortBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );

            return new ViewHolder(resourceItemShortBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            PrepNestUtil.roundViewWithRipple(holder.binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            if (list.get(position).containsKey("resourceTitle")) {
                holder.binding.title.setText(getPrettyResourceTitle(Objects.requireNonNull(list.get(position).get("resourceTitle")).toString()));
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
            } else {
                holder.binding.sessionTxt.setVisibility(View.GONE);
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
            ViewGroup.LayoutParams params = holder.binding.container.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
                if (position == 0) {
                    marginParams.setMargins((int) convertToDp(20), (int) convertToDp(10), 0, (int) convertToDp(10));
                } else if (position == (list.size() - 1)) {
                    marginParams.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) convertToDp(20), (int) convertToDp(10));
                } else {
                    marginParams.setMargins((int) convertToDp(10), (int) convertToDp(10), 0, (int) convertToDp(10));
                }
                holder.binding.container.setLayoutParams(marginParams);
            }
            holder.binding.container.setOnClickListener(_view1 -> {
                if (list.get(position).containsKey("type")) {
                    toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                    toResources.putExtra("user", new Gson().toJson(userData));
                    try {
                        toResources.removeExtra("tagType");
                    } catch (Exception ignored) {

                    }
                    startActivity(toResources);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ResourceItemShortBinding binding;

            public ViewHolder(ResourceItemShortBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    public class BestListAdapter extends RecyclerView.Adapter<BestListAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> list;

        public BestListAdapter(ArrayList<HashMap<String, Object>> list) {
            this.list = list;
        }

        public void updateData(List<HashMap<String, Object>> newData) {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ResourceDiffCallback(list, newData));
            list.clear();
            list.addAll(newData);
            result.dispatchUpdatesTo(this);
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ResourceItemShortBinding resourceItemShortBinding = ResourceItemShortBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );

            return new ViewHolder(resourceItemShortBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            PrepNestUtil.roundViewWithRipple(holder.binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            if (list.get(position).containsKey("resourceTitle")) {
                holder.binding.title.setText(getPrettyResourceTitle(Objects.requireNonNull(list.get(position).get("resourceTitle")).toString()));
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
            } else {
                holder.binding.sessionTxt.setVisibility(View.GONE);
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
            ViewGroup.LayoutParams params = holder.binding.container.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
                if (position == 0) {
                    marginParams.setMargins((int) convertToDp(20), (int) convertToDp(10), 0, (int) convertToDp(10));
                } else if (position == (list.size() - 1)) {
                    marginParams.setMargins((int) convertToDp(10), (int) convertToDp(10), (int) convertToDp(20), (int) convertToDp(10));
                } else {
                    marginParams.setMargins((int) convertToDp(10), (int) convertToDp(10), 0, (int) convertToDp(10));
                }
                holder.binding.container.setLayoutParams(marginParams);
            }

            holder.binding.container.setOnClickListener(_view1 -> {
                if (list.get(position).containsKey("type")) {
                    toResources.setClass(HomepageActivity.this, ResourcesActivity.class);
                    toResources.putExtra("user", new Gson().toJson(userData));
                    try {
                        toResources.removeExtra("tagType");
                    } catch (Exception ignored) {

                    }
                    startActivity(toResources);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ResourceItemShortBinding binding;

            public ViewHolder(ResourceItemShortBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    public class ResourceDiffCallback extends DiffUtil.Callback {

        private final List<HashMap<String, Object>> oldList;
        private final List<HashMap<String, Object>> newList;

        public ResourceDiffCallback(List<HashMap<String, Object>> oldList, List<HashMap<String, Object>> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldId = String.valueOf(oldList.get(oldItemPosition).get("id"));
            String newId = String.valueOf(newList.get(newItemPosition).get("id"));
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
