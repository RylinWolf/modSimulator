cd ../..
jpackage \
  --type dmg \
  --dest dist \
  --name "ModbusSimulator" \
  --mac-package-name "Modbus模拟器" \
  --app-version "1.1" \
  --vendor "WolfHouse" \
  --input target/libs \
  --main-jar modbus_simulator-1.1.jar \
  --main-class com.wolfhouse.modbus_simulator.MainApplication \
  --module-path "/Volumes/Work/Enviroment/javafx/javafx-jmods-25.0.2" \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.logging,java.desktop,java.sql \
  --java-options "-Dfile.encoding=UTF-8" \
  --resource-dir package/macosx