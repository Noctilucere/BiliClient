package com.BiliClient.Noctilucere.activity.video;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.activity.base.BaseActivity;
import com.BiliClient.Noctilucere.activity.DownloadActivity;
import com.BiliClient.Noctilucere.api.ConfInfoApi;
import com.BiliClient.Noctilucere.api.PlayerApi;
import com.BiliClient.Noctilucere.util.CenterThreadPool;
import com.BiliClient.Noctilucere.util.NetWorkUtil;
import com.BiliClient.Noctilucere.util.SharedPreferencesUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Response;

// 重构与优化（Noctilucere 芋泥P）：
//   - requestVideo：增加 playurl 返回码校验与可读错误提示，未登录 / 大会员限制不再被笼统掩盖；
//   - data / durl 字段增加类型与空值保护，避免风控返回非对象时直接崩溃；
//   - 移除向 logcat 打印完整 Cookie 的日志，消除登录态泄漏隐患；
//   - 新 API 适配：未登录时强制使用可播放清晰度(qn=16)，避免请求 1080P(qn=80) 无权限导致无法观看。
public class JumpToPlayerActivity extends BaseActivity {
    private String videourl;
    private String danmakuurl;
    private String title;
    private TextView textView;

    int download;

    boolean destroyed = false;

    boolean html5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_jump);

        textView = findViewById(R.id.title);

        Intent intent = getIntent();
        Log.e("debug-哔哩终端-跳转页","已接收数据");
        String bvid = intent.getStringExtra("bvid");
        long aid = intent.getLongExtra("aid", 0);
        long cid = intent.getLongExtra("cid", 0);

        title = intent.getStringExtra("title");
        download = intent.getIntExtra("download",0);

        html5 = intent.getBooleanExtra("html5",true);

        Log.e("debug-哔哩终端-跳转页", "cid=" + cid);
        danmakuurl = "https://comment.bilibili.com/" + cid + ".xml";
        if (aid == 0) {
            Log.e("debug-哔哩终端-跳转页", "bid=" + bvid);
            requestVideo(0, bvid, cid);
        } else {
            Log.e("debug-哔哩终端-跳转页", "aid=" + aid);
            requestVideo(aid, null, cid);
        }
    }

    @SuppressLint("SetTextI18n")
    private void requestVideo(long aid, String bvid, long cid) {
        CenterThreadPool.run(()->{
//            String url;
            /*if(SharedPreferencesUtil.getBoolean("high_res",false)){
                if (aid == 0) {
                    url = "https://api.bilibili.com/x/player/playurl?bvid=" + bvid + "&cid=" + cid + "&qn=80&type=mp4";
                } else {
                    url = "https://api.bilibili.com/x/player/playurl?avid=" + aid + "&cid=" + cid + "&qn=80&type=mp4";
                }
            }
            else {
                if (aid == 0) {
                    url = "https://api.bilibili.com/x/player/playurl?platform=html5&bvid=" + bvid + "&cid=" + cid + "&qn=16&type=mp4";
                } else {
                    url = "https://api.bilibili.com/x/player/playurl?platform=html5&avid=" + aid + "&cid=" + cid + "&qn=16&type=mp4";
                }
            }*/ //原来的方法看起来太多if else 闲的没事的ic改了改

            // 新 API 适配（Noctilucere 芋泥P）：未登录时即使开启“高清视频”，也强制使用可播放的清晰度(qn=16)。
            // 未登录账号无 1080P(qn=80) 权限，请求后 playurl 会返回 -404/-412，导致无法播放。
            boolean highRes = SharedPreferencesUtil.getBoolean("high_res", false);
            String sessdata = NetWorkUtil.getInfoFromCookie("SESSDATA", SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));
            boolean loggedIn = sessdata != null && !sessdata.isEmpty();
            String qnParam = (highRes && loggedIn) ? (html5 ? "&high_quality=1&qn=80" : "&qn=80") : "&qn=16";

            String url = "https://api.bilibili.com/x/player/wbi/playurl?"
                    + (aid == 0 ? ("bvid=" + bvid): ("avid=" + aid))
                    + "&cid=" + cid + "&type=mp4"
                    + qnParam
                    + "&platform=" + (html5 ? "html5" : "pc");
            //顺便把platform html5给删了,实测删除后放和番剧相关的东西不会404了 (不到为啥)

            try {
                url=ConfInfoApi.signWBI(url);

                Response response = NetWorkUtil.get(url, ConfInfoApi.webHeaders);

                String body = Objects.requireNonNull(response.body()).string();
                JSONObject body1 = new JSONObject(body);

                // 重构点（Noctilucere 芋泥P）：先校验接口返回码，给出可读错误，
                // 避免未登录 / 大会员限制时被笼统的“视频获取失败”掩盖，便于定位未登录播放问题。
                int code = body1.optInt("code", -1);
                if (code != 0) {
                    final String msg = body1.optString("message", "未知错误");
                    runOnUiThread(() -> textView.setText("视频获取失败！\n错误码：" + code + "\n" + msg
                            + "\n可能原因：需要登录 / 大会员，或接口已变更"));
                    return;
                }

                // data 字段可能为 null / 非对象（如未登录被风控），需做类型与空值保护
                Object dataObj = body1.get("data");
                if (!(dataObj instanceof JSONObject)) {
                    runOnUiThread(() -> textView.setText("视频获取失败！\n未返回可用播放地址\n（可能需登录或大会员）"));
                    return;
                }
                JSONObject data = (JSONObject) dataObj;
                // 兼容更多 API（Noctilucere 芋泥P）：优先使用渐进流 durl，
                // 若视频仅返回 DASH 结构则兜底取最高清晰度视频流，提升对不同返回格式的兼容。
                JSONArray durl = data.optJSONArray("durl");
                if (durl != null && durl.length() > 0) {
                    videourl = durl.getJSONObject(0).getString("url");
                } else if (data.has("dash") && !data.isNull("dash")) {
                    JSONObject dash = data.getJSONObject("dash");
                    JSONArray video = dash.optJSONArray("video");
                    if (video != null && video.length() > 0) {
                        videourl = video.getJSONObject(0).getString("baseUrl");
                    } else {
                        runOnUiThread(() -> textView.setText("视频获取失败！\n未返回可用播放地址\n（可能需登录或大会员）"));
                        return;
                    }
                } else {
                    runOnUiThread(() -> textView.setText("视频获取失败！\n未返回可用播放地址\n（可能需登录或大会员）"));
                    return;
                }

                if(!destroyed) {
                    if (download != 0) {
                        Intent intent = new Intent();
                        intent.setClass(this, DownloadActivity.class);
                        intent.putExtra("type", download);
                        intent.putExtra("link", videourl);
                        intent.putExtra("danmaku", danmakuurl);
                        intent.putExtra("title", title);
                        intent.putExtra("cover", getIntent().getStringExtra("cover"));
                        if (download == 2)
                            intent.putExtra("parent_title", getIntent().getStringExtra("parent_title"));
                        startActivity(intent);
                    } else {
                        PlayerApi.jumpToPlayer(JumpToPlayerActivity.this, videourl, danmakuurl, title, false);
                    }
                    finish();
                }
            } catch (IOException e) {
                runOnUiThread(()->textView.setText("网络错误！\n请检查你的网络连接是否正常"));
                e.printStackTrace();
            } catch (JSONException e) {
                runOnUiThread(()->textView.setText("视频获取失败！\n可能的原因：\n1.本视频仅大会员可播放\n2.视频获取接口失效"));
                e.printStackTrace();
            } catch (ActivityNotFoundException e){
                runOnUiThread(()->textView.setText("跳转失败！\n请安装对应的播放器\n或将哔哩终端和播放器同时更新到最新版本"));
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        super.onDestroy();
    }
}