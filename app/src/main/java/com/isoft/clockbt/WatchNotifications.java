package com.isoft.clockbt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Timer;
import java.util.TimerTask;

public class WatchNotifications extends Service {
    NotificationManagerCompat nmCompat;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;


    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
        startForeground();

        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service destroyed by user.", Toast.LENGTH_LONG).show();
        nmCompat.cancel(101);
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("Background Service", "Background Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "Background Service")
                .setSmallIcon(R.drawable.bt_not_con)
                .setContentTitle("Background service")
                .setOngoing(true)
                .setContentText("This notification is used to run the clock app on the background")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        Notification n = builder.build();
        nmCompat = NotificationManagerCompat.from(this);
        nmCompat.notify(101, n);

    }

}
