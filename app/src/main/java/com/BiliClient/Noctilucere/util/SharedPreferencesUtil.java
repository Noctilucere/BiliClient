package com.BiliClient.Noctilucere.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 被 luern0313 创建于 2020/5/4.
 * #以下代码来源于腕上哔哩的开源项目，有修改。感谢开源者做出的贡献！
 */
public class SharedPreferencesUtil
{
    public static String cookies = "cookies";
    public static String mid = "mid";
    public static String csrf = "csrf";
    public static String access_key = "access_key";
    public static String refresh_token = "refresh_token";
    public static String setup = "setup";
    public static String last_version = "last_version";
    public static String player = "player";
    public static String padding_horizontal = "padding_horizontal";
    public static String padding_vertical = "padding_vertical";
    public static String cookie_refresh = "cookie_refresh";


    private static SharedPreferences sharedPreferences;

    public static void initSharedPrefs(Context context){
        //实际上在BaseActivity里已经帮你init过了，通常无需再调用此函数
        if(sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("default", Context.MODE_PRIVATE);
        }
    }

    // 重构点（Noctilucere 芋泥P）：所有存取方法增加空安全保护。
    // 原先 sharedPreferences 未初始化（sharedPreferences==null）时直接调用其方法会抛 NPE，
    // 属于典型的“暗病”——在类加载顺序异常或极端启动时序下极易崩溃。
    // 这里统一在 null 时回退默认值 / 静默跳过写入，杜绝此类空指针隐患。

    public static String getString(String key, String def) {
        return (sharedPreferences != null) ? sharedPreferences.getString(key, def) : def;
    }

    public static void putString(String key, String value) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putString(key, value).apply();
    }

    public static int getInt(String key, int def)
    {
        return (sharedPreferences != null) ? sharedPreferences.getInt(key, def) : def;
    }

    public static void putInt(String key, int value) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public static long getLong(String key, long def)
    {
        return (sharedPreferences != null) ? sharedPreferences.getLong(key, def) : def;
    }

    public static void putLong(String key, long value) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putLong(key, value).apply();
    }

    public static boolean getBoolean(String key, boolean def)
    {
        return (sharedPreferences != null) ? sharedPreferences.getBoolean(key, def) : def;
    }

    public static void putBoolean(String key, boolean value)
    {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public static void putFloat(String key, float value) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    public static float getFloat(String key, float def)
    {
        return (sharedPreferences != null) ? sharedPreferences.getFloat(key, def) : def;
    }

    public static void removeValue(String key) {
        if (sharedPreferences == null) return;
        sharedPreferences.edit().remove(key).apply();
    }


}
