package com.bzm.hookactivity;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.bzm.hook.HookActivityUtils;
import com.bzm.hook.TargetActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    public void hookAms(View view) {
        //第一种方法
        HookActivityUtils.hookActivityManager(this);
        HookActivityUtils.hookHandler();

        startActivity(new Intent(this, TargetActivity.class));
    }

    public void hookAt(View view) {
        //第二种方法
        HookActivityUtils.hookActivityThread();
        Intent intent = new Intent(this, TargetActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //需要Context来启动Activity
        getApplicationContext().startActivity(intent);
    }
}
