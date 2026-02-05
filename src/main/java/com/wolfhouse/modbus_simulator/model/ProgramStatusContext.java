package com.wolfhouse.modbus_simulator.model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 程序状态上下文
 *
 * @author Rylin Wolf
 */
public class ProgramStatusContext {
    /** 是否有未保存的更改 */
    public static final AtomicBoolean UNSAVED          = new AtomicBoolean(false);
    public static final AtomicBoolean DATA_DIR_READY   = new AtomicBoolean(false);
    public static final AtomicBoolean LOG_DIR_READY    = new AtomicBoolean(false);
    public static final AtomicBoolean CONF_DIR_READY   = new AtomicBoolean(false);
    public static final AtomicBoolean CORE_CONF_LOADED = new AtomicBoolean(false);
    public static final AtomicBoolean DEBUG            = new AtomicBoolean(false);

    private ProgramStatusContext() {}

    public static void saved() {
        UNSAVED.set(false);
    }

    public static void unsaved() {
        UNSAVED.set(true);
    }

    public static boolean isSaved() {
        return !UNSAVED.get();
    }

    public static void dataDirReady() {
        DATA_DIR_READY.set(true);
    }

    public static void logDirReady() {
        LOG_DIR_READY.set(true);
    }

    public static void coreConfLoaded() {
        CORE_CONF_LOADED.set(true);
    }

    public static void confDirReady() {
        CONF_DIR_READY.set(true);
    }

    public static void debug() {
        DEBUG.set(true);
    }

    public static boolean isDataDirReady() {
        return DATA_DIR_READY.get();
    }

    public static boolean isLogDirReady() {
        return LOG_DIR_READY.get();
    }

    public static boolean isConfDirReady() {
        return CONF_DIR_READY.get();
    }

    public static boolean isDebug() {
        return DEBUG.get();
    }

    public static boolean isCoreConfLoaded() {
        return CORE_CONF_LOADED.get();
    }
}
