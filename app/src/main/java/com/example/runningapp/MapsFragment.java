package com.example.runningapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

public class MapsFragment extends Fragment {

    public static final int PERMISSION_FINE_LOCATION = 44;

    private ControlFragmentListener CFL;

    private GoogleMap mMap;
    private FusedLocationProviderClient client;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private PolylineOptions polylineOptions;
    private boolean lockCamToUser = true;
    private UiSettings mUiSettings;
    private static float currentZoomLevel=20;
    private LatLng temp;
    private double alreadyRunDistance =0;
    boolean isPause=true;
    private Button btnBackToMonitor;

    private ArrayList<LatLng> everySecLocationList = new ArrayList<LatLng>();
    private ArrayList<LatLng> continueLocationList = new ArrayList<LatLng>();

    public interface ControlFragmentListener{
        public void SendMapMessage(double Distance, ArrayList<LatLng> alreadRunDistance, ArrayList<LatLng> continueLocation);
        public void GoBackToMonitor();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        CFL = (ControlFragmentListener) context;
    }

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        @Override
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;
            mMap.moveCamera(CameraUpdateFactory.zoomTo(20)); // default zoom level
            // check permission
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions( new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
            mMap.setMyLocationEnabled(true);
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(@NonNull LatLng latLng) {
                    lockCamToUser = false;
                    currentZoomLevel = mMap.getCameraPosition().zoom;// get current zoom level
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,currentZoomLevel));//set camera
                }
            });

            // add zooming buttons
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

            client = LocationServices.getFusedLocationProviderClient(getContext());
            findMeWhenOpen();
            //initial polyline
            polylineOptions = new PolylineOptions();

            // initial the location request and set request interval
            locationRequest = LocationRequest.create();
            locationRequest.setInterval(5000); // 5 sec
            locationRequest.setFastestInterval(1000); // 1 sec
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);// use one sec as callback interval


            // initial locationCallback to get location every second
            locationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    if (!isPause) {
                    // get location per second
                    Location location = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    currentZoomLevel = mMap.getCameraPosition().zoom;// get current zoom level
                    if (lockCamToUser && temp!=null) {
                        // make camera and user move in same direction
                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latLng)
                                .tilt(50)
                                .zoom(currentZoomLevel)
                                .bearing(getBearing(temp, latLng))
                                .build();
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    }

                    if(temp!=null)    { // has previous location
                        double speed = getSpeed(latLng);
                        alreadyRunDistance += speed;//update pass distance
                        // add a new position and draw polyline
                        mMap.addPolyline(new PolylineOptions().add(temp, latLng));

                    }

                    temp = latLng; // update previous location
                    everySecLocationList.add(latLng);// record location
                    CFL.SendMapMessage(alreadyRunDistance,everySecLocationList,continueLocationList);//send info to activity
                }
                }
            };

            // check permission
            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }else{
                // user's origin location
                startLocationUpdate();
            }

        }
    };

    // when convert pause and continue state, update list that record location every second and location when continue pressed.
    public void pauseContinue(){
        isPause = !isPause;
        if(isPause == false){
            updateGPS();
        }
    }

    // update gps for one time
    private void updateGPS(){
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }else{
            client.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // after pause, when user continue running, set latest location as previous location and record them in list.
                    // we will use these two location list when drawing routes.
                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    temp=latLng;
                    continueLocationList.add(temp);
                    everySecLocationList.add(temp);
                }
            });
        }
    }

    // show user's location when start activity
    private void findMeWhenOpen(){
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
            }
        }else{
            client.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //get user's location and make camera follow the user
                    LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,currentZoomLevel));
                }
            });
        }
    }

    // get distance between two geo location
    // reference:https://stackoverflow.com/questions/27928/calculate-distance-between-two-latitude-longitude-points-haversine-formula
    public static double getDistance(double lat1, double lat2, double lon1, double lon2) {
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);
        double c = 2 * Math.asin(Math.sqrt(a));
        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;
        // calculate the result
        return(c * r);
    }

    public double getSpeed( LatLng latLng )
    {
        double distance = getDistance( latLng.latitude, temp.latitude,latLng.longitude,temp.longitude);
        return (distance)/1.6 ;// 1.6 for mile unit
    }

    // helper func that outputs direction of user
    private float getBearing(LatLng begin, LatLng end) {
        double dLon = (end.longitude - begin.longitude);
        double x = Math.sin(Math.toRadians(dLon)) * Math.cos(Math.toRadians(end.latitude));
        double y = Math.cos(Math.toRadians(begin.latitude))*Math.sin(Math.toRadians(end.latitude))
                - Math.sin(Math.toRadians(begin.latitude))*Math.cos(Math.toRadians(end.latitude)) * Math.cos(Math.toRadians(dLon));
        double bearing = Math.toDegrees((Math.atan2(x, y)));
        return (float) bearing;
    }

    private void startLocationUpdate(){
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FINE_LOCATION);
        }
        // start location update callback
        client.requestLocationUpdates(locationRequest,locationCallback,null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // stop location update callback
        client.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // inflate the view
        View view = inflater.inflate(R.layout.fragment_maps, container, false);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        // render map
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
        // switch UI
        btnBackToMonitor = (Button) view.findViewById(R.id.btnBacktoMonitor);
        btnBackToMonitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CFL.GoBackToMonitor();
            }
        });
        return view;
    }
}
