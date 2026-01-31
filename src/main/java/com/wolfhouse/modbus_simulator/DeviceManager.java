package com.wolfhouse.modbus_simulator;

import com.wolfhouse.modbus_simulator.model.TcpDeviceModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

public class DeviceManager {
    private static DeviceManager instance;

    @Getter
    private final ObservableList<TcpDeviceModel> tcpDevices = FXCollections.observableArrayList();

    private volatile boolean shuttingDown = false;

    private DeviceManager() {}

    public static synchronized DeviceManager getInstance() {
        if (instance == null) {
            instance = new DeviceManager();
        }
        return instance;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public void stopAll() {
        shuttingDown = true;
        tcpDevices.forEach(device -> {
            if (device.getSimulator() != null) {
                device.getSimulator().stop();
                device.setSimulator(null);
                device.setStatus("已停止");
            }
        });
    }
}
