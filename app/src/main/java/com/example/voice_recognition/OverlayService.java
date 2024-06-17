package com.example.voice_recognition;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class OverlayService extends Service {

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String outputFile;
    private Button toggleRecordButton;
    private Button closeButton;
    private View overlayView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Inflate the overlay layout
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_layout, null);

        // Find buttons
        toggleRecordButton = overlayView.findViewById(R.id.toggleRecordButton);
        closeButton = overlayView.findViewById(R.id.closeButton);

        // Set button click listeners
        toggleRecordButton.setOnClickListener(v -> toggleRecording());
        closeButton.setOnClickListener(v -> stopSelf());

        // Set layout parameters for the overlay
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
//        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.x = 0;
        params.y = 0;

        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

        // Add the overlay view to the window
        windowManager.addView(overlayView, params);
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private String getInternalStorageFilePath() {
        File internalStorageDir = getFilesDir();
        if (internalStorageDir != null) {
            return internalStorageDir.getAbsolutePath() + File.separator + "recording.aac";
        } else {
            return null;
        }
    }

    private void startRecording() {
        String outputFile = getInternalStorageFilePath();

        if (outputFile != null) {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(outputFile);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                isRecording = true;
                toggleRecordButton.setText("Stop");
                Toast.makeText(this, "Recording started. File saved at: " + outputFile, Toast.LENGTH_SHORT).show();
                Log.d("OverlayService", outputFile);
            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Internal storage directory not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        String outputFile = getInternalStorageFilePath();

        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            toggleRecordButton.setText("Start");
            Toast.makeText(this, "Recording stopped. File saved at: " + outputFile, Toast.LENGTH_SHORT).show();
            startPlayback();
        }
    }

    private void startPlayback() {
        String outputFile = getInternalStorageFilePath();

        if (outputFile != null) {
            MediaPlayer mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(outputFile);
                mediaPlayer.prepare();
                mediaPlayer.start();
                Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error playing audio", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Audio file not found", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRecording) {
            stopRecording();
        }
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }
}
