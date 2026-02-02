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
    requires mod4j;

    opens com.wolfhouse.modbus_simulator to javafx.fxml;
    exports com.wolfhouse.modbus_simulator;
}