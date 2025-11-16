package com.cocode.prepnest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.cocode.prepnest.databinding.ImagecropBinding;
import com.google.firebase.FirebaseApp;
import com.isseiaoki.simplecropview.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class ImagecropActivity extends AppCompatActivity {

    private ImagecropBinding binding;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = ImagecropBinding.inflate(getLayoutInflater());
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

        binding.cropBtn.setOnClickListener(_view -> {
            Uri savedUri = saveCroppedImage(binding.cropImageView.getCroppedBitmap());
            Intent resultIntent = new Intent();
            resultIntent.putExtra("croppedImageUri", savedUri.toString());
            setResult(RESULT_OK, resultIntent);
            overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
            finish();
        });

        binding.rotateIcon.setOnClickListener(_view -> binding.cropImageView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D));
    }

    private void initializeLogic() {
        initializeImageCropper();
        designUI();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
    }


    public void designUI() {
        PrepNestUtil.roundViewWithRipple(binding.cropBtn, "#000000", 360, 0, "#000000", "#212121");
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public void initializeImageCropper() {
        Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            binding.cropImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            PrepNestUtil.showToast(this, "Failed to load image");
        }


        // Set square aspect ratio
        binding.cropImageView.setCropMode(CropImageView.CropMode.SQUARE);
        binding.cropImageView.setOutputMaxSize(400, 400);
        binding.cropImageView.setMinFrameSizeInDp(150);
        binding.cropImageView.setFrameStrokeWeightInDp(1);
        binding.cropImageView.setGuideStrokeWeightInDp(1);
        binding.cropImageView.setHandleSizeInDp(10);
        binding.cropImageView.setHandleShowMode(CropImageView.ShowMode.SHOW_ALWAYS);
        binding.cropImageView.setGuideShowMode(CropImageView.ShowMode.SHOW_ON_TOUCH);

    }


    private Uri saveCroppedImage(Bitmap bitmap) {
        File cacheDir = getCacheDir(); // app's cache directory

        String filename = System.currentTimeMillis() + ".jpg";

        File imageFile = new File(cacheDir, filename);


        // Save the new image
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            PrepNestUtil.showToast(this, "Failed to save image");
            return null;
        }

        return FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
    }
}
