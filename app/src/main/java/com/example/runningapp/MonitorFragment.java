package com.example.runningapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MonitorFragment extends Fragment {

    private static final String TAG = "monitor-fragment";

    // Firebase
    DatabaseReference mDatabase;
    FirebaseUser mUser;

    // User info
    float weight = 80;

    // Everything for the sensor
    public SensorManager sensorManager;
    public int steps = 0;
    public int counter = 0;
    public int previousTotalSteps = 0;

    SeekBar sbRunProgress;
    TextView tvGoal, tvRunProgressPercentage, tvCurrentRunDistance, tvPace, tvTimePass, tvCalorie;
    ImageButton btnShowMap, btnShowMusic;
    Button btnStopRun, btnContinuePause;
    double goalDistance = 0;
    boolean running = false;

    String pace = "00'00''";
    String startTime, runDistance, duration, calories="0", avgSteps="0", totalSteps="0";
    double distance = 0;
    int seconds = 0;

    ArrayList<LatLng> everySecLocation;

    public interface MonitorFragmentListener {
        public void sentMonitorMessage();
        public void sentMusicMessage();
        public void sentMonitorPauseContinue();
        public void sentGoReportRequest(String startTime,String distance, String avgPace,
                                        String duration, String Calories,String avgSteps, String totalSteps);
    }

    MonitorFragmentListener MFL;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        MFL = (MonitorFragmentListener) context;
    }

    @SuppressLint({"ClickableViewAccessibility", "SimpleDateFormat"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_monitor, container, false);
        sbRunProgress = (SeekBar) view.findViewById(R.id.sbRunProgress);
        tvRunProgressPercentage = (TextView) view.findViewById(R.id.tvMonitorProgress);
        tvCurrentRunDistance = (TextView) view.findViewById(R.id.tvCurrentRunDistance);
        tvPace = (TextView) view.findViewById(R.id.tvMonitorPace);
        tvTimePass = (TextView) view.findViewById(R.id.tvMonitorElapsedTime);
        tvCalorie = (TextView) view.findViewById(R.id.tvMonitorCalorie);
        tvGoal = (TextView) view.findViewById(R.id.tvMonitorGoal);
        btnShowMap = (ImageButton) view.findViewById(R.id.btnShowMap);
        btnShowMusic = (ImageButton) view.findViewById(R.id.btnMusicPanel);
        btnStopRun = (Button) view.findViewById(R.id.btnStop);
        btnContinuePause = (Button) view.findViewById(R.id.btnPauseContinue);

        sbRunProgress.setMax(100);
        sbRunProgress.setProgress(0);

        // Firebase
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
//        mDatabase.child("/users/" + mUser.getUid() + "/weight").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DataSnapshot> task) {
//                if (task.isSuccessful()) {
//                    weight = Float.parseFloat((String) task.getResult().getValue());
//                } else {
//                    Log.d(TAG, "onComplete: Everything works");
//                }
//            }
//        });

        // Sensor for steps counting
        if(ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){
            // Ask for permission
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        }
        SensorManager sensorManager = (SensorManager) this.getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor == null) {
            Log.i(TAG, "No sensor detected!" );
        } else {
            Log.i(TAG, "Sensor has been detected..." );
            boolean temp = sensorManager.registerListener(sensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
            Log.i(TAG, "Register Sensor result: " + temp );
            if(temp)
                Log.i(TAG, "Register Sensor is successful." );
            else
                Log.i(TAG, "Register Sensor has failed." );
        }

        startTime = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Timestamp(System.currentTimeMillis()));
        runTimer();

        sbRunProgress.setOnTouchListener(new View.OnTouchListener() {// make the progress bar cannot be move by finger
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        btnShowMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MFL.sentMonitorMessage();
            }
        });

        btnShowMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MFL.sentMusicMessage();
            }
        });

        btnContinuePause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                running=!running;
                MFL.sentMonitorPauseContinue();
            }
        });

        btnStopRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(),"Long press to stop",Toast.LENGTH_SHORT).show();
            }
        });

        /**
         * Long press to stop: fix the duration
         * */
        btnStopRun.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                running=!running;
                MFL.sentGoReportRequest(startTime, runDistance, pace, duration, calories, avgSteps, totalSteps);
                return true;
            }
        });

        return view;
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            Log.i(TAG, "IN onSensorChanged!" );
            Log.i(TAG, "Sensor Event Values: " + sensorEvent.values[0] );
            int numSteps = (int) sensorEvent.values[0] - previousTotalSteps;

            if( counter == 5 ) {
                steps++;
                counter = 0;
            } else {
                counter++;
            }

            previousTotalSteps = (int) sensorEvent.values[0];
            Log.i(TAG, "numSteps: " + numSteps);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private void runTimer()
    {
        // Creates a new Handler
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override

            public void run()
            {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;

                // Format the seconds into hours, minutes, and seconds.
                duration = String
                        .format(Locale.getDefault(),
                                "%d:%02d:%02d", hours,
                                minutes, secs);

                // Set the text view text.
                tvTimePass.setText(duration);
                if(distance>0){
                    double pace = seconds/distance;
                    int paceMin=(int)pace/60;
                    int paceSec= (int)(pace - paceMin*60);
                    DecimalFormat formattingObject1 = new DecimalFormat("00");
                    MonitorFragment.this.pace = formattingObject1.format(paceMin) + "'" + formattingObject1.format(paceSec)+"''";
                    tvPace.setText(MonitorFragment.this.pace);
                }


                // If running is true, increment the seconds variable.
                if (running) {
                    seconds++;
                }

                // Post the code again with a delay of 1 second.
                handler.postDelayed(this, 1000);
            }
        });
    }

    public void setMonitorData(double Distance, ArrayList<LatLng> everySecLocationFromMap){
        // Calculate the distance and convert it to String
        distance = Math.round(Distance*100.0)/100.0;
        runDistance = String.valueOf(distance);

        // Progress
        double rate = (Distance/goalDistance);
        int percent = (int) (Math.round(rate*100.0));
        tvCurrentRunDistance.setText(String.valueOf(distance));
        if(percent>0) {
            tvRunProgressPercentage.setText(percent+" %");
        }
        sbRunProgress.setProgress(percent);
        everySecLocation = everySecLocationFromMap;

        // Calories = weight * 35 / 200 * time(minutes)
        TimePart timePart = TimePart.parse(duration);
        float caloriesSeconds = timePart.hours*3600 + timePart.minutes*60 + timePart.seconds;
        float caloriesMinute = caloriesSeconds / 60;
        float caloriesNum = (this.weight * 35 / 200) * caloriesMinute;
        this.calories = String.valueOf(Math.round(caloriesNum));
        Log.d(TAG, "setMonitorData: calories: " + calories + " times: " + caloriesMinute);
        tvCalorie.setText(String.valueOf(Math.round(caloriesNum)));

        // Steps per min
        avgSteps = String.valueOf(Math.round(steps / caloriesMinute));

        // Total Steps
        totalSteps = String.valueOf(steps);
    }

    public void setMonitorGoal(String distance){
        tvGoal.setText("Goal:  "+ distance);
        goalDistance = Double.valueOf(distance);
    }
}