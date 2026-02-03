package com.wolfhouse.modbus_simulator;

import atlantafx.base.theme.CupertinoDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

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
        stage.setTitle("Modbus 模拟器 V1.1");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        DeviceManager.getInstance().stopAll();
        super.stop();
    }
}
