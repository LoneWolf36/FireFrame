package com.fireframe.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

// Fire TV won't let another app be the home screen, and Amazon's launcher can't be turned off.
// So this watches for Amazon's launcher coming to the front (Home button, boot, leaving an app) and:
//  1. at once, covers it with FireFrame drawn as an overlay, which works right away, and
//  2. opens the FireFrame app, which Fire OS holds back for up to 5 seconds after Home.
// When the app arrives it takes the overlay away. Same screen, so the swap can't be seen.
public class HomeService extends AccessibilityService {
    static final String AMAZON_HOME = "com.amazon.tv.launcher";

    // "Amazon home screen" in FireFrame's settings menu pauses all this until another app opens.
    static long pausedUntil;
    static HomeService running;

    HomeScreen overlay;
    boolean showing;

    static boolean paused() {
        return SystemClock.elapsedRealtime() < pausedUntil;
    }

    static void hideOverlay() {
        if (running != null) running.hide();
    }

    WindowManager wm;
    WindowManager.LayoutParams lp;

    @Override
    protected void onServiceConnected() {
        running = this;
        // The overlay window stays attached and hidden, so showing it is instant, even the first time.
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = new HomeScreen(this);
        overlay.setVisibility(View.GONE);
        lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, HIDDEN, PixelFormat.OPAQUE);
        attach();
    }

    // Can fail if the service is being turned off and on at that moment; show() tries again.
    boolean attach() {
        if (added) return true;
        try {
            wm.addView(overlay, lp);
            added = true;
        } catch (RuntimeException e) {
            added = false;
        }
        return added;
    }

    boolean added;

    static final int SHOWN = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;
    static final int HIDDEN = SHOWN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

    @Override
    public boolean onUnbind(Intent intent) {
        if (added) wm.removeViewImmediate(overlay);
        added = false;
        overlay = null;
        running = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        CharSequence pkg = e.getPackageName();
        if (pkg == null) return;
        String p = pkg.toString();
        if (p.equals(AMAZON_HOME)) {
            amazonHomeOpened();
        } else if (!p.equals(getPackageName()) && !p.equals("com.android.systemui")) {
            pausedUntil = 0; // another app opened
            hide();
        }
    }

    void amazonHomeOpened() {
        if (paused()) return;
        show();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    void show() {
        if (overlay == null || showing || !attach()) return;
        showing = true;
        overlay.setVisibility(View.VISIBLE);
        lp.flags = SHOWN; // takes the remote's keys while it's up
        wm.updateViewLayout(overlay, lp);
        overlay.onShown();
    }

    void hide() {
        if (overlay == null || !showing || !added) return;
        showing = false;
        overlay.setVisibility(View.GONE);
        lp.flags = HIDDEN;
        wm.updateViewLayout(overlay, lp);
    }

    @Override
    public void onInterrupt() {}
}
