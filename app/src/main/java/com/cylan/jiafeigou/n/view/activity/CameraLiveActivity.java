package com.cylan.jiafeigou.n.view.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.module.DataSourceManager;
import com.cylan.jiafeigou.cache.db.module.Device;
import com.cylan.jiafeigou.misc.JConstant;
import com.cylan.jiafeigou.misc.JFGRules;
import com.cylan.jiafeigou.misc.ver.AbstractVersion;
import com.cylan.jiafeigou.n.BaseFullScreenFragmentActivity;
import com.cylan.jiafeigou.n.base.BaseApplication;
import com.cylan.jiafeigou.n.view.cam.CamMessageListFragment;
import com.cylan.jiafeigou.n.view.cam.CameraLiveFragmentEx;
import com.cylan.jiafeigou.rx.RxBus;
import com.cylan.jiafeigou.rx.RxEvent;
import com.cylan.jiafeigou.support.badge.Badge;
import com.cylan.jiafeigou.support.badge.TreeNode;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.BindUtils;
import com.cylan.jiafeigou.utils.ContextUtils;
import com.cylan.jiafeigou.utils.MiscUtils;
import com.cylan.jiafeigou.utils.PreferencesUtils;
import com.cylan.jiafeigou.widget.CustomToolbar;
import com.cylan.jiafeigou.widget.CustomViewPager;
import com.cylan.jiafeigou.widget.HintTextView;
import com.cylan.jiafeigou.widget.ImageViewTip;
import com.cylan.jiafeigou.widget.indicator.PagerSlidingTabStrip;
import com.google.gson.Gson;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.cylan.jiafeigou.support.photoselect.helpers.Constants.REQUEST_CODE;

@Badge(parentTag = "NewHomeActivity")
public class CameraLiveActivity extends BaseFullScreenFragmentActivity {

    @BindView(R.id.vp_camera_live)
    CustomViewPager vpCameraLive;
    @BindView(R.id.custom_toolbar)
    CustomToolbar customToolbar;

    private ImageViewTip imgVCameraTitleTopSetting;
    private PagerSlidingTabStrip vIndicator;

