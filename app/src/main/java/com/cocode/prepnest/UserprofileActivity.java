package com.cocode.prepnest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cocode.prepnest.databinding.ProfileViewBinding;
import com.cocode.prepnest.databinding.StatusViewBinding;
import com.cocode.prepnest.databinding.UserprofileBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;


public class UserprofileActivity extends AppCompatActivity implements ItemListSheetFragment.BottomSheetListener {

    private static final int REQUEST_PICK_IMAGE = 100;
    private static final int REQUEST_CROP_IMAGE = 200;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "upload_channel_id";
    private final int otpResendTime = 120;
    private final String verificationID = "";
    private final String tempUID = "";
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final Timer _timer = new Timer();
    Uri imageUri;
    Uri resultUri;
    private ArrayList<HashMap<String, Object>> itemsList = new ArrayList<>();
    private UserprofileBinding binding;
    private HashMap<String, Object> userData = new HashMap<>();
    private boolean editMode = false;
    private boolean removeProfileFromStorage = false;
    private String jsonCourseData = null;
    private String selectedSemesterId = "-1";
    private DatabaseReference users;
    private StorageReference profile_pictures;
    private NetworkMonitor networkMonitor;
    private int selectedSemester = 0;
    private boolean semesterChanged = false;
    private com.google.android.material.bottomsheet.BottomSheetDialog emailVerificationSheet;
    private com.google.android.material.bottomsheet.BottomSheetDialog phn_verification_sheet;
    private int minutes = 0;
    private int seconds = 0;
    private TimerTask timer;
    private SharedPreferences cachedData;
    private SharedPreferences userCredentials;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = UserprofileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize() {
        userCredentials = getSharedPreferences("credentials", Activity.MODE_PRIVATE);
        cachedData = getSharedPreferences("userCachedData", Activity.MODE_PRIVATE);

        binding.backIcon.setOnClickListener(_view -> {
            if (editMode) {
                PrepNestUtil.TransitionManager(binding.container, 150);
                PrepNestUtil.showLoadingDialog(UserprofileActivity.this, true);
                editMode = false;
                binding.editIcon.setImageResource(R.drawable.icon_edit);
                binding.nameEdittext.setEnabled(false);
                binding.phnNumberEdittext.setEnabled(false);
                binding.semesterSelectContainer.setEnabled(false);
                binding.changeImgTxt.setVisibility(View.GONE);
                binding.removeProfileTxt.setVisibility(View.GONE);
                binding.downArrowIcon1.setVisibility(View.GONE);
                binding.nameErrorTxt.setVisibility(View.GONE);
                binding.phnNmberErrorTxt.setVisibility(View.GONE);
                PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
            } else {
                finish();
                overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
            }
        });

        binding.editIcon.setOnClickListener(_view -> {
            Rect r = new Rect();
            binding.background.getWindowVisibleDisplayFrame(r);
            int screenHeight = binding.background.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > (screenHeight * 0.15d)) {
                PrepNestUtil.hideKeyboard(UserprofileActivity.this);
            }
            PrepNestUtil.TransitionManager(binding.container, 150);
            if (editMode) {
                if (binding.nameEdittext.getText().toString().trim().isEmpty()) {
                    binding.nameErrorTxt.setText("Enter your name");
                    binding.nameErrorTxt.setVisibility(View.VISIBLE);
                } else {
                    if (binding.phnNumberEdittext.getText().toString().trim().isEmpty()) {
                        if (userData.containsKey("phoneNumber")) {
                            binding.phnNmberErrorTxt.setText("Enter your phoneNumber");
                            binding.phnNmberErrorTxt.setVisibility(View.VISIBLE);
                        } else {
                            binding.editIcon.setImageResource(R.drawable.icon_edit);
                            binding.nameEdittext.setEnabled(false);
                            binding.phnNumberEdittext.setEnabled(false);
                            binding.semesterSelectContainer.setEnabled(false);
                            binding.changeImgTxt.setVisibility(View.GONE);
                            binding.removeProfileTxt.setVisibility(View.GONE);
                            binding.downArrowIcon1.setVisibility(View.GONE);
                            if (PrepNestUtil.isConnected(UserprofileActivity.this)) {
                                editMode = !editMode;
                                if (removeProfileFromStorage) {
                                    deleteProfileFromStorage(Objects.requireNonNull(userData.get("profile")).toString());
                                } else {
                                    if (resultUri != null) {
                                        uploadProfileToFirebase(resultUri);
                                    } else {
                                        updateUserData(false, "null");
                                    }
                                }
                            } else {
                                com.google.android.material.snackbar.Snackbar.make(binding.background, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", _view1 -> {

                                }).show();
                            }
                        }
                    } else {
                        if (!(binding.phnNumberEdittext.getText().toString().trim().length() == 10)) {
                            binding.phnNmberErrorTxt.setText("Enter a valid phoneNumber");
                            binding.phnNmberErrorTxt.setVisibility(View.VISIBLE);
                        } else {
                            binding.editIcon.setImageResource(R.drawable.icon_edit);
                            binding.nameEdittext.setEnabled(false);
                            binding.phnNumberEdittext.setEnabled(false);
                            binding.semesterSelectContainer.setEnabled(false);
                            binding.changeImgTxt.setVisibility(View.GONE);
                            binding.removeProfileTxt.setVisibility(View.GONE);
                            binding.downArrowIcon1.setVisibility(View.GONE);
                            if (PrepNestUtil.isConnected(UserprofileActivity.this)) {
                                editMode = !editMode;
                                if (removeProfileFromStorage) {
                                    deleteProfileFromStorage(Objects.requireNonNull(userData.get("profile")).toString());
                                } else {
                                    if (resultUri != null) {
                                        uploadProfileToFirebase(resultUri);
                                    } else {
                                        updateUserData(false, "null");
                                    }
                                }
                            } else {
                                com.google.android.material.snackbar.Snackbar.make(binding.background, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", _view2 -> {

                                }).show();
                            }
                        }
                    }
                }
            } else {
                editMode = true;
                binding.editIcon.setImageResource(R.drawable.icon_done);
                binding.nameEdittext.setEnabled(true);
                binding.phnNumberEdittext.setEnabled(true);
                binding.semesterSelectContainer.setEnabled(true);
                if (userData.containsKey("profile")) {
                    if (Objects.requireNonNull(userData.get("profile")).toString().equals("null")) {
                        binding.removeProfileTxt.setVisibility(View.GONE);
                    } else {
                        binding.removeProfileTxt.setVisibility(View.VISIBLE);
                    }
                } else {
                    binding.removeProfileTxt.setVisibility(View.GONE);
                }
                binding.changeImgTxt.setVisibility(View.VISIBLE);
                binding.downArrowIcon1.setVisibility(View.VISIBLE);
            }
        });

        binding.changeImgTxt.setOnClickListener(_view -> {
            Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
            pickIntent.setType("image/*");
            startActivityForResult(pickIntent, REQUEST_PICK_IMAGE);
        });

        binding.removeProfileTxt.setOnClickListener(_view -> {
            PrepNestUtil.TransitionManager(binding.background, 150);
            binding.removeProfileTxt.setVisibility(View.GONE);
            if (userData.containsKey("profile")) {
                if (Objects.requireNonNull(userData.get("profile")).toString().equals("null")) {
                    if (resultUri != null) {
                        resultUri = null;
                        binding.defaultProfileContainer.setVisibility(View.VISIBLE);
                        binding.userProfileContainer.setVisibility(View.GONE);
                    }
                } else {
                    if (resultUri == null) {
                        removeProfileFromStorage = true;
                        binding.defaultProfileContainer.setVisibility(View.VISIBLE);
                        binding.userProfileContainer.setVisibility(View.GONE);
                    } else {
                        resultUri = null;
                        Glide.with(getApplicationContext())
                                .load(Uri.parse(Objects.requireNonNull(userData.get("profile")).toString()))
                                .into(binding.userProfilePicture);
                        binding.removeProfileTxt.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        binding.userProfileContainer.setOnClickListener(_view -> {
            if (!editMode) {
                showUserProfileViewDialog();
            }
        });

        binding.nameEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {
                if (binding.nameErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.nameErrorTxt.setVisibility(View.GONE);
                }
            }
        });

        binding.semesterSelectContainer.setOnClickListener(_view -> {
            Rect r = new Rect();
            binding.background.getWindowVisibleDisplayFrame(r);
            int screenHeight = binding.background.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > (screenHeight * 0.15d)) {
                PrepNestUtil.hideKeyboard(UserprofileActivity.this);
            }
            createSemestersList(Objects.requireNonNull(userData.get("courseId")).toString());

            HashMap<String, Object> dataPayload = new HashMap<>();
            dataPayload.put("type", ItemListSheetFragment.SheetType.SEMESTER.toString());
            dataPayload.put("list", new ArrayList<>(itemsList));
            dataPayload.put("selectedItemId", selectedSemesterId);

            final ItemListSheetFragment semSheet = ItemListSheetFragment.newInstance(dataPayload);
            semSheet.show(getSupportFragmentManager(), "semSheet");
        });

        binding.btnEmailVerification.setOnClickListener(_view -> {
            PrepNestUtil.showLoadingDialog(UserprofileActivity.this, true);
            if (auth.getCurrentUser() != null) {
                auth.getCurrentUser().reload().addOnCompleteListener(checkStatus -> {
                    if (auth.getCurrentUser().isEmailVerified()) {
                        binding.btnEmailVerification.setVisibility(View.GONE);
                        PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
                    } else {
                        auth.getCurrentUser().sendEmailVerification().addOnCompleteListener(verifyStatus -> {
                            if (verifyStatus.isSuccessful()) {
                                showEmailVerificationSheet();
                                PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
                            } else {
                                PrepNestUtil.showToast(UserprofileActivity.this, "Failed to send link, try again later.");
                                PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
                            }
                        });
                    }
                });
            }
        });

        binding.phnNumberEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {
                if (binding.phnNmberErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.phnNmberErrorTxt.setVisibility(View.GONE);
                }
            }
        });

        binding.btnPhoneVerification.setOnClickListener(_view -> {
            /*
_loadingDialog(true);
if (userData.containsKey("phoneNumber")) {
if (userData.containsKey("phone verified")) {
if (userData.get("phone verified").toString().equals("true")) {
btn_phone_verification.setVisibility(View.GONE);
_loadingDialog(false);
} else {
btn_phone_verification.setVisibility(View.VISIBLE);
btn_phone_verification.setText("VERIFY");
//Send OTP
}
} else {
//Send OTP
}
} else {

}
*/
        });

    }

    private void initializeLogic() {
        users = FirebaseDatabase.getInstance().getReference("users");
        profile_pictures = FirebaseStorage.getInstance().getReference("profile_pictures");
        networkMonitor = new NetworkMonitor(this);
        semesterChanged = false;
        designUI();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
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
                    PrepNestUtil.hideKeyboard(UserprofileActivity.this);
                    view.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
        super.onActivityResult(_requestCode, _resultCode, _data);
        if ((_requestCode == REQUEST_PICK_IMAGE) && ((_resultCode == RESULT_OK) && _data != null)) {
            imageUri = _data.getData();
            Intent cropIntent = new Intent(this, ImagecropActivity.class);
            cropIntent.putExtra("imageUri", imageUri.toString());
            startActivityForResult(cropIntent, REQUEST_CROP_IMAGE);
        }
        if ((_requestCode == REQUEST_CROP_IMAGE) && ((_resultCode == RESULT_OK) && _data != null)) {
            resultUri = null;
            resultUri = Uri.parse(_data.getStringExtra("croppedImageUri"));
            binding.userProfilePicture.setImageURI(null);
            binding.userProfilePicture.setImageURI(resultUri);
            binding.defaultProfileContainer.setVisibility(View.GONE);
            binding.userProfileContainer.setVisibility(View.VISIBLE);
            binding.removeProfileTxt.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isFinishing() || !isDestroyed()) {
            checkEmailVerificationStatus();
        }
    }

    @Override
    protected void onPostCreate(Bundle _savedInstanceState) {
        super.onPostCreate(_savedInstanceState);
        loadAllCoursesFromJson(this);
        loadUserDataFromSP();
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
        binding.semesterSelectContainer.setEnabled(false);
        binding.defaultProfileContainer.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 360, 0xFFFAFAFA));
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }

    public void loadUserDataFromSP() {
        if (cachedData.contains("userData")) {
            userData = new Gson().fromJson(cachedData.getString("userData", ""), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            loadUserDataToUI();
        } else {
            getUserData();
        }
    }

    public void getUserData() {
        PrepNestUtil.showLoadingDialog(UserprofileActivity.this, true);
        PrepNestUtil.TransitionManager(binding.background, 150);
        assert auth.getCurrentUser() != null;
        users.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                userData = dataSnapshot.getValue(new GenericTypeIndicator<>() {
                });

                if (userData != null) {
                    loadUserDataToUI();
                } else {
                    PrepNestUtil.showToast(UserprofileActivity.this, "Failed to load data, login again!");
                    FirebaseAuth.getInstance().signOut();
                    finishAffinity();
                }
                PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                if (!isFinishing() && !isDestroyed()) {
                    PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
                    FirebaseAuth.getInstance().signOut();
                    PrepNestUtil.showToast(UserprofileActivity.this, "Please login again!");
                    finishAffinity();
                }
            }
        });

    }

    public void loadUserDataToUI() {
        if (userData.containsKey("courseId")) {
            userData.put("courseDuration", getCourseDuration(Objects.requireNonNull(userData.get("courseId")).toString()));
            final String courseName = getCourseName(Objects.requireNonNull(userData.get("courseId")).toString());
            int spaceIndex = courseName.indexOf(" ");
            if (spaceIndex != -1) {
                binding.courseTxt.setText(courseName.substring(0, spaceIndex));
            } else {
                binding.courseTxt.setText(courseName);
            }
        } else {
            userData.put("courseDuration", null);
            binding.courseTxt.setText("Course");
        }
        binding.profileContainer.setVisibility(View.VISIBLE);
        if (userData.containsKey("profile")) {
            if (Objects.requireNonNull(userData.get("profile")).toString().equals("null")) {
                binding.userProfileContainer.setVisibility(View.GONE);
                binding.defaultProfileContainer.setVisibility(View.VISIBLE);
            } else {
                binding.userProfileContainer.setVisibility(View.VISIBLE);
                binding.defaultProfileContainer.setVisibility(View.GONE);
                Glide.with(getApplicationContext())
                        .load(Uri.parse(Objects.requireNonNull(userData.get("profile")).toString()))
                        .into(binding.userProfilePicture);
            }
        } else {
            binding.userProfileContainer.setVisibility(View.GONE);
            binding.defaultProfileContainer.setVisibility(View.VISIBLE);
        }
        binding.nameEtContainer.setVisibility(View.VISIBLE);
        if (userData.containsKey("name")) {
            binding.defaultProfileTitle.setText(generateDefaultName(Objects.requireNonNull(userData.get("name")).toString()).toUpperCase());
            binding.nameEdittext.setText(Objects.requireNonNull(userData.get("name")).toString());
        } else {
            binding.defaultProfileTitle.setText("PU");
            binding.nameEdittext.setText("PrepNest User");
        }
        binding.emailEtContainer.setVisibility(View.VISIBLE);
        if (userData.containsKey("email")) {
            binding.emailText.setText(Objects.requireNonNull(userData.get("email")).toString());
        } else {
            binding.emailText.setText("");
        }
        binding.phnNumberContainer.setVisibility(View.VISIBLE);
        if (userData.containsKey("phoneNumber")) {
            binding.phnNumberEdittext.setText(decrypt(Objects.requireNonNull(userData.get("phoneNumber")).toString(), Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()));
						/*
if (userData.containsKey("phone verified")) {
if (userData.get("phone verified").toString().equals("true")) {
btn_phone_verification.setVisibility(View.GONE);
} else {
btn_phone_verification.setVisibility(View.VISIBLE);
btn_phone_verification.setText("VERIFY");
}
} else {
btn_phone_verification.setVisibility(View.VISIBLE);
btn_phone_verification.setText("VERIFY");
}
*/
        } else {
            binding.phnNumberEdittext.setText("");
            binding.btnPhoneVerification.setVisibility(View.GONE);
        }
        binding.otherDetailsContainer.setVisibility(View.VISIBLE);
        if (userData.containsKey("semester")) {
            selectedSemester = ((Number) Objects.requireNonNull(userData.get("semester"))).intValue();
            selectedSemesterId = String.valueOf(selectedSemester);
            binding.semesterTxt.setText(PrepNestUtil.getFormattedNumber(selectedSemester).concat(" semester"));
        } else {
            binding.semesterTxt.setText("Semester");
        }
        if ((userData.get("courseDuration") == null)) {
            createSemestersList("BON6SOM");
        } else {
            createSemestersList(Objects.requireNonNull(userData.get("courseId")).toString());
        }
    }

    public String generateDefaultName(final String _name) {
        if (_name.contains(" ")) {
            int lastIndex = _name.lastIndexOf(" ");
            return _name.substring(0, 1).concat(_name.substring(lastIndex + 1, lastIndex + 2));
        } else {
            return _name.substring(0, 1);
        }
    }

    public void loadAllCoursesFromJson(Context context) {
        try (InputStream is = context.getAssets().open("courses.json");
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }

            jsonCourseData = bos.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getCourseName(String courseId) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonCourseData);
            return jsonObject.getJSONObject(courseId).getString("name");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public double getCourseDuration(String courseId) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonCourseData);
            return jsonObject.getJSONObject(courseId).getDouble("duration");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void createSemestersList(String courseId) {
        final double courseMaxDuration = getCourseDuration(courseId);
        final int courseMaxSemesters = (int) (courseMaxDuration * 2);
        itemsList = new ArrayList<>();

        for (int sem = 1; sem <= courseMaxSemesters; sem++) {
            HashMap<String, Object> semesterMap = new HashMap<>();
            semesterMap.put("id", String.valueOf(sem));
            semesterMap.put("text", PrepNestUtil.getFormattedNumber(sem).concat(" semester"));

            itemsList.add(semesterMap);
        }
    }


    public void showEmailVerificationSheet() {
        emailVerificationSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UserprofileActivity.this);
        StatusViewBinding sheetBinding = StatusViewBinding.inflate(getLayoutInflater());

        emailVerificationSheet.setContentView(sheetBinding.getRoot());

        emailVerificationSheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });


        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetBinding.bg.setBackground(gd);
        sheetBinding.subtext.setTextSize(11);
        sheetBinding.title.setVisibility(View.GONE);
        sheetBinding.btnOk.setVisibility(View.GONE);
