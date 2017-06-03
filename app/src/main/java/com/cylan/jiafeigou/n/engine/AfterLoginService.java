package com.cylan.jiafeigou.n.engine;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;

import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.bind.UdpConstant;
import com.cylan.jiafeigou.misc.ver.ClientVersionChecker;
import com.cylan.jiafeigou.misc.ver.IVersion;
import com.cylan.jiafeigou.n.base.BaseApplication;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.utils.PreferencesUtils;
import com.google.gson.Gson;

import rx.Observable;
import rx.schedulers.Schedulers;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - setDevice intent actions, extra parameters and static
 * helper methods.
 * 登陆成功后，需要刷新一些缓存，数据，都在这里做。
 */
public class AfterLoginService extends IntentService {

    private static final String TAG = "KEY";
    /**
     * 保存账号密码，登陆成功后保存。
     */
    public static final String ACTION_SAVE_ACCOUNT = "action_save_account";
    public static final String ACTION_GET_ACCOUNT = "action_get_account";
    public static final String ACTION_SYN_OFFLINE_REQ = "action_offline_req";

    public static final String ACTION_CHECK_VERSION = "action_check_version";

    private static long clientVersionCheck = 0;

    public AfterLoginService() {
        super("AfterLoginService");
    }

    public static void startSaveAccountAction(Context context) {
        Intent intent = new Intent(context, AfterLoginService.class);
        intent.putExtra(TAG, ACTION_SAVE_ACCOUNT);
        context.startService(intent);
    }

    public static void startGetAccountAction(Context context) {
        Intent intent = new Intent(context, AfterLoginService.class);
        intent.putExtra(TAG, ACTION_GET_ACCOUNT);
        context.startService(intent);
    }

    /**
     * 恢复离线时候,加入请求队列的消息
     */
    public static void resumeOfflineRequest() {
        Intent intent = new Intent(ContextUtils.getContext(), AfterLoginService.class);
        intent.putExtra(TAG, ACTION_SYN_OFFLINE_REQ);
        ContextUtils.getContext().startService(intent);
    }

    /**
     * 恢复离线时候,加入请求队列的消息
     */
    public static void resumeTryCheckVersion() {
        if (clientVersionCheck == 0 || System.currentTimeMillis() - clientVersionCheck > 5 * 60 * 1000L) {
            clientVersionCheck = System.currentTimeMillis();
        } else return;
        Intent intent = new Intent(ContextUtils.getContext(), AfterLoginService.class);
        intent.putExtra(TAG, ACTION_CHECK_VERSION);
        ContextUtils.getContext().startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getStringExtra(TAG);
            AppLogger.i("AfterLoginService: " + action + ",looper: " + (Looper.myLooper() == Looper.getMainLooper()));
            if (TextUtils.equals(action, ACTION_SAVE_ACCOUNT)) {
            } else if (TextUtils.equals(action, ACTION_GET_ACCOUNT)) {
                BaseApplication.getAppComponent().getCmd().getAccount();
            } else if (TextUtils.equals(action, ACTION_SYN_OFFLINE_REQ)) {
                Observable.just("go and do something")
                        .subscribeOn(Schedulers.newThread())
                        .subscribe(s -> {
                            try {
                                String content = PreferencesUtils.getString(JConstant.BINDING_DEVICE);
                                UdpConstant.UdpDevicePortrait portrait = new Gson().fromJson(content, UdpConstant.UdpDevicePortrait.class);
                                if (portrait != null) {
                                    BaseApplication.getAppComponent().getCmd().bindDevice(portrait.uuid, portrait.bindCode, portrait.mac, portrait.bindFlag);
                                    //设备上线后,需要设置时区.
                                }
                            } catch (Exception e) {
                                AppLogger.d("err: " + e.getLocalizedMessage());
                            }
                        }, throwable -> AppLogger.e("err: " + throwable.getLocalizedMessage()));
            } else if (TextUtils.equals(action, ACTION_CHECK_VERSION)) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
                AppLogger.d("尝试检查版本");
                IVersion<ClientVersionChecker.CVersion> version = new ClientVersionChecker();
                version.startCheck();
            }
        }
    }

}