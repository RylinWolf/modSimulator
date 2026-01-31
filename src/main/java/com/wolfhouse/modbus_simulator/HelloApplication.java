package com.wolfhouse.modbus_simulator;

import atlantafx.base.theme.CupertinoDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
        
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Modbus 模拟器 V2");
        stage.setScene(scene);
        
        // 添加关闭程序确认
        stage.setOnCloseRequest(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认退出");
            alert.setHeaderText("确认退出 Modbus 模拟器？");
            alert.setContentText("退出后所有正在运行的模拟设备都将停止。");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // 停止所有设备后再退出
                DeviceManager.getInstance().stopAll();
                Platform.exit();
                System.exit(0);
            } else {
                event.consume(); // 取消关闭事件
            }
        });

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        DeviceManager.getInstance().stopAll();
        super.stop();
    }
}
