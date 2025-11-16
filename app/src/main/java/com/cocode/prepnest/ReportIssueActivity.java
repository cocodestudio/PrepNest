package com.cocode.prepnest;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.ActivityReportIssueBinding;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class ReportIssueActivity extends AppCompatActivity {

    private ActivityReportIssueBinding binding;
    private Uri imageURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportIssueBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle savedInstanceState) {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });

        binding.backIcon.setOnClickListener(_view -> {
            finish();
        });

        binding.attachContainer.setOnClickListener(_view -> {
            Intent openDoc = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            openDoc.setType("image/*");
            openDoc.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(openDoc, 1000);
        });

        binding.btnSubmit.setOnClickListener(_view -> {
            if (binding.edittext.getText().toString().isBlank()) {
                PrepNestUtil.TransitionManager(binding.wrapperLayout, 150);
                binding.edittextError.setVisibility(View.VISIBLE);
            } else {
                PrepNestUtil.showLoadingDialog(ReportIssueActivity.this, true);
                if (imageURI != null) {
                    uploadImage();
                } else {
                    sendReport(binding.edittext.getText().toString(), null);
                }
            }
        });

        binding.edittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s == null) return;
                int length = s.length();
                if (length > 1000) {
                    binding.edittext.removeTextChangedListener(this);
                    s.delete(1000, length);
                    binding.edittext.addTextChangedListener(this);
                }
                binding.edittextLimit.setText(s.length() + "/1000");

                if (binding.edittextError.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.wrapperLayout, 150);
                    binding.edittextError.setVisibility(View.INVISIBLE);
                }
            }


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
    }

    private void initializeLogic() {
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
        binding.edittextLimit.setText("0/1000");
        loadBannerAd();
    }

    private void loadBannerAd() {
        MobileAds.initialize(this, initializationStatus -> {});
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adView.loadAd(adRequest);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view != null && ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (view instanceof EditText) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                float x = ev.getRawX() + view.getLeft() - location[0];
                float y = ev.getRawY() + view.getTop() - location[1];

                if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom()) {
                    PrepNestUtil.hideKeyboard(ReportIssueActivity.this);
                    view.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000) {
            if (resultCode == ReportIssueActivity.RESULT_OK) {
                if (data != null) {
                    imageURI = data.getData();
                    if (imageURI != null) {
                        String fileName = getFileNameFromURI(imageURI);
                        if (fileName.length() > 15) {
                            binding.attachImageTitle.setText(fileName.substring(0, 15).concat("..."));
                        } else {
                            binding.attachImageTitle.setText(fileName);
                        }
                    }
                }
            }
        }
    }

    public void uploadImage() {
        PrepNestUtil.showLoadingDialog(this, true);
        StorageReference fileRef = FirebaseStorage.getInstance().getReference("other/reports/error_reports/").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(System.currentTimeMillis() + ".jpg");
        UploadTask uploadTask = fileRef.putFile(imageURI);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                sendReport(binding.edittext.getText().toString(), uri.toString());
            });
        }).addOnFailureListener(exception -> {
            PrepNestUtil.showToast(ReportIssueActivity.this, "An unknown error occurred : ".concat(exception.toString()));
            PrepNestUtil.showLoadingDialog(ReportIssueActivity.this, false);
        });
    }

    public void sendReport(String issue, String imageURL) {
        DatabaseReference errorRef = FirebaseDatabase.getInstance().getReference("reports/error_reports");
        HashMap<String, Object> pushData = new HashMap<>();

        if (imageURL != null) {
            pushData.put("imageUrl", imageURL);
        }

        pushData.put("reportBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
        pushData.put("issue", binding.edittext.getText().toString().trim());
        pushData.put("timestamp", String.valueOf(System.currentTimeMillis()));

        errorRef.child(errorRef.push().getKey()).setValue(pushData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                PrepNestUtil.showLoadingDialog(ReportIssueActivity.this, false);
                PrepNestUtil.showToast(ReportIssueActivity.this, "Sent successfully!");
                binding.edittext.setText("");
                binding.attachImageTitle.setText(R.string.attach_a_screenshot);
                imageURI = null;
            } else {
                PrepNestUtil.showLoadingDialog(ReportIssueActivity.this, false);
                PrepNestUtil.showToast(ReportIssueActivity.this, "An unknown error occurred: " + task.getException().toString());
            }
        });
    }

    public String getFileNameFromURI(final Uri uri) {
        String result = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                result = cursor.getString(nameIndex);
            }
        }
        return result;
    }
}