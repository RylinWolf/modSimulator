cd ../..
echo "[modbus-simulator] 执行 maven 操作..."
mvn clean package
echo "[modbus-simulator] 正在构建 EXE..."
jpackage \
  --type exe \
  --dest dist \
  --name "ModbusSimulator" \
  --app-version "1.3" \
  --vendor "WolfHouse" \
  --input target/libs \
  --main-jar modbus_simulator-1.3.jar \
  --main-class com.wolfhouse.modbus_simulator.MainApplication \
  --module-path "$JMODS_PATH" \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.logging,java.desktop,java.sql \
  --java-options "-Dfile.encoding=UTF-8" \
  --win-dir-chooser \
  --win-shortcut \
  --win-menu

echo "[modbus-simulator] EXE 构建脚本执行完成"
