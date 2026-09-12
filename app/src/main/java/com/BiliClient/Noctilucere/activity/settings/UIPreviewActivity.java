package com.BiliClient.Noctilucere.activity.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.activity.base.BaseActivity;

public class UIPreviewActivity extends BaseActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_ui_preview);

        findViewById(R.id.top).setOnClickListener(view -> finish());
    }
}