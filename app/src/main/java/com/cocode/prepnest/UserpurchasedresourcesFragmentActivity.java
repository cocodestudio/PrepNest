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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cocode.prepnest.databinding.ResourceDetailsSheetLayoutBinding;
import com.cocode.prepnest.databinding.ResourceItemCardFullBinding;
import com.cocode.prepnest.databinding.UserResourceOptionsSheetLayoutBinding;
import com.cocode.prepnest.databinding.UserpurchasedresourcesFragmentBinding;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UserpurchasedresourcesFragmentActivity extends Fragment {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference users = database.getReference("users");
    private final DatabaseReference resources = database.getReference("resources");
    private final ArrayList<HashMap<String, Object>> resourcesList = new ArrayList<>();
    private final Intent toImageView = new Intent();
    private final Intent toPDFView = new Intent();
    private UserpurchasedresourcesFragmentBinding binding;
    private resourcesAdapter listAdapter;
    private String jsonCourseData = null;
    private ProgressDialog progress_dialog;
    private ArrayList<String> resourceIDs = new ArrayList<>();

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
        binding = UserpurchasedresourcesFragmentBinding.inflate(_inflater, _container, false);
        initialize(_savedInstanceState, binding.getRoot());
        FirebaseApp.initializeApp(requireContext());
        initializeLogic();
        return binding.getRoot();
    }

    private void initialize(Bundle _savedInstanceState, View _view) {
    }

    private void initializeLogic() {
        attachAdapterToRecyclerView();
        getPurchasedResourcesIDs();
        loadAllCoursesFromJson(requireContext());

        PrepNestUtil.changeNavBarColor(requireActivity(), true);
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

    public void toggleEmptyState() {
        if (resourceIDs != null && (!resourceIDs.isEmpty())) {
            binding.progressBarLayout.setVisibility(View.GONE);
            binding.emptyStateContainer.setVisibility(View.GONE);
            binding.itemsLayout.setVisibility(View.VISIBLE);
        } else {
            binding.progressBarLayout.setVisibility(View.GONE);
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
            binding.itemsLayout.setVisibility(View.GONE);
        }
    }

    public void getPurchasedResourcesIDs() {
        binding.progressBarLayout.setVisibility(View.VISIBLE);
        assert auth.getCurrentUser() != null;
        DatabaseReference dataRef = users.child(auth.getCurrentUser().getUid()).child("purchasedResources");
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {

            final GenericTypeIndicator<ArrayList<String>> typeIndicator = new GenericTypeIndicator<>() {
            };

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && (dataSnapshot.getValue() != null && !Objects.requireNonNull(dataSnapshot.getValue(typeIndicator)).isEmpty())) {
                    resourceIDs = dataSnapshot.getValue(typeIndicator);
                } else {
                    resourceIDs = new ArrayList<>();
                }
                getPurchasedResources(resourceIDs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                PrepNestUtil.showToast(requireContext(), "An unknown error occurred : ".concat(databaseError.toString()));
                binding.progressBarLayout.setVisibility(View.GONE);
                binding.emptyStateContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showPurchasedResourcesUI() {
        binding.progressBarLayout.setVisibility(View.GONE);

        if (resourcesList.isEmpty()) {
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
            binding.itemsLayout.setVisibility(View.GONE);
        } else {
            binding.emptyStateContainer.setVisibility(View.GONE);
            binding.itemsLayout.setVisibility(View.VISIBLE);
            loadResourceItemsToList();
        }
    }


    public void getPurchasedResources(final ArrayList<String> _childNodes) {

        // Case: null or empty list
        if (_childNodes == null || _childNodes.isEmpty()) {
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
            binding.progressBarLayout.setVisibility(View.GONE);
            return;
        }

        AtomicInteger loadedCount = new AtomicInteger(0);

        for (String id : _childNodes) {
            final String finalId = id;

            DatabaseReference resourceRef = resources.child(finalId);

            resourceRef.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot resourceSnap) {

                    synchronized (resourcesList) {
                        // Remove any existing entry of same ID
                        resourcesList.removeIf(map -> finalId.equals(map.get("id")));

                        if (resourceSnap.exists()) {
                            Map<String, Object> item =
                                    (Map<String, Object>) resourceSnap.getValue();

                            if (item != null) {
                                item.put("id", finalId);
                                resourcesList.add(0, new HashMap<>(item));
                            }
                        }
                    }

                    if (loadedCount.incrementAndGet() == _childNodes.size()) {
                        showPurchasedResourcesUI();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    binding.progressBarLayout.setVisibility(View.GONE);
                    binding.emptyStateContainer.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            requireActivity(),
                            "Failed loading resources: " + error.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                    // Still count load so UI doesn't freeze
                    if (loadedCount.incrementAndGet() == _childNodes.size()) {
                        showPurchasedResourcesUI();
                    }
                }
            });
        }
    }


    public void attachAdapterToRecyclerView() {
        binding.itemsList.setLayoutManager(new LinearLayoutManager(getContext()));
        listAdapter = new resourcesAdapter(this::showResourceOptionsSheet, resourcesList);
        binding.itemsList.setAdapter(listAdapter);
    }

    public void showResourceOptionsSheet(final HashMap<String, Object> _item) {
        com.google.android.material.bottomsheet.BottomSheetDialog resource_options;
        resource_options = new com.google.android.material.bottomsheet.BottomSheetDialog(requireActivity());
        UserResourceOptionsSheetLayoutBinding sheetBinding = UserResourceOptionsSheetLayoutBinding.inflate(Objects.requireNonNull(getActivity()).getLayoutInflater());

        resource_options.setContentView(sheetBinding.getRoot());

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
        sheetBinding.container.setBackground(gd);
        PrepNestUtil.roundViewWithRipple(sheetBinding.deleteOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(sheetBinding.discontinueOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        PrepNestUtil.roundViewWithRipple(sheetBinding.showDetailsOption, "#FFFFFF", 0, 0, "#000000", "#EEEEEE");
        sheetBinding.deleteOption.setVisibility(View.VISIBLE);
        sheetBinding.discontinueOption.setVisibility(View.GONE);
        sheetBinding.showDetailsOption.setVisibility(View.VISIBLE);
        sheetBinding.deleteOption.setOnClickListener(_view -> {
            showLoadingDialog(true);
            resource_options.dismiss();
            deleteItemFromFirebase(_item);
        });
        sheetBinding.showDetailsOption.setOnClickListener(_view -> {
            showResourceDetailsSheet(_item);
            resource_options.dismiss();
        });
        resource_options.setCancelable(true);
        resource_options.show();
    }

    public void loadResourceItemsToList() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            synchronized (resourcesList) {

                ArrayList<HashMap<String, Object>> updatedList = new ArrayList<>(resourcesList);

                resourcesList.clear();
                resourcesList.addAll(updatedList);
                listAdapter.updateData(new ArrayList<>(resourcesList));
            }
        });
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

    public void deleteItemFromFirebase(final HashMap<String, Object> _item) {
        if (_item == null) {
            showLoadingDialog(false);
            return;
        }
        final String folderName = Objects.requireNonNull(_item.get("id")).toString();
        for (int pos = 0; pos < resourceIDs.size(); pos++) {
            if (resourceIDs.get(pos).equals(Objects.requireNonNull(_item.get("id")).toString())) {
                resourceIDs.remove(pos);
                break;
            }
        }
//        String newIDs = new Gson().toJson(resourceIDs);
        assert auth.getCurrentUser() != null;
        users.child(auth.getCurrentUser().getUid()).child("purchasedResources").setValue(resourceIDs).addOnCompleteListener(unused -> {
            showLoadingDialog(false);
            resourcesList.remove(_item);
            listAdapter.notifyItemRemoved(Integer.parseInt(Objects.requireNonNull(_item.get("position")).toString()));
            loadResourceItemsToList();
            toggleEmptyState();
            PrepNestUtil.showToast(requireActivity(), "Deleted successfully!");
            FolderUtils.deleteFolder(Objects.requireNonNull(getContext()), folderName);
        }).addOnFailureListener(error -> {
            showLoadingDialog(false);
            PrepNestUtil.showToast(requireActivity(), "Failed to delete: " + error.getMessage());
        });
    }

    public void openResource(final HashMap<String, Object> _item) {
        File internalDir = Objects.requireNonNull(getContext()).getFilesDir();
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
                FilesDownloader.downloadFiles(getContext(), fileURLs, folderName, new FilesDownloader.DownloadCallback() {
                    @Override
                    public void onDownloadComplete() {
                        showLoadingDialog(false);
                        navigateToResourceView(_item);
                    }

                    @Override
                    public void onDownloadFailed(Exception e) {
                        showLoadingDialog(false);
                        boolean success = FolderUtils.deleteFolder(Objects.requireNonNull(getContext()), folderName);
                        PrepNestUtil.showToast(getContext(), Objects.requireNonNull(e.getCause()).toString());
                    }
                });
            } else {
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), "An unknown error occurred !");
            }
        }
    }

    public void showResourceDetailsSheet(final HashMap<String, Object> _item) {
        com.google.android.material.bottomsheet.BottomSheetDialog resource_details_sheet;
        resource_details_sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireActivity());
        ResourceDetailsSheetLayoutBinding sheetBinding = ResourceDetailsSheetLayoutBinding.inflate(Objects.requireNonNull(getActivity()).getLayoutInflater());

        resource_details_sheet.setContentView(sheetBinding.getRoot());

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
        resource_details_sheet.show();
    }

    public void loadAllCoursesFromJson(Context context) {
        try (InputStream is = context.getAssets().open("courses.json");
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }

            jsonCourseData = bos.toString("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            holder.binding.subjectNameTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            holder.binding.sessionTxt.setBackground(new GradientDrawable() {
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

            if (item.containsKey("subject")) {
                holder.binding.subjectNameTxt.setText(Objects.requireNonNull(item.get("subject")).toString());
                holder.binding.subjectNameTxt.setVisibility(View.VISIBLE);

                ViewGroup.MarginLayoutParams sessionTxtParams =
                        (ViewGroup.MarginLayoutParams) holder.binding.sessionTxt.getLayoutParams();

                if (Objects.requireNonNull(item.get("subject")).toString().length() >= 20) {
                    holder.binding.subAndSessionContainer.setOrientation(LinearLayout.VERTICAL);
                    sessionTxtParams.setMargins(0, (int) convertToDp(8), 0, 0);
                } else {
                    sessionTxtParams.setMargins(0, 0, 0, 0);
                    holder.binding.subAndSessionContainer.setOrientation(LinearLayout.HORIZONTAL);
                }
                holder.binding.sessionTxt.setLayoutParams(sessionTxtParams);
            } else {
                holder.binding.subjectNameTxt.setVisibility(View.GONE);
            }

            if (item.containsKey("session")) {
                holder.binding.sessionTxt.setText(Objects.requireNonNull(item.get("session")).toString());
                holder.binding.sessionTxt.setVisibility(View.VISIBLE);
            } else {
                holder.binding.sessionTxt.setVisibility(View.GONE);
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
