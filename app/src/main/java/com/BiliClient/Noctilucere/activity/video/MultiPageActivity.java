package com.BiliClient.Noctilucere.activity.video;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.activity.base.BaseActivity;
import com.BiliClient.Noctilucere.adapter.PageChooseAdapter;
import com.BiliClient.Noctilucere.api.ConfInfoApi;
import com.BiliClient.Noctilucere.api.PlayerApi;
import com.BiliClient.Noctilucere.model.VideoInfo;
import com.BiliClient.Noctilucere.util.LittleToolsUtil;
import com.BiliClient.Noctilucere.util.MsgUtil;

import java.io.File;

//分页视频选集
//2023-07-17

public class MultiPageActivity extends BaseActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        findViewById(R.id.top).setOnClickListener(view -> finish());

        TextView textView = findViewById(R.id.pageName);
        textView.setText("请选择分页");

        Intent intent = getIntent();
        VideoInfo videoInfo = (VideoInfo) intent.getSerializableExtra("videoInfo");

        PageChooseAdapter adapter = new PageChooseAdapter(this,videoInfo.pagenames);

        if(intent.getIntExtra("download",0) == 1) {    //下载模式
            adapter.setOnItemClickListener(position -> {
                File rootPath = new File(ConfInfoApi.getDownloadPath(this), LittleToolsUtil.stringToFile(videoInfo.title));
                File downPath = new File(rootPath, LittleToolsUtil.stringToFile(videoInfo.pagenames.get(position)));
                if(downPath.exists()) MsgUtil.toast("已经缓存过了~",this);
                else PlayerApi.startDownloadingVideo(this,videoInfo,position);
            });
        }
        else{        //普通播放模式
            adapter.setOnItemClickListener(position -> PlayerApi.startGettingUrl(this,videoInfo,position));
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

}