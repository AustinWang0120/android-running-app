package com.example.runningapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 1. Get all chatting messages
 * 2. Get all running records belong to the current user
 * 3. Get all bitmaps belong to the current user
 * */

public class UserData {

    private static final String TAG = "user-data";

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private String uid;
    private ArrayList<Map<String, Object>> messages;
    private ArrayList<Map<String, Object>> runningRecords;
    private int bitmapsSize;
    private ArrayList<HashMap<String, Object>> bitmaps;

    /**
     * Custom callback function: after fetching all running records
     * */
    interface RunningRecordsCallback {
        void onRunningRecords(ArrayList runningRecords);
    }

    /**
     * Custom callback function: after fetching all messages
     * */
    interface MessagesCallback {
        void onMessages(ArrayList messages);
    }

    /**
     * Custom callback function: after getting all bitmaps
     * */
    interface BitmapsCallback {
        void onBitmaps(ArrayList bitmaps);
    }

    /**
     * Make a new instance everytime we want to fetch some data
     * */
    public UserData() {
        this.mAuth = FirebaseAuth.getInstance();
        this.mUser = mAuth.getCurrentUser();
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.mStorage = FirebaseStorage.getInstance().getReference();
        this.uid = mUser.getUid();
        this.messages = new ArrayList<Map<String, Object>>();
        this.runningRecords = new ArrayList<Map<String, Object>>();
        this.bitmapsSize = 0;
        this.bitmaps = new ArrayList<HashMap<String, Object>>();
    }

    public void getMessages(MessagesCallback callback) {
        mDatabase.child("chat_messages").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "getMessages: success");
                    Map<String, Object> records = (Map<String, Object>) task.getResult().getValue();
                    if (records != null) {
                        for (Map.Entry<String, Object> entry : records.entrySet()) {
                            Map<String, Object> singleRecord = (Map<String, Object>) entry.getValue();
                            messages.add(singleRecord);
                        }
                        callback.onMessages(messages);
                    }
                } else {
                    Log.d(TAG, "getMessages: failure");
                }
            }
        });
    }

    public void getRunningRecords(RunningRecordsCallback callback) {
        // user_records -> all running_records ids -> running_records
        mDatabase.child("user_records").child(uid).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "getRunningRecords: success");
                    Map<String, Object> records = (Map<String, Object>) task.getResult().getValue();
                    if (records != null) {
                        for (Map.Entry<String, Object> entry : records.entrySet()) {
                            Map<String, Object> singleRecord = (Map<String, Object>) entry.getValue();
                            runningRecords.add(singleRecord);
                        }
                    } else {
                        // convert runningRecords to null
                        runningRecords = null;
                    }
                    // We have all running records now
                    callback.onRunningRecords(runningRecords);
                } else {
                    Log.d(TAG, "getRunningRecords: failure");
                }
            }
        });
    }

    /**
     * onlyUser:
     * false -> get all images under the folder "shared_route_images"
     * true -> get all images under the folder "user_route_images/uid"
     * */
    public void getBitmaps(boolean onlyUser, BitmapsCallback callback) {
        // user_route_images -> uid -> key.jpg
        String path;
        if (onlyUser) {
            path = "user_route_images/" + mUser.getUid();
        } else {
            path = "shared_route_images";
        }
        mStorage.child(path).listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        // prefix vs. item: don't know what does the prefix do
                        bitmapsSize = listResult.getItems().size();
                        for (StorageReference item : listResult.getItems()) {
                            item.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                @Override
                                public void onSuccess(StorageMetadata storageMetadata) {
                                    Log.d(TAG, "onSuccess: item time -> "
                                            + new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(storageMetadata.getCreationTimeMillis()));
                                    String date = String.valueOf(new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(storageMetadata.getCreationTimeMillis()));
                                    // Download the bitmap to local
                                    try {
                                        File localFile = File.createTempFile(item.getName(), "jpg");
                                        item.getFile(localFile)
                                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                                    @Override
                                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                                        // Add the bitmap to the ArrayList
                                                        Log.d(TAG, "onSuccess: gitBitmaps successfully added one bitmap");
                                                        // Add date and bitmap to the arraylist
                                                        HashMap<String, Object> singleMap = new HashMap<>();
                                                        singleMap.put("date", date);
                                                        singleMap.put("bitmap", localFile);
                                                        bitmaps.add(singleMap);
                                                        // Log.d(TAG, "onSuccess: " + bitmaps.size());
                                                        // Trigger the callback function if the size of the arraylist is correct
                                                        if (bitmaps.size() == bitmapsSize) {
                                                            callback.onBitmaps(bitmaps);
                                                        }
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.d(TAG, "getBitmaps: onFailure: Unknown Error");
                                                    }
                                                });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Firebase Storage Failure");
                    }
                });
    }

    public static int calculateTotalTimes(ArrayList<Map<String, Object>> runningRecords) {
        return runningRecords.size();
    }

    public static float calculateTotalDistance(ArrayList<Map<String, Object>> runningRecords) {
        float totalDistance = 0;
        for (int i = 0; i < runningRecords.size(); i++) {
            if (runningRecords.get(i).containsKey("distance")) {
                totalDistance += Float.parseFloat((String) runningRecords.get(i).get("distance"));
            }
        }
        return totalDistance;
    }

    public static String calculateTotalDuration(ArrayList<Map<String, Object>> runningRecords) {
        TimePart totalDuration = TimePart.parse("0:00:00");
        for (int i = 0; i < runningRecords.size(); i++) {
            TimePart timePart = TimePart.parse((String) runningRecords.get(i).get("duration"));
            totalDuration = totalDuration.add(timePart);
        }
        return totalDuration.toString();
    }

    public static String calculateAveragePace(ArrayList<Map<String, Object>> runningRecords) {
        int totalTimes = calculateTotalTimes(runningRecords);
        Pace totalPace = Pace.parse("0'00''");
        for (int i = 0; i < runningRecords.size(); i++) {
            Pace pace = Pace.parse((String) runningRecords.get(i).get("average pace"));
            totalPace = totalPace.add(pace);
        }
        totalPace.divide(totalTimes);
        return totalPace.toString();
    }

    public static float calculateAverageMiles(ArrayList<Map<String, Object>> runningRecords) {
        float totalDistance = calculateTotalDistance(runningRecords);
        int totalTimes = calculateTotalTimes(runningRecords);
        return totalDistance/totalTimes;
    }

    /**
     * TODO: Filter data based on year and month
     * 1. runningRecords contains all data
     * 2. update runningRecords after filtering: [r1, r2, r3 ...] -> [r1, r3]
     * 3. then we can call aux functions to calculate again by passing the updated runningRecords
     * */
}

