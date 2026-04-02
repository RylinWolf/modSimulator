package com.wolfhouse.modbus_simulator.service;

import com.wolfhouse.mod4j.utils.HexUtils;
import com.wolfhouse.modbus_simulator.model.MockResponseModel;
import com.wolfhouse.modbus_simulator.model.ProgramStatusContext;
import com.wolfhouse.modbus_simulator.model.TcpDeviceModel;
import com.wolfhouse.modbus_simulator.util.LogUtil;
import com.wolfhouse.modbus_simulator.util.WindowUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.*;

/**
 * 虚拟响应服务
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"unchecked"})
public class MockResponseService {
    private MockResponseService() {}

    /**
     * 显示添加或修改虚拟响应对话框
     *
     * @param model        要添加响应的模型
     * @param existingResp 若为 null 则为添加，否则为修改
     */
    public static Stage showAddResponseDialog(TcpDeviceModel model, MockResponseModel existingResp) {
        boolean isEdit = existingResp != null;
        Stage   stage  = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(isEdit ? "编辑虚拟响应" : "添加虚拟响应");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(30));

        TextField nameField    = new TextField(isEdit ? existingResp.getName() : "");
        TextField slaveIdField = new TextField(isEdit ? existingResp.getSlaveId() : "01");
        TextField addrField    = new TextField(isEdit ? existingResp.getRegAddr() : "0x0000");
        TextField sizeField    = new TextField(isEdit ? String.valueOf(existingResp.getPair().dataSize()) : "2");
        CheckBox  randCheckBox = new CheckBox("随机数据");
        randCheckBox.setSelected(isEdit && existingResp.getPair().randData());

        TextField functionCodeField = new TextField(isEdit ? String.valueOf(existingResp.getFunctionCode()) : "3");
        functionCodeField.setPromptText("例如：3 (0-255)");

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
        remarkArea.setPrefRowCount(3);
        remarkArea.setWrapText(true);

        grid.add(new Label("名称:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("从机 ID:"), 0, 1);
        grid.add(slaveIdField, 1, 1);
        grid.add(new Label("地址 (十进制/0x十六进制):"), 0, 2);
        grid.add(addrField, 1, 2);
        grid.add(new Label("大小 (字节):"), 0, 3);
        grid.add(sizeField, 1, 3);
        grid.add(new Label("功能码:"), 0, 4);
        grid.add(functionCodeField, 1, 4);
        grid.add(randCheckBox, 0, 5);
        grid.add(new Label("固定数据 (十六进制):"), 0, 6);
        grid.add(dataField, 1, 6);
        grid.add(new Label("备注:"), 0, 7);
        grid.add(remarkArea, 1, 7);

        dataField.disableProperty().bind(randCheckBox.selectedProperty());

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 30, 30, 30));

        Button cancelBtn = new Button("取消");
        cancelBtn.setOnAction(e -> stage.close());

        Button saveBtn = new Button("保存");
        saveBtn.getStyleClass().add("success");
        saveBtn.setPrefWidth(100);

        footer.getChildren().addAll(cancelBtn, saveBtn);

        VBox root = new VBox();
        root.getChildren().addAll(grid, footer);
        Scene scene = new Scene(root);
        stage.setScene(scene);

        Runnable saveTask = () -> {
            try {
                String  addrText = addrField.getText();
                String  sizeText = sizeField.getText();
                int     addr     = addrText == null ? 0x0000 : HexUtils.parseInt(addrText);
                int     size     = sizeText == null ? 0x02 : Integer.parseInt(sizeText);
                String  funcText = functionCodeField.getText();
                int     fc       = funcText == null ? 0x03 : Integer.parseInt(funcText);
                boolean rand     = randCheckBox.isSelected();
                byte[]  data     = null;
                if (!rand) {
                    data = HexUtils.parseHexData(dataField.getText().replaceAll("\\s+", ""), 2);
                }

                com.wolfhouse.mod4j.utils.ModbusTcpSimulator.MockRespPair pair = new com.wolfhouse.mod4j.utils.ModbusTcpSimulator.MockRespPair(addr, size, rand, data, fc);
                if (isEdit) {
                    existingResp.setName(nameField.getText());
                    existingResp.setSlaveId(slaveIdField.getText());
                    existingResp.setFunctionCode(fc);
                    existingResp.setPair(pair);
                    existingResp.setRemark(remarkArea.getText());
                    existingResp.setDataSize(sizeField.getText());
                    existingResp.setDataType(rand ? "随机" : "固定");
                    existingResp.setRegAddr(addrField.getText());
                } else {
                    MockResponseModel respModel = new MockResponseModel(slaveIdField.getText(), addrField.getText(), pair);
                    respModel.setName(nameField.getText());
                    respModel.setRemark(remarkArea.getText());
                    model.getMockResponses().add(respModel);
                    respModel.enabledProperty().addListener((obs, old, val) -> TcpSimulatorService.updateSimulatorResps(model, model.getSimulator()));
                }

                TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
                // 更新状态
                ProgramStatusContext.unsaved();
                // 新增状态，则保存后立刻关闭窗口
                if (!isEdit) {
                    stage.close();
                }
            } catch (Exception ex) {
                LogUtil.error("响应编辑失败", ex);
                WindowUtil.showError("输入无效", ex, stage);
            }
        };

        saveBtn.setOnAction(_ -> {
            saveTask.run();
            stage.close();
        });
        grid.setOnKeyPressed(event -> {
            // 按下 Enter 保存，排除备注区域
            if (event.getCode() == KeyCode.ENTER && !remarkArea.isFocused()) {
                saveTask.run();
                event.consume();
                stage.close();
            }
        });
        // 按下 cmd+s / ctrl+s 保存
        WindowUtil.addSaveShortcut(root, saveTask, true);

        WindowUtil.setupDialogCloseShortcuts(stage, scene);
        return stage;
    }

    public static Stage showResponseManagementDialog(TcpDeviceModel model) {
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
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox  titleBox = new VBox(4);
        Label title    = new Label("虚拟响应管理");
        title.getStyleClass().add("header-title");
        Label subTitle = new Label("配置端口 " + model.getPort() + " 的模拟数据响应");
        subTitle.getStyleClass().add("text-muted");
        titleBox.getChildren().addAll(title, subTitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button batchEnableBtn = new Button("批量启用");
        batchEnableBtn.getStyleClass().addAll("button-outlined", "button-sm");
        batchEnableBtn.setDisable(isRunning);
        batchEnableBtn.setGraphic(new FontIcon("mdi2p-play"));

        Button batchDisableBtn = new Button("批量停用");
        batchDisableBtn.getStyleClass().addAll("button-outlined", "button-sm");
        batchDisableBtn.setDisable(isRunning);
        batchDisableBtn.setGraphic(new FontIcon("mdi2s-stop"));


        Button batchDelBtn = new Button("批量删除");
        batchDelBtn.getStyleClass().addAll("button-outlined", "button-sm", "danger");
        batchDelBtn.setDisable(isRunning);
        batchDelBtn.setGraphic(new FontIcon("mdi2t-trash-can-outline"));


        Button addBtn = new Button("添加响应");
        addBtn.getStyleClass().addAll("accent", "button-sm");
        addBtn.setDisable(isRunning);
        addBtn.setGraphic(new FontIcon("mdi2p-plus"));

        addBtn.setOnAction(e -> WindowUtil.showBased(stage, showAddResponseDialog(model, null)));

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
            TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
            // 更新保存状态
            ProgramStatusContext.UNSAVED.set(true);
        });
        batchDisableBtn.setOnAction(e -> {
            respTable.getSelectionModel().getSelectedItems().forEach(r -> r.setEnabled(false));
            TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
            // 更新保存状态
            ProgramStatusContext.UNSAVED.set(true);
        });

        // 删除操作
        Runnable delAction = () -> {
            List<MockResponseModel> selected = new ArrayList<>(respTable.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                return;
            }
            Optional<ButtonType> result = WindowUtil.showAlert(Alert.AlertType.CONFIRMATION,
                                                               "确认删除",
                                                               "确认删除选中的 " + selected.size() + " 个响应？",
                                                               "此操作无法恢复",
                                                               stage,
                                                               ButtonType.OK, ButtonType.CANCEL);
            if (result.orElse(null) == ButtonType.OK) {
                model.getMockResponses().removeAll(selected);
                TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
                // 更新保存状态
                ProgramStatusContext.UNSAVED.set(true);
            }
        };
        // 删除按钮事件
        batchDelBtn.setOnAction(_ -> delAction.run());
        // 绑定删除快捷键
        WindowUtil.addDeleteShortcut(stage, delAction, true);

        setupResponseTableContextMenu(respTable, model, stage);

        // 双击编辑响应
        respTable.setRowFactory(tv -> {
            TableRow<MockResponseModel> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    if (!isRunning) {
                        WindowUtil.showBased(stage, showAddResponseDialog(model, row.getItem()));
                        respTable.refresh();
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

        TableColumn<MockResponseModel, String> addrCol = new TableColumn<>("寄存器地址");
        addrCol.setPrefWidth(120);
        addrCol.setCellValueFactory(data -> data.getValue().regAddrProperty());

        TableColumn<MockResponseModel, Integer> fcCol = new TableColumn<>("功能码");
        fcCol.setPrefWidth(80);
        fcCol.setCellValueFactory(data -> data.getValue().functionCodeProperty().asObject());

        TableColumn<MockResponseModel, String> sizeCol = new TableColumn<>("大小(Byte)");
        sizeCol.setPrefWidth(80);
        sizeCol.setCellValueFactory(data -> data.getValue().dataSizeProperty());

        TableColumn<MockResponseModel, String> typeCol = new TableColumn<>("数据类型");
        typeCol.setPrefWidth(100);
        typeCol.setCellValueFactory(data -> data.getValue().dataTypeProperty());

        TableColumn<MockResponseModel, Void> actionCol = getActionCol(model, stage, isRunning);

        respTable.getColumns().addAll(enabledCol, nameCol, slaveCol, addrCol, fcCol, sizeCol, typeCol, actionCol);
        root.getChildren().addAll(headerPanel, respTable);
        respTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        model.getMockResponses().forEach(r -> r.enabledProperty().addListener((obs, old, val) -> {
            TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
        }));

        Scene scene = new Scene(root);
        WindowUtil.setupDialogCloseShortcuts(stage, scene);
        URL resource = MockResponseService.class.getResource("style.css");
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        }
        stage.setScene(scene);
        return stage;
    }

    private static TableColumn<MockResponseModel, Void> getActionCol(TcpDeviceModel model, Stage stage, boolean isRunning) {
        TableColumn<MockResponseModel, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(150);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("编辑");
            private final Button delBtn  = new Button("删除");
            private final HBox   pane    = new HBox(8, editBtn, delBtn);

            {
                pane.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().addAll("button-outlined", "button-sm");
                delBtn.getStyleClass().addAll("button-outlined", "button-sm", "danger");

                editBtn.setOnAction(_ -> {
                    MockResponseModel resp = getTableView().getItems().get(getIndex());
                    WindowUtil.showBased(stage, showAddResponseDialog(model, resp));
                });

                delBtn.setOnAction(_ -> {
                    MockResponseModel resp = getTableView().getItems().get(getIndex());

                    Optional<ButtonType> result = WindowUtil.showAlert(Alert.AlertType.CONFIRMATION,
                                                                       "确认删除响应",
                                                                       "确认删除该虚拟响应？",
                                                                       "地址: " + String.format("0x%04x", resp.getPair().registerAddr()),
                                                                       stage,
                                                                       ButtonType.OK, ButtonType.CANCEL);

                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        model.getMockResponses().remove(resp);
                        TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
                        // 更新保存状态
                        ProgramStatusContext.UNSAVED.set(true);
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
        return actionCol;
    }

    public static void setupResponseTableContextMenu(TableView<MockResponseModel> respTable, TcpDeviceModel model, Stage based) {
        ContextMenu contextMenu = new ContextMenu();
        boolean     isRunning   = "运行中".equals(model.getStatus());

        MenuItem addMenu = new MenuItem("添加新响应");
        addMenu.setOnAction(e -> WindowUtil.showBased(based, showAddResponseDialog(model, null)));
        addMenu.setDisable(isRunning);

        MenuItem enableMenu = new MenuItem("启用");
        enableMenu.setOnAction(e -> {
            respTable.getSelectionModel().getSelectedItems().forEach(r -> r.setEnabled(true));
            TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
        });

        MenuItem disableMenu = new MenuItem("停用");
        disableMenu.setOnAction(e -> {
            respTable.getSelectionModel().getSelectedItems().forEach(r -> r.setEnabled(false));
            TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
        });

        MenuItem editMenu = new MenuItem("编辑");
        editMenu.setOnAction(e -> {
            MockResponseModel selected = respTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                WindowUtil.showBased(based, showAddResponseDialog(model, selected));
                respTable.refresh();
            }
        });

        MenuItem deleteMenu = new MenuItem("删除");
        deleteMenu.getStyleClass().add("danger");
        deleteMenu.setOnAction(e -> {
            List<MockResponseModel> selected = new ArrayList<>(respTable.getSelectionModel().getSelectedItems());
            if (selected.isEmpty()) {
                return;
            }
            Optional<ButtonType> result = WindowUtil.showAlert(Alert.AlertType.CONFIRMATION,
                                                               "确认删除响应",
                                                               "确认删除选中的虚拟响应？",
                                                               "选中响应数量: " + selected.size() + "个",
                                                               based,
                                                               ButtonType.OK, ButtonType.CANCEL);
            if (result.orElse(null) != ButtonType.OK) {
                return;
            }
            model.getMockResponses().removeAll(selected);
            TcpSimulatorService.updateSimulatorResps(model, model.getSimulator());
            // 更新保存状态
            ProgramStatusContext.UNSAVED.set(true);
        });

        contextMenu.getItems().addAll(addMenu, new SeparatorMenuItem(), enableMenu, disableMenu, editMenu, new SeparatorMenuItem(), deleteMenu);
        respTable.setContextMenu(contextMenu);

        contextMenu.setOnShowing(e -> {
            boolean hasSelection     = !respTable.getSelectionModel().getSelectedItems().isEmpty();
            boolean singleSelection  = respTable.getSelectionModel().getSelectedItems().size() == 1;
            boolean currentlyRunning = "运行中".equals(model.getStatus());

            addMenu.setDisable(currentlyRunning);
            enableMenu.setDisable(!hasSelection || currentlyRunning);
            disableMenu.setDisable(!hasSelection || currentlyRunning);
            editMenu.setDisable(!singleSelection || currentlyRunning);
            deleteMenu.setDisable(!hasSelection || currentlyRunning);
        });
    }

    /**
     * 延时配置对话框
     *
     * @param model TCP 设备模型
     * @return 窗口
     */
    public static Stage showTimeoutDialog(TcpDeviceModel model) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("延时配置 - 端口 " + model.getPort());

        boolean isRunning = "运行中".equals(model.getStatus());

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        Label title = new Label("虚拟延时与成功率配置");
        title.getStyleClass().add("header-title");
        Label hint = new Label("支持端口与主机位粒度配置：延时(ms)、成功率(0~1)");
        hint.getStyleClass().add("text-muted");

        HBox globalBox = new HBox(16);
        globalBox.setAlignment(Pos.CENTER_LEFT);
        Label     globalLabel = new Label("全局延时(ms):");
        TextField globalField = new TextField(String.valueOf(model.getGlobalTimeoutMs()));
        globalField.setPrefWidth(120);
        Label     globalSuccessLabel = new Label("全局成功率:");
        TextField globalSuccessField = new TextField(String.valueOf(model.getGlobalSuccessRate()));
        globalSuccessField.setPrefWidth(120);
        globalField.setDisable(isRunning);
        globalSuccessField.setDisable(isRunning);
        globalBox.getChildren().addAll(globalLabel, globalField, globalSuccessLabel, globalSuccessField);

        TableView<HostTimeoutRow> timeoutTable = new TableView<>();
        timeoutTable.setEditable(!isRunning);
        timeoutTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(timeoutTable, Priority.ALWAYS);

        TableColumn<HostTimeoutRow, String> hostCol = new TableColumn<>("主机位");
        hostCol.setCellValueFactory(data -> data.getValue().hostProperty());
        hostCol.setEditable(false);

        TableColumn<HostTimeoutRow, Integer> timeoutCol = new TableColumn<>("延时(ms)");
        timeoutCol.setCellValueFactory(data -> data.getValue().timeoutProperty().asObject());
        timeoutCol.setEditable(!isRunning);
        timeoutCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        timeoutCol.setOnEditCommit(event -> {
            Integer value = event.getNewValue();
            if (value == null || value < 0) {
                event.getRowValue().setTimeout(0);
                timeoutTable.refresh();
                return;
            }
            event.getRowValue().setTimeout(value);
        });

        TableColumn<HostTimeoutRow, Double> successCol = new TableColumn<>("成功率");
        successCol.setCellValueFactory(data -> data.getValue().successRateProperty().asObject());
        successCol.setEditable(!isRunning);
        successCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        successCol.setOnEditCommit(event -> {
            Double value = event.getNewValue();
            if (value == null || value < 0.0D || value > 1.0D) {
                event.getRowValue().setSuccessRate(1.0D);
                timeoutTable.refresh();
                return;
            }
            event.getRowValue().setSuccessRate(value);
        });

        timeoutTable.getColumns().addAll(hostCol, timeoutCol, successCol);
        timeoutTable.getItems().addAll(buildHostRows(model));

        if (timeoutTable.getItems().isEmpty()) {
            timeoutTable.setPlaceholder(new Label("当前端口下暂无主机位，请先配置虚拟响应"));
        }

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button(isRunning ? "关闭" : "取消");
        Button saveBtn   = new Button("保存");
        saveBtn.getStyleClass().add("success");
        saveBtn.setDisable(isRunning);
        cancelBtn.setOnAction(_ -> stage.close());

        Runnable saveAction = () -> {
            Integer globalTimeout = parseTimeout(globalField.getText(), "全局延时", stage);
            if (globalTimeout == null) {
                return;
            }
            Double globalSuccessRate = parseSuccessRate(globalSuccessField.getText(), "全局成功率", stage);
            if (globalSuccessRate == null) {
                return;
            }
            Map<String, Integer> hostTimeouts     = new LinkedHashMap<>();
            Map<String, Double>  hostSuccessRates = new LinkedHashMap<>();
            for (HostTimeoutRow row : timeoutTable.getItems()) {
                int    timeout     = row.getTimeout();
                double successRate = row.getSuccessRate();
                if (timeout < 0) {
                    WindowUtil.showAlert(Alert.AlertType.WARNING, "输入错误", "参数校验失败", "主机位延时必须是大于等于 0 的整数", stage, ButtonType.OK);
                    return;
                }
                if (successRate < 0.0D || successRate > 1.0D) {
                    WindowUtil.showAlert(Alert.AlertType.WARNING, "输入错误", "参数校验失败", "主机位成功率必须是 0 到 1 之间的小数", stage, ButtonType.OK);
                    return;
                }
                hostTimeouts.put(row.getHost(), timeout);
                hostSuccessRates.put(row.getHost(), successRate);
            }

            model.setGlobalTimeoutMs(globalTimeout);
            model.setHostTimeoutMs(hostTimeouts);
            model.setGlobalSuccessRate(globalSuccessRate);
            model.setHostSuccessRates(hostSuccessRates);
            TcpSimulatorService.updateSimulatorTimeouts(model, model.getSimulator());
            ProgramStatusContext.unsaved();
            stage.close();
        };

        saveBtn.setOnAction(_ -> saveAction.run());
        WindowUtil.addSaveShortcut(root, saveAction, !isRunning);

        footer.getChildren().addAll(cancelBtn, saveBtn);
        root.getChildren().addAll(title, hint, new Separator(), globalBox, timeoutTable, footer);

        Scene scene = new Scene(root, 620, 480);
        WindowUtil.setupDialogCloseShortcuts(stage, scene);
        URL resource = MockResponseService.class.getResource("style.css");
        if (resource != null) {
            scene.getStylesheets().add(resource.toExternalForm());
        }
        stage.setScene(scene);
        return stage;
    }

    private static Integer parseTimeout(String text, String fieldName, Stage stage) {
        try {
            int timeout = Integer.parseInt(text == null ? "" : text.trim());
            if (timeout < 0) {
                throw new NumberFormatException();
            }
            return timeout;
        } catch (NumberFormatException e) {
            WindowUtil.showAlert(Alert.AlertType.WARNING, "输入错误", "参数校验失败", fieldName + "必须是大于等于 0 的整数", stage, ButtonType.OK);
            return null;
        }
    }

    private static Double parseSuccessRate(String text, String fieldName, Stage stage) {
        try {
            double rate = Double.parseDouble(text == null ? "" : text.trim());
            if (rate < 0.0D || rate > 1.0D) {
                throw new NumberFormatException();
            }
            return rate;
        } catch (NumberFormatException e) {
            WindowUtil.showAlert(Alert.AlertType.WARNING, "输入错误", "参数校验失败", fieldName + "必须是 0 到 1 之间的小数", stage, ButtonType.OK);
            return null;
        }
    }

    private static List<HostTimeoutRow> buildHostRows(TcpDeviceModel model) {
        List<String> hosts = model.getMockResponses()
                                  .stream()
                                  .map(MockResponseModel::getSlaveId)
                                  .filter(v -> v != null && !v.isBlank())
                                  .map(String::trim)
                                  .distinct()
                                  .sorted(Comparator.comparingInt(MockResponseService::parseHostSortValue))
                                  .toList();
        List<HostTimeoutRow> rows = new ArrayList<>(hosts.size());
        for (String host : hosts) {
            rows.add(new HostTimeoutRow(host,
                                        model.getHostTimeoutMs().getOrDefault(host, 0),
                                        model.getHostSuccessRates().getOrDefault(host, 1.0D)));
        }
        return rows;
    }

    private static int parseHostSortValue(String host) {
        try {
            return Integer.parseInt(host, 16);
        } catch (NumberFormatException ignored) {
            try {
                return Integer.parseInt(host);
            } catch (NumberFormatException ignoredAgain) {
                return Integer.MAX_VALUE;
            }
        }
    }

    private static class HostTimeoutRow {
        private final javafx.beans.property.StringProperty  host        = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.IntegerProperty timeout     = new javafx.beans.property.SimpleIntegerProperty();
        private final javafx.beans.property.DoubleProperty  successRate = new javafx.beans.property.SimpleDoubleProperty(1.0D);

        HostTimeoutRow(String host, int timeout, double successRate) {
            this.host.set(host);
            this.timeout.set(Math.max(timeout, 0));
            this.successRate.set(Math.clamp(successRate, 0.0D, 1.0D));
        }

        public String getHost()                                           {return host.get();}

        public javafx.beans.property.StringProperty hostProperty()        {return host;}

        public int getTimeout()                                           {return timeout.get();}

        public void setTimeout(int timeout)                               {this.timeout.set(Math.max(timeout, 0));}

        public javafx.beans.property.IntegerProperty timeoutProperty()    {return timeout;}

        public double getSuccessRate()                                    {return successRate.get();}

        public void setSuccessRate(double successRate)                    {this.successRate.set(Math.max(0.0D, Math.min(successRate, 1.0D)));}

        public javafx.beans.property.DoubleProperty successRateProperty() {return successRate;}
    }
}
