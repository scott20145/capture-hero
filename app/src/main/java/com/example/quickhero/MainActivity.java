package com.example.quickhero;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
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
            initCamera();
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

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    public void onButtonClicked(View view){
        takePicture();

        File imageFile = new File(
                getOutputDirectory(),
                new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                        .format(System.currentTimeMillis()) + ".jpg"
        );
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(imageFile).build();

        imageCapture.takePicture(
                outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String imageFilePath = imageFile.getAbsolutePath();
                        Log.d("MY_APP","saved:"+ imageFilePath);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.d("MY_APP","ERROR:"+ exception.getMessage());
                    }
                });
    }

    private File getOutputDirectory() {
        File outputDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        return outputDirectory;
    }

    private void takePicture() {

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
}