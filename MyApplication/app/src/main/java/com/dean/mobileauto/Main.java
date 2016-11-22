package com.dean.mobileauto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity  implements View.OnClickListener, View.OnTouchListener{
    ImageView iv;
    ImageButton iBup;
    ImageButton iBleft;
    ImageButton iBright;
    ImageButton iBdown;
    ImageButton iBcom;
    ImageButton iBconnect;
    ImageButton speakButton;
    ImageButton iBgravity;
    ImageButton iBslide;//滑块
    ImageButton iBdjUp;
    ImageButton iBdjDown;
    ImageButton iBfLeft;
    ImageButton iBfRight;
    ImageButton iBbLeft;
    ImageButton iBbRight;
    ImageView circle;
    ImageView ivOrbit;//轨道
    boolean isFirstTouch = false;

    RelativeLayout.LayoutParams lp;
    RelativeLayout layoutkeyboard;//用以控制滑块
    private ListView mList;

    //******************手势识别部分*******************************************
    LinearLayout gestureLayout;
    LinearLayout.LayoutParams LinearParams = null;
    GestureView myGestureView = null;
    static int curr_angle = 90;
    // 手机上的方向永远为正方向
    private static final int GESTURE_OK = 5;
    String cmd;
    //**********************************************************************


    //**********************wifi 连接部分****************************************
    TextView command;

    String ip = "110"; //ip 为主机地址
    int PORT = 8888;

    String CommandToServer = null;// 最终要发送的数据
    Boolean isConnected = false;

    // 通过设置来选择传输哪一个字符串
    String keyboradString = null;
    String slideString = "c100#";
    String voiceString = null;
    String gravityString = null;
    String gestureString = null;
    String selected = "1"; // 保存选择的操作模式

    private static final int SEND_MESSAGE = 2;
    private static final int CONNECT_FAILED = 3;
    private Handler cmdHandler;
    MyWifiThread wifiThread = null;
    boolean isTrue = true;
    //***********************************************************************

    private BroadcastReceiver myBroadcast = new BroadcastReceiver(){
        @Override   //语音识别接收部分
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String tmpString;
            tmpString = intent.getStringExtra("name");
            Judge judger = new Judge();
            voiceString = judger.translate(tmpString);
            changeCommand();
            if(selected.contentEquals("2")) { // 若选择3则会尝试发送数据
                if(wifiThread != null){
                    Message msg = cmdHandler.obtainMessage();
                    msg.obj = SEND_MESSAGE;
                    // 发送消息，MainThread 向 WorkerThread 发送消息
                    cmdHandler.sendMessage(msg);
                }
            }
        }
    };

    //************************ 重力感应部分****************************************
    String axis; // 显示手机陀螺仪坐标
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private OnSensorEventListener mOnSensorEventListener = new OnSensorEventListener();
    private static final int UPDATE_MY_AXIS = 1;
    Message message = null;

    //************视频传输部分***********************************
    private Bitmap bitmap;
    private static final int COMPLETED = 0;
    //**********************************************************
    private  Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            System.out.println("连接失败04");
            switch (msg.what) {
                case UPDATE_MY_AXIS:
                    String currentAxis = (String)msg.obj;
                    gravityString = currentAxis;
                    changeCommand();
                    if(Main.this.selected.contentEquals("3")){
                        if(wifiThread != null){
                            Message msg1 = cmdHandler.obtainMessage();
                            msg1.obj = SEND_MESSAGE;
                            cmdHandler.sendMessage(msg1);
                        }
                    }
                    break;
                case COMPLETED: //将视频与重力感应的handler合并
                    System.out.println("传来一帧图");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                iv.setImageBitmap(bitmap);
                            }
                            catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    });
                    break;
                case CONNECT_FAILED:
                    System.out.println("线程连接失败");
                    isTrue = false;
                    wifiThread.interrupt();
                    wifiThread = null;
                    System.out.println("连接失败05");
                    iBconnect.setEnabled(true);
                    System.out.println("连接失败06");
                    isConnected = false;
                    displayToast("连接失败，请重新连接");
                    break;
                case GESTURE_OK:
                    gestureString = cmd;
                    changeCommand();
                    if(selected.contentEquals("4")){
                        if(wifiThread != null){
                            Message msg1 = cmdHandler.obtainMessage();
                            msg1.obj = SEND_MESSAGE;
                            cmdHandler.sendMessage(msg1);
                        }
                    }
            }
        }
    };

    private Thread updateAxis = null; // 用于更新坐标轴数据
    boolean runFlag = false;
    // ********************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();

        // 语音识别
        PackageManager pm = getPackageManager(); //用于获取语音识别界面的listview
        List activities = pm.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0); //本地识别程序
        if (activities.size() != 0) {
            speakButton.setOnClickListener(this);
        } else {                 // 若检测不到语音识别程序在本机安装，测将扭铵置灰
            speakButton.setEnabled(false);
            speakButton.setImageResource(R.drawable.mic);
        }
        //**********************************
        SharedPreferences getData;
        getData = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        selected = getData.getString("list", "1"); //

    }

    private void init() {
        // TODO Auto-generated method stub
        iv = (ImageView) findViewById (R.id.imageViewTop);
        circle = (ImageView) findViewById (R.id.imageCircle);
        ivOrbit = (ImageView) findViewById (R.id.ivOrbit);
        iBup = (ImageButton) findViewById (R.id.iBup);
        iBleft = (ImageButton) findViewById (R.id.iBleft);
        iBright = (ImageButton) findViewById (R.id.iBright);
        iBdown = (ImageButton) findViewById (R.id.iBdown);
        iBcom = (ImageButton) findViewById (R.id.iBcom);
        iBconnect = (ImageButton) findViewById (R.id.iBconnect);
        speakButton = (ImageButton) findViewById(R.id.iBvoiceMain); // 识别按钮
        iBgravity = (ImageButton) findViewById(R.id.iBgravity);
        iBslide = (ImageButton) findViewById(R.id.iBslide);
        iBdjDown = (ImageButton) findViewById(R.id.iBdjDown);
        iBdjUp = (ImageButton) findViewById(R.id.iBdjUp);
        iBfLeft = (ImageButton) findViewById(R.id.iBfLeft);
        iBfRight = (ImageButton) findViewById(R.id.iBfRight);
        iBbLeft = (ImageButton) findViewById(R.id.iBbLeft);
        iBbRight = (ImageButton) findViewById(R.id.iBbRight);
        mList = (ListView) findViewById(R.id.lvVoiceMain);
        command = (TextView) findViewById(R.id.tvCommand);
        iBup.setOnTouchListener(this);
        iBleft.setOnTouchListener(this);
        iBright.setOnTouchListener(this);
        iBdown.setOnTouchListener(this);
        iBdjDown.setOnTouchListener(this);
        iBdjUp.setOnTouchListener(this);
        iBfLeft.setOnTouchListener(this);
        iBfRight.setOnTouchListener(this);
        iBbLeft.setOnTouchListener(this);
        iBbRight.setOnTouchListener(this);
        iBcom.setOnClickListener(this);
        iBconnect.setOnClickListener(this);
        speakButton.setOnClickListener(this);
        iBgravity.setOnClickListener(this);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //注册传感器监听
        mSensorManager.registerListener(mOnSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        iBslide.setOnTouchListener(this);
        lp = (RelativeLayout.LayoutParams)iBslide.getLayoutParams();//lp 用来给ibslide的layout传递参数
        layoutkeyboard = (RelativeLayout)findViewById(R.id.KeyboradControlLayout);

        gestureLayout = (LinearLayout)findViewById(R.id.GestureLayout);
        LinearParams = new LinearLayout.LayoutParams(LinearParams.WRAP_CONTENT,220);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()){
            case R.id.iBcom:
                //*******视频*******************
                Thread myVideoThread = new MyVideoThread();
                myVideoThread.start();
                System.out.println("视频线程创建完成");
                iBcom.setEnabled(false);
                iBcom.setVisibility(View.INVISIBLE);
                break;
            case R.id.iBgravity:
                // ************重力感应部分***********
                if(!runFlag){
                    runFlag = true;
                    updateAxis = new Thread(){
                        @Override
                        public void run(){
                            message = handler.obtainMessage(UPDATE_MY_AXIS,axis);
                            handler.sendMessage(message);
                            handler.postDelayed(this, 1000);
                        }
                    };
                    updateAxis.start();
                }
                else{
                    runFlag = false;
                    handler.removeCallbacks(updateAxis);
                    updateAxis.stop();
                }
                break;
            case R.id.iBconnect:
                System.out.println("Try to connect");
                iBconnect.setEnabled(false);
                wifiThread = new MyWifiThread();
                wifiThread.start();
                isTrue = true;
                break;
            case R.id.iBvoiceMain:
                Intent a = new Intent(Main.this, Voice.class);
                a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(a);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu);
        MenuInflater blowUp = getMenuInflater();
        blowUp.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        switch(item.getItemId()){
            case R.id.aboutUs:
                Intent i = new Intent("com.dean.mobileauto.ABOUT");
                startActivity(i);
                break;
            case R.id.preferences:
                Intent s = new Intent("com.dean.mobileauto.PREFS");
                startActivity(s);
                break;
            case R.id.exit:
                System.exit(0);
                break;
        }
        return false;
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int[] location = new int[2];
        ivOrbit.getLocationOnScreen(location);
        int y = location[1];
        int  dy = 0;
        dy = (int) event.getRawY();

        if(event.getAction() == MotionEvent.ACTION_UP){
            BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.bk);
            circle.setImageDrawable(draw);
            isFirstTouch = false;
            keyboradString = slideString;
            System.out.println(keyboradString);
            changeCommand();
            System.out.println("命令以改变");
            if (selected.contentEquals("1")) {
                if (wifiThread != null) {
                    // send message
                    System.out.println("线程不空");
                    Message msg = cmdHandler.obtainMessage();
                    msg.obj = SEND_MESSAGE;
                    // 发送消息，MainThread 向 WorkerThread 发送消息
                    cmdHandler.sendMessage(msg);
                    System.out.println("发给子线程");
                }

            }
        } else {
            switch(v.getId()){
                case R.id.iBup:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.upbk);
                        circle.setImageDrawable(draw);
                        keyboradString = "f";
                    }
                    break;
                case R.id.iBdown:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.downbk);
                        circle.setImageDrawable(draw);
                        keyboradString = "b";
                    }
                    break;
                case R.id.iBleft:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.leftbk);
                        circle.setImageDrawable(draw);
                        keyboradString = "l";
                    }
                    break;
                case R.id.iBright:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.rightbk);
                        circle.setImageDrawable(draw);
                        keyboradString = "r";
                    }
                    break;
                case R.id.iBfLeft:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        keyboradString = "w";//左上  csj
                        BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.nwbk);
                        circle.setImageDrawable(draw);
                    }
                    break;
                case R.id.iBfRight:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        keyboradString = "x";//左上
                        BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.nebk);
                        circle.setImageDrawable(draw);
                    }
                    break;//右上
                case R.id.iBbLeft:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        keyboradString = "y";
                        BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.swbk);
                        circle.setImageDrawable(draw);
                    }
                    break;//左下
                case R.id.iBbRight:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        keyboradString = "z";
                        BitmapDrawable draw=(BitmapDrawable) getResources().getDrawable(R.drawable.sebk);
                        circle.setImageDrawable(draw);
                    }
                    break;//右下
                case R.id.iBdjUp:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        keyboradString = "h";//左上
                    }
                    break;//舵机上
                case R.id.iBdjDown:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        keyboradString = "i";//左上
                    }
                    break;//舵机下
                case R.id.iBslide:
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                    }
                    if(event.getAction() == MotionEvent.ACTION_MOVE){
//					iBslide.setLeft(x - temp[0]);
//					iBslide.setTop(y - temp[1]);  大错特错
                        if( (dy - y) >= -1 && (dy - y) <= ivOrbit.getHeight()-iBslide.getHeight()+5){
                            lp.setMargins(0,dy - y, 0, 0);
                            iBslide.setLayoutParams(lp);
                            int a = (ivOrbit.getHeight()+y-dy-44)*70/150+30;
                            if(a > 100) a = 100; if (a < 35) a = 0;
                            keyboradString = String.format("c%d#s", a);
                        }
                    }
                    slideString = keyboradString;
                    break;
            }
            if(!isFirstTouch) {
                isFirstTouch = true;
                changeCommand();
                if (selected.contentEquals("1")) {
                    if (wifiThread != null) {
                        System.out.println("线程不空");
                        Message msg = cmdHandler.obtainMessage();
                        msg.obj = SEND_MESSAGE;
                        cmdHandler.sendMessage(msg);
                        System.out.println("发给子线程");
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        IntentFilter intentFilter = new IntentFilter("ncn"); // zym
        registerReceiver(myBroadcast,intentFilter);

        SharedPreferences getData = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        ip = getData.getString("ip", "110");
        selected = getData.getString("list", "1");
        if(!selected.contentEquals("4")){
            gestureLayout.removeAllViews();
        }

        if(!selected.contentEquals("2")){
            speakButton.setEnabled(false);
        }else{speakButton.setEnabled(true);}

        if(selected.contentEquals("1")){
            layoutkeyboard.setVisibility(View.VISIBLE);
        }else{
            layoutkeyboard.setVisibility(View.GONE);
            if(selected.contentEquals("4")){
                    this.myGestureView = new GestureView(this);
                    gestureLayout.addView(myGestureView, LinearParams);

            }
        }

    }

    public void onPause(){
        super.onPause();
        gestureLayout.removeAllViews();
    }

    public void onDestroy(){
        super.onDestroy();
        //注销传感器监听
        mSensorManager.unregisterListener(mOnSensorEventListener, mAccelerometer);
        //System.exit(0);//
    }

    // 其他功能函数

    private class OnSensorEventListener implements SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x, y, z;
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            if(-1 <= x && x <= 1 && -1 <= y && y <= 1) axis = "s";
            else if(y >= 2 && -1 <= x && x <= 1) axis = "r";
            else if(y <= -2 && -1 <= x && x <= 1) axis = "l";
            else if(x >= 2 && -1 <= y && y <= 1) axis = "b";
            else if(x <= -2 && -1 <= y && y <= 1) axis = "f";
            else if(x <= -2 &&  y <= -2) axis = "w"; // lf
            else if(x <= -2 &&  y >= 2) axis = "x"; //rf
            else if(x >= 2 && y <= -2) axis = "y";
            else if(x >= 2 && y >= 2) axis = "z";
            //axis = String.format("x = %f\n y = %f\n z = %f\n", x, y, z);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    }


    private void displayToast(String s){
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void changeCommand() {
        String v = selected;
        if(v.contentEquals("1")){
            CommandToServer = keyboradString;
            command.setText(keyboradString);
        }else if (v.contentEquals("2")) {
            CommandToServer = voiceString;
            command.setText(voiceString);
        }else if (v.contentEquals("3")) {
            CommandToServer = gravityString;
            command.setText(gravityString);
        }else if (v.contentEquals("4")) {
            CommandToServer = gestureString;
            command.setText(gestureString);
        }
    } //改变要发送的指令

    class MyWifiThread extends Thread {
        public Socket clientSocket = null;
        public OutputStream outputStream = null;

        @Override
        public void run() {
            while (isTrue) {
                Looper.prepare();
                super.run();
                if (!isConnected) {  //!ip.contentEquals("")
                    try {
                        isConnected = true;
                        //实例化对象并连接到服务器
                        clientSocket = new Socket(); // 获得通信连接
                        SocketAddress socketAddress = new InetSocketAddress(ip, PORT);
                        //设置超时限制
                        clientSocket.connect(socketAddress, 1000);//设置超时限制
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        isConnected = false;
                        Message msg = handler.obtainMessage();
                        msg.what = CONNECT_FAILED;
                        handler.sendMessage(msg);
                        displayToast("主机地址错误");
                    } catch (IOException e) {
                        e.printStackTrace();
                        isConnected = false;
                        System.out.println("连接失败01");
                        Message msg = handler.obtainMessage();
                        msg.what = CONNECT_FAILED;
                        handler.sendMessage(msg);
                        System.out.println("连接失败02");
                        displayToast("IO异常");
                    }
                }
                if(isConnected) displayToast("连接成功！");
                //2. WifiThread中创建一个消息处理对象Handler
                cmdHandler = new Handler() {
                    // 重载消息处理方法，用于接收和处理WorkerThread发送的消息
                    @Override
                    public void handleMessage(Message msg) {
                        System.out.println("开始handle");
                    //    if (msg.what == SEND_MESSAGE) { // 连接成功才开始发送
                            byte[] msgBuffer = null;
                            System.out.println("子进程准备发送信息");
                            try {
                                //字符编码转换
                                msgBuffer = CommandToServer.getBytes("GB2312");
                            } catch (UnsupportedEncodingException e1) {
                                e1.printStackTrace();
                                displayToast("字符编码错误！");
                            }

                            try {
                                //获得Socket的输出流, 找到管道
                                outputStream = clientSocket.getOutputStream();
                                System.out.println("找管道");
                            } catch (IOException e) {
                                e.printStackTrace();
                                displayToast("Socket输出错误！");
                            }

                            try {
                                //往管道里发送数据
                                outputStream.write(msgBuffer);
                                CommandToServer = null;
                            } catch (IOException e) {
                                e.printStackTrace();
                                displayToast("写错误！");
                            }
                            System.out.println("发送成功");
                 //       }
                    }
                };

                // 调用Looper.loop()方法，从消息队列中不断获取消息，然后调用该消息对象的Handler对象的handleMessage(Message msg)进行处理
                // 如果消息队列中没有消息，则Looper线程阻塞等待
                Looper.loop();
            }
        }

    }

    class MyVideoThread extends Thread {
        public Socket s;
        public ServerSocket ss;

        public void run() {
            int len = 0;
            Message msg;
            msg = new Message();

            try {
                ss = new ServerSocket(8889);
                System.out.println("VideoServerSocket is created!");

            } catch (IOException e2) {
                e2.printStackTrace();
            }

            while (true) {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                try {
                    //接收并解码图片
                    s = ss.accept();
                    InputStream ins = null;
                    ins = s.getInputStream();
                    System.out.println("accept new pic");
                    byte[] buffer = new byte[1024];

                    while( (len = ins.read(buffer)) != -1) {
                        outStream.write(buffer, 0, len);
                        System.out.println("buffer len = "+ len);
                    }
                    ins.close();

                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                byte[] data;
                data = new byte[10240];
                data = outStream.toByteArray();
                Bitmap bm1 = BitmapFactory.decodeByteArray(data, 0, data.length);
                //翻转90度
                Matrix m = new Matrix();
                m.setRotate(90,(float) bm1.getWidth() / 2, (float) bm1.getHeight() / 2);
                final Bitmap bm2 = Bitmap.createBitmap(bm1, 0, 0, bm1.getWidth(), bm1.getHeight(), m, true);
                bitmap = bm2;

                msg = handler.obtainMessage();
                msg.what = COMPLETED;
                handler.sendMessage(msg);
                System.out.println("1pic");
                try {
                    outStream.flush();
                    outStream.close();
                    s.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


            }
        }


    }

    public class GestureView extends SurfaceView implements SurfaceHolder.Callback,
            Runnable {
        private Thread th = new Thread(this);
        private SurfaceHolder sfh;
        private Canvas canvas;

        private Paint paint;
        private ArrayList<PointF> graphics = new ArrayList<PointF>();
        private Paint lPaint;
        private Path mPath;
        private int of = 0;
        private int step = 0;
        private boolean over = false;// 表示已画完
        private boolean left = false;
        private boolean first = false;
        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        public GestureView(Context context) {
            super(context);
            this.setKeepScreenOn(true);
            sfh = this.getHolder();
            sfh.addCallback(this);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);//抗锯齿效果
            paint.setColor(Color.RED);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(45);

            lPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            lPaint.setColor(Color.BLACK);
            lPaint.setStyle(Paint.Style.STROKE);//空心
            lPaint.setStrokeJoin(Paint.Join.ROUND);
            lPaint.setStrokeCap(Paint.Cap.ROUND);
            lPaint.setStrokeWidth(5);

            mPath = new Path();
            this.setLongClickable(true);
            // setLongClickable( true )是必须的，因为 只有这样，
            // 我们当前的SurfaceView(view)才能够处理不同于触屏形式;
            // 例如：ACTION_MOVE，或者多个ACTION_DOWN
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // 当系统调用了此方法才创建了view所以在这里才能取到view的宽高！！有些童鞋总是把东西都放在初始化函数里！
            // 线程最好放在这里来启动，因为放在初始化里的画，那view还没有呢,到了提交画布unlockCanvasAndPost的时候就异常啦！
            th.start();
        }

        public void draw() {
            try {
                canvas = sfh.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.rgb(54,69,78));//清理
                    canvas.drawPath(mPath, lPaint);
                    canvas.drawRect(25,4,455,216,lPaint);
                    if (over && graphics.size() > 0) {
                        canvas.drawPoint(graphics.get(of).x, graphics.get(of).y, paint);
                        of += 1;
                        if (of < graphics.size()) {
                            if (of == graphics.size() - 1) {
                                mPath.reset();//移动完成后移除线条
                                System.out.println("the total size is "+graphics.size());
                            }
                            invalidate();
                        }
                    }
                    int step_length = (graphics.size() > 30) ? graphics.size()/5 : 6;//更改步长

                    if (over && step+step_length <= graphics.size()) {
                        step += step_length;
                        int turn = curr_angle - angle(graphics.get(step-step_length), graphics.get(step));
                        curr_angle = angle(graphics.get(step-step_length), graphics.get(step));
                        String cmd1;
                        if(turn < 0){
                           // turn += 180;
                           // if(left) {turn = 180-turn;left = false;}
                            turn = 0-turn;
                            cmd1 = String.format("k" + turn );
                        }else{
                           // if(left) {turn = 180-turn;left = false;}
                            if(left) {turn = 180-turn;left = false;cmd1 = String.format("k" + turn );}
                            else cmd1 = String.format("m" + turn );
                        }
                        String cmd2 = String.format("g" + distance(graphics.get(step-step_length), graphics.get(step)));
                        cmd = cmd + cmd1 + "#" + cmd2 + "#";
                        if (step + step_length >= graphics.size()) {
                            System.out.println(cmd);
                            Message msg = handler.obtainMessage();
                            msg.what = GESTURE_OK;
                            handler.sendMessage(msg);
                            curr_angle = 90;
                        }
                    }

                }
            }catch(Exception e){
                Log.v("Himi", "draw is Error!");
            }finally{
                try {
                    sfh.unlockCanvasAndPost(canvas);
                }catch(Exception e){
                }
            }
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            while (true) {
                draw();
                try {
                    Thread.sleep(1);
                } catch (Exception ex) {
                }
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();// x, y 为当前触摸点
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    over = false;
                    left = false;
                    first = false;
                    graphics.clear(); //清空点阵
                    of = 0;
                    step = 0;
                    cmd = "";
                    graphics.add(new PointF(x, y));
                    touch_start(x, y);
                    invalidate(); //请求重绘Canvas
                    break;
                case MotionEvent.ACTION_MOVE:
                    graphics.add(new PointF(x, y));
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    over = true;
                    touch_up();
                    invalidate();
                    break;
            }

            return true;
        }
        private void touch_start(float x, float y) {
            mPath.reset(); // 重设绘制Path
            mPath.moveTo(x, y);//设置Path起始点
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX); //mX为上一个路径点
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
            }
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
        }

        private int  distance(PointF a, PointF b ){
            double z = Math.pow((a.x - b.x), 2) + Math.pow((a.y - b.y), 2);
            z = Math.sqrt(z)/10;
            // System.out.println("distance = "+(int)z);
            return (int) z;
        }

        private int angle(PointF a, PointF b){
            double x = (a.y - b.y) / (b.x - a.x);
            double f =  Math.atan(x)*360/(2*Math.PI);
            if(b.x < a.x && !first  ) {left = true;first = true;} //二三象限要取余
           // else left = false;
            //System.out.println(a.x+" "+a.y +" "+b.x+" "+b.y);
            return (int)f;
        }
    }

}