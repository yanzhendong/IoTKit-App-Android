package com.cylan.jiafeigou.widget.dialog;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.TextView;

import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.support.log.AppLogger;
import com.cylan.jiafeigou.utils.TimeUtils;
import com.cylan.jiafeigou.widget.pick.AbstractWheel;
import com.cylan.jiafeigou.widget.pick.OnWheelChangedListener;
import com.cylan.jiafeigou.widget.pick.WheelVerticalView;
import com.cylan.jiafeigou.widget.pick.adapters.AbstractWheelTextAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by cylan-hunt on 16-12-22.
 */

public class DatePickerDialogFragment extends BaseDialog {

    public static final String KEY_LEFT_CONTENT = "key_left";
    public static final String KEY_RIGHT_CONTENT = "key_right";

    @BindView(R.id.tv_dialog_title)
    TextView tvDialogTitle;
    @BindView(R.id.wheel_date_pick)
    WheelVerticalView wheelDatePick;
    @BindView(R.id.tv_dialog_btn_left)
    TextView tvDialogBtnLeft;
    @BindView(R.id.tv_dialog_btn_right)
    TextView tvDialogBtnRight;
    /**
     * <凌晨时间戳,当天最早视频时间></>
     */
    private Map<Long, Long> dateMap = new HashMap<>();
    private List<Long> dateStartList = new ArrayList<>();
    private long timeFocus;
    private int finalIndex;

    public static DatePickerDialogFragment newInstance(Bundle bundle) {
        DatePickerDialogFragment fragment = new DatePickerDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_date_pick_dialog, container, true);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        final String title = bundle.getString(KEY_TITLE);
        final String lContent = bundle.getString(KEY_LEFT_CONTENT);
        final String rContent = bundle.getString(KEY_RIGHT_CONTENT);
        if (!TextUtils.isEmpty(title))
            tvDialogTitle.setText(title);
        if (!TextUtils.isEmpty(lContent))
            tvDialogBtnLeft.setText(lContent);
        if (!TextUtils.isEmpty(rContent))
            tvDialogBtnRight.setText(rContent);
        getDialog().setCanceledOnTouchOutside(bundle.getBoolean(KEY_TOUCH_OUT_SIDE_DISMISS, false));
        initWheel(getIndexByTime());
    }

    private int getIndexByTime() {
        //得到凌晨时间戳
        long time = SystemClock.currentThreadTimeMillis();
        long timeStart = TimeUtils.getTimeStart(timeFocus);
        Log.d("getIndexByTime", "getIndexByTime: " + dateStartList);
        //由于dateStartList是降序,所以需要Collections.reverseOrder()
        int index = Collections.binarySearch(dateStartList, timeStart, Collections.reverseOrder());
        if (index < 0) {
            index = -(index + 1);
            if (index < 0 || index > dateMap.size() - 1) {
                index = dateMap.size() - 1;
            }
        }
        Log.d("getIndexByTime", "getIndexByTime: performance: " + (SystemClock.currentThreadTimeMillis() - time));
        return index;
    }

    public void setTimeFocus(long timeFocus) {
        this.timeFocus = timeFocus;
    }

    public void setDateMap(Map<Long, Long> dateMap) {
        this.dateMap = dateMap;
        AppLogger.i("count" + (dateMap == null));
        long time = System.currentTimeMillis();
        Set<Long> set = dateMap.keySet();
        dateStartList = new ArrayList<>(new HashSet<>(set));
        Collections.sort(dateStartList, Collections.reverseOrder());//来一个降序
        Log.d("setDateMap", "setDateMap performance: " + (System.currentTimeMillis() - time));
    }

    private void initWheel(int index) {
        AbstractWheelTextAdapter adapter = new AbstractWheelTextAdapter(getContext()) {
            @Override
            public int getItemsCount() {
                return dateMap.size();
            }

            @Override
            protected CharSequence getItemText(int index) {
                return TimeUtils.getSpecifiedDate(dateMap.get(dateStartList.get(index)));
            }
        };
        adapter.setTextColor(getContext().getResources().getColor(R.color.color_4b9fd5));
        wheelDatePick.setViewAdapter(adapter);
        wheelDatePick.setCurrentItem(index);
        wheelDatePick.addChangingListener(changedListener);
        wheelDatePick.setCyclic(false);
        wheelDatePick.setInterpolator(new AnticipateOvershootInterpolator());
    }

    // Wheel changed listener
    private OnWheelChangedListener changedListener = new OnWheelChangedListener() {
        public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
            finalIndex = newValue;
        }
    };

    @OnClick({R.id.tv_dialog_btn_left, R.id.tv_dialog_btn_right})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_dialog_btn_right:
                dismiss();
                break;
            case R.id.tv_dialog_btn_left:
                dismiss();
                if (action != null && finalIndex > 0 && finalIndex < dateStartList.size()) {
                    action.onDialogAction(1, dateMap.get(dateStartList.get(finalIndex)));
                }
                break;
        }
    }
}