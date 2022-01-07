package com.tencent.liteav.tuichorus.ui.floatwindow;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.blankj.utilcode.util.ToastUtils;
import com.tencent.liteav.basic.ImageLoader;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoom;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomCallback;
import com.tencent.liteav.tuichorus.ui.room.ChorusAudienceRoomEntity;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomAudienceActivity;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomInfoController;
import com.tencent.liteav.tuichorus.ui.widget.RoundCornerImageView;

import java.lang.reflect.Method;

public class FloatWindow implements IFloatWindowCallback {
    private static final String TAG = "FloatWindow";

    private Context                    mContext;
    private ChorusAudienceRoomEntity   mEntity;
    private View                       mRootView;
    private RoundCornerImageView       mImgCover;
    private RoundCornerImageView       mImgClose;
    private WindowManager              mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private TRTCChorusRoom             mTUIChorus;
    private ChorusRoomInfoController   mRoomInfoController;

    private float   mStartX;   //最开始点击的X坐标
    private float   mStartY;   //最开始点击的Y坐标
    private float   mCurX;     //X坐标
    private float   mCurY;     //Y坐标
    private boolean mIsMove;
    private OnClick mOnClick;  //点击事件接口

    private static FloatWindow sInstance;
    public static  boolean     mIsShowing       = false; //悬浮窗是否显示
    public static  boolean     mIsDestroyByself = false; //悬浮窗是否自己关闭

    public String mRoomUrl = "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover1.png";

    /**
     * 设置悬浮窗监听事件
     */
    int mTag  = 0;//0：初始状态；1：非初始状态
    int mOldX = 0;//原X
    int mOldY = 0;//原Y

    public synchronized static FloatWindow getInstance() {
        if (sInstance == null) {
            sInstance = new FloatWindow();
        }
        return sInstance;
    }

    @Override
    public void onAppBackground(boolean isBackground) {
        Log.d(TAG, "onAppBackground: isBackground = " + isBackground);
        if (isBackground) {
            hide();
        } else {
            show();
        }
    }

