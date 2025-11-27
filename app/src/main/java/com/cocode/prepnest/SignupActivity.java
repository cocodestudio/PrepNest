package com.cocode.prepnest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.SignupBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SignupActivity extends AppCompatActivity implements ItemListSheetFragment.BottomSheetListener {

    private final DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
    private final DatabaseReference transaction_history = FirebaseDatabase.getInstance().getReference("transactions_history");
    private final Intent toLogin = new Intent();
    private final Intent toAddRefer = new Intent();
    private final Intent toInformation = new Intent();
    private ArrayList<HashMap<String, Object>> itemsList = new ArrayList<>();
    private HashMap<String, Object> dataPayload = new HashMap<>();
    private SignupBinding binding;
    private boolean isPasswordShow = false;
    private int selectedSemester = 0;
    private FirebaseAuth auth;
    private OnCompleteListener<AuthResult> _auth_create_user_listener;
    private SharedPreferences userCredentials;
    private SharedPreferences cachedData;
    private String selectedCourseId = "-1";
    private String selectedSemesterId = "-1";
    private String jsonCourseData = null;

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
        userCredentials = getSharedPreferences("credentials", Activity.MODE_PRIVATE);
        cachedData = getSharedPreferences("userCachedData", Activity.MODE_PRIVATE);

        binding.signupBtn.setOnClickListener(_view -> {
            Rect r = new Rect();
            binding.wrapperLayout.getWindowVisibleDisplayFrame(r);
            int screenHeight = binding.wrapperLayout.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > (screenHeight * 0.15d)) {
                PrepNestUtil.hideKeyboard(SignupActivity.this);
            }
            PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
            if (binding.nameEdittext.getText().toString().trim().isEmpty()) {
                binding.nameErrorTxt.setText("Enter your name");
                binding.nameErrorTxt.setVisibility(View.VISIBLE);
            } else {
                if (selectedCourseId.equals("-1")) {
                    binding.otherDetailsErrorTxt.setText("Select your course");
                    binding.otherDetailsErrorTxt.setVisibility(View.VISIBLE);
                } else {
                    if (selectedSemesterId.equals("-1")) {
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
                                                binding.confirmPwdErrorTxt.setText("Password must be at least 8 characters long");
                                                binding.confirmPwdErrorTxt.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    } else {
                                        binding.passwordErrorTxt.setText("Password must be at least 8 characters long");
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
            int screenHeight = binding.wrapperLayout.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            if (keypadHeight > (screenHeight * 0.15d)) {
                PrepNestUtil.hideKeyboard(SignupActivity.this);
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
            if (selectedCourseId.equals("-1")) {
                PrepNestUtil.TransitionManager(binding.wrapperLayout, 150);
                binding.otherDetailsErrorTxt.setText("Select your course first");
                binding.otherDetailsErrorTxt.setVisibility(View.VISIBLE);
            } else {
                Rect r = new Rect();
                binding.wrapperLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = binding.wrapperLayout.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                if (keypadHeight > (screenHeight * 0.15d)) {
                    PrepNestUtil.hideKeyboard(SignupActivity.this);
                }

                createSemestersList(selectedCourseId);

                dataPayload = new HashMap<>();
                dataPayload.put("type", ItemListSheetFragment.SheetType.SEMESTER.toString());
                dataPayload.put("list", new ArrayList<>(itemsList));
                dataPayload.put("selectedItemId", selectedSemesterId);

                final ItemListSheetFragment semSheet = ItemListSheetFragment.newInstance(dataPayload);
                semSheet.show(getSupportFragmentManager(), "semSheet");
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
                HashMap<String, Object> addUser;
                addUser = new HashMap<>();
                addUser.put("name", binding.nameEdittext.getText().toString().trim());
                addUser.put("email", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail());
                addUser.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                addUser.put("profile", null);
                addUser.put("courseId", selectedCourseId);
                addUser.put("semester", selectedSemester);
                addUser.put("currentYear", (selectedSemester + 1) / 2);
                addUser.put("provider", false);
//                addUser.put("referred by", "");
//                addWelcomeBonusToUser(addUser);
                createNewUser(addUser);
                // SAVING USER CREDENTIALS
                HashMap<String, Object> credentialsMap = new HashMap<>();
                credentialsMap.put("email", binding.emailEdittext.getText().toString());
                credentialsMap.put("password", binding.confirmPwdEdittext.getText().toString());
                userCredentials.edit().putString("user credentials", new Gson().toJson(credentialsMap)).apply();

                // SAVING USER DATA
                cachedData.edit().putString("userData", new Gson().toJson(addUser)).apply();
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
                } else {
                    PrepNestUtil.showToast(SignupActivity.this, task.getException() != null ? task.getException().getMessage() : "");
                }
            }
        };
    }

    private void initializeLogic() {
        designUI();
//        getCourses();
        loadAllCoursesFromJson(this);
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
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }

    public void createNewUser(final HashMap<String, Object> userData) {
        assert auth.getCurrentUser() != null;
        auth.getCurrentUser().getIdToken(true)
                .addOnSuccessListener((GetTokenResult result) -> {
                    String idToken = result.getToken();
                    userData.put("timestamp", System.currentTimeMillis());

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
                            Log.e("createNewUser", Objects.requireNonNull(e.getMessage()));
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) {
                            if (response.isSuccessful()) {
                                PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                                toAddRefer.setClass(SignupActivity.this, AddreferralActivity.class);
                                startActivity(toAddRefer);
                                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                                finish();
                            } else {
                                PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                                PrepNestUtil.showToast(SignupActivity.this, "An unknown error occurred");
                                assert response.body() != null;
                                Log.d("createNewUser", "Response: " + response.body());
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

    public String setCourseName(String courseId) {
        final String courseName = getCourseName(courseId);
        int spaceIndex = courseName.indexOf(" ");

        if (spaceIndex == -1) {
            return courseName;
        }

        return courseName.substring(0, spaceIndex);
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

    public boolean validateEmail(final String _email) {
        return Patterns.EMAIL_ADDRESS.matcher(_email).matches();
    }

    public void addWelcomeBonusToUser(final HashMap<String, Object> _user) {
        final String temp = "10";
        assert auth.getCurrentUser() != null;
        DatabaseReference userRef = users.child(auth.getCurrentUser().getUid());
        userRef.updateChildren(_user).addOnCompleteListener(addUser -> {
            if (addUser.isSuccessful()) {
                HashMap<String, Object> userCredentials;
                userCredentials = new HashMap<>();
                userCredentials.put("email", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail());
                userCredentials.put("password", binding.confirmPwdEdittext.getText().toString().trim());
//                userCredentials.edit().putString("credentials", new Gson().toJson(userCredentials)).apply();
                userRef.child("coins").setValue(Long.parseLong(temp)).addOnCompleteListener(addBonus -> {
                    if (addBonus.isSuccessful()) {
                        DatabaseReference historyRef = transaction_history.child(auth.getCurrentUser().getUid());
                        final String key = historyRef.push().getKey();
                        HashMap<String, Object> addHistory;
                        addHistory = new HashMap<>();
                        addHistory.put("type", "welcome_bonus");
                        addHistory.put("amount", Double.parseDouble(temp));
                        addHistory.put("timestamp", String.valueOf(System.currentTimeMillis()));
                        historyRef.child(key).setValue(addHistory).addOnCompleteListener(historyTask -> {
                            if (historyTask.isSuccessful()) {
                            } else {
                            }
                            PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                            toAddRefer.setClass(SignupActivity.this, AddreferralActivity.class);
                            startActivity(toAddRefer);
                            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                            finish();
                        });
                    } else {
                        PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                        toAddRefer.setClass(SignupActivity.this, AddreferralActivity.class);
                        startActivity(toAddRefer);
                        overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
                        finish();
                    }
                });
            } else {
                PrepNestUtil.showLoadingDialog(SignupActivity.this, false);
                PrepNestUtil.showToast(SignupActivity.this, "Error: try again");
                FirebaseAuth.getInstance().signOut();
                finish();
            }
        });
    }

    @Override
    public void onDataReturned(HashMap<String, Object> updatedMap) {
        if (updatedMap.containsKey("type") && updatedMap.containsKey("id")) {
            final String type = Objects.requireNonNull(updatedMap.get("type")).toString();
            if (type.equals(ItemListSheetFragment.SheetType.COURSE.toString())) {
                selectedCourseId = Objects.requireNonNull(updatedMap.get("id")).toString();
                binding.courseTxt.setText(setCourseName(selectedCourseId));
                selectedSemesterId = "-1";
                binding.semesterTxt.setText("Semester");
            } else if (type.equals(ItemListSheetFragment.SheetType.SEMESTER.toString())) {
                selectedSemesterId = Objects.requireNonNull(updatedMap.get("id")).toString();
                selectedSemester = Integer.parseInt(selectedSemesterId);
                binding.semesterTxt.setText(PrepNestUtil.getFormattedNumber(selectedSemester).concat(" semester"));
            }

            if (binding.otherDetailsErrorTxt.getVisibility() == View.VISIBLE) {
                PrepNestUtil.TransitionManager(binding.wrapperLayout, 150);
                binding.otherDetailsErrorTxt.setVisibility(View.GONE);
            }
        }
    }
}
