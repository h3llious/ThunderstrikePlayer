package com.example.thunderstrikeplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.thunderstrikeplayer.MainActivity;

public class PauseReceiver extends BroadcastReceiver {
    MainActivity main = null;

    void setMainActivityHandler(MainActivity main) {
        this.main = main;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(MusicService.ACTION_PLAY))
            main.start();
        else if (action.equals(MusicService.ACTION_STOP_FOREGROUND_SERVICE))
            main.stop();
        else if (action.equals(MusicService.ACTION_PAUSE))
            main.pause();
    }
}
