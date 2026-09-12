package com.BiliClient.Noctilucere.util;

import android.util.Log;

import com.BiliClient.Noctilucere.api.ConfInfoApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.Inflater;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 被 luern0313 创建于 2019/10/13.
 * #以下代码来源于腕上哔哩的开源项目，感谢开源者做出的贡献！
 */

public class NetWorkUtil
{
    private static final AtomicReference<OkHttpClient> INSTANCE = new AtomicReference<>();
    private static OkHttpClient getOkHttpInstance() {
        while(INSTANCE.get() == null){
            INSTANCE.compareAndSet(null, new OkHttpClient
                    .Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS).build());
        }
        return INSTANCE.get();
    }

    // 重构点（Noctilucere 芋泥P）：单参数 get 统一复用带 web 请求头的实现。
    // 原实现返回 null 导致 bangumi_to_card 等调用处 Objects.requireNonNull 触发 NPE 暗病；
    // 现复用 get(url, headers) 保证成功/失败都返回非 null 的 Response，并自动补齐 Cookie。
    public static Response get(String url) throws IOException
    {
        return get(url, ConfInfoApi.webHeaders);
    }

    public static Response get(String url, ArrayList<String> headers) throws IOException
    {
        Log.e("debug-get","----------------");
        Log.e("debug-get-url",url);
        Log.e("debug-get","----------------");
        OkHttpClient client = getOkHttpInstance();
        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        for(int i = 0; i < headers.size(); i+=2)
            requestBuilder = requestBuilder.addHeader(headers.get(i), headers.get(i+1));
        Request request = requestBuilder.build();
        Response response =  client.newCall(request).execute();
        saveCookiesFromResponse(response);
        return response;
    }

    public static Response post(String url, String data, ArrayList<String> headers) throws IOException
    {
        Log.e("debug-post","----------------");
        Log.e("debug-post-url",url);
        Log.e("debug-post-data",data);
        Log.e("debug-post","----------------");
        OkHttpClient client = getOkHttpInstance();
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"), data);
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        for(int i = 0; i < headers.size(); i+=2)
            requestBuilder = requestBuilder.addHeader(headers.get(i), headers.get(i+1));
        Request request = requestBuilder.build();
        Response response =  client.newCall(request).execute();
        saveCookiesFromResponse(response);
        return response;
    }


    public static byte[] readStream(InputStream inStream) throws IOException
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inStream.read(buffer)) != -1)
        {
            outStream.write(buffer, 0, len);
        }
        outStream.close();
        inStream.close();
        return outStream.toByteArray();
    }

    // 重构点（Noctilucere 芋泥P）：解压失败时不再静默吞掉异常并返回空字节数组，
    // 否则调用方只能得到无意义的解析错误、极难排查。改为向上抛出原始异常。
    public static byte[] uncompress(byte[] inputByte) throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputByte.length);
        try
        {
            Inflater inflater = new Inflater(true);
            inflater.setInput(inputByte);
            byte[] buffer = new byte[4 * 1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        }
        catch (Exception e)
        {
            throw new IOException("gzip/deflate 解压失败", e);
        }
        finally
        {
            try { outputStream.close(); } catch (IOException ignored) {}
        }
    }

    public static String getInfoFromCookie(String name, String cookie)
    {
        String[] cookies = cookie.split("; ");
        for(String i : cookies)
        {
            if(i.contains(name + "="))
                return i.substring(name.length() + 1);
        }
        return "";
    }

    private static void saveCookiesFromResponse(Response response) {
        List<String> newCookies = response.headers("Set-Cookie");

        //如果没有新cookies，直接返回
        if (newCookies.isEmpty()) return;
        String cookiesStr = SharedPreferencesUtil.getString(SharedPreferencesUtil.cookies, "");
        ArrayList<String> oldCookies = (cookiesStr.equals("") ? new ArrayList<>() : new ArrayList<>(Arrays.asList(cookiesStr.split("; "))));  //转list

        for (String newCookie : newCookies) {  //对每一条新cookie遍历

            int index = newCookie.indexOf("; ");
            if (index != -1) newCookie = newCookie.substring(0, index);  //如果没有分号不做处理

            index = newCookie.indexOf("=") + 1;
            if(index == 0) continue;   //如果没有等号，跳过

            String key = newCookie.substring(0, index);    //key=

            boolean added = false;
            for (int i = 0; i < oldCookies.size(); i++) {  //查找旧cookie表有没有
                String oldCookie = oldCookies.get(i);
                if (oldCookie.contains(key)) {
                    oldCookies.set(i, newCookie);    //有的话直接换掉
                    added = true;
                    break;
                }
            }
            if (!added) {
                oldCookies.add(newCookie);  //没有就加项
            }
        }

        StringBuilder setCookies = new StringBuilder();
        for (String setCookie : oldCookies) {
            setCookies.append(setCookie).append("; ");
        }
        //如果一次setCookies都没有，就不要存了， 因为是个空字符串
        if(setCookies.length() >= 2) {
            // 重构点（Noctilucere 芋泥P）：移除原先打印完整 Cookie 串的日志，避免 SESSDATA 等登录态泄漏到 logcat。
            SharedPreferencesUtil.putString(SharedPreferencesUtil.cookies, setCookies.substring(0, setCookies.length() - 2));
            ConfInfoApi.refreshHeaders();
        }
    }
}
