package com.BiliClient.Noctilucere.activity.user.favorite;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.activity.base.BaseActivity;
import com.BiliClient.Noctilucere.adapter.FolderChooseAdapter;
import com.BiliClient.Noctilucere.api.FavoriteApi;
import com.BiliClient.Noctilucere.util.CenterThreadPool;
import com.BiliClient.Noctilucere.util.MsgUtil;
import com.BiliClient.Noctilucere.util.SharedPreferencesUtil;

import java.util.ArrayList;

//添加收藏
//2023-08-28

public class AddFavoriteActivity extends BaseActivity {
    private static final String TAG = "AddFavoriteActivity";
    FolderChooseAdapter adapter;
    ArrayList<String> folderList = new ArrayList<>();
    ArrayList<Boolean> stateList = new ArrayList<>();
    ArrayList<Long> fidList = new ArrayList<>();
    long aid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_add);

        findViewById(R.id.top).setOnClickListener(view -> finish());

        Intent intent = getIntent();
        aid = intent.getLongExtra("aid",0);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        CenterThreadPool.run(()->{
            try {
                FavoriteApi.getFavoriteState(aid,folderList,fidList,stateList);

                adapter = new FolderChooseAdapter(this,folderList,fidList,stateList,aid);

                runOnUiThread(()->{
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(adapter);
                });
            } catch (Exception e) {
                runOnUiThread(() -> MsgUtil.quickErr(MsgUtil.err_net, this));
                Log.wtf(TAG, e);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if(adapter!=null) {
            if (SharedPreferencesUtil.getBoolean("fav_notice", false)) {
                if (adapter.added) MsgUtil.toast("添加成功", this);
                else if (adapter.changed) MsgUtil.toast("更改成功", this);
            }
            Intent intent = new Intent();
            intent.putExtra("is_changed", adapter.changed);
            setResult(RESULT_OK, intent);
        }
        
        super.onDestroy();
        
    }
}