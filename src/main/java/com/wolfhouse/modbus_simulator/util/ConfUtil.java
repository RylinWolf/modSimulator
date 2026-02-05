package com.wolfhouse.modbus_simulator.util;

import com.wolfhouse.modbus_simulator.meta.AppConf;
import com.wolfhouse.modbus_simulator.meta.AppDirs;
import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * 配置文件相关工具
 *
 * @author Rylin Wolf
 */
public class ConfUtil {
    /** 配置文件目录路径 */
    public static final String CONFIG_DIR_PATH = Path.of(SystemUtil.APPDATA_DIR, AppDirs.CONFIG_DIR_NAME).toString();
    public static final String CORE_CONF_PATH  = Path.of(CONFIG_DIR_PATH, AppConf.APP_CONF_CORE).toString();

    private ConfUtil() {}

    public static Exception init() {
        Exception e;
        // 初始化配置文件目录
        if ((e = initConfDir()) != null) {
            return e;
        }
        // 初始化核心配置文件
        return initCoreConf();

        // 初始化其他...
    }

    private static Exception initConfDir() {
        Exception e = SystemUtil.mkdirs(Path.of(CONFIG_DIR_PATH));
        if (e == null) {
            ProgramStatusContext.confDirReady();
        }
        return e;
    }

    public static Exception initCoreConf() {
        // 1. 检查文件是否存在
        Path coreConfPath = Path.of(CORE_CONF_PATH);
        if (Files.exists(coreConfPath) && Files.isRegularFile(coreConfPath)) {
            LogUtil.info("核心配置文件已存在，不再初始化");
            return null;
        }
        if (!Files.exists(Path.of(CONFIG_DIR_PATH))) {
            LogUtil.error("配置文件目录不存在，无法初始化核心配置文件");
            return new FileNotFoundException("配置文件目录不存在");
        }
        // 2. 不存在则根据模板创建
        LogUtil.info("核心配置文件不存在，正在尝试初始化");
        InputStream ins = ConfUtil.class.getClassLoader().getResourceAsStream(AppConf.APP_CONF_CORE_TEMPLATE_NAME);
        if (ins == null) {
            LogUtil.error("核心配置文件模板不存在，无法初始化核心配置文件");
            return new FileNotFoundException("核心配置文件模板不存在");
        }
        try {
            Files.copy(ins, coreConfPath);
        } catch (IOException e) {
            LogUtil.error("核心配置文件模板复制失败，无法初始化核心配置文件");
            return e;
        }
        LogUtil.info("核心配置文件初始化成功");
        return null;
    }

    public static Optional<Properties> getCoreConf() {
        if (!ProgramStatusContext.isConfDirReady()) {
            LogUtil.error("未能成功加载配置文件目录，无法获取配置资源");
            return Optional.empty();
        }
        return SystemUtil.getProperties(CORE_CONF_PATH);
    }

    public static boolean isDebug() {
        return ProgramStatusContext.isDebug();
    }
}
