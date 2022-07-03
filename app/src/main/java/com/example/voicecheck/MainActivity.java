package com.example.voicecheck;


import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.voicecheck.databinding.ActivityMainBinding;
import com.example.voicecheck.ui.main.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;




public class MainActivity extends FragmentActivity {

    public static final String DATE_FORMAT_NOW = "dd-MM-yyyy";
    public static Byte CheckState = 1;
    public static final String BROADCAST_ACTION = "com.example.voicecheck.send_phone_num";
    LinkedHashSet<String> Numbers = new LinkedHashSet<>();//входящие номера
    LinkedHashSet<String> scamList = new LinkedHashSet<>();//список мошенников
    LinkedHashSet<String> spamList = new LinkedHashSet<>();//список спамеров
    //LinkedHashSet<Date>recordsHistory = new LinkedHashSet<Date>();//1 поле имя файла, 2-дата
    Map<String, String>recordsHistory = new HashMap<>();
    private ActivityMainBinding binding;
    Intent RecordingService;


    private OneCallFragment frag;
    SharedPreferences prefs;
    SharedPreferences blackListFile;
    SharedPreferences numbersKeeper;
    SharedPreferences RecordDateKeeper;
    File sampleDir;
    public static Integer Record_keep_hysteresis;

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }




    @Override
    protected void onResume() {
    super.onResume();
    getNumbers();
    getBlackList();
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefs = this.getSharedPreferences(
                "com.voicerecognize.app", Context.MODE_PRIVATE);
        blackListFile = this.getSharedPreferences(
                "com.blackList.app", Context.MODE_PRIVATE);
        numbersKeeper = this.getSharedPreferences(
                "com.NumbersKeeper.app", Context.MODE_PRIVATE);
        RecordDateKeeper = this.getSharedPreferences(
                "com.RecordDateKeeper.app", Context.MODE_PRIVATE);
        //DataStore == this.getSharedPreferences("com.DataStore.app", Context.MODE_PRIVATE);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);
        CheckState = Byte.parseByte(prefs.getString("CHECK_STATE", "0"));
        Record_keep_hysteresis = prefs.getInt("RECORD_KEEP_OFFSET", 2);




        getBlackList();
        getNumbers();
        GetRecordsDB();
        removeOldRecords();
        RecordingService = new Intent(this, Record_service.class);
       if(CheckState!=0) {
           startForegroundService(RecordingService);
       }

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean check1=false, check2=false;
                try {
                    {
                        if(scamList.contains(intent.getStringExtra("INCOMING_NUBMER"))) check1 = true;
                        if(spamList.contains(intent.getStringExtra("INCOMING_NUBMER"))) check2 = true;
                        if(!check1&&!check2) Numbers.add(intent.getStringExtra("INCOMING_NUBMER"));
                    }
                }
                catch (NullPointerException e)
                {
                    Numbers.add(intent.getStringExtra("INCOMING_NUBMER"));
                }

            }
        };
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, intFilt);





        viewPager.addOnPageChangeListener(
                new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(
                            int position, float positionOffset, int positionOffsetPixels) {}

                    @Override
                    public void onPageSelected(int position) {
                        switch (position)
                        {
                            case 0:
                                EditText record_hyst_edit = (EditText) findViewById(R.id.saving_interval);
                                record_hyst_edit.setOnKeyListener(new View.OnKeyListener() {
                                    @Override
                                    public boolean onKey(View view, int i, KeyEvent keyEvent) {
                                        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                                            try {
                                                Record_keep_hysteresis = Integer.parseInt(String.valueOf(record_hyst_edit.getText()));
                                            }
                                            catch (NumberFormatException e)
                                            {
                                                Record_keep_hysteresis = 2;
                                            }
                                            record_hyst_edit.clearFocus();
                                            record_hyst_edit.setCursorVisible(false);
                                        }
                                        return false;
                                    }

                                });

                                RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
                                radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
                                {
                                    @Override
                                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                                        switch (checkedId)
                                        {
                                            case R.id.no_check: CheckState = 0;break;//проверка отключена
                                            case R.id.check_some_calls: CheckState = 1; break;//только незнакомые номера
                                            case R.id.check_all_calls: CheckState = 2; break;//все номера
                                        }
                                        updateCheckFlag(CheckState);
                                    }
                                });
                            break;
                            case 1:
                                setOrUpdateCalls();
                                break;
                            default:break;
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {}
                });
    }


    void updateCheckFlag(int flag)
    {
        Intent send;
        switch (flag)
        {
            case 0:
                NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(504312);
                stopService(RecordingService); break;
            case 1:
                 RecordingService = new Intent(this, Record_service.class);
                RecordingService.putExtra("ONLY_CONTACTS", true);
                startForegroundService(RecordingService); break;
            case 2:
                RecordingService = new Intent(this, Record_service.class);
                RecordingService.putExtra("ONLY_CONTACTS", false);
                startForegroundService(RecordingService); break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        prefs.edit().clear().apply();
        prefs.edit().putString("CHECK_STATE", Byte.toString(CheckState)).apply();//сохранение настройки проверки
        prefs.edit().putInt("RECORD_KEEP_OFFSET", Record_keep_hysteresis).apply();
         StoreBlackList();//сохранение блеклиста
        StoreRecordsDB();
        saveNumbers();
    }





    void removeOldRecords()
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate date, dateNow = LocalDate.parse(now(), formatter);
        for(Map.Entry<String, String> entry : recordsHistory.entrySet())
        {
             date = LocalDate.parse(entry.getValue(), formatter);
             if(dateNow.minusDays(Record_keep_hysteresis).isAfter(date))
             {
                 new File(sampleDir.getAbsolutePath()+entry.getKey()).delete();
             }

        }
    }



    void setOrUpdateCalls()
    {
        LinearLayout layout = (LinearLayout) findViewById(R.id.incoming_calls_container);
        layout.removeAllViews();
        for (String s : Numbers)
        {
            frag = new OneCallFragment();
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.incoming_calls_container, frag)
                    .commit();
            frag.SetNumber(s);
        }
    }

    void getBlackList()
    {
        Map<String,?> keys = numbersKeeper.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(entry.getKey().toString().equals("SCAMER")) scamList.add(entry.getValue().toString());
            if(entry.getKey().toString().equals("SPAMER")) spamList.add(entry.getValue().toString());
        }
    }



    void saveNumbers()
    {
        for (String s : Numbers)
        {
            numbersKeeper.edit().putString("NUMBER", s).apply();
        }
    }

    void getNumbers()
    {
        Map<String,?> keys = numbersKeeper.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(entry.getKey().toString().equals("NUMBER"))
                Numbers.add(entry.getValue().toString());
        }



        //Numbers = (LinkedHashSet<String>)numbersKeeper.getStringSet("NUMBER", null);
    }

    void StoreRecordsDB()
    {
        //numbersKeeper.edit().clear().apply();
        RecordDateKeeper.edit().clear().apply();
        String date;
        for(Map.Entry<String,String> entry : recordsHistory.entrySet()){
            RecordDateKeeper.edit().putString(entry.getKey(), entry.getValue()).apply();
        }
    }

    void GetRecordsDB()
    {
        recordsHistory = (Map<String, String>) RecordDateKeeper.getAll();
    }

    void StoreBlackList()
    {
        for (String s : scamList)
        {
            blackListFile.edit().putString("SCAMER", s).apply();
        }
        for (String s : spamList)
        {
            blackListFile.edit().putString("SPAMER", s).apply();
        }
    }



    public void scamBtnClick(View view) {
        View parr = (View) view.getParent();
        TextView Num = parr.findViewById(R.id.incoming_call_number);
        String addingNumber = (String) Num.getText();
        scamList.add(addingNumber);
        Numbers.remove(addingNumber);
        setOrUpdateCalls();
        String audioPath = sampleDir.getAbsolutePath()+addingNumber;//путь до аудиофайла...
        recordsHistory.remove(addingNumber);
        //deleteFile(audioPath);
        /*...отправка аудио с номером на сервер*/
    }

    public void spamBtnClick(View view) {
        View parr = (View) view.getParent();
        TextView Num = parr.findViewById(R.id.incoming_call_number);
        String addingNumber = (String) Num.getText();
        spamList.add(addingNumber);
        Numbers.remove(addingNumber);
        setOrUpdateCalls();
        String audioPath = sampleDir.getAbsolutePath()+addingNumber;//путь до аудиофайла...
        recordsHistory.remove(addingNumber);
        /*String putbase=this.getCacheDir()+"/moshpit.py";
        ProcessBuilder pb=new ProcessBuilder("python",putbase,audioPath);
        Process process=null;
        try
        {
            process=pb.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }*/
    }
}