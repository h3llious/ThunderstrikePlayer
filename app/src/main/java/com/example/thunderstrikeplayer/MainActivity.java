package com.example.thunderstrikeplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    final static int MY_PERMISSIONS_REQUEST_READ_MEDIA = 1;

    ArrayList<Song> songList;
    RecyclerView songView;

    private MusicService musicService;

    private Intent playIntent;
    private boolean musicBound = false;

    private MusicController controller;

    private boolean paused = false, playbackPaused = false;

    PauseReceiver pauseReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        pauseReceiver = null;
        pauseReceiver = new PauseReceiver();
        pauseReceiver.setMainActivityHandler(this);

        final IntentFilter filter = new IntentFilter(MusicService.ACTION_PAUSE);
        final IntentFilter filter2 = new IntentFilter(MusicService.ACTION_PLAY);
        final IntentFilter filter3 = new IntentFilter(MusicService.ACTION_STOP_FOREGROUND_SERVICE);

        registerReceiver(pauseReceiver, filter3);
        registerReceiver(pauseReceiver, filter2);
        registerReceiver(pauseReceiver, filter);



        songView = findViewById(R.id.song_list);
        songList = new ArrayList<Song>();


        ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.drawable.ic_electricity);    //Icon muốn hiện thị
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setTitle("  Thunderstrike");


        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_MEDIA);
        } else
            getSongInfo();

        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });

        MusicListAdapter adapter = new MusicListAdapter(songList);
        songView.setLayoutManager(new GridLayoutManager(this, 2));
        songView.setAdapter(adapter);

        setController();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setController();
            paused = false;
        }
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setList(songList);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

    @Override
    protected void onStop() {
        controller.hide();
        unregisterReceiver(pauseReceiver);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            musicBound = true;
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_MEDIA:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getSongInfo();
                }
                break;

            default:
                break;
        }
    }


    public void getSongInfo() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicExUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri musicInUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        Cursor musicExCursor = musicResolver.query(musicExUri, null, selection, null, null);
        Cursor musicInCursor = musicResolver.query(musicInUri, null, selection, null, null);

        Cursor[] cursors = {musicExCursor, musicInCursor};

        MergeCursor mergeCursor = new MergeCursor(cursors);

        if (mergeCursor.moveToFirst()) {
            int titleColumn = mergeCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = mergeCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = mergeCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            do {
                long id = mergeCursor.getLong(idColumn);
                String title = mergeCursor.getString(titleColumn);
                String artist = mergeCursor.getString(artistColumn);
                songList.add(new Song(id, title, artist));
            } while (mergeCursor.moveToNext());

            mergeCursor.close();
        }

    }

    public void songPicked(View view) {
        if (musicBound) {
            musicService.setSong(Integer.parseInt(view.getTag().toString()));
            musicService.playSong();
            if(playbackPaused){
                setController();
                playbackPaused=false;
            }
            controller.show(0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected

        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicService.setShuffle(); //shuffle songs

                break;
            case R.id.action_end:
                Intent intent = new Intent(MainActivity.this, MusicService.class);


                stopService(playIntent);

//                if (musicBound) {
//                    unbindService(musicConnection);
//                    musicBound = false;
//                }

                musicService = null;
                Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show();
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    void stop(){
//        unbindService(musicConnection);
        stopService(playIntent);
        musicService = null;
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicService = null;
        super.onDestroy();
    }

    private void setController() {
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    private void playNext() {
        musicService.playNext();
        if (playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    private void playPrev() {
        musicService.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    @Override
    public void start() {
        musicService.go();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (musicService != null && musicBound && musicService.isPlaying())
            return musicService.getDur();
        else
            return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicService != null && musicBound && musicService.isPlaying())
            return musicService.getPos();
        else
            return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if (musicService != null && musicBound)
            return musicService.isPlaying();
        else
            return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
