package com.cocode.prepnest;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.DetailsSelectSheetBinding;
import com.cocode.prepnest.databinding.GuidanceSheetLayoutBinding;
import com.cocode.prepnest.databinding.SheetSingleItemSelectBinding;
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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class UploadresourceActivity extends AppCompatActivity {

    private UploadresourceBinding binding;
    private HashMap<String, Object> features_visit_map = new HashMap<>();
    private HashMap<String, Object> userData = new HashMap<>();
    private Files_recyclerviewAdapter filesListAdapter;
    private HashMap<String, Object> reqValues = new HashMap<>();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseDatabase fdb = FirebaseDatabase.getInstance();
    private DatabaseReference users;
    private NetworkMonitor networkMonitor;
    private static final String CHANNEL_ID = "files_upload";
    private String jsonCourseData = "";
    private static final int NOTIFICATION_ID = 1010;
    private DatabaseReference requests = fdb.getReference("requests/new_resources_requests");

    private ArrayList<String> keysList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> filesMapList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> dataList = new ArrayList<>();

    private SharedPreferences features_visit;

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
        features_visit = getSharedPreferences("features visit", Activity.MODE_PRIVATE);

        binding.backIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                finish();
                overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
            }
        });

        binding.guidanceIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                features_visit_map.put("upload guidance", "false");
                showGuidanceSheet();
            }
        });

        binding.subjectEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                final String _charSeq = _param1.toString();
                PrepNestUtil.TransitionManager(binding.background, 150);
                if (binding.subjectErrorTxt.getVisibility() == View.VISIBLE) {
                    binding.subjectErrorTxt.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {

            }
        });

        binding.sessionContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                Rect r = new Rect();
                binding.background.getWindowVisibleDisplayFrame(r);
                int screenheight = binding.background.getRootView().getHeight();
                int keypadheight = screenheight - r.bottom;
                if (keypadheight > (screenheight * 0.15d)) {
                    PrepNestUtil.hideKeyboard(UploadresourceActivity.this);
                }
                com.google.android.material.bottomsheet.BottomSheetDialog details_sheet;
                details_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UploadresourceActivity.this);
                DetailsSelectSheetBinding sheetbinding = DetailsSelectSheetBinding.inflate(getLayoutInflater());

                details_sheet.setContentView(sheetbinding.getRoot());

                details_sheet.setOnShowListener(dialog -> {
                    BottomSheetDialog d = (BottomSheetDialog) dialog;
                    View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    if (bottomSheet != null) {
                        bottomSheet.setBackgroundResource(android.R.color.transparent);
                    }
                });


                LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) sheetbinding.container.getLayoutParams();
                paramscontainer.width = LinearLayout.LayoutParams.MATCH_PARENT;
                paramscontainer.height = LinearLayout.LayoutParams.WRAP_CONTENT;

                sheetbinding.container.setLayoutParams(paramscontainer);
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#FFFFFF"));
                gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
                sheetbinding.container.setBackground(gd);
                sheetbinding.searchContainer.setVisibility(View.GONE);
                sheetbinding.itemsList.setHorizontalScrollBarEnabled(false);
                sheetbinding.itemsList.setVerticalScrollBarEnabled(false);
                dataList = new Gson().fromJson(reqValues.get("sessions_json").toString(), new TypeToken<ArrayList<HashMap<String, Object>>>() {
                }.getType());
                sheetbinding.itemsList.setAdapter(new ItemsAdapter(dataList));

                ((BaseAdapter) sheetbinding.itemsList.getAdapter()).notifyDataSetChanged();
                sheetbinding.itemsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
                        final int _position = _param3;
                        binding.sessionText.setText(((HashMap<String, Object>) _param1.getItemAtPosition(_position)).get("session").toString());
                        if (binding.sessionErrorTxt.getVisibility() == View.VISIBLE) {
                            PrepNestUtil.TransitionManager(binding.background, 150);
                            binding.sessionErrorTxt.setVisibility(View.GONE);
                        }
                        details_sheet.dismiss();
                    }
                });
                details_sheet.setCancelable(true);
                details_sheet.show();
            }
        });

        binding.uploadBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                PrepNestUtil.TransitionManager(binding.background, 150);
                if (binding.subjectEdittext.getText().toString().trim().equals("")) {
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
                                    if (filesMapList.size() <= 0) {
                                        PrepNestUtil.showToast(UploadresourceActivity.this, "Select atleast one image file");
                                    } else {
                                        showUploadConfirmationSheet();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        binding.courseSelectContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                Rect r = new Rect();
                binding.background.getWindowVisibleDisplayFrame(r);
                int screenheight = binding.background.getRootView().getHeight();
                int keypadheight = screenheight - r.bottom;
                if (keypadheight > (screenheight * 0.15d)) {
                    PrepNestUtil.hideKeyboard(UploadresourceActivity.this);
                }
                com.google.android.material.bottomsheet.BottomSheetDialog details_sheet;
                details_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UploadresourceActivity.this);
                DetailsSelectSheetBinding sheetbinding = DetailsSelectSheetBinding.inflate(getLayoutInflater());

                details_sheet.setContentView(sheetbinding.getRoot());

                details_sheet.setOnShowListener(dialog -> {
                    BottomSheetDialog d = (BottomSheetDialog) dialog;
                    View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    if (bottomSheet != null) {
                        bottomSheet.setBackgroundResource(android.R.color.transparent);
                    }
                });

                LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) sheetbinding.container.getLayoutParams();
                paramscontainer.width = LinearLayout.LayoutParams.MATCH_PARENT;
                paramscontainer.height = (int) convertToDp(500);

                sheetbinding.container.setLayoutParams(paramscontainer);
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#FFFFFF"));
                gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
                sheetbinding.container.setBackground(gd);
                sheetbinding.searchContainer.setVisibility(View.VISIBLE);
                sheetbinding.itemsList.setHorizontalScrollBarEnabled(false);
                sheetbinding.itemsList.setVerticalScrollBarEnabled(false);
                sheetbinding.searchEdittext.setFocusableInTouchMode(true);
                sheetbinding.searchEdittext.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                        final String _charSeq = _param1.toString();
                        dataList = new Gson().fromJson(reqValues.get("courses_json").toString(), new TypeToken<ArrayList<HashMap<String, Object>>>() {
                        }.getType());
                        int lengthOfCoursesList = dataList.size();
                        int index = lengthOfCoursesList - 1;
                        for (int _repeat34 = 0; _repeat34 < (int) (lengthOfCoursesList); _repeat34++) {
                            if (!(_charSeq.length() > dataList.get((int) index).get("name").toString().length()) && dataList.get((int) index).get("name").toString().toLowerCase().contains(_charSeq.toLowerCase())) {

                            } else {
                                dataList.remove((int) (index));
                            }
                            index--;
                        }
                        sheetbinding.itemsList.setAdapter(new ItemsAdapter(dataList));

                        ((BaseAdapter) sheetbinding.itemsList.getAdapter()).notifyDataSetChanged();
                    }

                    @Override
                    public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

                    }

                    @Override
                    public void afterTextChanged(Editable _param1) {

                    }
                });
                dataList = new Gson().fromJson(reqValues.get("courses_json").toString(), new TypeToken<ArrayList<HashMap<String, Object>>>() {
                }.getType());
                sheetbinding.itemsList.setAdapter(new ItemsAdapter(dataList));

                ((BaseAdapter) sheetbinding.itemsList.getAdapter()).notifyDataSetChanged();
                sheetbinding.itemsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
                        final int _position = _param3;
                        reqValues.put("selected_course_id", keysList.get((int) (_position)));
                        int spaceIndex = dataList.get((int) _position).get("name").toString().indexOf(" ");
                        if (spaceIndex != -1) {
                            binding.courseTxt.setText(dataList.get((int) _position).get("name").toString().substring((int) (0), spaceIndex));
                        } else {
                            binding.courseTxt.setText(dataList.get((int) _position).get("name").toString());
                        }
                        binding.semesterTxt.setText("Semester");
                        reqValues.put("selected_semester", 0);
                        addTotalSemesters(Double.parseDouble(fetchCourseData(keysList.get((int) (_position))).get("duration").toString()));
                        details_sheet.dismiss();
                    }
                });
                details_sheet.setCancelable(true);
                details_sheet.show();
            }
        });

        binding.semesterSelectContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                Rect r = new Rect();
                binding.background.getWindowVisibleDisplayFrame(r);
                int screenheight = binding.background.getRootView().getHeight();
                int keypadheight = screenheight - r.bottom;
                if (keypadheight > (screenheight * 0.15d)) {
                    PrepNestUtil.hideKeyboard(UploadresourceActivity.this);
                }
                com.google.android.material.bottomsheet.BottomSheetDialog details_sheet;
                details_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UploadresourceActivity.this);
                DetailsSelectSheetBinding sheetbinding = DetailsSelectSheetBinding.inflate(getLayoutInflater());

                details_sheet.setContentView(sheetbinding.getRoot());

                details_sheet.setOnShowListener(dialog -> {
                    BottomSheetDialog d = (BottomSheetDialog) dialog;
                    View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    if (bottomSheet != null) {
                        bottomSheet.setBackgroundResource(android.R.color.transparent);
                    }
                });

                LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) sheetbinding.container.getLayoutParams();
                paramscontainer.width = LinearLayout.LayoutParams.MATCH_PARENT;
                paramscontainer.height = LinearLayout.LayoutParams.WRAP_CONTENT;

                sheetbinding.container.setLayoutParams(paramscontainer);
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#FFFFFF"));
                gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
                sheetbinding.container.setBackground(gd);
                sheetbinding.searchContainer.setVisibility(View.GONE);
                sheetbinding.itemsList.setVerticalScrollBarEnabled(false);
                sheetbinding.itemsList.setVerticalScrollBarEnabled(false);
                dataList = new Gson().fromJson(reqValues.get("semesters_json").toString(), new TypeToken<ArrayList<HashMap<String, Object>>>() {
                }.getType());
                sheetbinding.itemsList.setAdapter(new ItemsAdapter(dataList));

                ((BaseAdapter) sheetbinding.itemsList.getAdapter()).notifyDataSetChanged();
                sheetbinding.itemsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> _param1, View _param2, int _param3, long _param4) {
                        final int _position = _param3;
                        reqValues.put("selected_semester", Double.parseDouble(dataList.get((int) _position).get("semester").toString()));
                        binding.semesterTxt.setText(getFormattedNumber(Double.parseDouble(dataList.get((int) _position).get("semester").toString())).concat(" semester"));
                        details_sheet.dismiss();
                    }
                });
                details_sheet.setCancelable(true);
                details_sheet.show();
            }
        });

        binding.iconAddFiles.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                pickImageFromGallery();
            }
        });
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
    protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
        super.onActivityResult(_requestCode, _resultCode, _data);
        if ((_requestCode == (int) reqValues.get("IMAGE_PICK_CODE")) && ((_resultCode == RESULT_OK) && _data != null)) {
            Uri imageUri = _data.getData();
            if (imageUri != null) {
                Uri compressedUri = compressImageAndSaveToCache(imageUri);
                if (compressedUri != null) {
                    {
                        HashMap<String, Object> _item = new HashMap<>();
                        _item.put("file uri", compressedUri.toString());
                        filesMapList.add(_item);
                    }
                    filesListAdapter.notifyItemInserted(filesMapList.size() - 1);
                    filesListAdapter.notifyDataSetChanged();
                    if (filesMapList.size() == Double.parseDouble(reqValues.get("IMAGES_LIMIT").toString())) {
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
        if (features_visit.contains("features visit")) {
            features_visit_map = new Gson().fromJson(features_visit.getString("features visit", ""), new TypeToken<HashMap<String, Object>>() {
            }.getType());
        }
        showGuidanceSheet();
        attachAdapterToRecyclerView();
        initializeRequiredValues();
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
    }


    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }


    public HashMap<String, Object> fetchCourseData(final String _id) {
        HashMap<String, Object> courseData = new HashMap<>();
        if (jsonCourseData.isEmpty()) {
            try {
                InputStream is = getAssets().open("courses.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                jsonCourseData = new String(buffer, "UTF-8");
            } catch (IOException ex) {
                PrepNestUtil.showToast(UploadresourceActivity.this, ex.toString());
                return courseData;
            }
        }
        try {
            JSONObject allCourses = new JSONObject(jsonCourseData);
            JSONObject course = allCourses.getJSONObject(_id);
            courseData = new Gson().fromJson(course.toString(), new TypeToken<HashMap<String, Object>>() {
            }.getType());
            return courseData;
        } catch (Exception e) {
            PrepNestUtil.showToast(UploadresourceActivity.this, e.toString());
            return courseData;
        }
    }


    public void showGuidanceSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog upload_guidance_sheet;
        upload_guidance_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UploadresourceActivity.this);
        GuidanceSheetLayoutBinding sheetbinding = GuidanceSheetLayoutBinding.inflate(getLayoutInflater());

        upload_guidance_sheet.setContentView(sheetbinding.getRoot());

        upload_guidance_sheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetbinding.container.setBackground(gd);
        sheetbinding.title.setText(getString(R.string.upload_guidance_title));
        sheetbinding.subtext.setText(getString(R.string.upload_guidance_message));
        upload_guidance_sheet.setCancelable(true);
        if (features_visit_map.containsKey("upload guidance")) {
            if (features_visit_map.get("upload guidance").toString().equals("false")) {
                features_visit_map.put("upload guidance", "true");
                features_visit.edit().putString("features visit", new Gson().toJson(features_visit_map)).commit();
                upload_guidance_sheet.show();
            }
        } else {
            features_visit_map.put("upload guidance", "true");
            features_visit.edit().putString("features visit", new Gson().toJson(features_visit_map)).commit();
            upload_guidance_sheet.show();
        }
    }


    public void getUserData() {
        PrepNestUtil.showLoadingDialog(this, true);
        users.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userData = dataSnapshot.getValue(new GenericTypeIndicator<HashMap<String, Object>>() {
                });

                if (userData != null) {
                    if (userData.containsKey("course id")) {
                        userData.put("course duration", Double.parseDouble(fetchCourseData(userData.get("course id").toString()).get("duration").toString()));
                        binding.courseTxt.setText(fetchCourseData(userData.get("course id").toString()).get("name").toString().substring((int) (0), (int) (fetchCourseData(userData.get("course id").toString()).get("name").toString().indexOf(" "))));
                        reqValues.put("selected_course_id", userData.get("course id").toString());
                    } else {
                        userData.put("course duration", null);
                        binding.courseTxt.setText("Course");
                    }
                    if (userData.containsKey("semester")) {
                        reqValues.put("selected_semester", ((Number) userData.get("semester")).doubleValue());
                        binding.semesterTxt.setText(getFormattedNumber(((Number) userData.get("semester")).doubleValue()).concat(" semester"));
                    } else {
                        binding.semesterTxt.setText("Semester");
                    }
                    if ((userData.get("course duration") == null)) {
                        addTotalSemesters(5.5d);
                    } else {
                        addTotalSemesters(((Number) userData.get("course duration")).doubleValue());
                    }
                    loadCourses();
                } else {
                    PrepNestUtil.showToast(UploadresourceActivity.this, "Failed to load data, please login again!");
                    auth.signOut();
                    finishAffinity();
                }
                PrepNestUtil.showLoadingDialog(UploadresourceActivity.this, false);
                addSessions();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                if (!isFinishing() && !isDestroyed()) {
                    PrepNestUtil.showLoadingDialog(UploadresourceActivity.this, false);
                    PrepNestUtil.showToast(UploadresourceActivity.this, "Please login again!");
                    auth.signOut();
                    finishAffinity();
                }

            }
        });
    }


    public class ItemsAdapter extends BaseAdapter {

        ArrayList<HashMap<String, Object>> _data;

        public ItemsAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public int getCount() {
            return _data.size();
        }

        @Override
        public HashMap<String, Object> getItem(int _index) {
            return _data.get(_index);
        }

        @Override
        public long getItemId(int _index) {
            return _index;
        }

        @Override
        public View getView(final int _position, View _v, ViewGroup _container) {
            SheetSingleItemSelectBinding binding = SheetSingleItemSelectBinding.inflate(getLayoutInflater());
            View _view = binding.getRoot();
            if (_data.get((int) _position).containsKey("name")) {
                binding.text.setText(_data.get((int) _position).get("name").toString());
            }
            if (_data.get((int) _position).containsKey("semester")) {
                binding.text.setText(getFormattedNumber(Double.parseDouble(_data.get((int) _position).get("semester").toString())).concat(" semester"));
            }
            if (_data.get((int) _position).containsKey("session")) {
                binding.text.setText(_data.get((int) _position).get("session").toString());
            }
            return _view;
        }
    }


    public void loadCourses() {
        ArrayList<HashMap<String, Object>> _temp = new ArrayList<>();
        try {
            JSONObject allCourses = new JSONObject(jsonCourseData);
            Iterator<String> keys = allCourses.keys();
            while (keys.hasNext()) {
                keysList.add(keys.next());
            }
            for (int pos = 0; pos < keysList.size(); pos++) {
                JSONObject course = allCourses.getJSONObject(keysList.get(pos));
                {
                    HashMap<String, Object> _item = new HashMap<>();
                    _item.put("name", course.getString("name"));
                    _temp.add(_item);
                }
                _temp.get((int) _temp.size() - 1).put("duration", String.valueOf(course.getInt("duration")));
            }
            reqValues.put("courses_json", new Gson().toJson(_temp));
        } catch (Exception ignored) {

        }
    }


    public void addSessions() {
        ArrayList<HashMap<String, Object>> _temp = new ArrayList<>();
        int currentYear = Year.now().getValue() + 1;
        for (int year = currentYear - 5; year <= currentYear - 1; year++) {
            {
                HashMap<String, Object> _item = new HashMap<>();
                _item.put("session", String.valueOf((long) (year)).concat("-".concat(String.valueOf((long) ((year + 1) - 2000)))));
                _temp.add(_item);
            }
        }
        reqValues.put("sessions_json", new Gson().toJson(_temp));
    }


    public void radioGroupsHandling() {
        binding.pyqTypeRadiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull RadioGroup group, int checkedId) {
                PrepNestUtil.TransitionManager(binding.background, 250);

                if (checkedId == binding.semesterPyqRadiobutton.getId()) {
                    binding.midtermTypeTitle.setVisibility(View.GONE);
                    binding.midtermTypeRadiogroup.setVisibility(View.GONE);

                } else if (checkedId == binding.midtermPyqRadiobutton.getId()) {

                    binding.midtermTypeTitle.setVisibility(View.VISIBLE);
                    binding.midtermTypeRadiogroup.setVisibility(View.VISIBLE);
                }
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
        startActivityForResult(intent, (int) reqValues.get("IMAGE_PICK_CODE"));
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
            // Get bitmap from uri
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), sourceUri);
            // Compress the bitmap to JPEG with 70% quality
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outStream);
            byte[] imageData = outStream.toByteArray();
            //Create a file in cache directory
            File cacheDir = getCacheDir();
            String fileName = getFileNameFromURI(sourceUri);
            if (fileName == null) return null;
            File outputFile = new File(cacheDir, fileName);
            // Writes compressed data to file
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(imageData);
            fos.flush();
            fos.close();
            // Return the file uri
            return FileProvider.getUriForFile(
                    this,
                    UploadresourceActivity.this.getPackageName() + ".provider",
                    outputFile
            );
        } catch (IOException e) {
            PrepNestUtil.showToast(UploadresourceActivity.this, "Failed to process image, try again");
            return null;
        }
    }

    public class Files_recyclerviewAdapter extends RecyclerView.Adapter<Files_recyclerviewAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;
        private int _lastPosition = -1; // Used to track animations

        public Files_recyclerviewAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = LayoutInflater.from(parent.getContext());
            View _v = _inflater.inflate(R.layout.file_chip_card, parent, false);

            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            final LinearLayout container = _view.findViewById(R.id.container);
            final TextView file_title = _view.findViewById(R.id.file_title);
            final ImageView icon_remove = _view.findViewById(R.id.icon_remove);
            PrepNestUtil.roundViewWithRipple(container, "#F5F5F5", 360, 0, "#000000", "#E0E0E0");

            if (_data.get((int) _position).containsKey("file uri")) {
                String fileUri = _data.get((int) _position).get("file uri").toString();

                file_title.setText(getFileNameFromURI(Uri.parse(fileUri)));

                if (fileUri.length() > 12) {
                    Uri uri = Uri.parse(fileUri);
                    file_title.setText(getFileNameFromURI(uri).substring((int) (0), (int) (12)).concat("..."));
                }
            } else {
                file_title.setText("File");
            }


            setAnimation(_view, _position);
            icon_remove.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View _view) {
                    int _currentPos = _holder.getAdapterPosition();
                    if (_currentPos != RecyclerView.NO_POSITION) {
                        _data.remove((int) (_currentPos));
                        notifyItemRemoved(_currentPos);
                        notifyItemRangeChanged(_currentPos, _data.size());
                        addFileIconVisibility(true);
                    }
                }
            });

            container.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View _view) {
                    Intent toImageView = new Intent();
                    toImageView.setClass(UploadresourceActivity.this, ImageviewActivity.class);
                    toImageView.putExtra("uri", _data.get((int) _position).get("file uri").toString());
                    toImageView.putExtra("id", "null");
                    startActivity(toImageView);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                }
            });
        }

        @Override
        public int getItemCount() {
            return _data.size();
        }

        // Custom animation method
        private void setAnimation(View viewToAnimate, int position) {
            if (position > _lastPosition) {
                Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), android.R.anim.fade_in);
                viewToAnimate.startAnimation(animation);
                _lastPosition = position;
            }
        }

        // Custom clear list method with animation
        private void clearList() {
            int size = _data.size();

            if (size > 0) {
                for (int i = size - 1; i >= 0; i--) {
                    _data.remove(i);
                    notifyItemRemoved(i);
                }
            }
        }

        // Optional: clear animation on view detached (prevents flickering)
        @Override
        public void onViewDetachedFromWindow(ViewHolder holder) {
            holder.itemView.clearAnimation();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }

    public static class FileUploader {
        public interface UploadCallback {
            void onSuccess(List<String> downloadURLs, List<StorageReference> uploadedFileRefs);

            void onFailure(Exception e);
        }

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
                        .addOnSuccessListener(taskSnapshot -> {
                            fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                uploadedFileRefs.add(fileRef);
                                downloadURLs.add(downloadUri.toString());
                                if (uploadedCount.incrementAndGet() == totalFilesSize && !failed.get()) {
                                    builder.setContentText("Successfully uploaded")
                                            .setOngoing(false)
                                            .setProgress(0, 0, false);
                                    notificationManager.notify(NOTIFICATION_ID, builder.build());
                                    callback.onSuccess(downloadURLs, uploadedFileRefs);
                                }
                            });
                        })
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
    }

    private static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "upload progress", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("notification for file upload");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }


    public double getFileSizeInMB(final Uri _sourceUri) {
        Cursor cursor = null;
        try {
            String[] projection = {OpenableColumns.SIZE};
            cursor = getContentResolver().query(_sourceUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    long sizeInBytes = cursor.getLong(sizeIndex);
                    return sizeInBytes / (1024.0 * 1024.0);
                }
            }
        } catch (Exception ignored) {

        } finally {
            if (cursor != null) cursor.close();
        }
        if ("file".equals(_sourceUri.getScheme())) {
            File file = new File(_sourceUri.getPath());
            if (file.exists()) {
                long sizeInBytes = file.length();
                return sizeInBytes / (1024.0 * 1024.0);
            }
        }
        return -1;
    }


    public void attachAdapterToRecyclerView() {
        filesListAdapter = new Files_recyclerviewAdapter(filesMapList);
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


    public void initializeRequiredValues() {
        reqValues = new HashMap<>();
        reqValues.put("courses_json", "");
        reqValues.put("semesters_json", "");
        reqValues.put("sessions_json", "");
        reqValues.put("selected_course_id", "");
        reqValues.put("selected_semester", 0);
        reqValues.put("IMAGE_PICK_CODE", 1002);
        reqValues.put("IMAGES_LIMIT", "3");
    }


    public String getFormattedNumber(final double _value) {
        if (_value == 1) {
            return "1st";
        }
        if (_value == 2) {
            return "2nd";
        }
        if (_value == 3) {
            return "3rd";
        }
        return String.valueOf((long) _value).concat("th");
    }


    public void addTotalSemesters(final double _duration) {
        ArrayList<HashMap<String, Object>> _temp = new ArrayList<>();
        int maxSemesters = (int) (_duration * 2);
        for (int sem = 1; sem <= maxSemesters; sem++) {
            {
                HashMap<String, Object> _item = new HashMap<>();
                _item.put("semester", String.valueOf((long) (sem)));
                _temp.add(_item);
            }
        }
        reqValues.put("semesters_json", new Gson().toJson(_temp));
    }


    public String capitalizeString(final String _input) {
        if (_input == null || _input.isEmpty()) return _input;
        return _input.substring(0, 1).toUpperCase() + _input.substring(1).toLowerCase();

    }


    public void showUploadConfirmationSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog confirmation_sheet;
        confirmation_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(UploadresourceActivity.this);
        StatusViewBinding sheetbinding = StatusViewBinding.inflate(getLayoutInflater());

        confirmation_sheet.setContentView(sheetbinding.getRoot());

        confirmation_sheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetbinding.bg.setBackground(gd);
        sheetbinding.title.setTextSize((int) 16);
        sheetbinding.subtext.setTextSize((int) 11);
        sheetbinding.imageContainer.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 360, 0xFFFAFAFA));
        sheetbinding.image.setImageResource(R.drawable.icon_upload);
        sheetbinding.title.setText("Confirmation!");
        sheetbinding.subtext.setText("Please review all your details carefully before uploading. Once submitted, you won’t be able to make any changes.");
        PrepNestUtil.roundViewWithRipple(sheetbinding.btnOk, "#000000", 15, 0, "#000000", "#212121");
        PrepNestUtil.roundViewWithRipple(sheetbinding.btnCancel, "#F5F5F5", 15, 0, "#000000", "#E0E0E0");
        sheetbinding.btnOkTxt.setText("Confirm");
        sheetbinding.btnCancelTxt.setText("Cancel");
        sheetbinding.btnOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                uploadAllData();
                confirmation_sheet.dismiss();
            }
        });
        sheetbinding.btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                confirmation_sheet.dismiss();
            }
        });
        confirmation_sheet.setCancelable(true);
        confirmation_sheet.show();
    }


    public void uploadAllData() {
        List<String> URIs = new ArrayList<>();
        DatabaseReference requestRef = requests.child(auth.getCurrentUser().getUid());
        for (HashMap<String, Object> uriMap : filesMapList) {
            URIs.add(uriMap.get("file uri").toString());
        }

        PrepNestUtil.showLoadingDialog(this, true);
        FileUploader.uploadFilesToStorage(URIs, this, new FileUploader.UploadCallback() {
            @Override
            public void onSuccess(List<String> downloadURLs, List<StorageReference> uploadedFileRefs) {


                HashMap<String, Object> addResource = new HashMap<>();
                String resourceID = requestRef.push().getKey();
                addResource.put("subject", binding.subjectEdittext.getText().toString().trim());
                addResource.put("course id", reqValues.get("selected_course_id").toString());
                addResource.put("semester", ((Number) reqValues.get("selected_semester")).intValue());
                addResource.put("uploader uid", auth.getCurrentUser().getUid());
                addResource.put("date of upload", String.valueOf((long) System.currentTimeMillis()));
                addResource.put("resource id", resourceID);
                addResource.put("resource urls", new Gson().toJson(downloadURLs));
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
                String resourceTitle = capitalizeString(addResource.get("subtype").toString()).concat(" ");
                if (addResource.get("subtype").toString().equals("midterm")) {
                    resourceTitle += midtermType + " ";
                }
                resourceTitle += "Paper";
                addResource.put("resource title", resourceTitle);
                requestRef.child(resourceID).setValue(addResource).addOnCompleteListener(uploadTask -> {
                    if (uploadTask.isSuccessful()) {
                        PrepNestUtil.showLoadingDialog(UploadresourceActivity.this, false);
                        sendNotifications("admin", "New Resource", "New resource from '".concat(userData.get("name").toString().concat("'")));
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


    public void sendNotifications(final String _topic, final String _title, final String _message) {
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
                jsonParam.put("topic", _topic);
                jsonParam.put("title", _title);
                jsonParam.put("body", _message);

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
			PrepNestUtil.showToast(UploadresourceActivity.this, "Response: " + response);
		});
        */
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
				        /*
        _loadingDialog(false);
		runOnUiThread(() -> {
			PrepNestUtil.showToast(UploadresourceActivity.this, "Error: " + e.getMessage());
		});
        */
            }
        }).start();

    }
}
