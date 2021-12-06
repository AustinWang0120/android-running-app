package com.example.runningapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import retrofit.client.Response;


public class OneForAll extends AppCompatActivity implements MapsFragment.ControlFragmentListener,
        MonitorFragment.MonitorFragmentListener,
        spotifyFragment.MusicBackToControl {

    private static final String TAG = "one-for-all-activity";

    private FragmentManager fragmentManager;
    private Fragment mapFragment;
    private Fragment monitorFragment;
    private Fragment spotifyFragment;
    private MonitorFragment mf;
    private ArrayList<LatLng> everySecLocationOFA;
    private ArrayList<LatLng> continueLocationOFA;

    private String goal;

    // Spotify Auth parameters
    private static final int REQUEST_CODE = 1337;
    private static final String REDIRECT_URI = "http://example.com/callback/";
    private static final String CLIENT_ID = "256b771d2bc24a1fbde3be0375162fce";
    private static String myToken;

    /**
     * Setup All Fragments
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_for_all);

        // get the goal of running
        Bundle bundle = getIntent().getExtras();
        goal = bundle.getString("goal");

        /**
         * Spotify client to authenticate the user
         * Use the method provided by Spotify Android SDK Authentication and Authorization Guide
         * Reference https://developer.spotify.com/documentation/android/guides/android-authentication/
         * */
        Log.e(TAG,"spotify authentication");
        AuthorizationClient.clearCookies(this);
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"streaming"});
        AuthorizationRequest request = builder.build();
        // onActivityResult for the request's result
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);

        // initialize map fragment
        mapFragment = new MapsFragment();
        spotifyFragment = new spotifyFragment();
        monitorFragment = new MonitorFragment();
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(R.id.frame_layout, mapFragment,"tagMap");
        ft.add(R.id.frame_layout,monitorFragment,"tagMonitor");
        ft.add(R.id.frame_layout, spotifyFragment,"tagMusic");
        ft.commit();
    }

    /**
     * receive the authorization result
     * Response：Token
     * */
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Return TOKEN -> successful
                case TOKEN:
                    Log.e(TAG,"spotify authentication done");
                    myToken = response.getAccessToken();
                    Log.e(TAG,"spotify authentication token is " + myToken);

                    // Use the Spotify Web Api to get user's playlists
                    SpotifyApi api = new SpotifyApi();
                    api.setAccessToken(myToken);
                    SpotifyService spotify = api.getService();
                    spotify.getMyPlaylists(new SpotifyCallback<Pager<PlaylistSimple>>() {

                        @Override
                        public void failure(SpotifyError spotifyError) {
                            Log.e(TAG, "Spotify API: playlist failure");
                            // do nothing...
                        }

                        @Override
                        public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                            // for(PlaylistSimple playlistSimple: playlistSimplePager.items) {
                                // Log.e(TAG, playlistSimple.name);
                            // }
                            // pass the playlist data into the SpotifyFragment
                            spotifyFragment sf = (spotifyFragment) getSupportFragmentManager().findFragmentByTag("tagMusic");
                            sf.getPlaylist(playlistSimplePager);
                        }
                    });
                    break;
                // Error -> handle error
                case ERROR:
                    // do nothing...
                    Log.d(TAG, "onActivityResult: spotify authentication failure");
                    break;

                default:
                    // do nothing...
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // find the map fragment and start it as default
        mf = (MonitorFragment) getSupportFragmentManager().findFragmentByTag("tagMonitor");
        mf.setMonitorGoal(goal);
    }

    @Override
    public void SendMapMessage(double Distance, ArrayList<LatLng> everSecLocation, ArrayList<LatLng> continueLocation) {
        mf = (MonitorFragment) getSupportFragmentManager().findFragmentByTag("tagMonitor");
        mf.setMonitorData(Distance, everSecLocation);
        mf.setMonitorGoal(goal);
        everySecLocationOFA = everSecLocation;
        continueLocationOFA = continueLocation;
    }

    @Override
    public void GoBackToMonitor() {
        // hide the map and show the monitor
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.hide(mapFragment);
        ft.show(monitorFragment);
        ft.commit();
    }

    @Override
    public void SendMonitorMessage() {
        // hide the monitor and show the map
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.hide(monitorFragment);
        ft.show(mapFragment);
        ft.commit();
    }

    @Override
    public void SendMusicMessage() {
        // hide everything and show the spotify page
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.hide(monitorFragment);
        ft.hide(mapFragment);
        ft.show(spotifyFragment);
        ft.commit();
    }

    @Override
    public void SendMonitorPauseContinue() {
        MapsFragment mp = (MapsFragment) getSupportFragmentManager().findFragmentByTag("tagMap");
        mp.pauseContinue();
    }

    /**
     * Put all info in the bundle and push to the ReportActivity
     * */
    @Override
    public void SendGoReportRequest(String startTime, String distance, String avgPace,
                                    String duration, String calories, String avgSteps, String totalSteps) {
        Intent goReportActivity = new Intent(getBaseContext(), ReportActivity.class);
        goReportActivity.putExtra("startTime", startTime);
        goReportActivity.putExtra("distance", distance);
        goReportActivity.putExtra("avgPace", avgPace);
        goReportActivity.putExtra("duration", duration);
        goReportActivity.putExtra("calories", calories);
        goReportActivity.putExtra("avgSteps", avgSteps);
        goReportActivity.putExtra("totalSteps", totalSteps);
        goReportActivity.putExtra("everySecLocation", everySecLocationOFA);
        goReportActivity.putExtra("continueLocation", continueLocationOFA);
        startActivity(goReportActivity);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Do nothing...
    }

    @Override
    public void BackToControl() {
        // hide the spotify page ann show the monitor
        FragmentTransaction ft = fragmentManager.beginTransaction ();
        ft.hide(spotifyFragment);
        ft.show(monitorFragment);
        ft.commit();
    }

    @Override
    protected void onDestroy() {
        // clear the spotify authorization token
        super.onDestroy();
        AuthorizationClient.clearCookies(this);
    }
}