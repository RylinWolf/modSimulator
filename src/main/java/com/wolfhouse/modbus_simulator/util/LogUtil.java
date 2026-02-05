package com.wolfhouse.modbus_simulator.util;

import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * 日志工具类
 *
 * @author Junie
 */
public class LogUtil {
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LOG_TIME_FORMATTER  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static Path    currentLogFile;
    private static boolean initialized = false;

    private LogUtil() {}

    /**
     * 初始化日志工具
     */
    public static synchronized void init(Stage based) {
        if (initialized) {
            return;
        }

        Path logDir = Path.of(SystemUtil.LOG_DIR_PATH);
        if (!Files.exists(logDir)) {
            Platform.runLater(() -> WindowUtil.showAlert(Alert.AlertType.WARNING,
                                                         "日志初始化提示",
                                                         "日志目录不存在",
                                                         "目录 [" + logDir.toAbsolutePath() + "] 不存在，日志将不会被记录。",
                                                         based,
                                                         ButtonType.OK));
            return;
        }

        String fileName = LocalDate.now().format(FILE_NAME_FORMATTER) + ".log";
        currentLogFile = logDir.resolve(fileName);

        try {
            if (!Files.exists(currentLogFile)) {
                Files.createFile(currentLogFile);
            }
            initialized = true;
            info("日志工具初始化成功，文件: {0}", currentLogFile.getFileName());
        } catch (IOException e) {
            System.err.println("创建日志文件失败: " + e.getMessage());
        }
    }

    public static void info(String format, Object... args) {
        log("INFO", format, args);
    }

    public static void debug(String format, Object... args) {
        if (ProgramStatusContext.isDebug()) {
            log("DEBUG", format, args);
        }
    }

    public static void warn(String format, Object... args) {
        log("WARN", format, args);
    }

    public static void error(String format, Object... args) {
        log("ERROR", format, args);
    }

    public static void error(String message, Throwable t) {
        log("ERROR", message + (t == null ? "" : " - " + t));
    }

    private static void log(String level, String format, Object... args) {
        String message = formatMessage(format, args);

        if (!initialized || currentLogFile == null) {
            String threadName = Thread.currentThread().getName();
            String time       = LocalDateTime.now().format(LOG_TIME_FORMATTER);
            System.out.printf("[%s]-[%s]-[%s]: %s%n", time, threadName, level, message);
            return;
        }

        // 获取调用者信息
        String methodName = getCallingMethodName();

        String threadName = Thread.currentThread().getName();
        String time       = LocalDateTime.now().format(LOG_TIME_FORMATTER);

        // 格式: [时间]-[线程名]-[级别]-[方法名]: [日志内容]
        String logEntry = String.format("[%s]-[%s]-[%s]-[%s]: %s%n",
                                        time, threadName, level, methodName, message);

        try {
            System.out.print(logEntry);
            Files.writeString(currentLogFile, logEntry, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("写入日志失败: " + e.getMessage());
        }
    }

    /**
     * 格式化消息，支持 {0}, {1} 格式的占位符
     *
     * @param format 格式字符串
     * @param args   参数
     * @return 格式化后的字符串
     */
    private static String formatMessage(String format, Object... args) {
        if (format == null || args == null || args.length == 0) {
            return format;
        }

        String result = format;
        for (int i = 0; i < args.length; i++) {
            String placeholder = "{" + i + "}";
            String value       = String.valueOf(args[i]);
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * 获取调用者方法名
     *
     * @return 方法名
     */
    private static String getCallingMethodName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String              methodName = "unknown";
        if (stackTrace.length > 4) {
            // 0: getStackTrace, 1: log, 2: info/warn/error, 3: getCalling, 4.caller
            methodName = stackTrace[4].getClassName() + "." + stackTrace[4].getMethodName();
            // 简化类名
            int lastDot = methodName.lastIndexOf('.');
            if (lastDot > 0) {
                int secondLastDot = methodName.lastIndexOf('.', lastDot - 1);
                if (secondLastDot >= 0) {
                    methodName = methodName.substring(secondLastDot + 1);
                }
            }
        }
        return methodName;
    }

    /**
     * 删除指定日期前的日志文件
     *
     * @param beforeDate 日期
     */
    public static void cleanLogsBefore(LocalDate beforeDate) {
        Path logDir = Path.of(SystemUtil.LOG_DIR_PATH);
        if (!Files.exists(logDir)) {
            return;
        }

        try (Stream<Path> files = Files.list(logDir)) {
            files.filter(f -> f.toString().endsWith(".log"))
                 .forEach(f -> {
                     String name = f.getFileName().toString();
                     try {
                         String    dateStr  = name.substring(0, name.lastIndexOf('.'));
                         LocalDate fileDate = LocalDate.parse(dateStr, FILE_NAME_FORMATTER);
                         if (fileDate.isBefore(beforeDate)) {
                             Files.deleteIfExists(f);
                             info("删除了旧日志文件: " + name);
                         }
                     } catch (Exception e) {
                         // 忽略非标准命名的文件
                     }
                 });
        } catch (IOException e) {
            error("清理日志失败", e);
        }
    }

    /**
     * 保留最近多少天的日志
     *
     * @param days 保留天数
     */
    public static void cleanLogsKeepDays(int days) {
        cleanLogsBefore(LocalDate.now().minusDays(days));
    }
}
