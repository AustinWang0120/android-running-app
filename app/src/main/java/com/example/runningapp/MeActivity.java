package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MeActivity extends AppCompatActivity {

    private static final String TAG = "me-page";

    private TextView userNameLabel;
    private ListView lvSettingPosts;
    private Button goSetting;
    private ProgressBar progressBar;
    private TextView progressBarGoal;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseUser mUser;

    // UserData
    UserData userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_me);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        // UserData
        userData = new UserData();

        // Set the username
        userNameLabel = (TextView) findViewById(R.id.tvSettingUserName);
        userNameLabel.setText(mUser.getDisplayName());

        // Set the progress bar
        progressBar = (ProgressBar) findViewById(R.id.miles_progress);
        progressBarGoal = (TextView) findViewById(R.id.current_goal_label);
        userData.getRunningRecords(new UserData.RunningRecordsCallback() {
            @Override
            public void onRunningRecords(ArrayList runningRecords) {
                if (runningRecords != null) {
                    float distance = UserData.calculateTotalDistance(runningRecords);
                    int defaultGoal = 1;
                    while (defaultGoal < distance) {
                        defaultGoal *= 10;
                    }
                    // Set the progress bar by some math manipulation
                    progressBarGoal.setText("To " + defaultGoal + " miles");
                    progressBar.setMax(defaultGoal * defaultGoal * 10);
                    progressBar.setProgress(Math.round(distance * defaultGoal * 10));
                }
                // If there is no record, do nothing...
            }
        });

        lvSettingPosts = (ListView) findViewById(R.id.lvSetting);
        goSetting = (Button) findViewById(R.id.btnSetting);

        /**
         * Setting button
         * */
        goSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent toSettingActivity = new Intent(MeActivity.this, SettingActivity.class);
                startActivity(toSettingActivity);
            }
        });

        /**
         * Render the post list
         * */
        userData.getBitmaps(true, new UserData.BitmapsCallback() {
            @Override
            public void onBitmaps(ArrayList bitmaps) {
                // Sort the arraylist first
                Collections.sort(bitmaps, new CustomComparator());
                bitmapsAdapter adapter = new bitmapsAdapter(MeActivity.this, R.layout.setting_lv, bitmaps);
                lvSettingPosts.setAdapter(adapter);
            }
        });

        // Display the bottom navigation bar
        displayNavigationBar();
    }

    public void displayNavigationBar() {
        /**
         * Navigation bar
         * */
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.mbMe);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.mbHome:
                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.mbPost:
                        Intent goPostActivity = new Intent(getBaseContext(), PostActivity.class);
                        startActivity(goPostActivity);
                        break;
                    case R.id.mbRun:
                        Intent setGoalIntent = new Intent(getBaseContext(), SetGoalActivity.class);
                        startActivity(setGoalIntent);
                        break;
                    case R.id.mbMessage:
                        Intent goMessageActivity = new Intent(getBaseContext(), ChattingActivity.class);
                        startActivity(goMessageActivity);
                        break;
                    case R.id.mbMe:
                        break;
                }
                return false;
            }
        });
    }
}

/**
 * Aux class for comparing dates
 * */
class CustomComparator implements Comparator<Map<String, Object>> {

    @Override
    public int compare(Map o1, Map o2) {
        String date1 = (String) o1.get("date");
        String date2 = (String) o2.get("date");
        // yyyy-MM-dd hh:mm
        String[] array1 = date1.split("-");
        String[] array2 = date2.split("-");
        int compare1, compare2;
        // Iteratively compare all fields of the date
        for (int i = 0; i < array1.length; i++) {
            compare1 = Integer.parseInt(array1[i]);
            compare2 = Integer.parseInt(array2[i]);
            if (compare1 != compare2) {
                return -(compare1 - compare2);
            }
        }
        // All equals (should be impossible): return 1
        return 1;
    }
}

class bitmapsAdapter extends ArrayAdapter<HashMap<String, Object>> {

    private static final String TAG = "bitmapsAdapter-class";

    private int layout;
    private Context context;

    public bitmapsAdapter(@NonNull Context context, int resource, ArrayList<HashMap<String, Object>> bitmaps) {
        super(context, resource, bitmaps);
        this.layout = resource;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View view = convertView;

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            view = layoutInflater.inflate(layout, null);
        }

        // Get the bitmap file
        HashMap<String, Object> singleMap = (HashMap<String, Object>) getItem(position);

        if (singleMap != null) {
            // imgMapSettingLV, tvDatePostLV
            File file = (File) singleMap.get("bitmap");
            ImageView image = (ImageView) view.findViewById(R.id.imgMapSettingLV);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
            image.setImageBitmap(bitmap);

            String creationDate = (String) singleMap.get("date");
            TextView date = (TextView) view.findViewById(R.id.tvDatePostLV);
            date.setText(creationDate);
        }

        return view;
    }
}