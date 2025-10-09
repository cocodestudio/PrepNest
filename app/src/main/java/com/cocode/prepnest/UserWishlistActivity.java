package com.cocode.prepnest;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.ResourceItemCardFullBinding;
import com.cocode.prepnest.databinding.UserWishlistBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class UserWishlistActivity extends AppCompatActivity {

    private UserWishlistBinding binding;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private LogUtils logFile;
    private Items_listAdapter listAdapter;
    private FirebaseDatabase firebase_database = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase_database.getReference("users");
    private DatabaseReference resources = firebase_database.getReference("resources");
    private NetworkMonitor networkMonitor;

    private ArrayList<String> resourceIDs = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> wishlistedResources = new ArrayList<>();

    private Intent toResources = new Intent();

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
        listAdapter = new Items_listAdapter(wishlistedResources);
        binding.itemsList.setAdapter(listAdapter);
        binding.itemsList.setLayoutManager(new LinearLayoutManager(this));
        getWishlistedResources();
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
        binding.stateImg.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 360, 0xFFFAFAFA));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
    }


    public void getWishlistedResources() {
        logFile.addLog("WISHLIST", "GETTING USER WISHLISTED RESOURCES IDs");
        DatabaseReference dataRef = users.child(auth.getCurrentUser().getUid()).child("wishlist");
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
            public void onCancelled(DatabaseError databaseError) {
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

    public class Items_listAdapter extends RecyclerView.Adapter<Items_listAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public Items_listAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
            if (_data.get((int) _position).containsKey("resource title")) {
                binding.title.setText(_data.get((int) _position).get("resource title").toString());
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
            if (_data.get((int) _position).containsKey("best choice")) {
                if (_data.get((int) _position).get("best choice").toString().equals("true")) {
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
            if (_data.get((int) _position).containsKey("recommended")) {
                if (_data.get((int) _position).get("recommended").toString().equals("true")) {
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
            if (_data.get((int) _position).containsKey("type")) {
                if (_data.get((int) _position).get("type").toString().equals("paper")) {
                    binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    if (_data.get((int) _position).get("type").toString().equals("notes")) {
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
            LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) binding.container.getLayoutParams();
            if (_position == (_data.size() - 1)) {
                paramscontainer.setMargins((int) convertToDp(20), (int) convertToDp(10), (int) convertToDp(20), (int) convertToDp(10));
            } else {
                paramscontainer.setMargins((int) convertToDp(20), (int) convertToDp(10), (int) convertToDp(20), (int) 0);
            }
            paramscontainer.width = LinearLayout.LayoutParams.MATCH_PARENT;
            paramscontainer.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            binding.container.setLayoutParams(paramscontainer);
            binding.container.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View _view) {
                    if (_data.get((int) _position).containsKey("type")) {
                        toResources.setClass(UserWishlistActivity.this, ResourcesActivity.class);
                        toResources.putExtra("user", getIntent().getStringExtra("user"));
                        if (_data.get((int) _position).get("type").toString().equals("paper")) {
                            toResources.putExtra("type", "paper");
                        } else {
                            if (_data.get((int) _position).get("type").toString().equals("notes")) {
                                toResources.putExtra("type", "notes");
                            }
                        }
                        startActivity(toResources);
                        overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                        finish();
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
