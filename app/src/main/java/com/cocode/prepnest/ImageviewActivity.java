package com.cocode.prepnest;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.cocode.prepnest.databinding.ImageviewBinding;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.FirebaseApp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ImageviewActivity extends AppCompatActivity {

    private final List<Uri> imageURIs = new ArrayList<>();
    private ImageviewBinding binding;
    private LogUtils logFile;
    private SharedPreferences features_visit;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = ImageviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        features_visit = getSharedPreferences("features visit", Activity.MODE_PRIVATE);
    }

    private void initializeLogic() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        logFile = new LogUtils(this);
        logFile.addActivity();
        loadImages(getIntent().getStringExtra("id"));
        initializeAndAttachAdapter();
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//        getWindow().setStatusBarColor(0xFFFFFFFF);
        if (features_visit.contains("features visit")) {
            HashMap<String, Object> map = new Gson().fromJson(features_visit.getString("features visit", ""), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            if (map.containsKey("first resource view")) {
                if (map.get("first resource view").toString().equals("false")) {
                    PrepNestUtil.showToast(ImageviewActivity.this, "Swipe left to see more !");
                    map.put("first resource view", "true");
                    features_visit.edit().putString("features visit", new Gson().toJson(map)).apply();
                }
            }
        }

        PrepNestUtil.changeNavBarColor(this, true);
    }

    public void initializeAndAttachAdapter() {
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, imageURIs);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Reset scale on new image
                PhotoView current = adapter.getPhotoViewAt(position);
                if (current != null) {
                    current.setScale(1.0f, true);
                }
            }
        });
    }


    public void loadImages(final String _folder) {
        if (getIntent().hasExtra("uri")) {
            imageURIs.add(Uri.parse(getIntent().getStringExtra("uri")));
            return;
        }
        File folder = new File(getFilesDir(), _folder);
        if (folder.exists() && folder.isDirectory()) {
            logFile.addLog("RESOURCE", "FOLDER EXISTS");
            File[] files = folder.listFiles();
            logFile.addLog("RESOURCE", "HAS FILES : ".concat(String.valueOf((long) (files.length))));
            for (File file : files) {
                if (file.isFile()) {
                    Uri fileUri = Uri.fromFile(file);
                    imageURIs.add(fileUri);
                }
            }
        }
    }

}
