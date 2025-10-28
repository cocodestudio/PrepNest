package com.cocode.prepnest;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.AppmaintenanceBinding;
import com.google.firebase.FirebaseApp;

public class AppmaintenanceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        AppmaintenanceBinding binding = AppmaintenanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
    }

    private void initializeLogic() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        PrepNestUtil.changeNavBarColor(this, true);
    }

}
