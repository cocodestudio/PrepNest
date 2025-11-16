package com.cocode.prepnest;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.AddcashBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;


public class AddcashActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;
    Uri screenshotUri;
    private AddcashBinding binding;
    private String qrText = "";
    private String fileName = "";
    private FirebaseAuth auth;
    private LogUtils logFile;
    private NetworkMonitor networkMonitor;


    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = AddcashBinding.inflate(getLayoutInflater());
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

        binding.sendReqBtn.setOnClickListener(_view -> {
            if (screenshotUri == null) {
                PrepNestUtil.showToast(AddcashActivity.this, "Please select the payment screenshot.");
            } else {
                if (PrepNestUtil.isConnected(AddcashActivity.this)) {
                    auth = FirebaseAuth.getInstance();
                    if (auth.getCurrentUser() != null) {
                        uploadPhotoToStorage(screenshotUri);
                    } else {
                        PrepNestUtil.showToast(AddcashActivity.this, "Please login again!");
                        auth.signOut();
                        finish();
                    }
                } else {
                    com.google.android.material.snackbar.Snackbar.make(binding.wrapperLayout, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", _view1 -> {

                    }).show();
                }
            }
        });

        binding.ssFileNameContainer.setOnClickListener(_view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            startActivityForResult(intent, PICK_IMAGE_REQUEST);

        });
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        networkMonitor = new NetworkMonitor(this);
        logFile.addActivity();
        designUI();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
    }

    @Override
    protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
        super.onActivityResult(_requestCode, _resultCode, _data);
        if (_requestCode == PICK_IMAGE_REQUEST) {
            if (_resultCode == Activity.RESULT_OK) {
                if (_data != null) {
                    screenshotUri = _data.getData();
                    if (screenshotUri != null) {
                        fileName = getFileNameFromURI(screenshotUri);
                        binding.ssFileName.setText(fileName);
                        if (fileName.length() > 25) {
                            binding.ssFileName.setText(fileName.substring(0, 25).concat("..."));
                        }
                    }
                }
            }
        }
    }


    @Override
    protected void onPostCreate(Bundle _savedInstanceState) {
        super.onPostCreate(_savedInstanceState);
        String QR_CODE_STRUCTURE = "upi://pay?pa=upi-id&pn=XXXPGN%20KOTAK%20811%20WALLET%20PGN&mc=0000&mode=02&purpose=00";
        String UPI_ID = "7078031800@axl";
        qrText = QR_CODE_STRUCTURE.replace("upi-id", UPI_ID);
        generateQR();
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

    public void designUI() {
        PrepNestUtil.roundViewWithRipple(binding.ssFileNameContainer, "#FAFAFA", 30, 5, "#000000", "#E0E0E0");
        binding.ssAttachContainer.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 30, 0xFFFAFAFA));
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public void generateQR() {
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        QRGEncoder qrgEncode = getQrgEncoder(manager);

        try {
            Bitmap qrBitmap = qrgEncode.getBitmap();

            // Just in case transparency causes issues, enforce white background
            Bitmap finalBitmap = Bitmap.createBitmap(
                    qrBitmap.getWidth(),
                    qrBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(finalBitmap);
            canvas.drawColor(Color.WHITE); // solid white background
            canvas.drawBitmap(qrBitmap, 0, 0, null);

            binding.qrCode.setBackgroundColor(Color.WHITE);
            binding.qrCode.setImageBitmap(finalBitmap);

            PrepNestUtil.TransitionManager(binding.wrapperLayout, 150);
            binding.qrCode.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private QRGEncoder getQrgEncoder(WindowManager manager) {
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = Math.min(width, height);
        smallerDimension = smallerDimension * 3 / 4;

        QRGEncoder qrgEncode = new QRGEncoder(
                qrText,
                null,
                QRGContents.Type.TEXT,
                smallerDimension
        );

        // Force black QR squares + white background
        qrgEncode.setColorBlack(Color.WHITE);  // QR pattern
        qrgEncode.setColorWhite(Color.BLACK);  // Background
        return qrgEncode;
    }


    public String getFileNameFromURI(final Uri _uri) {
        String result = null;
        try (Cursor cursor = getContentResolver().query(_uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                result = cursor.getString(nameIndex);
            }
        }

        return result;

    }


    public void uploadPhotoToStorage(final Uri _uri) {
        PrepNestUtil.showLoadingDialog(this, true);
        logFile.addLog("ADD CASH", "UPLOADING SCREENSHOT");
        StorageReference fileRef = FirebaseStorage.getInstance().getReference("other/add_cash_screenshots").child(auth.getCurrentUser().getUid()).child(System.currentTimeMillis() + ".jpg");
        UploadTask fileUpload = fileRef.putFile(_uri);
        fileUpload.addOnSuccessListener(taskSnapshot -> {
            logFile.addLog("ADD CASH", "SCREENSHOT UPLOADED");
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> sendRequest(uri.toString()));
        });
        fileUpload.addOnFailureListener(exception -> {
            logFile.addLog("ADD CASH", "FAILED TO UPLOAD : ".concat(exception.toString()));
            PrepNestUtil.showToast(AddcashActivity.this, "An unknown error occurred : ".concat(exception.toString()));
            PrepNestUtil.showLoadingDialog(this, false);
        });
    }


    public void sendRequest(final String _url) {
        logFile.addLog("ADD CASH", "SENDING REQUEST");
        DatabaseReference addCashRef = FirebaseDatabase.getInstance().getReference("requests/add_cash_requests").child(auth.getCurrentUser().getUid());
        String key = addCashRef.push().getKey();
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("photo url", _url);
        dataMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
        addCashRef.child(key).setValue(dataMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                logFile.addLog("ADD CASH", "REQUEST SENT SUCCESSFULLY");
                screenshotUri = null;
                fileName = "";
                PrepNestUtil.showToast(AddcashActivity.this, "Request sent successfully.");
                binding.ssFileName.setText("Select screenshot");
                PrepNestUtil.showLoadingDialog(this, false);
            } else {
                logFile.addLog("ADD CASH", "FAILED TO SENT REQUEST : ".concat(task.getException().toString()));
                PrepNestUtil.showLoadingDialog(this, false);
                PrepNestUtil.showToast(AddcashActivity.this, "An unknown error occurred.");
            }
        });
    }
}
