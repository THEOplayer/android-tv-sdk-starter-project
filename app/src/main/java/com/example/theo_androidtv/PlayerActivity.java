package com.example.theo_androidtv;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;


import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.MutableLiveData;


import com.theoplayer.android.api.event.player.PlayerEventTypes;
import com.theoplayer.android.api.event.track.mediatrack.video.list.VideoTrackListEventTypes;
import com.theoplayer.android.api.event.track.texttrack.list.TextTrackListEventTypes;
import com.theoplayer.android.api.player.Player;
import com.theoplayer.android.api.player.track.mediatrack.quality.QualityList;
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality;
import com.theoplayer.android.api.player.track.texttrack.TextTrack;
import com.theoplayer.android.api.player.track.texttrack.TextTrackMode;
import com.theoplayer.android.api.source.SourceDescription;
import com.theoplayer.android.api.source.SourceType;
import com.theoplayer.android.api.source.TypedSource;
import com.example.theo_androidtv.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.theoplayer.android.api.source.SourceDescription.Builder.sourceDescription;

public class PlayerActivity extends Activity {

    private static final String TAG = PlayerActivity.class.getSimpleName();
    private Typeface fontAwesome;
    private ActivityMainBinding viewBinding;
    Handler timeout = new Handler();
    final double SKIP_FORWARD_SECS = 10;
    final int INACTIVITY_SECONDS = 10;
    final double VOLUME_DELTA = 0.05;
    final double SKIP_BACKWARD_SECS = 10;
    final double[] playbackRates = {-2.0, -1.50, -1.25, -1, -.5, .5, 1.0, 1.25, 1.50, 2.00};
    int currentPlaybackRateIndex = 6;
    double currentTime;
    Player player;
    int currentTextTrackIndex = 0;
    MutableLiveData<Boolean> trickPlayVisible = new MutableLiveData<>();
    final Runnable r = () -> trickPlayVisible.setValue(false);

    Map<String, TextTrack> textTracks = new HashMap<>();
    Map<String, VideoQuality> qualities = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        fontAwesome = Typeface.createFromAsset(this.getAssets(), "fa.otf");


