package com.bzm.hook;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.bzm.hookactivity.R;

public class StubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stub);
    }
}
