package com.cylan.jiafeigou.n.view.panorama;

import android.content.Intent;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.base.injector.component.ActivityComponent;
import com.cylan.jiafeigou.base.wrapper.BaseActivity;

/**
 * Created by yanzhendong on 2017/8/11.
 */

public class ConnectionActivity extends BaseActivity {
    @Override
    protected void setActivityComponent(ActivityComponent activityComponent) {

    }

    @Override
    protected int getContentViewID() {
        return R.layout.activity_device_connection;
    }

    @Override
    protected void initViewAndListener() {
        super.initViewAndListener();
        Intent intent = getIntent();
        if ("offline".equals(intent.getStringExtra("which"))) {
            getSupportFragmentManager().beginTransaction().replace(R.id.root, DeviceConnectionDescriptionFragment.newInstance(uuid)).commit();
        } else if ("no_network".equals(intent.getStringExtra("which"))) {
            getSupportFragmentManager().beginTransaction().replace(R.id.root, ConnectionDescriptionFragment.newInstance()).commit();
        }
    }
}
