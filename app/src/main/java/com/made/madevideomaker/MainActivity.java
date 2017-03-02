package com.made.madevideomaker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void videoActivity(View view){
        Intent videoIntent = new Intent(MainActivity.this, VideoActivity.class);
        Bundle extras = new Bundle();
        extras.putInt("duration",5);
        extras.putString("filename","madevideo1.mpeg4");
        videoIntent.putExtras(extras);
        startActivity(videoIntent);
    }

    public void reviewActivity(View view){
        Intent reviewIntent = new Intent(MainActivity.this, ReviewActivity.class);
        Bundle extras = new Bundle();
        extras.putString("filename","madevideo1.mpeg4");
        reviewIntent.putExtras(extras);
        startActivity(reviewIntent);
    }
}
