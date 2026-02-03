package com.wolfhouse.modbus_simulator.model;

import com.wolfhouse.mod4j.utils.ModbusTcpSimulator;
import javafx.beans.property.*;

/**
 * @author Rylin Wolf
 */
public class MockResponseModel implements SimulatorModel {
    private final StringProperty                                  name     = new SimpleStringProperty("");
    private final StringProperty                                  remark   = new SimpleStringProperty("");
    private final BooleanProperty                                 enabled  = new SimpleBooleanProperty(true);
    private final StringProperty                                  slaveId  = new SimpleStringProperty("1");
    private final StringProperty                                  regAddr  = new SimpleStringProperty("0");
    private final StringProperty                                  dataSize = new SimpleStringProperty("2");
    private final StringProperty                                  dataType = new SimpleStringProperty("固定");
    private final ObjectProperty<ModbusTcpSimulator.MockRespPair> pair     = new SimpleObjectProperty<>();

    public MockResponseModel(String slaveId, ModbusTcpSimulator.MockRespPair pair) {
        this.slaveId.set(slaveId);
        this.pair.set(pair);
        this.regAddr.set(String.format("%04x", pair.registerAddr()));
        this.dataSize.set(String.valueOf(pair.dataSize()));
        this.dataType.set(pair.randData() ? "随机" : "固定");
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getRemark() {
        return remark.get();
    }

    public void setRemark(String remark) {
        this.remark.set(remark);
    }

    public StringProperty remarkProperty() {
        return remark;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public String getSlaveId() {
        return slaveId.get();
    }

    public void setSlaveId(String slaveId) {
        this.slaveId.set(slaveId);
    }

    public StringProperty slaveIdProperty() {
        return slaveId;
    }

    public ModbusTcpSimulator.MockRespPair getPair() {
        return pair.get();
    }

    public void setPair(ModbusTcpSimulator.MockRespPair pair) {
        this.pair.set(pair);
    }

    public ObjectProperty<ModbusTcpSimulator.MockRespPair> pairProperty() {
        return pair;
    }

    public String getRegAddr() {
        return regAddr.get();
    }

    public void setRegAddr(String regAddr) {
        this.regAddr.set(regAddr);
    }

    public String getDataSize() {
        return dataSize.get();
    }

    public void setDataSize(String dataSize) {
        this.dataSize.set(dataSize);
    }

    public String getDataType() {
        return dataType.get();
    }

    public void setDataType(String dataType) {
        this.dataType.set(dataType);
    }

    public StringProperty regAddrProperty() {
        return regAddr;
    }

    public StringProperty dataSizeProperty() {
        return dataSize;
    }

    public StringProperty dataTypeProperty() {
        return dataType;
    }
}