        // Getting all buttons within trickbar control
        for (int index = 0; index <= viewBinding.trickbar.getChildCount(); index++) {
            View innerControl = viewBinding.trickbar.getChildAt(index);
            if (innerControl instanceof Button) {
                Button b = (Button) innerControl;

                // applying font awesome
                b.setTypeface(fontAwesome);

                // adding focus change listener and changing background depending on the focus state
                b.setOnFocusChangeListener((view, bl) -> {
                    int color = bl ? ContextCompat.getColor(PlayerActivity.this, R.color.THEOBlue) :
                            ContextCompat.getColor(PlayerActivity.this, R.color.THEOYellow);
                    view.setBackgroundTintList(ColorStateList.valueOf(color));
                });

                // hiding trickbar upon pressing key down
                b.setOnKeyListener((view, i, keyEvent) -> {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                        timeout.removeCallbacks(r);
                        trickPlayVisible.setValue(false);
                    }
                    // return false to allow upper components to handle they keystroke
                    return false;
                });
            }
        }


        // Setting the player reference for future use
        player = viewBinding.theoPlayerView.getPlayer();

        //Configure the player
        configureTheoPlayer();

        // Hiding the trickbar to disable the keys when the hiding animation is complete
        viewBinding.trickbar.animate().setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!trickPlayVisible.getValue()) {
                    viewBinding.trickbar.setVisibility(View.GONE);
                }
            }
        });

        // Observing trickPlayVisible livedata and showing/hiding the trickbar accordingly
        trickPlayVisible.observeForever(visible -> {
            if (visible) {
                viewBinding.trickbar.setVisibility(View.VISIBLE);
                viewBinding.trickbar.animate().translationY(0).alpha(1);
            } else {
                viewBinding.trickbar.animate().translationY(64).alpha(0);
            }
        });

        // Play/pause action
        viewBinding.playPauseBtn.setOnClickListener(v -> {
            if (player.isPaused()) {
                player.play();
            } else {
                player.pause();
            }
        });

        // Volume up
        viewBinding.volumeUpBtn.setOnClickListener(view -> {
            double currentVolume = player.getVolume();
            if (currentVolume < 1) {
                player.setVolume(currentVolume + VOLUME_DELTA);
            }
        });

        // Volume down
        viewBinding.volumeDownBtn.setOnClickListener(view -> {
            double currentVolume = player.getVolume();
            if (currentVolume > 0) {
                player.setVolume(currentVolume - VOLUME_DELTA);
            }
        });

        // Skipping forward by SKIP_FORWARD_SECS seconds
        viewBinding.forwardBtn.setOnClickListener(view -> player.setCurrentTime(currentTime + SKIP_FORWARD_SECS));

        // Skipping backward by SKIP_BACKWARD_SECS seconds
        viewBinding.backwardBtn.setOnClickListener(view -> player.setCurrentTime(currentTime - SKIP_BACKWARD_SECS));


        // Setting faster playback rate based on the playbackrates array
        viewBinding.ffwdBtn.setOnClickListener(view -> {
            if (currentPlaybackRateIndex < playbackRates.length - 1) {
                currentPlaybackRateIndex++;
            }
            player.setPlaybackRate(playbackRates[currentPlaybackRateIndex]);
        });

        // Setting slower playback rate based on the playbackrates array
        viewBinding.fbckBtn.setOnClickListener(view -> {
            if (currentPlaybackRateIndex > 0) {
                currentPlaybackRateIndex--;
            }
            player.setPlaybackRate(playbackRates[currentPlaybackRateIndex]);
        });

        // Showing dialog to chose subtitles/cc track
        viewBinding.ccBtn.setOnClickListener(view -> {
            final ArrayList<String> items = new ArrayList<>();
            items.add(getString(R.string.none));

            for (Map.Entry<String, TextTrack> entry : textTracks.entrySet()) {
                items.add(entry.getKey());
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(PlayerActivity.this, R.style.THEO_Dialog));
            LayoutInflater inflater = getLayoutInflater();
            View convertView = inflater.inflate(R.layout.list, null);
            builder.setView(convertView);

            builder.setIcon(R.drawable.cc);
            builder.setTitle(getString(R.string.selectSubtitles));
            final AlertDialog dialog = builder.create();
            ListView lv = convertView.findViewById(R.id.lv);
            final ArrayAdapter<String> adapter = new ArrayAdapter(PlayerActivity.this, R.layout.list_item, items);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener((adapterView, view1, i, l) -> {
                currentTextTrackIndex = i;
                String strName = items.get(i);

                // First disable other tracks
                for (Map.Entry<String, TextTrack> entry : textTracks.entrySet()) {
                    entry.getValue().setMode(TextTrackMode.DISABLED);
                }

                // If a track different than 'none' was selected, set its mode to SHOWING
                if (!strName.equals(getString(R.string.none))) {
                    TextTrack textTrack = textTracks.get(strName);
                    textTrack.setMode(TextTrackMode.SHOWING);
                }
                dialog.dismiss();
            });
            // Try to focus back on CC button on cancel dialog
            dialog.setOnCancelListener(dialogInterface -> {
                viewBinding.trickbar.requestFocus();
                viewBinding.ccBtn.requestFocus();
            });
            // Try to focus back on CC button dismiss dialog
            dialog.setOnDismissListener(dialogInterface -> {
                viewBinding.trickbar.requestFocus();
                viewBinding.ccBtn.requestFocus();
            });
            dialog.show();

            // Stylize the CC button to appear "pressed" when a CC track is chosen
            if (currentTextTrackIndex != 0) {
                int color = ContextCompat.getColor(PlayerActivity.this, R.color.THEOYellowAlt);
                viewBinding.ccBtn.setBackgroundTintList(ColorStateList.valueOf(color));
                viewBinding.ccBtn.setBackgroundTintMode(PorterDuff.Mode.DARKEN);
            } else {
                int color = ContextCompat.getColor(PlayerActivity.this, R.color.THEOYellow);
                viewBinding.ccBtn.setBackgroundTintList(ColorStateList.valueOf(color));
                viewBinding.ccBtn.setBackgroundTintMode(PorterDuff.Mode.DARKEN);
            }
        });

        // Showing dialog to chose subtitles/cc track
        viewBinding.qualityBtn.setOnClickListener(view -> {
            final ArrayList<String> items = new ArrayList<>();

            for (Map.Entry<String, VideoQuality> entry : qualities.entrySet()) {
                items.add(entry.getKey());
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(PlayerActivity.this, R.style.THEO_Dialog));
            LayoutInflater inflater = getLayoutInflater();
            View convertView = inflater.inflate(R.layout.list, null);
            builder.setView(convertView);

            builder.setIcon(R.drawable.cc);
            builder.setTitle(getString(R.string.selectSubtitles));
            final AlertDialog dialog = builder.create();
            ListView lv = convertView.findViewById(R.id.lv);
            final ArrayAdapter<String> adapter = new ArrayAdapter(PlayerActivity.this, R.layout.list_item, items);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener((adapterView, view1, i, l) -> {
                currentTextTrackIndex = i;
                String strName = items.get(i);

                // If a track different than 'none' was selected, set its mode to SHOWING
                if (!strName.equals(getString(R.string.none))) {
                    VideoQuality targetQuality = qualities.get(strName);
                    Log.i(TAG, "targetQuality: " + targetQuality.getWidth() + "x" + targetQuality.getHeight());
                    player.getVideoTracks().getItem(0).setTargetQuality(targetQuality);
                }

                dialog.dismiss();
            });
            // Try to focus back on CC button on cancel dialog
            dialog.setOnCancelListener(dialogInterface -> {
                viewBinding.trickbar.requestFocus();
                viewBinding.qualityBtn.requestFocus();
            });
            // Try to focus back on CC button dismiss dialog
            dialog.setOnDismissListener(dialogInterface -> {
                viewBinding.trickbar.requestFocus();
                viewBinding.qualityBtn.requestFocus();
            });
            dialog.show();

            // Stylize the CC button to appear "pressed" when a CC track is chosen
            if (currentTextTrackIndex != 0) {
                int color = ContextCompat.getColor(PlayerActivity.this, R.color.THEOYellowAlt);
                viewBinding.qualityBtn.setBackgroundTintList(ColorStateList.valueOf(color));
                viewBinding.qualityBtn.setBackgroundTintMode(PorterDuff.Mode.DARKEN);
            } else {
                int color = ContextCompat.getColor(PlayerActivity.this, R.color.THEOYellow);
                viewBinding.qualityBtn.setBackgroundTintList(ColorStateList.valueOf(color));
                viewBinding.qualityBtn.setBackgroundTintMode(PorterDuff.Mode.DARKEN);
            }
        });

        // In INACTIVITY_SECONDS seconds of inactivity hide the trickbar
        timeout.postDelayed(r, INACTIVITY_SECONDS * 1000);
    }


    private void configureTheoPlayer() {
        // Creating a TypedSource builder that defines the location of a single stream source
        // and has Widevine DRM parameters applied.
        // TypedSource.Builder typedSource = typedSource(getString(R.string.defaultSourceUrl));
        //   .drm(drmConfiguration.build());

        TypedSource typedSource = TypedSource.Builder
                .typedSource()
                .src(getString(R.string.defaultSourceUrl))
                .liveOffset(1.0)
                .lowLatency(true)
                .timeServer("https://time.akamai.com/?ios&ms=true")
                .type(SourceType.DASH)
                .build();


        // Creating a SourceDescription builder that contains the settings to be applied as a new
        // THEOplayer source.
        SourceDescription.Builder sourceDescription = sourceDescription(typedSource)
                .poster(getString(R.string.defaultPosterUrl));

        //Setting the source to the player
        player.setSource(sourceDescription.build());

        //Setting the Autoplay to true
        player.setAutoplay(true);


        //Playing the source in the FullScreen Landscape mode
        viewBinding.theoPlayerView.getSettings().setFullScreenOrientationCoupled(true);

        // Build text track list
        player.getTextTracks().addEventListener(TextTrackListEventTypes.ADDTRACK, addTrackEvent -> {
            String id = addTrackEvent.getTrack().getLabel() + ": " + addTrackEvent.getTrack().getLanguage();
            textTracks.put(id, addTrackEvent.getTrack());
        });

        // Update play button icons on PLAY, PAUSE and ERROR events
        player.addEventListener(PlayerEventTypes.PLAY, playEvent -> {
            viewBinding.playPauseBtn.setText(getString(R.string.pauseIcon));
        });
        player.addEventListener(PlayerEventTypes.PAUSE, pauseEvent -> viewBinding.playPauseBtn.setText(getString(R.string.playIcon)));
        player.addEventListener(PlayerEventTypes.ERROR, pauseEvent -> viewBinding.playPauseBtn.setText(getString(R.string.playIcon)));

        // Adding listeners to THEOplayer content protection events.
        player.addEventListener(PlayerEventTypes.CONTENTPROTECTIONSUCCESS, event -> Log.i(TAG, "Event: CONTENT_PROTECTION_SUCCESS, mediaTrackType=" + event.getMediaTrackType()));
        player.addEventListener(PlayerEventTypes.CONTENTPROTECTIONERROR, event -> Log.i(TAG, "Event: CONTENT_PROTECTION_ERROR, error=" + event.getError()));


        //Adding the Video Quality tracks in the List
        player.getVideoTracks().addEventListener(VideoTrackListEventTypes.ADDTRACK, event -> {
            QualityList<VideoQuality> qualitiesArray = player.getVideoTracks().getItem(0).getQualities();
            for (int i = 0; i < qualitiesArray.length(); i++) {
                String id = qualitiesArray.getItem(i).getWidth() + "x" + qualitiesArray.getItem(i).getHeight();
                qualities.put(id, qualitiesArray.getItem(i));
            }
        });

        // Update time and progress bar
        player.addEventListener(PlayerEventTypes.TIMEUPDATE, timeUpdateEvent -> {
            currentTime = timeUpdateEvent.getCurrentTime();
            boolean isLive = Double.isNaN(player.getDuration());
            String text;
            if (isLive) {
                // If live stream, only show current time
                text = getString(R.string.live) + " " + formatTime((int) currentTime);
                viewBinding.progress.setProgress(100);
            } else {
                double duration = player.getDuration();
                int progress = (int) Math.round(currentTime / duration * 100);
                text = formatTime((int) currentTime) + getString(R.string.timeProgressSeparator) + formatTime((int) duration);
                viewBinding.progress.setProgress(progress);
            }
            viewBinding.time.setText(text);

            if (player.getVideoTracks().length() > 0) {
                VideoQuality activeQuality = player.getVideoTracks().getItem(0).getActiveQuality();
                if (activeQuality != null) {
                    Log.i(TAG, "Event: activeQuality:" + activeQuality.getWidth() + "x" + activeQuality.getHeight());

                }
            }
        });


    }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            // When a key is hit, cancel the timeout of hiding the trick bar and set it again
            timeout.removeCallbacks(r);
            timeout.postDelayed(r, INACTIVITY_SECONDS * 1000);
            trickPlayVisible.setValue(true);
            super.dispatchKeyEvent(event);
            return false;
        }


        // Overriding default events
        @Override
        protected void onPause() {
            super.onPause();
            viewBinding.theoPlayerView.onPause();
        }

        @Override
        protected void onResume() {
            super.onResume();
            viewBinding.theoPlayerView.onResume();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            viewBinding.theoPlayerView.onDestroy();
        }

        // Make seconds look nice
        public String formatTime(long secs) {
            return String.format(getString(R.string.timeFormat), secs / 3600, (secs % 3600) / 60, secs % 60);
        }
}
