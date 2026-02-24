package com.wolfhouse.modbus_simulator;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import com.wolfhouse.modbus_simulator.meta.AppConf;
import com.wolfhouse.modbus_simulator.meta.AppDirs;
import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;
import com.wolfhouse.modbus_simulator.util.ConfUtil;
import com.wolfhouse.modbus_simulator.util.LogUtil;
import com.wolfhouse.modbus_simulator.util.SystemUtil;
import com.wolfhouse.modbus_simulator.util.WindowUtil;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

public class MainApplication extends Application {

    private static final BooleanProperty darkModeProperty = new SimpleBooleanProperty(SystemUtil.isDarkMode());
    private static       Stage           primaryStage;

    public static BooleanProperty darkModeProperty() {
        return darkModeProperty;
    }

    public static boolean isDarkMode() {
        return darkModeProperty.get();
    }

    public static void setDarkMode(boolean darkMode) {
        darkModeProperty.set(darkMode);
    }

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
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

        // 绑定主题切换
        darkModeProperty.addListener((_, _, newVal) -> {
            Platform.runLater(() -> {
                Scene scene = primaryStage.getScene();
                if (scene == null || scene.getRoot() == null) {
                    applyTheme(newVal);
                    return;
                }

                // 1. 获取当前场景截图
                WritableImage snapshot = scene.snapshot(null);

                // 2. 创建覆盖层
                ImageView imageView = new ImageView(snapshot);
                Pane      root      = (Pane) scene.getRoot();

                // 如果 root 已经是一个 StackPane，直接添加，否则可能需要包装
                // 考虑到 main-view.fxml 的根节点是 BorderPane，我们建议在 start 方法中进行包装
                if (root instanceof StackPane stackPane) {
                    stackPane.getChildren().add(imageView);
                } else {
                    // 如果不是 StackPane，则执行普通切换
                    applyTheme(newVal);
                    return;
                }

                // 3. 切换主题
                applyTheme(newVal);

                // 4. 动画淡出截图
                FadeTransition fade = new FadeTransition(Duration.millis(400), imageView);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(_ -> stackPane.getChildren().remove(imageView));
                fade.play();
            });
        });

        // 初始化主题
        if (isDarkMode()) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        }

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main-view.fxml"));
        Parent     root       = fxmlLoader.load();
        // 使用 StackPane 包装原始根节点，以便添加动画覆盖层
        StackPane wrapper = new StackPane(root);
        Scene     scene   = new Scene(wrapper);
        stage.setTitle("Modbus 模拟器");
        stage.setScene(scene);
        stage.show();

        // 执行初始化
        initApplication(stage);
    }

    private void applyTheme(boolean dark) {
        if (dark) {
            Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
        }
    }


    @Override
    public void stop() throws Exception {
        DeviceManager.getInstance().stopAll();
        super.stop();
    }

    private void initApplication(Stage stage) {
        // 修改进程名称
        Thread.currentThread().setName("JavaFX");
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

        // 启动系统深色模式监听
        startDarkModeMonitor();

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

    private void startDarkModeMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    // 每 3 秒检查一次系统模式
                    Thread.sleep(3000);
                    boolean currentSystemDarkMode = SystemUtil.isDarkMode();
                    if (currentSystemDarkMode != isDarkMode()) {
                        LogUtil.info("检测到系统主题变化，正在同步...");
                        Platform.runLater(() -> setDarkMode(currentSystemDarkMode));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LogUtil.error("系统主题监控出错", e);
                }
            }
        }, "DarkModeMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
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
        // 控制台最大字符数
        String consoleMaxLeng = coreConf.getProperty(AppConf.APP_CONF_CONSOLE_MAX_CHARS);
        int    consoleMaxChars;
        if (consoleMaxLeng == null
                || (consoleMaxChars = Integer.parseInt(consoleMaxLeng)) <= 0
                || consoleMaxChars > AppConf.CONSOLE_MAX_CHARS) {
            consoleMaxChars = AppConf.CONSOLE_MAX_CHARS;
        }
        ProgramStatusContext.consoleMaxChars(consoleMaxChars);
        return true;
    }
}
