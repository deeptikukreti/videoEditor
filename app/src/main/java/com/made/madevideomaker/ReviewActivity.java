package com.made.madevideomaker;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;



public class ReviewActivity extends AppCompatActivity{


    private static final String TAG = "ReviewActivity";
    boolean pressed = false;
    String filepath;
    String filename;
    VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        /** Lock display orientation */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        final Button playButton = (Button) findViewById(getResources().getIdentifier("playbut", "id", getPackageName()));
        final Button keepButton = (Button) findViewById(getResources().getIdentifier("keepbut", "id", getPackageName()));
        final Button redoButton = (Button) findViewById(getResources().getIdentifier("redobut", "id", getPackageName()));
        mVideoView = (VideoView) findViewById(R.id.videoView);

        /** Receive data from MainActivity intent and declare video dependent defaults */
        Intent vIntent = getIntent();
        Bundle extras = vIntent.getExtras();
        filepath = getApplicationContext().getFilesDir().getPath();
        filename = extras.getString("filename");
        Bitmap thumb = ThumbnailUtils.createVideoThumbnail(filepath + "/" + filename, MediaStore.Images.Thumbnails.MINI_KIND);
        final BitmapDrawable bitmapDrawable = new BitmapDrawable(thumb);
        mVideoView.setBackgroundDrawable(bitmapDrawable);

        /** Timer thread */
        final Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(500);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update TextView here!
                                if (!mVideoView.isPlaying() && pressed && mVideoView.getDuration()>500) {
                                    playButton.setBackground(getResources().getDrawable(R.drawable.btn_play));
                                    mVideoView.setBackgroundDrawable(bitmapDrawable);
                                    pressed = false;
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, "Thread interrupt exception.");
                }
            }
        };
        try{
            t.start();
        }catch(Exception e){
            Log.d(TAG, "Start thread exception.");
        }


        /** Video play/stop button */
        playButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (pressed) {
                    //On click stop playing
                    stopRecordedVideo();
                    playButton.setBackground(getResources().getDrawable(R.drawable.btn_play));
                    mVideoView.setBackgroundDrawable(bitmapDrawable);
                    pressed = false;
                }else {
                    //On click play
                    playButton.setBackground(getResources().getDrawable(R.drawable.btn_stop));
                    pressed = true;
                    mVideoView.setBackgroundResource(0);
                    playbackRecordedVideo();
                }
            }
        });

        /** Keep button */
        keepButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent endIntent = new Intent(ReviewActivity.this, EndActivity.class);
                Bundle extras = new Bundle();
                extras.putString("filename","madevideo1.mpeg4");
                endIntent.putExtras(extras);
                startActivity(endIntent);
                finish();
            }
        });

        /** Redo button */
        redoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent videoIntent = new Intent(ReviewActivity.this, VideoActivity.class);
                Bundle extras = new Bundle();
                extras.putInt("duration",5);
                extras.putString("filename","madevideo1.mpeg4");
                videoIntent.putExtras(extras);
                startActivity(videoIntent);
                finish();
            }
        });
    }

    public void playbackRecordedVideo(){
        mVideoView.setVideoPath(filepath + "/" + filename);
        mVideoView.requestFocus();
        mVideoView.start();
    }

    public void stopRecordedVideo(){
        mVideoView.pause();
    }
}
