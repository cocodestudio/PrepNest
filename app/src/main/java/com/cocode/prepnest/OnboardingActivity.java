package com.cocode.prepnest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import com.cocode.prepnest.databinding.OnboardingBinding;
import com.cocode.prepnest.databinding.OnboardingItemBinding;
import com.google.firebase.FirebaseApp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;


public class OnboardingActivity extends AppCompatActivity {

    private final ArrayList<HashMap<String, Object>> datalist = new ArrayList<>();
    private final Intent toLogin = new Intent();
    private final Intent toSignup = new Intent();
    private OnboardingBinding binding;
    private HashMap<String, Object> features_visit_map = new HashMap<>();
    private SharedPreferences features_visit;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = OnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        features_visit = getSharedPreferences("features visit", Activity.MODE_PRIVATE);

        binding.viewpager.addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int _position, float _positionOffset, int _positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int _position) {
                if (_position == (datalist.size() - 1)) {
                    features_visit_map = new Gson().fromJson(features_visit.getString("features visit", ""), new TypeToken<HashMap<String, Object>>() {
                    }.getType());
                    features_visit_map.put("onboarding", "true");
                    features_visit.edit().putString("features visit", new Gson().toJson(features_visit_map)).apply();
                    PrepNestUtil.TransitionManager(binding.btnsContainer, 200);
                    binding.moveToNextBtn.setVisibility(View.GONE);
                    binding.accountBtnsContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int _scrollState) {

            }
        });

        binding.moveToNextBtn.setOnClickListener(_view -> binding.viewpager.setCurrentItem(binding.viewpager.getCurrentItem() + 1));

        binding.loginBtn.setOnClickListener(_view -> {
            toLogin.setClass(OnboardingActivity.this, LoginActivity.class);
            startActivity(toLogin);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            finish();
        });

        binding.signUpBtn.setOnClickListener(_view -> {
            toSignup.setClass(OnboardingActivity.this, SignupActivity.class);
            startActivity(toSignup);
            overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            finish();
        });
    }

    private void initializeLogic() {
        designUI();
    }

    public void initializeViewPager() {
        HashMap<String, Object> datamap = new HashMap<>();
        datamap.put("title", "Welcome");
        datamap.put("subtext", getString(R.string.app_introduction_msg));
        datalist.add(datamap);
        datamap = new HashMap<>();
        datamap.put("title", "How to use?");
        datamap.put("subtext", getString(R.string.app_upload_information_msg));
        datalist.add(datamap);
        datamap = new HashMap<>();
        datamap.put("title", "Earn more");
        datamap.put("subtext", getString(R.string.app_earn_coins_msg));
        datalist.add(datamap);
        binding.viewpager.setAdapter(new ViewpagerAdapter(datalist));
        binding.viewpager.getAdapter().notifyDataSetChanged();
    }


    public void designUI() {
        initializeViewPager();
        PrepNestUtil.roundViewWithRipple(binding.moveToNextBtn, "#000000", 360, 0, "#000000", "#212121");
        PrepNestUtil.roundViewWithRipple(binding.signUpBtn, "#FAFAFA", 15, 0, "#000000", "#E0E0E0");
        PrepNestUtil.setLightStatusBar(this);
        PrepNestUtil.changeNavBarColor(this, true);
    }

    public class ViewpagerAdapter extends PagerAdapter {

        Context _context;
        ArrayList<HashMap<String, Object>> _data;

        public ViewpagerAdapter(Context _ctx, ArrayList<HashMap<String, Object>> _arr) {
            _context = _ctx;
            _data = _arr;
        }

        public ViewpagerAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _context = OnboardingActivity.this;
            _data = _arr;
        }

        @Override
        public int getCount() {
            return _data.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View _view, @NonNull Object _object) {
            return _view == _object;
        }

        @Override
        public void destroyItem(ViewGroup _container, int _position, @NonNull Object _object) {
            _container.removeView((View) _object);
        }

        @Override
        public int getItemPosition(@NonNull Object _object) {
            return super.getItemPosition(_object);
        }

        @Override
        public CharSequence getPageTitle(int pos) {
            // Use the Activity Event (onTabLayoutNewTabAdded) in order to use this method
            return "page " + pos;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup _container, final int _position) {
            OnboardingItemBinding binding = OnboardingItemBinding.inflate(LayoutInflater.from(_context), _container, false);

            binding.title.setText(_data.get(_position).get("title").toString());
            binding.subtext.setText(_data.get(_position).get("subtext").toString());
            if (_position == 0) {
                binding.image.setImageResource(R.drawable.welcome_illus);
            }
            if (_position == 1) {
                binding.image.setImageResource(R.drawable.app_about_illus);
            }
            if (_position == 2) {
                binding.image.setImageResource(R.drawable.earn_illus);
            }

            View _view = binding.getRoot();
            _container.addView(_view);
            return _view;
        }
    }
}
