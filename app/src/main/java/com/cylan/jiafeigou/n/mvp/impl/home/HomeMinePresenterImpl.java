package com.cylan.jiafeigou.n.mvp.impl.home;

import android.text.TextUtils;

import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.cylan.entity.jniCall.JFGAccount;
import com.cylan.ex.JfgException;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.module.Command;
import com.cylan.jiafeigou.module.GlideApp;
import com.cylan.jiafeigou.module.LoginHelper;
import com.cylan.jiafeigou.n.mvp.contract.home.HomeMineContract;
import com.cylan.jiafeigou.n.mvp.impl.AbstractFragmentPresenter;
import com.cylan.jiafeigou.n.task.FetchFeedbackTask;
import com.cylan.jiafeigou.n.task.FetchFriendsTask;
import com.cylan.jiafeigou.n.task.SysUnreadCountTask;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.PreferencesUtils;

import java.io.File;
import java.util.Random;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by hunt on 16-5-23.
 */
public class HomeMinePresenterImpl extends AbstractFragmentPresenter<HomeMineContract.View> implements HomeMineContract.Presenter {

    private boolean isOpenLogin = false;

    public HomeMinePresenterImpl(HomeMineContract.View view) {
        super(view);
    }

    @Override
    public void start() {
        super.start();
        addSubscription(getAccountBack(), "HomeMinePresenterImpl.getAccountBack");
        addSubscription(loginInMe(), "HomeMinePresenterImpl.loginInMe");
    }

    @Override
    public void stop() {
        super.stop();
    }


    @Override
    public String createRandomName() {
        String[] firtPart = {"a", "isFriend", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l"
                , "m", "n", "o", "p", "q", "r", "account", "t", "u", "v", "w", "x", "y", "z"};
        Random random = new Random();
        int randNum1 = random.nextInt(10);
        int randNum2 = random.nextInt(10);

        if (randNum1 == randNum2) {
            randNum2 /= 2;
        }

        int randNum3 = random.nextInt(10);
        if ((randNum1 == randNum3) || (randNum2 == randNum3)) {
            randNum3 /= 2;
        }

        int a = random.nextInt(26);
        int b = random.nextInt(26);
        if (b == a) {
            b /= 2;
        }
        int c = random.nextInt(26);
        if ((a == c) || (b == c)) {
            c /= 2;
        }
        String result = firtPart[a] + firtPart[b] + firtPart[c]
                + randNum1 + randNum2 + randNum3;
        return result;
    }

    /**
     * 获取到用户信息
     *
     * @return
     */
    @Override
    public JFGAccount getUserInfoBean() {
        return DataSourceManager.getInstance().getJFGAccount();
    }

    /**
     * 判断是否是三方的登录
     *
     * @return
     */
    @Override
    public boolean checkOpenLogIn() {
        return isOpenLogin;
    }

    /**
     * 获取到未读消息数
     */
    @Override
    public void fetchNewInfo() {
        //未读数
        //亲友列表
        //用户反馈
        Observable.just(new FetchFeedbackTask(),
                new FetchFriendsTask(),
                new SysUnreadCountTask())
                .subscribeOn(Schedulers.io())
                .subscribe(objectAction1 -> objectAction1.call(""), AppLogger::e);
    }


    private boolean isDefaultPhoto(String photoUrl) {
        return TextUtils.isEmpty(photoUrl) || photoUrl.contains("image/default.jpg");
    }

    @Override
    public Subscription getAccountBack() {
        return RxBus.getCacheInstance().toObservableSticky(RxEvent.AccountArrived.class)
                .observeOn(Schedulers.io())
                .map(accountArrived -> {
                    AppLogger.w("监听到用户信息回调!");
                    isOpenLogin = LoginHelper.getLoginType() >= 3;
                    if (isOpenLogin) {
                        String photoUrl = isDefaultPhoto(accountArrived.account.getPhotoUrl()) ? PreferencesUtils.getString(JConstant.OPEN_LOGIN_USER_ICON) : null;
                        if (!TextUtils.isEmpty(photoUrl)) {//设置第三方登录图像
                            uploadOpenLoginIcon(accountArrived, photoUrl);
                        }
                    }
                    return accountArrived;

                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(accountArrived -> {
                    if (getView() != null && !TextUtils.isEmpty(accountArrived.account.getPhotoUrl())) {
                        getView().setUserImageHeadByUrl(accountArrived.account.getPhotoUrl());
                    }
                    if (getView() != null) {
                        String al = accountArrived.account.getAlias();
                        String a2 = accountArrived.account.getAccount();
                        getView().setAliasName(TextUtils.isEmpty(al) ? a2 : al);
                    }
                }, e -> {
                    AppLogger.e(e.getMessage());
                });
    }

    private void uploadOpenLoginIcon(RxEvent.AccountArrived accountArrived, String photoUrl) {
        GlideApp.with(getView().getContext())
                .downloadOnly()
                .load(photoUrl)
                .into(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource, Transition<? super File> transition) {
                        try {
                            String alias = TextUtils.isEmpty(accountArrived.account.getAlias()) ? PreferencesUtils.getString(JConstant.OPEN_LOGIN_USER_ALIAS) : accountArrived.account.getAlias();
                            if (TextUtils.isEmpty(alias)) {
                                boolean isEmail = JConstant.EMAIL_REG.matcher(accountArrived.jfgAccount.getAccount()).find();
                                if (isEmail) {
                                    String[] split = accountArrived.jfgAccount.getAccount().split("@");
                                    alias = split[0];
                                }
                            }
                            Command.getInstance().updateAccountPortrait(resource.getAbsolutePath());
                            AppLogger.d("正在设置第三方登录图像" + resource.getAbsolutePath());
                            if (!TextUtils.isEmpty(alias) && TextUtils.isEmpty(accountArrived.account.getAlias())) {//设置第三方登录昵称
                                accountArrived.jfgAccount.setAlias(alias);
                                try {
                                    AppLogger.d("正在设置第三方登录昵称" + alias);
                                    accountArrived.jfgAccount.resetFlag();
                                    accountArrived.jfgAccount.setPhoto(true);
                                    Command.getInstance().setAccount(accountArrived.jfgAccount);
                                } catch (JfgException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }


    @Override
    public void loginType() {
        int loginType = LoginHelper.getLoginType();
        if (getView() != null) {
            if (loginType == 3 || loginType == 4) {
                getView().jump2SetPhoneFragment();
            } else if (loginType == 6 || loginType == 7) {
                getView().jump2BindMailFragment();
            }
        }
    }

    public Subscription loginInMe() {
        return RxBus.getCacheInstance().toObservable(RxEvent.LoginMeTab.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(loginMeTab -> {
                    if (loginMeTab.b) {
                        start();
                    }
                }, AppLogger::e);
    }

}
