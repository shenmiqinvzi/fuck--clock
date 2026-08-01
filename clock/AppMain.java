package clock;

import javax.swing.*;

/**
 * 应用入口 —— 启动数据加载 → 创建主窗口 → 注册开机自启。
 * 整个生命周期：init → show → tray → exit。
 */
public class AppMain {

    public static void main(String[] args) {
        // 设置 Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // 加载数据
        Config.ensureDataDir();
        DataManager.getInstance().load();
        DataManager.getInstance().ensureRoutineFor(java.time.LocalDate.now());
        StudyStats.getInstance().load();

        // 启动 UI
        SwingUtilities.invokeLater(() -> {
            MainWindow win = new MainWindow();
            win.setVisible(true);

            // 注册开机自启（安静模式）
            win.setAutoStart();
        });
    }
}
