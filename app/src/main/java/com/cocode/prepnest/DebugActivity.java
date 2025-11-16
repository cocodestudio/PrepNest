package com.cocode.prepnest;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.DebugBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
//        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", getIntent().getStringExtra("error")));
        PrepNestUtil.setLightStatusBar(this);
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


//    public void sendErrorReport() {
//        File externalDir = getExternalFilesDir(null);
//
//        File logFile = new File(externalDir, "logs.txt");
//
//        if (logFile.exists()) {
//            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", logFile);
//
//            if (fileUri != null) {
//                StorageReference logRef = FirebaseStorage.getInstance().getReference("other").child("reports").child("error_reports").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(System.currentTimeMillis() + ".txt");
//                PrepNestUtil.showLoadingDialog(this, true);
//                logRef.putFile(fileUri).addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
//                    // upload data to fdb
//                    uploadDataToFDB(uri.toString());
//                })).addOnFailureListener(error -> {
//                    PrepNestUtil.showLoadingDialog(this, false);
//                    PrepNestUtil.showToast(DebugActivity.this, "Failed to send request");
//                });
//            }
//        } else {
//            // upload without log file
//            uploadDataToFDB("null");
//        }
//    }


    public void sendErrorReport() {
        PrepNestUtil.showLoadingDialog(this, true);
        DatabaseReference reports = FirebaseDatabase.getInstance().getReference("reports/error_reports");
        HashMap<String, Object> errorMap = new HashMap<>();
        errorMap.put("error", getIntent().getStringExtra("error"));
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            errorMap.put("user", FirebaseAuth.getInstance().getCurrentUser().getUid());
        } else {
            errorMap.put("user", "null");
        }
        errorMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
        reports.child(reports.push().getKey()).setValue(errorMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                PrepNestUtil.showLoadingDialog(this, false);
                PrepNestUtil.showToast(DebugActivity.this, "Sent successfully!");
                finishAffinity();
            } else {
                PrepNestUtil.showLoadingDialog(this, false);
                PrepNestUtil.showToast(DebugActivity.this, "An unknown error occurred");
                finishAffinity();
            }
        });
    }
}