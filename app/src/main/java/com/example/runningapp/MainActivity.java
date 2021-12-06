package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "main-page";

    // Views
    private TextView timesLabel;
    private TextView hourLabel;
    private TextView distanceLabel;
    private TextView avgMileLabel;
    private TextView avgPaceLabel;
    private ListView lvEpisodes;
    private ListAdapter lvAdapter;
    private BottomNavigationView bottomNavigationView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabase;

    // UserData
    private UserData userData;
    private ArrayList<Map<String, Object>> runningRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // UserData
        userData = new UserData();

        // get all running records
        userData.getRunningRecords(new UserData.RunningRecordsCallback() {
            @Override
            public void onRunningRecords(ArrayList allRecords) {
                runningRecords = allRecords;
                /**
                 * no records -> change the layout
                 * records -> render
                 * */
                if (runningRecords == null) {
                    Log.d(TAG, "onRunningRecords: null detected");
                    setContentView(R.layout.blank_page_no_record);
                    displayNavigationBar();
                } else {
                    setContentView(R.layout.activity_main);
                    // Sort all records first
                    Collections.sort(runningRecords, new RunningRecordsComparator());
                    // Find views
                    distanceLabel = (TextView) findViewById(R.id.tvHomeMileNum);
                    timesLabel = (TextView) findViewById(R.id.tvHomeTimesNum);
                    hourLabel = (TextView) findViewById(R.id.tvHomeHourNum);
                    avgMileLabel = (TextView) findViewById(R.id.tvHomeAvgMileNum);
                    avgPaceLabel = (TextView) findViewById(R.id.tvHomeAvgPaceNum);

                    // render texts after calculating
                    distanceLabel.setText(String.format("%.2f", UserData.calculateTotalDistance(runningRecords)));
                    timesLabel.setText(String.valueOf(UserData.calculateTotalTimes(runningRecords)));
                    hourLabel.setText(UserData.calculateTotalDuration(runningRecords));
                    avgMileLabel.setText(String.format("%.2f", UserData.calculateAverageMiles(runningRecords)));
                    avgPaceLabel.setText(UserData.calculateAveragePace(runningRecords));

                    // render the list of all records
                    lvEpisodes = (ListView) findViewById(R.id.listView);
                    // use outer_records.xml for listview rows layout
                    lvAdapter = new MyCustomAdapter(MainActivity.this, R.layout.outer_records, runningRecords);
                    lvEpisodes.setAdapter(lvAdapter);
                    // handle clicking action for the listview
                    lvEpisodes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            // Toast.makeText(MainActivity.this, "" + position, Toast.LENGTH_SHORT).show();
                            // Start the DetailOfOneRecord Activity and passing a bundle with the running record
                            Intent intent = new Intent(MainActivity.this, DetailOfOneRecord.class);
                            Map<String, Object> runningRecord = runningRecords.get(position);
                            intent.putExtra("timestamp", (String) runningRecord.get("timestamp"));
                            intent.putExtra("distance", (String) runningRecord.get("distance"));
                            intent.putExtra("avgPace", (String) runningRecord.get("average pace"));
                            intent.putExtra("duration", (String) runningRecord.get("duration"));
                            intent.putExtra("calories", (String) runningRecord.get("calories"));
                            intent.putExtra("stepsPerMin", (String) runningRecord.get("steps per min"));
                            intent.putExtra("totalSteps", (String) runningRecord.get("total steps"));
                            // Also includes the key for the bitmap in order to render the image
                            intent.putExtra("key", (String) runningRecord.get("key"));
                            startActivity(intent);
                        }
                    });

                    // display the navigation bar
                    displayNavigationBar();
                }
            }
        });
    }

    public void displayNavigationBar() {
        /**
         * Navigation bar
         * */
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.mbHome);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.mbHome:
                        break;
                    case R.id.mbPost:
                        Intent goPostActivity = new Intent(MainActivity.this, PostActivity.class);
                        startActivity(goPostActivity);
                        break;
                    case R.id.mbRun:
                        Intent goRunActivity = new Intent(MainActivity.this, SetGoalActivity.class);
                        startActivity(goRunActivity);
                        break;
                    case R.id.mbMessage:
                        Intent goMessageActivity = new Intent(MainActivity.this, ChattingActivity.class);
                        startActivity(goMessageActivity);
                        break;
                    case R.id.mbMe:
                        Intent goMeActivity = new Intent(MainActivity.this, MeActivity.class);
                        startActivity(goMeActivity);
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
class RunningRecordsComparator implements Comparator<Map<String, Object>> {

    @Override
    public int compare(Map o1, Map o2) {
        String date1 = (String) o1.get("timestamp");
        String date2 = (String) o2.get("timestamp");
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

// Custom Adapter For Running Records List
class MyCustomAdapter extends ArrayAdapter<Map<String, Object>> {

    private static final String TAG = "main-page";

    private int layout;
    private Context context;

    public MyCustomAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Map<String, Object>> objects) {
        super(context, resource, objects);
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

        Map<String, Object> runningRecord = getItem(position);

        if (runningRecord != null) {
            /**
             * time
             * distance
             * duration
             * calories
             * steps
             * */
            TextView timestampLabel = (TextView) view.findViewById(R.id.timestamp_label);
            TextView distanceLabel = (TextView) view.findViewById(R.id.distance_label);
            TextView durationLabel = (TextView) view.findViewById(R.id.duration_label);
            TextView averagePaceLabel = (TextView) view.findViewById(R.id.average_pace_label);
            TextView caloriesLabel = (TextView) view.findViewById(R.id.calories_label);

            timestampLabel.setText((String) runningRecord.get("timestamp"));
            distanceLabel.setText((String) runningRecord.get("distance") + " miles");
            durationLabel.setText((String) runningRecord.get("duration"));
            averagePaceLabel.setText((String) runningRecord.get("average pace"));
            caloriesLabel.setText((String) runningRecord.get("calories") + " kcal");
        }
        return view;
    }
}