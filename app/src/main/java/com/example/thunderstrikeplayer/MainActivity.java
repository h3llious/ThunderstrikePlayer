package com.example.thunderstrikeplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    final static int MY_PERMISSIONS_REQUEST_READ_MEDIA = 1;

    ArrayList<Song> songList;
    RecyclerView songView;

    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songView = findViewById(R.id.song_list);
        songList = new ArrayList<Song>();


        ActionBar actionBar = getSupportActionBar();
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        actionBar.setLogo(R.drawable.thunder);    //Icon muốn hiện thị
        actionBar.setDisplayUseLogoEnabled(true);



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
    }


    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null){
            playIntent = new Intent(this, MusicService.class);
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

        Cursor musicExCursor = musicResolver.query(musicExUri, null, null, null, null);
        Cursor musicInCursor = musicResolver.query(musicInUri, null, null, null, null);

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

    public void songPicked(View view ){
        musicService.setSong(Integer.parseInt(view.getTag().toString()));
        musicService.playSong();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected

        switch (item.getItemId()){
            case R.id.action_shuffle:
                break;
            case R.id.action_end:
                Intent  intent = new Intent(MainActivity.this, MusicService.class);


                stopService(intent);
                unbindService(musicConnection);
                musicService = null;
                Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show();
//                System.exit(0);
                break;
        }
        return onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Intent  intent = new Intent(MainActivity.this, MusicService.class);
        stopService(intent);
        musicService=null;
        super.onDestroy();
    }
}
