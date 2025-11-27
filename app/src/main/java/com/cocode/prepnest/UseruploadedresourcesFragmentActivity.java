package com.cocode.prepnest;

import android.annotation.SuppressLint;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final DatabaseReference requests = firebase_database.getReference("requests/newResourcesRequests");
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
    private String jsonCourseData = null;
    private ArrayList<String> resourceIDs = new ArrayList<>();
    private ProgressDialog progress_dialog;

    public static String extractStoragePathFromURL(String url) {
        try {
            Pattern pattern = Pattern.compile("/o/(.+?)\\?");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String encodedPath = matcher.group(1);
                return URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValue(HashMap<String, Object> map, String key) {
        return (T) map.get(key);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
        binding = UseruploadedresourcesFragmentBinding.inflate(_inflater, _container, false);
        initialize(_savedInstanceState, binding.getRoot());
        FirebaseApp.initializeApp(requireContext());
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
        assert auth.getCurrentUser() != null;
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
            userRequestedResources.child(id).removeEventListener(Objects.requireNonNull(listeners.get(id)));
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
        assert auth.getCurrentUser() != null;
        DatabaseReference dataRef = users.child(auth.getCurrentUser().getUid());
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userData = dataSnapshot.getValue(new GenericTypeIndicator<>() {
                });

                if (dataSnapshot.exists()) {
                    if (userData.containsKey("isProvider")) {
                        if (Boolean.parseBoolean(Objects.requireNonNull(userData.get("isProvider")).toString())) {
                            binding.btnFab.setVisibility(View.VISIBLE);
                            resourceIDs = new ArrayList<>();
                            if (userData.containsKey("uploadedResources")) {
                                if ((userData.get("uploadedResources") == null) || Objects.requireNonNull(userData.get("uploadedResources")).toString().isEmpty()) {
                                    toggleState(R.drawable.icon_empty_box, getString(R.string.no_uploaded_resource_message), true, false);
                                } else {
                                    resourceIDs = getValue(userData, "uploadedResources");
                                    toggleState(R.drawable.icon_empty_box, getString(R.string.no_uploaded_resource_message), false, false);
                                }
                            } else {
                                toggleState(R.drawable.icon_empty_box, getString(R.string.no_uploaded_resource_message), true, false);
                            }
                            getVerifiedResources(resourceIDs);
                            getRequestedResources();
                        } else {
                            binding.btnFab.setVisibility(View.GONE);
                            if (userData.containsKey("providerVerificationStatus")) {
                                if (Objects.requireNonNull(userData.get("providerVerificationStatus")).toString().equals("pending")) {
                                    toggleState(R.drawable.icon_hourglass, getString(R.string.pending_provider_verification_message), true, false);
                                    binding.progressBarLayout.setVisibility(View.GONE);
                                } else {
                                    if (Objects.requireNonNull(userData.get("providerVerificationStatus")).toString().equals("failed")) {
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
                ArrayList<HashMap<String, Object>> updatedList = getUpdatedList();

                allResources.clear();
                allResources.addAll(updatedList);
                ListMapUtils.sortListByKey(allResources, "dateOfUpload", false, ListMapUtils.SortType.NUMBER);
                listAdapter.updateData(new ArrayList<>(allResources));
                listAdapter.notifyItemInserted(0);
            }
        });
    }

    @NonNull
    private ArrayList<HashMap<String, Object>> getUpdatedList() {
        ArrayList<HashMap<String, Object>> updatedList = new ArrayList<>();

        for (int index = requestedResources.size() - 1; index >= 0; index--) {
            HashMap<String, Object> item = requestedResources.get(index);
            updatedList.add(item);
        }

        for (int index = verifiedResources.size() - 1; index >= 0; index--) {
            HashMap<String, Object> item = verifiedResources.get(index);
            updatedList.add(item);
        }
        return updatedList;
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
            DatabaseReference resourceRef = resources.child(id);

            ValueEventListener listener = new ValueEventListener() {

                boolean firstLoad = true;

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
                        if (firstLoad) {
                            if (loadedCount.incrementAndGet() == _childNodes.size()) {
                                verifiedResourcesLoaded.set(true);
                                checkIfBothLoaded();
                            }
                            firstLoad = false;
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

                Objects.requireNonNull(progress_dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

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
        com.google.android.material.bottomsheet.BottomSheetDialog resourceOptions;
        resourceOptions = new com.google.android.material.bottomsheet.BottomSheetDialog(requireActivity());
        UserResourceOptionsSheetLayoutBinding sheetBinding = UserResourceOptionsSheetLayoutBinding.inflate(requireActivity().getLayoutInflater());

        resourceOptions.setContentView(sheetBinding.getRoot());

        resourceOptions.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetBinding.container.setBackground(gd);
        PrepNestUtil.roundViewWithRipple(sheetBinding.deleteOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(sheetBinding.discontinueOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(sheetBinding.showDetailsOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        if (_item.containsKey("status")) {
            if (Objects.requireNonNull(_item.get("status")).toString().equals("verified")) {
                sheetBinding.deleteOption.setVisibility(View.GONE);
                sheetBinding.discontinueOption.setVisibility(View.VISIBLE);
            } else {
                sheetBinding.deleteOption.setVisibility(View.VISIBLE);
                sheetBinding.discontinueOption.setVisibility(View.GONE);
            }
        } else {
            sheetBinding.deleteOption.setVisibility(View.VISIBLE);
            sheetBinding.discontinueOption.setVisibility(View.GONE);
        }
        if (_item.containsKey("isDiscontinue")) {
            if (Boolean.parseBoolean(Objects.requireNonNull(_item.get("isDiscontinue")).toString())) {
                sheetBinding.discontinueTxt.setText("Continue");
            } else {
                sheetBinding.discontinueTxt.setText("Discontinue");
            }
        } else {
            sheetBinding.discontinueTxt.setText("Discontinue");
        }
        sheetBinding.deleteOption.setOnClickListener(_view -> {
            showLoadingDialog(true);
            resourceOptions.dismiss();
            deleteItemFromFirebase(_item);
        });
        sheetBinding.discontinueOption.setOnClickListener(_view -> {
            showLoadingDialog(true);
            changeResourceStatus(_item);
            resourceOptions.dismiss();
        });
        sheetBinding.showDetailsOption.setOnClickListener(_view -> {
            showResourceDetailsSheet(_item);
            resourceOptions.dismiss();
        });
        resourceOptions.setCancelable(true);
        resourceOptions.show();
    }

    public void deleteItemFromFirebase(final HashMap<String, Object> _item) {
        String id = String.valueOf(_item.get("id"));
        int length = allResources.size();
        if (!resourceIDs.contains(Objects.requireNonNull(_item.get("id")).toString())) {
            userRequestedResources.child(id).removeValue().addOnCompleteListener(aVoid -> {
                List<String> fileURLs;
                fileURLs = new Gson().fromJson(Objects.requireNonNull(_item.get("resourceUrls")).toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
                FirebaseFileDeleter.deleteFilesFromStorage(fileURLs, requireActivity(), new FirebaseFileDeleter.DeletionCallback() {
                    @Override
                    public void onAllDeleted() {
                        if (allResources.size() == length) {
                            allResources.remove(_item);
                            listAdapter.notifyItemRemoved(Integer.parseInt(Objects.requireNonNull(_item.get("position")).toString()));
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
        File internalDir = requireContext().getFilesDir();
        String folderName = Objects.requireNonNull(_item.get("id")).toString();
        File targetFolder = new File(internalDir, folderName);
        if (targetFolder.exists()) {
            navigateToResourceView(_item);
        } else {
            showLoadingDialog(true);
            boolean created = targetFolder.mkdirs();

            if (created) {

                List<String> fileURLs;
                fileURLs = new Gson().fromJson(Objects.requireNonNull(_item.get("resourceUrls")).toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
                FilesDownloader.downloadFiles(requireContext(), fileURLs, folderName, new FilesDownloader.DownloadCallback() {
                    @Override
                    public void onDownloadComplete() {
                        showLoadingDialog(false);
                        navigateToResourceView(_item);
                    }

                    @Override
                    public void onDownloadFailed(Exception e) {
                        showLoadingDialog(false);
                        boolean success = FolderUtils.deleteFolder(requireContext(), folderName);
                        PrepNestUtil.showToast(requireContext(), Objects.requireNonNull(e.getCause()).toString());
                    }
                });
            } else {
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), "An unknown error occurred !");
            }

        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void changeResourceStatus(final HashMap<String, Object> _item) {
        String id = Objects.requireNonNull(_item.get("id")).toString();
        DatabaseReference resourceItem = resources.child(id);
        boolean status = false;
        if (_item.containsKey("isDiscontinue")) {
            status = Boolean.parseBoolean(Objects.requireNonNull(_item.get("isDiscontinue")).toString());
        }
        resourceItem.child("isDiscontinue").setValue(!status).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), "Successfully updated!");
                listAdapter.notifyItemChanged(Integer.parseInt(Objects.requireNonNull(_item.get("position")).toString()));
            } else {
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), task.getException() != null ? Objects.requireNonNull(task.getException().getCause()).toString() : "An unknown error occurred");
            }
        });
    }

    public void showResourceDetailsSheet(final HashMap<String, Object> _item) {
        com.google.android.material.bottomsheet.BottomSheetDialog resourceDetailsSheet;
        resourceDetailsSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireActivity());
        ResourceDetailsSheetLayoutBinding sheetBinding = ResourceDetailsSheetLayoutBinding.inflate(requireActivity().getLayoutInflater());

        resourceDetailsSheet.setContentView(sheetBinding.getRoot());

        resourceDetailsSheet.setOnShowListener(dialog -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog;
            View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
            }
        });

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor("#FFFFFF"));
        gd.setCornerRadii(new float[]{30, 30, 30, 30, 0, 0, 0, 0});
        sheetBinding.container.setBackground(gd);
        if (_item.containsKey("resourceTitle")) {
            sheetBinding.resourceTitle.setText(Objects.requireNonNull(_item.get("resourceTitle")).toString());
        } else {
            sheetBinding.resourceTitle.setText("No title");
        }
        if (_item.containsKey("courseId")) {
            sheetBinding.resourceCourse.setText(getCourseName(Objects.requireNonNull(_item.get("courseId")).toString()));
        } else {
            sheetBinding.resourceCourse.setText("None");
        }
        if (_item.containsKey("session")) {
            sheetBinding.sessionValueTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetBinding.sessionValueTxt.setText(Objects.requireNonNull(_item.get("session")).toString());
        } else {
            sheetBinding.sessionContainer.setVisibility(View.GONE);
        }
        if (_item.containsKey("semester")) {
            sheetBinding.semesterValueTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetBinding.semesterValueTxt.setText(getFormattedNumber(((Number) Objects.requireNonNull(_item.get("semester"))).doubleValue()).concat(" semester"));
        } else {
            sheetBinding.semesterContainer.setVisibility(View.GONE);
        }
        if (_item.containsKey("subject")) {
            sheetBinding.subjectValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetBinding.subjectValue.setText(Objects.requireNonNull(_item.get("subject")).toString());
        } else {
            sheetBinding.subjectContainer.setVisibility(View.GONE);
        }
        if (_item.containsKey("subtype")) {
            sheetBinding.subtypeValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            if (Objects.requireNonNull(_item.get("subtype")).toString().equals("midterm")) {
                sheetBinding.subtypeValue.setText("Midterm");
            } else {
                if (Objects.requireNonNull(_item.get("subtype")).toString().equals("semester")) {
                    sheetBinding.subtypeValue.setText("Semester");
                } else {
                    sheetBinding.subtypeContainer.setVisibility(View.GONE);
                }
            }
        } else {
            sheetBinding.subtypeContainer.setVisibility(View.GONE);
        }
		/*
if (_item.containsKey("rating")) {
sheetBinding.ratingContainer.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)360, 0xFFFFF3E0));
sheetBinding.ratingTxt.setText(_item.get("rating").toString());
} else {
sheetBinding.ratingTitle.setVisibility(View.GONE);
sheetBinding.ratingContainer.setVisibility(View.GONE);
}
*/
        if (_item.containsKey("dateOfUpload")) {
            sheetBinding.dateOfUploadValue.setText(formatTimeDifference(Objects.requireNonNull(_item.get("dateOfUpload")).toString()));
            sheetBinding.dateOfUploadValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
        } else {
            sheetBinding.dateOfUploadTitle.setVisibility(View.GONE);
            sheetBinding.dateOfUploadValue.setVisibility(View.GONE);
        }
        if (_item.containsKey("isBestChoice")) {
            if (Boolean.parseBoolean(Objects.requireNonNull(_item.get("isBestChoice")).toString())) {
                sheetBinding.bestChoiceTag.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFF000000));
                sheetBinding.bestChoiceTag.setVisibility(View.VISIBLE);
            } else {
                sheetBinding.bestChoiceTag.setVisibility(View.GONE);
            }
        } else {
            sheetBinding.bestChoiceTag.setVisibility(View.GONE);
        }
        if (_item.containsKey("isRecommended")) {
            if (Boolean.parseBoolean(Objects.requireNonNull(_item.get("isRecommended")).toString())) {
                sheetBinding.recommendedTag.setBackground(new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns((int) 360, 0xFF000000));
                sheetBinding.recommendedTag.setVisibility(View.VISIBLE);
            } else {
                sheetBinding.recommendedTag.setVisibility(View.GONE);
            }
        } else {
            sheetBinding.recommendedTag.setVisibility(View.GONE);
        }
        if (_item.containsKey("price")) {
            sheetBinding.cashValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetBinding.coinsValue.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            sheetBinding.cashValue.setText("₹".concat(String.valueOf(((Number) Objects.requireNonNull(_item.get("price"))).longValue())));
            sheetBinding.coinsValue.setText(String.valueOf(((Number) Objects.requireNonNull(_item.get("price"))).longValue() * 5).concat(" coins"));
        } else {
            sheetBinding.priceTitle.setVisibility(View.GONE);
            sheetBinding.cashValue.setVisibility(View.GONE);
            sheetBinding.coinsValue.setVisibility(View.GONE);
            sheetBinding.slash.setVisibility(View.GONE);
        }
        resourceDetailsSheet.show();
    }

    public void loadCourses() {
        if (jsonCourseData == null || jsonCourseData.isEmpty()) {
            try {
                InputStream is = requireContext().getAssets().open("courses.json");
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                jsonCourseData = new String(buffer, StandardCharsets.UTF_8);
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
            if (Objects.requireNonNull(_item.get("type")).toString().equals("paper")) {
                toImageView.setClass(requireContext(), ImageviewActivity.class);
                toImageView.putExtra("id", Objects.requireNonNull(_item.get("id")).toString());
                startActivity(toImageView);
            } else {
                toPDFView.putExtra("id", Objects.requireNonNull(_item.get("id")).toString());
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
                    StorageReference fileRef = firebase_storage.getReference().child(Objects.requireNonNull(extractStoragePathFromURL(url)));
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
                assert storagePath != null;
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
                holder.binding.statusTxt.setText(Objects.requireNonNull(item.get("status")).toString());
                holder.binding.statusTxt.setVisibility(View.VISIBLE);
                switch (Objects.requireNonNull(item.get("status")).toString()) {
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
                        if (item.containsKey("isDiscontinue")) {
                            if (Boolean.parseBoolean(Objects.requireNonNull(item.get("isDiscontinue")).toString())) {
                                holder.binding.statusTxt.setBackground(new GradientDrawable() {
                                    public GradientDrawable getIns(int a, int b) {
                                        this.setCornerRadius(a);
                                        this.setColor(b);
                                        return this;
                                    }
                                }.getIns((int) 360, 0xFFF5F5F5));
                                holder.binding.statusTxt.setTextColor(0xFF757575);
                                holder.binding.statusTxt.setText("Discontinue");
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
                holder.binding.subjectNameTxt.setText(Objects.requireNonNull(item.get("subject")).toString());
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

                if (Objects.requireNonNull(item.get("subject")).toString().length() >= 20) {
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


            if (item.containsKey("resourceTitle")) {
                holder.binding.title.setText(Objects.requireNonNull(item.get("resourceTitle")).toString());
            } else {
                holder.binding.title.setText("No name");
            }

            if (item.containsKey("type")) {
                if (Objects.requireNonNull(item.get("type")).toString().equals("paper")) {
                    holder.binding.image.setImageResource(R.drawable.previous_paper);
                } else {
                    holder.binding.image.setImageResource(R.drawable.short_notes);
                }
            } else {
                holder.binding.image.setVisibility(View.INVISIBLE);
            }
            if (item.containsKey("isBestChoice")) {
                if (Boolean.parseBoolean(Objects.requireNonNull(item.get("isBestChoice")).toString())) {
                    holder.binding.bestChoiceTag.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.bestChoiceTag.setVisibility(View.GONE);
                }
            } else {
                holder.binding.bestChoiceTag.setVisibility(View.GONE);
            }
            if (item.containsKey("isRecommended")) {
                if (Boolean.parseBoolean(Objects.requireNonNull(item.get("isRecommended")).toString())) {
                    holder.binding.recommendedTag.setVisibility(View.VISIBLE);
                } else {
                    holder.binding.recommendedTag.setVisibility(View.GONE);
                }
            } else {
                holder.binding.recommendedTag.setVisibility(View.GONE);
            }
            if (item.containsKey("dateOfUpload")) {
                holder.binding.uploadDateTxt.setText(formatTimeDifference(Objects.requireNonNull(item.get("dateOfUpload")).toString()));
                holder.binding.uploadDateTxt.setVisibility(View.VISIBLE);
            } else {
                holder.binding.uploadDateTxt.setVisibility(View.GONE);
            }


            ViewGroup.MarginLayoutParams paramsContainer =
                    (ViewGroup.MarginLayoutParams) holder.binding.container.getLayoutParams();

            if (position == (list.size() - 1)) {
                paramsContainer.setMargins(
                        (int) convertToDp(20),
                        (int) convertToDp(10),
                        (int) convertToDp(20),
                        (int) convertToDp(10)
                );
            } else {
                paramsContainer.setMargins(
                        (int) convertToDp(20),
                        (int) convertToDp(10),
                        (int) convertToDp(20),
                        0
                );
            }

            paramsContainer.width = ViewGroup.LayoutParams.MATCH_PARENT;
            paramsContainer.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            holder.binding.container.setLayoutParams(paramsContainer);


            holder.binding.iconMoreOptions.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    HashMap<String, Object> itemTemp = list.get(pos);
                    itemTemp.put("position", pos);
                    listener.showOptions(itemTemp);
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
