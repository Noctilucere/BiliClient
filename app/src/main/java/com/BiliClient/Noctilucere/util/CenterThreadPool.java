package com.BiliClient.Noctilucere.util;

import androidx.core.util.Supplier;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.bumptech.glide.util.Executors;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author silent碎月
 * 核心运行线程池, 最大线程容量为CPU核心数的2倍, 线程空闲时间为60s, 线程队列容量为20
 * 重构（Noctilucere 芋泥P）：核心线程数由固定 1 提升为 min(4, CPU*2)，
 * 后台任务尽量并行执行以改善卡顿；拒绝策略改为 CallerRunsPolicy 防止队列打满时崩溃。
 */
public class CenterThreadPool {


    private static final AtomicReference<ExecutorService> INSTANCE = new AtomicReference<>();
    private static ExecutorService getInstance(){
        while(INSTANCE.get() == null){
            // 重构点（Noctilucere 芋泥P）：原核心线程数为 1，低并发下任务串行执行、易卡顿；
            // 提升核心线程数让网络请求等任务并行，并以 CallerRunsPolicy 兜底避免 RejectedExecutionException。
            int max = Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
            int core = Math.min(4, max);
            INSTANCE.compareAndSet(null, new ThreadPoolExecutor(
                    core,
                    max,
                    60,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(20),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            ));
        }
        return INSTANCE.get();
    }



    /**
     * 在后台运行, 用于网络请求等耗时操作
     * @param runnable 要运行的任务
     */
    public static void run(Runnable runnable){
      /*  BuildersKt.launch(INSTANCE, Dispatchers.getIO(), CoroutineStart.DEFAULT, (CoroutineScope scope, Continuation continuation) -> {
            runnable.run();
            return Unit.INSTANCE;
        });*/
        getInstance().submit(runnable);
    }

    /**
     * 在后台运行, 用于网络请求等耗时操作, 有返回值, 使用LiveData.observe()获取返回值, 会自动切到主线程,不需要再runOnUiThread().
     * @param supplier 要运行的任务
     * @return LiveData包装的返回值
     * @param <T> 返回值类型
     */
    public static <T> LiveData<T> supplyAsync(Supplier<T> supplier) {
        MutableLiveData<T> retval = new MutableLiveData<>();
        /*BuildersKt.launch(INSTANCE, Dispatchers.getIO(), CoroutineStart.DEFAULT, (CoroutineScope scope, Continuation continuation) -> {
            T res = supplier.get();
            retval.postValue(res);
            return Unit.INSTANCE;
        });*/
        getInstance().submit(() -> {
            T res = supplier.get();
            retval.postValue(res);
        });
        return retval;
    }

    /**
     * 在主线程运行, 用于更新UI, 例如Toast, Snackbar等
     * @param runnable 要运行的任务
     */
    public static void runOnMainThread(Runnable runnable){
       /* BuildersKt.launch(INSTANCE, Dispatchers.getMain(), CoroutineStart.DEFAULT, (CoroutineScope scope, Continuation continuation) -> {
            runnable.run();
            return Unit.INSTANCE;
        });*/
        Executors.mainThreadExecutor().execute(runnable);
    }

// 想在这里实现一个自动切线程的网络请求一个方法,
// 但是这种方式需要json转换器, 例如Gson, Moshi的第三方库的引入
// 现在用的仍然是jsonObject做手动json转换, 先注释掉
//    public static requireNetWork<T>(String url, NetWorkUtil.Callback<T> callback){
//        CenterThreadPool.run(() -> {
//        JsonObject obj = request(url);
//        T res = Gson.fromJson(obj);
//        runOnUiThread(() -> callback.onSuccess(res));
//       });
//    }


}
