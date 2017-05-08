package com.cylan.jiafeigou.n.mvp.impl.cloud;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.cylan.jiafeigou.n.mvp.contract.cloud.CloudCorrelationDoorBellContract;
import com.cylan.jiafeigou.n.mvp.impl.AbstractPresenter;
import com.cylan.jiafeigou.n.mvp.model.BellBean;
import com.cylan.jiafeigou.n.view.adapter.RelationDoorBellAdapter;
import com.cylan.jiafeigou.n.view.adapter.UnRelationDoorBellAdapter;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.support.superadapter.internal.SuperViewHolder;
import com.cylan.jiafeigou.utils.PreferencesUtils;

import java.util.ArrayList;
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
 * 创建时间：2016/9/29
 * 描述：
 */
public class CloudCorrelationDoorBellPresenterImp extends AbstractPresenter<CloudCorrelationDoorBellContract.View> implements CloudCorrelationDoorBellContract.Presenter {

    private Subscription subscription;
    private Subscription unRelativeSub;
    public List<BellBean> unRelativieList;

    public int notifyFlag = 1;
    private Subscription refreshViewSub;
    private Subscription refreshViewUnSub;

    public CloudCorrelationDoorBellPresenterImp(CloudCorrelationDoorBellContract.View view) {
        super(view);
    }

    @Override
    public void start() {
        super.start();
        loadDoorBellData("");
        loadUnRelaiveDoorBellData("");
    }

    @Override
    public void stop() {
        super.stop();
        if (subscription != null) {
            subscription.unsubscribe();
        }

        if (unRelativeSub != null) {
            unRelativeSub.unsubscribe();
        }

        if (refreshViewSub != null) {
            refreshViewSub.unsubscribe();
        }

        if (refreshViewUnSub != null) {
            refreshViewUnSub.unsubscribe();
        }
    }

    @Override
    public void loadDoorBellData(String url) {

        subscription = Observable.just(url)
                .map(new Func1<String, List<BellBean>>() {
                    @Override
                    public List<BellBean> call(String url) {
                        //TODO 开启网络访问服务器
                        List<BellBean> list = new ArrayList<BellBean>();
                        list.addAll(TestData());
                        return list;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<BellBean>>() {
                    @Override
                    public void call(List<BellBean> bellInfoBeen) {
                        if (bellInfoBeen.size() == 0) {
                            getView().showNoRelativeDevicesView(notifyFlag);
                        }
                        getView().initRelativeRecycleView(bellInfoBeen);
                        getView().setOnRelaItemClickListener(new RelativeItemListener());
                    }
                }, e -> AppLogger.d(e.getMessage()));
    }

    @Override
    public void loadUnRelaiveDoorBellData(String url) {

        unRelativeSub = Observable.just(url)
                .map(new Func1<String, List<BellBean>>() {
                    @Override
                    public List<BellBean> call(String s) {
                        //TODO 开启网络访问服务器
                        List<BellBean> list = new ArrayList<BellBean>();
                        list.addAll(TestData());
                        return list;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<BellBean>>() {
                    @Override
                    public void call(List<BellBean> list) {
                        if (unRelativieList == null) {
                            unRelativieList = new ArrayList<BellBean>();
                        }
                        unRelativieList.addAll(list);
                        if (list.size() == 0) {
                            getView().showNoUnRelativeDevicesView(notifyFlag);
                        }
                        getView().initUnRelativeRecycleView(unRelativieList);
                        getView().setOnUnRelItemClickListener(new UnRelativeItemListener());
                    }
                }, e -> AppLogger.d(e.getMessage()));
    }

    /**
     * desc:测试数据
     */
    private List<BellBean> TestData() {
        List<BellBean> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            BellBean bean = new BellBean();
            bean.nickName = "门铃" + i;
            bean.ssid = "序列号" + i;
            list.add(bean);
        }
        return list;
    }

    /**
     * desc:关联按钮监听器
     */
    private class UnRelativeItemListener implements UnRelationDoorBellAdapter.OnRelativeClickListener {
        @Override
        public void relativeClick(final SuperViewHolder holder, final int viewType, final int layoutPosition, final BellBean item) {
            notifyFlag = 1;
            getView().showProgress();
            refreshViewSub = Observable.just(null)
                    .delay(3000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Object>() {
                        @Override
                        public void call(Object o) {
                            getView().hideProgress();
                            getView().notifyUnRelativeRecycle(holder, viewType, layoutPosition, item, notifyFlag);
                            getView().notifyRelativeRecycle(holder, viewType, layoutPosition, item, notifyFlag);
                        }
                    }, e -> AppLogger.d(e.getMessage()));

        }
    }

    /**
     * desc:取消关联按钮监听
     */
    private class RelativeItemListener implements RelationDoorBellAdapter.OnUnRelaItemClickListener {
        @Override
        public void unRelativeClick(final SuperViewHolder holder, final int viewType, final int layoutPosition, final BellBean item) {
            notifyFlag = 2;
            getView().showProgress();
            refreshViewUnSub = Observable.just(null)
                    .delay(3000, TimeUnit.MILLISECONDS)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Object>() {
                        @Override
                        public void call(Object o) {
                            getView().hideProgress();
                            getView().notifyRelativeRecycle(holder, viewType, layoutPosition, item, notifyFlag);
                            getView().notifyUnRelativeRecycle(holder, viewType, layoutPosition, item, notifyFlag);
                            if (PreferencesUtils.getBoolean("isFirstUnRelative", true)) {
                                showFirstUnRelDialog(item);
                            }
                        }
                    }, e -> AppLogger.d(e.getMessage()));
        }
    }

    /**
     * desc：第一次取消关联弹框提示
     */
    private void showFirstUnRelDialog(BellBean item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getView().getContext())
                .setMessage(item.nickName + "取消关联后，该中控设备将不再收到呼叫信息")
                .setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferencesUtils.putBoolean("isFirstUnRelative", false);
                        dialog.dismiss();
                    }
                });
        builder.show();
    }
}
