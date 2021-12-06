package com.example.runningapp;


import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.example.runningapp.databinding.ActivityRunPrepareBinding;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.location.LocationRequest;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.ArrayList;

public class RunPrepareActivity extends FragmentActivity implements OnMapReadyCallback {

    String flag = "USER";

    public static final int PERMISSION_FINE_LOCATION = 44;
    private GoogleMap mMap;
    private ActivityRunPrepareBinding binding;
    private FusedLocationProviderClient client;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    PolylineOptions polylineOptions;
    private boolean lockCamToUser = false;
    private UiSettings mUiSettings;
    private static float currentZoomLevel=20;
    LatLng temp = new LatLng(0,0);

    // TileOverlay to display the heat map
    TileOverlay overlay;

    private ArrayList<LatLng> travelData = new ArrayList<LatLng>();
    private ArrayList<Double> speedData = new ArrayList<Double>();
    private ArrayList<WeightedLatLng> weightedTravelData = new ArrayList<WeightedLatLng>();

    double maxSpeed = Double.MIN_VALUE;
    double minSpeed = Double.MAX_VALUE;

    LatLng prev;
    Long lastTime;
    int RED = Color.rgb(235, 104, 87);
    int ORANGE = Color.rgb(235, 171, 87);
    int GREEN = Color.rgb(65, 224, 108);

    Button startBtn;
    Button stopBtn;

