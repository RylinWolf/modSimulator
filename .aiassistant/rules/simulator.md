---
apply: 按模型决策
指令: 若需理解项目
---

# Modbus 模拟器项目总结文档 (AI 辅助理解专用)

## 1. 项目概览

本项目是一个基于 **JavaFX** 开发的 Modbus 协议模拟器，目前主要支持 **Modbus TCP** 设备的模拟。它允许用户创建多个虚拟设备，监听特定端口，并根据配置的规则自动响应
Modbus 请求。

### 核心功能：

- **多设备模拟**：支持同时启动多个 Modbus TCP 从站（Slave）模拟器。
- **自定义响应**：可针对特定的寄存器地址配置固定或随机的响应数据。
- **实时日志**：每个模拟设备都有独立的控制台，实时显示交互日志。
- **持久化支持**：支持将设备配置和响应规则导出为文件，并可重新导入。
- **现代化 UI**：使用 AtlantaFX 主题（Cupertino Dark/Light），支持深色/浅色模式切换。

---

## 2. 技术栈

- **语言**：Java 23
- **框架**：JavaFX 21 (使用 FXML 进行 UI 布局)
- **核心库**：
    - `mod4j`: 底层 Modbus 协议栈实现。
    - `AtlantaFX`: UI 主题库。
    - `Ikonli`: 图标库。
    - `Lombok`: 简化 POJO 代码。
    - `jSerialComm`: 串口通信（为后续 RTU 支持预留）。
- **构建工具**：Maven

---

## 3. 核心模块与架构

### 3.1 核心架构 (MVC/MVVM 风格)

项目采用 JavaFX 标准的 Controller-View 模式，结合模型驱动的设计。

- **`com.wolfhouse.modbus_simulator.model`**:
    - `TcpDeviceModel`: 代表一个 TCP 模拟设备，包含端口、状态、日志和关联的 `MockResponseModel` 列表。
    - `MockResponseModel`: 代表一个虚拟响应规则，定义了从站 ID、寄存器地址、数据大小及数据生成策略（固定/随机）。
- **`com.wolfhouse.modbus_simulator.service`**:
    - `TcpSimulatorService`: 处理模拟器的生命周期（启动/停止）和 UI 对话框（如日志控制台）。
    - `FileService`: 处理配置文件的导入导出。
- **`com.wolfhouse.modbus_simulator` (控制器与入口)**:
    - `MainApplication`: 程序入口，负责初始化 JavaFX 阶段和全局设置。
    - `MainController`: 主窗口控制器，管理视图切换和主题。
    - `TcpSimulatorController`: TCP 模拟界面的核心逻辑，管理表格展示、设备操作等。
    - `DeviceManager`: 单例模式，管理全局运行中的设备实例。

### 3.2 数据持久化

项目通过 `ModelMappingStrategy` 接口实现了一套灵活的映射机制：

- `TcpMappingStrategy` / `MockResponseMappingStrategy`: 将 JavaFX 模型对象（带有 ObservableProperty）与普通的
  `Map<String, Object>` 互相转换，以便使用 Java 原生序列化进行文件存取，避免了直接序列化 JavaFX 属性对象的复杂性。

---

## 4. 关键流程分析

### 4.1 设备启动流程

1. 用户在 `TcpSimulatorController` 中添加或启动设备。
2. 调用 `ModbusTcpSimulator`（来自 `mod4j`）启动监听。
3. 将配置好的 `MockResponseModel` 注入到模拟器实例中。
4. 模拟器监听 TCP 端口，当收到匹配的请求时，根据 `MockRespPair` 返回预定义或随机生成的数据。
5. 模拟器的日志通过回调/监听器实时更新到 `TcpDeviceModel` 的 `logs` 属性中。

### 4.2 配置导入导出

1. `FileService` 使用 `ObjectOutputStream` 将模型映射后的 Map 列表写入文件。
2. 导入时，读取文件并利用策略类（Strategy）将数据恢复为 `TcpDeviceModel` 对象，并添加到 `DeviceManager` 的全局列表中。

---

## 5. 项目结构说明

- `src/main/java`: Java 源代码。
- `src/main/resources`: FXML 布局文件和 CSS 样式表。
- `module-info.java`: Java 模块化配置。
- `pom.xml`: Maven 依赖与构建插件配置。

---

## 6. 开发建议 (针对 AI)

- **UI 修改**：主要的布局文件位于 `resources` 目录下的 `.fxml`。逻辑绑定在对应的 `Controller` 类中。
- **协议扩展**：如需增加对 Modbus RTU 的支持，应参考 `TcpDeviceModel` 的实现模式，并在 `DeviceManager` 中增加对应的管理逻辑。
- **状态同步**：项目大量使用 JavaFX 的 `Property` 绑定，修改模型属性会自动更新 UI，反之亦然。
