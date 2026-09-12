package com.BiliClient.Noctilucere.activity.video;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.activity.base.BaseActivity;
import com.BiliClient.Noctilucere.api.ConfInfoApi;
import com.BiliClient.Noctilucere.util.NetWorkUtil;
import com.BiliClient.Noctilucere.util.SharedPreferencesUtil;
import com.BiliClient.Noctilucere.view.DanmakuView;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

// 内置播放器（Noctilucere 芋泥P）：基于 VideoView 实现，无需安装第三方播放器即可直接播放。
// 关键点：B站视频 CDN 必须携带 Referer 与登录态 Cookie，否则会返回 403，因此通过 setVideoURI(Uri, headers) 注入请求头。
// 弹幕：后台拉取 B站评论 XML（comment.bilibili.com/{cid}.xml）并解析，按播放进度在 DanmakuView 上绘制。
public class BuiltinPlayerActivity extends BaseActivity {

    private VideoView videoView;
    private ProgressBar loading;
    private TextView title;
    private TextView danmakuStatus;
    private DanmakuView danmakuView;
    private ImageButton danmakuToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_builtin_player);

        String url = getIntent().getStringExtra("url");
        String videoTitle = getIntent().getStringExtra("title");
        boolean local = getIntent().getBooleanExtra("local", false);
        String danmakuUrl = getIntent().getStringExtra("danmakuurl");

        videoView = findViewById(R.id.videoView);
        loading = findViewById(R.id.loading);
        title = findViewById(R.id.title);
        danmakuStatus = findViewById(R.id.danmakuStatus);
        danmakuView = findViewById(R.id.danmakuView);
        danmakuToggle = findViewById(R.id.danmakuToggle);
        if (videoTitle != null) title.setText(videoTitle);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        MediaController controller = new MediaController(this);
        controller.setAnchorView(videoView);
        videoView.setMediaController(controller);

        videoView.setOnPreparedListener(mp -> {
            loading.setVisibility(View.GONE);
            mp.setScreenOnWhilePlaying(true);
            videoView.start();
            danmakuView.setPositionProvider(() -> videoView.getCurrentPosition());
            danmakuView.start();
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            loading.setVisibility(View.GONE);
            Toast.makeText(this, "播放失败（" + what + "," + extra + "），可尝试改用外部播放器", Toast.LENGTH_LONG).show();
            return true;
        });
        videoView.setOnCompletionListener(mp -> loading.setVisibility(View.GONE));

        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "视频地址为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (local) {
            videoView.setVideoPath(url);
        } else {
            // B站视频 CDN 需要 Referer 与 Cookie，否则返回 403（兼容更多风控场景）
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://www.bilibili.com/");
            headers.put("User-Agent", ConfInfoApi.USER_AGENT_WEB);
            headers.put("Cookie", SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));
            videoView.setVideoURI(Uri.parse(url), headers);
        }

        // 弹幕开关
        danmakuToggle.setOnClickListener(v -> {
            boolean show = !danmakuView.isShowing();
            danmakuView.setShowing(show);
            danmakuToggle.setAlpha(show ? 1.0f : 0.4f);
            Toast.makeText(this, show ? "弹幕已开启" : "弹幕已关闭", Toast.LENGTH_SHORT).show();
        });

        // 后台拉取并解析弹幕（B站评论 XML），失败给出明确提示
        if (danmakuUrl != null && !danmakuUrl.isEmpty()) {
            loadDanmaku(danmakuUrl);
        } else {
            danmakuStatus.setText("弹幕：无");
        }
    }

    private void loadDanmaku(String danmakuUrl) {
        danmakuStatus.setText("弹幕：加载中…");
        new Thread(() -> {
            try {
                // 弹幕获取（Noctilucere 芋泥P）：
                // 1) 先尝试原版评论 XML 接口 comment.bilibili.com/{cid}.xml；
                // 2) 失败则回退到新版弹幕接口 api.bilibili.com/x/v1/dm/list.so（需 WBI 签名）。
                // 两者返回格式相同（<d p="...">文本</d>），parseDanmaku 通用解析。
                String xml = fetchDanmakuXml(danmakuUrl);
                if ((xml == null || !xml.contains("<d ")) && danmakuUrl.contains("comment.bilibili.com")) {
                    String cid = extractCid(danmakuUrl);
                    if (!cid.isEmpty()) {
                        try {
                            String listUrl = ConfInfoApi.signWBI(
                                    "https://api.bilibili.com/x/v1/dm/list.so?oid=" + cid + "&type=1");
                            xml = fetchDanmakuXml(listUrl);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (xml == null || !xml.contains("<d ")) {
                    runOnUiThread(() -> {
                        danmakuStatus.setText("弹幕：获取失败");
                        Toast.makeText(this, "弹幕获取失败，已回退其他接口仍不可用", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                List<DanmakuView.Item> items = parseDanmaku(xml);
                if (items.isEmpty()) {
                    runOnUiThread(() -> {
                        danmakuStatus.setText("弹幕：空");
                        Toast.makeText(this, "该视频暂无可显示弹幕", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                runOnUiThread(() -> {
                    danmakuView.setItems(items);
                    danmakuStatus.setText("弹幕：" + items.size() + "条");
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    danmakuStatus.setText("弹幕：失败");
                    Toast.makeText(this, "弹幕加载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // 拉取弹幕 XML：NetWorkUtil.get 的 headers 为交替的 key/value 列表（见 ConfInfoApi.webHeaders），
    // 必须严格使用 "Key","Value" 交替格式，否则会越界崩溃。返回 null 表示未获取到有效 XML。
    private String fetchDanmakuXml(String url) {
        try {
            ArrayList<String> headers = new ArrayList<>();
            headers.add("User-Agent");
            headers.add(ConfInfoApi.USER_AGENT_WEB);
            headers.add("Referer");
            headers.add("https://www.bilibili.com/");
            headers.add("Cookie");
            headers.add(SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, ""));
            Response response = NetWorkUtil.get(url, headers);
            if (response == null || response.body() == null) return null;
            String xml = response.body().string();
            if (xml == null || xml.trim().isEmpty()) return null;
            return xml;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 从 comment.bilibili.com/{cid}.xml 中取出 cid
    private String extractCid(String danmakuUrl) {
        int slash = danmakuUrl.lastIndexOf('/');
        int dot = danmakuUrl.lastIndexOf('.');
        if (slash >= 0 && dot > slash) {
            String mid = danmakuUrl.substring(slash + 1, dot);
            if (mid.matches("\\d+")) return mid;
        }
        return "";
    }

    // 解析 B站评论弹幕 XML：<d p="time,mode,fontsize,color,...">文本</d>
    private List<DanmakuView.Item> parseDanmaku(String xml) throws Exception {
        List<DanmakuView.Item> list = new ArrayList<>();
        XmlPullParser parser = android.util.Xml.newPullParser();
        parser.setInput(new StringReader(xml));
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && "d".equals(parser.getName())) {
                String p = parser.getAttributeValue(null, "p");
                if (p != null) {
                    String[] parts = p.split(",");
                    if (parts.length >= 4) {
                        DanmakuView.Item item = new DanmakuView.Item();
                        item.time = Float.parseFloat(parts[0]);
                        item.mode = Integer.parseInt(parts[1]);
                        // B站颜色为十进制 0xRRGGBB，补足 alpha
                        item.color = 0xFF000000 | Integer.parseInt(parts[3]);
                        item.text = parser.nextText();
                        list.add(item);
                    }
                }
            }
            event = parser.next();
        }
        return list;
    }

    @Override
    protected void onDestroy() {
        danmakuView.stop();
        if (videoView != null) videoView.stopPlayback();
        super.onDestroy();
    }
}
