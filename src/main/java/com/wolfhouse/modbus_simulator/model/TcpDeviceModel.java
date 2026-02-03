package com.wolfhouse.modbus_simulator.model;

import com.wolfhouse.mod4j.utils.ModbusTcpSimulator;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

public class TcpDeviceModel implements SimulatorModel {
    private final StringProperty                    name          = new SimpleStringProperty("");
    private final StringProperty                    remark        = new SimpleStringProperty("");
    private final IntegerProperty                   port          = new SimpleIntegerProperty();
    private final StringProperty                    status        = new SimpleStringProperty("已停止");
    @Getter
    private final ObservableList<MockResponseModel> mockResponses = FXCollections.observableArrayList();
    private final StringProperty                    logs          = new SimpleStringProperty("");
    @Getter
    @Setter
    private       ModbusTcpSimulator                simulator;

    public TcpDeviceModel(int port) {
        this.port.set(port);
    }

    public String getLogs()              {return logs.get();}

    public void setLogs(String logs)     {this.logs.set(logs);}

    public StringProperty logsProperty() {return logs;}

    public void appendLog(String log) {
        if (com.wolfhouse.modbus_simulator.DeviceManager.getInstance().isShuttingDown()) {
            return;
        }
        if (javafx.application.Platform.isFxApplicationThread()) {
            if (this.logs.get() == null) {
                return;
            }
            this.logs.set(this.logs.get() + log + "\n");
        } else {
            javafx.application.Platform.runLater(() -> {
                if (com.wolfhouse.modbus_simulator.DeviceManager.getInstance().isShuttingDown()) {
                    return;
                }
                if (this.logs.get() == null) {
                    return;
                }
                this.logs.set(this.logs.get() + log + "\n");
            });
        }
    }

    public void clearLogs()                {this.logs.set("");}

    public String getName()                {return name.get();}

    public void setName(String name)       {this.name.set(name);}

    public StringProperty nameProperty()   {return name;}

    public String getRemark()              {return remark.get();}

    public void setRemark(String remark)   {this.remark.set(remark);}

    public StringProperty remarkProperty() {return remark;}

    public int getPort()                   {return port.get();}

    public void setPort(int port)          {this.port.set(port);}

    public IntegerProperty portProperty()  {return port;}

    public String getStatus()              {return status.get();}

    public void setStatus(String status)   {this.status.set(status);}

    public StringProperty statusProperty() {return status;}

}
