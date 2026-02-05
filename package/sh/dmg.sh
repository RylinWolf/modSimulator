cd ../..
echo "[modbus-simulator] 执行 maven 操作..."
mvn clean package
echo "[modbus-simulator] 正在构建..."
jpackage \
  --type dmg \
  --dest dist \
  --name "ModbusSimulator" \
  --mac-package-name "Modbus模拟器" \
  --app-version "1.2.2" \
  --vendor "WolfHouse" \
  --input target/libs \
  --main-jar modbus_simulator-1.2.2.jar \
  --main-class com.wolfhouse.modbus_simulator.MainApplication \
  --module-path "$JMODS_PATH" \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.logging,java.desktop,java.sql \
  --java-options "-Dfile.encoding=UTF-8" \
  --resource-dir package/macosx

echo "[modbus-simulator] 构建完成"