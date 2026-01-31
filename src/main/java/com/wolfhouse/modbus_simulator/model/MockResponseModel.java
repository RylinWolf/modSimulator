package com.wolfhouse.modbus_simulator.model;

import com.wolfhouse.mod4j.utils.ModbusTcpSimulator;
import javafx.beans.property.*;

public class MockResponseModel {
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty remark = new SimpleStringProperty("");
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);
    private final ObjectProperty<ModbusTcpSimulator.MockRespPair> pair = new SimpleObjectProperty<>();
    private final StringProperty slaveId = new SimpleStringProperty("1");

    public MockResponseModel(String slaveId, ModbusTcpSimulator.MockRespPair pair) {
        this.slaveId.set(slaveId);
        this.pair.set(pair);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    public String getRemark() { return remark.get(); }
    public StringProperty remarkProperty() { return remark; }
    public void setRemark(String remark) { this.remark.set(remark); }

    public boolean isEnabled() { return enabled.get(); }
    public BooleanProperty enabledProperty() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled.set(enabled); }

    public String getSlaveId() { return slaveId.get(); }
    public StringProperty slaveIdProperty() { return slaveId; }
    public void setSlaveId(String slaveId) { this.slaveId.set(slaveId); }

    public ModbusTcpSimulator.MockRespPair getPair() { return pair.get(); }
    public ObjectProperty<ModbusTcpSimulator.MockRespPair> pairProperty() { return pair; }
    public void setPair(ModbusTcpSimulator.MockRespPair pair) { this.pair.set(pair); }
}
