# Modbus 模拟器 (Modbus Simulator)

一个基于 **JavaFX** 开发的现代化 Modbus 协议模拟器，旨在为工业通讯开发提供便捷的从站（Slave/Server）仿真环境。支持多设备同时模拟、高度自定义的响应规则以及深色/浅色模式切换。

### 核心特性

- **多设备仿真**：支持同时启动多个 Modbus TCP 设备，每个设备监听独立的端口。
- **灵活的协议支持**：
    - **Modbus TCP**：标准 TCP 报文格式（包含 MBAP 报头）。
    - **RTU over TCP**：透传 Modbus RTU 报文（无 MBAP 报头，含 CRC），适用于特定网关调试。
- **自定义响应规则**：
    - 可针对特定从机 ID、寄存器地址和功能码配置响应。
    - 支持**固定数据**（十六进制输入）或**随机数据**生成。
    - 支持自定义功能码（0-255）。
- **实时交互日志**：每个设备拥有独立的日志控制台，实时查看请求与响应细节。
- **配置持久化**：支持将所有设备配置及响应规则导出为文件，方便重复使用。

---

## 安装说明

### 环境要求

- **Java**: JDK 23 或更高版本。
- **构建工具**: Maven 3.9+。
- **操作系统**: macOS (已验证), Windows, Linux。
- **JavaFX**: 若需打包，需先下载 JavaFX 25 Jmods，见 <a href="https://gluonhq.com/products/javafx/">JavaFX</a> 。

### 获取项目

```bash
git clone https://github.com/your-repo/modbus_simulator.git
cd modbus_simulator
```

### 运行程序

使用 Maven 直接运行：

```bash
mvn clean javafx:run
```

### 打包发布

项目提供了打包脚本（位于 `package/sh` 目录下）：

- **macOS**: 运行 `./package/sh/dmg.sh` 生成 `.dmg` 安装包。
- **Windows**: 运行 `./package/sh/exe.sh` 生成可执行文件。

---

## 详细用法

### 1. 设备管理

- **添加设备**：在主界面点击“添加设备”，设置端口号（默认 5502）、选择协议类型（TCP 或 RTU）并填写备注。
- **控制设备**：在设备列表中点击“启动”或“停止”按钮。启动后，设备将开始监听指定端口。
- **日志查看**：点击“日志”按钮可打开该设备的独立控制台，实时观察数据交互情况。

### 2. 配置虚拟响应

- 点击设备对应的“配置”按钮，管理该设备的响应规则。
- **添加响应**：
    - **从机 ID**：对应 Modbus 报文中的 Unit ID。
    - **地址**：支持十进制或十六进制（以 `0x` 开头）地址。
    - **功能码**：支持自定义。
    - **数据生成策略**：可选择“随机数据”或手动输入“固定数据”（十六进制字节）。

### 3. 配置导入与导出

- **导出**：点击主工具栏的“导出配置”，可将当前所有设备及其响应规则保存为文件。
- **导入**：点击“导入配置”并选择文件，即可恢复之前保存的工作环境。
- **注意：不同版本之间的配置文件可能不兼容！**

### 4. 系统设置

- **数据目录**：应用配置和日志默认保存在用户目录的 AppData 下（如 macOS:
  `~/Library/Application Support/ModbusSimulator`）。

---

## 技术架构

- **UI 框架**: JavaFX 21 + FXML
- **主题库**: AtlantaFX (Cupertino)
- **协议栈**: `mod4j` (自定义 Modbus 核心库，见 <a href="https://github.com/RylinWolf/mod4j">mod4j</a> )
- **依赖管理**: Maven
- **核心工具类**:
    - `DeviceManager`: 负责管理所有运行中的设备实例。
    - `TcpSimulatorService`: 处理模拟逻辑与 UI 交互。
    - `FileService`: 处理配置文件的序列化映射。

---

## 项目结构

```text
├── package/                # 平台打包脚本及配置 (dmg, exe)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/wolfhouse/modbus_simulator/
│   │   │       ├── meta/           # 元数据与静态常量 (AppInfo, AppDirs)
│   │   │       ├── model/          # 数据模型与持久化策略
│   │   │       │   └── persistent/  # 配置文件映射策略 (Strategy 模式)
│   │   │       ├── service/        # 业务逻辑服务 (文件处理、模拟器控制)
│   │   │       ├── util/           # 通用工具类 (日志、系统信息、窗口管理)
│   │   │       ├── MainApplication # 程序入口
│   │   │       └── ...Controller   # 界面控制器 (FXML 绑定)
│   │   └── resources/
│   │       ├── com/.../            # FXML 布局、CSS 样式
│   │       ├── conf/               # 默认配置文件模板
│   │       └── info.properties     # 应用版本等信息
├── pom.xml                 # Maven 项目对象模型
└── README.md               # 项目自述文件
```
