package com.cocode.prepnest;

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

import com.cocode.prepnest.databinding.InformationViewLayoutBinding;
import com.cocode.prepnest.databinding.InfoviewBinding;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.HashMap;


public class InfoviewActivity extends AppCompatActivity {

    private InfoviewBinding binding;
    private HashMap<String, Object> map = new HashMap<>();

    private ArrayList<HashMap<String, Object>> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = InfoviewBinding.inflate(getLayoutInflater());
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
        binding.recyclerview.setHorizontalScrollBarEnabled(false);
        binding.recyclerview.setVerticalScrollBarEnabled(false);
        data();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
    }


    public void data() {
        if (getIntent().hasExtra("type")) {
            if (getIntent().getStringExtra("type").equals("terms")) {
                binding.headerTitle.setText("Terms & Conditions");
                map = new HashMap<>();
                map.put("title", "Who Can Use PrepNest");
                map.put("info", "You must be at least 17 years old and create an account using a valid email and phone number.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "What You Can Do");
                map.put("info", "PrepNest lets you buy and sell study resources like notes, previous papers. You can upload your own resources, but our team reviews everything before it’s published.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Payments and E-Coins");
                map.put("info", "You can add real cash to your account by scanning a UPI QR code and sending a screenshot. After we verify it, your balance is updated.\n\nYou can buy resources using real cash or E-coins.\n\nIf you sell a resource, you'll receive 30% of the sale amount:\n\nIn E-coins if the buyer pays using E-coins\n\nIn real cash if the buyer pays with real cash\n\n\nE-coins can be used to buy other resources, but cannot be transferred to others or cashed out.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Content Ownership");
                map.put("info", "You own everything you upload. However, PrepNest may remove anything that’s illegal, copied without permission, or violates our rules.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Privacy");
                map.put("info", "We collect your name, email, and phone number — your profile photo is optional. We don’t share this information. Ads in the app may use limited anonymous info to function properly.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Using PrepNest");
                map.put("info", "Use the platform responsibly. PrepNest isn’t liable for issues between users.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Changes");
                map.put("info", "We may update these terms occasionally. If we do, we’ll let you know in the app.");
                list.add(map);
            } else {
                binding.headerTitle.setText("Privacy Policy");
                map = new HashMap<>();
                map.put("title", "What We Collect");
                map.put("info", "Name\n\nEmail\n\nPhone number\n\nOptional profile photo");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Why We Collect It");
                map.put("info", "To help you create and manage your account\n\nTo verify your identity\n\nTo show you resources and ads inside the app");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Who We Share It With");
                map.put("info", "Nobody. We don’t sell or share your personal info.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Ads");
                map.put("info", "We may show ads inside the app. These might use anonymous data (not your name or phone) to show relevant content.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Your Rights");
                map.put("info", "You can update or delete your information from your profile at any time.");
                list.add(map);
                map = new HashMap<>();
                map.put("title", "Updates");
                map.put("info", "We’ll let you know if this policy changes.");
                list.add(map);
            }
        }
        binding.recyclerview.setAdapter(new RecyclerviewAdapter(list));
        binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));
    }


    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }

    public class RecyclerviewAdapter extends RecyclerView.Adapter<RecyclerviewAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public RecyclerviewAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.information_view_layout, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            InformationViewLayoutBinding binding = InformationViewLayoutBinding.bind(_view);

            binding.title.setText(String.valueOf((long) (_position + 1)).concat(". ".concat(_data.get((int) _position).get("title").toString())));
            binding.subtext.setText(_data.get((int) _position).get("info").toString());
            LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) binding.container.getLayoutParams();
            if (_position == 0) {
                paramscontainer.setMargins((int) convertToDp(10), (int) convertToDp(8), (int) convertToDp(10), (int) 0);
            } else {
                if (_position == (_data.size() - 1)) {
                    paramscontainer.setMargins((int) convertToDp(10), (int) 0, (int) convertToDp(10), (int) convertToDp(8));
                } else {
                    paramscontainer.setMargins((int) convertToDp(10), (int) convertToDp(8), (int) convertToDp(10), (int) convertToDp(8));
                }
            }
            binding.container.setLayoutParams(paramscontainer);
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
