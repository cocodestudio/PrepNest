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
import java.util.Objects;


public class ImageviewActivity extends AppCompatActivity {

    private final List<Uri> imageURIs = new ArrayList<>();
    private ImageviewBinding binding;
    private SharedPreferences appFirstVisitSp;

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
        appFirstVisitSp = getSharedPreferences("appFirstVisit", Activity.MODE_PRIVATE);
    }

    private void initializeLogic() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        loadImages(getIntent().getStringExtra("id"));
        initializeAndAttachAdapter();
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//        getWindow().setStatusBarColor(0xFFFFFFFF);
        if (appFirstVisitSp.contains("appFirstVisit")) {
            HashMap<String, Object> map = new Gson().fromJson(appFirstVisitSp.getString("appFirstVisit", ""), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            if (map.containsKey("firstResourceView")) {
                if (!Boolean.parseBoolean(Objects.requireNonNull(map.get("firstResourceView")).toString())) {
                    PrepNestUtil.showToast(ImageviewActivity.this, "Swipe left to see more !");
                    map.put("firstResourceView", true);
                    appFirstVisitSp.edit().putString("appFirstVisit", new Gson().toJson(map)).apply();
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
            File[] files = folder.listFiles();
            assert files != null;
            for (File file : files) {
                if (file.isFile()) {
                    Uri fileUri = Uri.fromFile(file);
                    imageURIs.add(fileUri);
                }
            }
        }
    }

}
