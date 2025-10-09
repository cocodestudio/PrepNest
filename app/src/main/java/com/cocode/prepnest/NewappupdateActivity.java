package com.cocode.prepnest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.NewappupdateBinding;
import com.google.firebase.FirebaseApp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;

public class NewappupdateActivity extends AppCompatActivity {

    private NewappupdateBinding binding;
    private HashMap<String, Object> data = new HashMap<>();

    private Intent downloadApp = new Intent();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = NewappupdateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {

        binding.btnDownload.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                downloadApp.setAction(Intent.ACTION_VIEW);
                downloadApp.setData(Uri.parse(data.get("download_link").toString()));
                startActivity(downloadApp);
                finish();
            }
        });

        binding.seeChangesContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                PrepNestUtil.TransitionManager(binding.container, 150);
                if (binding.newChangesTxt.getVisibility() == View.VISIBLE) {
                    binding.newChangesTxt.setVisibility(View.GONE);
                    binding.iconSeeChanges.setRotation((float) (0));
                } else {
                    binding.newChangesTxt.setVisibility(View.VISIBLE);
                    binding.iconSeeChanges.setRotation((float) (180));
                }
            }
        });
    }

    private void initializeLogic() {
        data = new Gson().fromJson(getIntent().getStringExtra("data"), new TypeToken<HashMap<String, Object>>() {
        }.getType());
        PrepNestUtil.roundViewWithRipple(binding.seeChangesContainer, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
        binding.newChangesTxt.setText(data.get("new_changes").toString());
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
    }
}
