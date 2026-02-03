package com.wolfhouse.modbus_simulator.model.persistent.strategy;

import com.wolfhouse.modbus_simulator.model.SimulatorModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 模型映射策略
 *
 * @author Rylin Wolf
 */
public interface ModelMappingStrategy<T extends SimulatorModel> {
    /**
     * 将设备模型映射为键值对
     *
     * @param model 设备模型
     * @return 映射后的键值对
     */
    Map<String, Object> map(T model);

    /**
     * 将设备模型集合映射为键值对列表
     *
     * @param models 设备模型集合
     * @return 映射后的键值对列表
     */
    List<Map<String, Object>> mapAll(Collection<T> models);

    /**
     * 将键值对映射为设备模型
     *
     * @param map 键值对
     * @return 映射后的设备模型
     */
    T read(Map<String, Object> map);

    /**
     * 将键值对列表映射为设备模型列表
     *
     * @param maps 键值对列表
     * @return 映射后的设备模型列表
     */
    List<T> readAll(List<Map<String, Object>> maps);
}
