package clock;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 待办数据管理 —— 增删改查 + 文件持久化 + 每日例行任务 + 完成日志。
 * 数据格式完全兼容旧版 FloatTodoClock。
 */
public class DataManager {

    // ==================== 数据模型 ====================
    public static class TodoItem {
        public String text;
        public boolean done;
        public LocalDate createdDate;

        public TodoItem(String text, LocalDate createdDate) {
            this.text = text; this.createdDate = createdDate;
        }
        public TodoItem(String text, boolean done, LocalDate createdDate) {
            this.text = text; this.done = done; this.createdDate = createdDate;
        }
        /** 距离今天拖延了多少天 */
        public long overdueDays() {
            return ChronoUnit.DAYS.between(createdDate, LocalDate.now());
        }
    }

    // ==================== 核心数据 ====================
    private final Map<LocalDate, List<TodoItem>> todoMap = new LinkedHashMap<>();

    // ==================== 单例 ====================
    private static DataManager instance;
    public static DataManager getInstance() {
        if (instance == null) instance = new DataManager();
        return instance;
    }
    private DataManager() {}

    // ==================== CRUD ====================
    public List<TodoItem> getItems(LocalDate date) {
        return todoMap.getOrDefault(date, Collections.emptyList());
    }

    public void addItem(LocalDate date, String text) {
        todoMap.computeIfAbsent(date, k -> new ArrayList<>())
               .add(new TodoItem(text, date));
    }

    public void toggleItem(LocalDate date, int index, boolean done) {
        List<TodoItem> items = todoMap.get(date);
        if (items != null && index >= 0 && index < items.size()) {
            items.get(index).done = done;
        }
    }

    public void deleteItem(LocalDate date, int index) {
        List<TodoItem> items = todoMap.get(date);
        if (items != null && index >= 0 && index < items.size()) {
            items.remove(index);
            if (items.isEmpty()) todoMap.remove(date);
        }
    }

    public boolean hasUndoneItems(LocalDate date) {
        return getItems(date).stream().anyMatch(t -> !t.done);
    }

    // ==================== 每日例行任务 ====================
    /** 按需补全指定日期的例行任务（仅今天及未来，过去日期不补） */
    public void ensureRoutineFor(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) return;  // 过去日期不自动补例行任务

        LocalDate end;
        try {
            end = LocalDate.parse(Config.ROUTINE_END);
        } catch (Exception e) {
            return;
        }
        if (date.isAfter(end)) return;

        List<TodoItem> items = todoMap.computeIfAbsent(date, k -> new ArrayList<>());
        for (String task : Config.DAILY_ROUTINE) {
            boolean exists = items.stream().anyMatch(t -> t.text.equals(task));
            if (!exists) items.add(new TodoItem(task, date));
        }
    }

    // ==================== 完成日志 ====================
    /** 记录一条完成事项到日志文件 */
    public void logCompletion(TodoItem item) {
        Config.ensureDataDir();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.M.d HH:mm");
            String line = sdf.format(new Date()) + " 完成了：" + item.text + System.lineSeparator();
            try (FileOutputStream fos = new FileOutputStream(Config.LOG_FILE.toFile(), true);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 BufferedWriter bw = new BufferedWriter(osw)) {
                bw.write(line);
                bw.flush();
            }
        } catch (IOException e) {
            System.err.println("[DataManager] 日志写入失败: " + e.getMessage());
        }
    }

    // ==================== 持久化：保存 ====================
    public void save() {
        Config.ensureDataDir();
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(Config.TODO_FILE.toFile()), StandardCharsets.UTF_8))) {
            for (Map.Entry<LocalDate, List<TodoItem>> entry : todoMap.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                bw.write("DATE|" + entry.getKey().toString());
                bw.newLine();
                for (TodoItem item : entry.getValue()) {
                    bw.write("ITEM|" + item.done + "|" + item.createdDate + "|" + item.text);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("[DataManager] 保存失败: " + e.getMessage());
        }
    }

    // ==================== 持久化：加载 ====================
    public void load() {
        Config.ensureDataDir();
        // 优先加载 data/ 目录，不存在则尝试从旧位置迁移
        Path src = Config.TODO_FILE;
        if (!Files.exists(src)) {
            src = migrateOldFile("FloatTodoClock_数据.txt", src);
        }
        if (!Files.exists(src)) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(src.toFile()), StandardCharsets.UTF_8))) {
            LocalDate currentDate = null;
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("DATE|")) {
                    currentDate = LocalDate.parse(line.substring(5));
                    todoMap.putIfAbsent(currentDate, new ArrayList<>());
                } else if (line.startsWith("ITEM|") && currentDate != null) {
                    // 格式: ITEM|done|createdDate|内容  (4段，新版)
                    // 格式: ITEM|done|内容            (3段，旧版兼容)
                    String rest = line.substring(5);
                    int pipe1 = rest.indexOf('|');
                    if (pipe1 > 0) {
                        boolean done = Boolean.parseBoolean(rest.substring(0, pipe1));
                        String afterDone = rest.substring(pipe1 + 1);
                        int pipe2 = afterDone.indexOf('|');
                        LocalDate cd;
                        String text;
                        if (pipe2 > 0) {
                            cd = LocalDate.parse(afterDone.substring(0, pipe2));
                            text = afterDone.substring(pipe2 + 1);
                        } else {
                            cd = currentDate;
                            text = afterDone;
                        }
                        todoMap.get(currentDate).add(new TodoItem(text, done, cd));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DataManager] 加载失败: " + e.getMessage());
        }
    }

    // ==================== 数据迁移 ====================
    /** 从旧位置复制文件到 data/，不删除原文件 */
    private Path migrateOldFile(String filename, Path target) {
        Path oldFile = Config.OLD_DATA_DIR.resolve(filename);
        if (Files.exists(oldFile)) {
            try {
                Config.ensureDataDir();
                Files.copy(oldFile, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[DataManager] 已迁移: " + oldFile + " → " + target);
                return target;
            } catch (IOException e) {
                System.err.println("[DataManager] 迁移失败: " + e.getMessage());
            }
        }
        return target;
    }

    // ==================== 收集未完成项（供关闭提醒使用） ====================
    public record Snapshot(LocalDate date, TodoItem item) {}

    public List<Snapshot> getUndoneSnapshots() {
        List<Snapshot> undone = new ArrayList<>();
        for (Map.Entry<LocalDate, List<TodoItem>> entry : todoMap.entrySet()) {
            for (TodoItem item : entry.getValue()) {
                if (!item.done) undone.add(new Snapshot(entry.getKey(), item));
            }
        }
        undone.sort((a, b) -> Long.compare(b.item.overdueDays(), a.item.overdueDays()));
        return undone;
    }
}
