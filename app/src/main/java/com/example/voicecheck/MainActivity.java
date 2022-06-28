package com.example.voicecheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.example.voicecheck.databinding.ActivityMainBinding;
import com.example.voicecheck.ui.main.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;




public class MainActivity extends FragmentActivity {


    public final static String BROADCAST_ACTION = "ru.android.p0961servicebackbroadcast";
    public final static String RECORD_START = "ru.android.Broadcast.Start";
    public final static String RECORD_STOP = "ru.android.Broadcast.Stop";
    public static Byte CheckState = 1;
    LinkedHashSet<String> Numbers = new LinkedHashSet<String>();//входящие номера
    LinkedHashSet<String> scamList = new LinkedHashSet<String>();//список мошенников
    LinkedHashSet<String> spamList = new LinkedHashSet<String>();//список спамеров
    //LinkedHashSet<Date>recordsHistory = new LinkedHashSet<Date>();//1 поле имя файла, 2-дата
    Map<String, Date>recordsHistory = new HashMap<>();
    private ActivityMainBinding binding;



    private FragmentManager manager = getSupportFragmentManager();
    private FragmentTransaction transaction;
    private OneCallFragment frag;
    SharedPreferences prefs, blackListFile, numbersKeeper, RecordDateKeeper;
    File audiofile;
    MediaRecorder recorder;
    boolean isRecording=false;
    File sampleDir;

    private void startRecording(String file_name)
    {
        sampleDir = new File(this.getCacheDir(), "/Records");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }
            audiofile = new File(sampleDir.getAbsolutePath()+file_name + ".3gpp");



        recorder = new MediaRecorder();
                       //  recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);

        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(audiofile.getAbsolutePath());
        try {
            recorder.prepare();
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Date CallDate = new Date();
        recordsHistory.put(file_name,CallDate);//добавление звонка в базу

        try {
        this.recorder.start();//вылазит эксепшн, запись пустая, но файл есть...
        isRecording = true;
    }
        catch (RuntimeException e) {
        e.printStackTrace();
    }
    }

    private void stopRecording() {
        if(isRecording) {
            isRecording = false;
            recorder.stop();
        }
    }

    @Override
    protected void onResume() {
    super.onResume();
    getNumbers();
    //setOrUpdateCalls();
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
        PhoneStateChangedReceiver phoneStateChangedReceiver = new PhoneStateChangedReceiver();
        TelephonyManager tm = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        transaction = manager.beginTransaction();
        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean check1=false, check2=false;
                try {
                    {
                        if(scamList.contains(intent.getStringExtra("incomingNumber"))) check1 = true;
                        if(spamList.contains(intent.getStringExtra("incomingNumber"))) check2 = true;
                        if(!check1&&!check2) Numbers.add(intent.getStringExtra("incomingNumber"));
                    }
                }
                catch (NullPointerException e)
                {
                    Numbers.add(intent.getStringExtra("incomingNumber"));
                }

            }
        };
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, intFilt);


        BroadcastReceiver recordStartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction()==MainActivity.RECORD_START && !isRecording)
                    startRecording(intent.getStringExtra("RECORD_INCOMING_NUMBER"));
            }
        };
        IntentFilter intentFilterStart = new IntentFilter(RECORD_START);
        registerReceiver(recordStartReceiver, intentFilterStart);

        BroadcastReceiver recordStopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction()==MainActivity.RECORD_STOP && isRecording)
                    stopRecording();
            }
        };
        IntentFilter intentFilterStop = new IntentFilter(RECORD_STOP);
        registerReceiver(recordStopReceiver, intentFilterStop);



        if(prefs.contains("CHECK_STATE"))
        {
            CheckState = Byte.parseByte(prefs.getString("CHECK_STATE", ""));
        }
        else CheckState = 0;
        getBlackList();
        getNumbers();
        GetRecordsDB();




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
                                            default: CheckState = 0; break;
                                        }
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


    @Override
    protected void onStop() {
        super.onStop();
        prefs.edit().putString("CHECK_STATE", Byte.toString(CheckState)).apply();//сохранение настройки проверки
         StoreBlackList();//сохранение блеклиста
        StoreRecordsDB();
        saveNumbers();
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
        for(Map.Entry<String,Date> entry : recordsHistory.entrySet()){
            RecordDateKeeper.edit().putString(entry.getKey(), entry.getValue().toString()).apply();
        }
    }

    void GetRecordsDB()
    {
        recordsHistory = (Map<String, Date>) RecordDateKeeper.getAll();
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
        String audioPath = sampleDir.getAbsolutePath()+addingNumber + ".3gpp";//путь до аудиофайла...
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
        String audioPath = sampleDir.getAbsolutePath()+addingNumber + ".3gpp";//путь до аудиофайла...

        //deleteFile(audioPath);
        /*...отправка аудио с номером на сервер*/
    }
}