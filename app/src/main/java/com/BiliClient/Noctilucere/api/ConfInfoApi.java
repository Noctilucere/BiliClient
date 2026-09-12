package com.BiliClient.Noctilucere.api;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.BiliClient.Noctilucere.R;
import com.BiliClient.Noctilucere.util.LittleToolsUtil;
import com.BiliClient.Noctilucere.util.NetWorkUtil;
import com.BiliClient.Noctilucere.util.SharedPreferencesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import okhttp3.Response;

/**
 * 被 luern0313 创建于 2019/8/25.
 * (人尽皆知的)绝 · 密 · 档 · 案
 * #以下代码修改自腕上哔哩的开源项目，感谢开源者做出的贡献！
 *
 * 重构与优化（Noctilucere 芋泥P）：
 *   - WBI 签名健壮性增强：nav 接口解析增加空安全，未登录时也能正确获取 wbi_img 与 buvid3；
 *   - 缓存的 mixin_key 丢失时强制重新获取，避免“一天一次”限制导致空签名被风控拦截；
 *   - 请求头 webHeaders/bbHeaders 在 SharedPreferences 未初始化时回退为 ""，消除类加载期 NPE；
 *   - 新 API 适配：新增 finger/spi 获取 buvid3/buvid4/b_nut（原版 getbuvid 仅 buvid3 作为回退保留），
 *     修复未登录风控缺失 buvid4 导致无法观看视频的 bug。
 */

public class ConfInfoApi
{
    public static File getDownloadPath(Context context){
        return new File(Environment.getExternalStorageDirectory() + "/Android/media/" + context.getPackageName() + "/");
    }

    private static final String TAG = "ConfInfoApi";

    //==================== 新 API 适配（Noctilucere 芋泥P）====================
    //B站风控升级后，未登录请求 playurl 必须在 Cookie 中同时携带 buvid3 / buvid4 / b_nut。
    //下方 finger/spi 接口为新版「无登录态」获取方式；原版 nav 获取 WBI 密钥的逻辑完整保留。
    public static final String WEB_GATEWAY = "https://api.bilibili.com";
    private static volatile boolean buvidReady = false;   // 本地是否已持有可用的 buvid3/buvid4

    public static final String USER_AGENT_BB = "Mozilla/5.0 BiliDroid/4.34.0 (bbcallen@gmail.com)";
    public static final String USER_AGENT_OWN = "BiliClient/2.2 (robin_0229@qq.com; bilibili@RobinNotBad;)";
    public static final String USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";


    /*
    这里是WBI签名校验
    https://socialsisteryi.github.io/bilibili-API-collect/docs/misc/sign/wbi.html#wbi-%E7%AD%BE%E5%90%8D%E7%AE%97%E6%B3%95
     */
    private static final int[] MIXIN_KEY_ENC_TAB = {46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52};

    // 重构点（Noctilucere 芋泥P）：
    //  1) nav 响应增加空安全：data / wbi_img 缺失时抛出可读异常，而非直接 NPE；
    //  2) NetWorkUtil.get 内部会自动 saveCookiesFromResponse，nav 返回的 buvid3 等 Cookie 会被持久化，
    //     这是“未登录也能观看视频”的关键——未登录请求需要 buvid3 才能通过风控。
    public static String getWBIRawKey() throws IOException, JSONException {
        Response response = NetWorkUtil.get("https://api.bilibili.com/x/web-interface/nav", webHeaders);
        String body = Objects.requireNonNull(response.body(), "nav 响应体为空").string();
        JSONObject getJson = new JSONObject(body);
        if (!getJson.has("data") || getJson.isNull("data")) {
            throw new JSONException("nav 返回异常：缺少 data 字段（可能触发风控或未登录限制）");
        }
        JSONObject data = getJson.getJSONObject("data");
        if (!data.has("wbi_img") || data.isNull("wbi_img")) {
            throw new JSONException("nav 返回异常：缺少 wbi_img 字段");
        }
        JSONObject wbi_img = data.getJSONObject("wbi_img");  //不要被名称骗了，这玩意是签名用的
        String img_key = LittleToolsUtil.getFileFirstName(LittleToolsUtil.getFileNameFromLink(wbi_img.getString("img_url")));  //得到文件名
        String sub_key = LittleToolsUtil.getFileFirstName(LittleToolsUtil.getFileNameFromLink(wbi_img.getString("sub_url")));

        return img_key + sub_key;  //相连
    }

