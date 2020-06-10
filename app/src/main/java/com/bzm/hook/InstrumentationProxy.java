package com.bzm.hook;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;


public class InstrumentationProxy extends Instrumentation {

    private static final String TARGET_INTENT = "target_intent";

    private Instrumentation mInstrumentation;

    private PackageManager mPackageManager;
    private static final String TAG = "=======";

    public InstrumentationProxy(Instrumentation instrumentation,PackageManager packageManager){
        mInstrumentation = instrumentation;
        mPackageManager = packageManager;
    }

    public InstrumentationProxy(Instrumentation instrumentation) {
        mInstrumentation = instrumentation;
    }


    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        Log.e(TAG, "execStartActivity  start" );
        try {
            intent.putExtra(TARGET_INTENT, intent.getComponent().getClassName());
//            //设置为占坑Activity
            intent.setClassName(who, "com.bzm.hook.StubActivity");
            boolean intentIsNull = intent == null;
            Log.e(TAG, "execStartActivity: " + intent.getComponent().toString() + intentIsNull );

            Method execStartActivityMethod = Instrumentation.class.getDeclaredMethod(
                    "execStartActivity",Context.class,IBinder.class,IBinder.class,Activity.class
                    ,Intent.class,int.class,Bundle.class);
            execStartActivityMethod.setAccessible(true);
            Log.e(TAG, "execStartActivity: " + execStartActivityMethod.getName() );
            ActivityResult result = (ActivityResult)execStartActivityMethod.invoke
                    (mInstrumentation,who,contextThread,token,target,intent,requestCode,options);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "execStartActivity Exception: " + e.getMessage() );
            e.printStackTrace();
        }
        Log.e(TAG, "return null ");
        return null;
    }


    public Activity newActivity(ClassLoader cl, String className,
                                Intent intent) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        Log.e(TAG, "newActivity: " );
        String clazName = intent.getStringExtra(TARGET_INTENT);
        if (!TextUtils.isEmpty(clazName)){

            return mInstrumentation.newActivity(cl,clazName,intent);
        }
        return mInstrumentation.newActivity(cl,className,intent);

    }



}
