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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UserpurchasedresourcesFragmentActivity extends Fragment {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase firebase_database = FirebaseDatabase.getInstance();
    private final DatabaseReference users = firebase_database.getReference("users");
    private final DatabaseReference resources = firebase_database.getReference("resources");
    private final ArrayList<HashMap<String, Object>> resourcesList = new ArrayList<>();
    private final Intent toImageView = new Intent();
    private final Intent toPDFView = new Intent();
    private UserpurchasedresourcesFragmentBinding binding;
    private resourcesAdapter listAdapter;
    private LogUtils logFile;
    private String jsonCourseData = "";
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
        FirebaseApp.initializeApp(getContext());
        initializeLogic();
        return binding.getRoot();
    }

    private void initialize(Bundle _savedInstanceState, View _view) {
    }

    private void initializeLogic() {
        logFile = new LogUtils(this);
        logFile.addFragment();
        attachAdapterToRecyclerView();
        logFile.addLog("RESOURCES", "LOADING RESOURCES");
        getPurchasedResourcesIDs();
        loadCourses();

        PrepNestUtil.changeNavBarColor(requireActivity(), true);
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
        logFile.addLog("RESOURCES", "GETTING RESOURCES IDS");
        DatabaseReference dataRef = users.child(auth.getCurrentUser().getUid()).child("purchased resources");
        dataRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && (dataSnapshot.getValue() != null && !dataSnapshot.getValue(String.class).isEmpty())) {
                    resourceIDs = new Gson().fromJson(dataSnapshot.getValue(String.class), new TypeToken<ArrayList<String>>() {
                    }.getType());
                    logFile.addLog("RESOURCES", "GOT SUCCESSFULLY");
                } else {
                    resourceIDs = new ArrayList<>();
                    logFile.addLog("RESOURCES", "NO IDS FOUND");
                }
                logFile.addLog("RESOURCES", "LOADING RESOURCES DATA");
                getPurchasedResources(resourceIDs);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                logFile.addLog("RESOURCES", "FAILED TO GET IDS : ".concat(databaseError.toString()));
                PrepNestUtil.showToast(requireContext(), "An unknown error occurred : ".concat(databaseError.toString()));
                binding.progressBarLayout.setVisibility(View.GONE);
                binding.emptyStateContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    public void getPurchasedResources(final ArrayList<String> _childNodes) {
        if (_childNodes != null && _childNodes.isEmpty()) {
            binding.emptyStateContainer.setVisibility(View.VISIBLE);
            binding.progressBarLayout.setVisibility(View.GONE);
            return;
        }
        AtomicInteger loadedCount = new AtomicInteger(0);
        for (String id : _childNodes) {
            logFile.addLog("RESOURCES", "LOADING RESOURCE WITH ID : ".concat(id));
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Map<String, Object> item = (Map<String, Object>) dataSnapshot.getValue();
                        if (item != null) {
                            item.put("id", id);
                            resourcesList.add(0, new HashMap<>(item));
                        }
                    }

                    if (loadedCount.incrementAndGet() == _childNodes.size()) {
                        binding.progressBarLayout.setVisibility(View.GONE);
                        binding.emptyStateContainer.setVisibility(View.GONE);
                        binding.itemsLayout.setVisibility(View.VISIBLE);
                        loadResourceItemsToList();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    binding.progressBarLayout.setVisibility(View.GONE);
                    binding.emptyStateContainer.setVisibility(View.VISIBLE);
                    logFile.addLog("RESOURCES", "FAILED LOADING DATA : " + id);
                    Toast.makeText(requireActivity(), "Failed loading resources: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            };
            resources.child(id).addListenerForSingleValueEvent(listener);
        }
    }

    public void attachAdapterToRecyclerView() {
        binding.itemsList.setLayoutManager(new LinearLayoutManager(getContext()));
        listAdapter = new resourcesAdapter(this::showResourceOptionsSheet);
        binding.itemsList.setAdapter(listAdapter);
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
        sheetbinding.deleteOption.setVisibility(View.VISIBLE);
        sheetbinding.discontinueOption.setVisibility(View.GONE);
        sheetbinding.showDetailsOption.setVisibility(View.VISIBLE);
        sheetbinding.deleteOption.setOnClickListener(_view -> {
            showLoadingDialog(true);
            resource_options.dismiss();
            logFile.addLog("RESOURCE DELETE", "RESOURCE DELETION START");
            deleteItemFromFirebase(_item);
        });
        sheetbinding.showDetailsOption.setOnClickListener(_view -> {
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
        logFile.addLog("RESOURCE DELETION", "RESOURCE STARTS DELETING");
        final String folderName = _item.get("id").toString();
        for (int pos = 0; pos < resourceIDs.size(); pos++) {
            if (resourceIDs.get(pos).equals(_item.get("id").toString())) {
                resourceIDs.remove(pos);
                break;
            }
        }
        String newIDs = new Gson().toJson(resourceIDs);
        users.child(auth.getCurrentUser().getUid()).child("purchased resources").setValue(newIDs).addOnCompleteListener(unused -> {
            logFile.addLog("RESOURCE DELETE", "DELETED SUCCESSFULLY");
            showLoadingDialog(false);
            resourcesList.remove(_item);
            loadResourceItemsToList();
            toggleEmptyState();
            PrepNestUtil.showToast(requireActivity(), "Deleted successfully!");
            FolderUtils.deleteFolder(getContext(), folderName);
        }).addOnFailureListener(error -> {
            logFile.addLog("RESOURCE DELETE", "FAILED TO DELETE : " + error.getCause());
            showLoadingDialog(false);
            PrepNestUtil.showToast(requireActivity(), "Failed to delete: " + error.getMessage());
        });
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
                List<String> fileURLs;
                fileURLs = new Gson().fromJson(_item.get("resource urls").toString(), new TypeToken<ArrayList<String>>() {
                }.getType());
                FilesDownloader.downloadFiles(getContext(), fileURLs, folderName, new FilesDownloader.DownloadCallback() {
                    @Override
                    public void onDownloadComplete() {
                        logFile.addLog("RESOURCE DOWNLOAD", "ALL RESOURCES DOWNLOADED");
                        showLoadingDialog(false);
                        logFile.addLog("RESOURCE OPEN", "MOVING TO NEXT ACTIVITY");
                        navigateToResourceView(_item);
                    }

                    @Override
                    public void onDownloadFailed(Exception e) {
                        logFile.addLog("RESOURCE DOWNLOAD", "FAILED TO DOWNLOAD RESOURCES : " + e.getCause().toString());
                        showLoadingDialog(false);
                        boolean success = FolderUtils.deleteFolder(getContext(), folderName);
                        logFile.addLog("RESOURCE DOWNLOAD", "FOLDER DELETION STATUS : " + success);
                        PrepNestUtil.showToast(getContext(), e.getCause().toString());
                    }
                });
            } else {
                logFile.addLog("RESOURCE DOWNLOAD", "FAILED TO CREATE FOLDER");
                showLoadingDialog(false);
                PrepNestUtil.showToast(getContext(), "An unknown error occurred !");
            }
        }
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

        private final ArrayList<HashMap<String, Object>> _data = new ArrayList<>();
        private final onResourceActionListener listener;


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

        @NonNull
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
            binding.subjectNameTxt.setBackground(new GradientDrawable() {
                public GradientDrawable getIns(int a, int b) {
                    this.setCornerRadius(a);
                    this.setColor(b);
                    return this;
                }
            }.getIns((int) 360, 0xFFF5F5F5));
            binding.sessionTxt.setBackground(new GradientDrawable() {
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

            HashMap<String, Object> item = _data.get(_position);

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

            if (item.containsKey("subject")) {
                binding.subjectNameTxt.setText(item.get("subject").toString());
                binding.subjectNameTxt.setVisibility(View.VISIBLE);
            } else {
                binding.subjectNameTxt.setVisibility(View.GONE);
            }

            if (item.containsKey("session")) {
                binding.sessionTxt.setText(item.get("session").toString());
                binding.sessionTxt.setVisibility(View.VISIBLE);
            } else {
                binding.sessionTxt.setVisibility(View.GONE);
            }


            ViewGroup.MarginLayoutParams paramscontainer =
                    (ViewGroup.MarginLayoutParams) binding.container.getLayoutParams();

            if (_position == (_data.size() - 1)) {
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

            binding.container.setLayoutParams(paramscontainer);

            binding.iconMoreOptions.setOnClickListener(v -> {
                int pos = _holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.showOptions(_data.get(pos));
                }
            });


            binding.container.setOnClickListener(_view1 -> {
                int pos = _holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    logFile.addLog("RESOURCE OPEN", "OPENING RESOURCE ITEM");
                    openResource(_data.get(pos));
                }
            });
        }

        @Override
        public int getItemCount() {
            return _data.size();
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
}
