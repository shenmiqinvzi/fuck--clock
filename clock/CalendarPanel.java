package clock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * 日历面板 —— 月份导航 + 7×6 日期网格 + 热力图 + 逾期标记。
 * 点击日期即可切换 selectedDate，外部通过回调感知。
 */
public class CalendarPanel extends JPanel {

    // ==================== 回调 ====================
    public interface OnDateSelected {
        void selected(LocalDate date);
    }

    // ==================== 状态 ====================
    private YearMonth currentMonth;
    private LocalDate selectedDate = LocalDate.now();
    private JLabel monthLabel;
    private OnDateSelected onDateSelected;

    private static final int WEEKDAY_H = 20;
    private static final int CELL_H = 33;

    // ==================== 构造 ====================
    public CalendarPanel() {
        setBackground(Config.CARD_BG);
        setLayout(new BorderLayout());
        currentMonth = YearMonth.from(selectedDate);

        // 月份导航栏
        JPanel navBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                UIUtils.drawSakuraFlower(g2, w / 2 - 55, getHeight() / 2 + 1, 12, Config.SAKURA, 0.18f);
                UIUtils.drawSakuraFlower(g2, w / 2 + 55, getHeight() / 2 + 1, 12, Config.SAKURA, 0.18f);
                g2.dispose();
            }
        };
        navBar.setBackground(Config.CARD_BG);
        navBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        JButton prevBtn = navBtn("<");
        prevBtn.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); updateLabel(); repaint(); });
        JButton nextBtn = navBtn(">");
        nextBtn.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); updateLabel(); repaint(); });

        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(Config.CAL_HEADER_FONT);
        monthLabel.setForeground(Config.TEXT_DARK);
        updateLabel();

        navBar.add(prevBtn, BorderLayout.WEST);
        navBar.add(monthLabel, BorderLayout.CENTER);
        navBar.add(nextBtn, BorderLayout.EAST);
        add(navBar, BorderLayout.NORTH);

        // 日期网格
        JPanel grid = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawCalendar((Graphics2D) g);
            }
        };
        grid.setBackground(Config.CARD_BG);
        grid.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                LocalDate clicked = dateAtPoint(e.getX(), e.getY(), grid.getWidth());
                if (clicked != null) {
                    selectedDate = clicked;
                    if (!YearMonth.from(clicked).equals(currentMonth)) {
                        currentMonth = YearMonth.from(clicked);
                        updateLabel();
                    }
                    if (onDateSelected != null) onDateSelected.selected(clicked);
                    repaint();
                }
            }
        });
        add(grid, BorderLayout.CENTER);
    }

    // ==================== 公开 API ====================
    public void setOnDateSelected(OnDateSelected cb) { this.onDateSelected = cb; }
    public LocalDate getSelectedDate() { return selectedDate; }
    public void setSelectedDate(LocalDate d) { this.selectedDate = d; }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(300, WEEKDAY_H + 6 * CELL_H + 30);
    }

    // ==================== 内部绘制 ====================
    private JButton navBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        btn.setForeground(Config.SAKURA);
        btn.setBackground(Config.CARD_BG);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void updateLabel() {
        monthLabel.setText(currentMonth.getYear() + "年" + currentMonth.getMonthValue() + "月");
    }

    private void drawCalendar(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int cellW = w / 7;
        String[] weekdays = {"日", "一", "二", "三", "四", "五", "六"};

        g2.setFont(Config.SMALL_FONT);
        g2.setColor(Config.TEXT_MUTED);
        for (int i = 0; i < 7; i++) {
            int sw = g2.getFontMetrics().stringWidth(weekdays[i]);
            g2.drawString(weekdays[i], i * cellW + (cellW - sw) / 2, WEEKDAY_H - 4);
        }

        int daysInMonth = currentMonth.lengthOfMonth();
        int firstDow = currentMonth.atDay(1).getDayOfWeek().getValue() % 7;
        LocalDate today = LocalDate.now();
        DataManager dm = DataManager.getInstance();
        StudyStats ss = StudyStats.getInstance();

        g2.setFont(Config.CAL_DATE_FONT);
        FontMetrics fm = g2.getFontMetrics();
        int gap = 3;

        for (int day = 1; day <= daysInMonth; day++) {
            int idx = firstDow + day - 1;
            int col = idx % 7;
            int row = idx / 7;
            if (row >= 6) break;

            int rx = col * cellW + gap;
            int ry = WEEKDAY_H + row * CELL_H + gap;
            int rw = cellW - 2 * gap;
            int rh = CELL_H - 2 * gap;
            int cx = col * cellW + cellW / 2;
            int cy = WEEKDAY_H + row * CELL_H + CELL_H / 2;

            LocalDate date = currentMonth.atDay(day);
            boolean isToday = date.equals(today);
            boolean isSel = date.equals(selectedDate);
            boolean isPast = date.isBefore(today);
            java.util.List<DataManager.TodoItem> todos = dm.getItems(date);
            boolean hasUndone = todos.stream().anyMatch(t -> !t.done);

            Color bgColor = null;
            boolean whiteText = false;

            if (isSel) {
                bgColor = Config.SAKURA;
                whiteText = true;
            } else if (!date.isAfter(today) && hasUndone) {
                bgColor = Config.OVERDUE_RED;
                whiteText = true;
            } else if (isPast) {
                long study = ss.getDailyMap().getOrDefault(date, 0L);
                bgColor = StudyStats.heatColor(study);
                whiteText = (study >= 3600 * 3);
            } else if (isToday) {
                long study = ss.getTodaySec();
                bgColor = StudyStats.heatColor(study);
                whiteText = (study >= 3600 * 3);
            }

            if (bgColor != null) {
                g2.setColor(bgColor);
                g2.fillRoundRect(rx, ry, rw, rh, 5, 5);
                g2.setColor(whiteText ? Color.WHITE : Config.TEXT_DARK);
            } else {
                g2.setColor(Config.TEXT_DARK);
            }

            String num = String.valueOf(day);
            int sw = fm.stringWidth(num);
            g2.drawString(num, cx - sw / 2, cy + fm.getAscent() / 2 - 1);

            // 全部完成 → 右下角小三角
            if (!hasUndone && !todos.isEmpty()) {
                g2.setColor((bgColor != null && whiteText) ? new Color(255, 255, 255, 180) : Config.SAKURA);
                int tx = rx + rw - 9, ty = ry + rh - 9;
                g2.fillPolygon(new int[]{tx, tx + 7, tx + 7}, new int[]{ty + 7, ty, ty + 7}, 3);
            }
        }
    }

    private LocalDate dateAtPoint(int px, int py, int panelW) {
        int cellW = panelW / 7;
        int row = (py - WEEKDAY_H) / CELL_H;
        int col = px / cellW;
        if (row < 0 || col < 0 || col >= 7) return null;
        int idx = row * 7 + col;
        int firstDow = currentMonth.atDay(1).getDayOfWeek().getValue() % 7;
        int day = idx - firstDow + 1;
        if (day < 1 || day > currentMonth.lengthOfMonth()) return null;
        return currentMonth.atDay(day);
    }
}
