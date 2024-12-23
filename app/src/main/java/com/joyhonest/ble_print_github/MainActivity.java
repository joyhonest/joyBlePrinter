package com.joyhonest.ble_print_github;

import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.joyhonest.joyBlePrinter.joyBlePrinterClient;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        joyBlePrinterClient.joyBlePrinter_Init(getApplicationContext());

    }
}