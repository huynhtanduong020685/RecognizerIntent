package com.example.recognitionservice;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.ContentResolver;
import android.media.AudioManager;
import android.media.MediaPlayer;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.net.Uri;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.io.OutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.io.IOException;


import android.support.v7.app.AppCompatActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;


public class MainActivity extends AppCompatActivity {
    /**
     * called  when activity the first create
     **/

    private static final int REQUEST_RECORD_AUDIO_PERMISSION_CODE = 1;
    private final static String TAG="SpeechRecognizerManager";

    private static final int VOICE_RECOGNITION_REQUEST = 1;
    //private TextView textSaid;
    private Button btnStart, btnStop, btnPlay,buttonStopPlayingRecording;
    // private String outputFile;
    Intent intent = getIntent();

    protected AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected boolean mIsListening;
    private boolean mIsStreamSolo;
    private onResultsReady mListener;
    private  Context context;
    private  onResultsReady listener;
    private MediaPlayer mMediaPlayer;

    private boolean mIsFinishAfterPlayAudio = false;



    private boolean mMute = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //set up interface from activity main
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //set up property  get from  main -> id
        setContentView(R.layout.activity_main);
//        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";

        btnStart = (Button) findViewById(R.id.btnStart);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnStop = (Button) findViewById(R.id.btnStop);
        buttonStopPlayingRecording=(Button) findViewById(R.id.buttonStopPlayingRecording);
        btnStop.setEnabled(false);
        btnPlay.setEnabled(false);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission()) {
                    SpeechRecognizerManager();
                    btnStart.setEnabled(false);
                    btnStop.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
                }else{
                    requestPermission();
                }

            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
                btnStop.setEnabled(false);
                btnPlay.setEnabled(true);
                btnStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Audio Recorder successfully", Toast.LENGTH_LONG).show();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Uri audioUri = intent.getData();
                if (audioUri != null) {
                    playAudio(audioUri, intent.getType());
                }

                btnStart.setEnabled(false);
                btnStop.setEnabled(false);
                buttonStopPlayingRecording.setEnabled(true);

                Toast.makeText(getApplicationContext(), "Playing Audio", Toast.LENGTH_LONG).show();
            }


        });
        buttonStopPlayingRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btnStop.setEnabled(false);
                btnStart.setEnabled(true);
                buttonStopPlayingRecording.setEnabled(false);
                btnPlay.setEnabled(true);
                if(mMediaPlayer != null){
                    mMediaPlayer.stop();
                    mMediaPlayer.release();
                    SpeechRecognizerManager();
                }

            }
        });

    }
    /* start Recognition*/

    public void SpeechRecognizerManager() {
        try {
            mListener = listener;
        } catch (ClassCastException e) {
            Log.e(TAG, e.toString());
        }
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());

        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
        mSpeechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
        mSpeechRecognizerIntent.putExtra("android.speech.extra.GET_AUDIO", true);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "please record slowly and clearly");

        try {
            startActivityForResult(mSpeechRecognizerIntent, VOICE_RECOGNITION_REQUEST);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }

    }

    /* return result from Activity , function will call to  onActivityResult*/
    @Override

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // the resulting text is in the getExtras:

        ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        String result = matches.get(0);
        Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();

        // Save audio to file

        Uri audioUri = data.getData();
        ContentResolver contentResolver = getContentResolver();
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            inputStream = contentResolver.openInputStream(audioUri);
            outputStream = null;
            final File recordFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test");
            if (!recordFile.exists()) {
                recordFile.mkdir();
            }
            String FileName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".amr";
            outputStream = new FileOutputStream(recordFile + "/" + FileName);
            // Transfer bytes from in to out
            int read = 0;
            byte[] buf = new byte[1024];

            while ((read = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, read);
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {

                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        }
    }

    public boolean checkPermission() {

        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }


    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "Requires RECORD_AUDIO permission", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION_CODE);
        }
    }
    /**
     * @param uri audio URI to be played
     */
    private void playAudio(Uri uri,String type){
        mMediaPlayer = MediaPlayer.create(this, uri);
        if (mMediaPlayer == null) {
            // create can return null, e.g. on Android Wear
            toast(String.format(getString(R.string.errorFailedPlayAudio), uri.toString(), type));
        } else {
            int duration = mMediaPlayer.getDuration();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    releasePlayer();
                    toast(getString(R.string.toastPlayingAudioDone));
                    if (mIsFinishAfterPlayAudio) {
                        finish();
                    }
                }
            });
            mMediaPlayer.start();
            toast(String.format(getString(R.string.toastPlayingAudio), uri.toString(), type, duration));
        }


    }

    private void toast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void releasePlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }





    private void listenAgain()
    {
        if(mIsListening) {
            mIsListening = false;
            mSpeechRecognizer.cancel();
            startListening();
        }
    }

    private void startListening()
    {
        if(!mIsListening)
        {
            mIsListening = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                // turn off beep sound
                if (!mIsStreamSolo && mMute) {
                    mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
                    mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, true);
                    mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
                    mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
                    mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
                    mIsStreamSolo = true;
                }
            }
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }
    }

    public void destroy()
    {
        mIsListening=false;
        if (!mIsStreamSolo) {
            mAudioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_ALARM, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
            mAudioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false);
            mIsStreamSolo = true;
        }
        Log.d(TAG, "onDestroy");
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.stopListening();
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
            mSpeechRecognizer=null;
        }

    }



    public void stop() {
        if (mIsListening && mSpeechRecognizer != null) {
            mSpeechRecognizer.stopListening();
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }

        mIsListening = false;
    }


    protected class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public synchronized void onError(int error) {
            if(error==SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
            {
                if(mListener!=null) {
                    ArrayList<String> errorList=new ArrayList<String>(1);
                    errorList.add("ERROR RECOGNIZER BUSY");
                    if(mListener!=null)
                        mListener.onResults(errorList);
                }
                return;
            }

            if(error==SpeechRecognizer.ERROR_NO_MATCH)
            {
                if(mListener!=null)
                    mListener.onResults(null);
            }

            if(error==SpeechRecognizer.ERROR_NETWORK)
            {
                ArrayList<String> errorList=new ArrayList<String>(1);
                errorList.add("STOPPED LISTENING");
                if(mListener!=null)
                    mListener.onResults(errorList);
            }
            Log.d(TAG, "error = " + error);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    listenAgain();
                }
            },100);



        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            if (partialResults != null && mListener != null) {
                ArrayList<String> texts = partialResults.getStringArrayList("android.speech.extra.UNSTABLE_TEXT");
                mListener.onStreamingResult(texts);
            }

        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onResults(Bundle results) {
            if (results != null && mListener != null) {
                ArrayList<String> ahihi = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                mListener.onResults(ahihi);
            }

        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }
    }
    public boolean ismIsListening() {
        return mIsListening;
    }


    public interface onResultsReady
    {
        public void onResults(ArrayList<String> results);

        public void onStreamingResult(ArrayList<String> partialResults);

    }

    public void mute(boolean mute)
    {
        mMute=mute;
    }

    public boolean isInMuteMode()
    {
        return mMute;
    }

}
