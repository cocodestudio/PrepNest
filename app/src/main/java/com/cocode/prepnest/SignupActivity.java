package com.cocode.prepnest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.DetailsSelectSheetBinding;
import com.cocode.prepnest.databinding.SheetSingleItemSelectBinding;
import com.cocode.prepnest.databinding.SignupBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SignupActivity extends AppCompatActivity {

    private final DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
    private final DatabaseReference transaction_history = FirebaseDatabase.getInstance().getReference("transactions_history");
    private final ArrayList<String> keysList = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> totalSemestersList = new ArrayList<>();
    private final Intent toLogin = new Intent();
    private final Intent toAddRefer = new Intent();
    private final Intent toInformation = new Intent();
    private SignupBinding binding;
    private boolean isPasswordShow = false;
    private String coursesJson = "";
    private String selectedCourseID = "";
    private LogUtils logFile;
    private double selectedSemester = 0;
    private ArrayList<HashMap<String, Object>> allCoursesList = new ArrayList<>();
    private FirebaseAuth auth;
    private OnCompleteListener<AuthResult> _auth_create_user_listener;
    private SharedPreferences user_credentials;
    private int selectedPosition = -1;
    private int selectedCoursePosition = -1;
    private int selectedSemesterPosition = -1;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = SignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        user_credentials = getSharedPreferences("credentials", Activity.MODE_PRIVATE);

        binding.signupBtn.setOnClickListener(_view -> {
            Rect r = new Rect();
            binding.wrapperLayout.getWindowVisibleDisplayFrame(r);
            int screenheight = binding.wrapperLayout.getRootView().getHeight();
            int keypadheight = screenheight - r.bottom;
            if (keypadheight > (screenheight * 0.15d)) {
                PrepNestUtil.hideKeyboard(SignupActivity.this);
            }
            PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
            if (binding.nameEdittext.getText().toString().trim().isEmpty()) {
                binding.nameErrorTxt.setText("Enter your name");
                binding.nameErrorTxt.setVisibility(View.VISIBLE);
            } else {
                if (selectedCourseID.isEmpty()) {
                    binding.otherDetailsErrorTxt.setText("Select your course");
                    binding.otherDetailsErrorTxt.setVisibility(View.VISIBLE);
                } else {
                    if (selectedSemester == 0) {
                        binding.otherDetailsErrorTxt.setText("Select your semester");
                        binding.otherDetailsErrorTxt.setVisibility(View.VISIBLE);
                    } else {
                        if (binding.emailEdittext.getText().toString().trim().isEmpty()) {
                            binding.emailErrorTxt.setText("Enter your email");
                            binding.emailErrorTxt.setVisibility(View.VISIBLE);
                        } else {
                            if (validateEmail(binding.emailEdittext.getText().toString().trim())) {
                                if (binding.passwordEdittext.getText().toString().isEmpty()) {
                                    binding.passwordErrorTxt.setText("Enter your password");
                                    binding.passwordErrorTxt.setVisibility(View.VISIBLE);
                                } else {
                                    if (binding.passwordEdittext.getText().toString().trim().length() > 7) {
                                        if (binding.confirmPwdEdittext.getText().toString().trim().isEmpty()) {
                                            binding.confirmPwdErrorTxt.setText("Confirm your password");
                                            binding.confirmPwdErrorTxt.setVisibility(View.VISIBLE);
                                        } else {
                                            if (binding.confirmPwdEdittext.getText().toString().trim().length() > 7) {
                                                if (binding.passwordEdittext.getText().toString().trim().equals(binding.confirmPwdEdittext.getText().toString().trim())) {
                                                    if (PrepNestUtil.isConnected(SignupActivity.this)) {
                                                        PrepNestUtil.showLoadingDialog(SignupActivity.this, true);
                                                        logFile.addLog("USER", "CREATING USER ACCOUNT");
                                                        auth.createUserWithEmailAndPassword(binding.emailEdittext.getText().toString().trim(), binding.confirmPwdEdittext.getText().toString().trim()).addOnCompleteListener(SignupActivity.this, _auth_create_user_listener);
                                                    } else {
                                                        com.google.android.material.snackbar.Snackbar.make(binding.wrapperLayout, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", _view1 -> {

                                                        }).show();
                                                    }
                                                } else {
                                                    binding.confirmPwdErrorTxt.setText("Password did not match");
                                                    binding.passwordErrorTxt.setVisibility(View.GONE);
                                                    binding.confirmPwdErrorTxt.setVisibility(View.VISIBLE);
                                                }
                                            } else {
                                                binding.confirmPwdErrorTxt.setText("Password must be atleast 8 characters long");
                                                binding.confirmPwdErrorTxt.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    } else {
                                        binding.passwordErrorTxt.setText("Password must be atleast 8 characters long");
                                        binding.passwordErrorTxt.setVisibility(View.VISIBLE);
                                    }
                                }
                            } else {
                                binding.emailErrorTxt.setText("Enter a valid email address");
                                binding.emailErrorTxt.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
            }
        });

        binding.nameEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                if (binding.nameErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.nameErrorTxt.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {

            }
        });

        binding.courseSelectContainer.setOnClickListener(_view -> {
            Rect r = new Rect();
            binding.wrapperLayout.getWindowVisibleDisplayFrame(r);
            int screenheight = binding.wrapperLayout.getRootView().getHeight();
            int keypadheight = screenheight - r.bottom;
            if (keypadheight > (screenheight * 0.15d)) {
                PrepNestUtil.hideKeyboard(SignupActivity.this);
            }
            final BottomSheetDialog coursesSheet = new BottomSheetDialog(SignupActivity.this);

            DetailsSelectSheetBinding sheetbinding = DetailsSelectSheetBinding.inflate(getLayoutInflater());

            coursesSheet.setContentView(sheetbinding.getRoot());

            coursesSheet.setOnShowListener(dialog -> {
                BottomSheetDialog d = (BottomSheetDialog) dialog;
                View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    bottomSheet.setBackgroundResource(android.R.color.transparent);
                }
            });

            ViewGroup.LayoutParams paramscontainer = sheetbinding.container.getLayoutParams();
            paramscontainer.width = ViewGroup.LayoutParams.MATCH_PARENT;
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
                    allCoursesList = new Gson().fromJson(coursesJson, new TypeToken<ArrayList<HashMap<String, Object>>>() {
                    }.getType());
                    for (int index = allCoursesList.size() - 1; index >= 0; index--) {
                        if (!(_charSeq.length() > allCoursesList.get(index).get("name").toString().length()) && allCoursesList.get(index).get("name").toString().toLowerCase().contains(_charSeq.toLowerCase())) {

                        } else {
                            allCoursesList.remove(index);
                        }
                    }
                    sheetbinding.itemsList.setAdapter(new ItemsAdapter(allCoursesList));

                    ((BaseAdapter) sheetbinding.itemsList.getAdapter()).notifyDataSetChanged();
                }

                @Override
                public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

                }

                @Override
                public void afterTextChanged(Editable _param1) {

                }
            });
            allCoursesList = new Gson().fromJson(coursesJson, new TypeToken<ArrayList<HashMap<String, Object>>>() {
            }.getType());
            sheetbinding.itemsList.setAdapter(new ItemsAdapter(allCoursesList));

            selectedPosition = selectedCoursePosition;

            ((BaseAdapter) sheetbinding.itemsList.getAdapter()).notifyDataSetChanged();
            sheetbinding.itemsList.setOnItemClickListener((_param1, _param2, _param3, _param4) -> {
                selectedCoursePosition = _param3;
                selectedCourseID = keysList.get(_param3);
                int spaceIndex = allCoursesList.get(_param3).get("name").toString().indexOf(" ");
                if (spaceIndex != -1) {
                    binding.courseTxt.setText(allCoursesList.get(_param3).get("name").toString().substring(0, spaceIndex));
                } else {
                    binding.courseTxt.setText(allCoursesList.get(_param3).get("name").toString());
                }
                if (binding.otherDetailsErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.otherDetailsErrorTxt.setVisibility(View.GONE);
                }
                addTotalSemesters(Double.parseDouble(allCoursesList.get(_param3).get("duration").toString()));
                selectedSemester = 0;
                selectedSemesterPosition = -1;
                binding.semesterTxt.setText("Semester");
                coursesSheet.dismiss();
            });
            coursesSheet.setCancelable(true);
            coursesSheet.show();
        });

        binding.semesterSelectContainer.setOnClickListener(_view -> {
            if (selectedCourseID.isEmpty()) {
                PrepNestUtil.TransitionManager(binding.wrapperLayout, 150);
                binding.otherDetailsErrorTxt.setText("Select your course first");
                binding.otherDetailsErrorTxt.setVisibility(View.VISIBLE);
            } else {
                Rect r = new Rect();
                binding.wrapperLayout.getWindowVisibleDisplayFrame(r);
                int screenheight = binding.wrapperLayout.getRootView().getHeight();
                int keypadheight = screenheight - r.bottom;
                if (keypadheight > (screenheight * 0.15d)) {
                    PrepNestUtil.hideKeyboard(SignupActivity.this);
                }
                final BottomSheetDialog semesterSheet = new BottomSheetDialog(SignupActivity.this);

                DetailsSelectSheetBinding sheetbinding = DetailsSelectSheetBinding.inflate(getLayoutInflater());

                semesterSheet.setContentView(sheetbinding.getRoot());

                semesterSheet.setOnShowListener(dialog -> {
                    BottomSheetDialog d = (BottomSheetDialog) dialog;
                    View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    if (bottomSheet != null) {
                        bottomSheet.setBackgroundResource(android.R.color.transparent);
                    }
                });

                ViewGroup.LayoutParams paramscontainer = sheetbinding.container.getLayoutParams();
                paramscontainer.width = ViewGroup.LayoutParams.MATCH_PARENT;
                paramscontainer.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                sheetbinding.container.setLayoutParams(paramscontainer);

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#FFFFFF"));
                gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
                sheetbinding.container.setBackground(gd);
                sheetbinding.searchContainer.setVisibility(View.GONE);
                sheetbinding.itemsList.setVerticalScrollBarEnabled(false);
                sheetbinding.itemsList.setVerticalScrollBarEnabled(false);
                sheetbinding.itemsList.setAdapter(new ItemsAdapter(totalSemestersList));

                selectedPosition = selectedSemesterPosition;

                ((BaseAdapter) sheetbinding.itemsList.getAdapter()).notifyDataSetChanged();
                sheetbinding.itemsList.setOnItemClickListener((_param1, _param2, position, _param4) -> {
                    selectedSemesterPosition = position;
                    selectedSemester = Double.parseDouble(totalSemestersList.get(position).get("semester").toString());
                    binding.semesterTxt.setText(getFormattedNumber(selectedSemester).concat(" semester"));
                    if (binding.otherDetailsErrorTxt.getVisibility() == View.VISIBLE) {
                        PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                        binding.otherDetailsErrorTxt.setVisibility(View.GONE);
                    }
                    semesterSheet.dismiss();
                });
                semesterSheet.setCancelable(true);
                semesterSheet.show();
            }
        });

        binding.emailEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                if (binding.emailErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.emailErrorTxt.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {

            }
        });

        binding.passwordEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                if (binding.passwordErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.passwordErrorTxt.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {

            }
        });

        binding.confirmPwdEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                if (binding.confirmPwdErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.confirmPwdErrorTxt.setVisibility(View.GONE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {

            }
        });

        binding.pwdShowIcon.setOnClickListener(_view -> {
            if (isPasswordShow) {
                binding.passwordEdittext.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());

                binding.confirmPwdEdittext.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                binding.pwdShowIcon.setImageResource(R.drawable.icon_show_password);
            } else {
                binding.passwordEdittext.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());

                binding.confirmPwdEdittext.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());

                binding.pwdShowIcon.setImageResource(R.drawable.icon_hide_password);
            }
            isPasswordShow = !isPasswordShow;
            binding.passwordEdittext.setSelection(binding.passwordEdittext.getText().toString().length());

            binding.confirmPwdEdittext.setSelection(binding.confirmPwdEdittext.getText().toString().length());
        });

        binding.termsTxt.setOnClickListener(_view -> {
            toInformation.setClass(SignupActivity.this, InfoviewActivity.class);
            toInformation.putExtra("type", "terms");
            startActivity(toInformation);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        });

        binding.privacyPolicyTxt.setOnClickListener(_view -> {
            toInformation.setClass(SignupActivity.this, InfoviewActivity.class);
            toInformation.putExtra("type", "policy");
            startActivity(toInformation);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        });

        binding.loginBtnText.setOnClickListener(_view -> {
            toLogin.setClass(SignupActivity.this, LoginActivity.class);
            startActivity(toLogin);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        _auth_create_user_listener = task -> {
//                final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
            if (task.isSuccessful()) {
                logFile.addLog("USER", "ACCOUNT CREATED SUCCESSFULLY");
                HashMap<String, Object> addUser;
                addUser = new HashMap<>();
                addUser.put("name", binding.nameEdittext.getText().toString().trim());
                addUser.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
                addUser.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                addUser.put("profile", null);
                addUser.put("course id", selectedCourseID);
                addUser.put("semester", selectedSemester);
                addUser.put("current year", (int) ((selectedSemester + 1) / 2));
                addUser.put("provider", false);
//                addUser.put("referred by", "");
                logFile.addLog("USER", "ADDING USER TO DB");
//                addWelcomeBonusToUser(addUser);
                createNewUser(addUser);
            } else {
                PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                if (task.getException() instanceof FirebaseAuthException) {
                    String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                    if (errorCode.equals("ERROR_EMAIL_ALREADY_IN_USE")) {
                        PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                        binding.emailErrorTxt.setText("This email is already in use, try login instead");
                        binding.emailErrorTxt.setVisibility(View.VISIBLE);
                    } else {
                        PrepNestUtil.showToast(SignupActivity.this, task.getException() != null ? task.getException().getMessage() : "");
                    }
                }
            }
        };
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        logFile.addActivity();
        designUI();
        getCourses();
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
                    PrepNestUtil.hideKeyboard(SignupActivity.this);
                    view.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void designUI() {
        binding.passwordEdittext.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());

        binding.confirmPwdEdittext.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
        PrepNestUtil.changeNavBarColor(this, true);
    }

    public void createNewUser(final HashMap<String, Object> userData) {
        auth.getCurrentUser().getIdToken(true)
                .addOnSuccessListener((GetTokenResult result) -> {
                    String idToken = result.getToken();
                    userData.put("timestamp", String.valueOf(System.currentTimeMillis()));

                    Gson gson = new Gson();
                    String jsonData = gson.toJson(userData);

                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = RequestBody.create(
                            jsonData,
                            MediaType.parse("application/json; charset=utf-8")
                    );

                    String functionUrl = "https://us-central1-prepnest-65133.cloudfunctions.net/createNewUser";

                    Request request = new Request.Builder()
                            .url(functionUrl)
                            .addHeader("Authorization", "Bearer " + idToken)
                            .post(requestBody)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                            PrepNestUtil.showToast(SignupActivity.this, "An unknown error occurred");
                            Log.e("createNewUser", e.getMessage());
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                                toAddRefer.setClass(SignupActivity.this, AddreferralActivity.class);
                                startActivity(toAddRefer);
                                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                                finish();
                            } else {
                                PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                                PrepNestUtil.showToast(SignupActivity.this, "An unknown error occurred");
                                Log.d("createNewUser", "Response: " + response.body().toString());
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                    PrepNestUtil.showToast(SignupActivity.this, "An unknown error occurred");
                    Log.e("createNewUser", "Failed to get id token", e);
                });
    }


    public String loadCoursesFromJsonFile() {
        String result;

        try {

            InputStream is = getAssets().open("courses.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            result = new String(buffer, "UTF-8");

        } catch (IOException ex) {

            ex.printStackTrace();
            return null;

        }

        return result;

    }


    public void getCourses() {
        try {
            JSONObject allCourses = new JSONObject(loadCoursesFromJsonFile());
            Iterator<String> keys = allCourses.keys();
            while (keys.hasNext()) {
                keysList.add(keys.next());
            }
            for (int pos = 0; pos < keysList.size(); pos++) {
                JSONObject course = allCourses.getJSONObject(keysList.get(pos));
                HashMap<String, Object> addCourse;
                addCourse = new HashMap<>();
                addCourse.put("name", course.getString("name"));
                addCourse.put("duration", String.format("%.1f", course.getDouble("duration")));
                allCoursesList.add(addCourse);
            }
            coursesJson = new Gson().toJson(allCoursesList);
        } catch (Exception ignored) {

        }
    }

    public boolean validateEmail(final String _email) {
        return Patterns.EMAIL_ADDRESS.matcher(_email).matches();
    }

    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }

    public void addTotalSemesters(final double _duration) {
        totalSemestersList.clear();
        int maxSemesters = (int) (_duration * 2);
        for (int sem = 1; sem <= maxSemesters; sem++) {
            {
                HashMap<String, Object> _item = new HashMap<>();
                _item.put("semester", String.valueOf((long) (sem)));
                totalSemestersList.add(_item);
            }
        }
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

    public void addWelcomeBonusToUser(final HashMap<String, Object> _user) {
        final String temp = "10";
        DatabaseReference userRef = users.child(auth.getCurrentUser().getUid());
        userRef.updateChildren(_user).addOnCompleteListener(addUser -> {
            if (addUser.isSuccessful()) {
                logFile.addLog("USER", "USER ADDED TO DB SUCCESSFULLY");
                HashMap<String, Object> userCredentials;
                userCredentials = new HashMap<>();
                userCredentials.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
                userCredentials.put("password", binding.confirmPwdEdittext.getText().toString().trim());
                user_credentials.edit().putString("credentials", new Gson().toJson(userCredentials)).apply();
                logFile.addLog("USER", "ADDING WELCOME BONUS");
                userRef.child("coins").setValue(Long.parseLong(temp)).addOnCompleteListener(addBonus -> {
                    if (addBonus.isSuccessful()) {
                        logFile.addLog("USER", "WELCOME BONUS ADDED SUCCESSFULLY");
                        logFile.addLog("USER", "ADDING BONUS TO USER HISTORY");
                        DatabaseReference historyRef = transaction_history.child(auth.getCurrentUser().getUid());
                        final String key = historyRef.push().getKey();
                        HashMap<String, Object> addHistory;
                        addHistory = new HashMap<>();
                        addHistory.put("type", "welcome_bonus");
                        addHistory.put("amount", Double.parseDouble(temp));
                        addHistory.put("timestamp", String.valueOf(System.currentTimeMillis()));
                        historyRef.child(key).setValue(addHistory).addOnCompleteListener(historyTask -> {
                            if (historyTask.isSuccessful()) {
                                logFile.addLog("USER", "BONUS ADDED SUCCESSFULLY");
                            } else {
                                logFile.addLog("USER", "FAILED TO ADD BONUS HISTORY : ".concat(historyTask.getException().toString()));
                            }
                            PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                            toAddRefer.setClass(SignupActivity.this, AddreferralActivity.class);
                            startActivity(toAddRefer);
                            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                            finish();
                        });
                    } else {
                        logFile.addLog("USER", "FAILED TO ADD BONUS TO USER : ".concat(addBonus.getException().toString()));
                        PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                        toAddRefer.setClass(SignupActivity.this, AddreferralActivity.class);
                        startActivity(toAddRefer);
                        overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                        finish();
                    }
                });
            } else {
                logFile.addLog("USER", "FAILED TO ADD USER TO DB : ".concat(addUser.getException().toString()));
                PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                PrepNestUtil.showToast(SignupActivity.this, "Error: try again");
                FirebaseAuth.getInstance().signOut();
                finish();
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
            if (_data.get(_position).containsKey("name")) {
                binding.text.setText(_data.get(_position).get("name").toString());
            }
            if (_data.get(_position).containsKey("semester")) {
                binding.text.setText(getFormattedNumber(Double.parseDouble(_data.get(_position).get("semester").toString())).concat(" semester"));
            }

            if (selectedPosition == _position) {
                binding.selectedCircle.setVisibility(View.VISIBLE);
            } else {
                binding.selectedCircle.setVisibility(View.GONE);
            }
            return _view;
        }
    }
}
