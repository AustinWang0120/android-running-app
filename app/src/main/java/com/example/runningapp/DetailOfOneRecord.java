package com.example.runningapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class DetailOfOneRecord extends AppCompatActivity {

    private static final String TAG = "detail-of-single-run";

    // UserData
    UserData userData;

    // Firebase
    FirebaseUser mUser;
    StorageReference mStorage;

    ImageView bitmapImage;
    TextView timestamp_label;
    TextView distance_label;
    TextView avgPace_label;
    TextView duration_label;
    TextView calories_label;
    TextView stepsPerMin_label;
    TextView totalSteps_label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_of_single_run);

        // UserData
        userData = new UserData();

        // Firebase
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mStorage = FirebaseStorage.getInstance().getReference();

        /**
         * Get all data from the bundle and render
         * */
        bitmapImage = (ImageView) findViewById(R.id.imgMap);
        timestamp_label = (TextView) findViewById(R.id.tvRmm_dd_yy_starthr);
        distance_label = (TextView) findViewById(R.id.tvRmiles);
        avgPace_label = (TextView) findViewById(R.id.tvRAvgPaceNum);
        duration_label = (TextView) findViewById(R.id.tvRduarationNum);
        calories_label = (TextView) findViewById(R.id.tvRcaloriesNum);
        stepsPerMin_label = (TextView) findViewById(R.id.tvRstepMinNum);
        totalSteps_label = (TextView) findViewById(R.id.tvRtotalStepNum);

        Bundle bundle = getIntent().getExtras();

        String time = bundle.getString("timestamp");
        String distance = bundle.getString("distance");
        String avgPace = bundle.getString("avgPace");
        String duration = bundle.getString("duration");
        String calories = bundle.getString("calories");
        String stepsPerMin = bundle.getString("stepsPerMin");
        String totalSteps = bundle.getString("totalSteps");
        String key = bundle.getString("key");

        timestamp_label.setText(time);
        distance_label.setText(distance);
        avgPace_label.setText(avgPace);
        duration_label.setText(duration);
        calories_label.setText(calories);
        stepsPerMin_label.setText(stepsPerMin);
        totalSteps_label.setText(totalSteps);

        // Get the bitmap with the key
        try {
            File localFile = File.createTempFile("image", "jpg");
            mStorage.child("user_route_images/" + mUser.getUid() + "/" + key + ".jpg")
                    .getFile(localFile)
                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Bitmap bitmap = BitmapFactory.decodeFile(localFile.getPath());
                            // Find the ImageView and render
                            bitmapImage.setImageBitmap(bitmap);
                        }
                    });
        } catch (Exception e) {
            // do nothing...
        }
    }
}