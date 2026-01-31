module com.wolfhouse.modbus_simulator {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires com.fazecast.jSerialComm;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    opens com.wolfhouse.modbus_simulator to javafx.fxml;
    opens com.wolfhouse.mod4j.utils to javafx.base;
    exports com.wolfhouse.modbus_simulator;
    exports com.wolfhouse.mod4j.utils;
}