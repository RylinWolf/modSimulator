package com.wolfhouse.modbus_simulator.model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 程序状态上下文
 *
 * @author Rylin Wolf
 */
public class ProgramStatusContext {
    /** 是否有未保存的更改 */
    public static final AtomicBoolean UNSAVED = new AtomicBoolean(false);

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
}