//        sheetBinding.imageContainer.setBackground(new GradientDrawable() {
//            public GradientDrawable getIns(int a, int b) {
//                this.setCornerRadius(a);
//                this.setColor(b);
//                return this;
//            }
//        }.getIns((int) 360, 0xFFFAFAFA));
        PrepNestUtil.roundViewWithRipple(sheetBinding.btnCancel, "#000000", 15, 0, "#000000", "#212121");
        sheetBinding.btnCancelTxt.setTextColor(0xFFFFFFFF);
        sheetBinding.image.setImageResource(R.drawable.icon_email);
        sheetBinding.subtext.setText("We have sent you a mail on ".concat(Objects.requireNonNull(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail()).concat(" with a account verification link, click on the link to verify your email. If you didn't find the email, check spam folder.")));
        sheetBinding.btnCancelTxt.setText("Dismiss");
        sheetBinding.btnCancel.setOnClickListener(_view -> emailVerificationSheet.dismiss());
        emailVerificationSheet.setCancelable(true);
        emailVerificationSheet.show();
    }

    public void checkEmailVerificationStatus() {
        if (auth.getCurrentUser() != null) {
            auth.getCurrentUser().reload().addOnCompleteListener(checkStatus -> {
                if (auth.getCurrentUser().isEmailVerified()) {
                    binding.btnEmailVerification.setVisibility(View.GONE);
                } else {
                    binding.btnEmailVerification.setVisibility(View.VISIBLE);
                    binding.btnEmailVerification.setText("VERIFY");
                }
            });
        } else {
            PrepNestUtil.showToast(UserprofileActivity.this, "Unknown error, please login again");
            FirebaseAuth.getInstance().signOut();
            finishAffinity();
        }
    }

    public void _showOTPVerificationSheet() {
		/*
phn_verification_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UserprofileActivity.this);
View phn_verification_sheetView;

phn_verification_sheetView = getLayoutInflater().inflate(R.layout.otp_verification_sheet, null );

phn_verification_sheet.setContentView(phn_verification_sheetView);

phn_verification_sheet.getWindow().findViewById(R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
final LinearLayout container = (LinearLayout) phn_verification_sheetView.findViewById(R.id.container);
final LinearLayout upper_line = (LinearLayout) phn_verification_sheetView.findViewById(R.id.upper_line);
final LinearLayout et_container = (LinearLayout) phn_verification_sheetView.findViewById(R.id.et_container);
final LinearLayout btn_verify = (LinearLayout) phn_verification_sheetView.findViewById(R.id.btn_verify);
final TextView title = (TextView) phn_verification_sheetView.findViewById(R.id.title);
final TextView subtext = (TextView) phn_verification_sheetView.findViewById(R.id.subtext);
final TextView otp_edittext = (TextView) phn_verification_sheetView.findViewById(R.id.otp_edittext);
final TextView btn_verify_txt = (TextView) phn_verification_sheetView.findViewById(R.id.btn_verify_txt);
final TextView minutes_txt = (TextView) phn_verification_sheetView.findViewById(R.id.minutes_txt);
final TextView colon = (TextView) phn_verification_sheetView.findViewById(R.id.colon);
final TextView seconds_txt = (TextView) phn_verification_sheetView.findViewById(R.id.seconds_txt);
final TextView not_get_code_txt = (TextView) phn_verification_sheetView.findViewById(R.id.not_get_code_txt);
final TextView resend_code_txt = (TextView) phn_verification_sheetView.findViewById(R.id.resend_code_txt);
android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
gd.setColor(Color.parseColor("#FFFFFF"));
gd.setCornerRadii(new float[] {30, 30, 30, 30, 0, 0, 0, 0});
container.setBackground(gd);
upper_line.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFEEEEEE));
et_container.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)15, (int)3, 0xFF9E9E9E, 0xFFFAFAFA));
_addRippleAndRadius(btn_verify, "#000000", 15, 0, "#000000", "#212121");
resend_code_txt.setEnabled(false);
resend_code_txt.setTextColor(0xFF616161);
_rippleOnText(resend_code_txt, "#F5F5F5");
title.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
subtext.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
otp_edittext.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
btn_verify_txt.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
minutes_txt.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
colon.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
seconds_txt.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
not_get_code_txt.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
resend_code_txt.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/default_font.ttf"), 0);
otp_edittext.setFocusableInTouchMode(true);
_showTimerCountdown(otpResendTime, minutes_txt, seconds_txt, resend_code_txt);
subtext.setText("We have sent you an OTP on +91 ".concat(phn_number_edittext.getText().toString().trim().concat(" number to verify your phoneNumber")));
btn_verify.setOnClickListener(new View.OnClickListener() {
@Override
public void onClick(View _view) {
_loadingDialog(true);
//Verify OTP
}
});
resend_code_txt.setOnClickListener(new View.OnClickListener() {
@Override
public void onClick(View _view) {
otpResendTime += 60;
_showTimerCountdown(otpResendTime, minutes_txt, seconds_txt, resend_code_txt);
resend_code_txt.setEnabled(false);
resend_code_txt.setTextColor(0xFF616161);
//Send OTP
}
});
phn_verification_sheet.setCancelable(false);
phn_verification_sheet.show();
*/
    }

    public void _showTimerCountdown(final double _totalSeconds, final TextView _mText, final TextView _sText, final TextView _buttonText) {
        try {
            if (timer != null) {
                timer.cancel();
            }
        } catch (Exception ignored) {

        }
        minutes = (int) _totalSeconds / 60;
        seconds = (int) _totalSeconds % 60;
        timer = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (seconds == 0) {
                        if (minutes == 0) {
                            timer.cancel();
                            _buttonText.setEnabled(true);
                            _buttonText.setTextColor(0xFF000000);
                        } else {
                            minutes--;
                            seconds = 59;
                        }
                    } else {
                        seconds--;
                    }
                    if (minutes > 9) {
                        _mText.setText(String.valueOf((long) (minutes)));
                    } else {
                        _mText.setText("0".concat(String.valueOf((long) (minutes))));
                    }
                    if (seconds > 9) {
                        _sText.setText(String.valueOf((long) (seconds)));
                    } else {
                        _sText.setText("0".concat(String.valueOf((long) (seconds))));
                    }
                });
            }
        };

        _timer.scheduleAtFixedRate(timer, 0, 1000);
        //Defining scope variables
    }

    private javax.crypto.SecretKey generateKey(String pwd) throws Exception {

        final java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");

        byte[] b = pwd.getBytes(StandardCharsets.UTF_8);

        digest.update(b, 0, b.length);

        byte[] key = digest.digest();

        return new javax.crypto.spec.SecretKeySpec(key, "AES");
    }

    public String encrypt(final String _msg, final String _key) {
        try {
            javax.crypto.SecretKey key = generateKey(_key);
            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES");
            c.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            byte[] encVal = c.doFinal(_msg.getBytes());

            return android.util.Base64.encodeToString(encVal, android.util.Base64.DEFAULT);

        } catch (Exception ex) {
            Toast.makeText(UserprofileActivity.this, String.valueOf(ex), Toast.LENGTH_SHORT).show();
            return "null";
        }

    }

    public String decrypt(final String _msg, final String _key) {
        try {
            javax.crypto.spec.SecretKeySpec key = (javax.crypto.spec.SecretKeySpec) generateKey(_key);

            javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES");
            c.init(javax.crypto.Cipher.DECRYPT_MODE, key);
            byte[] decode = android.util.Base64.decode(_msg, android.util.Base64.DEFAULT);
            byte[] decal = c.doFinal(decode);
            return new String(decal);

        } catch (Exception ex) {
            return "Error: Invalid key";
        }

    }

    public void uploadProfileToFirebase(final Uri _imageUri) {
        PrepNestUtil.showLoadingDialog(this, true);
        if (_imageUri == null) {
            PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
            PrepNestUtil.showToast(UserprofileActivity.this, "Unknown error occurred, try again");
            return;
        }
        createNotificationChannel();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setContentTitle("Uploading").setContentText("Profile uploading").setPriority(NotificationCompat.PRIORITY_LOW).setOnlyAlertOnce(true).setProgress(100, 0, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build());
        assert auth.getCurrentUser() != null;
        StorageReference ref = profile_pictures.child(auth.getCurrentUser().getUid() + "." + getFileExtension(_imageUri));
        UploadTask profileUpload = ref.putFile(_imageUri);
        profileUpload.addOnProgressListener(taskSnapshot -> {
            long transferred = taskSnapshot.getBytesTransferred();
            long total = taskSnapshot.getTotalByteCount();
            int progress = (int) (100.0 * transferred / total);

            builder.setProgress(100, progress, false);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        });
        profileUpload.addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            updateUserData(true, uri.toString());
            builder.setContentText("Upload complete").setProgress(0, 0, false);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }));
        profileUpload.addOnFailureListener(e -> {
            builder.setContentText("Upload failed!").setProgress(0, 0, false);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
        });
    }

    public void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Upload Progress", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Notifications for file upload progress");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public String getFileExtension(final Uri _fileUri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(_fileUri));
    }

    public void updateUserData(final boolean _hasProfile, final String _url) {
        HashMap<String, Object> newUserData = new HashMap<>();
        if (!binding.nameEdittext.getText().toString().trim().equals(Objects.requireNonNull(userData.get("name")).toString())) {
            newUserData.put("name", binding.nameEdittext.getText().toString().trim());
        }
        String encryptedPhn;
        if (userData.containsKey("phoneNumber")) {
            String decryptedPhn = decrypt(Objects.requireNonNull(userData.get("phoneNumber")).toString(), Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
            if (!binding.phnNumberEdittext.getText().toString().trim().equals(decryptedPhn)) {
                encryptedPhn = encrypt(binding.phnNumberEdittext.getText().toString().trim(), FirebaseAuth.getInstance().getCurrentUser().getUid());
                newUserData.put("phoneNumber", encryptedPhn);
            }
        } else {
            if (!binding.phnNumberEdittext.getText().toString().trim().isEmpty()) {
                encryptedPhn = encrypt(binding.phnNumberEdittext.getText().toString().trim(), Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
                newUserData.put("phoneNumber", encryptedPhn);
            }
        }
        if (semesterChanged) {
            newUserData.put("semester", selectedSemester);
            newUserData.put("currentYear", (selectedSemester + 1) / 2);
        }
        if (_hasProfile) {
            newUserData.put("profile", _url);
            if (_url.equals("delete")) {
                newUserData.put("profile", null);
            }
        }
        if (!newUserData.isEmpty()) {
            if (!_hasProfile) {
                PrepNestUtil.showLoadingDialog(this, true);
            }
            assert auth.getCurrentUser() != null;
            users.child(auth.getCurrentUser().getUid()).updateChildren(newUserData).addOnCompleteListener(task -> {
                PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
                if (task.isSuccessful()) {
                    PrepNestUtil.showToast(UserprofileActivity.this, "Changed successfully!");
                } else {
                    PrepNestUtil.showToast(UserprofileActivity.this, "Error : Try again");
                }
            });
        }
    }

    public void deleteProfileFromStorage(final String _url) {
        PrepNestUtil.showLoadingDialog(this, true);
        Uri uri = Uri.parse(_url);
        String fullPath = uri.getPath();
        if (fullPath == null || !fullPath.contains("/o/")) {
            PrepNestUtil.showToast(UserprofileActivity.this, "Unknown error occurred.");
            PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
            return;
        }
        String encodedPath = fullPath.substring(fullPath.indexOf("/o/") + 3);
        if (encodedPath.contains("?")) {
            encodedPath = encodedPath.substring(0, encodedPath.indexOf("?"));
        }
        String decodedPath = Uri.decode(encodedPath);
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(decodedPath);
        ref.delete().addOnSuccessListener(aVoid -> updateUserData(true, "delete")).addOnFailureListener(exception -> {
            PrepNestUtil.showLoadingDialog(UserprofileActivity.this, false);
            PrepNestUtil.showToast(UserprofileActivity.this, "Failed to remove profile picture, try again later.");
        });
    }

    public void showUserProfileViewDialog() {
        AlertDialog profileViewDialog = new AlertDialog.Builder(UserprofileActivity.this).create();
        ProfileViewBinding dialogBinding = ProfileViewBinding.inflate(getLayoutInflater());

        Objects.requireNonNull(profileViewDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        profileViewDialog.setView(dialogBinding.getRoot());

        profileViewDialog.setCancelable(true);
        profileViewDialog.show();

        ViewGroup.LayoutParams paramsImageLayout = dialogBinding.imageLayout.getLayoutParams();
        if (paramsImageLayout == null) {
            paramsImageLayout = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        }

        int screenWidthPx = PrepNestUtil.getDisplayWidthPixels(UserprofileActivity.this);
        int sizePx = screenWidthPx - 100;

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(sizePx, sizePx);
        layoutParams.gravity = Gravity.CENTER;

        dialogBinding.imageLayout.setLayoutParams(layoutParams);


        profileFadeAnimation(dialogBinding.imageLayout);
        Glide.with(getApplicationContext())
                .load(Uri.parse(Objects.requireNonNull(userData.get("profile")).toString()))
                .circleCrop()
                .into(dialogBinding.photoView);
    }

    public void profileFadeAnimation(final View _view) {
        _view.animate().alpha(1f).setDuration(500).start();
    }

    @Override
    public void onDataReturned(HashMap<String, Object> updatedMap) {
        if (updatedMap.containsKey("type") && updatedMap.containsKey("id")) {
            final String type = Objects.requireNonNull(updatedMap.get("type")).toString();
            if (type.equals(ItemListSheetFragment.SheetType.SEMESTER.toString())) {
                selectedSemesterId = Objects.requireNonNull(updatedMap.get("id")).toString();
                selectedSemester = Integer.parseInt(selectedSemesterId);
                binding.semesterTxt.setText(PrepNestUtil.getFormattedNumber(selectedSemester).concat(" semester"));
                semesterChanged = true;
            }
        }
    }
}
