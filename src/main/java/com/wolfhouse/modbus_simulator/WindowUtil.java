package com.wolfhouse.modbus_simulator;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * 日志工具类
 *
 * @author Rylin Wolf
 */
public class WindowUtil {
    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void setupDialogCloseShortcuts(Stage stage, Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                return;
            }
            // 兼容 Cmd+W (Mac) 和 Alt+F4 (Windows 由 OS 处理，但 JavaFX 也可以捕捉)
            KeyCombination closeCombo = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
            if (closeCombo.match(event)) {
                stage.close();
            }
        });
    }

    /**
     * 基于某个窗口位置进行展示
     *
     * @param based 窗口位置基准
     * @param show  要展示的窗口
     */
    public static void showBased(Window based, Stage show) {
        show.initOwner(based);
        show.sizeToScene();

        double centerX = based.getX() + based.getWidth() / 2;
        double centerY = based.getY() + based.getHeight() / 2;
        show.setOnShown(_ -> {
            show.setX(centerX - show.getWidth() / 2);
            show.setY(centerY - show.getHeight() / 2);
        });
        show.show();
    }

    public static Stage getStage(Node node) {
        return (Stage) node.getScene().getWindow();
    }

    public static <T> Optional<T> showWaitBased(Stage based, Dialog<T> show) {
        show.initOwner(based);

        double centerX = based.getX() + based.getWidth() / 2;
        double centerY = based.getY() + based.getHeight() / 2;
        show.setOnShown(_ -> {
            // 进行布局计算
            DialogPane pane = show.getDialogPane();
            pane.layout();
            Window window = pane.getScene().getWindow();
            window.setX(centerX - pane.getWidth() / 2);
            window.setY(centerY - pane.getHeight() / 2);
        });
        return show.showAndWait();
    }
}
