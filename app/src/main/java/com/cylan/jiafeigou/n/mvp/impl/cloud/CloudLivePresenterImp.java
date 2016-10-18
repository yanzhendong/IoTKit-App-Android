package com.cylan.jiafeigou.n.mvp.impl.cloud;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.cylan.jiafeigou.ICloudLiveService;
import com.cylan.jiafeigou.n.db.CloudLiveDbUtil;
import com.cylan.jiafeigou.n.engine.CloudLiveService;
import com.cylan.jiafeigou.n.mvp.contract.cloud.CloudLiveContract;
import com.cylan.jiafeigou.n.mvp.impl.AbstractPresenter;
import com.cylan.jiafeigou.n.mvp.model.CloudLiveBaseBean;
import com.cylan.jiafeigou.n.mvp.model.CloudLiveBaseDbBean;

import com.cylan.jiafeigou.n.mvp.model.CloudLiveVideoTalkBean;
import com.cylan.jiafeigou.support.db.DbManager;
import com.cylan.jiafeigou.support.db.DbManagerImpl;
import com.cylan.jiafeigou.support.db.ex.DbException;
import com.cylan.jiafeigou.support.db.sqlite.SqlInfo;
import com.cylan.jiafeigou.support.db.sqlite.SqlInfoBuilder;
import com.sina.weibo.sdk.utils.LogUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.List;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 作者：zsl
 * 创建时间：2016/9/26
 * 描述：
 */
public class CloudLivePresenterImp extends AbstractPresenter<CloudLiveContract.View> implements CloudLiveContract.Presenter {

    private int val1;
    private int val2;
    private Subscription talkSub;
    private int BASE = 600;

    private MediaRecorder mMediaRecorder;
    private MediaPlayer mPlayer;
    public static final int MAX_LENGTH = 1000 * 60 * 10;// 最大录音时长;
    private File filePath;
    private long startTime;
    private long endTime;

    private String output_Path = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + System.currentTimeMillis()+"luyin.3gp";

    private DbManager base_db;

    private ICloudLiveService mService;
    private Subscription checkDeviceOnLineSub;
    private Subscription leaveMesgSub;

    public CloudLivePresenterImp(CloudLiveContract.View view) {
        super(view);
        view.setPresenter(this);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        if (talkSub != null) {
            talkSub.unsubscribe();
        }

        if (checkDeviceOnLineSub != null){
            checkDeviceOnLineSub.unsubscribe();
        }

        if (leaveMesgSub != null){
            leaveMesgSub.unsubscribe();
        }

        stopPlayRecord();
    }

    @Override
    public void startTalk() {
        talkSub = Observable.interval(500, 200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .map(new Func1<Long, Double>() {
                    @Override
                    public Double call(Long aLong) {
                        return null;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Double>() {
                    @Override
                    public void call(Double value) {
                        int val = updateMicStatus();
                        val1 = val + 12;
                        val2 = val;
                        if (getView() != null)
                            getView().refreshView(val1, val2);
                    }
                });
    }

    @Override
    public String startRecord() {

        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        try {
            filePath = new File(output_Path);
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);

            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            mMediaRecorder.setOutputFile(filePath.getAbsolutePath());
            mMediaRecorder.setMaxDuration(MAX_LENGTH);
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            startTime = System.currentTimeMillis();

        } catch (IllegalStateException e) {

        } catch (IOException e) {

        }

        return filePath.getAbsolutePath();
    }

    private int updateMicStatus() {
        int voiceVal = 0;
        if (mMediaRecorder != null) {
            double ratio = (double) mMediaRecorder.getMaxAmplitude() / BASE;
            double db = 0;// 分贝
            if (ratio > 1)
                db = 20 * Math.log10(ratio);
            voiceVal = (int) db;
        }
        return voiceVal;
    }

    @Override
    public void stopRecord() {
        if (mMediaRecorder == null)
            return;
        endTime = System.currentTimeMillis();
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
    }

    @Override
    public void playRecord(String mFileName) {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            LogUtil.d("cloud_live_play_record","prepare() failed"+e.getMessage());
        }
    }

    @Override
    public void stopPlayRecord() {
        if (mPlayer == null){
            return;
        }
        mPlayer.stop();
        mPlayer.reset();
        mPlayer.release();
        mPlayer = null;
    }

    @Override
    public boolean checkSDCard() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return true;
        else
            return false;
    }

    /**
     * desc:添加一个数据
     */
    @Override
    public void addMesgItem(CloudLiveBaseBean bean) {
        if (getView() != null)
            getView().refreshRecycleView(bean);
    }

    @Override
    public CloudLiveBaseBean creatMesgBean() {
        return new CloudLiveBaseBean();
    }

    @Override
    public String getLeaveMesgLength() {
        String timeLength = "";
        long time = (endTime - startTime) / 1000;
        if (time / 60 == 0) {
            timeLength = time + "''";
        } else {
            timeLength = time / 60 + "'" + time % 60 + "''";
        }
        return timeLength;
    }

    @Override
    public String parseTime(String times) {
        long timem = Long.parseLong(times);
        Date time = new Date(timem);
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd HH:mm");
        String dateString = formatter.format(time);
        return dateString;
    }

    /**
     * desc:创建数据库
     */
    @Override

    public void createDB() {
        base_db = CloudLiveDbUtil.getInstance().dbManager;;
    }

    @Override
    public byte[] getSerializedObject(Serializable s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(s);
        } catch (IOException e) {
            return null;
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
            }
        }
        byte[] result = baos.toByteArray();
        return result;
    }

    @Override
    public Object readSerializedObject(byte[] in) {
        Object result = null;
        ByteArrayInputStream bais = new ByteArrayInputStream(in);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bais);
            result = ois.readObject();
        } catch (Exception e) {
            result = null;
        } finally {
            try {
                ois.close();
            } catch (Throwable e) {
            }
        }
        return result;
    }

    @Override
    public void saveIntoDb(CloudLiveBaseDbBean bean) {
        try {
            base_db.save(bean);
        } catch (DbException e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<CloudLiveBaseDbBean> findFromAllDb() {
        List<CloudLiveBaseDbBean> allData = new ArrayList<>();
        try {
            allData = base_db.findAll(CloudLiveBaseDbBean.class);
        } catch (DbException e) {
            e.printStackTrace();
        }
        return allData;
    }

    @Override
    public void initService() {
        Intent serviceIntent = new Intent(getView().getContext(), CloudLiveService.class);
        getView().getContext().startService(serviceIntent);
        getView().getContext().bindService(serviceIntent,conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void refreshHangUpView() {
        try {
            if (mService.getHangUpFlag()){
                getView().hangUpRefreshView(mService.getHangUpResultData());
                mService.setHangUpFlag(false);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handlerVideoTalk() {
        getView().showReconnetProgress();
        checkDeviceOnLineSub = Observable.just(null)
                .map(new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object o) {
                        //TODO 检测设备是否离线；true离线、false在线
                        return true;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        getView().hideReconnetProgress();
                        getView().handlerVideoTalk(aBoolean);
                    }
                });
    }

    @Override
    public void handlerLeveaMesg(final Context context) {
        getView().showReconnetProgress();
        leaveMesgSub = Observable.just(null)
                .delay(1000,TimeUnit.MILLISECONDS)
                .map(new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object o) {
                        //TODO 检查设备是否在线
                        return true;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        getView().hideReconnetProgress();
                        getView().showVoiceTalkDialog(context,aBoolean);
                    }
                });
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ICloudLiveService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
}
