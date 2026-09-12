package com.BiliClient.Noctilucere.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// 轻量弹幕渲染层（Noctilucere 芋泥P）：无需第三方库，直接叠加在 VideoView 之上绘制 B站评论弹幕。
// 支持滚动弹幕(mode=1)与顶部/底部固定弹幕(mode=5/4)，按视频播放进度驱动出现时间。
// 使用 postOnAnimation 对齐 VSync，并对并发弹幕数量限流，保证播放流畅度。
public class DanmakuView extends View {

    public static class Item {
        public float time;   // 出现时间（秒）
        public int mode;     // 1 滚动；4 底部；5 顶部
        public int color;    // 0xAARRGGBB
        public String text;
    }

    private static class Active {
        String text;
        float x, y;
        float speed;     // px/ms（仅滚动弹幕）
        int color;
        int mode;
        float width;
        long expireAt;  // 固定弹幕消失时间戳
    }

    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private List<Item> items = new ArrayList<>();
    private int nextIndex = 0;
    private final List<Active> active = new ArrayList<>();
    private boolean running = false;
    private boolean showing = true;
    private PositionProvider positionProvider;
    private int laneHeight = 40;
    private static final int MAX_ACTIVE = 250;   // 并发弹幕上限，超出则跳过，避免卡顿

    private long lastTick = 0;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            long dt = Math.min(now - lastTick, 50);
            lastTick = now;
            if (showing) {
                if (!items.isEmpty() && positionProvider != null) {
                    spawnUpTo(positionProvider.getPosition());
                }
                move(dt);
                invalidate();   // 仅在有弹幕时才有绘制开销（空列表 onDraw 直接返回）
            }
            if (running) postOnAnimation(tick);
        }
    };

    public interface PositionProvider {
        long getPosition();   // 当前播放位置（毫秒）
    }

    public DanmakuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);   // 确保自定义绘制始终生效
        paint.setTextSize(18);
        paint.setColor(0xffffffff);
        paint.setShadowLayer(1.5f, 1f, 1f, 0x80000000);  // 描边阴影，提升可读性
    }

    public void setItems(List<Item> items) {
        Collections.sort(items, (a, b) -> Float.compare(a.time, b.time));
        this.items = items;
        this.nextIndex = 0;
        this.active.clear();
        if (running) invalidate();
    }

    public void setPositionProvider(PositionProvider p) {
        this.positionProvider = p;
    }

    public void setShowing(boolean s) {
        this.showing = s;
        if (!s) {
            active.clear();
            invalidate();
        }
    }

    public boolean isShowing() {
        return showing;
    }

    public void start() {
        if (running) return;
        running = true;
        lastTick = System.currentTimeMillis();
        postOnAnimation(tick);
    }

    public void stop() {
        running = false;
        removeCallbacks(tick);
    }

    private void spawnUpTo(long posMs) {
        if (active.size() >= MAX_ACTIVE) return;   // 限流，保证流畅
        float posSec = posMs / 1000f;
        while (nextIndex < items.size() && items.get(nextIndex).time <= posSec) {
            Item it = items.get(nextIndex++);
            Active a = new Active();
            a.text = it.text;
            a.color = it.color;
            a.mode = it.mode;
            a.width = paint.measureText(it.text);
            if (it.mode == 4 || it.mode == 5) {
                a.y = (it.mode == 5) ? laneTop(0) : (getHeight() - laneHeight - 4);
                a.x = (getWidth() - a.width) / 2f;
                a.expireAt = System.currentTimeMillis() + 4000;
            } else {
                a.x = getWidth();
                a.y = laneTop(randomLane());
                a.speed = (getWidth() + a.width) / 8000f;   // 约 8 秒横穿屏幕
            }
            active.add(a);
            if (active.size() >= MAX_ACTIVE) break;
        }
    }

    private int laneCount() {
        return Math.max(1, getHeight() / laneHeight);
    }

    private int laneTop(int lane) {
        return 6 + lane * laneHeight;
    }

    private int randomLane() {
        return random.nextInt(laneCount());
    }

    private void move(long dt) {
        float dtf = dt;
        for (int i = active.size() - 1; i >= 0; i--) {
            Active a = active.get(i);
            if (a.mode == 1) {
                a.x -= a.speed * dtf;
                if (a.x + a.width < 0) active.remove(i);
            } else if (System.currentTimeMillis() > a.expireAt) {
                active.remove(i);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!showing || active.isEmpty()) return;
        for (Active a : active) {
            paint.setColor(a.color);
            canvas.drawText(a.text, a.x, a.y + laneHeight * 0.78f, paint);
        }
    }
}
