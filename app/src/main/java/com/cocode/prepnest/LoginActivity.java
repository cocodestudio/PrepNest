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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.LoginBinding;
import com.cocode.prepnest.databinding.StatusViewBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.util.HashMap;


public class LoginActivity extends AppCompatActivity {

    private LoginBinding binding;
    private LogUtils logFile;

    private Intent toSignup = new Intent();
    private FirebaseAuth auth;
    private OnCompleteListener<AuthResult> _auth_sign_in_listener;
    private OnCompleteListener<Void> _auth_reset_password_listener;
    private Intent toHomePage = new Intent();
    private AlertDialog pwd_reset_dialog;
    private AlertDialog login_error_dialog;
    private SharedPreferences user_credentials;

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
        user_credentials = getSharedPreferences("credentials", Activity.MODE_PRIVATE);

        binding.loginBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                PrepNestUtil.hideKeyboard(LoginActivity.this);
                PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                if (binding.emailEdittext.getText().toString().trim().equals("")) {
                    binding.emailErrorTxt.setText("Enter your email");
                    binding.emailErrorTxt.setVisibility(View.VISIBLE);
                } else {
                    if (validateEmail(binding.emailEdittext.getText().toString().trim())) {
                        if (binding.passwordEdittext.getText().toString().trim().equals("")) {
                            binding.passwordErrorTxt.setText("Enter your password");
                            binding.passwordErrorTxt.setVisibility(View.VISIBLE);
                        } else {
                            if (binding.passwordEdittext.getText().toString().trim().length() > 7) {
                                if (PrepNestUtil.isConnected(LoginActivity.this)) {
                                    PrepNestUtil.showLoadingDialog(LoginActivity.this, true);
                                    logFile.addLog("USER", "USER IS LOGGING");
                                    auth.signInWithEmailAndPassword(binding.emailEdittext.getText().toString().trim(), binding.passwordEdittext.getText().toString().trim()).addOnCompleteListener(LoginActivity.this, _auth_sign_in_listener);
                                } else {
                                    com.google.android.material.snackbar.Snackbar.make(binding.background, "No internet connection", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).setAction("", new OnClickListener() {
                                        @Override
                                        public void onClick(View _view) {

                                        }
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
            }
        });

        binding.emailEdittext.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                final String _charSeq = _param1.toString();
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
                final String _charSeq = _param1.toString();
                if (binding.passwordErrorTxt.getVisibility() == View.VISIBLE) {
                    PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                    binding.passwordErrorTxt.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

            }

            @Override
            public void afterTextChanged(Editable _param1) {

            }
        });

        binding.forgetPassTxt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                PrepNestUtil.hideKeyboard(LoginActivity.this);
                PrepNestUtil.TransitionManager(binding.edittextsContainer, 150);
                if (binding.emailEdittext.getText().toString().trim().equals("")) {
                    binding.emailErrorTxt.setText("Enter your email");
                    binding.emailErrorTxt.setVisibility(View.VISIBLE);
                } else {
                    if (validateEmail(binding.emailEdittext.getText().toString().trim())) {
                        PrepNestUtil.showLoadingDialog(LoginActivity.this, true);
                        auth.sendPasswordResetEmail(binding.emailEdittext.getText().toString().trim()).addOnCompleteListener(_auth_reset_password_listener);
                    } else {
                        binding.emailErrorTxt.setText("Enter a valid email address");
                        binding.emailErrorTxt.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        binding.signupBtnText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toSignup.setClass(LoginActivity.this, SignupActivity.class);
                startActivity(toSignup);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });

        _auth_sign_in_listener = new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
//                final boolean _success = _param1.isSuccessful();
//                final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
                PrepNestUtil.showLoadingDialog(LoginActivity.this, false);
                if (task.isSuccessful()) {
                    subscribeToTopic();
                    logFile.addLog("USER", "USER LOGGED IN SUCCESSFULLY");
                    HashMap<String, Object> userCredentials = new HashMap<>();
                    userCredentials = new HashMap<>();
                    userCredentials.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
                    userCredentials.put("password", binding.passwordEdittext.getText().toString().trim());
                    user_credentials.edit().putString("credentials", new Gson().toJson(userCredentials)).commit();
                    toHomePage.setClass(LoginActivity.this, HomepageActivity.class);
                    startActivity(toHomePage);
                    overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);

                    finish();
                } else {
                    logFile.addLog("USER", "FAILED TO LOGGED IN : ".concat(task.getException() != null ? task.getException().getMessage() : ""));
                    showLoginErrorDialog();
                }
            }
        };

        _auth_reset_password_listener = new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> _param1) {
                final boolean _success = _param1.isSuccessful();
                PrepNestUtil.showLoadingDialog(LoginActivity.this, false);
                ;
                if (_success) {
                    showPasswordResetDialog();
                } else {
                    PrepNestUtil.showToast(LoginActivity.this, "Error in sending email, try again later");
                }
            }
        };
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        logFile.addActivity();
        designUI();
    }

    public void designUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
    }


    public boolean validateEmail(final String _email) {
        return Patterns.EMAIL_ADDRESS.matcher(_email).matches();
    }


    public void showPasswordResetDialog() {
        pwd_reset_dialog = new AlertDialog.Builder(LoginActivity.this).create();


        StatusViewBinding dialogbinding = StatusViewBinding.inflate(getLayoutInflater());

        pwd_reset_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        pwd_reset_dialog.setView(dialogbinding.getRoot());


        LinearLayout.LayoutParams paramsbg = (LinearLayout.LayoutParams) dialogbinding.bg.getLayoutParams();

        paramsbg.setMargins((int) convertToDp(20), (int) convertToDp(20), (int) convertToDp(20), (int) convertToDp(20));

        dialogbinding.bg.setLayoutParams(paramsbg);
        dialogbinding.btnOk.setVisibility(View.GONE);
        dialogbinding.btnCancel.setVisibility(View.GONE);
        dialogbinding.image.setImageResource(R.drawable.icon_email);
        dialogbinding.bg.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 30, 0xFFFFFFFF));
        dialogbinding.imageContainer.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 360, 0xFFFAFAFA));
        dialogbinding.title.setText("Password Reset Link");
        dialogbinding.subtext.setText("We have sent you a mail on ".concat(binding.emailEdittext.getText().toString().concat(" with a password reset link, click on the link to reset your password.")));
        pwd_reset_dialog.setCancelable(true);
        pwd_reset_dialog.show();


    }


    public void showLoginErrorDialog() {
        login_error_dialog = new AlertDialog.Builder(LoginActivity.this).create();


        StatusViewBinding dialogbinding = StatusViewBinding.inflate(getLayoutInflater());

        login_error_dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        login_error_dialog.setView(dialogbinding.getRoot());


        LinearLayout.LayoutParams paramsbg = (LinearLayout.LayoutParams) dialogbinding.bg.getLayoutParams();

        paramsbg.setMargins((int) convertToDp(20), (int) convertToDp(20), (int) convertToDp(20), (int) convertToDp(20));

        dialogbinding.bg.setLayoutParams(paramsbg);
        dialogbinding.image.setImageResource(R.drawable.icon_error);
        dialogbinding.btnOk.setVisibility(View.GONE);
        dialogbinding.btnCancel.setVisibility(View.GONE);
        dialogbinding.bg.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 30, 0xFFFFFFFF));
        dialogbinding.imageContainer.setBackground(new GradientDrawable() {
            public GradientDrawable getIns(int a, int b) {
                this.setCornerRadius(a);
                this.setColor(b);
                return this;
            }
        }.getIns((int) 360, 0xFFFAFAFA));
        dialogbinding.title.setText("Incorrect details");
        dialogbinding.subtext.setText("The account with email : ".concat(binding.emailEdittext.getText().toString().concat(" doesn't exists or the password is wrong.")));
        login_error_dialog.setCancelable(true);
        login_error_dialog.show();


    }


    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }


    public void subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("all");
        FirebaseMessaging.getInstance().subscribeToTopic(auth.getCurrentUser().getUid());
    }

}
