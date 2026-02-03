package com.wolfhouse.modbus_simulator.service;

import com.wolfhouse.modbus_simulator.WindowUtil;
import com.wolfhouse.modbus_simulator.model.MockResponseModel;
import com.wolfhouse.modbus_simulator.model.SimulatorModel;
import com.wolfhouse.modbus_simulator.model.TcpDeviceModel;
import com.wolfhouse.modbus_simulator.model.persistent.strategy.MockResponseMappingStrategy;
import com.wolfhouse.modbus_simulator.model.persistent.strategy.TcpMappingStrategy;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件相关服务
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"unchecked", "unused", "UnusedReturnValue"})
public class FileService {
    private FileService() {
    }

    /**
     * 加载一个文件选择器。
     *
     * @param title        文件选择器的标题
     * @param extensionTag 拓展名过滤器的标签名称
     * @param extension    允许的文件拓展名列表
     * @param initialDir   文件选择器的初始目录路径。如果为 null，则默认使用用户当前工作目录
     * @return 配置完成的 {@link FileChooser} 对象
     */
    public static FileChooser loadFileChooser(String title,
                                              String extensionTag,
                                              List<String> extension,
                                              String initialDir) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        FileChooser.ExtensionFilter filters = new FileChooser.ExtensionFilter(extensionTag, extension);
        chooser.getExtensionFilters().add(filters);
        chooser.setSelectedExtensionFilter(filters);
        chooser.setInitialDirectory(new File(initialDir == null ? System.getProperty("user.dir") : initialDir));
        return chooser;
    }

    /**
     * 加载一个文件选择器。
     *
     * @param title           文件选择器的标题
     * @param initialFileName 初始文件名
     * @param extensionTag    拓展名过滤器的标签名称
     * @param extension       允许的文件拓展名列表
     * @return 配置完成的 {@link FileChooser} 对象
     */
    public static FileChooser saveFileChooser(String title, String initialFileName, String extensionTag, List<String> extension) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(initialFileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(extensionTag, extension));
        chooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        return chooser;
    }

    /**
     * 导出配置文件
     *
     * @param file      配置文件
     * @param objs      配置对象列表
     * @param baseStage 基础窗口
     * @param <T>       配置对象类型
     * @return 是否导出成功
     */
    public static <T extends SimulatorModel> boolean exportConf(File file, List<T> objs, Stage baseStage) {
        if (objs == null || objs.isEmpty()) {
            return true;
        }
        try {
            if (!file.exists() && !file.createNewFile()) {
                throw new IOException("无法创建新文件");
            }
        } catch (IOException e) {
            WindowUtil.showError("文件创建失败", e, baseStage);
            return false;
        }
        if (!file.canWrite() && !file.setWritable(true)) {
            WindowUtil.showError("文件无写入权限", null, baseStage);
            return false;
        }

        // 获取类型，调用对应的适配器
        Class<T> aClass = (Class<T>) objs.getFirst().getClass();
        // 写入对象
        if (!writeDevice(objs, file, baseStage, aClass)) {
            return false;
        }
        WindowUtil.showAlert(Alert.AlertType.INFORMATION, "导出成功", "文件导出成功",
                             null, baseStage, ButtonType.OK);
        return true;
    }

    /**
     * 导入配置文件列表，并将其反序列化为对象数据。支持显示导入失败的详细信息。
     *
     * @param files     要导入的文件列表，每个文件需为可读且存在的有效文件。如果列表为空或为 null，将直接返回 true。
     * @param baseStage 基础窗口对象，用于展示导入结果的提示框。如果部分文件导入失败，提示框会显示具体失败原因。
     * @return 如果所有文件均成功导入，则返回 true；如果任意文件导入失败，则返回 false。
     */
    public static boolean importConf(List<File> files, Stage baseStage) {
        if (files == null || files.isEmpty()) {
            return true;
        }
        // 失败文件列表
        List<FailedFile> failedFiles = new ArrayList<>();
        // 读取文件并序列化
        files.forEach(f -> {
            if (!f.exists()) {
                failedFiles.add(new FailedFile(f.getAbsolutePath(), "文件不存在"));
                return;
            }
            if (!f.canRead()) {
                failedFiles.add(new FailedFile(f.getAbsolutePath(), "文件不可读"));
                return;
            }
            try (FileInputStream fis = new FileInputStream(f);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                List<Map<String, Object>> maps = (List<Map<String, Object>>) ois.readObject();
                // 目前只支持 TcpDeviceModel
                importTcpModel(maps);
            } catch (IOException | ClassNotFoundException e) {
                failedFiles.add(new FailedFile(f.getAbsolutePath(), e.toString()));
            }
        });

        if (!failedFiles.isEmpty()) {
            StringBuilder sb = new StringBuilder("以下文件导入失败：\n");
            failedFiles.forEach(ff -> sb.append(ff.filepath()).append(": ").append(ff.cause()).append("\n"));
            WindowUtil.showError("导入部分失败", new Exception(sb.toString()), baseStage);
            return false;
        }

        WindowUtil.showAlert(Alert.AlertType.INFORMATION, "导入成功", "所有配置文件导入成功",
                             null, baseStage, ButtonType.OK);
        return true;
    }

    private static void importTcpModel(List<Map<String, Object>> maps) {
        List<TcpDeviceModel> devices = TcpMappingStrategy.getInstance().readAll(maps);
        com.wolfhouse.modbus_simulator.DeviceManager.getInstance().getTcpDevices().addAll(devices);
    }

    private static <T extends SimulatorModel> boolean writeDevice(List<T> objs, File file, Stage baseStage, Class<T> clazz) {
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            List<Map<String, Object>> maps;
            switch (clazz.getSimpleName()) {
                // TCP 设备模型
                case "TcpDeviceModel" -> maps = TcpMappingStrategy.getInstance().mapAll((List<TcpDeviceModel>) objs);
                case "MockResponseModel" -> maps = MockResponseMappingStrategy.getInstance().mapAll((List<MockResponseModel>) objs);
                default -> {
                    WindowUtil.showError("不支持保存的设备类型", new IllegalArgumentException("不支持的设备类型: " + clazz.getName()), baseStage);
                    return false;
                }
            }
            oos.writeObject(maps);

        } catch (FileNotFoundException e) {
            WindowUtil.showError("文件不存在", e, baseStage);
            return false;
        } catch (IOException e) {
            WindowUtil.showError("文件写入失败", e, baseStage);
            return false;
        }

        return true;
    }

    public record FailedFile(String filepath, String cause) {}
}
