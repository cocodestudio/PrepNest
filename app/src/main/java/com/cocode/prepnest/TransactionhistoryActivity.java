package com.cocode.prepnest;

import android.content.Intent;
import android.os.Bundle;
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


public class TransactionhistoryActivity extends AppCompatActivity {

    private final FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private final ArrayList<HashMap<String, Object>> historyList = new ArrayList<>();
    private final DatabaseReference transaction_history = _firebase.getReference("transactions_history");
    private final Intent toNoConnection = new Intent();
    private TransactionhistoryBinding binding;
    private double amount = 0;
    private History_listviewAdapter adapter;
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
        adapter = new History_listviewAdapter(historyList);
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
            transaction_history.child(auth.getCurrentUser().getUid()).removeEventListener(transactionListener);
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
                }
            };

        }
        transaction_history.child(auth.getCurrentUser().getUid()).addChildEventListener(transactionListener);
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
        transaction_history.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
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
        transaction_history.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
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

    public class History_listviewAdapter extends RecyclerView.Adapter<History_listviewAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public History_listviewAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.trans_history, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            TransHistoryBinding binding = TransHistoryBinding.bind(_view);

            PrepNestUtil.roundViewWithRipple(binding.container, "#FFFFFF", 0, 0, "#FFFFFF", "#E0E0E0");
            String amountType = "";
            String typeValue = "";
            boolean hasAmount = _data.get(_data.size() - 1 - _position).containsKey("amount");
            boolean hasType = _data.get(_data.size() - 1 - _position).containsKey("type");
            boolean hasTime = _data.get(_data.size() - 1 - _position).containsKey("timestamp");
            if (hasAmount && hasType) {
                typeValue = _data.get(_data.size() - 1 - _position).get("type").toString();
                if ((typeValue.equals("purchase") || typeValue.equals("resource_sold")) && _data.get(_data.size() - 1 - _position).containsKey("amount type")) {
                    amountType = _data.get(_data.size() - 1 - _position).get("amount type").toString();
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
                    binding.container.setVisibility(View.VISIBLE);
                    binding.line.setVisibility(View.VISIBLE);
                    amount = Double.parseDouble(_data.get(_data.size() - 1 - _position).get("amount").toString());
                } else {
                    binding.container.setVisibility(View.GONE);
                    binding.line.setVisibility(View.GONE);
                }
            } else {
                binding.container.setVisibility(View.GONE);
                binding.line.setVisibility(View.GONE);
            }
            if (hasType) {
                binding.container.setVisibility(View.VISIBLE);
                binding.line.setVisibility(View.VISIBLE);
                switch (typeValue) {
                    case "purchase":
                        binding.amountTxt.setTextColor(0xFFF44336);
                        binding.type.setText("Resource purchase");
                        if (amountType.equals("cash")) {
                            binding.amountTxt.setText("- ₹".concat(String.valueOf((long) (amount))));
                        } else {
                            if (amountType.equals("coins")) {
                                binding.amountTxt.setText("- ".concat(String.valueOf((long) (amount)).concat(" coins")));
                            } else {
                                binding.container.setVisibility(View.GONE);
                                binding.container.setVisibility(View.GONE);
                            }
                        }
                        break;
                    case "resource_sold":
                        binding.amountTxt.setTextColor(0xFF4CAF50);
                        binding.type.setText("Resource sold");
                        if (amountType.equals("cash")) {
                            binding.amountTxt.setText("+ ₹".concat(String.valueOf((long) (amount))));
                        } else {
                            if (amountType.equals("coins")) {
                                binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                            } else {
                                binding.container.setVisibility(View.GONE);
                                binding.line.setVisibility(View.GONE);
                            }
                        }
                        break;
                    case "cash_deduct":
                        binding.amountTxt.setTextColor(0xFFF44336);
                        binding.type.setText("Payout");
                        binding.amountTxt.setText("- ₹".concat(String.valueOf((long) (amount))));
                        break;
                    case "refer_success":
                        binding.amountTxt.setTextColor(0xFF4CAF50);
                        binding.type.setText("Successful refer");
                        binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "refer_reward":
                        binding.amountTxt.setTextColor(0xFF4CAF50);
                        binding.type.setText("Referral reward");
                        binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "cash_add":
                        binding.amountTxt.setTextColor(0xFF4CAF50);
                        binding.type.setText("Add cash");
                        binding.amountTxt.setText("+ ₹ ".concat(String.valueOf((long) (amount))));
                        break;
                    case "welcome_bonus":
                        binding.amountTxt.setTextColor(0xFF4CAF50);
                        binding.type.setText("Welcome bonus");
                        binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "rewarded_ads":
                        binding.amountTxt.setTextColor(0xFF4CAF50);
                        binding.type.setText("Rewarded ad");
                        binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "purchase_reward":
                        binding.amountTxt.setTextColor(0xFF4CAF50);
                        binding.type.setText("Purchase reward");
                        binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    case "resource_reward":
                        binding.amountTxt.setTextColor(0xFF4CAF50);
                        binding.type.setText("New resource reward");
                        binding.amountTxt.setText("+ ".concat(String.valueOf((long) (amount)).concat(" coins")));
                        break;
                    default:
                        binding.container.setVisibility(View.GONE);
                        binding.line.setVisibility(View.GONE);
                        break;
                }
            } else {
                binding.container.setVisibility(View.GONE);
                binding.line.setVisibility(View.GONE);
            }
            if (hasTime) {
                binding.container.setVisibility(View.VISIBLE);
                binding.line.setVisibility(View.VISIBLE);
                binding.timestamp.setText(formatTimeDifference(_data.get(_data.size() - 1 - _position).get("timestamp").toString()));
                switch (_data.get(_data.size() - 1 - _position).getOrDefault("status", "").toString()) {
                    case "pending":
                        binding.status.setVisibility(View.VISIBLE);
                        binding.status.setTextColor(0xFFFF9800);
                        binding.amountTxt.setTextColor(0xFFFF9800);
                        binding.status.setText("• pending");
                        break;
                    case "failed":
                        binding.status.setVisibility(View.VISIBLE);
                        binding.status.setTextColor(0xFFF44336);
                        binding.amountTxt.setTextColor(0xFFF44336);
                        binding.status.setText("• failed");
                        break;
                    default:
                        binding.status.setVisibility(View.GONE);
                        break;
                }
            } else {
                binding.container.setVisibility(View.GONE);
                binding.line.setVisibility(View.GONE);
            }
            if (_position == (historyList.size() - 1)) {
                binding.line.setVisibility(View.GONE);
            } else {
                binding.line.setVisibility(View.VISIBLE);
            }
            binding.container.setOnClickListener(_view1 -> {

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
