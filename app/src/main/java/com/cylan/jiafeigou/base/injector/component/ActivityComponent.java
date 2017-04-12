package com.cylan.jiafeigou.base.injector.component;

import com.cylan.jiafeigou.base.injector.lifecycle.PerActivity;
import com.cylan.jiafeigou.base.injector.module.ActivityModule;
import com.cylan.jiafeigou.n.view.bell.BellLiveActivity;
import com.cylan.jiafeigou.n.view.bell.BellRecordDetailActivity;
import com.cylan.jiafeigou.n.view.bell.DoorBellHomeActivity;
import com.cylan.jiafeigou.n.view.panorama.PanoramaAlbumActivity;
import com.cylan.jiafeigou.n.view.panorama.PanoramaCameraActivity;
import com.cylan.jiafeigou.n.view.panorama.PanoramaDetailActivity;
import com.cylan.jiafeigou.n.view.panorama.PanoramaSettingActivity;
import com.cylan.jiafeigou.n.view.record.DelayRecordActivity;

import dagger.Component;

/**
 * Created by yanzhendong on 2017/4/12.
 */
@PerActivity
@Component(dependencies = AppComponent.class, modules = ActivityModule.class)
public interface ActivityComponent {

    void inject(BellLiveActivity activity);

    void inject(PanoramaCameraActivity activity);

    void inject(PanoramaAlbumActivity activity);

    void inject(PanoramaDetailActivity activity);

    void inject(PanoramaSettingActivity activity);

    void inject(DelayRecordActivity activity);

    void inject(BellRecordDetailActivity activity);

    void inject(DoorBellHomeActivity activity);

}