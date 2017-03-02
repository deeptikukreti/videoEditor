package com.made.madevideomaker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import static android.media.MediaRecorder.OutputFormat.AMR_NB;
import static android.media.MediaRecorder.OutputFormat.MPEG_4;
import static android.media.MediaRecorder.VideoEncoder.MPEG_4_SP;


public class VideoActivity extends AppCompatActivity implements SensorEventListener {

    /**
     * Var initialization
     */
    private static final String TAG = "CameraActivity";

    private SurfaceView preview;
    private SurfaceHolder previewHolder = null;
    private Camera camera = null;
    private boolean inPreview = false;
    private boolean cameraConfigured = false;
    private int cam = 0;
    private boolean pressed = false;
    private int degrees = 0;
    SensorManager sm;
    WindowManager mWindowManager;

    public int mOrientationDeg;
    private static final int _DATA_X = 0;
    private static final int _DATA_Y = 1;
    private static final int _DATA_Z = 2;
    int ORIENTATION_UNKNOWN = -1;

    private int screenWidth;
    private int screenHeight;

    private float viewfinderHalfPx;

    private MediaRecorder mediarecorder;

    private String filename;
    private int durationint;
    private int durationtimer;
    private String prefix = "0:";
    private String timerstring;
    private File mOutputFile;
    private String filepath;



    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        /** Receive data from MainActivity intent */
        Intent vIntent = getIntent();
        Bundle extras = vIntent.getExtras();

        durationint = extras.getInt("duration", 0)*1000;
        durationtimer = durationint/1000;
        filepath = getApplicationContext().getFilesDir().getPath();
        filename = extras.getString("filename");
        mOutputFile = new File(filepath + "/" + filename);

        /** xml resource initialization */
        preview = (SurfaceView) findViewById(getResources().getIdentifier("preview", "id", getPackageName()));
        final Button flipCamera = (Button) findViewById(getResources().getIdentifier("flipCamera", "id", getPackageName()));
        final Button captureButton = (Button) findViewById(getResources().getIdentifier("captureButton", "id", getPackageName()));
        final ImageView viewfinder = (ImageView) findViewById(getResources().getIdentifier("viewfinder", "id", getPackageName()));
        final TextView timerview = (TextView) findViewById(getResources().getIdentifier("timerview", "id", getPackageName()));
        timerstring=prefix+String.format("%02d",durationtimer);
        timerview.setText(timerstring);
        viewfinderHalfPx = pxFromDp(72) / 2;

