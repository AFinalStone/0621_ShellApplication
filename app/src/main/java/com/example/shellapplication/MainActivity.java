package com.example.shellapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ClassLoader classLoader = getClassLoader();
        while (classLoader != null) {
            System.out.println("======================" + classLoader);
            classLoader = classLoader.getParent();
        }
    }
}