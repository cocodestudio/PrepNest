package com.cocode.prepnest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.ResourceDetailsSheetLayoutBinding;
import com.cocode.prepnest.databinding.ResourceItemCardFullBinding;
import com.cocode.prepnest.databinding.UserResourceOptionsSheetLayoutBinding;
import com.cocode.prepnest.databinding.UseruploadedresourcesFragmentBinding;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UseruploadedresourcesFragmentActivity extends Fragment {

    private UseruploadedresourcesFragmentBinding binding;
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private HashMap<String, Object> userData = new HashMap<>();
    private final AtomicBoolean requestedResourcesLoaded = new AtomicBoolean(false);
    private final AtomicBoolean verifiedResourcesLoaded = new AtomicBoolean(false);
    private final Map<String, ValueEventListener> listeners = new HashMap<>();
    private FirebaseDatabase firebase_database = FirebaseDatabase.getInstance();
    private DatabaseReference users = firebase_database.getReference("users");
    private resourcesAdapter listAdapter;
    private DatabaseReference requests = firebase_database.getReference("requests/new_resources_requests");
    private DatabaseReference resources = firebase_database.getReference("resources");
    private DatabaseReference userRequestedResources;
    private LogUtils logFile;
    private String jsonCourseData = "";

    private ArrayList<String> resourceIDs = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> requestedResources = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> verifiedResources = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> allResources = new ArrayList<>();

    private Intent toBecomeProvider = new Intent();
    private Intent toUploadResource = new Intent();
    private Intent toImageView = new Intent();
    private Intent toPDFView = new Intent();
    private ProgressDialog progress_dialog;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
        binding = UseruploadedresourcesFragmentBinding.inflate(_inflater, _container, false);
        initialize(_savedInstanceState, binding.getRoot());
        FirebaseApp.initializeApp(getContext());
        initializeLogic();
        return binding.getRoot();
    }

    private void initialize(Bundle _savedInstanceState, View _view) {

        binding.btnFab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
				/*
toUploadResource.setClass(requireContext(), UploadActivity.class);
*/
                toUploadResource.setClass(requireContext(), UploadresourceActivity.class);
                startActivity(toUploadResource);
                requireActivity().overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            }
        });

        binding.btnProvider.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                toBecomeProvider.setClass(requireActivity(), BecomeproviderActivity.class);
                startActivity(toBecomeProvider);
                requireActivity().finish();
                requireActivity().overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
            }
        });
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        userRequestedResources = requests.child(auth.getCurrentUser().getUid());
        attachAdapterToRecyclerView();
        logFile.addFragment();
        logFile.addLog("RESOURCES", "LOADING RESOURCES");
        requestedResourcesLoaded.set(false);
        verifiedResourcesLoaded.set(false);
        getUploadedResources();
        loadCourses();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        for (String id : listeners.keySet()) {
            userRequestedResources.child(id).removeEventListener(listeners.get(id));
        }
    }

    public void toggleState(final int _imageID, final String _msg, final boolean _layoutIsVisible, final boolean _btnIsVisible) {
        if (_layoutIsVisible) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.itemsLayout.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.itemsLayout.setVisibility(View.VISIBLE);
        }
        binding.stateImage.setImageResource(_imageID);
        binding.stateMsg.setText(_msg);
        if (_btnIsVisible) {
            binding.btnProvider.setVisibility(View.VISIBLE);
        } else {
            binding.btnProvider.setVisibility(View.GONE);
        }
    }


    public void getUploadedResources() {
        DatabaseReference dataRef = users.child(auth.getCurrentUser().getUid());
        logFile.addLog("RESOURCES", "GETTING UPLOADED RESOURCES");
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userData = dataSnapshot.getValue(new GenericTypeIndicator<HashMap<String, Object>>() {
                });

                if (dataSnapshot.exists()) {
                    if (userData.containsKey("provider")) {
                        if ((Boolean) userData.get("provider")) {
                            binding.btnFab.setVisibility(View.VISIBLE);
                            resourceIDs = new ArrayList<>();
                            if (userData.containsKey("uploaded resources")) {
                                if ((userData.get("uploaded resources") == null) || userData.get("uploaded resources").toString().equals("")) {
                                    toggleState(R.drawable.empty_box_illus, getString(R.string.no_uploaded_resource_message), true, false);
                                } else {
                                    resourceIDs = new Gson().fromJson(userData.get("uploaded resources").toString(), new TypeToken<ArrayList<String>>() {
                                    }.getType());
                                    toggleState(R.drawable.empty_box_illus, getString(R.string.no_uploaded_resource_message), false, false);
                                }
                            } else {
                                toggleState(R.drawable.empty_box_illus, getString(R.string.no_uploaded_resource_message), true, false);
                            }
                            logFile.addLog("RESOURCES", "LOADING VERIFIED RESOURCES");
                            getVerifiedResources(resourceIDs);
                            logFile.addLog("RESOURCES", "LOADING REQUESTED RESOURCES");
                            getRequestedResources();
                        } else {
                            binding.btnFab.setVisibility(View.GONE);
                            if (userData.containsKey("provider verification status")) {
                                if (userData.get("provider verification status").toString().equals("pending")) {
                                    toggleState(R.drawable.pending_illus, getString(R.string.pending_provider_verification_message), true, false);
                                    binding.progressBarLayout.setVisibility(View.GONE);
                                } else {
                                    if (userData.get("provider verification status").toString().equals("failed")) {
                                        toggleState(R.drawable.failed_illus, getString(R.string.resend_provider_verification_message), true, false);
                                        binding.progressBarLayout.setVisibility(View.GONE);
                                    } else {
                                        binding.progressBarLayout.setVisibility(View.GONE);
                                    }
                                }
                            } else {
                                toggleState(R.drawable.locked_illus, getString(R.string.no_upload_access_message), true, true);
                                binding.progressBarLayout.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        toggleState(R.drawable.locked_illus, getString(R.string.no_upload_access_message), true, true);
                        binding.btnFab.setVisibility(View.GONE);
                        binding.progressBarLayout.setVisibility(View.GONE);
                    }
                } else {
                    logFile.addLog("RESOURCES", "FAILED LOADING USER DATA");
                    PrepNestUtil.showToast(requireContext(), "User data not found, please login again!");
                    auth.signOut();
                    requireActivity().finishAffinity();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                logFile.addLog("RESOURCES", "FAILED LOADING DATA : ".concat(databaseError.toString()));
                PrepNestUtil.showToast(requireContext(), "An unknown error occurred : ".concat(databaseError.toString()));
                binding.progressBarLayout.setVisibility(View.GONE);
            }
        });
    }


    public void attachAdapterToRecyclerView() {
        binding.itemsList.setLayoutManager(new LinearLayoutManager(getContext()));
        listAdapter = new resourcesAdapter(new onResourceActionListener() {
            @Override
            public void showOptions(HashMap<String, Object> item) {
                showResourceOptionsSheet(item);
            }
        });
        binding.itemsList.setAdapter(listAdapter);
        binding.itemsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    // Scrolling down: Hide FAB
                    binding.btnFab.animate()
                            .translationY(binding.btnFab.getHeight() + 100) // Push it out of view
                            .alpha(0f)
                            .setDuration(200)
                            .start();
                } else if (dy < 0) {
                    // Scrolling up: Show FAB
                    binding.btnFab.animate()
                            .translationY(0)
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                }
            }
        });
    }


    public void checkIfBothLoaded() {
        if (requestedResourcesLoaded.get() && verifiedResourcesLoaded.get()) {
            logFile.addLog("RESOURCES", "DATA FROM BOTH DB LOADED");
            binding.progressBarLayout.setVisibility(View.GONE);
            if ((requestedResources.size() + verifiedResources.size()) == 0) {
                toggleState(R.drawable.empty_box_illus, getString(R.string.no_uploaded_resource_message), true, false);
            } else {
                binding.emptyStateLayout.setVisibility(View.GONE);
                binding.itemsLayout.setVisibility(View.VISIBLE);
                logFile.addLog("RESOURCES", "COMBINING LIST");
                updateCombinedList();
            }
        }
    }


    public void updateCombinedList() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            synchronized (allResources) {
                ArrayList<HashMap<String, Object>> updatedList = new ArrayList<>();

                for (int index = requestedResources.size() - 1; index >= 0; index--) {
                    HashMap<String, Object> item = requestedResources.get(index);
                    updatedList.add(item);
                }

                for (int index = verifiedResources.size() - 1; index >= 0; index--) {
                    HashMap<String, Object> item = verifiedResources.get(index);
                    updatedList.add(item);
                }

                allResources.clear();
                allResources.addAll(updatedList);
                listAdapter.updateData(new ArrayList<>(allResources));
                logFile.addLog("RESOURCES", "LIST IS COMBINED");
            }
        });
    }


    public void getRequestedResources() {
        ValueEventListener otherListener = new ValueEventListener() {
            boolean firstLoad = true;

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    requestedResourcesLoaded.set(true);
                    checkIfBothLoaded();
                    return;
                }
                synchronized (requestedResources) {
                    requestedResources.clear();
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Map<String, Object> map = (Map<String, Object>) child.getValue();
                        if (map != null) {
                            map.put("id", child.getKey());
                            requestedResources.add(new HashMap<>(map));
                        }
                    }
                    if (firstLoad) {
                        requestedResourcesLoaded.set(true);
                        checkIfBothLoaded();
                        firstLoad = false;
                    }
                    if (requestedResourcesLoaded.get() && verifiedResourcesLoaded.get()) {
                        updateCombinedList();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (isAdded()) {
                    logFile.addLog("RESOURCES", "FAILED LOADING REQUESTED RESOURCES");
                    PrepNestUtil.showToast(requireActivity(), "An unknown error occurred: " + error.getMessage());
                }
            }
        };
        userRequestedResources.addValueEventListener(otherListener);
    }


    public void getVerifiedResources(final ArrayList<String> _childNodes) {
        if (_childNodes != null && !_childNodes.isEmpty()) {
            AtomicInteger loadedCount = new AtomicInteger(0);
            for (String id : _childNodes) {
                logFile.addLog("RESOURCES", "LOADING DATA OF ID : ".concat(id));
                DatabaseReference resourceRef = resources.child(id);
                ValueEventListener listener = new ValueEventListener() {
                    boolean firstLoad = true;

                    @Override
                    public void onDataChange(DataSnapshot dataSnapchot) {
                        synchronized (verifiedResources) {
                            verifiedResources.removeIf(map -> id.equals(map.get("id")));
                            if (dataSnapchot.exists()) {
                                Map<String, Object> map = (Map<String, Object>) dataSnapchot.getValue();
                                if (map != null) {
                                    map.put("id", id);
                                    verifiedResources.add(new HashMap<>(map));
                                }
                            }

                            if (firstLoad) {
                                if (loadedCount.incrementAndGet() == _childNodes.size()) {
                                    verifiedResourcesLoaded.set(true);
                                    checkIfBothLoaded();
                                }
                                firstLoad = false;
                            }
                            if (requestedResourcesLoaded.get() && verifiedResourcesLoaded.get()) {
                                updateCombinedList();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (isAdded()) {
                            logFile.addLog("RESOURCES", "FAILED TO LOAD DATA, ID : " + id);
                            PrepNestUtil.showToast(requireActivity(), "An unknown error occurred: " + error.getMessage());
                        }
                    }
                };
                resourceRef.addValueEventListener(listener);
                listeners.put(id, listener);
            }
        } else {
            verifiedResourcesLoaded.set(true);
            checkIfBothLoaded();
        }
    }


    public interface onResourceActionListener {
        void showOptions(HashMap<String, Object> item);
    }

    public class resourcesAdapter extends RecyclerView.Adapter<resourcesAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> _data = new ArrayList<>();
        private final onResourceActionListener listener;
        private int _lastPosition = -1; // Used to track animations


        public resourcesAdapter(onResourceActionListener listener) {
            this.listener = listener;
            setHasStableIds(true);
        }

        public void updateData(List<HashMap<String, Object>> newData) {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ResourceDiffCallback(_data, newData));
            _data.clear();
            _data.addAll(newData);
            result.dispatchUpdatesTo(this);
        }

        @Override
        public long getItemId(int position) {
            Object id = _data.get(position).get("id");
            return id != null ? id.toString().hashCode() : position;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = LayoutInflater.from(parent.getContext());
            View _v = _inflater.inflate(R.layout.resource_item_card_full, parent, false);

            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            ResourceItemCardFullBinding binding = ResourceItemCardFullBinding.bind(_view);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _view.setLayoutParams(_lp);

            // UI design
            binding.iconMoreOptions.setVisibility(View.VISIBLE);
            PrepNestUtil.roundViewWithRipple(binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            binding.uploadDateTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            binding.bestChoiceTag.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFF000000));
            binding.recommendedTag.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFF000000));
            // UI End


            HashMap<String, Object> item = _data.get((int) _position);

            if (item.containsKey("status")) {
                binding.statusTxt.setText(item.get("status").toString());
                binding.statusTxt.setVisibility(View.VISIBLE);
                switch (item.get("status").toString()) {
                    case "pending":
                        binding.statusTxt.setTextColor(0xFFF57C00);
                        binding.statusTxt.setBackground(new GradientDrawable() {
                            public GradientDrawable getIns(int a, int b) {
                                this.setCornerRadius(a);
                                this.setColor(b);
                                return this;
                            }
                        }.getIns((int) 360, 0xFFFFF3E0));
                        break;

                    case "failed":
                        binding.statusTxt.setTextColor(0xFFE53935);
                        binding.statusTxt.setBackground(new GradientDrawable() {
                            public GradientDrawable getIns(int a, int b) {
                                this.setCornerRadius(a);
                                this.setColor(b);
                                return this;
                            }
                        }.getIns((int) 360, 0xFFFFEBEE));
                        break;

                    case "verified":
                        if (item.containsKey("discontinue")) {
                            if (item.get("discontinue").toString().equals("true")) {
                                binding.statusTxt.setBackground(new GradientDrawable() {
                                    public GradientDrawable getIns(int a, int b) {
                                        this.setCornerRadius(a);
                                        this.setColor(b);
                                        return this;
                                    }
                                }.getIns((int) 360, 0xFFF5F5F5));
                                binding.statusTxt.setTextColor(0xFF757575);
                                binding.statusTxt.setText("discontinue");
                                binding.statusTxt.setVisibility(View.VISIBLE);
                            } else {
                                binding.statusTxt.setVisibility(View.GONE);
                            }
                        } else {
                            binding.statusTxt.setVisibility(View.GONE);
                        }
                        break;

                    default:
                        binding.statusTxt.setTextColor(0xFF757575);
                        binding.statusTxt.setVisibility(View.VISIBLE);
                }
            } else {
                binding.statusTxt.setVisibility(View.VISIBLE);
                binding.statusTxt.setText("pending");
                binding.statusTxt.setTextColor(0xFFF57C00);
                binding.statusTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFFFF3E0));
            }

            if (item.containsKey("subject")) {
                binding.subjectNameTxt.setText(item.get("subject").toString());
                binding.subjectNameTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                binding.subjectNameTxt.setVisibility(View.VISIBLE);
            } else {
                binding.subjectNameTxt.setText("None");
                binding.subjectNameTxt.setVisibility(View.VISIBLE);
            }


            if (item.containsKey("resource title")) {
                binding.title.setText(item.get("resource title").toString());
            } else {
                binding.title.setText("No name");
            }

            if (item.containsKey("type")) {
                if (item.get("type").toString().equals("paper")) {
                    binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    binding.image.setImageResource(R.drawable.short_notes);
                }
            } else {
                binding.image.setVisibility(View.INVISIBLE);
            }
            if (item.containsKey("best choice")) {
                if (item.get("best choice").toString().equals("true")) {
                    binding.bestChoiceTag.setVisibility(View.VISIBLE);
                } else {
                    binding.bestChoiceTag.setVisibility(View.GONE);
                }
            } else {
                binding.bestChoiceTag.setVisibility(View.GONE);
            }
            if (item.containsKey("recommended")) {
                if (item.get("recommended").toString().equals("true")) {
                    binding.recommendedTag.setVisibility(View.VISIBLE);
                } else {
                    binding.recommendedTag.setVisibility(View.GONE);
                }
            } else {
                binding.recommendedTag.setVisibility(View.GONE);
            }
            if (item.containsKey("date of upload")) {
                binding.uploadDateTxt.setText(formatTimeDifference(item.get("date of upload").toString()));
                binding.uploadDateTxt.setVisibility(View.VISIBLE);
            } else {
                binding.uploadDateTxt.setVisibility(View.GONE);
            }


            LinearLayout.LayoutParams paramscontainer = (LinearLayout.LayoutParams) binding.container.getLayoutParams();
            if (_position == (_data.size() - 1)) {
                paramscontainer.setMargins((int) convertToDp(20), (int) convertToDp(10), (int) convertToDp(20), (int) convertToDp(10));
            } else {
                paramscontainer.setMargins((int) convertToDp(20), (int) convertToDp(10), (int) convertToDp(20), (int) 0);
            }
            paramscontainer.width = LinearLayout.LayoutParams.MATCH_PARENT;
            paramscontainer.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            binding.container.setLayoutParams(paramscontainer);


            binding.iconMoreOptions.setOnClickListener(v -> {
                int pos = _holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.showOptions(_data.get(pos));
                }
            });


            binding.container.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View _view) {
                    int pos = _holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        logFile.addLog("RESOURCE OPEN", "OPENING RESOURCE ITEM");
                        openResource(_data.get((int) pos));
                    }
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

    public class ResourceDiffCallback extends DiffUtil.Callback {

        private final List<HashMap<String, Object>> oldList;
        private final List<HashMap<String, Object>> newList;

        public ResourceDiffCallback(List<HashMap<String, Object>> oldList, List<HashMap<String, Object>> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldId = String.valueOf(oldList.get(oldItemPosition).get("id"));
            String newId = String.valueOf(newList.get(newItemPosition).get("id"));
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }

    }


    public double convertToDp(final double _pixels) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (float) _pixels,
                getResources().getDisplayMetrics());
    }


    public String formatTimeDifference(final String _time) {
        long timeStampMS = Long.parseLong(_time);
        LocalDateTime timestamp = Instant.ofEpochMilli(timeStampMS).atZone(ZoneId.systemDefault()).toLocalDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");


        return timestamp.format(formatter);
    }


    public static class FirebaseFileDeleter {
        public interface DeletionCallback {
            void onAllDeleted();

            void onError(Exception e);
        }

        public static void deleteFilesFromStorage(List<String> urlList, Context context, DeletionCallback callback) {
            FirebaseStorage firebase_storage = FirebaseStorage.getInstance();
            AtomicInteger deleteCount = new AtomicInteger(0);

            if (urlList.isEmpty()) {
                callback.onAllDeleted();
                return;
            }

            LogUtils staticLog = new LogUtils(context);

            for (String url : urlList) {
                try {
                    StorageReference fileRef = firebase_storage.getReference().child(extractStoragePathFromURL(url));
                    staticLog.addLog("RESOURCE DELETE", "DELETING : ".concat(fileRef.getPath()));
                    fileRef.delete().addOnSuccessListener(unused -> {
                        if (deleteCount.incrementAndGet() == urlList.size()) {
                            callback.onAllDeleted();
                        }
                    }).addOnFailureListener(callback::onError);
                } catch (Exception e) {
                    callback.onError(e);
                    return;
                }
            }
        }
    }

    public static class FilesDownloader {
        public interface DownloadCallback {
            void onDownloadComplete();

            void onDownloadFailed(Exception e);
        }

        public static void downloadFiles(Context context, List<String> fileURLsList, String folderName, DownloadCallback callback) {
            File internalDir = new File(context.getFilesDir(), folderName);

            FirebaseStorage storage = FirebaseStorage.getInstance();
            int totalFiles = fileURLsList.size();
            AtomicInteger downloadedCount = new AtomicInteger(0);

            LogUtils staticLog = new LogUtils(context);


            for (String url : fileURLsList) {
                String storagePath = extractStoragePathFromURL(url);
                StorageReference fileRef = storage.getReference().child(storagePath);

                staticLog.addLog("RESOURCE DOWNLOAD", "DOWNLOADING RESOURCE : ".concat(storagePath));

                String fileName = storagePath.substring((int) (storagePath.indexOf("/")));

                if (!internalDir.exists()) {
                    internalDir.mkdirs();
                }

                File localFile = new File(internalDir, fileName);

                fileRef.getFile(localFile).addOnSuccessListener(aVoid -> {
                    staticLog.addLog("RESOURCE DOWNLOAD", "FILE DOWNLOADED SUCCESSFULLY");
                    if (downloadedCount.incrementAndGet() == totalFiles) {
                        callback.onDownloadComplete();
                    }
                }).addOnFailureListener(callback::onDownloadFailed);
            }
        }
    }

    public static class FolderUtils {

        public static boolean deleteFolder(Context context, String folderName) {
            File internalDir = new File(context.getFilesDir(), folderName);
            return deleteRecursively(internalDir);
        }

        private static boolean deleteRecursively(File fileOrDirectory) {
            if (fileOrDirectory != null && fileOrDirectory.exists()) {
                if (fileOrDirectory.isDirectory()) {
                    File[] children = fileOrDirectory.listFiles();
                    if (children != null) {
                        for (File child : children) {
                            deleteRecursively(child);
                        }
                    }
                }
                return fileOrDirectory.delete();
            }
            return false;
        }
    }

    public static String extractStoragePathFromURL(String url) {
        try {
            Pattern pattern = Pattern.compile("/o/(.+?)\\?");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String encodedPath = matcher.group(1);
                return URLDecoder.decode(encodedPath, "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    {
    }


    public void showLoadingDialog(final boolean isShowing) {
        if (isShowing) {
            if (progress_dialog == null) {

                progress_dialog = new ProgressDialog(requireActivity());

                progress_dialog.setCancelable(false);

                progress_dialog.setCanceledOnTouchOutside(false);

                progress_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                progress_dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            }

            progress_dialog.show();
            progress_dialog.setContentView(R.layout.progress_bar);
            LinearLayout container = (LinearLayout) progress_dialog.findViewById(R.id.container);
            container.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 70, 0xFFFFFFFF));
        } else {
            if (progress_dialog != null) {
                progress_dialog.dismiss();
            }
        }
    }


    public void showResourceOptionsSheet(final HashMap<String, Object> _item) {
        com.google.android.material.bottomsheet.BottomSheetDialog resource_options;
        resource_options = new com.google.android.material.bottomsheet.BottomSheetDialog(requireActivity());
        UserResourceOptionsSheetLayoutBinding sheetbinding = UserResourceOptionsSheetLayoutBinding.inflate(getActivity().getLayoutInflater());

        resource_options.setContentView(sheetbinding.getRoot());

        resource_options.setOnShowListener(dialog -> {
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
        PrepNestUtil.roundViewWithRipple(sheetbinding.deleteOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(sheetbinding.discontinueOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(sheetbinding.showDetailsOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        if (_item.containsKey("status")) {
            if (_item.get("status").toString().equals("verified")) {
                sheetbinding.deleteOption.setVisibility(View.GONE);
                sheetbinding.discontinueOption.setVisibility(View.VISIBLE);
            } else {
                sheetbinding.deleteOption.setVisibility(View.VISIBLE);
                sheetbinding.discontinueOption.setVisibility(View.GONE);
            }
        } else {
            sheetbinding.deleteOption.setVisibility(View.VISIBLE);
            sheetbinding.discontinueOption.setVisibility(View.GONE);
        }
        if (_item.containsKey("discontinue")) {
            if ((Boolean) _item.get("discontinue")) {
                sheetbinding.discontinueTxt.setText("Continue");
            } else {
                sheetbinding.discontinueTxt.setText("Discontinue");
            }
        } else {
            sheetbinding.discontinueTxt.setText("Discontinue");
        }
        sheetbinding.deleteOption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                logFile.addLog("ITEM DELETE", "ITEM DELETION START");
                showLoadingDialog(true);
                resource_options.dismiss();
                deleteItemFromFirebase(_item);
            }
        });
        sheetbinding.discontinueOption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                logFile.addLog("RESOURCE STATUS", "RESOURCE IS CONTINUING/DISCONTINUING");
                showLoadingDialog(true);
                changeResourceStatus(_item);
                resource_options.dismiss();
            }
        });
        sheetbinding.showDetailsOption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View _view) {
                showResourceDetailsSheet(_item);
                resource_options.dismiss();
            }
        });
        resource_options.setCancelable(true);
        resource_options.show();
    }


    public void deleteItemFromFirebase(final HashMap<String, Object> _item) {
        String id = String.valueOf(_item.get("id"));
        int length = allResources.size();
        if (!resourceIDs.contains(_item.get("id").toString())) {
            logFile.addLog("RESOURCE ITEM", "STARTS DELETING");
            userRequestedResources.child(id).removeValue().addOnCompleteListener(aVoid -> {
                logFile.addLog("RESOURCE ITEM", "DELETED FROM DATABASE");
                logFile.addLog("RESOURCE ITEM", "DELETING FROM STORAGE");
                List<String> fileURLs = new ArrayList<>();
                fileURLs = new Gson().fromJson(_item.get("resource urls").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
                FirebaseFileDeleter.deleteFilesFromStorage(fileURLs, requireActivity(), new FirebaseFileDeleter.DeletionCallback() {
                    @Override
                    public void onAllDeleted() {
                        if (allResources.size() == length) {
                            allResources.remove(_item);
                            listAdapter.updateData(new ArrayList<>(allResources));
                        }
                        logFile.addLog("RESOURCE DELETE", "ALL FILES DELETED");
                        showLoadingDialog(false);
                        PrepNestUtil.showToast(requireActivity(), "Deleted successfully!");
                        FolderUtils.deleteFolder(requireActivity(), id);
                    }

                    @Override
                    public void onError(Exception e) {
                        logFile.addLog("RESOURCE DELETE", "FILES FAILED TO DELETE : " + e.getMessage());
                        showLoadingDialog(false);
                        PrepNestUtil.showToast(requireActivity(), "An unknown error occurred: " + e.getMessage());
                    }
                });

            }).addOnFailureListener(e -> {
                logFile.addLog("RESOURCE DELETE", "FAILED TO DELETE FROM DATABASE : " + e.getMessage());
                showLoadingDialog(false);
                PrepNestUtil.showToast(requireActivity(), "Failed to delete, try again later");
            });
        }
    }


    public void openResource(final HashMap<String, Object> _item) {
        File internalDir = getContext().getFilesDir();
        String folderName = _item.get("id").toString();
        File targetFolder = new File(internalDir, folderName);
        if (targetFolder.exists()) {
            logFile.addLog("RESOURCE OPEN", "FOLDER EXISTS");
            navigateToResourceView(_item);
        } else {
            logFile.addLog("RESOURCE OPEN", "FOLDER DOESN'T EXISTS");
            logFile.addLog("RESOURCE DOWNLOAD", "DOWNLOADING RESOURCE ITEMS");
            showLoadingDialog(true);
            boolean created = targetFolder.mkdirs();

            if (created) {

                logFile.addLog("RESOURCE DOWNLOAD", "FOLDER CREATED SUCCESSFULLY");
                List<String> fileURLs = new ArrayList<>();
                fileURLs = new Gson().fromJson(_item.get("resource urls").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
                FilesDownloader.downloadFiles(getContext(), fileURLs, folderName, new FilesDownloader.DownloadCallback() {
                    @Override
                    public void onDownloadComplete() {
                        logFile.addLog("RESOURCE DOWNLOAD", "ALL RESOURCES DOWNLOADED");
                        showLoadingDialog(false);
                        navigateToResourceView(_item);
                    }

                    @Override
                    public void onDownloadFailed(Exception e) {
                        logFile.addLog("RESOURCE DOWNLOAD", "FAILED TO DOWNLOAD RESOURCES : " + e.toString());
                        showLoadingDialog(false);
                        boolean success = FolderUtils.deleteFolder(getContext(), folderName);
                        logFile.addLog("RESOURCE DOWNLOAD", "FOLDER DELETION STATUS : " + Boolean.toString(success));
                        PrepNestUtil.showToast(getContext(), e.getCause().toString());
                    }
                });
            } else {
                logFile.addLog("RESOURCE DOWNLOAD", "FAILED TO CREATE FOLDER");
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), "An unknown error occurred !");
                return;
            }

        }
    }


    public void changeResourceStatus(final HashMap<String, Object> _item) {
        String id = _item.get("id").toString();
        DatabaseReference resourceItem = resources.child(id);
        boolean status = false;
        if (_item.containsKey("discontinue")) {
            status = (Boolean) _item.get("discontinue");
        } else {
        }
        logFile.addLog("RESOURCE STATUS", "CURRENT STATUS : ".concat(Boolean.toString(status)));
        resourceItem.child("discontinue").setValue(!status).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                logFile.addLog("RESOURCE STATUS", "UPDATED SUCCESSFULLY");
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), "Successfully updated!");
            } else {
                logFile.addLog("RESOURCE STATUS", "FAILED TO UPDATE");
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), task.getException() != null ? task.getException().getCause().toString() : "An unknown error occurred");
            }
        });
    }


    public void showResourceDetailsSheet(final HashMap<String, Object> _item) {
        com.google.android.material.bottomsheet.BottomSheetDialog resource_details_sheet;
        resource_details_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireActivity());
        ResourceDetailsSheetLayoutBinding sheetbinding = ResourceDetailsSheetLayoutBinding.inflate(getActivity().getLayoutInflater());

        resource_details_sheet.setContentView(sheetbinding.getRoot());

        resource_details_sheet.setOnShowListener(dialog -> {
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
        if (_item.containsKey("resource title")) {
            sheetbinding.resourceTitle.setText(_item.get("resource title").toString());
        } else {
            sheetbinding.resourceTitle.setText("No title");
        }
        if (_item.containsKey("course id")) {
            sheetbinding.resourceCourse.setText(getCourseName(_item.get("course id").toString()));
        } else {
            sheetbinding.resourceCourse.setText("None");
        }
        if (_item.containsKey("session")) {
            sheetbinding.sessionValueTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetbinding.sessionValueTxt.setText(_item.get("session").toString());
        } else {
            sheetbinding.sessionContainer.setVisibility(View.GONE);
        }
        if (_item.containsKey("semester")) {
            sheetbinding.semesterValueTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetbinding.semesterValueTxt.setText(getFormattedNumber(((Number) _item.get("semester")).doubleValue()).concat(" semester"));
        } else {
            sheetbinding.semesterContainer.setVisibility(View.GONE);
        }
        if (_item.containsKey("subject")) {
            sheetbinding.subjectValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetbinding.subjectValue.setText(_item.get("subject").toString());
        } else {
            sheetbinding.subjectContainer.setVisibility(View.GONE);
        }
        if (_item.containsKey("subtype")) {
            sheetbinding.subtypeValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            if (_item.get("subtype").toString().equals("midtem")) {
                sheetbinding.subtypeValue.setText("Midterm");
            } else {
                if (_item.get("subtype").toString().equals("notes")) {
                    sheetbinding.subtypeValue.setText("Semester");
                } else {
                    sheetbinding.subtypeContainer.setVisibility(View.GONE);
                }
            }
        } else {
            sheetbinding.subtypeContainer.setVisibility(View.GONE);
        }
		/*
if (_item.containsKey("rating")) {
sheetbinding.ratingContainer.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFFFF3E0));
sheetbinding.ratingTxt.setText(_item.get("rating").toString());
} else {
sheetbinding.ratingTitle.setVisibility(View.GONE);
sheetbinding.ratingContainer.setVisibility(View.GONE);
}
*/
        if (_item.containsKey("date of upload")) {
            sheetbinding.dateOfUploadValue.setText(formatTimeDifference(_item.get("date of upload").toString()));
            sheetbinding.dateOfUploadValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
        } else {
            sheetbinding.dateOfUploadTitle.setVisibility(View.GONE);
            sheetbinding.dateOfUploadValue.setVisibility(View.GONE);
        }
        if (_item.containsKey("best choice")) {
            if ((Boolean) _item.get("best choice")) {
                sheetbinding.bestChoiceTag.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFF000000));
                sheetbinding.bestChoiceTag.setVisibility(View.VISIBLE);
            } else {
                sheetbinding.bestChoiceTag.setVisibility(View.GONE);
            }
        } else {
            sheetbinding.bestChoiceTag.setVisibility(View.GONE);
        }
        if (_item.containsKey("recommended")) {
            if ((Boolean) _item.get("recommended")) {
                sheetbinding.recommendedTag.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFF000000));
                sheetbinding.recommendedTag.setVisibility(View.VISIBLE);
            } else {
                sheetbinding.recommendedTag.setVisibility(View.GONE);
            }
        } else {
            sheetbinding.recommendedTag.setVisibility(View.GONE);
        }
        if (_item.containsKey("price")) {
            sheetbinding.cashValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetbinding.coinsValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetbinding.cashValue.setText("₹".concat(String.valueOf((long) (((Number) _item.get("price")).longValue()))));
            sheetbinding.coinsValue.setText(String.valueOf((long) (((Number) _item.get("price")).longValue() * 5)).concat(" coins"));
        } else {
            sheetbinding.priceTitle.setVisibility(View.GONE);
            sheetbinding.cashValue.setVisibility(View.GONE);
            sheetbinding.coinsValue.setVisibility(View.GONE);
            sheetbinding.slash.setVisibility(View.GONE);
        }
        resource_details_sheet.show();
    }


    public void loadCourses() {
        if (jsonCourseData.equals("")) {
            try {
                InputStream is = getContext().getAssets().open("courses.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                jsonCourseData = new String(buffer, "UTF-8");
            } catch (IOException ex) {
                PrepNestUtil.showToast(requireContext(), ex.toString());
            }
        }
    }


    public String getCourseName(final String _courseID) {
        try {
            JSONObject allCourses = new JSONObject(jsonCourseData);
            JSONObject course = allCourses.getJSONObject(_courseID);
            return course.getString("name");
        } catch (Exception e) {
            PrepNestUtil.showToast(requireContext(), e.toString());
        }
        return "None";
    }


    public void navigateToResourceView(final HashMap<String, Object> _item) {
        if (_item.containsKey("type")) {
            if (_item.get("type").toString().equals("paper")) {
                logFile.addLog("RESOURCE OPEN", "NAVIGATE TO IMAGE VIEW");
                toImageView.setClass(requireContext(), ImageviewActivity.class);
                toImageView.putExtra("id", _item.get("id").toString());
                startActivity(toImageView);
            } else {
                logFile.addLog("RESOURCE OPEN", "NAVIGATE TO PDF VIEW");
                toPDFView.putExtra("id", _item.get("id").toString());
                startActivity(toPDFView);
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
}
