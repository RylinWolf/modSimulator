package com.wolfhouse.modbus_simulator;

import com.wolfhouse.mod4j.utils.ModbusTcpSimulator;
import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;
import com.wolfhouse.modbus_simulator.model.TcpDeviceModel;
import com.wolfhouse.modbus_simulator.service.FileService;
import com.wolfhouse.modbus_simulator.service.MockResponseService;
import com.wolfhouse.modbus_simulator.service.TcpSimulatorService;
import com.wolfhouse.modbus_simulator.util.SystemUtil;
import com.wolfhouse.modbus_simulator.util.WindowUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TcpSimulatorController {

    public static final String                               STATE_RUNNING     = "运行中";
    private final       ObservableList<TcpDeviceModel>       devices           = DeviceManager.getInstance().getTcpDevices();
    /** 模拟器启动数量 */
    private final       AtomicInteger                        runningCount      = new AtomicInteger(0);
    /** 当前最大端口号 */
    private final       AtomicInteger                        nextAvailablePort = new AtomicInteger(5502);
    @FXML
    private             TableView<TcpDeviceModel>            deviceTable;
    @FXML
    private             TableColumn<TcpDeviceModel, Integer> portColumn;
    @FXML
    private             TableColumn<TcpDeviceModel, String>  statusColumn;
    @FXML
    private             TableColumn<TcpDeviceModel, String>  nameColumn;
    @FXML
    private             TableColumn<TcpDeviceModel, Void>    actionsColumn;
    /** 基础窗口 */
    private             Stage                                baseStage;

    public Stage getBaseStage() {
        if (baseStage == null) {
            // 获取 Stage
            baseStage = WindowUtil.getStage(deviceTable);
        }
        return baseStage;
    }

    @FXML
    public void initialize() {
        portColumn.setCellValueFactory(data -> data.getValue().portProperty().asObject());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if (STATE_RUNNING.equals(item)) {
                        badge.getStyleClass().add("badge-success");
                    } else {
                        badge.getStyleClass().add("badge-danger");
                    }
                    setGraphic(badge);
                }
            }
        });
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());

        setupActionsColumn();
        deviceTable.setItems(devices);
        deviceTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setupDeviceTableContextMenu();

        // 双击编辑设备
        deviceTable.setRowFactory(tv -> {
            TableRow<TcpDeviceModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    TcpDeviceModel model = row.getItem();
                    if (!STATE_RUNNING.equals(model.getStatus())) {
                        WindowUtil.showBased(getBaseStage(), getDeviceDialog(model));
                    }
                }
            });
            return row;
        });

        // 注册 stage
        deviceTable.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            newValue.windowProperty().addListener((_, _, newW) -> {
                this.baseStage = (Stage) newW;
                // 注册关闭事件
                newW.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, this::onClose);
                // 注册保存快捷键事件
                WindowUtil.addSaveShortcut(newW, this::handleExportConf, true);
                // 注册删除快捷键事件
                WindowUtil.addDeleteShortcut(newW, this::handleBatchDelete, true);
            });
        });
    }

    private void onClose(WindowEvent windowEvent) {
        if (!ProgramStatusContext.isSaved()) {
            // 未保存的更改
            Optional<ButtonType> choice = WindowUtil.showAlert(Alert.AlertType.WARNING,
                                                               "警告",
                                                               "未保存的更改",
                                                               "未保存的修改将丢失，是否继续关闭？",
                                                               baseStage, ButtonType.YES, ButtonType.NO);
            if (choice.isEmpty() || choice.get() == ButtonType.NO) {
                windowEvent.consume();
                return;
            }
        }
        // 有正在运行的模拟器
        if (runningCount.get() > 0) {
            Optional<ButtonType> result = WindowUtil.showAlert(Alert.AlertType.CONFIRMATION,
                                                               "确认退出",
                                                               "确认退出 Modbus 模拟器？",
                                                               "退出后所有正在运行的模拟设备都将停止",
                                                               baseStage, ButtonType.OK, ButtonType.CANCEL);
            if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
                // 消费关闭事件
                windowEvent.consume();
                return;
            }
        }
        // 停止所有设备后再退出
        DeviceManager.getInstance().stopAll();
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleBatchStart() {
        List<TcpDeviceModel> selected = new ArrayList<>(deviceTable.getSelectionModel().getSelectedItems());
        selected.forEach(this::startDevice);
        deviceTable.refresh();
    }

    @FXML
    private void handleBatchStop() {
        List<TcpDeviceModel> selected = new ArrayList<>(deviceTable.getSelectionModel().getSelectedItems());
        selected.forEach(this::stopDevice);
        deviceTable.refresh();
    }

    @FXML
    private void handleBatchDelete() {
        List<TcpDeviceModel> selected = new ArrayList<>(deviceTable.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            return;
        }

        Optional<ButtonType> result = WindowUtil.showAlert(Alert.AlertType.CONFIRMATION,
                                                           "确认删除",
                                                           "确认删除选中的 " + selected.size() + " 个设备？",
                                                           "此操作不可恢复。",
                                                           baseStage, ButtonType.OK, ButtonType.CANCEL);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ProgramStatusContext.saved();
            selected.forEach(model -> {
                stopDevice(model);
                devices.remove(model);
            });
        }
    }

    @FXML
    private void handleImportConf() {
        Stage stage = new Stage();
        FileChooser chooser = FileService.loadFileChooser("导入配置文件",
                                                          "modbus 配置文件",
                                                          List.of("*.mof"),
                                                          null);
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null) {
            return;
        }
        FileService.importConf(files, baseStage);
    }

    @FXML
    private void handleExportConf() {
        if (devices.isEmpty()) {
            WindowUtil.showAlert(Alert.AlertType.INFORMATION, "提示", "没有可导出的配置项",
                                 null, baseStage, ButtonType.OK);
            return;
        }
        FileChooser chooser = FileService.saveFileChooser("导出配置文件",
                                                          "devices-%s".formatted(System.currentTimeMillis()),
                                                          new File(ProgramStatusContext.isDataDirReady() ? SystemUtil.DATA_DIR_PATH : SystemUtil.USER_HOME),
                                                          "Modbus 配置文件", List.of("*.mof"));
        File file = chooser.showSaveDialog(new Stage());
        if (file == null) {
            return;
        }
        System.out.printf("导出配置文件: %s%n", file.getAbsolutePath());
        if (FileService.exportConf(file, devices.stream().toList(), baseStage)) {
            // 导出成功，修改保存状态
            ProgramStatusContext.saved();
        }
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(_ -> new TableCell<>() {
            private final Button startStopBtn = new Button();
            private final Button addRespBtn   = new Button("响应");
            private final Button editBtn      = new Button("编辑");
            private final Button consoleBtn   = new Button("控制台");
            private final Button deleteBtn    = new Button("删除");
            private final HBox   pane         = new HBox(8, startStopBtn, addRespBtn, editBtn, consoleBtn, deleteBtn);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                startStopBtn.getStyleClass().addAll("button-outlined", "button-sm");
                addRespBtn.getStyleClass().addAll("button-outlined", "button-sm");
                editBtn.getStyleClass().addAll("button-outlined", "button-sm");
                consoleBtn.getStyleClass().addAll("button-outlined", "button-sm");
                deleteBtn.getStyleClass().addAll("button-outlined", "button-sm", "danger");

                startStopBtn.setOnAction(event -> {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());
                    toggleDevice(model);
                });

                addRespBtn.setOnAction(event -> {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());
                    WindowUtil.showBased(baseStage, MockResponseService.showResponseManagementDialog(model));
                    ProgramStatusContext.unsaved();
                });

                editBtn.setOnAction(event -> {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());
                    WindowUtil.showBased(getBaseStage(), getDeviceDialog(model));
                    ProgramStatusContext.unsaved();
                });

                consoleBtn.setOnAction(event -> {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());
                    Stage          stage = TcpSimulatorService.showConsoleDialog(model);
                    // 基于当前目录展示
                    WindowUtil.showBased(getBaseStage(), stage);
                });

                deleteBtn.setOnAction(event -> {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());

                    Optional<ButtonType> result = WindowUtil.showAlert(Alert.AlertType.CONFIRMATION,
                                                                       "确定删除",
                                                                       "确认删除设备?",
                                                                       "名称: " + model.getName() + "\n端口: " + model.getPort(),
                                                                       baseStage,
                                                                       ButtonType.OK, ButtonType.CANCEL);
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        stopDevice(model);
                        devices.remove(model);
                        ProgramStatusContext.unsaved();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TcpDeviceModel model     = getTableView().getItems().get(getIndex());
                    boolean        isRunning = STATE_RUNNING.equals(model.getStatus());
                    startStopBtn.setText(isRunning ? "停止" : "启动");
                    if (isRunning) {
                        startStopBtn.getStyleClass().remove("success");
                        startStopBtn.getStyleClass().add("danger");
                    } else {
                        startStopBtn.getStyleClass().remove("danger");
                        startStopBtn.getStyleClass().add("success");
                    }
                    editBtn.setDisable(isRunning);
                    deleteBtn.setDisable(isRunning);
                    setGraphic(pane);
                }
            }
        });
    }

    private void setupDeviceTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem addMenu = new MenuItem("添加新设备");
        addMenu.setOnAction(e -> handleAddDevice());

        MenuItem startMenu = new MenuItem("启动");
        startMenu.setOnAction(e -> handleBatchStart());

        MenuItem stopMenu = new MenuItem("停止");
        stopMenu.setOnAction(e -> handleBatchStop());

        MenuItem editMenu = new MenuItem("编辑");

        editMenu.setOnAction(e -> {
            TcpDeviceModel selected = deviceTable.getSelectionModel().getSelectedItem();
            if (selected != null && !STATE_RUNNING.equals(selected.getStatus())) {
                WindowUtil.showBased(getBaseStage(), getDeviceDialog(selected));
            }
        });

        MenuItem deleteMenu = new MenuItem("删除选中设备");
        deleteMenu.getStyleClass().add("danger");
        deleteMenu.setOnAction(_ -> handleBatchDelete());

        // 操作选项分隔符
        SeparatorMenuItem actionSep = new SeparatorMenuItem();
        // 删除选项分隔符
        SeparatorMenuItem delSep = new SeparatorMenuItem();
        contextMenu.getItems().addAll(addMenu, actionSep, startMenu, stopMenu, editMenu, delSep, deleteMenu);

        deviceTable.setContextMenu(contextMenu);

        // 动态根据选中状态启用/禁用菜单项
        contextMenu.setOnShowing(e -> {
            boolean hasSelection    = !deviceTable.getSelectionModel().getSelectedItems().isEmpty();
            boolean singleSelection = deviceTable.getSelectionModel().getSelectedItems().size() == 1;
            boolean anyRunning = deviceTable.getSelectionModel().getSelectedItems().stream()
                                            .anyMatch(d -> STATE_RUNNING.equals(d.getStatus()));

            // 操作选项
            startMenu.setVisible(hasSelection);
            startMenu.setDisable(anyRunning);
            stopMenu.setVisible(hasSelection);
            stopMenu.setDisable(!anyRunning);
            editMenu.setVisible(singleSelection);
            editMenu.setDisable(anyRunning);
            actionSep.setVisible(hasSelection);
            // 删除选项
            deleteMenu.setVisible(hasSelection);
            deleteMenu.setDisable(anyRunning);
            delSep.setVisible(hasSelection);
        });
    }

    private void toggleDevice(TcpDeviceModel model) {
        if (STATE_RUNNING.equals(model.getStatus())) {
            stopDevice(model);
        } else {
            startDevice(model);
        }
        deviceTable.refresh();
    }

    private void startDevice(TcpDeviceModel model) {
        if (STATE_RUNNING.equals(model.getStatus())) {
            return;
        }
        try {
            ModbusTcpSimulator simulator = new ModbusTcpSimulator(model.getPort());
            simulator.setLogConsumer(model::appendLog);
            TcpSimulatorService.updateSimulatorResps(model, simulator);
            simulator.start();
            model.setSimulator(simulator);
            model.setStatus(STATE_RUNNING);
            runningCount.incrementAndGet();
        } catch (IOException e) {
            WindowUtil.showError("启动失败: " + e.getMessage(), e, baseStage);
        }
    }

    private void stopDevice(TcpDeviceModel model) {
        if (model.getSimulator() != null) {
            model.getSimulator().stop();
            model.setSimulator(null);
        }
        model.setStatus("已停止");
        runningCount.decrementAndGet();
    }

    @FXML
    private void handleAddDevice() {
        WindowUtil.showBased(getBaseStage(), getDeviceDialog(null));
    }

    private Stage getDeviceDialog(TcpDeviceModel existingModel) {
        boolean isEdit = existingModel != null;
        Stage   stage  = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(isEdit ? "编辑 TCP 设备" : "添加 TCP 设备");

        VBox root = new VBox(24);
        root.setPadding(new Insets(32));
        root.getStyleClass().add("dialog-root");
        root.setPrefWidth(450);

        Label titleLabel = new Label(isEdit ? "编辑设备" : "添加新设备");
        titleLabel.getStyleClass().add("header-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(20);

        TextField nameField = new TextField(isEdit ? existingModel.getName() : "");
        nameField.setPromptText("例如：局放监测主机");

        TextField portField = new TextField(String.valueOf(nextAvailablePort.get()));
        portField.setPromptText("默认 5502");

        TextArea remarkArea = new TextArea(isEdit ? existingModel.getRemark() : "");
        remarkArea.setPromptText("可选的备注信息...");
        remarkArea.setPrefRowCount(3);
        remarkArea.setWrapText(true);

        Label nameLabel = new Label("设备名称");
        nameLabel.getStyleClass().add("form-label");
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 0, 1);

        Label portLabel = new Label("端口号");
        portLabel.getStyleClass().add("form-label");
        grid.add(portLabel, 0, 2);
        grid.add(portField, 0, 3);

        Label remarkLabel = new Label("备注");
        remarkLabel.getStyleClass().add("form-label");
        grid.add(remarkLabel, 0, 4);
        grid.add(remarkArea, 0, 5);

        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().add(cc);

        HBox footer = new HBox(12);
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("取消");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = new Button("保存配置");
        saveBtn.getStyleClass().add("accent");
        saveBtn.setPrefWidth(120);

        footer.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(titleLabel, grid, footer);

        Runnable saveTask = () -> {
            try {
                int port = Integer.parseInt(portField.getText());
                if (port > 0xFFFF || port < 0) {
                    throw new NumberFormatException();
                }
                // 新增设备，检查端口是否重复
                if (!isEdit || port != existingModel.getPort()) {
                    if (devices.stream().anyMatch(d -> d.getPort() == port)) {
                        // 端口已存在
                        Optional<ButtonType> choice = WindowUtil.showAlert(Alert.AlertType.WARNING,
                                                                           "警告",
                                                                           null,
                                                                           "端口 " + port + " 已存在，是否继续添加",
                                                                           baseStage, ButtonType.YES, ButtonType.NO);
                        if (choice.isEmpty() || choice.get() != ButtonType.YES) {
                            return;
                        }
                    }
                }

                if (isEdit) {
                    existingModel.setName(nameField.getText());
                    existingModel.setPort(port);
                    existingModel.setRemark(remarkArea.getText());
                } else {
                    TcpDeviceModel newModel = new TcpDeviceModel(port);
                    newModel.setName(nameField.getText());
                    newModel.setRemark(remarkArea.getText());
                    devices.add(newModel);
                }
                // 刷新表格和状态
                nextAvailablePort.set(port + 1);
                deviceTable.refresh();
                ProgramStatusContext.unsaved();
                // 如果是添加新设备，则保存后立刻关闭窗口
                if (!isEdit) {
                    stage.close();
                }
            } catch (NumberFormatException ex) {
                WindowUtil.showError("无效的端口号", null, baseStage);
            }
        };

        saveBtn.setOnAction(_ -> {
            saveTask.run();
            stage.close();
        });

        // 按下 Enter 保存，排查备注区域
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !remarkArea.isFocused()) {
                saveTask.run();
                stage.close();
                event.consume();
            }
        });
        // 绑定保存按键
        WindowUtil.addSaveShortcut(stage, saveTask, true);

        Scene scene = new Scene(root);
        WindowUtil.setupDialogCloseShortcuts(stage, scene);
        stage.setScene(scene);
        return stage;
    }


}
