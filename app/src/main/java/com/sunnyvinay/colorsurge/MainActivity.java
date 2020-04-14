package com.sunnyvinay.colorsurge;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    Button camButton;
    Button fileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camButton = findViewById(R.id.camButton);
        fileButton = findViewById(R.id.fileButton);

        camButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to camera
                Intent intent = new Intent(MainActivity.this, PictureActivity.class);
                intent.putExtra("Location", "Camera");
                startActivity(intent);
            }
        });

        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to gallery
                Intent intent = new Intent(MainActivity.this, PictureActivity.class);
                intent.putExtra("Location", "Gallery");
                startActivity(intent);
            }
        });
    }
}
