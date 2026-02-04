package com.wolfhouse.modbus_simulator;

import atlantafx.base.theme.CupertinoDark;
import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;
import com.wolfhouse.modbus_simulator.util.SystemUtil;
import com.wolfhouse.modbus_simulator.util.WindowUtil;
import javafx.application.Application;
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

        // 虚拟线程初始化目录
        Thread.startVirtualThread(() -> {
            Map<String, Exception> initConfErr = initDirs();
            if (!initConfErr.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                initConfErr.forEach((k, v) -> sb.append(k).append(": ").append(v.getMessage()).append("\n"));
                WindowUtil.showAlert(Alert.AlertType.ERROR,
                                     "初始化目录失败",
                                     sb.toString(),
                                     null,
                                     stage,
                                     ButtonType.OK);
            }
            // 目录就绪
            ProgramStatusContext.allDirReady();
        });
    }

    @Override
    public void stop() throws Exception {
        DeviceManager.getInstance().stopAll();
        super.stop();
    }

    /**
     * 初始化程序所需目录
     *
     * @return 初始化过程中的异常
     */
    private Map<String, Exception> initDirs() {
        Map<String, Exception> res = HashMap.newHashMap(3);
        res.put(SystemUtil.LOG_DIR_NAME, SystemUtil.initLogDir());
        res.put(SystemUtil.CONFIG_DIR_NAME, SystemUtil.initConfDir());
        res.put(SystemUtil.DATA_DIR_NAME, SystemUtil.initDataDir());
        res.values().removeIf(Objects::isNull);
        return res;
    }
}
