package com.cylan.jiafeigou.n.view.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.cylan.jiafeigou.R;
import com.superlog.SLog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 *
 */
public class LoginModel1Fragment extends LoginModelFragment {

    @BindView(R.id.iv_login_top_left)
    public ImageView ivTopLeft;
    @BindView(R.id.tv_login_top_center)
    public TextView tvTopCenter;
    @BindView(R.id.tv_login_top_right)
    public TextView tvTopRight;
    @BindView(R.id.fLayout_login_container)
    FrameLayout fLayoutLoginContainer;


    public LoginModel1Fragment() {
        // Required empty public constructor
    }


    public static LoginModel1Fragment newInstance(Bundle bundle) {
        LoginModel1Fragment fragment = new LoginModel1Fragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ///
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login_model1, container, false);
        ButterKnife.bind(this, view);
        addOnTouchListener(view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        Fragment fragment = LoginFragment.newInstance(null);
        Bundle bundle = new Bundle();
        bundle.putBoolean("first", true);
        fragment.setArguments(bundle);
        getChildFragmentManager().beginTransaction().
                setCustomAnimations(R.anim.slide_down_in, R.anim.slide_down_out)
                .add(R.id.fLayout_login_container, fragment, "login").commit();

        super.onResume();
    }

    @OnClick({R.id.iv_login_top_left})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_login_top_left:
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SLog.e("onAttach Context");
    }


}
