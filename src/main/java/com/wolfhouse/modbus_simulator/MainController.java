package com.wolfhouse.modbus_simulator;

import com.wolfhouse.modbus_simulator.util.SystemUtil;
import com.wolfhouse.modbus_simulator.util.WindowUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class MainController {

    private final java.util.Map<String, Node> viewCache = new java.util.HashMap<>();
    @FXML
    private       StackPane                   contentArea;
    @FXML
    private       ToggleGroup                 navGroup;
    @FXML
    private       Button                      themeBtn;
    @FXML
    private       Label                       versionLabel;

    @FXML
    public void initialize() {
        themeBtn.setGraphic(new FontIcon(MainApplication.isDarkMode() ? MaterialDesignW.WEATHER_NIGHT : MaterialDesignW.WEATHER_SUNNY));
        versionLabel.setText("Version %s".formatted(SystemUtil.getVersion()));
        showTcpView();

        // 监听主题变化更新图标
        MainApplication
                .darkModeProperty()
                .addListener((_,
                              _,
                              newVal) -> Platform.runLater(
                        () -> themeBtn.setGraphic(new FontIcon(newVal ? MaterialDesignW.WEATHER_NIGHT : MaterialDesignW.WEATHER_SUNNY))));

        // 在 initialize 之后，Scene 才会附加到 contentArea 的窗口
        Platform.runLater(this::setupGlobalShortcuts);
    }

    private void setupGlobalShortcuts() {
        if (contentArea.getScene() == null) {
            return;
        }

        contentArea.getScene().setOnKeyPressed(event -> {
            // 兼容 Mac Cmd+W 和 Windows Alt+F4
            KeyCombination closeCombo = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
            if (closeCombo.match(event)) {
                Stage stage = (Stage) contentArea.getScene().getWindow();
                // 触发 WINDOW_CLOSE_REQUEST 事件，让系统处理关闭逻辑（包括触发 onCloseRequest 钩子）
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                event.consume();
            }
        });
    }

    @FXML
    private void toggleTheme() {
        MainApplication.setDarkMode(!MainApplication.isDarkMode());
    }

    @FXML
    private void showTcpView() {
        loadView("tcp-view.fxml");
    }

    @FXML
    private void showRtuView() {
        // Placeholder for RTU
        contentArea.getChildren().clear();
    }

    private Node loadView(String fxml) {
        Node viewNode = null;
        try {
            if (!viewCache.containsKey(fxml)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                viewNode = loader.load();
                viewCache.put(fxml, viewNode);
            }
            contentArea.getChildren().setAll(viewCache.get(fxml));
        } catch (IOException e) {
            WindowUtil.showError("加载视图失败", e, WindowUtil.getStage(contentArea));
        }
        return viewNode;
    }

    @FXML
    private void browseGithub() {
        openUrl("https://github.com/RylinWolf");
    }

    public void openUrl(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                WindowUtil.showError("出现错误", e, WindowUtil.getStage(contentArea));
            }
        }
    }
}
