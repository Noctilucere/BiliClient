package com.BiliClient.Noctilucere.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.activity.base.BaseActivity;
import com.BiliClient.Noctilucere.activity.base.InstanceActivity;
import com.BiliClient.Noctilucere.activity.dynamic.DynamicActivity;
import com.BiliClient.Noctilucere.activity.message.MessageActivity;
import com.BiliClient.Noctilucere.activity.search.SearchActivity;
import com.BiliClient.Noctilucere.activity.settings.QRLoginActivity;
import com.BiliClient.Noctilucere.activity.settings.SettingMainActivity;
import com.BiliClient.Noctilucere.activity.user.MySpaceActivity;
import com.BiliClient.Noctilucere.activity.video.PopularActivity;
import com.BiliClient.Noctilucere.activity.video.PreciousActivity;
import com.BiliClient.Noctilucere.activity.video.RecommendActivity;
import com.BiliClient.Noctilucere.activity.video.local.LocalListActivity;
import com.BiliClient.Noctilucere.util.MsgUtil;
import com.BiliClient.Noctilucere.util.SharedPreferencesUtil;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

//菜单页面
//2023-07-14

public class MenuActivity extends BaseActivity {

    private int from;

    private final List<Class<? extends InstanceActivity>> classList = new ArrayList<Class<? extends InstanceActivity>>(){{
        add(RecommendActivity.class);
        add(PopularActivity.class);
        add(PreciousActivity.class);
        add(SearchActivity.class);
        add(DynamicActivity.class);
        add(QRLoginActivity.class);
        add(MySpaceActivity.class);
        add(MessageActivity.class);
        add(LocalListActivity.class);
        add(SettingMainActivity.class);
    }};

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Log.e("debug","进入菜单页");

        Intent intent = getIntent();
        from = intent.getIntExtra("from",0);

        findViewById(R.id.top).setOnClickListener(view -> finish());

        List<MaterialCardView> cardList = new ArrayList<MaterialCardView>() {{
            add(findViewById(R.id.main));
            add(findViewById(R.id.popular));
            add(findViewById(R.id.precious));
            add(findViewById(R.id.search));
            add(findViewById(R.id.dynamic));
            add(findViewById(R.id.login));
            add(findViewById(R.id.profile));
            add(findViewById(R.id.message));
            add(findViewById(R.id.local));
            add(findViewById(R.id.settings));
            add(findViewById(R.id.exit));
        }};

        if(SharedPreferencesUtil.getLong("mid",0) == 0) {
            findViewById(R.id.profile).setVisibility(View.GONE);
            findViewById(R.id.dynamic).setVisibility(View.GONE);
            findViewById(R.id.message).setVisibility(View.GONE);
        }
        else findViewById(R.id.login).setVisibility(View.GONE);

        if(!SharedPreferencesUtil.getBoolean("menu_popular",true))
            findViewById(R.id.popular).setVisibility(View.GONE);

        if(!SharedPreferencesUtil.getBoolean("menu_precious",false))
            findViewById(R.id.precious).setVisibility(View.GONE);

        for (int i = 0; i < cardList.size(); i++) {
            int finalI = i;
            cardList.get(i).setOnClickListener(view -> killAndJump(finalI));
        }
        
        //我求求你了退出按钮能用吧....
        findViewById(R.id.exit).setOnClickListener(view -> {
            for(int j = 0;j<classList.size();j++){
                InstanceActivity instance = InstanceActivity.getInstance(classList.get(j));
                if(instance != null) instance.finish();
            }
            finish();
        });

        if(!SharedPreferencesUtil.getBoolean("tutorial_menu",false)){
            MsgUtil.showDialog(this,"使用教程","点击上方标题栏可以返回上一个页面",R.mipmap.tutorial_menu,true,5);
            SharedPreferencesUtil.putBoolean("tutorial_menu",true);
        }
    }

    private void killAndJump(int i){
        if (i <= classList.size() && i != from){
            InstanceActivity instance = InstanceActivity.getInstance(classList.get(from));
            if(instance != null) instance.finish();
            if(i != classList.size()) {
                Intent intent = new Intent();
                intent.setClass(MenuActivity.this, classList.get(i));
                startActivity(intent);
            }
        }
        finish();
    }

}