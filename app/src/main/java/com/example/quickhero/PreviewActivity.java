package com.example.quickhero;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class PreviewActivity extends AppCompatActivity {

    private ImageView imageView1, imageView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        Log.d("PREVIEW", "開始preview");
        displayImages();
    }

    private void displayImages() {
        imageView1 = findViewById(R.id.image1);
        imageView2 = findViewById(R.id.image2);
        ArrayList<String> bitmapList = getIntent().getStringArrayListExtra("temp_images");
        // Display the first two bitmaps in the list
        File dcimFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        File imageFile1 = new File(dcimFolder, bitmapList.get(0));
        File imageFile2 = new File(dcimFolder, bitmapList.get(1));
        Log.d("PREVIEW", imageFile1.getAbsolutePath());
        Log.d("PREVIEW", imageFile2.getAbsolutePath());
        Bitmap bitmap1 = BitmapFactory.decodeFile(imageFile1.getAbsolutePath());
        Bitmap bitmap2 = BitmapFactory.decodeFile(imageFile2.getAbsolutePath());
        imageView1.setImageBitmap(bitmap1);
        imageView2.setImageBitmap(bitmap2);
    }

    private void writeImages() {
        try {
            // Save the edited image to the DCIM folder
            File outputDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }
            ArrayList<Bitmap> imageFileList = getIntent().getParcelableArrayListExtra("image_files");
            for(int i = 0; i < imageFileList.size();i++) {
                File outputFile = new File(outputDirectory, "edited_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(outputFile);
                imageFileList.get(i).compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}