package clock;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 待办列表面板 —— 显示选中日期的待办事项。
 * 支持勾选完成、删除、内联添加。
 */
public class TodoPanel extends JPanel {

    private JPanel itemsPanel;
    private JTextField addField;
    private JLabel titleLabel;
    private JLabel todayStudyTitle;
    private LocalDate currentDate = LocalDate.now();
    private Runnable onChanged;  // 数据变更时通知外部（刷新日历）

    // ==================== 构造 ====================
    public TodoPanel() {
        setBackground(Config.CARD_BG);
        setLayout(new BorderLayout(0, 4));

        // 标题行
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setBackground(Config.CARD_BG);
        titleLabel = new JLabel();
        titleLabel.setFont(Config.TITLE_FONT);
        titleLabel.setForeground(Config.TEXT_DARK);
        titleRow.add(titleLabel, BorderLayout.WEST);

        todayStudyTitle = new JLabel("", new UIUtils.ClockIcon(11), SwingConstants.RIGHT);
        todayStudyTitle.setFont(Config.SMALL_FONT);
        todayStudyTitle.setForeground(Config.SAKURA);
        titleRow.add(todayStudyTitle, BorderLayout.EAST);
        add(titleRow, BorderLayout.NORTH);

        // 待办列表（可滚动）
        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(Config.CARD_BG);
        JScrollPane scroll = new JScrollPane(itemsPanel);
        scroll.setBorder(null);
        scroll.setBackground(Config.CARD_BG);
        scroll.getViewport().setBackground(Config.CARD_BG);
        scroll.setPreferredSize(new Dimension(280, 90));
        add(scroll, BorderLayout.CENTER);

        // 底部添加栏
        JPanel addRow = new JPanel(new BorderLayout(5, 0));
        addRow.setBackground(Config.CARD_BG);
        addField = new JTextField();
        addField.setFont(Config.NORMAL_FONT);
        addField.addActionListener(e -> doAdd());
        JButton addBtn = new JButton("＋");
        addBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        addBtn.setForeground(Color.WHITE);
        addBtn.setBackground(Config.SAKURA);
        addBtn.setBorder(new UIUtils.RoundedBorder(7, Config.SAKURA));
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setPreferredSize(new Dimension(36, 26));
        addBtn.addActionListener(e -> doAdd());
        addRow.add(addField, BorderLayout.CENTER);
        addRow.add(addBtn, BorderLayout.EAST);
        add(addRow, BorderLayout.SOUTH);
    }

    // ==================== 公开 API ====================
    public void setOnChanged(Runnable r) { this.onChanged = r; }

    /** 刷新当前日期的待办列表 */
    public void refresh(LocalDate date) {
        this.currentDate = date;

        DataManager dm = DataManager.getInstance();
        dm.ensureRoutineFor(date);  // 按需生成该日期的例行任务
        itemsPanel.removeAll();
        List<DataManager.TodoItem> items = dm.getItems(date);

        if (items.isEmpty()) {
            JLabel empty = new JLabel("  暂无待办，点击下方添加～", SwingConstants.LEFT);
            empty.setFont(Config.SMALL_FONT);
            empty.setForeground(Config.TEXT_MUTED);
            itemsPanel.add(empty);
        } else {
            // 未完成在前，已完成在后
            List<DataManager.TodoItem> sorted = new java.util.ArrayList<>(items);
            sorted.sort((a, b) -> Boolean.compare(a.done, b.done));
            for (int i = 0; i < sorted.size(); i++) {
                itemsPanel.add(createItemRow(sorted.get(i), items));
            }
        }

        // 更新标题日期
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M月d日");
        titleLabel.setText(date.format(fmt) + " 待办");

        // 更新今日学习时长
        StudyStats ss = StudyStats.getInstance();
        boolean isToday = date.equals(LocalDate.now());
        todayStudyTitle.setText(isToday ? " " + StudyStats.formatHours(ss.getTodaySec()) : "");

        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    // ==================== 内部 ====================
    /** 创建单条待办行 */
    private JPanel createItemRow(DataManager.TodoItem item, List<DataManager.TodoItem> originalList) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(Config.CARD_BG);
        row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        // 勾选框
        JCheckBox cb = new JCheckBox();
        cb.setSelected(item.done);
        cb.setBackground(Config.CARD_BG);
        cb.addActionListener(e -> {
            item.done = cb.isSelected();
            if (item.done) DataManager.getInstance().logCompletion(item);
            saveAndNotify();
        });

        // 文字
        JLabel label = new JLabel();
        label.setFont(Config.NORMAL_FONT);
        if (item.done) {
            String hex = String.format("%06x", Config.DONE_COLOR.getRGB() & 0xFFFFFF);
            label.setText("<html><strike><font color='#" + hex + "'>"
                    + UIUtils.escHtml(item.text) + "</font></strike></html>");
        } else {
            label.setText(item.text);
            label.setForeground(Config.TEXT_DARK);
        }
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        JPanel left = new JPanel(new BorderLayout(2, 0));
        left.setBackground(Config.CARD_BG);
        left.add(cb, BorderLayout.WEST);
        left.add(label, BorderLayout.CENTER);

        // 删除按钮
        JButton delBtn = new JButton("x");
        delBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 9));
        delBtn.setForeground(Config.DELETE_PINK);
        delBtn.setBackground(Config.CARD_BG);
        delBtn.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        delBtn.setFocusPainted(false);
        delBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delBtn.setToolTipText("删除");
        delBtn.addActionListener(e -> {
            int idx = originalList.indexOf(item);
            if (idx >= 0) {
                DataManager.getInstance().deleteItem(currentDate, idx);
                saveAndNotify();
            }
        });

        row.add(left, BorderLayout.CENTER);
        row.add(delBtn, BorderLayout.EAST);
        return row;
    }

    private void doAdd() {
        String text = addField.getText().trim();
        if (text.isEmpty()) return;
        DataManager.getInstance().addItem(currentDate, text);
        addField.setText("");
        saveAndNotify();
    }

    private void saveAndNotify() {
        DataManager.getInstance().save();
        refresh(currentDate);
        if (onChanged != null) onChanged.run();
    }
}
