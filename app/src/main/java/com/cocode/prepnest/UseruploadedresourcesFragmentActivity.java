package com.cocode.prepnest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
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

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final AtomicBoolean requestedResourcesLoaded = new AtomicBoolean(false);
    private final AtomicBoolean verifiedResourcesLoaded = new AtomicBoolean(false);
    private final Map<String, ValueEventListener> listeners = new HashMap<>();
    private final FirebaseDatabase firebase_database = FirebaseDatabase.getInstance();
    private final DatabaseReference users = firebase_database.getReference("users");
    private final DatabaseReference requests = firebase_database.getReference("requests/new_resources_requests");
    private final DatabaseReference resources = firebase_database.getReference("resources");
    private final ArrayList<HashMap<String, Object>> requestedResources = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> verifiedResources = new ArrayList<>();
    private final ArrayList<HashMap<String, Object>> allResources = new ArrayList<>();
    private final Intent toBecomeProvider = new Intent();
    private final Intent toUploadResource = new Intent();
    private final Intent toImageView = new Intent();
    private final Intent toPDFView = new Intent();
    private UseruploadedresourcesFragmentBinding binding;
    private HashMap<String, Object> userData = new HashMap<>();
    private resourcesAdapter listAdapter;
    private DatabaseReference userRequestedResources;
    private String jsonCourseData = "";
    private ArrayList<String> resourceIDs = new ArrayList<>();
    private ProgressDialog progress_dialog;

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

        binding.btnFab.setOnClickListener(_view1 -> {
            /*
toUploadResource.setClass(requireContext(), UploadActivity.class);
*/
            toUploadResource.setClass(requireContext(), UploadresourceActivity.class);
            startActivity(toUploadResource);
            requireActivity().overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        });

        binding.btnProvider.setOnClickListener(_view2 -> {
            toBecomeProvider.setClass(requireActivity(), BecomeproviderActivity.class);
            startActivity(toBecomeProvider);
            requireActivity().finish();
            requireActivity().overridePendingTransition(R.anim.slide_in_right_fade, R.anim.slide_out_left_fade);
        });
    }

    private void initializeLogic() {
        userRequestedResources = requests.child(auth.getCurrentUser().getUid());
        attachAdapterToRecyclerView();
        requestedResourcesLoaded.set(false);
        verifiedResourcesLoaded.set(false);
        getUploadedResources();
        loadCourses();

        PrepNestUtil.changeNavBarColor(requireActivity(), true);
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
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userData = dataSnapshot.getValue(new GenericTypeIndicator<>() {
                });

                if (dataSnapshot.exists()) {
                    if (userData.containsKey("provider")) {
                        if ((Boolean) userData.get("provider")) {
                            binding.btnFab.setVisibility(View.VISIBLE);
                            resourceIDs = new ArrayList<>();
                            if (userData.containsKey("uploaded resources")) {
                                if ((userData.get("uploaded resources") == null) || userData.get("uploaded resources").toString().isEmpty()) {
                                    toggleState(R.drawable.icon_empty_box, getString(R.string.no_uploaded_resource_message), true, false);
                                } else {
                                    resourceIDs = new Gson().fromJson(userData.get("uploaded resources").toString(), new TypeToken<ArrayList<String>>() {
                                    }.getType());
                                    toggleState(R.drawable.icon_empty_box, getString(R.string.no_uploaded_resource_message), false, false);
                                }
                            } else {
                                toggleState(R.drawable.icon_empty_box, getString(R.string.no_uploaded_resource_message), true, false);
                            }
                            getVerifiedResources(resourceIDs);
                            getRequestedResources();
                        } else {
                            binding.btnFab.setVisibility(View.GONE);
                            if (userData.containsKey("provider verification status")) {
                                if (userData.get("provider verification status").toString().equals("pending")) {
                                    toggleState(R.drawable.icon_hourglass, getString(R.string.pending_provider_verification_message), true, false);
                                    binding.progressBarLayout.setVisibility(View.GONE);
                                } else {
                                    if (userData.get("provider verification status").toString().equals("failed")) {
                                        toggleState(R.drawable.icon_failed_error, getString(R.string.resend_provider_verification_message), true, false);
                                        binding.progressBarLayout.setVisibility(View.GONE);
                                    } else {
                                        binding.progressBarLayout.setVisibility(View.GONE);
                                    }
                                }
                            } else {
                                toggleState(R.drawable.icon_locked_3d, getString(R.string.no_upload_access_message), true, true);
                                binding.progressBarLayout.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        toggleState(R.drawable.icon_locked_3d, getString(R.string.no_upload_access_message), true, true);
                        binding.btnFab.setVisibility(View.GONE);
                        binding.progressBarLayout.setVisibility(View.GONE);
                    }
                } else {
                    PrepNestUtil.showToast(requireContext(), "User data not found, please login again!");
                    auth.signOut();
                    requireActivity().finishAffinity();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                PrepNestUtil.showToast(requireContext(), "An unknown error occurred : ".concat(databaseError.toString()));
                binding.progressBarLayout.setVisibility(View.GONE);
            }
        });
    }

    public void attachAdapterToRecyclerView() {
        binding.itemsList.setLayoutManager(new LinearLayoutManager(getContext()));
        listAdapter = new resourcesAdapter(this::showResourceOptionsSheet, allResources);
        binding.itemsList.setAdapter(listAdapter);
        binding.itemsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
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
            Log.d("ALL DATA", "ALL DATA IS LOADED");
            binding.progressBarLayout.setVisibility(View.GONE);
            if ((requestedResources.size() + verifiedResources.size()) == 0) {
                toggleState(R.drawable.empty_box_illus, getString(R.string.no_uploaded_resource_message), true, false);
            } else {
                binding.emptyStateLayout.setVisibility(View.GONE);
                binding.itemsLayout.setVisibility(View.VISIBLE);
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
                        Log.d("COUNT", "COUNT IS MAX FOR REQUEST");
                        Log.d("DATA", "REQUESTED RESOURCES LOADED");
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
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    PrepNestUtil.showToast(requireActivity(), "An unknown error occurred: " + error.getMessage());
                }
            }
        };
        userRequestedResources.addValueEventListener(otherListener);
    }

    public void getVerifiedResources(final ArrayList<String> _childNodes) {

        if (_childNodes == null || _childNodes.isEmpty()) {
            verifiedResourcesLoaded.set(true);
            checkIfBothLoaded();
            return;
        }

        AtomicInteger loadedCount = new AtomicInteger(0);

        for (String id : _childNodes) {

            final String finalId = id;

            Log.d("VERIFIED", "VERIFIED RESOURCES LOADING ID : " + finalId);

            DatabaseReference lookupRef = firebase_database.getReference("other")
                    .child("resource_lookup")
                    .child(finalId);

            lookupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot lookupSnap) {

                    if (!lookupSnap.exists()) {
                        // Still count it as loaded to avoid blocking the UI
                        if (loadedCount.incrementAndGet() == _childNodes.size()) {
                            verifiedResourcesLoaded.set(true);
                            checkIfBothLoaded();
                        }
                        return;
                    }

                    String courseId = lookupSnap.child("course id").getValue(String.class);

                    if (courseId == null || courseId.trim().isEmpty()) {
                        if (loadedCount.incrementAndGet() == _childNodes.size()) {
                            verifiedResourcesLoaded.set(true);
                            checkIfBothLoaded();
                        }
                        return;
                    }

                    DatabaseReference resourceRef = resources.child(courseId).child(finalId);

                    // Track whether we counted this ID
                    final boolean[] counted = {false};

                    ValueEventListener listener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot resSnap) {

                            synchronized (verifiedResources) {

                                // Remove old entry of same ID
                                verifiedResources.removeIf(map -> finalId.equals(map.get("id")));

                                if (resSnap.exists()) {
                                    Map<String, Object> data = (Map<String, Object>) resSnap.getValue();
                                    if (data != null) {
                                        data.put("id", finalId);
                                        verifiedResources.add(new HashMap<>(data));
                                    }
                                }

                                // COUNT ONLY FIRST DATA LOAD
                                if (!counted[0]) {
                                    if (loadedCount.incrementAndGet() == _childNodes.size()) {
                                        verifiedResourcesLoaded.set(true);
                                        checkIfBothLoaded();
                                    }
                                    counted[0] = true;
                                }

                                // Now update combined list if both lists are done
                                if (requestedResourcesLoaded.get() && verifiedResourcesLoaded.get()) {
                                    updateCombinedList();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (isAdded()) {
                                PrepNestUtil.showToast(requireActivity(),
                                        "Error loading data for " + finalId + ": " + error.getMessage());
                            }
                        }
                    };

                    // Use ADD VALUE LISTENER (real-time updates)
                    resourceRef.addValueEventListener(listener);

                    // Store for future removal
                    listeners.put(finalId, listener);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
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
        sheetbinding.deleteOption.setOnClickListener(_view -> {
            showLoadingDialog(true);
            resource_options.dismiss();
            deleteItemFromFirebase(_item);
        });
        sheetbinding.discontinueOption.setOnClickListener(_view -> {
            showLoadingDialog(true);
            changeResourceStatus(_item);
            resource_options.dismiss();
        });
        sheetbinding.showDetailsOption.setOnClickListener(_view -> {
            showResourceDetailsSheet(_item);
            resource_options.dismiss();
        });
        resource_options.setCancelable(true);
        resource_options.show();
    }

    public void deleteItemFromFirebase(final HashMap<String, Object> _item) {
        String id = String.valueOf(_item.get("id"));
        int length = allResources.size();
        if (!resourceIDs.contains(_item.get("id").toString())) {
            userRequestedResources.child(id).removeValue().addOnCompleteListener(aVoid -> {
                List<String> fileURLs;
                fileURLs = new Gson().fromJson(_item.get("resource urls").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
                FirebaseFileDeleter.deleteFilesFromStorage(fileURLs, requireActivity(), new FirebaseFileDeleter.DeletionCallback() {
                    @Override
                    public void onAllDeleted() {
                        if (allResources.size() == length) {
                            allResources.remove(_item);
                            listAdapter.updateData(new ArrayList<>(allResources));
                        }
                        showLoadingDialog(false);
                        PrepNestUtil.showToast(requireActivity(), "Deleted successfully!");
                        FolderUtils.deleteFolder(requireActivity(), id);
                    }

                    @Override
                    public void onError(Exception e) {
                        showLoadingDialog(false);
                        PrepNestUtil.showToast(requireActivity(), "An unknown error occurred: " + e.getMessage());
                    }
                });

            }).addOnFailureListener(e -> {
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
            navigateToResourceView(_item);
        } else {
            showLoadingDialog(true);
            boolean created = targetFolder.mkdirs();

            if (created) {

                List<String> fileURLs;
                fileURLs = new Gson().fromJson(_item.get("resource urls").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
                FilesDownloader.downloadFiles(getContext(), fileURLs, folderName, new FilesDownloader.DownloadCallback() {
                    @Override
                    public void onDownloadComplete() {
                        showLoadingDialog(false);
                        navigateToResourceView(_item);
                    }

                    @Override
                    public void onDownloadFailed(Exception e) {
                        showLoadingDialog(false);
                        boolean success = FolderUtils.deleteFolder(getContext(), folderName);
                        PrepNestUtil.showToast(getContext(), e.getCause().toString());
                    }
                });
            } else {
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), "An unknown error occurred !");
            }

        }
    }

    public void changeResourceStatus(final HashMap<String, Object> _item) {
        String id = _item.get("id").toString();
        DatabaseReference resourceItem = resources.child(id);
        boolean status = false;
        if (_item.containsKey("discontinue")) {
            status = (Boolean) _item.get("discontinue");
        }
        resourceItem.child("discontinue").setValue(!status).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), "Successfully updated!");
            } else {
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
            sheetbinding.cashValue.setText("₹".concat(String.valueOf(((Number) _item.get("price")).longValue())));
            sheetbinding.coinsValue.setText(String.valueOf(((Number) _item.get("price")).longValue() * 5).concat(" coins"));
        } else {
            sheetbinding.priceTitle.setVisibility(View.GONE);
            sheetbinding.cashValue.setVisibility(View.GONE);
            sheetbinding.coinsValue.setVisibility(View.GONE);
            sheetbinding.slash.setVisibility(View.GONE);
        }
        resource_details_sheet.show();
    }

    public void loadCourses() {
        if (jsonCourseData.isEmpty()) {
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
                toImageView.setClass(requireContext(), ImageviewActivity.class);
                toImageView.putExtra("id", _item.get("id").toString());
                startActivity(toImageView);
            } else {
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


    public interface onResourceActionListener {
        void showOptions(HashMap<String, Object> item);
    }

    public static class FirebaseFileDeleter {
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

        public interface DeletionCallback {
            void onAllDeleted();

            void onError(Exception e);
        }
    }

    public static class FilesDownloader {
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

                String fileName = storagePath.substring(storagePath.indexOf("/"));

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

        public interface DownloadCallback {
            void onDownloadComplete();

            void onDownloadFailed(Exception e);
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

    public class resourcesAdapter extends RecyclerView.Adapter<resourcesAdapter.ViewHolder> {

        private final ArrayList<HashMap<String, Object>> list;
        private final onResourceActionListener listener;
        private int _lastPosition = -1; // Used to track animations


        public resourcesAdapter(onResourceActionListener listener, ArrayList<HashMap<String, Object>> list) {
            this.list = list;
            this.listener = listener;
            setHasStableIds(true);
        }

        public void updateData(List<HashMap<String, Object>> newData) {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ResourceDiffCallback(list, newData));
            list.clear();
            list.addAll(newData);
            result.dispatchUpdatesTo(this);
        }

        @Override
        public long getItemId(int position) {
            Object id = list.get(position).get("id");
            return id != null ? id.toString().hashCode() : position;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ResourceItemCardFullBinding resourceItemCardFullBinding = ResourceItemCardFullBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );

            return new ViewHolder(resourceItemCardFullBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            // UI design
            holder.binding.iconMoreOptions.setVisibility(View.VISIBLE);
            PrepNestUtil.roundViewWithRipple(holder.binding.container, "#FAFAFA", 20, 3, "#EEEEEE", "#E0E0E0");
            holder.binding.uploadDateTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            holder.binding.bestChoiceTag.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFF000000));
            holder.binding.recommendedTag.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFF000000));
            // UI End


            HashMap<String, Object> item = list.get(position);

            if (item.containsKey("status")) {
                holder.binding.statusTxt.setText(item.get("status").toString());
                holder.binding.statusTxt.setVisibility(View.VISIBLE);
                switch (item.get("status").toString()) {
                    case "pending":
                        holder.binding.statusTxt.setTextColor(0xFFF57C00);
                        holder.binding.statusTxt.setBackground(new GradientDrawable() {
                            public GradientDrawable getIns(int a, int b) {
                                this.setCornerRadius(a);
                                this.setColor(b);
                                return this;
                            }
                        }.getIns((int) 360, 0xFFFFF3E0));
                        break;

                    case "failed":
                        holder.binding.statusTxt.setTextColor(0xFFE53935);
                        holder.binding.statusTxt.setBackground(new GradientDrawable() {
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
                                holder.binding.statusTxt.setBackground(new GradientDrawable() {
                                    public GradientDrawable getIns(int a, int b) {
                                        this.setCornerRadius(a);
                                        this.setColor(b);
                                        return this;
                                    }
                                }.getIns((int) 360, 0xFFF5F5F5));
                                holder.binding.statusTxt.setTextColor(0xFF757575);
                                holder.binding.statusTxt.setText("discontinue");
                                holder.binding.statusTxt.setVisibility(View.VISIBLE);
                            } else {
                                holder.binding.statusTxt.setVisibility(View.GONE);
                            }
                        } else {
                            holder.binding.statusTxt.setVisibility(View.GONE);
                        }
                        break;

                    default:
                        holder.binding.statusTxt.setTextColor(0xFF757575);
                        holder.binding.statusTxt.setVisibility(View.VISIBLE);
                }
            } else {
                holder.binding.statusTxt.setVisibility(View.VISIBLE);
                holder.binding.statusTxt.setText("pending");
                holder.binding.statusTxt.setTextColor(0xFFF57C00);
                holder.binding.statusTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFFFF3E0));
            }

            if (item.containsKey("subject")) {
                holder.binding.subjectNameTxt.setText(item.get("subject").toString());
                holder.binding.subjectNameTxt.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFFF5F5F5));
                holder.binding.subjectNameTxt.setVisibility(View.VISIBLE);

                ViewGroup.MarginLayoutParams bottomContainerParams =
                        (ViewGroup.MarginLayoutParams) holder.binding.statusAndDateContainer.getLayoutParams();

                if (item.get("subject").toString().length() >= 20) {
                    holder.binding.metaDataContainer.setOrientation(LinearLayout.VERTICAL);
                    bottomContainerParams.setMargins(0, (int) convertToDp(8), 0, 0);
                } else {
                    bottomContainerParams.setMargins(0, 0, 0, 0);
                    holder.binding.metaDataContainer.setOrientation(LinearLayout.HORIZONTAL);
                }
                holder.binding.statusAndDateContainer.setLayoutParams(bottomContainerParams);
            } else {
                holder.binding.subjectNameTxt.setText("None");
                holder.binding.subjectNameTxt.setVisibility(View.VISIBLE);
            }


            if (item.containsKey("resource title")) {
                holder.binding.title.setText(item.get("resource title").toString());
            } else {
                holder.binding.title.setText("No name");
            }

            if (item.containsKey("type")) {
                if (item.get("type").toString().equals("paper")) {
                    holder.binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    holder.binding.image.setImageResource(R.drawable.short_notes);
                }
            } else {
                holder.binding.image.setVisibility(View.INVISIBLE);
            }
            if (item.containsKey("best choice")) {
                if (item.get("best choice").toString().equals("true")) {
                    holder.binding.bestChoiceTag.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.bestChoiceTag.setVisibility(View.GONE);
                }
            } else {
                holder.binding.bestChoiceTag.setVisibility(View.GONE);
            }
            if (item.containsKey("recommended")) {
                if (item.get("recommended").toString().equals("true")) {
                    holder.binding.recommendedTag.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.recommendedTag.setVisibility(View.GONE);
                }
            } else {
                holder.binding.recommendedTag.setVisibility(View.GONE);
            }
            if (item.containsKey("date of upload")) {
                holder.binding.uploadDateTxt.setText(formatTimeDifference(item.get("date of upload").toString()));
                holder.binding.uploadDateTxt.setVisibility(View.VISIBLE);
            } else {
                holder.binding.uploadDateTxt.setVisibility(View.GONE);
            }


            ViewGroup.MarginLayoutParams paramscontainer =
                    (ViewGroup.MarginLayoutParams) holder.binding.container.getLayoutParams();

            if (position == (list.size() - 1)) {
                paramscontainer.setMargins(
                        (int) convertToDp(20),
                        (int) convertToDp(10),
                        (int) convertToDp(20),
                        (int) convertToDp(10)
                );
            } else {
                paramscontainer.setMargins(
                        (int) convertToDp(20),
                        (int) convertToDp(10),
                        (int) convertToDp(20),
                        0
                );
            }

            paramscontainer.width = ViewGroup.LayoutParams.MATCH_PARENT;
            paramscontainer.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            holder.binding.container.setLayoutParams(paramscontainer);


            holder.binding.iconMoreOptions.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.showOptions(list.get(pos));
                }
            });


            holder.binding.container.setOnClickListener(_view1 -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    openResource(list.get(pos));
                }
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
            ResourceItemCardFullBinding binding;
            public ViewHolder(ResourceItemCardFullBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
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
}
