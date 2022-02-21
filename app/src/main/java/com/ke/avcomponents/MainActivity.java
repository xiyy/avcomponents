package com.ke.avcomponents;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mScreenCaptureBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScreenCaptureBtn = findViewById(R.id.screen_capture);
        mScreenCaptureBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.screen_capture:
                Intent intent = new Intent(MainActivity.this, ScreenCaptureActivity.class);
                startActivity(intent);

                break;
        }
    }
}