    public void createDemoApplication(Context context, IFloatWindowCallback callback) {
        try {
            Class clz = Class.forName("com.tencent.liteav.demo.DemoApplication");
            Method method = clz.getMethod("setCallback", IFloatWindowCallback.class);
            Object obj = method.invoke(context, callback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init(Context context, ChorusRoomInfoController infoController) {
        mContext = context;
        initLayoutParams();
        initView();
        mIsShowing = false;
        mTUIChorus = TRTCChorusRoom.sharedInstance(context);
        mRoomInfoController = infoController;
        createDemoApplication(context, this);
    }

    public void showView(View view) {
        if (null != mWindowManager) {
            mWindowManager.addView(view, mLayoutParams);
        }
    }

    public void createView() {
        Log.d(TAG, "createView: mIsShowing = " + mIsShowing);
        if (!mIsShowing) {
            showView(mRootView);
            mIsShowing = true;
        }

    }

    public void show() {
        Log.d(TAG, "show: mIsShowing = " + mIsShowing);
        if (!mIsShowing && mRootView != null) {
            mRootView.setVisibility(View.VISIBLE);
            mIsShowing = true;
        }
    }

    public void hide() {
        Log.d(TAG, "hide: mIsShowing = " + mIsShowing);
        if (mIsShowing && mRootView != null) {
            mRootView.setVisibility(View.GONE);
            mIsShowing = false;
        }

    }

    public void initView() {
        mRootView = LayoutInflater.from(mContext).inflate(R.layout.tuichorus_floatview, null);
        mImgCover = mRootView.findViewById(R.id.iv_cover);
        mImgClose = mRootView.findViewById(R.id.iv_close);
        ImageLoader.loadImage(mContext, mImgCover, mRoomUrl, R.drawable.tuichorus_ic_cover);
        mImgCover.setOnTouchListener(new FloatingOnTouchListener());
        mImgClose.setOnTouchListener(new FloatingOnTouchListener());
    }

    private void initLayoutParams() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        mLayoutParams.format = PixelFormat.RGBA_8888;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mLayoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        //指定位置
        mLayoutParams.x = 0;
        mLayoutParams.y = mWindowManager.getDefaultDisplay().getHeight() / 2;
        //悬浮窗的宽高
        mLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.format = PixelFormat.TRANSPARENT;
    }

    public void setOnClick(OnClick onClick) {
        this.mOnClick = onClick;
    }

    //点击事件
    private void click(int i) {
        if (i == R.id.iv_cover) {
            if (mEntity != null) {
                ChorusRoomAudienceActivity.enterRoom(mContext, mEntity.roomId, mEntity.userId, mEntity.audioQuality);
            }
        } else if (i == R.id.iv_close) {
            mIsDestroyByself = true;
            destroy();
        }
    }

    public void destroy() {
        if (mWindowManager != null && mRootView != null) {
            Log.d(TAG, "destroy:  removeView ");
            mWindowManager.removeView(mRootView);
            mRootView = null;
            mWindowManager = null;
        }

        mEntity = null;
        mIsDestroyByself = false;
        ChorusRoomAudienceActivity.mLastEntity = null;
        mIsShowing = false;
        Log.d(TAG, "destroy: WindowManager");

        if (mTUIChorus != null) {
            mTUIChorus.exitRoom(new TRTCChorusRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    ToastUtils.showShort(mContext.getString(R.string.tuichorus_toast_exit_the_room_successfully));
                }
            });
        }
        if (mRoomInfoController != null && mRoomInfoController.getMusicServiceImpl() != null) {
            mRoomInfoController.getMusicServiceImpl().onExitRoom();
        }
        createDemoApplication(mContext, null);
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mCurX = mLayoutParams.x;
            mCurY = mLayoutParams.y;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsMove = false;
                    mOldX = (int) event.getRawX();
                    mOldY = (int) event.getRawY();
                    //获取初始位置
                    mStartX = (event.getRawX() - mLayoutParams.x);
                    mStartY = (event.getRawY() - mLayoutParams.y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    mCurX = event.getRawX();
                    mCurY = event.getRawY();
                    updateViewPosition();//更新悬浮窗口位置
                    if (Math.abs(mCurX - mOldX) <= 5 && Math.abs(mCurY - mOldY) <= 5) {
                    } else {
                        mIsMove = true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    mCurX = event.getRawX();
                    mCurY = event.getRawY();
                    //若位置变动不大,默认为点击
                    if (Math.abs(mCurX - mOldX) <= 5 && Math.abs(mCurY - mOldY) <= 5 && !mIsMove) {
                        click(v.getId());
                    }
                    move();
                    mOldX = (int) event.getRawX();
                    mOldY = (int) event.getRawY();
                    break;
            }
            return true;
        }
    }

    /**
     * 更新悬浮窗口位置
     */
    private void updateViewPosition() {
        mLayoutParams.x = (int) (mCurX - mStartX);
        mLayoutParams.y = (int) (mCurY - mStartY);
        if (mWindowManager != null) {
            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
        }
    }

    /**
     * 点击事件接口
     */
    public interface OnClick {
        void click(int type);
    }

    public void move() {
        if (mHandler == null || mWindowManager == null) {
            return;
        }

        for (int i = 0; i < mWindowManager.getDefaultDisplay().getWidth(); i++) {//一毫秒更新一次，直到达到边缘了
            mHandler.sendEmptyMessageDelayed(i, 300);
        }

        mWindowManager.updateViewLayout(mRootView, mLayoutParams);
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            moveToBeside();
        }
    };

    //滑动到左边
    private void moveToBeside() {
        if (!mIsShowing) {
            return;
        }
        if (mLayoutParams.x > 0) {
            mLayoutParams.x = mLayoutParams.x / 2;
            if (mLayoutParams.x < 10) {
                mLayoutParams.x = 0;
            }
        } else if (mLayoutParams.x < 0) {
            mLayoutParams.x++;
        }
        if (mWindowManager != null) {
            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
        }
    }

    public void setRoomInfo(ChorusAudienceRoomEntity entity) {
        mEntity = entity;
    }
}
