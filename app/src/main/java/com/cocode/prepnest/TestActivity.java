package com.cocode.prepnest;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.cocode.prepnest.databinding.ActivityTestBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TestActivity extends AppCompatActivity implements ItemListSheetFragment.BottomSheetListener {

    private ActivityTestBinding binding;
    private String jsonCourseData = null;
    private ArrayList<HashMap<String, Object>> itemsList = new ArrayList<>();
    private HashMap<String, Object> dataPayload = new HashMap<>();
    private String selectedCourseId = "-1";
    private String selectedSemesterId = "-1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
        initializeLogic();
    }

    private void initializeLogic() {
        loadAllCoursesFromJson(this);
//        Log.d("COURSE IDS", courseIds.toString());
//        Log.d("COURSE LIST", itemsList.toString());
    }

    private void initialize() {
        binding.btnCourse.setOnClickListener(_view -> {
            List<String> courseIds = getCourseIds();
            createCoursesList(courseIds);

            dataPayload = new HashMap<>();
            dataPayload.put("type", ItemListSheetFragment.SheetType.COURSE.toString());
            dataPayload.put("list", new ArrayList<>(itemsList));
            dataPayload.put("selectedItemId", selectedCourseId);

            final ItemListSheetFragment courseSheet = ItemListSheetFragment.newInstance(dataPayload);
            courseSheet.show(getSupportFragmentManager(), "courseSheet");
        });

        binding.btnSem.setOnClickListener(_view -> {
            createSemestersList(selectedCourseId);

            dataPayload = new HashMap<>();
            dataPayload.put("type", ItemListSheetFragment.SheetType.SEMESTER.toString());
            dataPayload.put("list", new ArrayList<>(itemsList));
            dataPayload.put("selectedItemId", selectedSemesterId);

            final ItemListSheetFragment semSheet = ItemListSheetFragment.newInstance(dataPayload);
            semSheet.show(getSupportFragmentManager(), "semSheet");
        });
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

    public List<String> getCourseIds() {
        List<String> idsList = new ArrayList<>();
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonCourseData);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Iterator<String> ids = jsonObject.keys();

        while (ids.hasNext()) {
            idsList.add(ids.next());
        }

        return idsList;
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

    public double getCourseDuration(String courseId) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonCourseData);
            return jsonObject.getJSONObject(courseId).getDouble("duration");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void createCoursesList(List<String> courseIds) {
        itemsList = new ArrayList<>();

        for (int index = 0; index < courseIds.size(); index++) {
            HashMap<String, Object> courseMap = new HashMap<>();
            courseMap.put("id", courseIds.get(index));
            courseMap.put("text", getCourseName(courseIds.get(index)));

            itemsList.add(courseMap);
        }
    }

    public void createSemestersList(String courseId) {
        final double courseMaxDuration = getCourseDuration(courseId);
        final int courseMaxSemesters = (int) (courseMaxDuration * 2);
        itemsList = new ArrayList<>();


        for (int sem = 1; sem <= courseMaxSemesters; sem++) {
            HashMap<String, Object> semesterMap = new HashMap<>();
            semesterMap.put("id", String.valueOf(sem));
            semesterMap.put("text", PrepNestUtil.getFormattedNumber(sem).concat(" semester"));

            itemsList.add(semesterMap);
        }
    }

    @Override
    public void onDataReturned(HashMap<String, Object> updatedMap) {
        Log.d("DATA FROM FRAGMENT", updatedMap.toString());
        if (updatedMap.containsKey("type") && updatedMap.containsKey("id")) {
            final String type = Objects.requireNonNull(updatedMap.get("type")).toString();
            if (type.equals(ItemListSheetFragment.SheetType.COURSE.toString())) {
                selectedCourseId = Objects.requireNonNull(updatedMap.get("id")).toString();
                selectedSemesterId = "-1";
            } else if (type.equals(ItemListSheetFragment.SheetType.SEMESTER.toString())) {
                selectedSemesterId = Objects.requireNonNull(updatedMap.get("id")).toString();
            }
        }
    }
}