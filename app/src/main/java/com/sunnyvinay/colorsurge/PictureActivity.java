package com.sunnyvinay.colorsurge;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PictureActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 1888;
    private static final int GALLERY_REQUEST = 1889;

    private ImageView pictureView;
    String currentPhotoPath;
    Bitmap pictureBit = null;

    ActionBar bar;
    private ImageView fromColorImage;
    private TextView redColor;
    private TextView greenColor;
    private TextView blueColor;
    private TextView hexText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        pictureView = findViewById(R.id.pictureView);
        fromColorImage = findViewById(R.id.fromColorImage);
        redColor = findViewById(R.id.redColor);
        greenColor = findViewById(R.id.greenColor);
        blueColor = findViewById(R.id.blueColor);
        hexText = findViewById(R.id.hexText);

        bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        if ((getIntent().getStringExtra("Location")).equals("Camera")) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_REQUEST);
            } else {
                openCameraTocaptureImage();
            }

        } else { // User wants gallery
            if (ActivityCompat.checkSelfPermission(PictureActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(PictureActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PictureActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, GALLERY_REQUEST);
            } else { // Permissions already granted
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                String[] mimeTypes = {"image/jpeg", "image/png"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                startActivityForResult(intent, GALLERY_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case GALLERY_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, GALLERY_REQUEST);
                } else {
                    // User denied permission
                    Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_LONG).show();
                }
                break;
            case CAMERA_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_LONG).show();
                    openCameraTocaptureImage();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Result code is RESULT_OK only if the user selects an Image
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST:
                    BitmapFactory.Options options;
                    String[] projection = new String[]{
                            MediaStore.Images.ImageColumns._ID,
                            MediaStore.Images.ImageColumns.DATA,
                            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                            MediaStore.Images.ImageColumns.DATE_TAKEN,
                            MediaStore.Images.ImageColumns.MIME_TYPE,
                            MediaStore.Images.ImageColumns.DISPLAY_NAME,
                    };
                    final Cursor cursor = this.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

                    if (cursor.moveToFirst()) {
                        if (Build.VERSION.SDK_INT >= 29) { // Different for Android 10
                            Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getInt(0));

                            try (ParcelFileDescriptor pfd = this.getContentResolver().openFileDescriptor(imageUri, "r")) {
                                if (pfd != null) {
                                    pictureBit = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                                }
                            } catch (IOException ex) {
                                Toast.makeText(this, "An error has occurred. Try again later.", Toast.LENGTH_LONG).show();
                                ex.printStackTrace();
                            }
                        } else {
                            String imageLocation = cursor.getString(1);
                            File imageFile = new File(imageLocation);

                            if (imageFile.exists()) {
                                options = new BitmapFactory.Options();
                                options.inSampleSize = 2;

                                try {
                                    pictureBit = BitmapFactory.decodeFile(imageLocation, options);
                                } catch (Exception e) {
                                    Toast.makeText(this, "An error has occurred. Try again later.", Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                }
                            }
                        }
                        pictureView.setImageBitmap(pictureBit);
                    }
                    break;

                case CAMERA_REQUEST:
                    File f = new File(currentPhotoPath);
                    Uri contentUri = Uri.fromFile(f);

                    //Toast.makeText(this, "uri "+contentUri, Toast.LENGTH_SHORT).show();

                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    bmOptions.inJustDecodeBounds = true;
                    // Decode the image file into a Bitmap sized to fill the View
                    bmOptions.inJustDecodeBounds = false;

                    pictureBit = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
                    pictureView.setImageBitmap(pictureBit);
                    break;
            }

            //TODO: Add accessibility for onTouch
            //TODO: Change font of instructions
            //TODO: Keep RGB and Hex in place
            pictureView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    try {
                        Matrix inverse = new Matrix();
                        pictureView.getImageMatrix().invert(inverse);
                        float[] touchPoint = new float[] {motionEvent.getX(), motionEvent.getY()};
                        inverse.mapPoints(touchPoint);
                        int x = Integer.valueOf((int)touchPoint[0]);
                        int y = Integer.valueOf((int)touchPoint[1]);
                        int pixel = pictureBit.getPixel(x,y);

                        int red = Color.red(pixel);
                        redColor.setText("R: " + red);
                        int blue = Color.blue(pixel);
                        blueColor.setText("B: " + blue);
                        int green = Color.green(pixel);
                        greenColor.setText("G: " + green);

                        String hex = String.format("#%02X%02X%02X", red, green, blue);
                        hexText.setText("Hex: " + hex);

                        fromColorImage.setColorFilter(Color.parseColor(hex));
                        //Log.i("Color from Pixel", hex);

                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        Toast toast = Toast.makeText(PictureActivity.this,
                                "Color not chosen",
                                Toast.LENGTH_LONG);
                        toast.show();
                    }

                    return false;
                }
            });
        }
    }

    private void openCameraTocaptureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
                Log.i("File error", "File error");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.sunnyvinay.colorsurge",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }
}
