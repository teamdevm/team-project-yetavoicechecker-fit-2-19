package com.example.voicecheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.telephony.TelephonyManager;

public class PhoneStateChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) != null){
            String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                String name = intent.getStringExtra(TelephonyManager.EXTRA_SPECIFIC_CARRIER_NAME);
                Intent returnToMain = new Intent(MainActivity.BROADCAST_ACTION);
                if(name==null) {
                    returnToMain.putExtra("incomingNumber", incomingNumber);
                }
                else
                {
                    returnToMain.putExtra("incomingNumber", name);
                }
                context.sendBroadcast(returnToMain);//отправляем в MainActivity номер звонящего

            }
        }
        String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) && null != intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)) {
                Intent intent_record = new Intent(MainActivity.RECORD_START);
                intent_record.putExtra("RECORD_INCOMING_NUMBER", intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
                context.sendBroadcast(intent_record);
            }
            if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE))
            {
                Intent intent_record = new Intent(MainActivity.RECORD_STOP);
                context.sendBroadcast(intent_record);
            }

    }


};