package com.wolfhouse.modbus_simulator.service;

import com.wolfhouse.modbus_simulator.WindowUtil;
import com.wolfhouse.modbus_simulator.model.MockResponseModel;
import com.wolfhouse.modbus_simulator.model.TcpDeviceModel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

/**
 * TCP 模拟器服务
 *
 * @author Rylin Wolf
 */
public class TcpSimulatorService {
    /**
     * 更新模拟器虚拟响应配置
     *
     * @param model     Tcp 设备模型
     * @param simulator 模拟器
     */
    public static void updateSimulatorResps(TcpDeviceModel model, com.wolfhouse.mod4j.utils.ModbusTcpSimulator simulator) {
        if (simulator == null) {
            return;
        }
        simulator.clearMockResps();
        for (MockResponseModel respModel : model.getMockResponses()) {
            if (respModel.isEnabled()) {
                simulator.addMockResp(respModel.getSlaveId(), respModel.getPair());
            }
        }
        model.appendLog("[系统] 虚拟响应配置已更新");
    }

    public static Stage showConsoleDialog(TcpDeviceModel model) {
        Stage stage = new Stage();
        stage.setTitle("控制台 - " + (model.getName().isEmpty() ? "未命名设备" : model.getName()) + " (端口: " + model.getPort() + ")");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        TextArea consoleArea = new TextArea();
        consoleArea.setEditable(false);
        consoleArea.setWrapText(true);
        consoleArea.getStyleClass().add("console-text");
        consoleArea.textProperty().bind(model.logsProperty());

        stage.setOnHidden(e -> consoleArea.textProperty().unbind());

        model.logsProperty().addListener((obs, old, val) -> {
            consoleArea.setScrollTop(Double.MAX_VALUE);
        });

        Button clearBtn = new Button("清空日志");
        clearBtn.setOnAction(e -> model.clearLogs());

        root.getChildren().addAll(consoleArea, clearBtn);
        VBox.setVgrow(consoleArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 600, 400);
        WindowUtil.setupDialogCloseShortcuts(stage, scene);
        URL resource = TcpSimulatorService.class.getResource("style.css");
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        }
        stage.setScene(scene);
        return stage;
    }

}
