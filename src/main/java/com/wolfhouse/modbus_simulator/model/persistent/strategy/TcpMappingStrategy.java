package com.wolfhouse.modbus_simulator.model.persistent.strategy;

import com.wolfhouse.modbus_simulator.model.TcpDeviceModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * TCP 设备模型映射策略
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"unchecked", "unused", "UnusedReturnValue"})
public class TcpMappingStrategy implements ModelMappingStrategy<TcpDeviceModel> {
    public static final String NAME            = "name";
    public static final String PORT            = "port";
    public static final String REMARK          = "remark";
    public static final String LOGS            = "logs";
    public static final String IS_TCP_STRATEGY = "isTcpStrategy";
    public static final String MOCK_RESPONSES  = "mockResponses";

    private TcpMappingStrategy() {}

    public static TcpMappingStrategy getInstance() {
        return SingletonHolder.getInstance();
    }

    @Override
    public Map<String, Object> map(TcpDeviceModel model) {
        return Map.ofEntries(Map.entry(NAME, model.getName()),
                             Map.entry(PORT, model.getPort()),
                             Map.entry(REMARK, model.getRemark()),
                             Map.entry(LOGS, model.getLogs()),
                             Map.entry(IS_TCP_STRATEGY, model.isTcpStrategy()),
                             Map.entry(MOCK_RESPONSES,
                                       MockResponseMappingStrategy
                                               .getInstance()
                                               .mapAll(model.getMockResponses())));
    }

    @Override
    public List<Map<String, Object>> mapAll(Collection<TcpDeviceModel> models) {
        return models.stream().map(this::map).toList();
    }

    @Override
    public TcpDeviceModel read(Map<String, Object> map) {
        TcpDeviceModel model = new TcpDeviceModel((int) map.get(PORT));
        model.setName((String) map.get(NAME));
        model.setRemark((String) map.get(REMARK));
        model.setLogs((String) map.get(LOGS));
        if (map.containsKey(IS_TCP_STRATEGY)) {
            model.setTcpStrategy((boolean) map.get(IS_TCP_STRATEGY));
        }
        model.getMockResponses()
             .addAll(MockResponseMappingStrategy
                             .getInstance()
                             .readAll((List<Map<String, Object>>) map.get(MOCK_RESPONSES)));
        return model;
    }

    @Override
    public List<TcpDeviceModel> readAll(List<Map<String, Object>> maps) {
        return maps.stream().map(this::read).toList();
    }

    private static class SingletonHolder {
        private static final TcpMappingStrategy INSTANCE = new TcpMappingStrategy();

        public static TcpMappingStrategy getInstance() {
            return INSTANCE;
        }
    }
}
