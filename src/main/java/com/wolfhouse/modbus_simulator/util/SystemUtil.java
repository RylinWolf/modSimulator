package com.wolfhouse.modbus_simulator.util;

import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 系统工具类
 *
 * @author Rylin Wolf
 */
@SuppressWarnings("all")
public class SystemUtil {
    /** APP 名称 */
    public static final String APP_NAME  = "ModbusSimulator";
    /** 系统名称 */
    public static final String OS_NAME   = System.getProperty("os.name").toLowerCase();
    /** 用户目录路径 */
    public static final String USER_HOME = System.getProperty("user.home");

    /** 配置文件目录名 */
    public static final String CONFIG_DIR_NAME = "conf";
    /** 数据目录名 */
    public static final String DATA_DIR_NAME   = "data";
    /** 日志目录名 */
    public static final String LOG_DIR_NAME    = "logs";

    /** 系统枚举类型 */
    public static final OS     OS_TYPE         = getOsType();
    /** AppData 目录路径 */
    public static final String APPDATA_DIR     = getAppDataDir().toAbsolutePath().toString();
    /** 配置文件目录路径 */
    public static final String CONFIG_DIR_PATH = Path.of(APPDATA_DIR, CONFIG_DIR_NAME).toString();
    /** 数据目录路径 */
    public static final String DATA_DIR_PATH   = Path.of(APPDATA_DIR, DATA_DIR_NAME).toString();
    /** 日志目录路径 */
    public static final String LOG_DIR_PATH    = Path.of(APPDATA_DIR, LOG_DIR_NAME).toString();

    public static Path getAppDataDir() {
        return switch (OS_TYPE) {
            case WINDOWS -> {
                String appdata = System.getenv("APPDATA");
                if (appdata != null) {
                    yield Paths.get(appdata, APP_NAME);
                }
                yield Paths.get(USER_HOME, "AppData", "Roaming", APP_NAME);
            }
            case MACOS -> {
                yield Paths.get(USER_HOME, "Library", "Application Support", APP_NAME);
            }
            case LINUX -> {
                String xdgConfig = System.getenv("XDG_CONFIG_HOME");
                if (xdgConfig != null) {
                    yield Paths.get(xdgConfig, APP_NAME.toLowerCase());
                }
                yield Paths.get(USER_HOME, ".config", APP_NAME.toLowerCase());
            }
            default -> {
                // 兜底，用户目录下隐藏文件夹
                yield Paths.get(USER_HOME, "." + APP_NAME.toLowerCase());
            }
        };
    }

    public static OS getOsType() {
        if (OS_NAME.contains("win")) {
            return OS.WINDOWS;
        } else if (OS_NAME.contains("mac")) {
            return OS.MACOS;
        } else if (OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix")) {
            return OS.LINUX;
        }
        return OS.OTHER;
    }

    public static Exception mkdirs(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            try {
                System.out.printf("创建目录[%s]%n", dir.toAbsolutePath());
                Files.createDirectories(dir);
            } catch (IOException e) {
                return e;
            }
            if (!Files.exists(dir)) {
                return new IOException("未知原因");
            }
            System.out.printf("成功创建目录[%s]%n", dir.toAbsolutePath());
            return null;
        }
        System.out.printf("目录[%s]已存在，跳过创建%n", dir.toAbsolutePath());
        return null;
    }

    public static Exception initLogDir() {
        Exception e = mkdirs(Path.of(LOG_DIR_PATH));
        if (e == null) {
            ProgramStatusContext.logDirReady();
        }
        return e;
    }

    public static Exception initConfDir() {
        Exception e = mkdirs(Path.of(CONFIG_DIR_PATH));
        if (e == null) {
            ProgramStatusContext.confDirReady();
        }
        return e;
    }

    public static Exception initDataDir() {
        Exception e = mkdirs(Path.of(DATA_DIR_PATH));
        if (e == null) {
            ProgramStatusContext.dataDirReady();
        }
        return e;
    }

    public enum OS {
        /** 操作系统类型 */
        WINDOWS, LINUX, MACOS, OTHER
    }
}
