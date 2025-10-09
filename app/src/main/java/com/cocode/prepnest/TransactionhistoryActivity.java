package com.cocode.prepnest;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.TransHistoryBinding;
import com.cocode.prepnest.databinding.TransactionhistoryBinding;
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

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

    private TransactionhistoryBinding binding;
    private String amountType = "";
    private double amount = 0;
    private boolean hasAmount = false;
    private boolean hasType = false;
    private boolean hasTime = false;
    private String typeValue = "";
    private History_listviewAdapter adapter;
    private ChildEventListener transactionListener;
    private LogUtils logFile;
    private NetworkMonitor networkMonitor;

    private ArrayList<HashMap<String, Object>> historyList = new ArrayList<>();

    private DatabaseReference transaction_history = _firebase.getReference("transactions_history");
    private FirebaseAuth auth;
    private Intent toNoConnection = new Intent();

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
        adapter = new History_listviewAdapter(historyList);
        binding.historyListview.setLayoutManager(new LinearLayoutManager(this));
        binding.historyListview.setAdapter(adapter);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (PrepNestUtil.isConnected(TransactionhistoryActivity.this)) {
            PrepNestUtil.showLoadingDialog(this, true);
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
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
        logFile.addLog("TRANSACTIONS HISTORY", "LOADING HISTORY DATA");
        if (transactionListener == null) {
            transactionListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {
                    };
                    HashMap<String, Object> map = snapshot.getValue(ind);
                    historyList.add(map);
                    adapter.notifyDataSetChanged();
                    toggleEmptyState();
                    PrepNestUtil.showLoadingDialog(TransactionhistoryActivity.this, false);
                    logFile.addLog("TRANSACTIONS HISTORY", "HISTORY LOADED SUCCESSFULLY");
                }

                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                    if (!isFinishing() && !isDestroyed()) {
                        PrepNestUtil.showLoadingDialog(TransactionhistoryActivity.this, false);
                    }
                    reloadAllData();
                }

                @Override
                public void onChildRemoved(DataSnapshot snapshot) {
                    if (!isFinishing() && !isDestroyed()) {
                        PrepNestUtil.showLoadingDialog(TransactionhistoryActivity.this, false);
                    }
                    reloadAllData();
                }

                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    PrepNestUtil.showLoadingDialog(TransactionhistoryActivity.this, false);
                }
            };

        }
        transaction_history.child(auth.getCurrentUser().getUid()).addChildEventListener(transactionListener);
    }


    public void toggleEmptyState() {
        if (historyList != null && (historyList.size() > 0)) {
            binding.historyListview.setVisibility(View.VISIBLE);
            binding.emptyContainer.setVisibility(View.GONE);
        } else {
            binding.historyListview.setVisibility(View.GONE);
            binding.emptyContainer.setVisibility(View.VISIBLE);
        }
    }


    public void reloadAllData() {
        transaction_history.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot _dataSnapshot) {
                historyList.clear();
                try {
                    GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {
                    };
                    for (DataSnapshot _data : _dataSnapshot.getChildren()) {
                        HashMap<String, Object> _map = _data.getValue(_ind);
                        historyList.add(_map);
                    }
                } catch (Exception _e) {
                    _e.printStackTrace();
                }
                adapter.notifyDataSetChanged();
                toggleEmptyState();
                PrepNestUtil.showLoadingDialog(TransactionhistoryActivity.this, false);
            }

            @Override
            public void onCancelled(DatabaseError _databaseError) {
                PrepNestUtil.showLoadingDialog(TransactionhistoryActivity.this, false);
            }
        });
    }


    public void checkDataExistence() {
        logFile.addLog("TRANSACTIONS HISTORY", "CHECKING DATA EXISTENCE");
        transaction_history.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot _dataSnapshot) {
                if (!_dataSnapshot.exists() || !_dataSnapshot.hasChildren()) {
                    logFile.addLog("TRANSACTIONS HISTORY", "CHECKED SUCCESSFULLY");
                    toggleEmptyState();
                    PrepNestUtil.showLoadingDialog(TransactionhistoryActivity.this, false);
                }
            }

            @Override
            public void onCancelled(DatabaseError _databaseError) {
                PrepNestUtil.showLoadingDialog(TransactionhistoryActivity.this, false);
            }
        });
    }

    public class History_listviewAdapter extends RecyclerView.Adapter<History_listviewAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public History_listviewAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
            amountType = "";
            typeValue = "";
            hasAmount = _data.get((int) (_data.size() - 1) - _position).containsKey("amount");
            hasType = _data.get((int) (_data.size() - 1) - _position).containsKey("type");
            hasTime = _data.get((int) (_data.size() - 1) - _position).containsKey("timestamp");
            if (hasAmount && hasType) {
                typeValue = _data.get((int) (_data.size() - 1) - _position).get("type").toString();
                logFile.addLog("HISTORY", String.valueOf((long) ((_data.size() - 1) - _position)).concat("-TYPE VALUE: ").concat(typeValue));
                if ((typeValue.equals("purchase") || typeValue.equals("resource_sold")) && _data.get((int) (_data.size() - 1) - _position).containsKey("amount type")) {
                    amountType = _data.get((int) (_data.size() - 1) - _position).get("amount type").toString();
                    logFile.addLog("HISTORY", String.valueOf((long) ((_data.size() - 1) - _position)).concat("-AMOUNT TYPE HERE: ").concat(amountType));
                } else {
                    if (typeValue.equals("cash_deduct") || typeValue.equals("cash_add")) {
                        amountType = "cash";
                    } else {
                        if (typeValue.equals("purchase_reward") || (typeValue.equals("resource_reward") || (typeValue.equals("refer_success") || (typeValue.equals("refer_reward") || typeValue.equals("welcome_bonus"))))) {
                            amountType = "coins";
                        }
                    }
                }
                logFile.addLog("HISTORY", String.valueOf((long) ((_data.size() - 1) - _position)).concat("-AMOUNT TYPE: ").concat(amountType));
                if (!amountType.isEmpty()) {
                    binding.container.setVisibility(View.VISIBLE);
                    binding.line.setVisibility(View.VISIBLE);
                    amount = Double.parseDouble(_data.get((int) (_data.size() - 1) - _position).get("amount").toString());
                    logFile.addLog("HISTORY", String.valueOf((long) ((_data.size() - 1) - _position)).concat("-AMOUNT: ").concat(amountType));
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
                binding.timestamp.setText(formatTimeDifference(_data.get((int) (_data.size() - 1) - _position).get("timestamp").toString()));
                switch (_data.get((int) (_data.size() - 1) - _position).getOrDefault("status", "").toString()) {
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
            binding.container.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View _view) {

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
