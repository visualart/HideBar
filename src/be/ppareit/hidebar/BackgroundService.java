/*******************************************************************************
 * Copyright (c) 2011 Pieter Pareit.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Pieter Pareit - initial API and implementation
 ******************************************************************************/
package be.ppareit.hidebar;

import java.util.ArrayList;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import be.ppareit.hidebar.Device.AndroidVersion;

public class BackgroundService extends Service {

    static final String TAG = BackgroundService.class.getSimpleName();

    static private Context sContext = null;
    static private Intent sIntent = null;

    private static boolean sStartupByBoot = false;

    /**
     * Only instantiate class using this method!
     */
    static public void start(Context context, boolean startupbyboot) {
        // only allow this service to run on rooted devices
        if (Device.isRooted() == false)
            return;
        BackgroundService.sStartupByBoot = startupbyboot;
        sIntent = new Intent(context, BackgroundService.class);
        context.startService(sIntent);
        sContext = context;
    }

    static public void stop(Context context) {
        context.stopService(sIntent);
        sIntent = null;
        sContext = null;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");

        // create the intent that can hide the statusbar
        Intent hideSystembarIntent = new Intent(this, HideSystembarReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, hideSystembarIntent, 0);

        // if started up from boot, maybe hide the statusbar
        if (sStartupByBoot && HideBarPreferences.shouldStatusbarHideAtBoot(this)) {
            try {
                pi.send();
            } catch (CanceledException e) {
                e.printStackTrace();
            }
        }
        sStartupByBoot = false;

        // create the notification used to start the intent to hide the
        // statusbar
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(this)
                .setContentTitle(getResources().getText(R.string.hidebar_notification))
                .setSmallIcon(R.drawable.ic_icon_hidebar).setOngoing(true)
                .setContentIntent(pi).getNotification();
        nm.notify(TAG, 0, notification);
        startForeground(0, notification);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        // remove the notification
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
        stopForeground(true);

        // we where asked to stop running, so make sure the user gets back his
        // status bar
        showBar(true);

        stopService(new Intent(this, RestoreSystembarService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");

        // we just have to start service once, not keep restarting
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return null;
    }

    static void showBar(boolean makeVisible) {
        try {
            // get the existing environment
            ArrayList<String> envlist = new ArrayList<String>();
            Map<String, String> env = System.getenv();
            for (String envName : env.keySet()) {
                envlist.add(envName + "=" + env.get(envName));
            }
            String[] envp = (String[]) envlist.toArray(new String[0]);
            // depending on makeVisible, show or hide the bar
            if (makeVisible) {
                Log.v(TAG, "showBar will show systembar");
                // execute in correct environment
                String command;
                if (Device.getAndroidVersion() == AndroidVersion.HC) {
                    command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService";
                } else {
                    command = "rm /sdcard/hidebar-lock\n"
                            + "sleep 5\n"
                            + "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService";
                }
                Runtime.getRuntime().exec(new String[] { "su", "-c", command }, envp);
                // no proc.waitFor();
                sContext.sendBroadcast(new Intent(Constants.ACTION_BARSHOWN));
            } else {
                Log.v(TAG, "showBar will hide the systembar");
                // execute in correct environment
                String command;
                if (Device.getAndroidVersion() == AndroidVersion.HC) {
                    command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib service call activity 79 s16 com.android.systemui";
                } else {
                    command = "touch /sdcard/hidebar-lock\n"
                            + "while [ -f /sdcard/hidebar-lock ]\n"
                            + "do\n"
                            + "killall com.android.systemui\n"
                            + "sleep 1\n"
                            + "done\n"
                            + "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService";
                }
                Runtime.getRuntime().exec(new String[] { "su", "-c", command }, envp);
                // no proc.waitFor();
                sContext.sendBroadcast(new Intent(Constants.ACTION_BARHIDDEN));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
