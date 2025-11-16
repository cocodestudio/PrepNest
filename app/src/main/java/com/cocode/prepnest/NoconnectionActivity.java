package com.cocode.prepnest;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.NoconnectionBinding;
import com.google.firebase.FirebaseApp;

public class NoconnectionActivity extends AppCompatActivity {

    private NoconnectionBinding binding;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = NoconnectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        binding.retryBtn.setOnClickListener(_view -> {
            if (PrepNestUtil.isConnected(NoconnectionActivity.this)) {
                finish();
                overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
            }
        });
    }

    private void initializeLogic() {
        PrepNestUtil.setLightStatusBar(this);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        PrepNestUtil.changeNavBarColor(this, true);
    }
}
