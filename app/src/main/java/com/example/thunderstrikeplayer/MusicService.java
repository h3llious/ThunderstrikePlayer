package com.example.thunderstrikeplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int songPos;

    private final IBinder musicBind = new MusicBinder();

    private String songTitle = "";
    private static final int NOTIFY_ID = 1;

    final String NOTIFICATION_CHANNEL_ID = "thunderstrike";

    final static String ACTION_PAUSE = "PAUSE";
    final static String ACTION_PLAY = "PLAY";
    final static String ACTION_STOP_FOREGROUND_SERVICE = "STOP";

    private boolean shuffle = false;
    private Random rand;



    MainActivity main = null;

    void setMainActivityHandler(MainActivity main) {
        this.main = main;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        player = new MediaPlayer();
        songPos = 0;

        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                main.getController().show();
            }
        });


        rand = new Random();

        createNotificationChannel();

        initMusicPlayer();
    }

    public void setShuffle(MenuItem item) {
        if (shuffle) {
            shuffle = false;
            item.setIcon(R.drawable.rand);
        } else {
            shuffle = true;
            item.setIcon(R.drawable.ic_shuffle);
        }

        Toast.makeText(this, "Shuffle is " + shuffle, Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }
    }

    private void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnPreparedListener(this);
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        mp.reset();
        playNext();

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null) {
//            String action = intent.getAction();
//
//            switch (action) {
//                case ACTION_STOP_FOREGROUND_SERVICE:
////                    stopForegroundService();
//                    onDestroy();
////                    Toast.makeText(getApplicationContext(), "Foreground service is stopped.", Toast.LENGTH_SHORT).show();
//                    break;
//                case ACTION_PLAY:
//                    player.start();
////                    Toast.makeText(getApplicationContext(), "You click Play button.", Toast.LENGTH_SHORT).show();
//                    break;
//                case ACTION_PAUSE:
//                    player.pause();
////                    Toast.makeText(getApplicationContext(), "You click Pause button.", Toast.LENGTH_SHORT).show();
//                    break;
//            }
//        }
//
//
//        return super.onStartCommand(intent, flags, startId);
//    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);


        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);

        Intent playIntent = new Intent(ACTION_PLAY);
//        playIntent.setAction(ACTION_PLAY);
        PendingIntent pendingPlayIntent = PendingIntent.getBroadcast(this, 0, playIntent, 0);
        NotificationCompat.Action playAction = new NotificationCompat.Action(android.R.drawable.btn_star, "Play", pendingPlayIntent);
        builder.addAction(playAction);

        Intent pauseIntent = new Intent(ACTION_PAUSE);
//        playIntent.setAction(ACTION_PAUSE);
        PendingIntent pendingPauseIntent = PendingIntent.getBroadcast(this, 0, pauseIntent, 0);
        NotificationCompat.Action pauseAction = new NotificationCompat.Action(R.drawable.end, "Pause", pendingPauseIntent);
        builder.addAction(pauseAction);

        Intent stopIntent = new Intent(ACTION_STOP_FOREGROUND_SERVICE);
//        playIntent.setAction(ACTION_STOP_FOREGROUND_SERVICE);
        PendingIntent pendingStopIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);
        NotificationCompat.Action stopAction = new NotificationCompat.Action(R.drawable.end, "Stop", pendingStopIntent);
        builder.addAction(stopAction);


        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);


    }

    public void setSong(int songIndex) {
        songPos = songIndex;
    }

    public void playSong() {
        player.reset();
        Song playSong = songs.get(songPos);
        songTitle = playSong.getTitle();
        long currSong = playSong.getId();
        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);

        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(trackUri, null, null, null, null);
        if (cur != null) {
            if (cur.moveToFirst()) {
                String filePath = cur.getString(0);

                if (new File(filePath).exists()) {
                    // do something if it exists
                } else {
                    //trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, currSong);
                }
            } else {
                // Uri was ok but no entry found.
                trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, currSong);
            }
            cur.close();
        } else {
            // content Uri was invalid or some other error occurred
            //trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, currSong);
        }


        try {
            player.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "What, bug?");
        }

        player.prepareAsync();


    }

    @Override
    public void onDestroy() {

        if (player != null) {
            player.stop();
            player.release();
        }
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    public int getPos() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void stopPlayer(){
        player.stop();
    }

    public void seek(int pos) {
        player.seekTo(pos);
    }

    public void go() {
        player.start();
    }

    public void playNext() {
        if (shuffle) {
            int newSong = songPos;
            while (newSong == songPos) {

                newSong = rand.nextInt(songs.size());
            }
            songPos = newSong;
        } else {
            songPos++;
            if (songPos == songs.size())
                songPos = 0;
        }

        playSong();
    }

    public void playPrev() {
        songPos--;
        if (songPos == 0)
            songPos = songs.size() - 1;
        playSong();
    }
}
