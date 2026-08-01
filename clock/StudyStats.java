package clock;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * 学习时长统计 —— 总计/今日/每日明细 + 文件持久化 + 热力图颜色。
 * 数据格式完全兼容旧版。
 */
public class StudyStats {

    // ==================== 数据 ====================
    private long totalStudySec = 0;
    private long todayStudySec = 0;
    private LocalDate todayStudyDate = LocalDate.now();
    private final Map<LocalDate, Long> dailyStudyMap = new LinkedHashMap<>();

    // ==================== 单例 ====================
    private static StudyStats instance;
    public static StudyStats getInstance() {
        if (instance == null) instance = new StudyStats();
        return instance;
    }
    private StudyStats() {}

    // ==================== 读写 ====================
    public long getTotalSec() { return totalStudySec; }
    public long getTodaySec() { checkTodayReset(); return todayStudySec; }
    public Map<LocalDate, Long> getDailyMap() { return dailyStudyMap; }

    /** 跨天自动归零今日时长 */
    private void checkTodayReset() {
        if (!LocalDate.now().equals(todayStudyDate)) {
            todayStudyDate = LocalDate.now();
            todayStudySec = 0;
        }
    }

    /** 累加学习时长（倒计时正常结束才调用） */
    public void accumulate(long seconds) {
        if (seconds <= 0) return;
        checkTodayReset();
        totalStudySec += seconds;
        todayStudySec += seconds;
        dailyStudyMap.merge(LocalDate.now(), seconds, Long::sum);
    }

    // ==================== 格式化 ====================
    /** 格式化秒为 xxh（例如 "2.5h"、"0h"） */
    public static String formatHours(long totalSec) {
        if (totalSec < 60) return "0h";
        return String.format("%.1fh", totalSec / 3600.0);
    }

    // ==================== 热力图颜色 ====================
    /** 根据学习秒数返回对应热力图颜色 */
    public static Color heatColor(long sec) {
        double h = sec / 3600.0;
        if (h < 1.0)   return Config.HEAT_EMPTY;   // 1h内 → 浅灰
        if (h < 3.0)   return Config.HEAT_LIGHT;   // 1~3h → 浅绿
        if (h < 5.0)   return Config.HEAT_MEDIUM;  // 3~5h → 中绿
        if (h < 6.5)   return Config.HEAT_DEEP;    // 5~6.5h → 深绿
        return Config.HEAT_DARK;                    // 6.5h+ → 最深绿
    }

    // ==================== 持久化 ====================
    public void save() {
        Config.ensureDataDir();
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(Config.STUDY_FILE.toFile()), StandardCharsets.UTF_8))) {
            bw.write("TOTAL|" + totalStudySec); bw.newLine();
            bw.write("TODAY|" + todayStudyDate + "|" + todayStudySec); bw.newLine();
            for (Map.Entry<LocalDate, Long> e : dailyStudyMap.entrySet()) {
                bw.write("DAY|" + e.getKey() + "|" + e.getValue()); bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("[StudyStats] 保存失败: " + e.getMessage());
        }
    }

    public void load() {
        Config.ensureDataDir();
        Path src = Config.STUDY_FILE;
        if (!Files.exists(src)) {
            src = migrateOldFile("学习时长_数据.txt", src);
        }
        if (!Files.exists(src)) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(src.toFile()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("DAY|")) {
                    String rest = line.substring(4);
                    int pipe = rest.indexOf('|');
                    if (pipe > 0) {
                        dailyStudyMap.put(LocalDate.parse(rest.substring(0, pipe)),
                                Long.parseLong(rest.substring(pipe + 1)));
                    }
                } else if (line.startsWith("TOTAL|")) {
                    totalStudySec = Long.parseLong(line.substring(6));
                } else if (line.startsWith("TODAY|")) {
                    String rest = line.substring(6);
                    int pipe = rest.indexOf('|');
                    if (pipe > 0) {
                        todayStudyDate = LocalDate.parse(rest.substring(0, pipe));
                        todayStudySec = Long.parseLong(rest.substring(pipe + 1));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[StudyStats] 加载失败: " + e.getMessage());
        }
    }

    /** 从旧位置迁移学习时长文件 */
    private Path migrateOldFile(String filename, Path target) {
        Path oldFile = Config.OLD_DATA_DIR.resolve(filename);
        if (Files.exists(oldFile)) {
            try {
                Config.ensureDataDir();
                Files.copy(oldFile, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[StudyStats] 已迁移: " + oldFile + " → " + target);
                return target;
            } catch (IOException e) {
                System.err.println("[StudyStats] 迁移失败: " + e.getMessage());
            }
        }
        return target;
    }
}