        /** Timer thread */
        final Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // update TextView here!
                                durationtimer -= 1;
                                timerstring=prefix+String.format("%02d",durationtimer);
                                if(durationtimer>=0){timerview.setText(timerstring);}
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        /** Camera parameter initialization */
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sm.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0) {
            Sensor s = sm.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            sm.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
        }
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        boolean isFrontCamera = false;
        if (Camera.getNumberOfCameras() > 1) {
            flipCamera.setVisibility(View.VISIBLE);
            isFrontCamera = true;
        } else {
            flipCamera.setVisibility(View.INVISIBLE);
            isFrontCamera = false;
        }
        Display display = getWindowManager().getDefaultDisplay();
        // Necessary to use deprecated methods for Android 2.x support
        screenWidth = display.getWidth();
        screenHeight = display.getHeight();

        /** Front Camera initialization */
        if(isFrontCamera) {
            flipCamera.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (cam == 0) {
                        cam = 1;
                        viewfinder.setVisibility(View.INVISIBLE);
                    } else {
                        cam = 0;
                        viewfinder.setVisibility(View.VISIBLE);
                        viewfinder.setX(screenWidth / 2 - viewfinderHalfPx);
                        viewfinder.setY(screenHeight / 2 - viewfinderHalfPx*3);
                    }
                    cameraConfigured = false;
                    restartPreview(cam);
                }
            });
        }

        /** Video capture button */
        captureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (pressed) {
                    try {
                        mediarecorder.stop();
                    } catch (RuntimeException e) {
                        Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                        mOutputFile.delete();
                        pressed = false;
                    }
                    t.interrupt();
                    releaseMediaRecorder();
                    releaseCamera();
                    Intent reviewIntent = new Intent(VideoActivity.this, ReviewActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString("filename","madevideo1.mpeg4");
                    reviewIntent.putExtras(extras);
                    startActivity(reviewIntent);
                    finish();
                }else {
                    int currentOrientation = getResources().getConfiguration().orientation;
                    if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    }
                    else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    }
                    t.start();
                    captureButton.setBackground(getResources().getDrawable(R.drawable.btn_stop));
                    flipCamera.setVisibility(View.INVISIBLE);
                    new MediaPrepareTask().execute(null,null,null);
                }
            }
        });
    }


    /**
     * Camera orientation fix
     */
    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * Set Camera Surface View controls (initalize,pause,update,async)/options
     */
    @Override
    public void onResume() {
        super.onResume();
        previewHolder = preview.getHolder();
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        previewHolder.addCallback(surfaceCallback);
        if (Camera.getNumberOfCameras() >= 1) {
            camera = Camera.open(cam);
        }

        // Initialize preview if surface still exists
        if (preview.getHeight() > 0) {
            initPreview(preview.getHeight());
            startPreview();
        }
    }
    private float pxFromDp(float dp) {
        return dp * VideoActivity.this.getResources().getDisplayMetrics().density;
    }
    void restartPreview(int isFront) {
        if (inPreview && camera!=null) {
            camera.stopPreview();
        }
        inPreview = false;
        camera.release();
        camera = Camera.open(isFront);
        initPreview(preview.getHeight());
        startPreview();
    }
    @Override
    public void onPause() {
        if (inPreview && camera!=null) {
            camera.stopPreview();
            camera.release();
        }
        camera = null;
        inPreview = false;
        super.onPause();
    }
    /** Pull camera parameters */
    private Camera.Size getBestPreviewSize(int height,
                                           Camera.Parameters parameters) {

        final double ASPECT_TOLERANCE = 0.1;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - height) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - height) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - height);
            }
        }
        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                if (Math.abs(size.height - height) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - height);
                }
            }
        }
        return optimalSize;
    }
    /** Initialize preview parameters */
    private void initPreview(int height) {
        if (camera != null && previewHolder.getSurface() != null) {
            try {
                camera.setPreviewDisplay(previewHolder);
            } catch (Throwable t) {
                Log.e("Preview-Callback",
                        "Excp in setPreviewDisplay()", t);
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(height, parameters);
                Camera.Size pictureSize = getSmallestPictureSize(parameters);
                if (size != null && pictureSize != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    parameters.setPictureSize(pictureSize.width, pictureSize.height);

                    parameters.setPictureFormat(ImageFormat.JPEG);
                    // For Android 2.3.4 quirk
                    if (parameters.getSupportedFocusModes() != null) {
                        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        } else if (parameters.getSupportedFocusModes().contains(android.hardware.Camera.Parameters.FOCUS_MODE_AUTO)) {
                            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        }
                    }
                    if (parameters.getSupportedSceneModes() != null) {
                        if (parameters.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO)) {
                            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                        }
                    }
                    if (parameters.getSupportedWhiteBalance() != null) {
                        if (parameters.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
                            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                        }
                    }
                    cameraConfigured = true;
                    camera.setParameters(parameters);
                }
            }
        }
    }
    private Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            } else {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea > resultArea) {
                    result = size;
                }
            }
        }
        return (result);
    }
    private void startPreview() {
        if (cameraConfigured && camera != null) {
            setCameraDisplayOrientation(this,cam,camera);
            camera.startPreview();
            inPreview = true;
        }
    }
    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            if (camera != null) {
                camera.setDisplayOrientation(90);
            }
            initPreview(preview.getHeight());
            startPreview();
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (camera != null) {
                if(inPreview && camera!=null) {
                    camera.stopPreview();
                }
                inPreview = false;
                camera.release();
                camera = null;
            }
        }
    };
    @Override
    protected void onDestroy() {
        // Stop listening to sensor
        sm.unregisterListener(this);
        super.onDestroy();
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = event.values;
            int orientation = ORIENTATION_UNKNOWN;
            float X = -values[_DATA_X];
            float Y = -values[_DATA_Y];
            float Z = -values[_DATA_Z];
            float magnitude = X * X + Y * Y;
            // Don't trust the angle if the magnitude is small compared to the y value
            if (magnitude * 4 >= Z * Z) {
                float OneEightyOverPi = 57.29577957855f;
                float angle = (float) Math.atan2(-Y, X) * OneEightyOverPi;
                orientation = 90 - Math.round(angle);
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360;
                }
                while (orientation < 0) {
                    orientation += 360;
                }
            }
            //^^ thanks to google for that code
            //now we must figure out which orientation based on the degrees
            if (orientation != mOrientationDeg) {
                mOrientationDeg = orientation;
                //figure out actual orientation
                if (orientation == -1) {//basically flat
                    if (cam == 1) {
                        degrees = 270;
                    } else {
                        degrees = 90;
                    }
                } else if (orientation <= 45 || orientation > 315) {//round to 0
                    if (cam == 1) {
                        degrees = 270;
                    } else {
                        degrees = 90;
                    }
                } else if (orientation > 45 && orientation <= 135) {//round to 90
                    degrees = 180;
//                    RotateAnimation a = new RotateAnimation(0, 90, 34, 34);
                } else if (orientation > 135 && orientation <= 225) {//round to 180
                    if (cam == 1) {
                        degrees = 90;
                    } else {
                        degrees = 270;
                    }
                } else if (orientation > 225 && orientation <= 315) {//round to 270
                    degrees = 0;
                }
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    /** Media recorder initialization/options */
    private void releaseMediaRecorder(){
        if (mediarecorder != null) {
            // clear recorder configuration
            mediarecorder.reset();
            // release the recorder object
            mediarecorder.release();
            mediarecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            camera.lock();
        }
    }

    private void releaseCamera(){
        if (camera != null){
            // release the camera for other applications
            camera.release();
            camera = null;
        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private boolean prepareVideoRecorder(){
        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters p = camera.getParameters();
        p.setRotation(degrees);
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        p.set("cam_mode",1);
        camera.setParameters(p);
        // END_INCLUDE (configure_preview)


        // BEGIN_INCLUDE (configure_media_recorder)
        mediarecorder = new MediaRecorder();


        // Step 1: Unlock and set camera to MediaRecorder
        try {
            camera.unlock();
        }catch(RuntimeException e){
            Log.d(TAG,"Runtime Exception: ",e);
        }
        mediarecorder.setCamera(camera);

        mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediarecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediarecorder.setOrientationHint(degrees);
        mediarecorder.setMaxDuration(durationint);
        mediarecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediarecorder, int i, int i1) {
                if(i==MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
                    try {
                        mediarecorder.stop();
                    } catch (RuntimeException e) {
                        Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                        mOutputFile.delete();
                        pressed = false;
                    }
                    releaseMediaRecorder();
                    releaseCamera();
                    Intent reviewIntent = new Intent(VideoActivity.this, ReviewActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString("filename","madevideo1.mpeg4");
                    reviewIntent.putExtras(extras);
                    startActivity(reviewIntent);
                    finish();
                }
            }
        });


        // Step 2: Set sources
        CamcorderProfile profile = CamcorderProfile.get(cam,CamcorderProfile.QUALITY_HIGH);
        profile.duration = durationint;
        profile.fileFormat = MPEG_4;
        profile.videoCodec = MPEG_4_SP;
        profile.audioCodec = AMR_NB;
        mediarecorder.setProfile(profile);

        // Step 4: Set output file
        mediarecorder.setOutputFile(filepath + "/" + filename);
        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mediarecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

/**
 * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
 * operation.
 */
class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Void... voids) {
        // initialize video camera
        if (prepareVideoRecorder()) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mediarecorder.start();
            pressed = true;
        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (!result) {
            VideoActivity.this.finish();
        }
        // inform the user that recording has started

    }
}

    private void messageBox(String method, String message)
    {
        Log.d("EXCEPTION: " + method,  message);

        AlertDialog.Builder messageBox = new AlertDialog.Builder(this);
        messageBox.setTitle(method);
        messageBox.setMessage(message);
        messageBox.setCancelable(false);
        messageBox.setNeutralButton("OK", null);
        messageBox.show();
    }
}
