package com.wolfhouse.modbus_simulator;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import java.io.IOException;
import java.util.Optional;

public class MainController {

    @FXML
    private StackPane contentArea;
    
    @FXML
    private ToggleGroup navGroup;

    @FXML
    private Button themeBtn;

    private boolean isDarkMode = true;

    private final java.util.Map<String, Node> viewCache = new java.util.HashMap<>();

    @FXML
    public void initialize() {
        themeBtn.setGraphic(new FontIcon(MaterialDesignW.WEATHER_NIGHT));
        showTcpView();

        // 在 initialize 之后，Scene 才会附加到 contentArea 的窗口
        Platform.runLater(() -> {
            applyTheme();
            setupGlobalShortcuts();
        });
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
        isDarkMode = !isDarkMode;
        applyTheme();
    }

    private void applyTheme() {
        if (isDarkMode) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
            themeBtn.setGraphic(new FontIcon(MaterialDesignW.WEATHER_NIGHT));
            contentArea.getScene().getRoot().getStyleClass().remove("light");
            contentArea.getScene().getRoot().getStyleClass().add("dark");
        } else {
            Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
            themeBtn.setGraphic(new FontIcon(MaterialDesignW.WEATHER_SUNNY));
            contentArea.getScene().getRoot().getStyleClass().remove("dark");
            contentArea.getScene().getRoot().getStyleClass().add("light");
        }
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

    private void loadView(String fxml) {
        try {
            if (!viewCache.containsKey(fxml)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                viewCache.put(fxml, loader.load());
            }
            contentArea.getChildren().setAll(viewCache.get(fxml));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
