package com.wolfhouse.modbus_simulator;

import com.wolfhouse.mod4j.utils.ModbusTcpSimulator;
import com.wolfhouse.modbus_simulator.model.MockResponseModel;
import com.wolfhouse.modbus_simulator.model.TcpDeviceModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TcpSimulatorController {

    @FXML
    private TableView<TcpDeviceModel> deviceTable;
    @FXML
    private TableColumn<TcpDeviceModel, Integer> portColumn;
    @FXML
    private TableColumn<TcpDeviceModel, String> statusColumn;
    @FXML
    private TableColumn<TcpDeviceModel, String> nameColumn;
    @FXML
    private TableColumn<TcpDeviceModel, Void> actionsColumn;

    private final ObservableList<TcpDeviceModel> devices = DeviceManager.getInstance().getTcpDevices();

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
                    if ("运行中".equals(item)) {
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
                    if (!"运行中".equals(model.getStatus())) {
                        showDeviceDialog(model);
                    }
                }
            });
            return row;
        });
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
        if (selected.isEmpty()) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认批量删除");
        alert.setHeaderText("确认批量删除选中的 " + selected.size() + " 个设备？");
        alert.setContentText("此操作不可恢复。");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            selected.forEach(model -> {
                stopDevice(model);
                devices.remove(model);
            });
        }
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button startStopBtn = new Button();
            private final Button addRespBtn = new Button("响应");
            private final Button editBtn = new Button("编辑");
            private final Button consoleBtn = new Button("控制");
            private final Button deleteBtn = new Button("删除");
            private final HBox pane = new HBox(8, startStopBtn, addRespBtn, editBtn, consoleBtn, deleteBtn);

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
                    showResponseManagementDialog(model);
                });

                editBtn.setOnAction(event -> {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());
                    showDeviceDialog(model);
                });

                consoleBtn.setOnAction(event -> {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());
                    showConsoleDialog(model);
                });

                deleteBtn.setOnAction(event -> {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());
                    
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("确认删除");
                    alert.setHeaderText("确认删除设备？");
                    alert.setContentText("名称: " + model.getName() + "\n端口: " + model.getPort());
                    
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        stopDevice(model);
                        devices.remove(model);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TcpDeviceModel model = getTableView().getItems().get(getIndex());
                    boolean isRunning = "运行中".equals(model.getStatus());
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

        MenuItem startMenu = new MenuItem("启动选中设备");
        startMenu.setOnAction(e -> handleBatchStart());

        MenuItem stopMenu = new MenuItem("停止选中设备");
        stopMenu.setOnAction(e -> handleBatchStop());

        MenuItem editMenu = new MenuItem("编辑选中设备");
        editMenu.setOnAction(e -> {
            TcpDeviceModel selected = deviceTable.getSelectionModel().getSelectedItem();
            if (selected != null && !"运行中".equals(selected.getStatus())) {
                showDeviceDialog(selected);
            }
        });

        MenuItem deleteMenu = new MenuItem("删除选中设备");
        deleteMenu.getStyleClass().add("danger");
        deleteMenu.setOnAction(e -> handleBatchDelete());

        contextMenu.getItems().addAll(addMenu, new SeparatorMenuItem(), startMenu, stopMenu, editMenu, new SeparatorMenuItem(), deleteMenu);

        deviceTable.setContextMenu(contextMenu);

        // 动态根据选中状态启用/禁用菜单项
        contextMenu.setOnShowing(e -> {
            boolean hasSelection = !deviceTable.getSelectionModel().getSelectedItems().isEmpty();
            boolean singleSelection = deviceTable.getSelectionModel().getSelectedItems().size() == 1;
            boolean anyRunning = deviceTable.getSelectionModel().getSelectedItems().stream()
                    .anyMatch(d -> "运行中".equals(d.getStatus()));

            startMenu.setDisable(!hasSelection);
            stopMenu.setDisable(!hasSelection);
            editMenu.setDisable(!singleSelection || anyRunning);
            deleteMenu.setDisable(!hasSelection || anyRunning);
        });
    }

    private void toggleDevice(TcpDeviceModel model) {
        if ("运行中".equals(model.getStatus())) {
            stopDevice(model);
        } else {
            startDevice(model);
        }
        deviceTable.refresh();
    }

    private void startDevice(TcpDeviceModel model) {
        if ("运行中".equals(model.getStatus())) return;
        try {
            com.wolfhouse.mod4j.utils.ModbusTcpSimulator simulator = new com.wolfhouse.mod4j.utils.ModbusTcpSimulator(model.getPort());
            simulator.setLogConsumer(model::appendLog);
            updateSimulatorResps(model, simulator);
            simulator.start();
            model.setSimulator(simulator);
            model.setStatus("运行中");
        } catch (IOException e) {
            showError("启动失败: " + e.getMessage());
        }
    }

    private void updateSimulatorResps(TcpDeviceModel model, com.wolfhouse.mod4j.utils.ModbusTcpSimulator simulator) {
        if (simulator == null) return;
        simulator.clearMockResps();
        for (MockResponseModel respModel : model.getMockResponses()) {
            if (respModel.isEnabled()) {
                simulator.addMockResp(respModel.getSlaveId(), respModel.getPair());
            }
        }
        model.appendLog("[系统] 虚拟响应配置已更新");
    }

    private void stopDevice(TcpDeviceModel model) {
        if (model.getSimulator() != null) {
            model.getSimulator().stop();
            model.setSimulator(null);
        }
        model.setStatus("已停止");
    }

    @FXML
    private void handleAddDevice() {
        showDeviceDialog(null);
    }

    private void showDeviceDialog(TcpDeviceModel existingModel) {
        boolean isEdit = existingModel != null;
        Stage stage = new Stage();
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
        nameField.setPromptText("例如：车间 A 模拟器");
        
        TextField portField = new TextField(isEdit ? String.valueOf(existingModel.getPort()) : "502");
        portField.setPromptText("默认 502");
        
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
                if (!isEdit || port != existingModel.getPort()) {
                    if (devices.stream().anyMatch(d -> d.getPort() == port)) {
                        showError("端口 " + port + " 已被占用");
                        return;
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
                deviceTable.refresh();
                stage.close();
            } catch (NumberFormatException ex) {
                showError("无效的端口号");
            }
        };

        saveBtn.setOnAction(e -> saveTask.run());

        // 按下 Enter 保存，排查备注区域
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !remarkArea.isFocused()) {
                saveTask.run();
                event.consume();
            }
        });

        Scene scene = new Scene(root);
        setupDialogCloseShortcuts(stage, scene);
        stage.setScene(scene);
        stage.show();
    }

    private void setupDialogCloseShortcuts(Stage stage, Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
            // 兼容 Cmd+W (Mac) 和 Alt+F4 (Windows 由 OS 处理，但 JavaFX 也可以捕捉)
            KeyCombination closeCombo = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
            if (closeCombo.match(event)) {
                stage.close();
            }
        });
    }

    private void showConsoleDialog(TcpDeviceModel model) {
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
        setupDialogCloseShortcuts(stage, scene);
        if (getClass().getResource("style.css") != null) {
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.show();
    }

    private void showResponseManagementDialog(TcpDeviceModel model) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("响应管理 - 端口 " + model.getPort());

        VBox root = new VBox(0);
        root.getStyleClass().add("dialog-root");
        root.setPrefSize(900, 600);

        boolean isRunning = "运行中".equals(model.getStatus());

        VBox headerPanel = new VBox(16);
        headerPanel.setPadding(new Insets(24));
        headerPanel.getStyleClass().add("header-panel");

        HBox topRow = new HBox(12);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        VBox titleBox = new VBox(4);
        Label title = new Label("虚拟响应管理");
        title.getStyleClass().add("header-title");
        Label subTitle = new Label("配置端口 " + model.getPort() + " 的模拟数据响应");
        subTitle.getStyleClass().add("text-muted");
        titleBox.getChildren().addAll(title, subTitle);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Button batchEnableBtn = new Button("批量启用");
        batchEnableBtn.getStyleClass().addAll("button-outlined", "button-sm");
        batchEnableBtn.setDisable(isRunning);
        
        Button batchDisableBtn = new Button("批量停用");
        batchDisableBtn.getStyleClass().addAll("button-outlined", "button-sm");
        batchDisableBtn.setDisable(isRunning);
        
        Button batchDelBtn = new Button("批量删除");
        batchDelBtn.getStyleClass().addAll("button-outlined", "button-sm", "danger");
        batchDelBtn.setDisable(isRunning);
        
        Button addBtn = new Button("添加响应");
        addBtn.getStyleClass().addAll("accent", "button-sm");
        addBtn.setDisable(isRunning);
        addBtn.setOnAction(e -> showAddResponseDialog(model, null));

        toolbar.getChildren().addAll(batchEnableBtn, batchDisableBtn, batchDelBtn, new Separator(javafx.geometry.Orientation.VERTICAL), addBtn);
        topRow.getChildren().addAll(titleBox, spacer, toolbar);
        headerPanel.getChildren().add(topRow);

        TableView<MockResponseModel> respTable = new TableView<>();
        respTable.setItems(model.getMockResponses());
        respTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        respTable.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(respTable, Priority.ALWAYS);

        batchEnableBtn.setOnAction(e -> {
            respTable.getSelectionModel().getSelectedItems().forEach(r -> r.setEnabled(true));
            updateSimulatorResps(model, model.getSimulator());
        });
        batchDisableBtn.setOnAction(e -> {
            respTable.getSelectionModel().getSelectedItems().forEach(r -> r.setEnabled(false));
            updateSimulatorResps(model, model.getSimulator());
        });
        batchDelBtn.setOnAction(e -> {
            List<MockResponseModel> selected = new ArrayList<>(respTable.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) return;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认批量删除");
            alert.setHeaderText("确认删除选中的 " + selected.size() + " 个响应？");
            if (alert.showAndWait().orElse(null) == ButtonType.OK) {
                model.getMockResponses().removeAll(selected);
                updateSimulatorResps(model, model.getSimulator());
            }
        });

        setupResponseTableContextMenu(respTable, model);

        // 双击编辑响应
        respTable.setRowFactory(tv -> {
            TableRow<MockResponseModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    if (!isRunning) {
                        showAddResponseDialog(model, row.getItem());
                    }
                }
            });
            return row;
        });

        TableColumn<MockResponseModel, Boolean> enabledCol = new TableColumn<>("状态");
        enabledCol.setPrefWidth(80);
        enabledCol.setCellValueFactory(data -> data.getValue().enabledProperty());
        enabledCol.setCellFactory(CheckBoxTableCell.forTableColumn(enabledCol));
        enabledCol.setEditable(!isRunning);
        respTable.setEditable(!isRunning);

        TableColumn<MockResponseModel, String> nameCol = new TableColumn<>("名称");
        nameCol.setPrefWidth(150);
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<MockResponseModel, String> slaveCol = new TableColumn<>("从机ID");
        slaveCol.setPrefWidth(80);
        slaveCol.setCellValueFactory(data -> data.getValue().slaveIdProperty());

        TableColumn<MockResponseModel, Integer> addrCol = new TableColumn<>("寄存器地址");
        addrCol.setPrefWidth(120);
        addrCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getPair().registerAddr()));
        
        TableColumn<MockResponseModel, Integer> sizeCol = new TableColumn<>("大小(B)");
        sizeCol.setPrefWidth(80);
        sizeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getPair().dataSize()));

        TableColumn<MockResponseModel, String> typeCol = new TableColumn<>("数据类型");
        typeCol.setPrefWidth(100);
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPair().randData() ? "随机" : "固定"));

        TableColumn<MockResponseModel, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(150);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("编辑");
            private final Button delBtn = new Button("删除");
            private final HBox pane = new HBox(8, editBtn, delBtn);
            {
                pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                editBtn.getStyleClass().addAll("button-outlined", "button-sm");
                delBtn.getStyleClass().addAll("button-outlined", "button-sm", "danger");
                
                editBtn.setOnAction(e -> {
                    MockResponseModel resp = getTableView().getItems().get(getIndex());
                    showAddResponseDialog(model, resp);
                });

                delBtn.setOnAction(e -> {
                    MockResponseModel resp = getTableView().getItems().get(getIndex());
                    
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("确认删除响应");
                    alert.setHeaderText("确认删除该虚拟响应？");
                    alert.setContentText("地址: " + resp.getPair().registerAddr());
                    
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        model.getMockResponses().remove(resp);
                        updateSimulatorResps(model, model.getSimulator());
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    editBtn.setDisable(isRunning);
                    delBtn.setDisable(isRunning);
                    setGraphic(pane);
                }
            }
        });

        respTable.getColumns().addAll(enabledCol, nameCol, slaveCol, addrCol, sizeCol, typeCol, actionCol);
        root.getChildren().addAll(headerPanel, respTable);
        respTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        model.getMockResponses().forEach(r -> r.enabledProperty().addListener((obs, old, val) -> {
            updateSimulatorResps(model, model.getSimulator());
        }));

        Scene scene = new Scene(root);
        setupDialogCloseShortcuts(stage, scene);
        if (getClass().getResource("style.css") != null) {
            scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        }
        stage.setScene(scene);
        stage.show();
    }

    private void setupResponseTableContextMenu(TableView<MockResponseModel> respTable, TcpDeviceModel model) {
        ContextMenu contextMenu = new ContextMenu();
        boolean isRunning = "运行中".equals(model.getStatus());

        MenuItem addMenu = new MenuItem("添加新响应");
        addMenu.setOnAction(e -> showAddResponseDialog(model, null));
        addMenu.setDisable(isRunning);

        MenuItem enableMenu = new MenuItem("批量启用");
        enableMenu.setOnAction(e -> {
            respTable.getSelectionModel().getSelectedItems().forEach(r -> r.setEnabled(true));
            updateSimulatorResps(model, model.getSimulator());
        });

        MenuItem disableMenu = new MenuItem("批量停用");
        disableMenu.setOnAction(e -> {
            respTable.getSelectionModel().getSelectedItems().forEach(r -> r.setEnabled(false));
            updateSimulatorResps(model, model.getSimulator());
        });

        MenuItem editMenu = new MenuItem("编辑选中响应");
        editMenu.setOnAction(e -> {
            MockResponseModel selected = respTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAddResponseDialog(model, selected);
            }
        });

        MenuItem deleteMenu = new MenuItem("批量删除");
        deleteMenu.getStyleClass().add("danger");
        deleteMenu.setOnAction(e -> {
            List<MockResponseModel> selected = new ArrayList<>(respTable.getSelectionModel().getSelectedItems());
            model.getMockResponses().removeAll(selected);
            updateSimulatorResps(model, model.getSimulator());
        });

        contextMenu.getItems().addAll(addMenu, new SeparatorMenuItem(), enableMenu, disableMenu, editMenu, new SeparatorMenuItem(), deleteMenu);
        respTable.setContextMenu(contextMenu);

        contextMenu.setOnShowing(e -> {
            boolean hasSelection = !respTable.getSelectionModel().getSelectedItems().isEmpty();
            boolean singleSelection = respTable.getSelectionModel().getSelectedItems().size() == 1;
            boolean currentlyRunning = "运行中".equals(model.getStatus());

            addMenu.setDisable(currentlyRunning);
            enableMenu.setDisable(!hasSelection || currentlyRunning);
            disableMenu.setDisable(!hasSelection || currentlyRunning);
            editMenu.setDisable(!singleSelection || currentlyRunning);
            deleteMenu.setDisable(!hasSelection || currentlyRunning);
        });
    }

    private void showAddResponseDialog(TcpDeviceModel model, MockResponseModel existingResp) {
        boolean isEdit = existingResp != null;
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(isEdit ? "编辑虚拟响应" : "添加虚拟响应");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(30));

        TextField nameField = new TextField(isEdit ? existingResp.getName() : "");
        TextField slaveIdField = new TextField(isEdit ? existingResp.getSlaveId() : "1");
        TextField addrField = new TextField(isEdit ? "0x" + Integer.toHexString(existingResp.getPair().registerAddr()) : "0");
        TextField sizeField = new TextField(isEdit ? String.valueOf(existingResp.getPair().dataSize()) : "2");
        CheckBox randCheckBox = new CheckBox("随机数据");
        randCheckBox.setSelected(isEdit && existingResp.getPair().randData());
        TextField dataField = new TextField();
        if (isEdit && !existingResp.getPair().randData() && existingResp.getPair().data() != null) {
            StringBuilder sb = new StringBuilder();
            for (byte b : existingResp.getPair().data()) {
                sb.append(String.format("%02x ", b));
            }
            dataField.setText(sb.toString().trim());
        } else if (!isEdit) {
            dataField.setText("00 01");
        }
        TextArea remarkArea = new TextArea(isEdit ? existingResp.getRemark() : "");
        remarkArea.setPrefRowCount(2);

        grid.add(new Label("名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("从机 ID:"), 0, 1);
        grid.add(slaveIdField, 1, 1);
        grid.add(new Label("地址 (十进制/0x十六进制):"), 0, 2);
        grid.add(addrField, 1, 2);
        grid.add(new Label("大小 (字节):"), 0, 3);
        grid.add(sizeField, 1, 3);
        grid.add(randCheckBox, 0, 4);
        grid.add(new Label("固定数据 (十六进制):"), 0, 5);
        grid.add(dataField, 1, 5);
        grid.add(new Label("备注:"), 0, 6);
        grid.add(remarkArea, 1, 6);

        dataField.disableProperty().bind(randCheckBox.selectedProperty());

        Button saveBtn = new Button("保存");
        saveBtn.getStyleClass().add("success");
        saveBtn.setPrefWidth(100);
        
        Runnable saveTask = () -> {
            try {
                int addr = parseAddress(addrField.getText());
                int size = Integer.parseInt(sizeField.getText());
                boolean rand = randCheckBox.isSelected();
                byte[] data = null;
                if (!rand) {
                    data = parseHexData(dataField.getText());
                }
                
                com.wolfhouse.mod4j.utils.ModbusTcpSimulator.MockRespPair pair = new com.wolfhouse.mod4j.utils.ModbusTcpSimulator.MockRespPair(addr, size, rand, data);
                if (isEdit) {
                    existingResp.setName(nameField.getText());
                    existingResp.setSlaveId(slaveIdField.getText());
                    existingResp.setPair(pair);
                    existingResp.setRemark(remarkArea.getText());
                } else {
                    MockResponseModel respModel = new MockResponseModel(slaveIdField.getText(), pair);
                    respModel.setName(nameField.getText());
                    respModel.setRemark(remarkArea.getText());
                    model.getMockResponses().add(respModel);
                    respModel.enabledProperty().addListener((obs, old, val) -> updateSimulatorResps(model, model.getSimulator()));
                }

                updateSimulatorResps(model, model.getSimulator());
                stage.close();
            } catch (Exception ex) {
                showError("输入无效: " + ex.getMessage());
            }
        };

        saveBtn.setOnAction(e -> saveTask.run());

        // 按下 Enter 保存，排除备注区域
        grid.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !remarkArea.isFocused()) {
                saveTask.run();
                event.consume();
            }
        });

        grid.add(saveBtn, 1, 7);

        Scene scene = new Scene(grid);
        setupDialogCloseShortcuts(stage, scene);
        stage.setScene(scene);
        stage.show();
    }

    private int parseAddress(String text) {
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return Integer.parseInt(text.substring(2), 16);
        }
        return Integer.parseInt(text);
    }

    private byte[] parseHexData(String text) {
        String[] parts = text.trim().split("\\s+");
        byte[] data = new byte[parts.length];
        for (int i = 0; i < parts.length; i++) {
            data[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return data;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
