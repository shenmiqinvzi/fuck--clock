package clock;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 共享 UI 工具 —— 图标、边框、装饰绘制、按钮工厂。
 * 所有方法都是静态的，不依赖任何业务数据。
 */
public class UIUtils {

    // ==================== 手绘樱花图标 ====================
    public static class SakuraIcon implements Icon {
        private final int s;
        private final Color color;
        public SakuraIcon(int size, Color color) { this.s = size; this.color = color; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawSakuraFlower(g2, x + s / 2, y + s / 2, s, color, 0.72f);
            g2.dispose();
        }
        @Override public int getIconWidth() { return s; }
        @Override public int getIconHeight() { return s; }
    }

    // ==================== 手绘时钟图标 ====================
    public static class ClockIcon implements Icon {
        private final int s;
        public ClockIcon(int s) { this.s = s; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Config.SAKURA);
            g2.setStroke(new BasicStroke(1.2f));
            int cx = x + s / 2, cy = y + s / 2;
            g2.drawOval(x + 1, y + 1, s - 3, s - 3);
            g2.drawLine(cx, cy, cx, y + 3);
            g2.drawLine(cx, cy, x + s - 4, cy);
            g2.fillOval(cx - 1, cy - 1, 3, 3);
            g2.dispose();
        }
        @Override public int getIconWidth() { return s; }
        @Override public int getIconHeight() { return s; }
    }

    // ==================== 圆角边框 ====================
    public static class RoundedBorder implements Border {
        private final int radius;
        private final Color borderColor;
        public RoundedBorder(int radius, Color borderColor) {
            this.radius = radius;
            this.borderColor = borderColor;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            int pad = radius / 2 + 1;
            return new Insets(pad, pad, pad, pad);
        }
        @Override public boolean isBorderOpaque() { return false; }
    }

    // ==================== 装饰背景面板 ====================
    /** 带散落樱花 + 星星 + 圆点装饰的 JPanel */
    public static class DecoPanel extends JPanel {
        private final int[][] flowers, stars, dots;
        public DecoPanel(LayoutManager layout, int[][] flowers, int[][] stars, int[][] dots) {
            super(layout);
            this.flowers = flowers;
            this.stars = stars;
            this.dots = dots;
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int[] f : flowers) drawSakuraFlower(g2, f[0], f[1], f[2], Config.SAKURA, 0.20f);
            for (int[] s : stars) drawStar(g2, s[0], s[1], s[2], new Color(0xFFB0C8), 0.25f);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
            g2.setColor(Config.SAKURA);
            for (int[] d : dots) g2.fillOval(d[0] - d[2] / 2, d[1] - d[2] / 2, d[2], d[2]);
            g2.dispose();
        }
    }

    // ==================== 手绘图形 ====================
    /** 绘制五瓣樱花 */
    public static void drawSakuraFlower(Graphics2D g2, int cx, int cy, int size, Color color, float alpha) {
        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72 - 90);
            int px = cx + (int) (size * 0.30 * Math.cos(angle));
            int py = cy + (int) (size * 0.30 * Math.sin(angle));
            Graphics2D g2p = (Graphics2D) g2.create();
            g2p.translate(px, py);
            g2p.rotate(angle + Math.PI / 2);
            g2p.setColor(color);
            g2p.fillOval(-size / 5, -size / 2 + size / 4, size * 2 / 5, size * 2 / 3);
            g2p.dispose();
        }
        g2.setColor(color.darker());
        g2.fillOval(cx - size / 6, cy - size / 6, size / 3, size / 3);
        g2.setComposite(original);
    }

    /** 绘制四角小星星 */
    public static void drawStar(Graphics2D g2, int cx, int cy, int r, Color color, float alpha) {
        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(color);
        int[] xs = {cx, cx + r / 3, cx + r, cx + r / 3, cx, cx - r / 3, cx - r, cx - r / 3};
        int[] ys = {cy - r, cy - r / 3, cy, cy + r / 3, cy + r, cy + r / 3, cy, cy - r / 3};
        g2.fillPolygon(xs, ys, 8);
        g2.setComposite(original);
    }

    // ==================== 工厂方法 ====================
    /** 创建一个小号圆角按钮 */
    public static JButton mkSmallBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(Config.SMALL_FONT);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setBorder(new RoundedBorder(7, bg));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(Config.BTN_HOVER); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    /** 将组件包装进白色圆角卡片 */
    public static JPanel wrapCard(JComponent content, int width, int height) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Config.CARD_BG);
        card.setBorder(new RoundedBorder(14, Config.CARD_BORDER));
        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Config.CARD_BG);
        inner.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        inner.add(content, BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(width, height));
        card.setMaximumSize(new Dimension(width, height));
        card.setMinimumSize(new Dimension(width, height));
        return card;
    }

    /** 给卡片四角加樱花装饰 */
    public static JPanel wrapCardDeco(JComponent content, int width, int height) {
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight(), m = 6;
                drawSakuraFlower(g2, m + 8, m + 8, 14, Config.SAKURA, 0.22f);
                drawSakuraFlower(g2, w - m - 8, m + 8, 14, Config.SAKURA, 0.22f);
                drawSakuraFlower(g2, m + 8, h - m - 8, 14, Config.SAKURA, 0.22f);
                drawSakuraFlower(g2, w - m - 8, h - m - 8, 14, Config.SAKURA, 0.22f);
                g2.dispose();
            }
        };
        outer.setBackground(Config.BG_PINK);
        outer.setOpaque(true);
        outer.add(wrapCard(content, width, height), BorderLayout.CENTER);
        return outer;
    }

    /** HTML 转义 */
    public static String escHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
