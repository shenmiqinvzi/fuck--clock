package clock;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * 全局配置管理 —— 常量定义 + config.properties 读写。
 * 所有颜色、字体、默认值集中在这里，方便统一调整。
 */
public class Config {

    // ==================== 文件路径 ====================
    /** 项目根目录（clock/ 的父目录） */
    public static final Path ROOT_DIR = findRootDir();
    /** 数据目录 */
    public static final Path DATA_DIR = ROOT_DIR.resolve("data");
    /** 旧项目数据位置（用于自动迁移） */
    public static final Path OLD_DATA_DIR = Paths.get(
            System.getProperty("user.home"), "Desktop", "AI工具栏");

    // 数据文件
    public static final Path TODO_FILE    = DATA_DIR.resolve("FloatTodoClock_数据.txt");
    public static final Path LOG_FILE     = DATA_DIR.resolve("已完成事项.txt");
    public static final Path STUDY_FILE   = DATA_DIR.resolve("学习时长_数据.txt");

    // ==================== 窗口默认值（可被 config.properties 覆盖） ====================
    public static int WIN_X = 1050;
    public static int WIN_Y = 200;
    public static int WIN_W = 235;
    public static int WIN_H = 146;
    public static float WIN_OPACITY = 0.93f;

    // ==================== 每日例行任务（可配置） ====================
    public static String[] DAILY_ROUTINE = {
        "背至少50个单词", "写一篇翻译", "听听力"
    };
    public static String ROUTINE_END = "2026-12-10";

    // ==================== 少女粉色系 ====================
    public static Color BG_PINK       = new Color(0xFFE4EC);
    public static Color CARD_BG       = new Color(0xFFFFFF);
    public static Color SAKURA        = new Color(0xFF7AA2);
    public static Color TEXT_DARK     = new Color(0x5C3144);
    public static Color TEXT_MUTED    = new Color(0xBF8A9E);
    public static Color DONE_COLOR    = new Color(0xC0A8B5);
    public static Color CARD_BORDER   = new Color(0xF0C8D8);
    public static Color DELETE_PINK   = new Color(0xFF6B8A);
    public static Color BTN_HOVER     = new Color(0xFF5A7D);
    public static Color TODAY_BG      = new Color(0xFFE0EA);

    // ==================== 热力图颜色 ====================
    public static Color HEAT_EMPTY    = new Color(0xEBEDF0);  // 学习时长1小时内
    public static Color HEAT_LIGHT    = new Color(0x9BE9A8);  // 1~3h
    public static Color HEAT_MEDIUM   = new Color(0x40C463);  // 3~5h
    public static Color HEAT_DEEP     = new Color(0x30A14E);  // 5~6.5h
    public static Color HEAT_DARK     = new Color(0x216E39);  // 6.5h+
    public static Color OVERDUE_RED   = new Color(0xB71C1C);  // 逾期未完成

