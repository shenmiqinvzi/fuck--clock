package clock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
// java.util.Timer 与 javax.swing.Timer 冲突，下方代码使用完全限定名

/**
 * 主悬浮窗口 —— 时钟显示 + 倒计时 + 学习时长 + 日历弹窗 + 系统托盘。
 * 所有 UI 组件在此组装，是应用的核心。
 */
public class MainWindow extends JFrame {

    // ==================== UI 组件 ====================
    private JLabel timeLabel;
    private JLabel countdownLabel;
    private JLabel studyLabel;
    private JTextField countInput;
    private java.util.Timer clockTimer;

    // ==================== 弹窗 ====================
    private JDialog calendarPopup;
    private CalendarPanel calendarPanel;
    private TodoPanel todoPanel;
    private javax.swing.Timer popupWatchdog;

    // ==================== 拖动 ====================
    private int dragX, dragY;

    // ==================== 构造 ====================
    public MainWindow() {
        initWindow();
        buildContent();
        buildTray();
        startClock();
    }

    // ==================== 窗口初始化 ====================
    private void initWindow() {
        setUndecorated(true);
        setAlwaysOnTop(true);
        setOpacity(Config.WIN_OPACITY);
        setSize(Config.WIN_W, Config.WIN_H);
        setLocation(Config.WIN_X, Config.WIN_Y);
        getContentPane().setBackground(Config.BG_PINK);

        // 鼠标拖动
        MouseAdapter dragAdapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragX = e.getX(); dragY = e.getY();
            }
        };
        MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                setLocation(getX() + e.getX() - dragX, getY() + e.getY() - dragY);
            }
        };
        getContentPane().addMouseListener(dragAdapter);
        getContentPane().addMouseMotionListener(motionAdapter);

        // 鼠标划入 → 展开弹窗
        getContentPane().addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { showPopup(); }
        });

        // 关闭 → 最小化到托盘
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                setVisible(false);  // 最小化到托盘
            }
        });
    }

    // ==================== 主界面 ====================
    private void buildContent() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Config.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(16, Config.CARD_BORDER),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        // 标题装饰行
        JPanel titleDeco = new JPanel(new BorderLayout());
        titleDeco.setBackground(Config.CARD_BG);

        JPanel leftDeco = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        leftDeco.setBackground(Config.CARD_BG);
        leftDeco.add(new JLabel(new UIUtils.SakuraIcon(10, Config.SAKURA)));

        JLabel titleHint = new JLabel("小樱花时钟", SwingConstants.CENTER);
        titleHint.setFont(Config.SMALL_FONT);
        titleHint.setForeground(Config.TEXT_MUTED);
        titleDeco.add(leftDeco, BorderLayout.WEST);
        titleDeco.add(titleHint, BorderLayout.CENTER);

        // 时钟
        timeLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        timeLabel.setFont(Config.CLOCK_FONT);
        timeLabel.setForeground(Config.TEXT_DARK);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 倒计时剩余
        countdownLabel = new JLabel("", new UIUtils.ClockIcon(13), SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        countdownLabel.setForeground(Config.SAKURA);
        countdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        countdownLabel.setVisible(false);

        // 学习时长
        studyLabel = new JLabel("0h", new UIUtils.ClockIcon(12), SwingConstants.CENTER);
        studyLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        studyLabel.setForeground(Config.TEXT_MUTED);
        studyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 操作栏
        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        countRow.setBackground(Config.CARD_BG);
        countInput = new JTextField(4);
        countInput.setFont(Config.SMALL_FONT);
        countInput.setToolTipText("分:秒 如 10:30");
        countInput.setText("05:00");

        JButton startBtn = UIUtils.mkSmallBtn("倒计时", Config.SAKURA);
        startBtn.addActionListener(e -> startCountdown());

        JButton addBtn = UIUtils.mkSmallBtn("+ 待办", new Color(0xCC88A0));
        addBtn.addActionListener(e -> openAddTodo());

        countRow.add(new JLabel(new UIUtils.ClockIcon(14)));
        countRow.add(countInput);
        countRow.add(startBtn);
        countRow.add(addBtn);

        card.add(titleDeco);
        card.add(timeLabel);
        card.add(countdownLabel);
        card.add(studyLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(countRow);

        // 用 JLayeredPane 放 × 按钮
        JLayeredPane layered = new JLayeredPane();
        layered.setBackground(Config.BG_PINK);
        card.setBounds(10, 8, 215, 130);
        layered.add(card, JLayeredPane.DEFAULT_LAYER);

        JButton closeBtn = new JButton("x");
        closeBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 10));
        closeBtn.setForeground(Config.DELETE_PINK);
        closeBtn.setBackground(new Color(0, 0, 0, 0));
        closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 2, 2, 2));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(false);
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(255, 200, 210)); closeBtn.setOpaque(true);
            }
            @Override public void mouseExited(MouseEvent e) {
                closeBtn.setOpaque(false); closeBtn.repaint();
            }
        });
        closeBtn.addActionListener(e -> { setVisible(false); });
        closeBtn.setBounds(217, 0, 18, 16);
        layered.add(closeBtn, JLayeredPane.PALETTE_LAYER);

        getContentPane().add(layered);
    }

    // ==================== 日历弹窗 ====================
    /** 懒加载日历弹窗（首次鼠标划入时才创建，减少初始内存） */
    private void ensurePopup() {
        if (calendarPopup != null) return;

        calendarPopup = new JDialog(this);
        calendarPopup.setUndecorated(true);
        calendarPopup.setOpacity(0.94f);
        calendarPopup.setSize(340, 460);
        calendarPopup.setBackground(Config.BG_PINK);

        UIUtils.DecoPanel popupRoot = new UIUtils.DecoPanel(null,
                // 花瓣
                new int[][]{{22, 30, 18}, {290, 22, 14}, {305, 200, 12}, {18, 180, 13},
                        {280, 380, 15}, {20, 400, 11}, {310, 80, 10}, {25, 310, 14}},
                // 星星
                new int[][]{{300, 22, 7}, {22, 18, 6}, {310, 395, 6}, {28, 420, 5},
                        {295, 160, 5}, {18, 260, 6}},
                // 圆点
                new int[][]{{50, 40, 3}, {80, 18, 2}, {260, 30, 3}, {160, 18, 2},
                        {40, 140, 3}, {310, 130, 2}, {30, 220, 2}, {315, 300, 3},
                        {55, 310, 2}, {140, 410, 3}, {220, 405, 2}, {290, 430, 2}}
        );
        popupRoot.setLayout(new BoxLayout(popupRoot, BoxLayout.Y_AXIS));
        popupRoot.setBackground(Config.BG_PINK);
        popupRoot.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        calendarPanel = new CalendarPanel();
        popupRoot.add(UIUtils.wrapCardDeco(calendarPanel, 320, 246));
        popupRoot.add(Box.createVerticalStrut(6));

        todoPanel = new TodoPanel();
        popupRoot.add(UIUtils.wrapCardDeco(todoPanel, 320, 175));

        // 日历点击 → 刷新待办
        calendarPanel.setOnDateSelected(date -> todoPanel.refresh(date));
        // 待办变更 → 刷新日历热力图
        todoPanel.setOnChanged(() -> calendarPanel.repaint());

        calendarPopup.getContentPane().add(popupRoot);
    }

    // ==================== 弹窗显示/隐藏 ====================
    private void showPopup() {
        ensurePopup();  // 懒加载：首次划入才创建弹窗
        if (calendarPopup.isVisible()) return;

        int px = getX() + getWidth() + 5;
        if (px + 340 > Toolkit.getDefaultToolkit().getScreenSize().width) {
            px = getX() - 345;
        }
        int py = getY();
        if (py + 460 > Toolkit.getDefaultToolkit().getScreenSize().height) {
            py = Toolkit.getDefaultToolkit().getScreenSize().height - 470;
        }
        calendarPopup.setLocation(px, py);
        todoPanel.refresh(calendarPanel.getSelectedDate());
        calendarPanel.repaint();
        calendarPopup.setVisible(true);

        // watchdog: 每 200ms 检查鼠标是否在两个窗口之一
        if (popupWatchdog == null || !popupWatchdog.isRunning()) {
            popupWatchdog = new javax.swing.Timer(200, e -> {
                if (!calendarPopup.isVisible()) {
                    ((javax.swing.Timer) e.getSource()).stop();
                    return;
                }
                Point mouse = MouseInfo.getPointerInfo().getLocation();
                boolean overMain = getBounds().contains(mouse);
                boolean overPopup = calendarPopup.getBounds().contains(mouse);
                if (!overMain && !overPopup) {
                    calendarPopup.setVisible(false);
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            });
            popupWatchdog.start();
        }
    }

    // ==================== 时钟刷新 ====================
    private void startClock() {
        clockTimer = new java.util.Timer();
        clockTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String now = sdf.format(new Date());
                CountdownService cs = CountdownService.getInstance();
                SwingUtilities.invokeLater(() -> {
                    timeLabel.setText(now);
                    if (cs.isRunning()) {
                        int m = cs.getRemainSec() / 60;
                        int s = cs.getRemainSec() % 60;
                        countdownLabel.setText(String.format("%02d:%02d", m, s));
                        countdownLabel.setVisible(true);
                    } else {
                        countdownLabel.setVisible(false);
                    }
                    updateStudyLabel();
                });
            }
        }, 0, 1000);
    }

    // ==================== 倒计时 ====================
    private void startCountdown() {
        CountdownService cs = CountdownService.getInstance();
        if (cs.isRunning()) {
            cs.stop();
            countdownLabel.setVisible(false);
            return;
        }

        String text = countInput.getText().trim();
        int totalSec;
        try {
            String[] split = text.split(":");
            int min = Integer.parseInt(split[0]);
            int sec = Integer.parseInt(split[1]);
            totalSec = min * 60 + sec;
            if (totalSec <= 0) {
                JOptionPane.showMessageDialog(this, "时间不能为0！");
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "格式错误，请输入 分:秒 例如 03:20");
            return;
        }

        cs.start(totalSec, new CountdownService.Callback() {
            @Override public void onTick(int remainSec) {
                int m = remainSec / 60;
                int s = remainSec % 60;
                countdownLabel.setText(String.format("%02d:%02d", m, s));
                countdownLabel.setVisible(true);
            }
            @Override public void onFinish(int duration) {
                countdownLabel.setVisible(false);
                StudyStats.getInstance().accumulate(duration);
                StudyStats.getInstance().save();
                updateStudyLabel();
                todoPanel.refresh(calendarPanel.getSelectedDate());
                showFinishDialog();
            }
        });
    }

    private void updateStudyLabel() {
        studyLabel.setText(StudyStats.formatHours(StudyStats.getInstance().getTotalSec()));
    }

    /** 倒计时结束弹窗 —— 展示今天及之前的未完成事项 */
    private void showFinishDialog() {
        // 收集今天及之前的未完成事项
        LocalDate today = LocalDate.now();
        List<DataManager.Snapshot> undone = new ArrayList<>();
        DataManager dm = DataManager.getInstance();
        for (DataManager.Snapshot s : dm.getUndoneSnapshots()) {
            if (!s.date().isAfter(today)) {
                undone.add(s);
            }
        }

        JDialog dlg = new JDialog(this);
        dlg.setUndecorated(true);
        dlg.setAlwaysOnTop(true);
        dlg.setOpacity(0.94f);
        dlg.setBackground(Config.BG_PINK);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Config.BG_PINK);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new UIUtils.RoundedBorder(14, Config.SAKURA),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JLabel title = new JLabel(" 倒计时结束！", new UIUtils.ClockIcon(16), SwingConstants.LEFT);
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 16));
        title.setForeground(Config.SAKURA);
        panel.add(title, BorderLayout.NORTH);

        if (undone.isEmpty()) {
            JLabel msg = new JLabel("太棒了，所有事项都已完成～", SwingConstants.CENTER);
            msg.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
            msg.setForeground(Config.TEXT_MUTED);
            panel.add(msg, BorderLayout.CENTER);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><div style='font-family:Microsoft YaHei UI;'>");
            sb.append("<span style='color:#BF8A9E; font-size:11px;'>还有 <b>")
              .append(undone.size()).append("</b> 件事没做完：</span><br><br>");
            sb.append("<table cellspacing='0' cellpadding='3' style='font-size:12px;'>");
            sb.append("<tr style='color:#BF8A9E;'><td align='left'><b>日期</b></td>")
              .append("<td align='left'><b>事项</b></td><td align='right'><b>拖延</b></td></tr>");

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM.dd");
            for (DataManager.Snapshot s : undone) {
                String rowColor = s.item().overdueDays() > 7 ? "#FF6B8A" : "#5C3144";
                sb.append("<tr style='color:").append(rowColor).append(";'>");
                sb.append("<td>").append(s.item().createdDate.format(fmt)).append("</td>");
                sb.append("<td>").append(UIUtils.escHtml(s.item().text)).append("</td>");
                sb.append("<td align='right'>").append(s.item().overdueDays()).append("天</td>");
                sb.append("</tr>");
            }
            sb.append("</table></div></html>");

            JLabel msg = new JLabel(sb.toString());
            panel.add(msg, BorderLayout.CENTER);
        }

        JButton closeBtn = new JButton("知道了");
        closeBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(Config.SAKURA);
        closeBtn.setBorder(new UIUtils.RoundedBorder(8, Config.SAKURA));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(100, 30));
        closeBtn.addActionListener(e -> dlg.dispose());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRow.setBackground(Config.BG_PINK);
        btnRow.add(closeBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        dlg.getContentPane().add(panel);
        dlg.pack();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dlg.setLocation((screen.width - dlg.getWidth()) / 2, 20);
        dlg.setVisible(true);

        // 使用 Swing Timer（EDT 轻量计时器，不创建新线程）
        javax.swing.Timer autoClose = new javax.swing.Timer(15000, e -> {
            dlg.dispose();
            ((javax.swing.Timer) e.getSource()).stop();
        });
        autoClose.setRepeats(false);
        autoClose.start();
    }

    // ==================== 添加待办 ====================
    private void openAddTodo() {
        String content = JOptionPane.showInputDialog(this,
                "输入新待办事项：", "✨ 新增待办", JOptionPane.PLAIN_MESSAGE);
        if (content != null && !content.trim().isEmpty()) {
            DataManager.getInstance().addItem(calendarPanel.getSelectedDate(), content.trim());
            DataManager.getInstance().save();
            todoPanel.refresh(calendarPanel.getSelectedDate());
            calendarPanel.repaint();
        }
    }

    // ==================== 系统托盘 ====================
    private void buildTray() {
        if (!SystemTray.isSupported()) return;

        try {
            // 绘制托盘图标
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Config.SAKURA);
            g2.fillOval(1, 1, 14, 14);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(8, 8, 8, 4);
            g2.drawLine(8, 8, 12, 8);
            g2.fillOval(7, 7, 3, 3);
            g2.dispose();

            TrayIcon trayIcon = new TrayIcon(img, "小樱花时钟", createTrayMenu());
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> {
                setVisible(true);
                setState(Frame.NORMAL);
                toFront();
            });

            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            System.err.println("[MainWindow] 系统托盘初始化失败: " + e.getMessage());
        }
    }

    private PopupMenu createTrayMenu() {
        PopupMenu menu = new PopupMenu();

        MenuItem showItem = new MenuItem("显示 / 隐藏");
        showItem.addActionListener(e -> {
            if (isVisible()) {
                setVisible(false);
            } else {
                setVisible(true);
                setState(Frame.NORMAL);
                toFront();
            }
        });
        menu.add(showItem);

        menu.addSeparator();

        MenuItem exitItem = new MenuItem("退出");
        exitItem.addActionListener(e -> requestExit());
        menu.add(exitItem);

        return menu;
    }

    // ==================== 退出流程 ====================
    public void requestExit() {
        // 收集今天及之前的未完成事项，排除未来日期
        LocalDate today = LocalDate.now();
        List<DataManager.Snapshot> all = DataManager.getInstance().getUndoneSnapshots();
        List<DataManager.Snapshot> undone = new ArrayList<>();
        for (DataManager.Snapshot s : all) {
            if (!s.date().isAfter(today)) undone.add(s);
        }
        if (!undone.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><div style='width:380px; font-family:Microsoft YaHei UI;'>");
            sb.append("<h3 style='color:#FF7AA2; margin:0 0 8px 0;'>⚠ 还有未完成的事项</h3>");
            sb.append("<table width='100%' cellspacing='0' cellpadding='4' style='font-size:12px;'>");
            sb.append("<tr style='color:#BF8A9E;'><td align='left'><b>创建时间</b></td>")
              .append("<td align='left'><b>事项</b></td><td align='right'><b>拖延</b></td></tr>");

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            for (DataManager.Snapshot s : undone) {
                String rowColor = s.item().overdueDays() > 7 ? "#FF6B8A" : "#5C3144";
                sb.append("<tr style='color:").append(rowColor).append(";'>");
                sb.append("<td>").append(s.item().createdDate.format(fmt)).append("</td>");
                sb.append("<td>").append(UIUtils.escHtml(s.item().text)).append("</td>");
                sb.append("<td align='right'>").append(s.item().overdueDays()).append("天</td>");
                sb.append("</tr>");
            }
            sb.append("</table></div></html>");

            JOptionPane.showMessageDialog(this, new JLabel(sb.toString()),
                    "关闭提醒 — 还有" + undone.size() + "件事没做完",
                    JOptionPane.WARNING_MESSAGE);
        }

        // 保存所有数据
        DataManager.getInstance().save();
        StudyStats.getInstance().save();
        // 保存窗口位置
        Config.saveWindowPosition(getX(), getY());

        // 释放资源
        CountdownService.getInstance().stop();
        if (clockTimer != null) clockTimer.cancel();
        if (popupWatchdog != null) popupWatchdog.stop();
        if (calendarPopup != null) calendarPopup.dispose();

        // 移除托盘
        if (SystemTray.isSupported()) {
            for (TrayIcon ti : SystemTray.getSystemTray().getTrayIcons()) {
                SystemTray.getSystemTray().remove(ti);
            }
        }

        System.exit(0);
    }

    // ==================== 开机自启 ====================
    /** 每次启动自动注册开机自启（/f 避免弹窗），自动适配 exe/jar */
    public void setAutoStart() {
        if (!Config.AUTO_START) return;
        try {
            String jarUrl = MainWindow.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            java.io.File jarFile = new java.io.File(jarUrl);
            java.io.File jarDir = jarFile.getParentFile();

            // jpackage: jar 在 app/ 子目录，上层有 .exe
            boolean isAppImage = jarDir.getName().equals("app")
                    && new java.io.File(jarDir.getParent(), "FloatTodoClock.exe").exists();

            String target;
            if (isAppImage) {
                target = "\"" + new java.io.File(jarDir.getParent(), "FloatTodoClock.exe").getAbsolutePath() + "\"";
            } else {
                String javaw = System.getProperty("java.home") + java.io.File.separator
                        + "bin" + java.io.File.separator + "javaw.exe";
                target = "\"" + javaw + "\" -jar \"" + jarFile.getAbsolutePath() + "\"";
            }

            String regCmd = "reg add \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run\" "
                    + "/v FloatTodoClock /t REG_SZ /d " + target + " /f";
            Process p = Runtime.getRuntime().exec(new String[]{"cmd", "/c", regCmd});
            p.waitFor();
            System.out.println("[MainWindow] 开机自启 " + (p.exitValue() == 0 ? "成功" : "失败"));
        } catch (Exception e) {
            System.err.println("[MainWindow] 开机自启失败: " + e.getMessage());
        }
    }
}
