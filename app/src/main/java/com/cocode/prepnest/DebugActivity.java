package com.cocode.prepnest;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.cocode.prepnest.databinding.DebugBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.HashMap;

public class DebugActivity extends AppCompatActivity {

    private DebugBinding binding;
    private NetworkMonitor networkMonitor;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = DebugBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {

        binding.btnSendReport.setOnClickListener(_view -> sendErrorReport());

        binding.btnExitApp.setOnClickListener(_view -> finishAffinity());
    }

    private void initializeLogic() {
        networkMonitor = new NetworkMonitor(this);
        PrepNestUtil.roundViewWithRipple(binding.btnSendReport, "#000000", 15, 0, "#000000", "#212121");
        PrepNestUtil.roundViewWithRipple(binding.btnExitApp, "#FAFAFA", 15, 0, "#000000", "#E0E0E0");
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", getIntent().getStringExtra("error")));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        PrepNestUtil.changeNavBarColor(this, true);
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


    public void sendErrorReport() {
        File externalDir = getExternalFilesDir(null);

        File logFile = new File(externalDir, "logs.txt");

        if (logFile.exists()) {
            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", logFile);

            if (fileUri != null) {
                StorageReference logRef = FirebaseStorage.getInstance().getReference("other").child("reports").child("error_reports").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(System.currentTimeMillis() + ".txt");
                PrepNestUtil.showLoadingDialog(this, true);
                logRef.putFile(fileUri).addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    // upload data to fdb
                    uploadDataToFDB(uri.toString());
                })).addOnFailureListener(error -> {
                    PrepNestUtil.showLoadingDialog(this, false);
                    PrepNestUtil.showToast(DebugActivity.this, "Failed to send request");
                });
            }
        } else {
            // upload without log file
            uploadDataToFDB("null");
        }
    }


    public void uploadDataToFDB(final String _url) {
        DatabaseReference reports = FirebaseDatabase.getInstance().getReference("reports/error_reports");
        HashMap<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", getIntent().getStringExtra("error"));
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            errorMap.put("user", FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
        if (!_url.equals("null")) {
            errorMap.put("log file", _url);
        }
        errorMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
        reports.child(reports.push().getKey()).setValue(errorMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                PrepNestUtil.showLoadingDialog(this, false);
                Toast.makeText(DebugActivity.this, "Sent successfully!", Toast.LENGTH_SHORT).show();
                finishAffinity();
            } else {
                PrepNestUtil.showLoadingDialog(this, false);
                Toast.makeText(DebugActivity.this, "An unknown error occurred", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        });
    }

}