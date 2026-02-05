package com.wolfhouse.modbus_simulator.meta;

/**
 * 应用配置相关字段
 *
 * @author Rylin Wolf
 */
public final class AppConf {
    /** APP 名称 */
    public static final String APP_NAME = "ModbusSimulator";

    // region conf-core
    /** APP 核心配置文件名 */
    public static final String APP_CONF_CORE               = "app_conf.properties";
    /** APP 核心配置文件 模板名 */
    public static final String APP_CONF_CORE_TEMPLATE_NAME = "app_conf_temp.properties";
    // endregion

    /** APP 调试模式字段 */
    public static final String APP_CONF_DEBUG_ENABLED = "debug.enabled";

    private AppConf() {}
}
