package com.example.voicecheck;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;

import androidx.core.app.ActivityCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class Recorder {
    private static Recorder mInstance;
    private AudioRecord mRecorder = null;
    static String AudiofilePath;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private Context context;
    private static boolean flag = true;



    private Recorder(Context context) {
        this.context = context;
    }

    public static Recorder getInstance(String path, Context context) {
        if (mInstance == null) {
            mInstance = new Recorder(context);
            AudiofilePath = path;
        }
        return mInstance;
    }

    private AudioRecord createAudioRecorder() {
        int minBuferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANEL_CONFIG, AUDIO_FORMAT);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            mRecorder = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANEL_CONFIG, AUDIO_FORMAT, minBuferSize);
        }
        return mRecorder;
    }

    public void startRecording(String record_file_name)
    {
        mRecorder = createAudioRecorder();
        byte[] buffer = new byte[1024];
        String tempFileName = UUID.randomUUID().toString() + ".raw";
        String wavFileName = record_file_name.toString() + ".wav";
        File path = new File(AudiofilePath);
        File tempFile = new File(path, tempFileName);
        File wavFile = new File(path, wavFileName);
        BufferedOutputStream bos = null;
        mRecorder.startRecording();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedOutputStream bos = null;
                mRecorder.startRecording();
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(tempFile));
                    while(mRecorder.getRecordingState()==AudioRecord.RECORDSTATE_RECORDING )
                    {
                        int num = mRecorder.read(buffer, 0, buffer.length);
                        bos.write(buffer, 0, num);
                    }
                    mRecorder.release();
                    mRecorder = null;
                    flag = true;
                    bos.flush();

                }
                catch (IOException e){}
                finally {
                    try {
                        if(bos!=null) bos.close();
                    }
                    catch (IOException e){}
                }
                properWAV(tempFile, wavFile);
                tempFile.delete();
            }
        }).start();
    }


    public void stopRecording(){
        if(mRecorder==null) return;
        if(mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
            mRecorder.stop();
    }






    private void properWAV(File sourceFile, File destinationFile)
    {
        try {
            long mySubChunk1Size = 16;
            int myBitsPerSample = 16;
            int myFormat = 1;
            long myChannels = 1;
            long mySampleRate = SAMPLE_RATE;
            long myByteRate = mySampleRate*myChannels*myBitsPerSample/8;
            int myBlockAlign = (int) (myChannels*myBitsPerSample/8);

            byte[] clipData = getBytesFromFile(sourceFile);
            long myDataSize = clipData.length;
            long myChunk2Size = myDataSize*myChannels*myBitsPerSample/8;
            long myChunkSize = 36 + myChunk2Size;
            OutputStream os;
            os = new FileOutputStream(destinationFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);
            outFile.writeBytes("RIFF");                                     // 00 - RIFF
            outFile.write(intToByteArray((int)myChunkSize), 0, 4);          // 04 - how big is the rest of this file?
            outFile.writeBytes("WAVE");                                     // 08 - WAVE
            outFile.writeBytes("fmt ");                                     // 12 - fmt
            outFile.write(intToByteArray((int)mySubChunk1Size), 0, 4);      // 16 - size of this chunk
            outFile.write(shortToByteArray((short)myFormat), 0, 2);         // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
            outFile.write(shortToByteArray((short)myChannels), 0, 2);       // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
            outFile.write(intToByteArray((int)mySampleRate), 0, 4);         // 24 - samples per second (numbers per second)
            outFile.write(intToByteArray((int)myByteRate), 0, 4);           // 28 - bytes per second
            outFile.write(shortToByteArray((short)myBlockAlign), 0, 2);     // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray((short)myBitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                                     // 36 - data
            outFile.write(intToByteArray((int)myDataSize), 0, 4);           // 40 - how big is this data chunk
            outFile.write(clipData);                                        // 44 - the actual data itself - just a long string of numbers

            outFile.flush();
            outFile.close();
            bos.close();
            os.close();
        } catch (IOException e) { }

    }

    private byte[] getBytesFromFile(File file) {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {}
        catch (IOException e) {}
        return bytes;
    }


    private static byte[] intToByteArray(int i)
    {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0x00FF);
        b[1] = (byte) ((i >> 8) & 0x000000FF);
        b[2] = (byte) ((i >> 16) & 0x000000FF);
        b[3] = (byte) ((i >> 24) & 0x000000FF);
        return b;
    }

    // convert a short to a byte array
    public static byte[] shortToByteArray(short data)
    {
        return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
    }




}
