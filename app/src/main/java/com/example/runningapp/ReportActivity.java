package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.example.runningapp.databinding.ActivityReportBinding;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "report-page";
    public static final int SAVEIMAGE = 9999;

    private UiSettings mUiSettings;
    private GoogleMap mMap;
    private ActivityReportBinding binding;

    // Views
    Bundle bundle;
    TextView startTime_label;
    TextView miles_label;
    TextView avgPace_label;
    TextView calories_label;
    TextView stepsPerMin_label;
    TextView totalSteps_label;
    TextView duration_label;
    Button btnShare, btnHome;

    CameraUpdate cu;
    ArrayList<LatLng> everySecLocation;
    ArrayList<LatLng> continueLocation;

    List<int[]> colorList = new ArrayList<>();
    View screenshotView;
    View rootView;

    LatLngBounds.Builder builder;
    LatLngBounds bounds;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;

    // RunningRecord instance for writing new record and posting image
    private RunningRecord runningRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();

        // Get the bundle
        bundle = getIntent().getExtras();

        // get the mapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapReport);
        mapFragment.getMapAsync(ReportActivity.this);
        screenshotView = findViewById(R.id.screenshotLayout);
        rootView = getWindow().getDecorView().findViewById(android.R.id.content);

        // Find views and set texts
        startTime_label = (TextView) findViewById(R.id.tvRmm_dd_yy_starthr_REPORT);
        miles_label = (TextView) findViewById(R.id.tvRmiles_REPORT);
        avgPace_label = (TextView) findViewById(R.id.tvRAvgPaceNum_REPORT);
        duration_label = (TextView) findViewById(R.id.tvRduarationNum_REPORT) ;
        calories_label = (TextView) findViewById(R.id.tvRcaloriesNum_REPORT);
        stepsPerMin_label = (TextView) findViewById(R.id.tvRstepMinNum_REPORT);
        totalSteps_label = (TextView) findViewById(R.id.tvRtotalStepNum_REPORT);
        btnShare = (Button) findViewById(R.id.btnShareReport);
        btnHome = (Button) findViewById(R.id.btnHomeReport);

        startTime_label.setText(bundle.getString("startTime"));
        miles_label.setText(bundle.getString("distance")+" mi");
        avgPace_label.setText(bundle.getString("avgPace"));
        duration_label.setText(bundle.getString("duration"));
        calories_label.setText(bundle.getString("calories"));
        stepsPerMin_label.setText(bundle.getString("avgSteps"));
        totalSteps_label.setText(bundle.getString("totalSteps"));
        everySecLocation = bundle.getParcelableArrayList("everySecLocation");
        continueLocation = bundle.getParcelableArrayList("continueLocation");

        /**
         * Database Update
         * Update the Real-Time Database
         * Update the Cloud Storage (even without clicking the share button)
         * */
        writeReportToDatabase();

        /**
         * Home Button: push the image to "/user_route_images"
         * */
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CaptureMapScreen(true, false);
                Intent intent = new Intent(ReportActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        /**
         * Share Button: push the image to "/shared_route_images" and change the field of "share"
         * */
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (runningRecord != null) {
                    mDatabase.child("running_records/" + runningRecord.getKey() + "/shared").setValue(true);
                    mDatabase.child("user_records/" + mUser.getUid() + "/" + runningRecord.getKey() + "/shared").setValue(true);
                }
                CaptureMapScreen(false, true);
            }
        });

        makeGradientList(colorList);
    }

    /**
     * Aux function for pushing the image to "user_route_images" or "shared_route_images"
     * param:
     * 1. user -> push to the user_route_images
     * 2. share -> push to the shared_route_images
     * */
    public void CaptureMapScreen(boolean user_folder_save, boolean shared_folder_save)
    {
        SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                // Check if the permission for "WRITE_EXTERNAL_STORAGE" is granted
                if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ReportActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 9999);
                } else {
                    // Get the directory path for the pictures folder
                    // Check if the directory is existed, create one if not
                    File path = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES);
                    File dir = new File(String.valueOf(path));
                    if(!dir.exists()) {
                        dir.mkdirs();
                    }
                    if (runningRecord != null) {
                        String uid = mUser.getUid();
                        // ".jpg" must be added manually
                        String filename;
                        if (user_folder_save) {
                            filename = runningRecord.getKey() + ".jpg";
                        } else {
                            filename = mUser.getDisplayName() + runningRecord.getKey() + ".jpg";
                        }
                        File file = new File(path, filename);
                        try {
                            // Save the image first
                            FileOutputStream fileOutputStream = new FileOutputStream(file);
                            snapshot.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            // End of saving the file

                            // Push the bitmap to the Firebase Cloud Storage "user_route_images"
                            Uri fileUri = Uri.fromFile(file);
                            // The path structure: /user_route_images/{uid}/{runningRecord ID}
                            // The file name: displayName + key
                            if (user_folder_save) {
                                UploadTask uploadTask= mStorage.child("user_route_images/" + uid + "/" + filename).putFile(fileUri);
                                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        Toast.makeText(ReportActivity.this, "Share successfully", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                uploadTask.addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(ReportActivity.this, "Failure within the Database", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            // Push the bitmap to the Firebase Cloud Storage "shared_route_images"
                            if (shared_folder_save) {
                                UploadTask sharedUploadTask = mStorage.child("shared_route_images/" + filename).putFile(fileUri);
                                sharedUploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // Do nothing
                                        Log.d(TAG, "onSuccess: Bitmap has been added to the shared_route_images successfully");
                                    }
                                });
                                sharedUploadTask.addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Do nothing
                                        Log.d(TAG, "onFailure: Error occurs while adding the bitmap to the shared folder");
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d(TAG, "onSnapshotReady: " + e.getMessage());
                            Intent intent = new Intent(ReportActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    }
                }
            }
        };
        mMap.snapshot(callback);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Add buttons for zooming in and out
        mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);
        // Draw the route on the map
        double maxSpeed = Double.MIN_VALUE;
        double minSpeed = Double.MAX_VALUE;
        for(int i = 1; i< everySecLocation.size(); i++){ // handle pause and continue action
            if(!continueLocation.contains(everySecLocation.get(i))){
                double curspeed = MapsFragment.getDistance(everySecLocation.get(i-1).latitude, everySecLocation.get(i).latitude,
                        everySecLocation.get(i-1).longitude, everySecLocation.get(i).longitude);
                if( minSpeed > curspeed ){ minSpeed = curspeed;}
                if( maxSpeed < curspeed ){ maxSpeed = curspeed;}
            }
        }

        //match speed to color for making a hearmap on route
        for(int i = 1; i< everySecLocation.size(); i++){
            if(!continueLocation.contains(everySecLocation.get(i))){//using continue location to handle pause and continue action
                double curspeed = MapsFragment.getDistance(everySecLocation.get(i-1).latitude, everySecLocation.get(i).latitude,
                        everySecLocation.get(i-1).longitude, everySecLocation.get(i).longitude);
                //get weight of speed on color demension
                Double weight = (curspeed-minSpeed) / (maxSpeed-minSpeed);
                //get cooresponding color index to calculate color
                int colorIndex = (int) Math.round(weight * (colorList.size()-1));
                int rgb[] = colorList.get(colorIndex);
                int color = Color.rgb(rgb[0], rgb[1], rgb[2]);
                //draw polyline on map
                Polyline line = mMap.addPolyline(new PolylineOptions().add(everySecLocation.get(i-1), everySecLocation.get(i)).color(color));
                //set start point
                if(i == 1){
                    mMap.addMarker(new MarkerOptions().position(everySecLocation.get(i-1)).title("Start")).showInfoWindow();
                }
                //set end point
                if(i == everySecLocation.size()-1){
                    mMap.addMarker(new MarkerOptions().position(everySecLocation.get(everySecLocation.size()-1)).title("End")).showInfoWindow();
                }

                // allow user to check speed on different part of polyline
                // add marker at every location and set them to invisible
                MarkerOptions options = new MarkerOptions()
                        .position(everySecLocation.get(i))
                        .title(Math.round(curspeed * 1609.34 * 100.0) / 100.0 + " m/s")
                        .anchor((float) 0, (float) 0);
                Marker marker = mMap.addMarker(options);
                marker.setAlpha(0);
            }
        }
        // adjust the camera by set bounds so that it can show entire routes on the screen
        builder = new LatLngBounds.Builder();
        for (LatLng loc : everySecLocation){
            builder.include(loc);
        }
        int padding = 70;
        bounds = builder.build();
        cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMap.animateCamera(cu);
            }
        });
    }

    /**
     * Database update
     * */
    public void writeReportToDatabase() {
        /**
         * Retrieve data from the bundle and update to the database
         * */
        String timestamp_data = bundle.getString("startTime");
        String distance_data = bundle.getString("distance");
        String average_pace_data = bundle.getString("avgPace");
        String duration_data = bundle.getString("duration");
        String calories_data = bundle.getString("calories");
        String steps_per_min_data = bundle.getString("avgSteps");
        String total_steps_data = bundle.getString("totalSteps");

        Log.d(TAG, "writeReportToDatabase: " + duration_data);

        // Not sharing the map as default
        runningRecord = new RunningRecord(
                mUser.getUid(), timestamp_data, distance_data, average_pace_data,
                duration_data, calories_data, steps_per_min_data, total_steps_data, false
        );

        runningRecord.writeNewRecord();
    }

    /**
     * create a list of RGB from red to yellow to green
     * */
    public void makeGradientList(List<int[]> colorList) {
        for(int i=0;i<=510;i++){
            int[] RGB = new int[3];
            if (i<=255) {
                RGB[0]=255;
                RGB[1]=i;
            } else{
                RGB[0]=510-i;
                RGB[1]=255;
            }
            RGB[2]=0;
            colorList.add(RGB);
        }
    }

    //define the function of back button to reset camera to route
    @Override
    public void onBackPressed() {
        mMap.animateCamera(cu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case SAVEIMAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    CaptureMapScreen(false, false);
                } else{
                    Toast.makeText(ReportActivity.this, "Permission Failure", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }
}