package com.cylan.jiafeigou.n.mvp.impl.mine;

import android.app.AlertDialog;
import android.content.Context;

import com.cylan.jiafeigou.n.mvp.contract.mine.MinePersonalInformationContract;
import com.cylan.jiafeigou.n.mvp.impl.AbstractPresenter;
import com.cylan.jiafeigou.n.mvp.model.UserInfoBean;
import com.cylan.jiafeigou.support.galleryfinal.FunctionConfig;



import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * 作者：zsl
 * 创建时间：2016/9/1
 * 描述：
 */
public class MinePersonalInformationPresenterImpl extends AbstractPresenter<MinePersonalInformationContract.View> implements MinePersonalInformationContract.Presenter {

    private Context context;
    public static FunctionConfig functionConfig;
    private Subscription getUserInfoSubscription;

    public MinePersonalInformationPresenterImpl(MinePersonalInformationContract.View view, Context context) {
        super(view);
        view.setPresenter(this);
        this.context = context;
    }

    @Override
    public void setPersonName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.show();
    }

    @Override
    public void bindPersonEmail() {
        getView().jump2SetEmailFragment();
    }

    @Override
    public void bindPersonPhone() {

    }

    @Override
    public void changePassword() {

    }

    @Override
    public void getUserInfomation(String url) {
        getUserInfoSubscription = Observable.just(url)
                .map(new Func1<String, UserInfoBean>() {
                    @Override
                    public UserInfoBean call(String s) {
                        return null;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<UserInfoBean>() {
                    @Override
                    public void call(UserInfoBean userInfoBean) {
                        getView().initPersonalInformation(userInfoBean);
                    }
                });
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        if (getUserInfoSubscription != null) {
            getUserInfoSubscription.unsubscribe();
        }
    }
}
