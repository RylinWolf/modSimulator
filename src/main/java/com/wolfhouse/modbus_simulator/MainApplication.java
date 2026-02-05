package com.wolfhouse.modbus_simulator;

import atlantafx.base.theme.CupertinoDark;
import com.wolfhouse.modbus_simulator.meta.AppConf;
import com.wolfhouse.modbus_simulator.meta.AppDirs;
import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;
import com.wolfhouse.modbus_simulator.util.ConfUtil;
import com.wolfhouse.modbus_simulator.util.LogUtil;
import com.wolfhouse.modbus_simulator.util.SystemUtil;
import com.wolfhouse.modbus_simulator.util.WindowUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 设置全局 Locale
        // 设置全局 Locale
        Locale.setDefault(Locale.CHINA);
        System.setProperty("user.language", "zh");
        System.setProperty("user.country", "CN");
        System.setProperty("user.region", "CN");

        // macOS 特定的设置
        System.setProperty("apple.awt.locale", "zh_CN");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Modbus 模拟器");
        ResourceBundle.clearCache();
        Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Scene      scene      = new Scene(fxmlLoader.load());
        stage.setTitle("Modbus 模拟器");
        stage.setScene(scene);
        stage.show();

        // 执行初始化
        initApplication(stage);

    }


    @Override
    public void stop() throws Exception {
        DeviceManager.getInstance().stopAll();
        super.stop();
    }

    private void initApplication(Stage stage) {
        // 0. 初始化日志工具
        final Exception logInitE = SystemUtil.initLogDir();
        if (logInitE != null) {
            Platform.runLater(() -> WindowUtil.showError("日志目录初始化失败", logInitE, stage));
        }
        LogUtil.init(stage);
        LogUtil.info("程序启动...");

        // 1. 初始化配置文件目录
        final Exception confInitE = ConfUtil.init();
        if (confInitE != null) {
            Platform.runLater(() -> WindowUtil.showError("配置文件目录初始化失败", confInitE, stage));
        }
        // 2. 初始化核心配置文件
        if (!initCoreConf()) {
            LogUtil.error("未能成功加载核心配置文件，程序无法启动");
            stage.close();
            Platform.exit();
            System.exit(1);
            return;
        }
        // 核心配置加载完毕
        ProgramStatusContext.coreConfLoaded();

        // 3. 虚拟线程初始化其他目录
        Map<String, Exception> initConfErr = initDirs();
        if (!initConfErr.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            initConfErr.forEach((k, v) -> sb.append(k).append(": ").append(v.getMessage()).append("\n"));
            Platform.runLater(() -> WindowUtil.showAlert(Alert.AlertType.ERROR,
                                                         "初始化目录失败",
                                                         sb.toString(),
                                                         null,
                                                         stage,
                                                         ButtonType.OK));
        }
    }

    /**
     * 初始化程序所需目录（除去日志、核心配置）
     *
     * @return 初始化过程中的异常
     */
    private Map<String, Exception> initDirs() {
        Map<String, Exception> res = HashMap.newHashMap(1);
        res.put(AppDirs.DATA_DIR_NAME, SystemUtil.initDataDir());
        res.values().removeIf(Objects::isNull);
        return res;
    }

    /**
     * 初始化核心配置文件
     *
     * @return 是否初始化成功
     */
    private boolean initCoreConf() {
        Properties coreConf = ConfUtil.getCoreConf().orElse(null);
        if (coreConf == null) {
            return false;
        }
        // 调试模式
        if (Boolean.parseBoolean(coreConf.getProperty(AppConf.APP_CONF_DEBUG_ENABLED))) {
            ProgramStatusContext.debug();
            LogUtil.debug("调试模式已启动");
        }
        return true;
    }
}
