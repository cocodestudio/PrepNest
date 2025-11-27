package com.cocode.prepnest;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cocode.prepnest.databinding.DetailsSelectSheetBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


public class ItemListSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_MAP = "argMap";
    private HashMap<String, Object> receivedMap;
    private ArrayList<HashMap<String, Object>> itemsList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> originalList = new ArrayList<>();
    private DetailsSelectSheetBinding binding;
    private DetailsListAdapter listAdapter;
    private BottomSheetListener listener;

    public static ItemListSheetFragment newInstance(HashMap<String, Object> map) {
        final ItemListSheetFragment fragment = new ItemListSheetFragment();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_MAP, map);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = (BottomSheetListener) context;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = DetailsSelectSheetBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            receivedMap = (HashMap<String, Object>) getArguments().getSerializable(ARG_MAP);

            assert receivedMap != null;
            if (receivedMap.containsKey("list")) {
                originalList = (ArrayList<HashMap<String, Object>>) receivedMap.get("list");
                assert originalList != null;
                itemsList = new ArrayList<>(originalList);
                listAdapter = new DetailsListAdapter(
                        requireContext(),
                        itemsList,
                        Objects.requireNonNull(receivedMap.getOrDefault("selectedItemId", "-1")).toString()
                );
                binding.itemsList.setAdapter(listAdapter);
            } else {
                throw new RuntimeException("'list' key not found in receivedMap");
            }

            if (Objects.equals(receivedMap.getOrDefault("type", SheetType.COURSE.toString()), SheetType.COURSE.toString())) {
                binding.searchContainer.setVisibility(View.VISIBLE);
            } else {
                binding.searchContainer.setVisibility(View.GONE);
            }

            binding.searchEdittext.setFocusableInTouchMode(true);
            binding.itemsList.setVerticalScrollBarEnabled(false);

            binding.searchEdittext.addTextChangedListener(new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    String query = s.toString().toLowerCase();

                    itemsList.clear();

                    if (query.isEmpty()) {
                        // Reset completely
                        itemsList.addAll(originalList);
                    } else {
                        // Filter
                        for (HashMap<String, Object> item : originalList) {
                            // Convert item into searchable text (choose the correct key)
                            String text = Objects.requireNonNull(item.get("text")).toString().toLowerCase();
                            if (text.contains(query)) {
                                itemsList.add(item);
                            }
                        }
                    }

                    listAdapter.notifyDataSetChanged();
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
            });

            binding.itemsList.setOnItemClickListener((parent, view, position, id) -> {
                HashMap<String, Object> dataMap = new HashMap<>();

                dataMap.put("type", receivedMap.getOrDefault("type", SheetType.COURSE.toString()));
                dataMap.put("id", itemsList.get(position).get("id"));
                dataMap.put("text", itemsList.get(position).get("text"));

                listener.onDataReturned(dataMap);
                dismiss();
            });
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();

        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        assert dialog != null;
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);

            if (Objects.equals(receivedMap.getOrDefault("type", SheetType.COURSE.toString()), SheetType.COURSE.toString())) {
                bottomSheet.getLayoutParams().height = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        500,
                        getResources().getDisplayMetrics()
                );
            }
            bottomSheet.requestLayout();

            // Optional: expand to full fixed height
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    public enum SheetType {
        COURSE,
        SEMESTER,
        SESSION
    }


    public interface BottomSheetListener {
        void onDataReturned(HashMap<String, Object> updatedMap);
    }
}