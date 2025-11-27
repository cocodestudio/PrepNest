package com.cocode.prepnest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.TransHistoryBinding;
import com.cocode.prepnest.databinding.TransactionhistoryBinding;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class TransactionhistoryActivity extends AppCompatActivity {

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final ArrayList<HashMap<String, Object>> historyList = new ArrayList<>();
    private final DatabaseReference transactionHistory = database.getReference("transactionsHistory");
    private final Intent toNoConnection = new Intent();
    private TransactionhistoryBinding binding;
    private double amount = 0;
    private HistoryListAdapter adapter;
    private ChildEventListener transactionListener;
    private NetworkMonitor networkMonitor;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = TransactionhistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        auth = FirebaseAuth.getInstance();

        binding.backIcon.setOnClickListener(_view -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
        });
    }

    private void initializeLogic() {
        networkMonitor = new NetworkMonitor(this);
        designUI();
        adapter = new HistoryListAdapter(historyList);
        binding.historyListview.setLayoutManager(new LinearLayoutManager(this));
        binding.historyListview.setAdapter(adapter);

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
    public void onStart() {
        super.onStart();
        if (PrepNestUtil.isConnected(TransactionhistoryActivity.this)) {
            showProgressBar(true);
            checkDataExistence();
            getTransactionHistory();
        } else {
            toNoConnection.setClass(TransactionhistoryActivity.this, NoconnectionActivity.class);
            startActivity(toNoConnection);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (transactionListener != null) {
            assert auth.getCurrentUser() != null;
            transactionHistory.child(auth.getCurrentUser().getUid()).removeEventListener(transactionListener);
            transactionListener = null;
            historyList.clear();
        }
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
        binding.historyListview.setHorizontalScrollBarEnabled(false);
        binding.historyListview.setVerticalScrollBarEnabled(false);
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public String formatTimeDifference(final String _time) {
        long timeStampMS = Long.parseLong(_time);
        LocalDateTime timestamp = Instant.ofEpochMilli(timeStampMS).atZone(ZoneId.systemDefault()).toLocalDateTime();


        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(timestamp, now);

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (days == 1) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            return "Yesterday at " + timestamp.format(formatter);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'at' HH:mm");
            return timestamp.format(formatter);
        }
    }


    public void getTransactionHistory() {
        if (transactionListener == null) {
            transactionListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                    GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<>() {
                    };
                    HashMap<String, Object> map = snapshot.getValue(ind);
                    historyList.add(map);
                    adapter.notifyDataSetChanged();
                    showProgressBar(false);
                    toggleEmptyState();
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                    if (!isFinishing() && !isDestroyed()) {
                        showProgressBar(true);
                    }
                    reloadAllData();
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    if (!isFinishing() && !isDestroyed()) {
                        showProgressBar(true);
                    }
                    reloadAllData();
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showProgressBar(false);
                    PrepNestUtil.showToast(getApplicationContext(), "Failed to load data, try again later.");
                    Log.d("HISTORY ERROR", error.getMessage());
                }
            };

        }
        assert auth.getCurrentUser() != null;
        transactionHistory.child(auth.getCurrentUser().getUid()).addChildEventListener(transactionListener);
    }

    public void showProgressBar(boolean show) {
        if (show) {
            binding.progressBarLayout.setVisibility(View.VISIBLE);
        } else {
            binding.progressBarLayout.setVisibility(View.GONE);
        }
    }


    public void toggleEmptyState() {
        if (historyList != null && (!historyList.isEmpty())) {
            binding.historyListview.setVisibility(View.VISIBLE);
            binding.emptyContainer.setVisibility(View.GONE);
        } else {
            binding.historyListview.setVisibility(View.GONE);
            binding.emptyContainer.setVisibility(View.VISIBLE);
        }
        binding.progressBarLayout.setVisibility(View.GONE);
    }


    public void reloadAllData() {
        assert auth.getCurrentUser() != null;
        transactionHistory.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot _dataSnapshot) {
                historyList.clear();
                try {
                    GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<>() {
                    };
                    for (DataSnapshot _data : _dataSnapshot.getChildren()) {
                        HashMap<String, Object> _map = _data.getValue(_ind);
                        historyList.add(_map);
                    }
                } catch (Exception _e) {
                    _e.printStackTrace();
                }
                adapter.notifyDataSetChanged();
                showProgressBar(false);
                toggleEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError _databaseError) {
                showProgressBar(false);
                PrepNestUtil.showToast(getApplicationContext(), "Failed to load data, try again later.");
            }
        });
    }


    public void checkDataExistence() {
        assert auth.getCurrentUser() != null;
        transactionHistory.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot _dataSnapshot) {
                if (!_dataSnapshot.exists() || !_dataSnapshot.hasChildren()) {
                    showProgressBar(false);
                    toggleEmptyState();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError _databaseError) {
                showProgressBar(false);
                PrepNestUtil.showToast(getApplicationContext(), "Failed to load data, try again later");
            }
        });
    }

    public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> list;

        public HistoryListAdapter(ArrayList<HashMap<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TransHistoryBinding transactionHistoryBinding = TransHistoryBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );

            return new ViewHolder(transactionHistoryBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            PrepNestUtil.roundViewWithRipple(holder.binding.container, "#FFFFFF", 0, 0, "#FFFFFF", "#E0E0E0");
            String amountType = "";
            String typeValue = "";
            boolean hasAmount = list.get(list.size() - 1 - position).containsKey("amount");
            boolean hasType = list.get(list.size() - 1 - position).containsKey("type");
            boolean hasTime = list.get(list.size() - 1 - position).containsKey("timestamp");
            if (hasAmount && hasType) {
                typeValue = Objects.requireNonNull(list.get(list.size() - 1 - position).get("type")).toString();
                if ((typeValue.equals("purchase") || typeValue.equals("resource_sold")) && list.get(list.size() - 1 - position).containsKey("amountType")) {
                    amountType = Objects.requireNonNull(list.get(list.size() - 1 - position).get("amountType")).toString();
                } else {
                    if (typeValue.equals("cash_deduct") || typeValue.equals("cash_add")) {
                        amountType = "cash";
                    } else {
                        if (typeValue.equals("purchase_reward") || typeValue.equals("resource_reward") || typeValue.equals("refer_success") || typeValue.equals("refer_reward") || typeValue.equals("welcome_bonus") || typeValue.equals("rewarded_ads")) {
                            amountType = "coins";
                        }
                    }
                }
                if (!amountType.isEmpty()) {
                    holder.binding.container.setVisibility(View.VISIBLE);
                    holder.binding.line.setVisibility(View.VISIBLE);
                    amount = Double.parseDouble(Objects.requireNonNull(list.get(list.size() - 1 - position).get("amount")).toString());
                } else {
                    holder.binding.container.setVisibility(View.GONE);
                    holder.binding.line.setVisibility(View.GONE);
                }
            } else {
                holder.binding.container.setVisibility(View.GONE);
                holder.binding.line.setVisibility(View.GONE);
            }
            if (hasType) {
                holder.binding.container.setVisibility(View.VISIBLE);
                holder.binding.line.setVisibility(View.VISIBLE);
                switch (typeValue) {
                    case "purchase":
                        holder.binding.amountTxt.setTextColor(0xFFF44336);
                        holder.binding.type.setText("Resource purchase");
                        if (amountType.equals("cash")) {
                            holder.binding.amountTxt.setText("- ₹".concat(String.valueOf((long) (amount))));
                        } else {
                            if (amountType.equals("coins")) {
                                holder.binding.amountTxt.setText("- ".concat(String.valueOf((long) (amount)).concat(" coins")));
                            } else {
                                holder.binding.container.setVisibility(View.GONE);
                                holder.binding.container.setVisibility(View.GONE);
                            }
                        }
                        break;
                    case "resource_sold":
                        holder.binding.amountTxt.setTextColor(0xFF4CAF50);
                        holder.binding.type.setText("Resource sold");
                        if (amountType.equals("cash")) {
                            holder.binding.amountTxt.setText("+ ₹".concat(String.valueOf((long) (amount))));
                        } else {
                            if (amountType.equals("coins")) {
                                holder.binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                            } else {
                                holder.binding.container.setVisibility(View.GONE);
                                holder.binding.line.setVisibility(View.GONE);
                            }
                        }
                        break;
                    case "cash_deduct":
                        holder.binding.amountTxt.setTextColor(0xFFF44336);
                        holder.binding.type.setText("Payout");
                        holder.binding.amountTxt.setText("- ₹".concat(String.valueOf((long) (amount))));
                        break;
                    case "refer_success":
                        holder.binding.amountTxt.setTextColor(0xFF4CAF50);
                        holder.binding.type.setText("Successful refer");
                        holder.binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "refer_reward":
                        holder.binding.amountTxt.setTextColor(0xFF4CAF50);
                        holder.binding.type.setText("Referral reward");
                        holder.binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "cash_add":
                        holder.binding.amountTxt.setTextColor(0xFF4CAF50);
                        holder.binding.type.setText("Add cash");
                        holder.binding.amountTxt.setText("+ ₹ ".concat(String.valueOf((long) (amount))));
                        break;
                    case "welcome_bonus":
                        holder.binding.amountTxt.setTextColor(0xFF4CAF50);
                        holder.binding.type.setText("Welcome bonus");
                        holder.binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "rewarded_ads":
                        holder.binding.amountTxt.setTextColor(0xFF4CAF50);
                        holder.binding.type.setText("Rewarded ad");
                        holder.binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "purchase_reward":
                        holder.binding.amountTxt.setTextColor(0xFF4CAF50);
                        holder.binding.type.setText("Purchase reward");
                        holder.binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "resource_reward":
                        holder.binding.amountTxt.setTextColor(0xFF4CAF50);
                        holder.binding.type.setText("New resource reward");
                        holder.binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    default:
                        holder.binding.container.setVisibility(View.GONE);
                        holder.binding.line.setVisibility(View.GONE);
                        break;
                }
            } else {
                holder.binding.container.setVisibility(View.GONE);
                holder.binding.line.setVisibility(View.GONE);
            }
            if (hasTime) {
                holder.binding.container.setVisibility(View.VISIBLE);
                holder.binding.line.setVisibility(View.VISIBLE);
                holder.binding.timestamp.setText(formatTimeDifference(Objects.requireNonNull(list.get(list.size() - 1 - position).get("timestamp")).toString()));
                switch (Objects.requireNonNull(list.get(list.size() - 1 - position).getOrDefault("status", "")).toString()) {
                    case "pending":
                        holder.binding.status.setVisibility(View.VISIBLE);
                        holder.binding.status.setTextColor(0xFFFF9800);
                        holder.binding.amountTxt.setTextColor(0xFFFF9800);
                        holder.binding.status.setText("• pending");
                        break;
                    case "failed":
                        holder.binding.status.setVisibility(View.VISIBLE);
                        holder.binding.status.setTextColor(0xFFF44336);
                        holder.binding.amountTxt.setTextColor(0xFFF44336);
                        holder.binding.status.setText("• failed");
                        break;
                    default:
                        holder.binding.status.setVisibility(View.GONE);
                        break;
                }
            } else {
                holder.binding.container.setVisibility(View.GONE);
                holder.binding.line.setVisibility(View.GONE);
            }
            if (position == (historyList.size() - 1)) {
                holder.binding.line.setVisibility(View.GONE);
            } else {
                holder.binding.line.setVisibility(View.VISIBLE);
            }
            holder.binding.container.setOnClickListener(_view1 -> {

            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TransHistoryBinding binding;

            public ViewHolder(TransHistoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
