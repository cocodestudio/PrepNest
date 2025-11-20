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
    private HashMap<String, Object> appFirstVisit = new HashMap<>();
    private SharedPreferences appFirstVisitSp;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = OnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize() {
        appFirstVisitSp = getSharedPreferences("app first visit", Activity.MODE_PRIVATE);

        binding.viewpager.addOnPageChangeListener(new OnPageChangeListener() {
            @Override
            public void onPageScrolled(int _position, float _positionOffset, int _positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int _position) {
                if (_position == (datalist.size() - 1)) {
                    appFirstVisit = new Gson().fromJson(appFirstVisitSp.getString("app first visit", ""), new TypeToken<HashMap<String, Object>>() {
                    }.getType());
                    appFirstVisit.put("onboarding", "true");
                    appFirstVisitSp.edit().putString("app first visit", new Gson().toJson(appFirstVisit)).apply();
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
        assert binding.viewpager.getAdapter() != null;
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

        private final Context context;
        private final ArrayList<HashMap<String, Object>> data;

        public ViewpagerAdapter(Context context, ArrayList<HashMap<String, Object>> data) {
            this.context = context;
            this.data = data;
        }

        // Only valid when called inside OnboardingActivity
        public ViewpagerAdapter(ArrayList<HashMap<String, Object>> data) {
            this.context = OnboardingActivity.this;
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "page " + position;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {

            OnboardingItemBinding binding = OnboardingItemBinding.inflate(
                    LayoutInflater.from(context),
                    container,
                    false
            );

            // Safe value extraction
            String title = String.valueOf(data.get(position).get("title"));
            String subtext = String.valueOf(data.get(position).get("subtext"));

            binding.title.setText(title);
            binding.subtext.setText(subtext);

            // Set image based on position
            switch (position) {
                case 0:
                    binding.image.setImageResource(R.drawable.welcome_illus);
                    break;
                case 1:
                    binding.image.setImageResource(R.drawable.app_about_illus);
                    break;
                case 2:
                    binding.image.setImageResource(R.drawable.earn_illus);
                    break;
            }

            View root = binding.getRoot();
            container.addView(root);
            return root;
        }
    }
}
