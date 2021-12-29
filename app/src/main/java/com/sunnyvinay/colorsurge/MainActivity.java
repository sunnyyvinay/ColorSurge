package com.sunnyvinay.colorsurge;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    Button camButton;
    Button fileButton;
    Switch themeSwitch;

    SharedPreferences settings;
    private SharedPreferences.Editor settingsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        settingsEditor = settings.edit();
        if (settings.getBoolean("Theme", true)) {
            setTheme(R.style.Azure);
        } else {
            setTheme(R.style.Azurelight);
        }

        setContentView(R.layout.activity_main);

        camButton = findViewById(R.id.camButton);
        fileButton = findViewById(R.id.fileButton);
        themeSwitch = findViewById(R.id.themeSwitch);

        if (settings.getBoolean("Theme", true)) {
            themeSwitch.setChecked(true);
        } else {
            themeSwitch.setChecked(false);
        }

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

        themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Dark theme enabled", Toast.LENGTH_SHORT);
                    toast.show();
                    settingsEditor.putBoolean("Theme", true);
                    settingsEditor.apply();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Dark theme disabled", Toast.LENGTH_SHORT);
                    toast.show();
                    settingsEditor.putBoolean("Theme", false);
                    settingsEditor.apply();
                }
                recreate();
            }
        });
    }
}
