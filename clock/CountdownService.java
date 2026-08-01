package clock;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

/**
 * 倒计时服务 —— 与 UI 分离的纯逻辑层。
 * 通过回调接口通知调用方（onTick / onFinish）。
 */
public class CountdownService {

    // ==================== 回调接口 ====================
    public interface Callback {
        /** 每秒回调，参数为剩余秒数 */
        void onTick(int remainSec);
        /** 倒计时归零时回调 */
        void onFinish(int totalSec);
    }

    // ==================== 状态 ====================
    private Timer timer;
    private volatile int remainSec = 0;
    private volatile boolean running = false;
    private volatile int expectedDuration = 0;
    private Callback callback;

    // ==================== 单例 ====================
    private static CountdownService instance;
    public static CountdownService getInstance() {
        if (instance == null) instance = new CountdownService();
        return instance;
    }
    private CountdownService() {}

    // ==================== API ====================
    public boolean isRunning() { return running; }
    public int getRemainSec() { return remainSec; }

    /** 启动倒计时，totalSec > 0 */
    public void start(int totalSec, Callback cb) {
        stop();
        if (totalSec <= 0) return;
        this.remainSec = totalSec;
        this.expectedDuration = totalSec;
        this.callback = cb;
        this.running = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                remainSec--;
                if (remainSec <= 0) {
                    stop();
                    int dur = expectedDuration;
                    expectedDuration = 0;
                    // 播放蜂鸣
                    for (int i = 0; i < 3; i++) {
                        Toolkit.getDefaultToolkit().beep();
                        if (i < 2) {
                            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
                        }
                    }
                    if (callback != null) {
                        SwingUtilities.invokeLater(() -> callback.onFinish(dur));
                    }
                } else {
                    if (callback != null) {
                        SwingUtilities.invokeLater(() -> callback.onTick(remainSec));
                    }
                }
            }
        }, 0, 1000);
    }

    public void stop() {
        running = false;
        if (timer != null) { timer.cancel(); timer = null; }
    }

    /** 获取本次倒计时的原始时长（秒），供 StudyStats 累加 */
    public int consumeDuration() {
        int d = expectedDuration;
        expectedDuration = 0;
        return d;
    }
}
