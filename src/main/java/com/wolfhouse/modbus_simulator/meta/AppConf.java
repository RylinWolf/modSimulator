package com.wolfhouse.modbus_simulator.meta;

/**
 * 应用配置相关字段
 *
 * @author Rylin Wolf
 */
public final class AppConf {
    /** APP 名称 */
    public static final String APP_NAME = "ModbusSimulator";

    // region conf-core-file
    /** APP 核心配置文件名 */
    public static final String APP_CONF_CORE          = "app_conf.properties";
    /** APP 核心配置文件 模板名 */
    public static final String APP_CONF_CORE_TEMPLATE = "/conf/app_conf_temp.properties";
    // endregion

    // region conf-core-item
    /** APP 调试模式字段 */
    public static final String APP_CONF_DEBUG_ENABLED     = "debug.enabled";
    /** 控制台最大字符长度 */
    public static final String APP_CONF_CONSOLE_MAX_CHARS = "console.max.chars";
    // endregion

    /** 控制台允许最大字符长度上限 */
    public static final Integer CONSOLE_MAX_CHARS = 0xFFFF;

    private AppConf() {}
}
