package com.cocode.prepnest;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.FileChipCardBinding;
import com.cocode.prepnest.databinding.GuidanceSheetLayoutBinding;
import com.cocode.prepnest.databinding.StatusViewBinding;
import com.cocode.prepnest.databinding.UploadresourceBinding;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class UploadresourceActivity extends AppCompatActivity implements ItemListSheetFragment.BottomSheetListener {

    private static final String CHANNEL_ID = "files_upload";
    private static final int NOTIFICATION_ID = 1010;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase fdb = FirebaseDatabase.getInstance();
    private final DatabaseReference requests = fdb.getReference("requests/newResourcesRequests");
    private final ArrayList<HashMap<String, Object>> filesMapList = new ArrayList<>();
    private final int IMAGE_PICK_CODE = 1102;
    private ArrayList<HashMap<String, Object>> itemsList = new ArrayList<>();
    private UploadresourceBinding binding;
    private HashMap<String, Object> appFirstVisit = new HashMap<>();
    private HashMap<String, Object> dataPayload = new HashMap<>();
    private HashMap<String, Object> userData = new HashMap<>();
    private FilesListAdapter filesListAdapter;
    private DatabaseReference users;
    private NetworkMonitor networkMonitor;
    private String jsonCourseData = null;
    private String selectedCourseId = "-1";
    private String selectedSemesterId = "-1";
    private String selectedSessionId = "-1";
    private int selectedSemester = 0;
    private SharedPreferences appFirstVisitSp;

    private static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "upload progress", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("notification for file upload");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = UploadresourceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        appFirstVisitSp = getSharedPreferences("appFirstVisit", Activity.MODE_PRIVATE);

        binding.backIcon.setOnClickListener(_view -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
        });

        binding.guidanceIcon.setOnClickListener(_view -> {
            appFirstVisit.put("uploadGuidance", false);
            showGuidanceSheet();
        });

        binding.subjectEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {
                PrepNestUtil.TransitionManager(binding.background, 150);
                if (binding.subjectErrorTxt.getVisibility() == View.VISIBLE) {
                    binding.subjectErrorTxt.setVisibility(View.GONE);
                }
            }
        });

        binding.sessionContainer.setOnClickListener(_view -> {
            Rect r = new Rect();
            binding.background.getWindowVisibleDisplayFrame(r);
            int screenHeight = binding.background.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > (screenHeight * 0.15d)) {
                PrepNestUtil.hideKeyboard(UploadresourceActivity.this);
            }

            createSessionsList();

            dataPayload = new HashMap<>();
            dataPayload.put("type", ItemListSheetFragment.SheetType.SESSION.toString());
            dataPayload.put("list", new ArrayList<>(itemsList));
            dataPayload.put("selectedItemId", selectedSessionId);

            final ItemListSheetFragment sessionSheet = ItemListSheetFragment.newInstance(dataPayload);
            sessionSheet.show(getSupportFragmentManager(), "sessionSheet");
        });

        binding.uploadBtn.setOnClickListener(_view -> {
            PrepNestUtil.TransitionManager(binding.background, 150);
            if (binding.subjectEdittext.getText().toString().trim().isEmpty()) {
                binding.subjectErrorTxt.setText("Enter the subject");
                binding.subjectErrorTxt.setVisibility(View.VISIBLE);
            } else {
                if (binding.subjectEdittext.getText().toString().trim().length() < 2) {
                    binding.subjectErrorTxt.setText("Enter the full subject name");
                    binding.subjectErrorTxt.setVisibility(View.VISIBLE);
                } else {
                    if (binding.courseTxt.getText().toString().trim().isEmpty() || binding.courseTxt.getText().toString().trim().equals("Course")) {
                        PrepNestUtil.showToast(UploadresourceActivity.this, "Select the course of your resource");
                    } else {
                        if (binding.semesterTxt.getText().toString().trim().isEmpty() || binding.semesterTxt.getText().toString().trim().equals("Semester")) {
                            PrepNestUtil.showToast(UploadresourceActivity.this, "Select the semester of your resource");
                        } else {
                            if (binding.sessionText.getText().toString().trim().equals("Session")) {
                                binding.sessionErrorTxt.setText("Select the session");
                                binding.sessionErrorTxt.setVisibility(View.VISIBLE);
                            } else {
                                if (filesMapList.isEmpty()) {
                                    PrepNestUtil.showToast(UploadresourceActivity.this, "Select at least one image file");
                                } else {
                                    showUploadConfirmationSheet();
                                }
                            }
                        }
                    }
                }
            }
        });

        binding.courseSelectContainer.setOnClickListener(_view -> {
            Rect r = new Rect();
            binding.background.getWindowVisibleDisplayFrame(r);
            int screenHeight = binding.background.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > (screenHeight * 0.15d)) {
                PrepNestUtil.hideKeyboard(UploadresourceActivity.this);
            }
            List<String> courseIds = getCourseIds();
            createCoursesList(courseIds);

            dataPayload = new HashMap<>();
            dataPayload.put("type", ItemListSheetFragment.SheetType.COURSE.toString());
            dataPayload.put("list", new ArrayList<>(itemsList));
            dataPayload.put("selectedItemId", selectedCourseId);

            final ItemListSheetFragment courseSheet = ItemListSheetFragment.newInstance(dataPayload);
            courseSheet.show(getSupportFragmentManager(), "courseSheet");
        });

        binding.semesterSelectContainer.setOnClickListener(_view -> {
            Rect r = new Rect();
            binding.background.getWindowVisibleDisplayFrame(r);
            int screenHeight = binding.background.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > (screenHeight * 0.15d)) {
                PrepNestUtil.hideKeyboard(UploadresourceActivity.this);
            }

            createSemestersList(selectedCourseId);

            dataPayload = new HashMap<>();
            dataPayload.put("type", ItemListSheetFragment.SheetType.SEMESTER.toString());
            dataPayload.put("list", new ArrayList<>(itemsList));
            dataPayload.put("selectedItemId", selectedSemesterId);

            final ItemListSheetFragment semSheet = ItemListSheetFragment.newInstance(dataPayload);
            semSheet.show(getSupportFragmentManager(), "semSheet");
        });

        binding.iconAddFiles.setOnClickListener(_view -> pickImageFromGallery());
    }

    private void initializeLogic() {
        networkMonitor = new NetworkMonitor(this);

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
                    PrepNestUtil.hideKeyboard(UploadresourceActivity.this);
                    view.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onActivityResult(int _requestCode, int _resultCode, Intent list) {
        super.onActivityResult(_requestCode, _resultCode, list);
        if ((_requestCode == IMAGE_PICK_CODE) && ((_resultCode == RESULT_OK) && list != null)) {
            Uri imageUri = list.getData();
            if (imageUri != null) {
                Uri compressedUri = compressImageAndSaveToCache(imageUri);
                if (compressedUri != null) {
                    {
                        HashMap<String, Object> _item = new HashMap<>();
                        _item.put("fileUri", compressedUri.toString());
                        filesMapList.add(_item);
                    }
                    filesListAdapter.notifyItemInserted(filesMapList.size() - 1);
                    filesListAdapter.notifyDataSetChanged();
                    double IMAGES_LIMIT = 4;
                    if (filesMapList.size() == IMAGES_LIMIT) {
                        addFileIconVisibility(false);
                    }
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle _savedInstanceState) {
        super.onPostCreate(_savedInstanceState);
        users = fdb.getReference("users");
        designUI();
        if (appFirstVisitSp.contains("appFirstVisit")) {
            appFirstVisit = new Gson().fromJson(appFirstVisitSp.getString("appFirstVisit", ""), new TypeToken<HashMap<String, Object>>() {
            }.getType());
        }
        showGuidanceSheet();
        attachAdapterToRecyclerView();
//        initializeRequiredValues();
        loadAllCoursesFromJson(this);
        getUserData();
        radioGroupsHandling();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (networkMonitor != null) {
            networkMonitor.register();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (networkMonitor != null) {
            networkMonitor.unregister();
        }
    }

    public void designUI() {
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }

    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
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

    public List<String> getCourseIds() {
        List<String> idsList = new ArrayList<>();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonCourseData);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Iterator<String> ids = jsonObject.keys();

        while (ids.hasNext()) {
            idsList.add(ids.next());
        }

        return idsList;
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

    public void createCoursesList(List<String> courseIds) {
        itemsList = new ArrayList<>();

        for (int index = 0; index < courseIds.size(); index++) {
            HashMap<String, Object> courseMap = new HashMap<>();
            courseMap.put("id", courseIds.get(index));
            courseMap.put("text", getCourseName(courseIds.get(index)));

            itemsList.add(courseMap);
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

    public void createSessionsList() {
        itemsList = new ArrayList<>();
        int currentYear = Year.now().getValue() + 1;
        for (int year = currentYear - 5; year <= currentYear - 1; year++) {
            {
                HashMap<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("id", String.valueOf(year));
                sessionMap.put("text", String.valueOf((long) (year)).concat("-".concat(String.valueOf((long) ((year + 1) - 2000)))));
                itemsList.add(sessionMap);
            }
        }
    }

    public String setCourseName(String courseId) {
        final String courseName = getCourseName(courseId);
        int spaceIndex = courseName.indexOf(" ");

        if (spaceIndex == -1) {
            return courseName;
        }

        return courseName.substring(0, spaceIndex);
    }

    public void showGuidanceSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog uploadGuidanceSheet;
        uploadGuidanceSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UploadresourceActivity.this);
        GuidanceSheetLayoutBinding sheetBinding = GuidanceSheetLayoutBinding.inflate(getLayoutInflater());

        uploadGuidanceSheet.setContentView(sheetBinding.getRoot());

        uploadGuidanceSheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetBinding.container.setBackground(gd);
        sheetBinding.title.setText(getString(R.string.upload_guidance_title));
        sheetBinding.subtext.setText(getString(R.string.upload_guidance_message));
        uploadGuidanceSheet.setCancelable(true);
        if (appFirstVisit.containsKey("uploadGuidance")) {
            if (!Boolean.parseBoolean(Objects.requireNonNull(appFirstVisit.get("uploadGuidance")).toString())) {
                appFirstVisit.put("uploadGuidance", true);
                appFirstVisitSp.edit().putString("appFirstVisit", new Gson().toJson(appFirstVisit)).apply();
                uploadGuidanceSheet.show();
            }
        } else {
            appFirstVisit.put("uploadGuidance", true);
            appFirstVisitSp.edit().putString("appFirstVisit", new Gson().toJson(appFirstVisit)).apply();
            uploadGuidanceSheet.show();
        }
    }

    public void getUserData() {
        PrepNestUtil.showLoadingDialog(this, true);
        assert auth.getCurrentUser() != null;
        users.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userData = dataSnapshot.getValue(new GenericTypeIndicator<>() {
                });

                if (userData != null) {
                    loadUserDataToUI();
                } else {
                    PrepNestUtil.showToast(UploadresourceActivity.this, "Failed to load data, please login again!");
                    auth.signOut();
                    finishAffinity();
                }
                PrepNestUtil.showLoadingDialog(UploadresourceActivity.this, false);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

                if (!isFinishing() && !isDestroyed()) {
                    PrepNestUtil.showLoadingDialog(UploadresourceActivity.this, false);
                    PrepNestUtil.showToast(UploadresourceActivity.this, "Please login again!");
                    auth.signOut();
                    finishAffinity();
                }

            }
        });
    }

    public void loadUserDataToUI() {
        if (userData.containsKey("courseId")) {
            selectedCourseId = Objects.requireNonNull(userData.get("courseId")).toString();
            userData.put("courseDuration", getCourseDuration(selectedCourseId));
            String courseName = Objects.requireNonNull(getCourseName(selectedCourseId));
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
            createSemestersList(selectedCourseId);
        }
    }

    public void radioGroupsHandling() {
        binding.pyqTypeRadiogroup.setOnCheckedChangeListener((group, checkedId) -> {
            PrepNestUtil.TransitionManager(binding.background, 250);

            if (checkedId == binding.semesterPyqRadiobutton.getId()) {
                binding.midtermTypeTitle.setVisibility(View.GONE);
                binding.midtermTypeRadiogroup.setVisibility(View.GONE);

            } else if (checkedId == binding.midtermPyqRadiobutton.getId()) {

                binding.midtermTypeTitle.setVisibility(View.VISIBLE);
                binding.midtermTypeRadiogroup.setVisibility(View.VISIBLE);
            }
        });

		/*
binding.resourceTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		PrepNestUtil.TransitionManager(binding.background, 250);

		if (binding.iconAddFiles.getVisibility() == View.INVISIBLE) {
			binding.iconAddFiles.setVisibility(View.VISIBLE);
			binding.iconAddFiles.setEnabled(true);
		}
        filesListAdapter.clearList();

		if (checkedId == binding.pyqRadiobutton.getId()) {
			binding.sessionContainer.setVisibility(View.VISIBLE);
			binding.pyqTypeTitle.setVisibility(View.VISIBLE);
			binding.pyqTypeRadiogroup.setVisibility(View.VISIBLE);
			binding.resourceTitleEdittext.setVisibility(View.GONE);
			if (binding.midtermPyqRadiobutton.isChecked()) {
				binding.midtermTypeTitle.setVisibility(View.VISIBLE);
				binding.midtermTypeRadiogroup.setVisibility(View.VISIBLE);
			}

		} else if (checkedId == binding.notesRadiobutton.getId()) {

			binding.sessionContainer.setVisibility(View.GONE);
			binding.sessionErrorTxt.setVisibility(View.GONE);
			binding.pyqTypeTitle.setVisibility(View.GONE);
			binding.pyqTypeRadiogroup.setVisibility(View.GONE);
			binding.resourceTitleEdittext.setVisibility(View.VISIBLE);
			binding.midtermTypeTitle.setVisibility(View.GONE);
			binding.midtermTypeRadiogroup.setVisibility(View.GONE);
		}
	}
});

*/
    }


    public void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, IMAGE_PICK_CODE);
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

    private Uri compressImageAndSaveToCache(final Uri sourceUri) {
        try {
            // Decode bitmap using ImageDecoder (Android 10+)
            ImageDecoder.Source source =
                    ImageDecoder.createSource(getContentResolver(), sourceUri);
            Bitmap bitmap = ImageDecoder.decodeBitmap(source);

            // Compress to JPEG (70% quality)
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outStream);
            byte[] imageData = outStream.toByteArray();

            // Create a file inside app cache directory
            File cacheDir = getCacheDir();
            String fileName = getFileNameFromURI(sourceUri);
            if (fileName == null) return null;

            File outputFile = new File(cacheDir, fileName);

            // Write compressed bytes to file
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(imageData);
            fos.flush();
            fos.close();

            // Return FileProvider URI for the compressed file
            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    outputFile
            );

        } catch (IOException e) {
            PrepNestUtil.showToast(UploadresourceActivity.this, "Failed to process image, try again");
            return null;
        }
    }

    public void attachAdapterToRecyclerView() {
        filesListAdapter = new FilesListAdapter(filesMapList);
        binding.filesRecyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.filesRecyclerview.setAdapter(filesListAdapter);
        binding.filesRecyclerview.setItemAnimator(new DefaultItemAnimator());
        int margin20 = (int) convertToDp(20);
        int margin10 = (int) convertToDp(10);

        binding.filesRecyclerview.addItemDecoration(new HorizontalMarginItemDecoration(margin20, margin20, margin10));
    }

    public void addFileIconVisibility(final boolean _isVisible) {
        PrepNestUtil.TransitionManager(binding.background, 150);
        if (_isVisible) {
            binding.iconAddFiles.setVisibility(View.VISIBLE);
            binding.iconAddFiles.setEnabled(true);
        } else {
            binding.iconAddFiles.setVisibility(View.INVISIBLE);
            binding.iconAddFiles.setEnabled(false);
        }
    }


    public String capitalizeString(final String _input) {
        if (_input == null || _input.isEmpty()) return _input;
        return _input.substring(0, 1).toUpperCase() + _input.substring(1).toLowerCase();

    }

    public void showUploadConfirmationSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog confirmationSheet;
        confirmationSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UploadresourceActivity.this);
        StatusViewBinding sheetBinding = StatusViewBinding.inflate(getLayoutInflater());

        confirmationSheet.setContentView(sheetBinding.getRoot());

        confirmationSheet.setOnShowListener(dialog -> {
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
        sheetBinding.title.setTextSize(16);
        sheetBinding.subtext.setTextSize(11);
//        sheetBinding.imageContainer.setBackground(new GradientDrawable() {
//            public GradientDrawable getIns(int a, int b) {
//                this.setCornerRadius(a);
//                this.setColor(b);
//                return this;
//            }
//        }.getIns((int) 360, 0xFFFAFAFA));
        sheetBinding.image.setImageResource(R.drawable.icon_upload_3d);
        sheetBinding.title.setText("Confirmation!");
        sheetBinding.subtext.setText("Please review all your details carefully before uploading. Once submitted, you won’t be able to make any changes.");
        PrepNestUtil.roundViewWithRipple(sheetBinding.btnOk, "#000000", 15, 0, "#000000", "#212121");
        PrepNestUtil.roundViewWithRipple(sheetBinding.btnCancel, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
        sheetBinding.btnOkTxt.setText("Confirm");
        sheetBinding.btnCancelTxt.setText("Cancel");
        sheetBinding.btnOk.setOnClickListener(_view -> {
            uploadAllData();
            confirmationSheet.dismiss();
        });
        sheetBinding.btnCancel.setOnClickListener(_view -> confirmationSheet.dismiss());
        confirmationSheet.setCancelable(true);
        confirmationSheet.show();
    }

    public void uploadAllData() {
        List<String> URIs = new ArrayList<>();
        assert auth.getCurrentUser() != null;
        DatabaseReference requestRef = requests.child(auth.getCurrentUser().getUid());
        for (HashMap<String, Object> uriMap : filesMapList) {
            URIs.add(Objects.requireNonNull(uriMap.get("fileUri")).toString());
        }

        PrepNestUtil.showLoadingDialog(this, true);
        FileUploader.uploadFilesToStorage(URIs, this, new FileUploader.UploadCallback() {
            @Override
            public void onSuccess(List<String> downloadURLs, List<StorageReference> uploadedFileRefs) {


                HashMap<String, Object> addResource = new HashMap<>();
                String resourceID = requestRef.push().getKey();
                addResource.put("subject", binding.subjectEdittext.getText().toString().trim());
                addResource.put("courseId", selectedCourseId);
                addResource.put("semester", selectedSemester);
                addResource.put("uploaderUid", auth.getCurrentUser().getUid());
                addResource.put("dateOfUpload", System.currentTimeMillis());
                addResource.put("resourceId", resourceID);
                addResource.put("resourceUrls", new Gson().toJson(downloadURLs));
                addResource.put("session", binding.sessionText.getText().toString().trim());
                addResource.put("type", "paper");
                if (binding.semesterPyqRadiobutton.isChecked()) {
                    addResource.put("subtype", "semester");
                } else {
                    addResource.put("subtype", "midterm");
                }
                String midtermType = "";
                if (binding.midterm1Radiobutton.isChecked()) {
                    midtermType = "1st";
                } else {
                    if (binding.midterm2Radiobutton.isChecked()) {
                        midtermType = "2nd";
                    } else {
                        if (binding.midterm3Radiobutton.isChecked()) {
                            midtermType = "3rd";
                        }
                    }
                }
                String resourceTitle = capitalizeString(Objects.requireNonNull(addResource.get("subtype")).toString()).concat(" ");
                if (Objects.requireNonNull(addResource.get("subtype")).toString().equals("midterm")) {
                    resourceTitle += midtermType + " ";
                }
                resourceTitle += "Paper";
                addResource.put("resourceTitle", resourceTitle);
                assert resourceID != null;
                requestRef.child(resourceID).setValue(addResource).addOnCompleteListener(uploadTask -> {
                    if (uploadTask.isSuccessful()) {
                        PrepNestUtil.showLoadingDialog(UploadresourceActivity.this, false);
                        PrepNestUtil.showToast(UploadresourceActivity.this, "Successfully uploaded and sent for verification!");
                        binding.backIcon.performClick();
                    } else {
                        for (StorageReference ref : uploadedFileRefs) {
                            ref.delete();
                        }
                        binding.backIcon.performClick();
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                PrepNestUtil.showLoadingDialog(UploadresourceActivity.this, false);
                PrepNestUtil.showToast(UploadresourceActivity.this, "Upload failed: " + e.getMessage());
            }
        });

    }

    @Override
    public void onDataReturned(HashMap<String, Object> updatedMap) {
        if (updatedMap.containsKey("type") && updatedMap.containsKey("id") && updatedMap.containsKey("text")) {
            final String type = Objects.requireNonNull(updatedMap.get("type")).toString();
            if (type.equals(ItemListSheetFragment.SheetType.COURSE.toString())) {
                selectedCourseId = Objects.requireNonNull(updatedMap.get("id")).toString();
                selectedSemesterId = "-1";
                binding.courseTxt.setText(setCourseName(selectedCourseId));
            } else if (type.equals(ItemListSheetFragment.SheetType.SEMESTER.toString())) {
                selectedSemesterId = Objects.requireNonNull(updatedMap.get("id")).toString();
                selectedSemester = Integer.parseInt(selectedSemesterId);
                binding.semesterTxt.setText(PrepNestUtil.getFormattedNumber(selectedSemester).concat(" semester"));
            } else if (type.equals(ItemListSheetFragment.SheetType.SESSION.toString())) {
                selectedSessionId = Objects.requireNonNull(updatedMap.get("id")).toString();
                binding.sessionText.setText(Objects.requireNonNull(updatedMap.get("text")).toString());
                if (binding.sessionErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.background, 150);
                    binding.sessionErrorTxt.setVisibility(View.GONE);
                }
            }
        }
    }

    public static class FileUploader {
        public static void uploadFilesToStorage(List<String> fileURIStrings, Context context, UploadCallback callback) {
            FirebaseStorage _firebase_storage_ = FirebaseStorage.getInstance();
            StorageReference storageRef = _firebase_storage_.getReference("resources");
            List<String> downloadURLs = new ArrayList<>();
            List<StorageReference> uploadedFileRefs = new ArrayList<>();

            int totalFilesSize = fileURIStrings.size();
            AtomicInteger uploadedCount = new AtomicInteger(0);
            AtomicBoolean failed = new AtomicBoolean(false);

            // Creating notification channel
            createNotificationChannel(context);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Uploading")
                    .setContentText("Uploading is in progress...")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setProgress(100, 0, false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            100
                    );
                }
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build());

            for (String uriString : fileURIStrings) {
                Uri fileUri = Uri.parse(uriString);

                String fileName = System.currentTimeMillis() + "_" + fileUri.getLastPathSegment();
                StorageReference fileRef = storageRef.child(fileName);

                fileRef.putFile(fileUri).addOnProgressListener(snapshot -> {
                            long bytesTransferred = snapshot.getBytesTransferred();
                            long totalBytes = snapshot.getTotalByteCount();
                            double fileProgress = (100.0 * bytesTransferred / totalBytes);
                            int overallProgress = (int) ((uploadedCount.get() * 100.0 + fileProgress) / totalFilesSize);
                            builder.setProgress(100, overallProgress, false);
                            notificationManager.notify(NOTIFICATION_ID, builder.build());
                        })
                        .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            uploadedFileRefs.add(fileRef);
                            downloadURLs.add(downloadUri.toString());
                            if (uploadedCount.incrementAndGet() == totalFilesSize && !failed.get()) {
                                builder.setContentText("Successfully uploaded")
                                        .setOngoing(false)
                                        .setProgress(0, 0, false);
                                notificationManager.notify(NOTIFICATION_ID, builder.build());
                                callback.onSuccess(downloadURLs, uploadedFileRefs);
                            }
                        }))
                        .addOnFailureListener(e -> {
                            if (!failed.getAndSet(true)) {
                                // Delete previously uploaded files
                                for (StorageReference ref : uploadedFileRefs) {
                                    ref.delete();
                                }

                                // Update notification to failed
                                builder.setContentTitle("Upload failed")
                                        .setContentText("One or more files failed to upload")
                                        .setOngoing(false)
                                        .setProgress(0, 0, false);
                                notificationManager.notify(NOTIFICATION_ID, builder.build());
                                callback.onFailure(e);
                            }
                        });
            }
        }

        public interface UploadCallback {
            void onSuccess(List<String> downloadURLs, List<StorageReference> uploadedFileRefs);

            void onFailure(Exception e);
        }
    }

    public class FilesListAdapter extends RecyclerView.Adapter<FilesListAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> list;
        private int _lastPosition = -1; // Used to track animations

        public FilesListAdapter(ArrayList<HashMap<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FileChipCardBinding fileChipCardBinding = FileChipCardBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );

            return new ViewHolder(fileChipCardBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            PrepNestUtil.roundViewWithRipple(holder.binding.container, "#F5F5F5", 360, 0, "#000000", "#E0E0E0");

            if (list.get(position).containsKey("fileUri")) {
                String fileUri = Objects.requireNonNull(list.get(position).get("fileUri")).toString();

                holder.binding.fileTitle.setText(getFileNameFromURI(Uri.parse(fileUri)));

                if (fileUri.length() > 12) {
                    Uri uri = Uri.parse(fileUri);
                    holder.binding.fileTitle.setText(getFileNameFromURI(uri).substring(0, 12).concat("..."));
                }
            } else {
                holder.binding.fileTitle.setText("File");
            }


            setAnimation(holder.itemView, position);
            holder.binding.iconRemove.setOnClickListener(_view1 -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    list.remove(currentPos);
                    notifyItemRemoved(currentPos);
                    notifyItemRangeChanged(currentPos, list.size());
                    addFileIconVisibility(true);
                }
            });

            holder.binding.container.setOnClickListener(_view2 -> {
                Intent toImageView = new Intent();
                toImageView.setClass(UploadresourceActivity.this, ImageviewActivity.class);
                toImageView.putExtra("uri", Objects.requireNonNull(list.get(position).get("fileUri")).toString());
                toImageView.putExtra("id", "null");
                startActivity(toImageView);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        // Custom animation method
        private void setAnimation(View viewToAnimate, int position) {
            if (position > _lastPosition) {
                Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), android.R.anim.fade_in);
                viewToAnimate.startAnimation(animation);
                _lastPosition = position;
            }
        }

        // Optional: clear animation on view detached (prevents flickering)
        @Override
        public void onViewDetachedFromWindow(ViewHolder holder) {
            holder.itemView.clearAnimation();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            FileChipCardBinding binding;

            public ViewHolder(FileChipCardBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
