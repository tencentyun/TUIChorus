package com.tencent.liteav.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.ToastUtils;
import com.tencent.imsdk.v2.V2TIMGroupInfoResult;
import com.tencent.liteav.basic.UserModel;
import com.tencent.liteav.basic.UserModelManager;
import com.tencent.liteav.debug.GenerateTestUserSig;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoom;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomCallback;
import com.tencent.liteav.tuichorus.model.TRTCChorusRoomManager;
import com.tencent.liteav.tuichorus.ui.floatwindow.FloatWindow;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomAudienceActivity;
import com.tencent.liteav.tuichorus.ui.room.ChorusRoomCreateDialog;
import com.tencent.trtc.TRTCCloudDef;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private EditText       mEditRoomId;
    private TextView       mTextEnterRoom;
    private TRTCChorusRoom mTRTCChorusRoom;

    private static final String ROOM_COVER_ARRAY[] = {
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover1.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover2.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover3.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover4.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover5.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover6.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover7.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover8.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover9.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover10.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover11.png",
            "https://liteav-test-1252463788.cos.ap-guangzhou.myqcloud.com/voice_room/voice_room_cover12.png",
    };

    private Handler mWorkHandler = new Handler(Looper.getMainLooper());

    private static final String URL_PUSH     = "url_push";       // RTMP 推流地址
    private static final String URL_PLAY_FLV = "url_play_flv";   // FLV  播放地址

    private TextWatcher mEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!TextUtils.isEmpty(mEditRoomId.getText().toString())) {
                mTextEnterRoom.setEnabled(true);
            } else {
                mTextEnterRoom.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initStatusBar();
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initLocalData(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (FloatWindow.mIsShowing) {
            FloatWindow.getInstance().destroy();
        }
    }

    private void initView() {
        findViewById(R.id.btn_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://cloud.tencent.com/document/product/647/45667"));
                startActivity(intent);
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mEditRoomId = findViewById(R.id.et_room_id);
        mEditRoomId.addTextChangedListener(mEditTextWatcher);

        mTextEnterRoom = findViewById(R.id.tv_enter);
        mTextEnterRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterRoom(mEditRoomId.getText().toString());
            }
        });

        RelativeLayout buttonCreateRoom = findViewById(R.id.rl_create_room);
        buttonCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRoom();
            }
        });
    }

    private void initData() {
        final UserModel userModel = UserModelManager.getInstance().getUserModel();
        mTRTCChorusRoom = TRTCChorusRoom.sharedInstance(this);
        mTRTCChorusRoom.login(GenerateTestUserSig.SDKAPPID, userModel.userId, userModel.userSig, new TRTCChorusRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    mTRTCChorusRoom.setSelfProfile(userModel.userName, userModel.userAvatar, new TRTCChorusRoomCallback.ActionCallback() {
                        @Override
                        public void onCallback(int code, String msg) {
                            if (code == 0) {
                                Log.d(TAG, "setSelfProfile success");
                            }
                        }
                    });
                }
            }
        });
    }

    private void createRoom() {
        int                          index    = new Random().nextInt(ROOM_COVER_ARRAY.length);
        final String                 coverUrl = ROOM_COVER_ARRAY[index];
        final String                 userName = UserModelManager.getInstance().getUserModel().userName;
        final String                 userId   = UserModelManager.getInstance().getUserModel().userId;
        final ChorusRoomCreateDialog dialog   = new ChorusRoomCreateDialog(this);
        //获取推拉流地址
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(GenerateTestUserSig.URL_FETCH_PUSH_URL)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "get url failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject   jsonRsp          = new JSONObject(response.body().string());
                        final String pusherURLDefault = jsonRsp.optString(URL_PUSH);
                        final String flvPlayURL       = jsonRsp.optString(URL_PLAY_FLV);
                        mWorkHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                dialog.showRoomCreateDialog(userId, userName, coverUrl, TRTCCloudDef.TRTC_AUDIO_QUALITY_DEFAULT, true, pusherURLDefault, flvPlayURL);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "error code:" + response.code() + "msg:" + response.message());
                }
            }
        });
    }

    private void enterRoom(final String roomIdStr) {
        TRTCChorusRoomManager.getInstance().getGroupInfo(roomIdStr, new TRTCChorusRoomManager.GetGroupInfoCallback() {
            @Override
            public void onSuccess(V2TIMGroupInfoResult result) {
                if (isRoomExist(result)) {
                    realEnterRoom(roomIdStr);
                } else {
                    ToastUtils.showLong(R.string.room_not_exist);
                }
            }

            @Override
            public void onFailed(int code, String msg) {
                ToastUtils.showLong(msg);
            }
        });
    }

    private void realEnterRoom(String roomIdStr) {
        UserModel userModel = UserModelManager.getInstance().getUserModel();
        String    userId    = userModel.userId;
        int       roomId;
        try {
            roomId = Integer.parseInt(roomIdStr);
        } catch (Exception e) {
            roomId = 10000;
        }
        ChorusRoomAudienceActivity.enterRoom(this, roomId, userId, TRTCCloudDef.TRTC_AUDIO_QUALITY_DEFAULT);
    }

    private boolean isRoomExist(V2TIMGroupInfoResult result) {
        if (result == null) {
            Log.e(TAG, "room not exist result is null");
            return false;
        }
        return result.getResultCode() == 0;
    }

    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initLocalData(Context context) {
        copyAssetsToFile(context, "houlai_bz.mp3");
        copyAssetsToFile(context, "houlai_yc.mp3");

        copyAssetsToFile(context, "qfdy_yc.mp3");
        copyAssetsToFile(context, "qfdy_bz.mp3");

        copyAssetsToFile(context, "xq_bz.mp3");
        copyAssetsToFile(context, "xq_yc.mp3");

        copyAssetsToFile(context, "nuannuan_bz.mp3");
        copyAssetsToFile(context, "nuannuan_yc.mp3");

        copyAssetsToFile(context, "jda.mp3");
        copyAssetsToFile(context, "jda_bz.mp3");

        copyAssetsToFile(context, "houlai_lrc.vtt");
        copyAssetsToFile(context, "qfdy_lrc.vtt");
        copyAssetsToFile(context, "xq_lrc.vtt");
        copyAssetsToFile(context, "nuannuan_lrc.vtt");
        copyAssetsToFile(context, "jda_lrc.vtt");
    }

    public static void copyAssetsToFile(Context context, String name) {
        String savePath = ContextCompat.getExternalFilesDirs(context, null)[0].getAbsolutePath();
        String filename = savePath + "/" + name;
        File   dir      = new File(savePath);
        // 如果目录不存在，创建这个目录
        if (!dir.exists())
            dir.mkdir();
        try {
            if (!(new File(filename)).exists()) {
                InputStream      is     = context.getResources().getAssets().open(name);
                FileOutputStream fos    = new FileOutputStream(filename);
                byte[]           buffer = new byte[7168];
                int              count  = 0;
                while ((count = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
                fos.close();
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}