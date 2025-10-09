package com.cocode.prepnest;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.AccountverificationBinding;
import com.google.firebase.FirebaseApp;

public class AccountverificationActivity extends AppCompatActivity {

    private AccountverificationBinding binding;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = AccountverificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {

        binding.checkVerificationBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {

            }
        });

        binding.resendLinkBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {

            }
        });
    }

    private void initializeLogic() {
        designUI();
    }

    public void designUI() {
    }


}
