package com.example.voicecheck;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class Record_service extends Service {
    File audiofile;
    public static final String DATE_FORMAT_NOW = "dd-MM-yyyy";
    boolean isRecording=false;
    File sampleDir;
    BroadcastReceiver Phone_call_reciever;
    private int startId;
    Recorder recorder = null;
    boolean contactsOnly;
    String lastCallNumber;
    String lastRecordPath;

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        sampleDir = new File(this.getCacheDir(), "/Records");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
        if (recorder==null)
            recorder = Recorder.getInstance(sampleDir.getAbsolutePath(), this);
        Phone_call_reciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) != null) {
                    String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    }

                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) && null != intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                        Intent returnToMain = new Intent(MainActivity.BROADCAST_ACTION);
                        returnToMain.putExtra("INCOMING_NUBMER", intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
                        context.sendBroadcast(returnToMain);
                        lastCallNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        recorder.startRecording(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
                    }
                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        recorder.stopRecording();
                        lastRecordPath = sampleDir.getAbsolutePath() + "/" + lastCallNumber + ".wav";
                    }
                }
            }
        };
        IntentFilter intFilt = new IntentFilter("android.intent.action.PHONE_STATE" );
        this.registerReceiver(Phone_call_reciever, intFilt);
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if(intent.hasExtra("ONLY_CONTACTS"))
            contactsOnly = Boolean.parseBoolean(intent.getStringExtra("ONLY_CONTACTS"));
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Служба записи звонков активна")
                    .setContentText("").build();
            startForeground(504312, notification);
        }
        return Service.START_REDELIVER_INTENT;
    }


}
