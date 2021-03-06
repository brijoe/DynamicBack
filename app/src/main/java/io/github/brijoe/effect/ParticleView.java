package io.github.brijoe.effect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import io.github.brijoe.effect.util.ConfigUtils;

/**
 * 承载动态效果的View extends SurfaceView
 * <p>
 * 支持代码调用和xml布局引入
 */
public class ParticleView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "ParticleView";

    private SurfaceHolder mSurfaceHolder;

    private static int mViewWidth;
    private static int mViewHeight;
    private volatile boolean mAllowDraw;

    private Canvas mCanvas;
    private ParticleDraw mParticleDraw = new ParticleDraw();
    private DrawThread mDrawThread = new DrawThread();
    private Paint mPaint = new Paint();

    public ParticleView(Context context) {
        this(context, null);
    }

    public ParticleView(Context context, AttributeSet attrs) {
        this(context, null, 0);
    }

    public ParticleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    public static int getViewWidth() {
        return mViewWidth;
    }

    public static int getViewHeight() {
        return mViewHeight;
    }


    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        this.setZOrderOnTop(true);
        mSurfaceHolder.addCallback(this);
        ParticleManager.getInstance().setParticleView(this);
        mDrawThread.start();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "surfaceCreated");
        if (!mAllowDraw) {
            return;
        }
        mDrawThread.startDraw();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed");
        mDrawThread.stopDraw();
    }


    public void start(@ParticleDraw.EffectType int particleType) {
        Log.e(TAG, "start- particleType=" + particleType);
        if (mParticleDraw != null) {
            mParticleDraw.init(particleType);
        }
        mAllowDraw = true;
        mDrawThread.startDraw();
    }

    public void stop() {
        Log.e(TAG, "stop操作");
        mAllowDraw = false;
        mDrawThread.stopDraw();
    }

    /**
     * 释放资源操作
     */
    public void release() {
        //退出绘制线程
        if (mDrawThread != null) {
            mDrawThread.quit();
        }
        //销毁特效View
        if (mParticleDraw != null) {
            mParticleDraw.destroy();
            mParticleDraw = null;
        }
        //
        mAllowDraw = false;
    }

    private long last_time = 0;

    //绘制
    private void drawOnCanvas() {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();
            if (mCanvas != null) {
                mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                //调用下层 特效View 绘制
                mParticleDraw.draw(mCanvas, mPaint);
            }
            if (mCanvas != null) {
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //绘制之前都要清空一下
    private void clearCanvas() {
        try {
            mCanvas = mSurfaceHolder.lockCanvas();
            if (mCanvas != null) {
                mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.e(TAG, "onAttachedToWindow: ");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.e(TAG, "onDetachedFromWindow");
        release();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
    }

    //绘制线程
    private class DrawThread extends HandlerThread {


        private final int DRAW_CANVAS = 0x01;

        private final int CLEAN_CANVAS = 0x02;

        private EffectHandler mEffectHandler;


        public DrawThread() {
            super("DrawThread");
        }


        public void start() {
            if (!isAlive()) {
                super.start();
            }
            if (mEffectHandler == null) {
                mEffectHandler = new EffectHandler(getLooper());
            }
        }

        public void startDraw() {
            Log.e(TAG, "startDraw: ");
            mEffectHandler.removeCallbacksAndMessages(null);
            mEffectHandler.sendEmptyMessageDelayed(DRAW_CANVAS,
                    ConfigUtils.getConfigRate());
        }

        public void stopDraw() {
            Log.e(TAG, "stopDraw: ");
            mEffectHandler.removeCallbacksAndMessages(null);
            mEffectHandler.sendEmptyMessage(CLEAN_CANVAS);
//            clearCanvas();
        }


        //粒子控制线程调用

        private class EffectHandler extends Handler {

            public EffectHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case DRAW_CANVAS:
                        //防止remove后还在执行
                        if (!mAllowDraw) {
                            break;
                        }
                        drawOnCanvas();
                        mEffectHandler.sendEmptyMessageDelayed(DRAW_CANVAS,
                                ConfigUtils.DRAW_FRAME_RATE);
                        break;
                    case CLEAN_CANVAS:
                        clearCanvas();
                        break;
                }
            }
        }
    }


}
