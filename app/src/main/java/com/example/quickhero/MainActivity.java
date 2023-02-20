package com.example.quickhero;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private Camera camera;
    private PreviewView previewView;

    private ImageCapture imageCapture;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    private boolean cameraPermissionGranted = false;
    private ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();

    private int remainingTime = 10;

    private ArrayList<String> imageFileNames = new ArrayList<>();

    private Runnable myRunnable;

    private boolean isReady = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Check if camera permission has been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // Camera permission has already been granted
            cameraPermissionGranted = true;
            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
            isReady = true;
        } else {
            // Camera permission has not been granted yet
            requestCameraPermission();
        }
    }

    private void initCamera() {
        // Initialize the PreviewView object
        previewView = findViewById(R.id.preview_view);

        // Wait for the view to be laid out before initializing the camera
        previewView.post(() -> startCamera());
    }

    public void onButtonClicked(View view){
        takePicture(null);
        imageFileNames.clear();

        TextView countdownText = findViewById(R.id.countdown_text);
        countdownText.setVisibility(View.VISIBLE);
        countdownText.setAlpha(1f);
        remainingTime = 10;

        new Thread(myRunnable).start();

        // Wait for 10 seconds before taking the second picture
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 1) {
                    remainingTime--;
                    countdownText.setText(getString(R.string.countdown_text, remainingTime));
                    handler.postDelayed(this, 1000);
                } else {
                    // Fade out the countdown text
                    ObjectAnimator fadeOut = ObjectAnimator.ofFloat(countdownText, "alpha", 1f, 0f);
                    fadeOut.setDuration(1000);
                    fadeOut.start();
                    // Take the second picture
                    takePicture(new OnImageSavedCallback() {
                        @Override
                        public void onImageSaved() {
                            Intent intent = new Intent(MainActivity.this, PreviewActivity.class);
                            intent.putStringArrayListExtra("temp_images", imageFileNames);
                            startActivity(intent);
                        }
                    });
                }
            }
        }, 1000);
    }

    private Bitmap addTimestampWatermark(Bitmap src, String timestamp, Paint paint) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);
        Rect bounds = new Rect();
        paint.getTextBounds(timestamp, 0, timestamp.length(), bounds);
        int x = w - bounds.width() - 16;
        int y = h - bounds.height() - 16;
        canvas.drawText(timestamp, x, y, paint);
        return result;
    }

    private File getOutputDirectory() {
        File outputDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        // return outputDirectory;
        return getExternalMediaDirs()[0];
    }

    private interface OnImageSavedCallback {
        void onImageSaved();
    }

    private void takePicture(final OnImageSavedCallback callback) {
        File imageFile = new File(
                getOutputDirectory(),
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                        .format(System.currentTimeMillis()) + ".jpg"
        );
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                // Convert the YUV_420_888 image to a byte array
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);

                // Create a Bitmap object from the JPEG data
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                // Add a timestamp watermark at the right-bottom corner
                Paint paint = new Paint();
                paint.setColor(Color.BLUE);
                paint.setTextSize(120);
                paint.setAntiAlias(true);
                paint.setTextAlign(Paint.Align.RIGHT);
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                bitmap = addTimestampWatermark(bitmap, timestamp, paint);

                // Save the edited image to the DCIM folder
                File outputDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
                if (!outputDirectory.exists()) {
                    outputDirectory.mkdirs();
                }
                String fileName = "edited_" + System.currentTimeMillis() + ".jpg";
                imageFileNames.add(fileName);
                File outputFile = new File(outputDirectory, fileName);
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(outputFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    image.close();
                    if(callback != null) {
                        callback.onImageSaved();
                    }
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.d("MY_APP","ERROR:"+ exception.getMessage());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // The picture was taken successfully, do something with it
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Log.d("CAMERA","12345");
            // TODO: Save the image to a file or display it in an ImageView
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            // Show an explanation to the user if they have previously denied the permission
            new AlertDialog.Builder(this)
                    .setTitle("Camera permission needed")
                    .setMessage("This app needs camera permission to take pictures.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Request camera permission
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                REQUEST_CAMERA_PERMISSION);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // User cancelled the dialog
                        finish();
                    })
                    .create()
                    .show();
        } else {
            // No explanation needed, request camera permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            // Check if camera permission has been granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted
                cameraPermissionGranted = true;
                initCamera();
            } else {
                // Camera permission has been denied
                Toast.makeText(this, "Camera permission is required to take pictures.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private void startCamera() {
        // Set up a configuration object for the camera
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Set up the camera preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Bind the camera use cases to the lifecycle of the activity
                Camera camera = cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview,imageCapture);
                // Save a reference to the camera object
                this.camera = camera;

            } catch (ExecutionException | InterruptedException e) {
                // Handle any exceptions
                Log.e("MyActivity", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start the camera
        if(isReady) {
            initCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                // Handle any exceptions
                Log.e("MyActivity", "Error camera pause", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
}