    // ==================== 字体 ====================
    public static final Font CLOCK_FONT      = new Font("Microsoft YaHei UI", Font.BOLD, 24);
    public static final Font TITLE_FONT      = new Font("Microsoft YaHei UI", Font.BOLD, 13);
    public static final Font NORMAL_FONT     = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
    public static final Font SMALL_FONT      = new Font("Microsoft YaHei UI", Font.PLAIN, 10);
    public static final Font CAL_HEADER_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 14);
    public static final Font CAL_DATE_FONT   = new Font("Microsoft YaHei UI", Font.PLAIN, 12);

    // ==================== 开机自启 ====================
    public static boolean AUTO_START = true;

    // ==================== 加载 config.properties ====================
    static {
        Path cfgFile = ROOT_DIR.resolve("config.properties");
        if (Files.exists(cfgFile)) {
            Properties p = new Properties();
            try (InputStreamReader r = new InputStreamReader(
                    new FileInputStream(cfgFile.toFile()), StandardCharsets.UTF_8)) {
                p.load(r);
                WIN_X = intProp(p, "window.x", WIN_X);
                WIN_Y = intProp(p, "window.y", WIN_Y);
                WIN_W = intProp(p, "window.width", WIN_W);
                WIN_H = intProp(p, "window.height", WIN_H);
                WIN_OPACITY = floatProp(p, "window.opacity", WIN_OPACITY);
                String tasks = p.getProperty("routine.tasks");
                if (tasks != null && !tasks.trim().isEmpty()) {
                    DAILY_ROUTINE = tasks.split(",");
                    for (int i = 0; i < DAILY_ROUTINE.length; i++) {
                        DAILY_ROUTINE[i] = DAILY_ROUTINE[i].trim();
                    }
                }
                ROUTINE_END = p.getProperty("routine.end", ROUTINE_END);
                BG_PINK     = colorProp(p, "theme.bg.pink", BG_PINK);
                SAKURA      = colorProp(p, "theme.sakura", SAKURA);
                TEXT_DARK   = colorProp(p, "theme.text.dark", TEXT_DARK);
                TEXT_MUTED  = colorProp(p, "theme.text.muted", TEXT_MUTED);
                AUTO_START  = boolProp(p, "auto.start", AUTO_START);
            } catch (Exception e) {
                System.err.println("[Config] 配置文件加载失败，使用默认值: " + e.getMessage());
            }
        }
    }

    // ==================== 保存窗口位置 ====================
    public static void saveWindowPosition(int x, int y) {
        Path cfgFile = ROOT_DIR.resolve("config.properties");
        Properties p = new Properties();
        // 先读取已有内容
        if (Files.exists(cfgFile)) {
            try (InputStreamReader r = new InputStreamReader(
                    new FileInputStream(cfgFile.toFile()), StandardCharsets.UTF_8)) {
                p.load(r);
            } catch (Exception ignored) {}
        }
        p.setProperty("window.x", String.valueOf(x));
        p.setProperty("window.y", String.valueOf(y));
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(cfgFile.toFile()), StandardCharsets.UTF_8)) {
            p.store(w, "樱花时钟 — 配置文件");
        } catch (Exception e) {
            System.err.println("[Config] 保存窗口位置失败: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================
    private static int intProp(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key)); } catch (Exception e) { return def; }
    }
    private static float floatProp(Properties p, String key, float def) {
        try { return Float.parseFloat(p.getProperty(key)); } catch (Exception e) { return def; }
    }
    private static boolean boolProp(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v != null ? Boolean.parseBoolean(v) : def;
    }
    private static Color colorProp(Properties p, String key, Color def) {
        try {
            String hex = p.getProperty(key);
            if (hex != null && hex.length() == 6) {
                return new Color(Integer.parseInt(hex, 16));
            }
        } catch (Exception ignored) {}
        return def;
    }

    /** 根据 class 文件路径推算项目根目录 */
    private static Path findRootDir() {
        try {
            String url = Config.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            Path start = Paths.get(url);
            // start 可能是目录（如 bin/）或 jar 文件
            if (!Files.isDirectory(start)) start = start.getParent();

            // 从 class 文件位置向上查找有 config.properties 或 clock/ 源码的目录
            Path p = start;
            while (p != null) {
                if (Files.exists(p.resolve("config.properties"))) return p;
                // 如果当前目录有 clock/ 子目录且里面有 .java 文件，说明是项目根目录
                Path clockSrc = p.resolve("clock");
                if (Files.isDirectory(clockSrc)) {
                    try (var s = Files.list(clockSrc)) {
                        if (s.anyMatch(f -> f.getFileName().toString().endsWith(".java"))) {
                            return p;
                        }
                    }
                }
                p = p.getParent();
            }
        } catch (Exception ignored) {}
        return Paths.get("").toAbsolutePath();
    }

    /** 确保 data/ 目录存在 */
    public static void ensureDataDir() {
        try { Files.createDirectories(DATA_DIR); } catch (Exception ignored) {}
    }
}
