package com.wolfhouse.modbus_simulator.util;

import com.wolfhouse.modbus_simulator.meta.AppDirs;
import com.wolfhouse.modbus_simulator.meta.AppInfo;
import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import static com.wolfhouse.modbus_simulator.meta.AppConf.APP_NAME;

/**
 * 系统工具类
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "magic"})
public class SystemUtil {
    /** 系统名称 */
    public static final String OS_NAME   = System.getProperty("os.name").toLowerCase();
    /** 用户目录路径 */
    public static final String USER_HOME = System.getProperty("user.home");

    /** 系统枚举类型 */
    public static final OS     OS_TYPE     = getOsType();
    /** AppData 目录路径 */
    public static final String APPDATA_DIR = getAppDataDir().toAbsolutePath().toString();

    /** 数据目录路径 */
    public static final String DATA_DIR_PATH = Path.of(APPDATA_DIR, AppDirs.DATA_DIR_NAME).toString();
    /** 日志目录路径 */
    public static final String LOG_DIR_PATH  = Path.of(APPDATA_DIR, AppDirs.LOG_DIR_NAME).toString();


    public static Path getAppDataDir() {
        return switch (OS_TYPE) {
            case WINDOWS -> {
                String appdata = System.getenv("APPDATA");
                if (appdata != null) {
                    yield Paths.get(appdata, APP_NAME);
                }
                yield Paths.get(USER_HOME, "AppData", "Roaming", APP_NAME);
            }
            case MACOS -> Paths.get(USER_HOME, "Library", "Application Support", APP_NAME);
            case LINUX -> {
                String xdgConfig = System.getenv("XDG_CONFIG_HOME");
                if (xdgConfig != null) {
                    yield Paths.get(xdgConfig, APP_NAME.toLowerCase());
                }
                yield Paths.get(USER_HOME, ".config", APP_NAME.toLowerCase());
            }
            // 兜底，用户目录下隐藏文件夹
            default -> Paths.get(USER_HOME, "." + APP_NAME.toLowerCase());

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
                LogUtil.info("创建目录[{0}]", dir.toAbsolutePath());
                Files.createDirectories(dir);
            } catch (IOException e) {
                return e;
            }
            if (!Files.exists(dir)) {
                return new IOException("未知原因");
            }
            LogUtil.info("成功创建目录[{0}]", dir.toAbsolutePath());
            return null;
        }
        LogUtil.debug("目录[{0}]已存在，跳过创建", dir.toAbsolutePath());
        return null;
    }

    public static Exception initLogDir() {
        Exception e = mkdirs(Path.of(LOG_DIR_PATH));
        if (e == null) {
            ProgramStatusContext.logDirReady();
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

    public static Optional<Properties> getAppInfoProperties() {
        return getClassPathProperties(AppInfo.APP_INFO_PROPERTIES);
    }

    public static String getVersion() {
        Optional<Properties> prop = getAppInfoProperties();
        if (prop.isPresent()) {
            return prop.get().getProperty(AppInfo.APP_INFO_VERSION);
        }
        return "";
    }

    public static Optional<Properties> getProperties(InputStream inputStream) {
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            LogUtil.error("加载属性文件失败", e);
            return Optional.empty();
        }
        return Optional.of(properties);
    }

    public static Optional<Properties> getClassPathProperties(String resourcePath) {
        return getProperties(SystemUtil.class.getClassLoader().getResourceAsStream(resourcePath));
    }

    public static Optional<Properties> getProperties(String abstractPath) {
        try {
            return getProperties(new FileInputStream(abstractPath));
        } catch (FileNotFoundException e) {
            LogUtil.error("文件不存在: {0}, error: {1}", abstractPath, e);
            return Optional.empty();
        }
    }


    /**
     * 判断系统是否处于深色模式
     *
     * @return true 如果是深色模式
     */
    public static boolean isDarkMode() {
        return switch (OS_TYPE) {
            case MACOS -> isMacDarkMode();
            case WINDOWS -> isWindowsDarkMode();
            default -> false;
        };
    }

    private static boolean isMacDarkMode() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"defaults", "read", "-g", "AppleInterfaceStyle"});
            try (InputStream is = process.getInputStream()) {
                String result = new String(is.readAllBytes()).trim();
                return "Dark".equalsIgnoreCase(result);
            }
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isWindowsDarkMode() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme"
            });
            try (InputStream is = process.getInputStream()) {
                String result = new String(is.readAllBytes());
                // 0 表示深色模式，1 表示浅色模式
                return result.contains("0x0");
            }
        } catch (IOException e) {
            return false;
        }
    }

    public enum OS {
        /** 操作系统类型 */
        WINDOWS, LINUX, MACOS, OTHER
    }
}
