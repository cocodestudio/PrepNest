package com.cocode.prepnest;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.AccountbanBinding;
import com.google.firebase.FirebaseApp;

public class AccountbanActivity extends AppCompatActivity {

    private AccountbanBinding binding;


    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = AccountbanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }


    private void initialize(Bundle _savedInstanceState) {
    }

    private void initializeLogic() {

    }

}