    public static String getWBIMixinKey(String raw_key){
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            key.append(raw_key.charAt(MIXIN_KEY_ENC_TAB[i]));
        }

        return key.toString();
    }

    //计算时需要按字母顺序排列
    //使用时记得切换web的请求头
    public static String signWBI(String url_query) throws JSONException, IOException {
        // 新 API 适配（Noctilucere 芋泥P）：签名前确保本地已持有 buvid3/buvid4，
        // 未登录请求 playurl 通过风控所需。best-effort，失败仅记录日志，不影响其它 WBI 接口。
        try { ensureBuvid(); } catch (IOException | JSONException e) { Log.w(TAG, "ensureBuvid 失败，未登录播放可能受限", e); }
        // 重构点（Noctilucere 芋泥P）：缓存的 mixin_key 为空（首次运行 / 被清理）时，
        // 不受“一天一次”限制，立即重新拉取，避免带着空签名请求接口被风控拦掉导致未登录无法播放。
        String mixin_key = SharedPreferencesUtil.getString("wbi_mixin_key", "");
        int curr = getDateCurr();
        if (mixin_key.isEmpty() || SharedPreferencesUtil.getInt("last_wbi", 0) < curr) {    //限制一天一次
            mixin_key = ConfInfoApi.getWBIMixinKey(ConfInfoApi.getWBIRawKey());
            SharedPreferencesUtil.putString("wbi_mixin_key", mixin_key);
            SharedPreferencesUtil.putInt("last_wbi", curr);
        }

        String wts = String.valueOf(System.currentTimeMillis() / 1000);
        String calc_str = sortUrlParams(Uri.encode(url_query, "@#&=*+-_.,:!?()/~'%") + "&wts=" + wts) + mixin_key;

        String w_rid = md5(calc_str);

        return url_query + "&w_rid=" + w_rid + "&wts=" + wts;
    }

    public static String sortUrlParams(String url) {
        // 解析URL参数
        Map<String, String> paramMap = new HashMap<>();
        String[] params = url.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                paramMap.put(keyValue[0], keyValue[1]);
            }else if (keyValue.length == 1) {
                paramMap.put(keyValue[0], "");
            }
        }

        // 使用TreeMap对参数进行排序
        Map<String, String> sortedMap = new TreeMap<>(paramMap);

        // 构建排序后的URL
        StringBuilder sortedUrl = new StringBuilder();
        boolean isFirst = true;
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            if (!isFirst) {
                sortedUrl.append("&");
            } else {
                isFirst = false;
            }
            sortedUrl.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return sortedUrl.toString();
    }

    private static String md5(String plainText) {
        byte[] secretBytes;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            secretBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有md5这个算法！");
        }
        StringBuilder md5code = new StringBuilder(new BigInteger(1, secretBytes).toString(16));
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code.insert(0, "0");
        }
        return md5code.toString();
    }

    public static ArrayList<String> webHeaders = new ArrayList<String>() {{
        add("Cookie");
        add(SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies,""));
        add("Referer");
        add("https://www.bilibili.com/");
        add("User-Agent");
        add(USER_AGENT_WEB);
    }};

    public static ArrayList<String> bbHeaders = new ArrayList<String>() {{
        add("Cookie");
        add(SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies,""));
        add("Referer");
        add("https://www.bilibili.com/");
        add("User-Agent");
        add(USER_AGENT_BB);
    }};


    public static void refreshHeaders(){
        bbHeaders.set(1,SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies,""));
        webHeaders.set(1,SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies,""));
    }

    // 将 buvid3/buvid4/b_nut 注入到全局 Cookie 头，保证未登录请求通过风控
    private static void injectBuvidCookie(String buvid3, String buvid4, String bNut) {
        String cookies = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
        String[] parts = cookies.split(";");
        StringBuilder sb = new StringBuilder();
        for (String kv : parts) {
            String t = kv.trim();
            if (t.isEmpty()) continue;
            String k = t.split("=")[0].trim();
            if ("buvid3".equals(k) || "buvid4".equals(k) || "b_nut".equals(k)) continue;  // 剔除旧的，避免重复
            if (sb.length() > 0) sb.append("; ");
            sb.append(t);
        }
        if (!buvid3.isEmpty()) { if (sb.length() > 0) sb.append("; "); sb.append("buvid3=").append(buvid3); }
        if (!buvid4.isEmpty()) { if (sb.length() > 0) sb.append("; "); sb.append("buvid4=").append(buvid4); }
        if (!bNut.isEmpty())   { if (sb.length() > 0) sb.append("; "); sb.append("b_nut=").append(bNut); }
        SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, sb.toString());
        refreshHeaders();
    }

    /**
     * 新 API：确保本地已持有 buvid3 / buvid4（未登录播放风控所需）。
     * 优先调用 /x/frontend/finger/spi（同时返回 buvid3 与 buvid4），失败时回退原版 /x/web-frontend/getbuvid。
     * 原版 nav 获取 WBI 密钥的逻辑保持不变。
     */
    public static void ensureBuvid() throws IOException, JSONException {
        if (buvidReady) return;
        synchronized (ConfInfoApi.class) {
            if (buvidReady) return;
            String have3 = SharedPreferencesUtil.getString("buvid3", "");
            String have4 = SharedPreferencesUtil.getString("buvid4", "");
            if (!have3.isEmpty() && !have4.isEmpty()) {
                injectBuvidCookie(have3, have4, SharedPreferencesUtil.getString("b_nut", ""));
                buvidReady = true;
                return;
            }
            Response response = NetWorkUtil.get(WEB_GATEWAY + "/x/frontend/finger/spi", webHeaders);
            if (response == null || response.body() == null) throw new IOException("finger/spi 响应为空");
            JSONObject result = new JSONObject(response.body().string());
            if (result.optInt("code", -1) != 0 || !result.has("data")) {
                throw new JSONException("finger/spi 返回异常：" + result.optString("message"));
            }
            JSONObject data = result.getJSONObject("data");
            String b3 = data.optString("b_3", "");
            String b4 = data.optString("b_4", "");
            if (b3.isEmpty()) {
                ensureBuvidLegacy();    // 回退到原版 getbuvid（仅 buvid3）
            } else {
                SharedPreferencesUtil.putString("buvid3", b3);
                if (!b4.isEmpty()) SharedPreferencesUtil.putString("buvid4", b4);
                String bNut = String.valueOf(System.currentTimeMillis() / 1000L);
                SharedPreferencesUtil.putString("b_nut", bNut);
                injectBuvidCookie(b3, b4, bNut);
            }
            buvidReady = true;
        }
    }

    /**
     * 原版 API 兼容：通过 /x/web-frontend/getbuvid 获取 buvid3（仅 buvid3，无 buvid4）。
     * 作为 finger/spi 的回退保留，不破坏原有实现。
     */
    public static void ensureBuvidLegacy() throws IOException, JSONException {
        String buvid3 = SharedPreferencesUtil.getString("buvid3", "");
        if (!buvid3.isEmpty()) {
            injectBuvidCookie(buvid3, "", SharedPreferencesUtil.getString("b_nut", ""));
            return;
        }
        Response response = NetWorkUtil.get(WEB_GATEWAY + "/x/web-frontend/getbuvid", webHeaders);
        if (response == null || response.body() == null) throw new IOException("getbuvid 响应为空");
        JSONObject result = new JSONObject(response.body().string());
        if (result.optInt("code", -1) != 0 || !result.has("data")) {
            throw new JSONException("getbuvid 返回异常：" + result.optString("message"));
        }
        JSONObject data = result.getJSONObject("data");
        if (data.has("buvid")) {
            buvid3 = data.getString("buvid");
            SharedPreferencesUtil.putString("buvid3", buvid3);
            injectBuvidCookie(buvid3, "", SharedPreferencesUtil.getString("b_nut", ""));
        }
    }

    // 计算当前日期（用于 WBI 密钥缓存的“每天一次”刷新限制，与原版检查更新无关）
    public static int getDateCurr(){
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) * 10000 + calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DATE);
    }

}
