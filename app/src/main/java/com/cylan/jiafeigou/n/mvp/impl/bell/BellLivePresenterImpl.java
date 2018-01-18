package com.cylan.jiafeigou.n.mvp.impl.bell;

import android.graphics.Bitmap;
import android.media.MediaScannerConnection;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.view.JFGSourceManager;
import com.cylan.jiafeigou.base.wrapper.BaseCallablePresenter;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.module.Command;
import com.cylan.jiafeigou.module.DoorLockHelper;
import com.cylan.jiafeigou.n.mvp.contract.bell.BellLiveContract;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.BitmapUtils;
import com.cylan.utils.JfgUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by cylan-hunt on 16-8-10.
 */

public class BellLivePresenterImpl extends BaseCallablePresenter<BellLiveContract.View> implements
        BellLiveContract.Presenter {
    @Inject
    JFGSourceManager sourceManager;

    @Inject
    public BellLivePresenterImpl(BellLiveContract.View view) {
        super(view);
    }

    @Override
    protected boolean disconnectBeforePlay() {
        return true;
    }

    @Override
    public void capture() {

        Observable.just("capture")
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .map(cmd -> {
                    byte[] screenshot = appCmd.screenshot(false);
                    if (screenshot != null) {
                        int w = Command.videoWidth;
                        int h = Command.videoHeight;
                        Bitmap bitmap = JfgUtils.byte2bitmap(w, h, screenshot);
                        AndroidSchedulers.mainThread().createWorker().schedule(() -> mView.onTakeSnapShotSuccess(bitmap));
                        String fileName = uuid + System.currentTimeMillis() + ".png";
                        String filePath = JConstant.MEDIA_PATH + File.separator + fileName;
//                        String fileName = System.currentTimeMillis() + ".png";
                        BitmapUtils.saveBitmap2file(bitmap, filePath);
//                        MiscUtils.insertImage(JConstant.MEDIA_PATH, fileName);

                        MediaScannerConnection.scanFile(mView.activity(), new String[]{filePath}, null, null);
                        AppLogger.e("截图文件地址:" + filePath);
                        // 先不分享到 每日精彩

//                        DpMsgDefine.DPWonderItem item = new DpMsgDefine.DPWonderItem();
//                        item.msgType = DpMsgDefine.DPWonderItem.TYPE_PIC;
//                        item.cid = uuid;
//                        Device device = sourceManager.getDevice(uuid);
//                        item.place = TextUtils.isEmpty(device.alias) ? device.uuid : device.alias;
//                        long time = System.currentTimeMillis();
//                        item.fileName = time / 1000 + ".jpg";
//                        item.time = (int) (time / 1000);
//                        IDPEntity entity = new DPEntity()
//                                .setUuid(uuid)
//                                .setMsgId(DpMsgMap.ID_602_ACCOUNT_WONDERFUL_MSG)
//                                .setVersion(System.currentTimeMillis())
//                                .setAccount(sourceManager.getAccount().getAccount())
//                                .setAction(DBAction.SHARED)
//                                .setOption(new DBOption.SingleSharedOption(1, 1, filePath))
//                                .setBytes(item.toBytes());
                        return null;
                    } else {
                        AndroidSchedulers.mainThread().createWorker().schedule(() -> mView.onTakeSnapShotFailed());
                        return null;
                    }

                })
                .filter(ret -> ret != null)
//                .flatMap(entity -> mTaskDispatcher.perform(entity))
                .subscribe(result -> {
                }, e -> AppLogger.d(e.getMessage()));
    }

    @Override
    public void openDoorLock(String password) {
        if (!liveStreamAction.hasStarted) {
            startViewer();
        }
        AppLogger.i("pwd: " + password);
        Subscription subscribe = DoorLockHelper.INSTANCE.openDoor(uuid, password)
                .timeout(10, TimeUnit.SECONDS, Observable.just(null))
                .observeOn(AndroidSchedulers.mainThread())
                .compose(applyLoading(false, R.string.DOOR_OPENING))
                .subscribe(success -> {
                    if (success == null) {
                        mView.onOpenDoorLockTimeOut();
                        AppLogger.e("开门超时了");
                    } else if (success == 0) {
                        mView.onOpenDoorLockSuccess();
                        AppLogger.i("开门成功了");
                    } else if (success == 2) {
                        mView.onOpenDoorLockPasswordError();
                        AppLogger.i("开门密码错误");
                    } else {
                        mView.onOpenDoorLockFailure();
                        AppLogger.i("开门失败了");
                    }
                }, e -> {
                    e.printStackTrace();
                    AppLogger.e(e);
                });
        addDestroySubscription(subscribe);
    }
}
