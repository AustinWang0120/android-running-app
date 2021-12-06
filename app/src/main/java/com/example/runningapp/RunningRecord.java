package com.example.runningapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RunningRecord {

    private static final String TAG = "running-record-class";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabase;

    public String uid;
    public String key;
    public String timestamp;
    public String distance;
    public String average_pace;
    public String duration;
    public String calories;
    public String steps_per_min;
    public String total_steps;
    public Boolean shared;

    public RunningRecord() {
        // Default constructor required for calls to DataSnapshot.getValue(RunningRecord.class)
    }

    public String getUid() {
        return uid;
    }

    public String getKey() {
        return key;
    }

    public RunningRecord(String uid, String timestamp, String distance, String average_pace, String duration,
                         String calories, String steps_per_min, String total_steps, Boolean shared) {
        this.mAuth = FirebaseAuth.getInstance();
        this.mUser = mAuth.getCurrentUser();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.uid = uid;
        this.timestamp = timestamp;
        this.distance = distance;
        this.average_pace = average_pace;
        this.duration = duration;
        this.calories = calories;
        this.steps_per_min = steps_per_min;
        this.total_steps = total_steps;
        this.shared = shared;
    }

    // Convert data to the Map structure
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", this.uid);
        result.put("key", this.key);
        result.put("timestamp", this.timestamp);
        result.put("distance", this.distance);
        result.put("average pace", this.average_pace);
        result.put("duration", this.duration);
        result.put("calories", this.calories);
        result.put("steps per min", this.steps_per_min);
        result.put("total steps", this.total_steps);
        result.put("shared", this.shared);

        return result;
    }

    /**
     * Add a new record to the DB path "/running_records/runID"
     * Add a new record to the DB path "/user_records"
     * */
    public void writeNewRecord() {
        this.key = mDatabase.child("running_records").push().getKey();
        Map<String, Object> recordValues = this.toMap();
        // Also includes the key in order to fetch the bitmap later
        recordValues.put("key", this.key);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/running_records/" + this.key, recordValues);
        childUpdates.put("/user_records/" + this.uid + "/" + this.key, recordValues);

        mDatabase.updateChildren(childUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "updateChildren: success");
                } else {
                    Log.d(TAG, "updateChildren: failure");
                }
            }
        });
    }
}
