package com.cocode.prepnest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.AppFaqsBinding;
import com.cocode.prepnest.databinding.FaqItemLayoutBinding;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.HashMap;


public class AppFaqsActivity extends AppCompatActivity {

    private final ArrayList<HashMap<String, Object>> faqsList = new ArrayList<>();
    private AppFaqsBinding binding;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        binding = AppFaqsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {

        binding.backIcon.setOnClickListener(_view -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_fade, R.anim.slide_out_right_fade);
        });
    }

    private void initializeLogic() {
        PrepNestUtil.setLightStatusBar(this);
        addingFAQs();
        binding.faqsList.setHorizontalScrollBarEnabled(false);
        binding.faqsList.setVerticalScrollBarEnabled(false);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                binding.backIcon.performClick();
            }
        });
        PrepNestUtil.changeNavBarColor(this, true);
    }

    public void addingFAQs() {
        HashMap<String, Object> faqItem;
        faqItem = new HashMap<>();
        faqItem.put("ques", "What is PrepNest?");
        faqItem.put("extra", getString(R.string.app_introduction_msg));
        faqsList.add(faqItem);
        faqItem = new HashMap<>();
        faqItem.put("ques", "How to earn coins in PrepNest?");
        faqItem.put("extra", "You can earn coins in multiple ways:\n– Refer new users: Share the app with friends and get rewarded when they purchase their first item.\n– Upload resources: Contribute useful content to the community and earn coins for each approved upload.\n– Purchase resources: Every time you buy a new resource, you’ll also receive bonus coins as a reward.");
        faqsList.add(faqItem);
        faqItem = new HashMap<>();
        faqItem.put("ques", "How to add cash to my PrepNest account?");
        faqItem.put("extra", "Tap the cash icon at the top-right corner of the home screen, then select “Add Cash” and follow the steps to complete your transaction.");
        faqsList.add(faqItem);
        faqItem = new HashMap<>();
        faqItem.put("ques", "Why can't i upload any resources?");
        faqItem.put("extra", "To gain access, you’ll need to become a Provider — a special user role. Simply open the navigation drawer from the Home tab and send us a request to get started.");
        faqsList.add(faqItem);
        faqItem = new HashMap<>();
        faqItem.put("ques", "How can i edit my course?");
        faqItem.put("extra", "At PrepNest, course changes are generally not allowed. However, if you have a valid and compelling reason, you may submit a request by emailing us at cocodestudio.org@gmail.com. Please note that requests without a strong justification will be declined.");
        faqsList.add(faqItem);
        faqItem = new HashMap<>();
        faqItem.put("ques", "Why my uploaded resources showing \"pending\" or \"failed\" status?");
        faqItem.put("extra", "When you upload a resource, it first goes through a verification process to check for authenticity and duplication. Once verified, its status will be updated accordingly.\n\nIf the status shows “Failed”, it could be due to one of the following reasons:\n– The same resource is already available in the app, uploaded by another provider.\n– The resource doesn’t meet our content guidelines or is considered invalid.\n\nMake sure your content is original and follows our submission rules for a successful upload.");
        faqsList.add(faqItem);
        binding.faqsList.setAdapter(new Faqs_listAdapter(faqsList));
        binding.faqsList.setLayoutManager(new LinearLayoutManager(this));
    }


    public class Faqs_listAdapter extends RecyclerView.Adapter<Faqs_listAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public Faqs_listAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.faq_item_layout, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            FaqItemLayoutBinding binding = FaqItemLayoutBinding.bind(_view);

            PrepNestUtil.roundViewWithRipple(binding.quesContainer, "#FFFFFF", 0, 0, "#FFFFFF", "#F5F5F5");
            binding.questionTxt.setText(_data.get(_position).get("ques").toString());
            binding.extraText.setText(_data.get(_position).get("extra").toString());
            binding.quesContainer.setOnClickListener(_view1 -> {
                PrepNestUtil.TransitionManager(binding.container, 250);
                if (binding.extraContentContainer.getVisibility() == View.VISIBLE) {
                    binding.extraContentContainer.setVisibility(View.GONE);
                    binding.iconDropDown
                            .animate()
                            .rotation(0)
                            .setDuration(250)
                            .start();
                } else {
                    binding.extraContentContainer.setVisibility(View.VISIBLE);
                    binding.iconDropDown
                            .animate()
                            .rotation(180)
                            .setDuration(250)
                            .start();
                }
            });
        }

        @Override
        public int getItemCount() {
            return _data.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }
}
