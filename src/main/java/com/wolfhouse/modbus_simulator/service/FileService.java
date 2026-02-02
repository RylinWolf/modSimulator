package com.wolfhouse.modbus_simulator.service;

import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * 文件相关服务
 *
 * @author Rylin Wolf
 */
public class FileService {
    public static FileChooser loadFile(String title,
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
}
