package com.tencent.liteav.tuichorus.ui.floatwindow;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.tencent.liteav.basic.ImageLoader;
import com.tencent.liteav.tuichorus.R;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoom;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomCallback;
import com.tencent.liteav.tuichorus.ui.room.ChorusAudienceRoomEntity;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomAudienceActivity;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomInfoController;
import com.tencent.liteav.tuichorus.ui.widget.RoundCornerImageView;

import java.lang.reflect.Method;

public class FloatWindow implements IChorusFloatWindowCallback {
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
    private float   mTouchX;   //开始移动时的X坐标
    private float   mTouchY;   //开始移动时的Y坐标
    private float   mCurX;     //X坐标
    private float   mCurY;     //Y坐标
    private boolean mIsMove;

    private static FloatWindow sInstance;
    public static  boolean     mIsShowing       = false; //悬浮窗是否显示
    public static  boolean     mIsDestroyByself = false; //悬浮窗是否自己关闭

    public String mRoomUrl = "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover1.png";

    public static synchronized FloatWindow getInstance() {
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

    public void createDemoApplication(Context context, IChorusFloatWindowCallback callback) {
        try {
            Class clz = Class.forName("com.tencent.liteav.demo.DemoApplication");
            Method method = clz.getMethod("setChorusCallBack", IChorusFloatWindowCallback.class);
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
        mImgCover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEntity != null) {
                    ChorusRoomAudienceActivity.enterRoom(mContext, mEntity.roomId, mEntity.userId,
                            mEntity.audioQuality);
                }
            }
        });
        mImgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsDestroyByself = true;
                destroy();
            }
        });
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

    public void destroy() {
        destroy(null);
    }

    public void destroy(final TRTCChorusRoomCallback.ActionCallback callback) {
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
                    if (null != callback) {
                        callback.onCallback(code, msg);
                    }
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
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsMove = false;
                    mStartX = (int) event.getRawX(); //初始点相对屏幕左上角的坐标
                    mStartY = (int) event.getRawY();
                    mTouchX = (int) event.getRawX(); //该值在move的时候变化
                    mTouchY = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    mCurX = event.getRawX();
                    mCurY = event.getRawY();
                    //更新悬浮窗口位置(跟手功能)
                    mLayoutParams.x += mCurX - mTouchX;
                    mLayoutParams.y += mCurY - mTouchY;
                    mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                    //更新坐标
                    mTouchX = mCurX;
                    mTouchY = mCurY;
                    if (Math.abs(mCurX - mStartX) >= 5 || Math.abs(mCurY - mStartY) >= 5) {
                        mIsMove = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    mCurX = event.getRawX();
                    mCurY = event.getRawY();
                    //若位置变动超过5,则认为有滑动,调用贴边动画
                    if ((Math.abs(mCurX - mStartX) >= 5 || Math.abs(mCurY - mStartY) >= 5) && mIsMove) {
                        startScroll();
                    }
                    break;
                default:
                    break;
            }
            return mIsMove;
        }
    }

    //悬浮窗贴边动画,只移动到左边
    public void startScroll() {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(mCurX, 0).setDuration(300);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLayoutParams.x = (int) (mCurX * (1 - animation.getAnimatedFraction()));
                //防止悬浮窗上下越界
                calculateHeight();
                mWindowManager.updateViewLayout(mRootView, mLayoutParams);
            }
        });
        valueAnimator.start();
    }

    //计算高度,防止悬浮窗上下越界
    private void calculateHeight() {
        int height = mRootView.getHeight();
        int screenHeight = mWindowManager.getDefaultDisplay().getHeight();
        //获取系统状态栏的高度
        int resourceId = mContext.getResources().getIdentifier("status_bar_height",
                "dimen", "android");
        int statusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        if (mLayoutParams.y < 0) {
            mLayoutParams.y = 0;
        } else if (mLayoutParams.y > (screenHeight - height - statusBarHeight)) {
            mLayoutParams.y = screenHeight - height - statusBarHeight;
        }
    }

    public void setRoomInfo(ChorusAudienceRoomEntity entity) {
        mEntity = entity;
    }
}
