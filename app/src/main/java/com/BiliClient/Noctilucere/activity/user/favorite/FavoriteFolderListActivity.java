package com.BiliClient.Noctilucere.activity.user.favorite;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.activity.base.BaseActivity;
import com.BiliClient.Noctilucere.adapter.FavoriteFolderAdapter;
import com.BiliClient.Noctilucere.api.FavoriteApi;
import com.BiliClient.Noctilucere.model.FavoriteFolder;
import com.BiliClient.Noctilucere.util.CenterThreadPool;
import com.BiliClient.Noctilucere.util.MsgUtil;
import com.BiliClient.Noctilucere.util.SharedPreferencesUtil;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

//收藏夹列表
//2023-08-07

public class FavoriteFolderListActivity extends BaseActivity {

    private RecyclerView recyclerView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        long mid = SharedPreferencesUtil.getLong("mid",0);

        findViewById(R.id.top).setOnClickListener(view -> finish());
        recyclerView = findViewById(R.id.recyclerView);

        TextView pageName = findViewById(R.id.pageName);
        pageName.setText("收藏");

        CenterThreadPool.run(()->{
            try {
                ArrayList<FavoriteFolder> folderList = FavoriteApi.getFavoriteFolders(mid);
                FavoriteFolderAdapter adapter = new FavoriteFolderAdapter(this,folderList,mid);
                runOnUiThread(()->{
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(adapter);
                });
            } catch (IOException e) {
                runOnUiThread(()-> MsgUtil.quickErr(MsgUtil.err_net,this));
                e.printStackTrace();
            } catch (JSONException e) {
                runOnUiThread(()-> MsgUtil.jsonErr(e,this));
                e.printStackTrace();
            }
        });
    }
}