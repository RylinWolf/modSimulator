package com.wolfhouse.modbus_simulator.model;

import com.wolfhouse.mod4j.utils.ModbusTcpSimulator;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TcpDeviceModel {
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty remark = new SimpleStringProperty("");
    private final IntegerProperty port = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty("已停止");
    private final ObservableList<MockResponseModel> mockResponses = FXCollections.observableArrayList();
    private ModbusTcpSimulator simulator;
    private final StringProperty logs = new SimpleStringProperty("");

    public TcpDeviceModel(int port) {
        this.port.set(port);
    }

    public String getLogs() { return logs.get(); }
    public StringProperty logsProperty() { return logs; }
    public void setLogs(String logs) { this.logs.set(logs); }
    public void appendLog(String log) {
        if (com.wolfhouse.modbus_simulator.DeviceManager.getInstance().isShuttingDown()) {
            return;
        }
        if (javafx.application.Platform.isFxApplicationThread()) {
            if (this.logs.get() == null) return;
            this.logs.set(this.logs.get() + log + "\n");
        } else {
            javafx.application.Platform.runLater(() -> {
                if (com.wolfhouse.modbus_simulator.DeviceManager.getInstance().isShuttingDown()) {
                    return;
                }
                if (this.logs.get() == null) return;
                this.logs.set(this.logs.get() + log + "\n");
            });
        }
    }
    public void clearLogs() { this.logs.set(""); }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    public String getRemark() { return remark.get(); }
    public StringProperty remarkProperty() { return remark; }
    public void setRemark(String remark) { this.remark.set(remark); }

    public int getPort() { return port.get(); }
    public IntegerProperty portProperty() { return port; }
    public void setPort(int port) { this.port.set(port); }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }

    public ObservableList<MockResponseModel> getMockResponses() { return mockResponses; }

    public ModbusTcpSimulator getSimulator() { return simulator; }
    public void setSimulator(ModbusTcpSimulator simulator) { this.simulator = simulator; }
}
