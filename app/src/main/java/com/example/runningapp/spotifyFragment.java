package com.example.runningapp;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.fragment.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;


import java.util.ArrayList;
import java.util.List;


import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;


public class spotifyFragment extends Fragment {

    /**
     * */

    private ImageView play_pause_img;
    private ImageView skip_next;
    private ImageView skip_previous;
    private Button backToMonitor;
    private TextView artist;
    private TextView trackName;
    private ImageView invite;
    private TextView friend_name;
    private TextView musicProgress;
    private com.google.android.material.imageview.ShapeableImageView friendImg;
    private SeekBar sbr;
    private Track track;
    private Spinner playlistSpinner;
    private Spinner friendlistSpinner;
    private long pos;
    long position;

    String song_uri;
    private Handler handler = new Handler();
    private boolean isPlaying = true;
    private boolean firstPlay = true;

    /**
     * Required parameters for spotify authorization by Spotify
     * */
    private static final int REQUEST_CODE = 1337;
    private static final String CLIENT_ID = "256b771d2bc24a1fbde3be0375162fce";
    private static final String REDIRECT_URI = "http://example.com/callback/";
    Pager<PlaylistSimple> playlistSimplePager;
    private List<PlaylistSimple> playListItem;
    private List<String> playListNames = new ArrayList<>();

    private SpotifyAppRemote mSpotifyAppRemote;

    public spotifyFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_spotify, container, false);
        play_pause_img = (ImageView) view.findViewById(R.id.imgSpotifyPlayPause);
        skip_previous = (ImageView) view.findViewById(R.id.imgSpotifyToPreivious);
        skip_next = (ImageView) view.findViewById(R.id.imgSpotifyToNext);
        backToMonitor = (Button)view.findViewById(R.id.btnSpotifyBackToMonitor);
        sbr = (SeekBar) view.findViewById(R.id.sbSpotify);
        musicProgress = (TextView) view.findViewById(R.id.tvSpotifyPercent);
        sbr.setMax(100);
        sbr.setProgress(0);
        playlistSpinner = view.findViewById(R.id.spSpotifyPlaylists);
        trackName = view.findViewById(R.id.tvSpotifytrackName);
        artist = view.findViewById(R.id.tvSpotifyArtist);
        backToMonitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MBC.BackToControl();
            }
        });

        // play or stop the player
        play_pause_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (track != null) {
                    if(isPlaying){
                        handler.removeCallbacks(updater);
                        mSpotifyAppRemote.getPlayerApi().pause();
                        play_pause_img.setImageResource(R.drawable.ic_baseline_play);
                        isPlaying = false;
                        mSpotifyAppRemote.getPlayerApi()
                                .subscribeToPlayerState()
                                .setEventCallback(playerState -> {
                                    track = playerState.track;
                                    if (track != null) {
                                        Log.d("MainActivity", "track uri is "+ track.uri);
                                    }else{
                                        Log.d("MainActivity", "track is null");
                                    }
                                });
                    }else{
                        mSpotifyAppRemote.getPlayerApi().resume();
                        isPlaying = true;
                        play_pause_img.setImageResource(R.drawable.ic_baseline_pause);
                        updateSeekBar();
                    }
                }else{
                    Log.d("MainActivity", "no track is playing");
                }
            }
        });
        // User can skip to previous or next track
        skip_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSpotifyAppRemote.getPlayerApi().skipNext();
            }
        });
        skip_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mSpotifyAppRemote.getPlayerApi().seekTo(0);
                mSpotifyAppRemote.getPlayerApi().skipPrevious();


            }
        });
        // User can adjust song's progress by dragging the seekbar
        sbr.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                handler.removeCallbacks(updater);
                if(b){
                    long duration = track.duration;
                    position = (long) (i*duration)/100;
                    musicProgress.setText(""+i+"%");
                    sbr.setProgress(i);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSpotifyAppRemote.getPlayerApi().seekTo(position);
                updateSeekBar();
            }
        });

        return view;
    }

    /**
     * Start a thread for updating seekbar
     * */
    private Runnable updater = new Runnable() {
        @Override
        public void run() {
            updateSeekBar();
        }
    };

    /**
     * Update the seekbar according to the music progress in real time
     * */
    private void updateSeekBar() {
        if (isPlaying) {
            mSpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
                @Override
                public void onResult(PlayerState data) {
                    pos = data.playbackPosition;
                }
            });
            sbr.setProgress((int)(((double)pos/(double)track.duration)*100));
            musicProgress.setText(""+(int)(((double)pos/(double)track.duration)*100)+"%");
            handler.postDelayed(updater,1);
        }
    }

    /**
     * Interface for fragment communication
     * */
    public interface MusicBackToControl {
        public void BackToControl(); //
    }
    MusicBackToControl MBC;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        MBC = (MusicBackToControl) context;
    }

    /**
     * Authorize our Application to be able to use the App Remote SDK.
     * Use the built-in authorization method provided by Spotify API tutorial
     * Reference - https://developer.spotify.com/documentation/android/quick-start/
     * */
    @Override
    public void onStart() {
        super.onStart();
        Log.d("MainActivity", "first play?" + firstPlay);
        if(firstPlay){
            firstPlay = false;
            // spotify build-in authentication. First set the connection parameters
            ConnectionParams connectionParams =
                    new ConnectionParams.Builder(CLIENT_ID)
                            .setRedirectUri(REDIRECT_URI)
                            .showAuthView(true)
                            .build();

            SpotifyAppRemote.connect(this.getActivity(), connectionParams,
                    new Connector.ConnectionListener() {

                        @Override
                        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                            mSpotifyAppRemote = spotifyAppRemote;
                            Log.d("MainActivity", "Connected! Yay!");
                            // Now you can start interacting with App Remote
                            connected();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.e("MainActivity", throwable.getMessage(), throwable);
                            // Something went wrong when attempting to connect! Handle errors here
                        }
                    });
        }else{
            connected();
        }
    }

    /**
     * After connection done, update the music page according to the state of spotify player
     * */
    private void connected() {
        //get the player state of spotify player
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(playerState -> {
                    track = playerState.track;
                    isPlaying = !playerState.isPaused; //check if the player is playing
                    if (track != null) {
                        if(isPlaying){
                            isPlaying = true; //if the player is playing, udpate the play/pause button
//                            play_pause_img.setImageResource(R.drawable.ic_baseline_pause);
                            trackName.setText(track.name);
                            artist.setText("By "+track.artist.name);
                        }
                        updateSeekBar(); // get the relevant information to update the seekbar
                    }else{
                        Log.d("MainActivity", "track is null");
                    }
                });
    }

    /**
     * Get playlist data from the host activity
     * */
    public void getPlaylist(Pager<PlaylistSimple> pls){
        playlistSimplePager = pls;
        playListItem = playlistSimplePager.items;
        for(PlaylistSimple playlistSimple: playListItem) {
            playListNames.add(playlistSimple.name);
        }
        initSpinner();

    }

    /**
     * Initialize the playlist spinner
     * */
    private void initSpinner() {
        ArrayAdapter<String> starAdapter = new ArrayAdapter<String>(getContext(), R.layout.item_select, playListNames);
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        playlistSpinner.setAdapter(starAdapter);
        playlistSpinner.setOnItemSelectedListener(new MySelectedListener());
    }


    /**
     * automatically play the selected playlist
     * */
    class MySelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if(i==0){
                return;
            }
            mSpotifyAppRemote.getPlayerApi().play(playListItem.get(i).uri);
            isPlaying = true;
            play_pause_img.setImageResource(R.drawable.ic_baseline_pause);
            connected();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    }
}