    private Device device;
    private String uuid;
    private Subscription newMsgSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_live);
        ButterKnife.bind(this);
        this.uuid = getIntent().getStringExtra(JConstant.KEY_DEVICE_ITEM_UUID);
        if (TextUtils.isEmpty(uuid)) {
            AppLogger.e("what the hell uuid is null");
            finishExt();
        }
        device = BaseApplication.getAppComponent().getSourceManager().getDevice(uuid);
        boolean hasNewMsg = getIntent().hasExtra(JConstant.KEY_JUMP_TO_MESSAGE);
        initToolbar(hasNewMsg);
        initAdapter();
        if (hasNewMsg && vpCameraLive.getAdapter().getCount() > 1) {
            //跳转到
            vpCameraLive.setCurrentItem(1);
            removeHint();
        }
        JConstant.KEY_CURRENT_PLAY_VIEW = this.getClass().getName();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // TODO: 2017/8/18 需要手动通知 CameraLiveFragment 调用 stop  避免 onStop 延迟调用 bug #118078
        final String tag = MiscUtils.makeFragmentName(vpCameraLive.getId(), 0);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment instanceof CameraLiveFragmentEx) {
            ((CameraLiveFragmentEx) fragment).onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AppLogger.d("onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        AppLogger.d("onRestoreInstanceState");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (MiscUtils.isLand()) {
            handleSystemBar(false, 1);
        }
        makeNewMsgSub();
    }

    private void makeNewMsgSub() {
        if (newMsgSub != null) newMsgSub.unsubscribe();
        newMsgSub = RxBus.getCacheInstance().toObservable(RxEvent.DeviceSyncRsp.class)
                .subscribeOn(Schedulers.io())
                .filter(ret -> TextUtils.equals(ret.uuid, device.uuid) && vpCameraLive.getCurrentItem() == 0)
                .flatMap(ret -> Observable.from(ret.dpList))
                .filter(ret -> filterNewMsgId(ret.id))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ret -> {
                    View vHint = vIndicator.findViewById(getString(R.string.Tap1_Camera_Messages).hashCode());
                    if (vHint != null && vHint instanceof HintTextView) {
                        ((HintTextView) vHint).showHint(true);
                    }
                }, AppLogger::e);
    }

    private boolean filterNewMsgId(long id) {
        if (JFGRules.isCamera(device.pid))
            return id == 505 || id == 222 || id == 512;
        if (JFGRules.isBell(device.pid))
            return id == 401;
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final boolean isLandScape = this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        getWindow().getDecorView().post(() -> {
            if (isLandScape) {
                handleSystemBar(false, 1);
            } else {
                showSystemUI();
            }
            customToolbar.setVisibility(isLandScape ? View.GONE : View.VISIBLE);
            vpCameraLive.setPagingEnabled(!isLandScape);
        });
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            Log.d("show", "show: " + visibility);
            if (visibility == 0 && MiscUtils.isLand())
                handleSystemBar(false, 100);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.uuid = getIntent().getStringExtra(JConstant.KEY_DEVICE_ITEM_UUID);
        Log.d("onNewIntent", "onNewIntent:" + uuid);
//        if (TextUtils.isEmpty(uuid)) {
//            AppLogger.e("what the hell uuid is null");
//            finishExt();
//        }
//        initToolbar(getIntent().hasExtra(JConstant.KEY_JUMP_TO_MESSAGE));
//        initAdapter();
        finish();
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Device device = DataSourceManager.getInstance().getDevice(uuid);
        if (device == null || !device.available()) {
            finish();//设备已经不存在了
        } else {
            this.device = device;
            updateRedHint();
        }
    }

    private void updateRedHint() {
        if (imgVCameraTitleTopSetting != null) {


//            if (JFGRules.hasProtection(device.pid)) {
//                //说明当前设备支持安全防护
//
//            }
//            if (JFGRules.hasHistory(device.pid)) {
//                //说明当前设备支持自动录像
//
//            }
            // TODO: 2017/7/12 因为当前 TreeHelper 把当前类当做了父类,因此需要先特殊处理,以后再完善,共享账号
            if (this.device != null && this.device.available() && TextUtils.isEmpty(this.device.shareAccount)) {
                TreeNode node = BaseApplication.getAppComponent().getTreeHelper().findTreeNodeByName(this.getClass().getSimpleName());
                boolean result = hasNewFirmware();
                boolean newNode = false;
                if (JFGRules.hasHistory(this.device.pid, false) && JFGRules.hasProtection(this.device.pid, false)) {
                    newNode = node != null && node.getTraversalCount() > 0;
                }
                //延时摄影，暂时隐藏。
                imgVCameraTitleTopSetting.setShowDot(result || newNode);
            } else {
                imgVCameraTitleTopSetting.setShowDot(false);//说明是共享账号,则不需要弹出 showDot
            }
        }
    }

    /**
     * true 有新版本
     *
     * @return
     */

    private boolean hasNewFirmware() {
        try {
            final String content = PreferencesUtils.getString(JConstant.KEY_FIRMWARE_CONTENT + getUuid());
            AbstractVersion.BinVersion version = new Gson().fromJson(content, AbstractVersion.BinVersion.class);
            final String newVersion = version.getTagVersion();
            final String currentVersion = BaseApplication.getAppComponent().getSourceManager().getDevice(getUuid()).$(207, "");
            if (BindUtils.versionCompare(currentVersion, newVersion) < 0) {
                PreferencesUtils.remove(JConstant.KEY_FIRMWARE_CONTENT + getUuid());
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (newMsgSub != null) newMsgSub.unsubscribe();
    }

    /**
     * 夸fragment传值
     *
     * @param bundle
     */
    public void addPutBundle(Bundle bundle) {
        final String tag = MiscUtils.makeFragmentName(vpCameraLive.getId(), 0);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment instanceof CameraLiveFragmentEx) {
            fragment.getArguments().putAll(bundle);
            vpCameraLive.setCurrentItem(0);
        }
    }

    private void initAdapter() {
        if (vpCameraLive.getAdapter() == null) {
            SimpleAdapterPager simpleAdapterPager = new SimpleAdapterPager(getSupportFragmentManager(), uuid);
            vpCameraLive.setAdapter(simpleAdapterPager);
        }
        final String tag = MiscUtils.makeFragmentName(vpCameraLive.getId(), 0);
        vpCameraLive.setPagingScrollListener(event -> {
            //消息页面,不需要拦截
            if (vpCameraLive.getCurrentItem() == 1) return true;
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment != null && fragment instanceof CameraLiveFragmentEx) {
                Rect rect = ((CameraLiveFragmentEx) fragment).mLiveViewRectInWindow;
                //true:不在区域内，
                boolean contains = !rect.contains((int) event.getRawX(), (int) event.getY());
                Log.d("contains", "contains:" + contains);
                return contains;
            } else
                return true;
        });
    }

    private void initToolbar(final boolean newMsg) {
        customToolbar.post(() -> {
            vIndicator = (PagerSlidingTabStrip) customToolbar.findViewById(R.id.v_indicator);
            vIndicator.setViewPager(vpCameraLive);
            vIndicator.setOnPageChangeListener(new SimplePageListener(uuid));
            imgVCameraTitleTopSetting = (ImageViewTip) customToolbar.findViewById(R.id.imgV_camera_title_top_setting);
            updateRedHint();
            customToolbar.findViewById(R.id.imgV_nav_back).setOnClickListener(v -> onNavBack());
        });
    }

    private void removeHint() {
        try {
            BaseApplication.getAppComponent().getSourceManager().clearValue(uuid, 1001, 1002, 1003, 1004, 1005);
            if (vIndicator == null) return;
            View vHint = vIndicator.findViewById(getString(R.string.Tap1_Camera_Messages).hashCode());
            if (vHint != null && vHint instanceof HintTextView) {
                ((HintTextView) vHint).showHint(false);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d("onBackPressed", this.getClass().getSimpleName());
        if (checkExtraChildFragment()) {
            return;
        } else if (checkExtraFragment())
            return;

        // TODO: 2017/8/18 需要手动通知 CameraLiveFragment 调用 stop  避免 onStop 延迟调用 bug #118078
        final String tag = MiscUtils.makeFragmentName(vpCameraLive.getId(), 0);
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment != null && fragment instanceof CameraLiveFragmentEx) {
            ((CameraLiveFragmentEx) fragment).onBackPressed();
            ((CameraLiveFragmentEx) fragment).removeVideoView();
        }
        if (this.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            finishExt();
        }
    }

    public void onNavBack() {
        onBackPressed();
    }

    /**
     * 当点击右上角的螺母按钮时，跳转到设备信息页面
     */
    @OnClick(R.id.imgV_camera_title_top_setting)
    public void onClickSetting() {
        Log.d("onClickSetting", "onClickSetting: " + getIntent().getStringExtra(JConstant.KEY_DEVICE_ITEM_UUID));
        Log.d("onClickSetting", "onClickSetting  uuid: " + uuid);
        Intent intent = new Intent(this, CamSettingActivity.class);
        intent.putExtra(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
        startActivity(intent, ActivityOptionsCompat.makeCustomAnimation(getApplicationContext(),
                R.anim.slide_in_right, R.anim.slide_out_left).toBundle());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    finishExt();
                }
                break;
        }
    }

    private class SimplePageListener implements ViewPager.OnPageChangeListener {
        private String uuid;

        private SimplePageListener(String uuid) {
            this.uuid = uuid;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            if (position == 1) {
                removeHint();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }


    class SimpleAdapterPager extends FragmentPagerAdapter {

        private String uuid;
        private boolean justJump = false;


        public SimpleAdapterPager(FragmentManager fm, String uuid) {
            super(fm);
            this.uuid = uuid;
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putString(JConstant.KEY_DEVICE_ITEM_UUID, uuid);
            if (position == 0) {
                return CameraLiveFragmentEx.newInstance(bundle);
            } else {
                return CamMessageListFragment.newInstance(bundle);
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }

        @Override
        public int getCount() {
            String shareAccount = device == null ? "" : device.shareAccount;
            return !TextUtils.isEmpty(shareAccount) && device != null && JFGRules.isCamera(device.pid) ? 1 : 2;//共享门铃需要显示消息吗,115833
        }


        @Override
        public CharSequence getPageTitle(int position) {
            return position == 0 ? ContextUtils.getContext().getString(R.string.Tap1_Camera_Video) : ContextUtils.getContext().getString(R.string.Tap1_Camera_Messages);
        }

        public void justJump(boolean jumpToMessage) {
            this.justJump = jumpToMessage;
        }
    }

}