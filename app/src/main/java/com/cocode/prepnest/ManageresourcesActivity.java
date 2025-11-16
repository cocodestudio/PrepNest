package com.cocode.prepnest;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.cocode.prepnest.databinding.ManageresourcesBinding;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.FirebaseApp;


public class ManageresourcesActivity extends AppCompatActivity {

    private ManageresourcesBinding binding;
    private NetworkMonitor networkMonitor;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = ManageresourcesBinding.inflate(getLayoutInflater());
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
    }

    private void initializeLogic() {
        LogUtils logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        logFile.addActivity();
        designUI();
        if (getIntent().hasExtra("navigation type")) {
            if (getIntent().getStringExtra("navigation type").equals("owner")) {
                binding.layoutsPager.setCurrentItem(1);
            }
        }
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
        binding.layoutsPager.setAdapter(new LayoutsFragmentAdapter(this));

        // Attach TabLayout to ViewPager2 manually
        new TabLayoutMediator(binding.tablayout, binding.layoutsPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("Purchased");
            } else if (position == 1) {
                tab.setText("Uploaded");
            }
        }).attach();
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public class LayoutsFragmentAdapter extends FragmentStateAdapter {
        public LayoutsFragmentAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new UserpurchasedresourcesFragmentActivity();
            } else if (position == 1) {
                return new UseruploadedresourcesFragmentActivity();
            }
            return new Fragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

}
