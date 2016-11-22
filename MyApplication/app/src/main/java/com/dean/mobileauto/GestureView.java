package com.dean.mobileauto;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;

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
    private Boolean over = false;// 表示已画完
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;
    public String cmd;
    private static int curr_angle = 90;


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
                int step_length = (graphics.size() > 100) ? graphics.size()/10 : 5;//更改步长

                if (over && step+step_length <= graphics.size()) {
                    step += step_length;
                    int turn = curr_angle - angle(graphics.get(step-step_length), graphics.get(step));
                    curr_angle = angle(graphics.get(step-step_length), graphics.get(step));

                    String cmd1 = String.format("\n转" + turn + "度");
                    String cmd2 = String.format("移动" + distance(graphics.get(step-step_length), graphics.get(step))+"毫米");
                    cmd = cmd + cmd1 + cmd2;

                    if (step + step_length >= graphics.size()) {
                        System.out.println(cmd);
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
                graphics.clear(); //清空点阵
                of = 0;
                step = 0;
                cmd = "";
                curr_angle = 90;
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
        z = Math.sqrt(z);
       // System.out.println("distance = "+(int)z);
        return (int) z;
    }

    private int angle(PointF a, PointF b){
        double x = (a.y - b.y) / (b.x - a.x);
        double f =  Math.atan(x)*360/(2*Math.PI);
        //System.out.println("angle = "+(int)f);
        return (int)f;
    }
}