/**
 * Aux class for calculating pace
 * */
class Pace {
    int minutes = 0;
    int seconds = 0;

    static Pace parse(String time) {
        if (time != null) {
            // [minutes, seconds]
            String[] arr = time.split("'");
            Pace pace = new Pace();
            pace.minutes = Integer.parseInt(arr[0]);
            pace.seconds = Integer.parseInt(arr[1]);
            return pace;
        }
        return null;
    }

    public Pace add(Pace pace) {
        this.seconds += pace.seconds;
        int of = 0;
        while (this.seconds >= 60) {
            of++;
            this.seconds -= 60;
        }
        this.minutes += pace.minutes + of;
        return this;
    }

    public void divide(int times) {
        this.minutes /= times;
        this.seconds /= times;
    }

    @Override
    public String toString() {
        return String.format("%02d'%02d''", this.minutes, this.seconds);
    }
}

/**
 * Aux class for calculating the duration
 * */
class TimePart {
    int hours = 0;
    int minutes = 0;
    int seconds = 0;

    static TimePart parse(String time) {
        if (time != null) {
            String[] arr = time.split(":");
            TimePart timePart = new TimePart();
            timePart.hours = Integer.parseInt(arr[0]);
            timePart.minutes = Integer.parseInt(arr[1]);
            timePart.seconds = Integer.parseInt(arr[2]);
            return timePart;
        }
        return null;
    }

    public TimePart add(TimePart timePart) {
        this.seconds += timePart.seconds;
        int of = 0;
        while (this.seconds >= 60) {
            of++;
            this.seconds -= 60;
        }
        this.minutes += timePart.minutes + of;
        of = 0;
        while (this.minutes >= 60) {
            of++;
            this.minutes -= 60;
        }
        this.hours += timePart.hours + of;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
