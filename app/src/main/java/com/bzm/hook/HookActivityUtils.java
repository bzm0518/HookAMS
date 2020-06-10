package com.bzm.hook;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class HookActivityUtils {

    private static final String TAG = "=====";
    private static final String TARGET_INTENT = "target_intent";

    public static void hookActivityManager(final Activity activity){
        try{

            Field gDefaultField;
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
                //用的是getService 来获取IActivityManager
                Class<?> amClaz = Class.forName("android.app.ActivityManager");
                gDefaultField = amClaz.getDeclaredField("IActivityManagerSingleton");
            }else {
                //用的是getDefalt
                Class<?> amClaz = Class.forName("android.app.ActivityManagerNative");
                gDefaultField = amClaz.getDeclaredField("gDefault");
            }
            gDefaultField.setAccessible(true);
            Object gDefault = gDefaultField.get(null);

            Class singletonClaz = Class.forName("android.util.Singleton");
            Field mInstance = singletonClaz.getDeclaredField("mInstance");
            mInstance.setAccessible(true);
            final Object iam = mInstance.get(gDefault);

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> iActivityManagerInterface = Class.forName("android.app.IActivityManager");

            Object proxy = Proxy.newProxyInstance(classLoader, new Class[]{iActivityManagerInterface}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    //判断调用的方法，如果是startActivity的话，拦截，修改里面的Intent
                    if (method.getName().equals("startActivity")){
                        for (Object arg : args) {
                            Log.e(TAG, "invoke args length: " + args.length + ",arg" + arg );
                        }
                        int index = 0;
                        Intent rawIntent = null;
                        for (int i = 0; i < args.length; i++) {
                            if (args[i] instanceof Intent){
                                index = i;
                                rawIntent = (Intent) args[i];
                                break;
                            }
                        }
                        //偷梁换柱,把StubActivity给换上去
                        Intent newIntent = new Intent();
                        newIntent.setComponent(new ComponentName(activity.getPackageName(),StubActivity.class.getName()));
                        newIntent.putExtra(TARGET_INTENT,rawIntent);
                        args[index] = newIntent;
                    }
                    //调用方法  相当于iam.startActivity(args)
                    return method.invoke(iam,args);
                }
            });
            mInstance.set(gDefault,proxy);
        }catch (Exception e){
            Log.e(TAG, "hookActivityManager: exception" + e.getMessage() );
            e.printStackTrace();
        }

    }


    public static void hookHandler(){
        try {
            //1.获取ActivityThread的class
            //2.获取里面的成员 sCurrentActivityThread
            //3.获取mH H继承Handler
            //4.转化mH 为 Handler
            //5.通过Handler获取mCallback
            //6.在callback里面把Intent的参数换过来
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Field sCurrentActivityThreadField = atClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);

            Object sCurrentActivityThread = sCurrentActivityThreadField.get(null);

            Field mHField = atClass.getDeclaredField("mH");
            mHField.setAccessible(true);
            final Handler mH = (Handler) mHField.get(sCurrentActivityThread);

            Field mCallbackField = Handler.class.getDeclaredField("mCallback");
            mCallbackField.setAccessible(true);

            mCallbackField.set(mH, new Handler.Callback() {
                @Override
                public boolean handleMessage(@NonNull Message msg) {
                    switch (msg.what){
                        case 100:{
                            try {
                                Field intentField = msg.obj.getClass().getDeclaredField("intent");
                                intentField.setAccessible(true);
                                Intent intent = (Intent) intentField.get(msg.obj);
                                Intent rawIntent = intent.getParcelableExtra(TARGET_INTENT);
                                intent.setComponent(rawIntent.getComponent());

                            } catch (Exception e) {
                                Log.e(TAG, "handleMsg 100: Exception" + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        break;
                        case 159:{
                            try{
                                Object obj = msg.obj;
                                Field mActivityCallbacksField = obj.getClass().getDeclaredField("mActivityCallbacks");
                                mActivityCallbacksField.setAccessible(true);
                                List<Object> mActivityCallbacks = (List<Object>) mActivityCallbacksField.get(obj);
                                Log.e(TAG, "handleMessage: mActivityCallbacks= " + mActivityCallbacks);
                                if (mActivityCallbacks.size() > 0 ){
                                    String luanchName = "android.app.servertransaction.LaunchActivityItem";
                                    if (mActivityCallbacks.get(0).getClass().getCanonicalName().equals(luanchName)){
                                        Log.e(TAG, "handleMessage: 找到了LaunchActivityItem" );
                                        Object object = mActivityCallbacks.get(0);
                                        Field intentField = object.getClass().getDeclaredField("mIntent");
                                        intentField.setAccessible(true);
                                        Intent intent = (Intent) intentField.get(object);
                                        Intent rawIntent = intent.getParcelableExtra("target_intent");
                                        intent.setComponent(rawIntent.getComponent());

                                    }
                                }
                            }catch (Exception e){
                                Log.e(TAG, "handleMsg 159: Exception" + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                    mH.handleMessage(msg);
                    return true;
                }
            });


        }catch (Exception e){
            Log.e(TAG, "hookHandler:Exception " + e.getMessage() );
            e.printStackTrace();
        }
    }

    /**
     * hookActivity
     * @param activity
     */
    public static void hookInstrumentation(Activity activity){
        try{

            Field mInstrumentationField = Activity.class.getDeclaredField("mInstrumentation");

            mInstrumentationField.setAccessible(true);

            Instrumentation mInstrumentation = (Instrumentation) mInstrumentationField.get(activity);

            InstrumentationProxy proxy = new InstrumentationProxy(mInstrumentation,activity.getPackageManager());
            mInstrumentationField.set(activity,proxy);

        }catch (Exception e){

        }
    }

    /**
     * hookActivityThread
     */
    public static void hookActivityThread(){
        try{
            //获取ActivityThread
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            //获取Activity里面的sCurrentActivityThread
            Field sCurrentActivityThreadField = atClass.getDeclaredField("sCurrentActivityThread");
            sCurrentActivityThreadField.setAccessible(true);
            //由于是static 所以get的时候不用object传null就可以了
            Object sCurrentActivityThread = sCurrentActivityThreadField.get(null);
            //获取ActivityThread的mInstrumentation
            Field mInstrumentationField = atClass.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            //获取mInstrumentation对象 设置静态代理
            Instrumentation mInstrumentation = (Instrumentation) mInstrumentationField.get(sCurrentActivityThread);
            Instrumentation proxy = new InstrumentationProxy(mInstrumentation);
            //把代理对象设置到ActivityThread里面
            mInstrumentationField.set(sCurrentActivityThread,proxy);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
