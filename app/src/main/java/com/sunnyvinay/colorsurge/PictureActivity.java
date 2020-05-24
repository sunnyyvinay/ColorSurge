package com.sunnyvinay.colorsurge;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private ImageView toColorImage;
    private int toColor = Color.parseColor("#0099ff");
    private ImageButton undoButton;
    private ImageButton redoButton;
    private ImageButton saveButton;

    private ArrayList<Bitmap> bitmaps = new ArrayList<>();
    private ArrayList<Bitmap> undoneBitmaps = new ArrayList<>();

    private ImageButton eraseButton;
    float currentBrush;
    float smallBrush, mediumBrush, largeBrush;
    boolean eraseActivated = false;
    boolean colorChanged = false;

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

        toColorImage = findViewById(R.id.toColorImage);
        toColorImage.setVisibility(View.VISIBLE);
        toColorImage.setColorFilter(Color.parseColor("#0099ff"));
        //recolorButton = findViewById(R.id.recolorButton);

        undoButton = findViewById(R.id.undoButton);
        undoButton.setEnabled(false);
        redoButton = findViewById(R.id.redoButton);
        redoButton.setEnabled(false);
        saveButton = findViewById(R.id.saveButton);

        eraseButton = findViewById(R.id.eraseButton);
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);
        currentBrush = mediumBrush;

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
                //String[] mimeTypes = {"image/jpeg", "image/png"};
                //intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
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

    @SuppressLint("ClickableViewAccessibility")
    public void onActivityResult(int requestCode, final int resultCode, Intent data) {
        // Result code is RESULT_OK only if the user selects an Image
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case GALLERY_REQUEST:
                    InputStream inputStream;
                    try {
                        inputStream = getApplicationContext().getContentResolver().openInputStream(data.getData());
                        pictureBit = BitmapFactory.decodeStream(inputStream);
                        bitmaps.add(pictureBit);
                        pictureView.setImageBitmap(pictureBit);
                        inputStream.close();
                    } catch (FileNotFoundException e) {
                        Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    } catch (NullPointerException | IOException e) {
                        Toast.makeText(this, "An error occurred. Try again later", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    break;

                case CAMERA_REQUEST:
                    File f = new File(currentPhotoPath);

                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    bmOptions.inJustDecodeBounds = true;

                    bmOptions.inJustDecodeBounds = false;

                    pictureBit = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
                    bitmaps.add(pictureBit);
                    pictureView.setImageBitmap(pictureBit);
                    break;
            }
            pictureView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });

            pictureView.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Matrix inverse = new Matrix();
                    pictureView.getImageMatrix().invert(inverse);
                    float[] touchPoint = new float[]{motionEvent.getX(), motionEvent.getY()};
                    inverse.mapPoints(touchPoint);
                    int x = (int) touchPoint[0];
                    int y = (int) touchPoint[1];

                    if (!eraseActivated) {
                        try {
                            final int originalPixel = ((BitmapDrawable) pictureView.getDrawable()).getBitmap().getPixel(x, y);

                            // represents RGB of original clicked on pixel
                            final int pixRed = Color.red(originalPixel);
                            redColor.setText("R: " + pixRed);
                            final int pixBlue = Color.blue(originalPixel);
                            blueColor.setText("B: " + pixBlue);
                            final int pixGreen = Color.green(originalPixel);
                            greenColor.setText("G: " + pixGreen);

                            final String hex = String.format("#%02X%02X%02X", pixRed, pixGreen, pixBlue);
                            hexText.setText("Hex: " + hex);

                            fromColorImage.setVisibility(View.VISIBLE);
                            fromColorImage.setColorFilter(Color.parseColor(hex));

                            toColorImage.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ColorPickerDialogBuilder
                                            .with(PictureActivity.this, R.style.BlueSurge)
                                            .setTitle("Choose color")
                                            .initialColor(toColor)
                                            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                                            .density(8)
                                            .setOnColorSelectedListener(new OnColorSelectedListener() {
                                                @Override
                                                public void onColorSelected(int selectedColor) { }
                                            })
                                            .setPositiveButton("SELECT", new ColorPickerClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                                    // Choose color and immediately recolor image
                                                    toColorImage.setColorFilter(selectedColor);
                                                    toColor = selectedColor;
                                                    colorChanged = true;

                                                    // Represents RGB of user selected color
                                                    int toRed = getRed(selectedColor);
                                                    int toGreen = getGreen(selectedColor);
                                                    int toBlue = getBlue(selectedColor);

                                                    // Represents RGB of difference between original "TO" and "FROM" colors
                                                    int fromPixR = toRed - pixRed;
                                                    int fromPixG = toGreen - pixGreen;
                                                    int fromPixB = toBlue - pixBlue;

                                                    Bitmap resultBit = Bitmap.createBitmap(pictureBit.getWidth(), pictureBit.getHeight(), Bitmap.Config.ARGB_8888);;

                                                    int[] pixels = new int[pictureBit.getHeight()*pictureBit.getWidth()];
                                                    ((BitmapDrawable) pictureView.getDrawable()).getBitmap().getPixels(pixels, 0, pictureBit.getWidth(), 0, 0, pictureBit.getWidth(), pictureBit.getHeight());

                                                    for (int i = 0; i < pixels.length; i++) {
                                                        // Represents RGB of current pixel
                                                        int fromR = getRed(pixels[i]);
                                                        int fromG = getGreen(pixels[i]);
                                                        int fromB = getBlue(pixels[i]);

                                                        if (isShade(fromR, fromG, fromB, pixRed, pixGreen, pixBlue)) {
                                                            int newR = (fromPixR >= 0) ? (Math.min(255, fromR + (fromPixR))) : (Math.max(0, fromR + (fromPixR)));
                                                            int newG = (fromPixG >= 0) ? (Math.min(255, fromG + (fromPixG))) : (Math.max(0, fromG + (fromPixG)));
                                                            int newB = (fromPixB >= 0) ? (Math.min(255, fromB + (fromPixB))) : (Math.max(0, fromB + (fromPixB)));

                                                            pixels[i] = createRGBFromColors(newR, newG, newB);
                                                        }
                                                    }

                                                    resultBit.setPixels(pixels, 0, pictureBit.getWidth(), 0, 0, pictureBit.getWidth(), pictureBit.getHeight());

                                                /*
                                                redColor.setText("R: " + Color.red(selectedColor));
                                                blueColor.setText("B: " + Color.blue(selectedColor));
                                                greenColor.setText("G: " + Color.green(selectedColor));

                                                String newHex = String.format("#%02X%02X%02X", Color.red(selectedColor), Color.green(selectedColor), Color.blue(selectedColor));
                                                hexText.setText("Hex: " + newHex);

                                                fromColorImage.setColorFilter(Color.parseColor(newHex));
                                                 */

                                                    bitmaps.add(resultBit);
                                                    pictureView.setImageBitmap(resultBit);

                                                    undoButton.setEnabled(true);
                                                    undoButton.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            if (bitmaps.size() > 1) {
                                                                pictureView.setImageBitmap(bitmaps.get(bitmaps.size() - 2));
                                                                undoneBitmaps.add(bitmaps.remove(bitmaps.size() - 1));
                                                                if (bitmaps.size() == 1) {
                                                                    undoButton.setEnabled(false);
                                                                }
                                                                redoButton.setEnabled(true);
                                                            }

                                                            redoButton.setEnabled(true);
                                                        }
                                                    });
                                                    redoButton.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View v) {
                                                            if (undoneBitmaps.size() > 0) {
                                                                pictureView.setImageBitmap(undoneBitmaps.get(undoneBitmaps.size()-1));
                                                                bitmaps.add(undoneBitmaps.remove(undoneBitmaps.size()-1));
                                                                if (undoneBitmaps.size() == 0) {
                                                                    redoButton.setEnabled(false);
                                                                }
                                                                undoButton.setEnabled(true);
                                                            }
                                                        }
                                                    });
                                                }
                                            })
                                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) { }
                                            })
                                            .build()
                                            .show();
                                }
                            });

                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            Toast.makeText(PictureActivity.this, "Color not chosen", Toast.LENGTH_LONG).show();
                        }

                    } else if (colorChanged) {
                        Bitmap currentBit = ((BitmapDrawable) pictureView.getDrawable()).getBitmap();
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                try {
                                    for (int i = 0; i < currentBrush; i++) {
                                        for (int j = 0; j < currentBrush; j++) {
                                            currentBit.setPixel(Math.min(pictureBit.getWidth(), x+i), Math.min(pictureBit.getHeight(), y+j),
                                                    pictureBit.getPixel(Math.min(pictureBit.getWidth(), x+i), Math.min(pictureBit.getHeight(), y+j)));
                                        }
                                    }

                                    pictureView.setImageBitmap(currentBit);
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                }

                                return true;
                            case MotionEvent.ACTION_UP:
                                bitmaps.add(currentBit);
                                return true;
                            default:
                                return PictureActivity.super.onTouchEvent(motionEvent);
                        }
                    }

                    return true;
                }
            });

            eraseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eraseActivated) {
                        eraseButton.setImageResource(R.drawable.white_erase);
                        eraseActivated = false;
                    } else {
                        final Dialog eraseDialog = new Dialog(PictureActivity.this);
                        eraseDialog.setTitle("Eraser size:");
                        eraseDialog.setContentView(R.layout.brush_chooser);

                        ImageButton smallBtn = eraseDialog.findViewById(R.id.small_brush);
                        smallBtn.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {
                                currentBrush = smallBrush;
                                eraseButton.setImageResource(R.drawable.blue_erase);
                                eraseActivated = true;
                                eraseDialog.dismiss();
                            }
                        });
                        ImageButton mediumBtn = eraseDialog.findViewById(R.id.medium_brush);
                        mediumBtn.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {
                                currentBrush = mediumBrush;
                                eraseButton.setImageResource(R.drawable.blue_erase);
                                eraseActivated = true;
                                eraseDialog.dismiss();
                            }
                        });
                        ImageButton largeBtn = eraseDialog.findViewById(R.id.large_brush);
                        largeBtn.setOnClickListener(new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {
                                currentBrush = largeBrush;
                                eraseButton.setImageResource(R.drawable.blue_erase);
                                eraseActivated = true;
                                eraseDialog.dismiss();
                            }
                        });

                        eraseDialog.show();
                    }
                }
            });

            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContentValues values = new ContentValues();
                            String filename = System.currentTimeMillis() + ".jpg";

                            values.put(MediaStore.Images.Media.TITLE, filename);
                            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
                            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
                            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/");

                            Uri uri = getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            if (uri != null) {
                                saveImageToStream(((BitmapDrawable) pictureView.getDrawable()).getBitmap(), getApplicationContext().getContentResolver().openOutputStream(uri));
                                values.put(MediaStore.Images.Media.IS_PENDING, false);
                                getApplicationContext().getContentResolver().update(uri, values, null, null);
                            }

                        } else {
                            MediaStore.Images.Media.insertImage(getContentResolver(), ((BitmapDrawable) pictureView.getDrawable()).getBitmap(), "", "");
                        }

                        Toast.makeText(PictureActivity.this, "Saved to Gallery", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(PictureActivity.this, "Unable to save. Try again later", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            });

        } else {
            // Activity result is not OK - user did not select a photo
            Intent intent = new Intent(PictureActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    public boolean isShade(int fromR, int fromG, int fromB, int toR, int toG, int toB) {
        return (fromR >= toR-40 && fromR <= toR+40) && // Determines whether certain pixel is a shade of the "TO" color
                (fromG >= toG-40 && fromG <= toG+40) &&
                (fromB >= toB-40 && fromB <= toB+40);
    }

    public static int getRed(int rgb) { return (rgb & 0x00FF0000) >> 16; }

    public static int getGreen(int rgb) { return (rgb & 0x0000FF00) >> 8; }

    public static int getBlue(int rgb) { return rgb & 0x000000FF; }

    public static int createRGBFromColors(int red, int green, int blue) {
        int rgb = 0;

        rgb |= blue;
        rgb |= green << 8;
        rgb |= red << 16;

        rgb |= 0xFF000000;

        return rgb;
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
                Toast.makeText(PictureActivity.this, "Error ocurred. Try again later", Toast.LENGTH_LONG).show();
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

    private void saveImageToStream(Bitmap bitmap, OutputStream outputStream) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
            } catch (Exception e) {
                Toast.makeText(PictureActivity.this, "Unable to save. Try again later", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }
}
