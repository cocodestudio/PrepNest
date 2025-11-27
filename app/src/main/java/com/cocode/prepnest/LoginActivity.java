package com.cocode.prepnest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.LoginBinding;
import com.cocode.prepnest.databinding.StatusViewBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Objects;


public class LoginActivity extends AppCompatActivity {

    private final Intent toSignup = new Intent();
    private final Intent toHomePage = new Intent();
    private boolean isPasswordShow = false;
    private LoginBinding binding;
    private FirebaseAuth auth;
    private OnCompleteListener<AuthResult> authSignInListener;
    private OnCompleteListener<Void> authResetPasswordListener;
    private SharedPreferences userCredentialsSP;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = LoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        userCredentialsSP = getSharedPreferences("credentials", Activity.MODE_PRIVATE);

        binding.loginBtn.setOnClickListener(_view -> {
            PrepNestUtil.hideKeyboard(LoginActivity.this);
            PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
            if (binding.emailEdittext.getText().toString().trim().isEmpty()) {
                binding.emailErrorTxt.setText("Enter your email");
                binding.emailErrorTxt.setVisibility(View.VISIBLE);
            } else {
                if (validateEmail(binding.emailEdittext.getText().toString().trim())) {
                    if (binding.passwordEdittext.getText().toString().trim().isEmpty()) {
                        binding.passwordErrorTxt.setText("Enter your password");
                        binding.passwordErrorTxt.setVisibility(View.VISIBLE);
                    } else {
                        if (binding.passwordEdittext.getText().toString().trim().length() > 7) {
                            if (PrepNestUtil.isConnected(LoginActivity.this)) {
                                PrepNestUtil.showLoadingDialog(LoginActivity.this, true);
                                auth.signInWithEmailAndPassword(binding.emailEdittext.getText().toString().trim(), binding.passwordEdittext.getText().toString().trim()).addOnCompleteListener(LoginActivity.this, authSignInListener);
                            } else {
                                com.google.android.material.snackbar.Snackbar.make(binding.wrapperLayout, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", _view1 -> {

                                }).show();
                            }
                        } else {
                            binding.passwordErrorTxt.setText("Password must be 8 characters long");
                            binding.passwordErrorTxt.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    binding.emailErrorTxt.setText("Enter a valid email address");
                    binding.emailErrorTxt.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.emailEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {
                if (binding.emailErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.emailErrorTxt.setVisibility(View.GONE);
                }
            }
        });

        binding.passwordEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {
                if (binding.passwordErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.passwordErrorTxt.setVisibility(View.INVISIBLE);
                }
            }
        });

        binding.forgetPassTxt.setOnClickListener(_view -> {
            PrepNestUtil.hideKeyboard(LoginActivity.this);
            PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
            if (binding.emailEdittext.getText().toString().trim().isEmpty()) {
                binding.emailErrorTxt.setText("Enter your email");
                binding.emailErrorTxt.setVisibility(View.VISIBLE);
            } else {
                if (validateEmail(binding.emailEdittext.getText().toString().trim())) {
                    PrepNestUtil.showLoadingDialog(LoginActivity.this, true);
                    auth.sendPasswordResetEmail(binding.emailEdittext.getText().toString().trim()).addOnCompleteListener(authResetPasswordListener);
                } else {
                    binding.emailErrorTxt.setText("Enter a valid email address");
                    binding.emailErrorTxt.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.signupBtnText.setOnClickListener(_view -> {
            toSignup.setClass(LoginActivity.this, SignupActivity.class);
            startActivity(toSignup);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        binding.pwdShowIcon.setOnClickListener(_view -> {
            if (isPasswordShow) {
                binding.passwordEdittext.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                binding.pwdShowIcon.setImageResource(R.drawable.icon_show_password);
            } else {
                binding.passwordEdittext.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                binding.pwdShowIcon.setImageResource(R.drawable.icon_hide_password);
            }
            isPasswordShow = !isPasswordShow;
            binding.passwordEdittext.setSelection(binding.passwordEdittext.getText().toString().length());

        });

        authSignInListener = task -> {
//                final boolean _success = _param1.isSuccessful();
//                final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
            PrepNestUtil.showLoadingDialog(LoginActivity.this, false);
            if (task.isSuccessful()) {
                HashMap<String, Object> userCredentials = new HashMap<>();
                userCredentials.put("email", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail());
                userCredentials.put("password", binding.passwordEdittext.getText().toString().trim());
                userCredentialsSP.edit().putString("credentials", new Gson().toJson(userCredentials)).apply();
                toHomePage.setClass(LoginActivity.this, HomepageActivity.class);
                startActivity(toHomePage);
                overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);

                finish();
            } else {
                showLoginErrorDialog();
            }
        };

        authResetPasswordListener = task -> {
            PrepNestUtil.showLoadingDialog(LoginActivity.this, false);
            if (task.isSuccessful()) {
                showPasswordResetDialog();
            } else {
                PrepNestUtil.showToast(LoginActivity.this, "Error in sending email, try again later");
            }
        };
    }

    private void initializeLogic() {
        designUI();
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
                    PrepNestUtil.hideKeyboard(LoginActivity.this);
                    view.clearFocus();
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public void designUI() {
        binding.passwordEdittext.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }


    public boolean validateEmail(final String _email) {
        return Patterns.EMAIL_ADDRESS.matcher(_email).matches();
    }


    public void showPasswordResetDialog() {
        AlertDialog pwdResetDialog = new AlertDialog.Builder(LoginActivity.this).create();


        StatusViewBinding dialogBinding = StatusViewBinding.inflate(getLayoutInflater());

        Objects.requireNonNull(pwdResetDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        pwdResetDialog.setView(dialogBinding.getRoot());


        dialogBinding.btnOk.setVisibility(View.GONE);
        dialogBinding.btnCancel.setVisibility(View.GONE);
        dialogBinding.image.setImageResource(R.drawable.icon_email);
        dialogBinding.bg.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 30, 0xFFFFFFFF));
//        dialogBinding.imageContainer.setBackground(new GradientDrawable() {
//            public GradientDrawable getIns(int a, int b) {
//                this.setCornerRadius(a);
//                this.setColor(b);
//                return this;
//            }
//        }.getIns((int) 360, 0xFFFAFAFA));
        dialogBinding.title.setText("Password Reset Link");
        dialogBinding.subtext.setText("We have sent you a mail on ".concat(binding.emailEdittext.getText().toString().concat(" with a password reset link, click on the link to reset your password.")));
        pwdResetDialog.setCancelable(true);
        pwdResetDialog.show();

        ViewGroup.MarginLayoutParams paramsBg =
                (ViewGroup.MarginLayoutParams) dialogBinding.bg.getLayoutParams();

        paramsBg.setMargins((int) convertToDp(20), (int) convertToDp(20), (int) convertToDp(20), (int) convertToDp(20));
        dialogBinding.bg.setLayoutParams(paramsBg);

    }


    public void showLoginErrorDialog() {
        AlertDialog loginErrorDialog = new AlertDialog.Builder(LoginActivity.this).create();


        StatusViewBinding dialogBinding = StatusViewBinding.inflate(getLayoutInflater());

        Objects.requireNonNull(loginErrorDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

        loginErrorDialog.setView(dialogBinding.getRoot());

        dialogBinding.image.setImageResource(R.drawable.icon_failed_error);
        dialogBinding.btnOk.setVisibility(View.GONE);
        dialogBinding.btnCancel.setVisibility(View.GONE);
        dialogBinding.bg.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 30, 0xFFFFFFFF));
//        dialogBinding.imageContainer.setBackground(new GradientDrawable() {
//            public GradientDrawable getIns(int a, int b) {
//                this.setCornerRadius(a);
//                this.setColor(b);
//                return this;
//            }
//        }.getIns((int) 360, 0xFFFAFAFA));
        dialogBinding.title.setText("Incorrect details");
        dialogBinding.subtext.setText("The account with email : ".concat(binding.emailEdittext.getText().toString().concat(" doesn't exists or the password is wrong.")));
        loginErrorDialog.setCancelable(true);
        loginErrorDialog.show();

        ViewGroup.MarginLayoutParams paramsBg =
                (ViewGroup.MarginLayoutParams) dialogBinding.bg.getLayoutParams();

        paramsBg.setMargins((int) convertToDp(20), (int) convertToDp(20), (int) convertToDp(20), (int) convertToDp(20));
        dialogBinding.bg.setLayoutParams(paramsBg);
    }


    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }
}
