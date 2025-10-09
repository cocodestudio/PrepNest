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
import android.view.View.OnClickListener;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.AddcashBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;


public class AddcashActivity extends AppCompatActivity {

    private AddcashBinding binding;
    private Bitmap bitmap;
    private QRGEncoder qrgEncode;
    private String qrText = "";
    private String QR_CODE_STRUCTURE = "";
    private String UPI_ID = "";
    private static final int PICK_IMAGE_REQUEST = 100;
    private String fileName = "";
    Uri screenshotUri;
    private HashMap<String, Object> dataMap = new HashMap<>();
    private String key = "";
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

        binding.backIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                finish();
                overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
            }
        });

        binding.sendReqBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
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
                        com.google.android.material.snackbar.Snackbar.make(binding.container, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", new OnClickListener() {
                            @Override
                            public void onClick(View _view) {

                            }
                        }).show();
                    }
                }
            }
        });

        binding.ssFileNameContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                startActivityForResult(intent, PICK_IMAGE_REQUEST);

            }
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
                            binding.ssFileName.setText(fileName.substring((int) (0), (int) (25)).concat("..."));
                        }
                    }
                }
            }
        }
    }


    @Override
    protected void onPostCreate(Bundle _savedInstanceState) {
        super.onPostCreate(_savedInstanceState);
        QR_CODE_STRUCTURE = "upi://pay?pa=upi-id&pn=XXXPGN%20KOTAK%20811%20WALLET%20PGN&mc=0000&mode=02&purpose=00";
        UPI_ID = "7078031800@axl";
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
    }


    public void generateQR() {
        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 3 / 4;

        qrgEncode = new QRGEncoder(
                qrText,
                null,
                QRGContents.Type.TEXT,
                smallerDimension
        );

        // Force black QR squares + white background
        qrgEncode.setColorBlack(Color.WHITE);  // QR pattern
        qrgEncode.setColorWhite(Color.BLACK);  // Background

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

            PrepNestUtil.TransitionManager(binding.container, 150);
            binding.qrCode.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public String getFileNameFromURI(final Uri _uri) {
        String result = null;
        Cursor cursor = getContentResolver().query(_uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                result = cursor.getString(nameIndex);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
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
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                sendRequest(uri.toString());
            });
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
        key = addCashRef.push().getKey();
        dataMap = new HashMap<>();
        dataMap.put("photo url", _url);
        dataMap.put("timestamp", String.valueOf((long) (System.currentTimeMillis())));
        addCashRef.child(key).setValue(dataMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                sendNotifications("admin", "Add cash request", "Add cash request from '".concat(getIntent().getStringExtra("name").concat("'")));
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


    public void sendNotifications(final String topic, final String title, final String message) {
        new Thread(() -> {
            try {
                URL url = new URL("https://us-central1-prepnest-65133.cloudfunctions.net/sendNotification");

                // Open Connection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                //Create JSON Object
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("topic", topic);
                jsonParam.put("title", title);
                jsonParam.put("body", message);

                // Write JSON to request body
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonParam.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                // Get response
                int responseCode = conn.getResponseCode();
                BufferedReader br;
                if (responseCode >= 200 && responseCode < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }

                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                br.close();

                // Show result on UI Thread
				        /*
		runOnUiThread(() -> {
            _loadingDialog(false);
			Toast.makeText(AddcashActivity.this, "Response: " + response, Toast.LENGTH_SHORT).show();
		});
        */
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
				        /*
        _loadingDialog(false);
		runOnUiThread(() -> {
			Toast.makeText(AddcashActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		});
        */
            }
        }).start();

    }

}
