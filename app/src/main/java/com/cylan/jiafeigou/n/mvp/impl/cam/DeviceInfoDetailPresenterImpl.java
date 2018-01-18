package com.cylan.jiafeigou.n.mvp.impl.cam;

import android.text.TextUtils;

import com.cylan.entity.jniCall.JFGDPMsg;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.cache.db.module.Device;
import com.cylan.jiafeigou.dp.DataPoint;
import com.cylan.jiafeigou.dp.DpMsgDefine;
import com.cylan.jiafeigou.dp.DpMsgMap;
import com.cylan.jiafeigou.dp.DpUtils;
import com.cylan.jiafeigou.misc.JError;
import com.cylan.jiafeigou.misc.JResultEvent;
import com.cylan.jiafeigou.module.Command;
import com.cylan.jiafeigou.n.mvp.contract.cam.CamInfoContract;
import com.cylan.jiafeigou.n.mvp.impl.AbstractPresenter;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by cylan-hunt on 16-11-25.
 */

public class DeviceInfoDetailPresenterImpl extends AbstractPresenter<CamInfoContract.View>
        implements CamInfoContract.Presenter {

    private boolean isInitSd;

    public DeviceInfoDetailPresenterImpl(CamInfoContract.View view, String uuid) {
        super(view);
    }

    private void loadParameters() {
        try {
            if (DataSourceManager.getInstance().isOnline()) {
                DataSourceManager.getInstance().syncDeviceProperty(uuid);
//                checkNewVersion();
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void start() {
        super.start();
        loadParameters();
        robotDeviceDataSync();
        clearSdcardReqBack();
        onClearSdReqBack();
    }

    /**
     * robot同步数据
     *
     * @return
     */
    private void robotDeviceDataSync() {
        Subscription subscribe = RxBus.getCacheInstance().toObservable(RxEvent.DeviceSyncRsp.class)
                .filter((RxEvent.DeviceSyncRsp jfgRobotSyncData) -> (getView() != null && TextUtils.equals(uuid, jfgRobotSyncData.uuid)))
                .filter(j -> j.dpList != null)
                .flatMap(deviceSyncRsp -> Observable.from(deviceSyncRsp.dpList))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> AppLogger.e("err: " + throwable.getLocalizedMessage()))
                .subscribe(msg -> {
                    try {
                        mView.deviceUpdate(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, AppLogger::e);
        addStopSubscription(subscribe);
    }

    @Override
    public <T extends DataPoint> void updateInfoReq(T value, long id) {
        Observable.just(value)
                .subscribeOn(Schedulers.io())
                .subscribe((Object o) -> {
                    try {
                        DataSourceManager.getInstance().updateValue(uuid, value, (int) id);
                    } catch (IllegalAccessException e) {
                        AppLogger.e("err: " + e.getLocalizedMessage());
                    }
                }, (Throwable throwable) -> {
                    AppLogger.e(throwable.getLocalizedMessage());
                });
    }

    @Override
    public void clearSdcard() {
        rx.Observable.just(null)
                .subscribeOn(Schedulers.io())
                .subscribe((Object o) -> {
                    try {
                        ArrayList<JFGDPMsg> ipList = new ArrayList<JFGDPMsg>();
                        JFGDPMsg mesg = new JFGDPMsg(DpMsgMap.ID_218_DEVICE_FORMAT_SDCARD, 0);
                        mesg.packValue = DpUtils.pack(0);
                        ipList.add(mesg);
                        Command.getInstance().robotSetData(uuid, ipList);
                        isInitSd = true;
                    } catch (Exception e) {
                        AppLogger.e("format sd： " + e.getLocalizedMessage());
                    }
                }, AppLogger::e);
    }

    public void clearSdcardReqBack() {
        Subscription subscribe = RxBus.getCacheInstance().toObservable(RxEvent.DeviceSyncRsp.class)
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<RxEvent.DeviceSyncRsp, Observable<DpMsgDefine.DPSdStatus>>() {
                    @Override
                    public Observable<DpMsgDefine.DPSdStatus> call(RxEvent.DeviceSyncRsp rsp) {
                        if (rsp != null && rsp.dpList.size() > 0) {
                            for (JFGDPMsg dp : rsp.dpList) {
                                try {
                                    if (dp.id == 203 && TextUtils.equals(uuid, rsp.uuid)) {
                                        DpMsgDefine.DPSdStatus sdStatus = DpUtils.unpackData(dp.packValue, DpMsgDefine.DPSdStatus.class);
                                        return Observable.just(sdStatus);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    return Observable.just(null);
                                }
                            }
                        }
                        return Observable.just(null);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    if (o != null) {
                        //清空SD卡提示
                        if (isInitSd) {
                            getView().clearSdResult(0);
                            isInitSd = false;
                        }

                    }
                }, AppLogger::e);
        addStopSubscription(subscribe);
    }

    public void onClearSdReqBack() {
        Subscription subscribe = RxBus.getCacheInstance().toObservable(RxEvent.SetDataRsp.class)
                .subscribeOn(Schedulers.io())
                .filter(ret -> mView != null && TextUtils.equals(ret.uuid, uuid))
                .map(ret -> ret.rets)
                .flatMap(Observable::from)
                .filter(msg -> msg.id == 218)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.ret == 0) {

                    } else {
                        getView().clearSdResult(1);
                    }
                }, AppLogger::e);
        addStopSubscription(subscribe);
    }

    @Override
    public void updateAlias(Device device) {
        addSubscription(Observable.just(device)
                .map(device1 -> {
                    DataSourceManager.getInstance().updateDevice(device);
                    try {
                        Command.getInstance().setAliasByCid(device.uuid, device.alias);
                        AppLogger.d("update alias suc");
                    } catch (JfgException e) {
                        AppLogger.e("err: set up remote alias failed: " + new Gson().toJson(device));
                    }
                    return null;
                })
                .flatMap(dev -> RxBus.getCacheInstance().toObservable(RxEvent.SetAlias.class)
                        .filter(setAlias -> setAlias.result.event == JResultEvent.JFG_RESULT_SET_DEVICE_ALIAS
                                && setAlias.result.code == 0)
                        .first())
                .filter(s -> getView() != null)
                .first()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(setAlias -> getView().setAliasRsp(JError.ErrorOK),
                        throwable -> AppLogger.e("err: " + throwable.getLocalizedMessage())));
    }

    public void checkNewVersion() {
        Subscription subscribe = Observable.create((Observable.OnSubscribe<Long>) subscriber -> {
            Device device = DataSourceManager.getInstance().getDevice(uuid);
            final String currentVersion = device.$(207, "");
            AppLogger.d("current version: " + currentVersion);
            try {
                long seq = Command.getInstance().CheckTagDeviceVersion(uuid);
                subscriber.onNext(seq);
                subscriber.onCompleted();
            } catch (Exception e) {
                subscriber.onError(e);
                AppLogger.e("checkNewHardWare:" + e.getLocalizedMessage());
            }
        }).flatMap(seq -> RxBus.getCacheInstance().toObservable(RxEvent.VersionRsp.class))
                .first(rsp -> rsp != null && TextUtils.equals(rsp.uuid, uuid))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(versionRsp -> {
                    mView.onCheckDeviceVersionFinished(versionRsp.getVersion().getTagVersion());
                }, throwable -> {
                    AppLogger.e(throwable);
                    mView.onCheckDeviceVersionFinished(null);
                });
        addStopSubscription(subscribe);
    }
}
