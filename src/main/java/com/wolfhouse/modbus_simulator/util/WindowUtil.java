package com.wolfhouse.modbus_simulator.util;

import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * 日志工具类
 *
 * @author Rylin Wolf
 */
public class WindowUtil {
    public static void showError(String message, Throwable e) {
        showError(message, e, null);
    }

    public static void showError(String message) {
        showError(message, null, null);
    }

    public static void showError(String message, Throwable e, Window based) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        String errorMsg = e == null ? "" : "\nError: " + e;
        alert.setContentText(message + errorMsg);
        alert.initOwner(based);
        if (based == null) {
            alert.showAndWait();
            return;
        }
        showWaitBased(based, alert);
    }

    /**
     * 显示一个提示框，支持根据窗口位置基准显示，提示框内容和类型可定制。
     *
     * @param type        提示框的类型，例如信息、警告、错误等
     * @param title       提示框的标题文本
     * @param headerText  提示框的头部内容文本
     * @param contentText 提示框的主要内容文本
     * @param based       用于定位提示框位置的基础窗口
     * @return 返回一个包含用户交互结果的 {@code Optional<ButtonType>} 对象，例如用户点击的按钮信息
     */
    public static Optional<ButtonType> showAlert(Alert.AlertType type,
                                                 String title,
                                                 String headerText,
                                                 String contentText,
                                                 Window based,
                                                 ButtonType... buttons) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getButtonTypes().setAll(buttons);
        return showWaitBased(based, alert);
    }


    public static void showBased(Window based, Stage show) {
        showBased(based, show, true);
    }

    /**
     * 基于某个窗口位置进行展示
     *
     * @param based     窗口位置基准
     * @param show      要展示的窗口
     * @param bindOwner 是否绑定 Owner。如果为 true，则窗口会跟随 Owner；
     *                  如果为 false，窗口将作为独立窗口，但初始位置仍基于 based。
     */
    public static void showBased(Window based, Stage show, boolean bindOwner) {
        if (bindOwner) {
            show.initOwner(based);
        }
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

    public static <T> Optional<T> showWaitBased(Window based, Dialog<T> show) {
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

    public static void addSaveShortcut(EventTarget target, Runnable saveAction, boolean consume) {
        target.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.S && (e.isControlDown() || e.isMetaDown())) {
                saveAction.run();
                if (consume) {
                    e.consume();
                }
            }
        });
    }

    public static void addDeleteShortcut(EventTarget target, Runnable deleteAction, boolean consume) {
        target.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DELETE) {
                deleteAction.run();
                if (consume) {
                    e.consume();
                }
            }
        });
    }

    /**
     * 为指定的场景设置快捷键以便快速关闭窗口。
     * 默认支持按下 Esc 键直接关闭窗口，并兼容 Mac 的 Cmd+W 和 Windows 的 Alt+F4 快捷键。
     * 在按下这些快捷键时，会触发关闭动作。
     *
     * @param stage 要绑定关闭行为的窗口对象，用于关闭窗口。
     * @param scene 与窗口关联的场景对象，用于监听键盘事件。
     */
    public static void setupDialogCloseShortcuts(Stage stage, Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                return;
            }
            // 兼容 Cmd+W (Mac) 和 Alt+F4
            KeyCombination closeCombo = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
            KeyCombination altF4Combo = new KeyCodeCombination(KeyCode.F4, KeyCombination.ALT_DOWN);
            if (closeCombo.match(event) || altF4Combo.match(event)) {
                stage.close();
            }
        });
    }

}