    Boolean trackLoc = true;
//=======================================Monitor vars initialize
    SeekBar sbRunProgress;
    TextView tvGoal, tvRunProgressPercentage,tvCurrentRunDistance,tvPace,tvTimePass,tvCalorie;
    ImageButton btnShowMap,btnShowMusic;
    Button btnStopRun,btnContinuePause;
    boolean isMonitorUI=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        if(isMonitorUI) {//=======================================monitor ui start
            setContentView(R.layout.activity_monitor);
            sbRunProgress = (SeekBar) findViewById(R.id.sbRunProgress);
            tvRunProgressPercentage = (TextView) findViewById(R.id.tvMonitorProgress);
            tvCurrentRunDistance = (TextView) findViewById(R.id.tvCurrentRunDistance);
            tvPace = (TextView) findViewById(R.id.tvMonitorPace);
            tvTimePass = (TextView) findViewById(R.id.tvMonitorElapsedTime);
            tvCalorie = (TextView) findViewById(R.id.tvMonitorCalorie);
            btnShowMap = (ImageButton) findViewById(R.id.btnShowMap);
            btnShowMusic = (ImageButton) findViewById(R.id.btnMusicPanel);
            btnStopRun = (Button) findViewById(R.id.btnStop);
            btnContinuePause = (Button) findViewById(R.id.btnPauseContinue);

            btnShowMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   isMonitorUI=false;
                   setContentView(R.layout.activity_run_prepare);
                }
            });

            sbRunProgress.setMax(100);
            sbRunProgress.setProgress(0);
            sbRunProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    Toast.makeText(getApplicationContext(), "cnm", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            sbRunProgress.setOnTouchListener(new View.OnTouchListener() {// make the progress bar cannot be move by finger
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d("ACETEST", "监听");
                    return true;
                }
            });

        }//============================================================monitorUI out


         if(!isMonitorUI) {//=====================================map UI start


            binding = ActivityRunPrepareBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            //initial fuesedloc
            client = LocationServices.getFusedLocationProviderClient(this);

            //initial polyline
            polylineOptions = new PolylineOptions();

            //initial locationrequest and set request interval
            locationRequest = LocationRequest.create();
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            //initialize lastTime to current time in order to calculate speed
            lastTime = System.currentTimeMillis();

            startBtn = (Button) findViewById(R.id.startBtn);
            stopBtn = (Button) findViewById(R.id.stopBtn);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trackLoc = true;
                lastTime = System.currentTimeMillis();
                if ( overlay != null )
                    overlay.remove();
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trackLoc = false;
                prev = null;
                addHeatMap();
            }
        });

            //initial locationCallback
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);

                    if (!trackLoc)
                        return;

                    Location location = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (prev == null) {
                        prev = latLng;
                    }

                    currentZoomLevel = mMap.getCameraPosition().zoom;//get current zoom level
                    if (lockCamToUser) {
//                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,currentZoomLevel));
                        //make camera and user move in same direction
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latLng)
                                .tilt(0)
                                .zoom(currentZoomLevel)
                                .bearing(getBearing(temp, latLng))
                                .build();
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    }
                    //============================================================================heatmap
                    double speed = getSpeed(latLng);

                    travelData.add(latLng);
                    speedData.add(speed);
                    int color = getColorFromSpeed(speed);
                    mMap.addPolyline(new PolylineOptions().add(latLng, prev).color(color));

                    temp = latLng;
                    prev = latLng;
                    lastTime = System.currentTimeMillis();
                }
            };

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(RunPrepareActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            } else {
                // user's origin location
                startLocationUpdate();
            }
        }//===================else if end
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    //helper func that outputs direction of user

    private float getBearing(LatLng begin, LatLng end) {
        double dLon = (end.longitude - begin.longitude);
        double x = Math.sin(Math.toRadians(dLon)) * Math.cos(Math.toRadians(end.latitude));
        double y = Math.cos(Math.toRadians(begin.latitude))*Math.sin(Math.toRadians(end.latitude))
                - Math.sin(Math.toRadians(begin.latitude))*Math.cos(Math.toRadians(end.latitude)) * Math.cos(Math.toRadians(dLon));
        double bearing = Math.toDegrees((Math.atan2(x, y)));
        return (float) bearing;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RunPrepareActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                lockCamToUser=false;
                currentZoomLevel = mMap.getCameraPosition().zoom;//get current zoom level
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,currentZoomLevel));
            }
        });

        //add zoom+- buttons
        mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);


        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                mMap.moveCamera(CameraUpdateFactory.zoomTo(20));
                lockCamToUser=true;
                return false;
            }
        });


    }


    private void startLocationUpdate(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(RunPrepareActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }
        client.requestLocationUpdates(locationRequest,locationCallback,null);
        updateGPS();
    }

    private void updateGPS(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                ActivityCompat.requestPermissions(RunPrepareActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }else{
            client.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //get user's location and make camera follow the user
                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    currentZoomLevel = mMap.getCameraPosition().zoom;//get current zoom level
                    if(lockCamToUser){
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,currentZoomLevel=20));
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                }else{
                    Toast.makeText(this, "permission grant required",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

        }

    }

    public double getDistance( LatLng pos1, LatLng pos2 )
    {
        double dx = Math.cos(pos2.latitude)*Math.cos(pos2.longitude)
                - Math.cos(pos1.latitude)*Math.cos(pos1.longitude);
        double dy = Math.cos(pos2.latitude)*Math.sin(pos2.longitude)
                - Math.cos(pos1.longitude)*Math.sin(pos1.longitude);

        return Math.sqrt( (dx*dx)+(dy*dy) );

    }

    public double getSpeed( LatLng latLng )
    {
        double distance = getDistance( latLng, prev );
        double time = (System.currentTimeMillis() - lastTime) / 1000.0f;
        Log.i( flag, "Speed: " + (distance/time) );
        return (distance) / ( time );
    }

    public int getColorFromSpeed( double speed )
    {
        if ( speed < 0.9 )
        {
            Log.i( flag, "RED" );
            return RED;
        }
        else if ( speed < 1.5 )
        {
            Log.i( flag, "ORANGE" );
            return ORANGE;
        }
        else if ( speed >= 1.5 )
        {
            Log.i( flag, "GREEN" );
            return GREEN;
        }
        return 0;
    }

    public void setMaxMinSpeeds( double speed )
    {
        if( minSpeed > speed )
            minSpeed = speed;
        if( maxSpeed < speed )
            maxSpeed = speed;
    }

    public void filterWeights()
    {
        for( int i = 0; i < travelData.size(); i++ )
        {
            LatLng currentLatLng = travelData.get(i);
            Double curSpeed = speedData.get(i);
            Double weight = (curSpeed-minSpeed) / (maxSpeed-minSpeed);
            Log.i( flag, "Weight: " + weight );
            weightedTravelData.add( new WeightedLatLng( currentLatLng, curSpeed ) );
        }
    }

    private void addHeatMap() {

        // Create a weighted heat map, with the speeds being the weights

        // !!!IMPORTANT!!! - Modify the existing weights in weightedTravelData to being a 0 - 5 range
        filterWeights();

        // Create the gradient.
        int[] colors = {
                Color.rgb(45, 85, 250), // green
                Color.rgb(3, 227, 252)    // red
        };

        float[] startPoints = {
                0.2f, 1f
        };

        Gradient gradient = new Gradient(colors, startPoints);

        // Create a heat map tile provider, passing it the latlngs of the police stations.
        HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                .weightedData(weightedTravelData)
                .gradient(gradient)
                .build();

        // Add a tile overlay to the map, using the heat map tile provider.
        TileOverlay overlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
    }

}