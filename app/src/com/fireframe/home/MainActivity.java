package com.fireframe.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

// FireFrame as an app: the Home screen in a window of its own. See HomeScreen for everything else.
public class MainActivity extends Activity {
    HomeScreen screen;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        screen = new HomeScreen(this);
        screen.applyExtras(getIntent());
        setContentView(screen);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        screen.applyExtras(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HomeService.pausedUntil = 0;
        HomeService.hideOverlay(); // the real app is in front now
        screen.onShown();
    }

    // Fire TV starts on Amazon's home screen; this opens FireFrame as soon as the TV has booted.
    public static class Boot extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent i) {
            c.startActivity(new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
