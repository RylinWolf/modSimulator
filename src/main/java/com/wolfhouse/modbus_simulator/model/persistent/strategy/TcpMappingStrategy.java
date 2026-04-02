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
    public static final String NAME                = "name";
    public static final String PORT                = "port";
    public static final String REMARK              = "remark";
    public static final String LOGS                = "logs";
    public static final String IS_TCP_STRATEGY     = "isTcpStrategy";
    public static final String MOCK_RESPONSES      = "mockResponses";
    public static final String GLOBAL_TIMEOUT      = "globalTimeoutMs";
    public static final String HOST_TIMEOUTS       = "hostTimeoutMs";
    public static final String GLOBAL_SUCCESS_RATE = "globalSuccessRate";
    public static final String HOST_SUCCESS_RATES  = "hostSuccessRates";

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
                             Map.entry(GLOBAL_TIMEOUT, model.getGlobalTimeoutMs()),
                             Map.entry(HOST_TIMEOUTS, model.getHostTimeoutMs()),
                             Map.entry(GLOBAL_SUCCESS_RATE, model.getGlobalSuccessRate()),
                             Map.entry(HOST_SUCCESS_RATES, model.getHostSuccessRates()),
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
        if (map.containsKey(GLOBAL_TIMEOUT)) {
            Object timeoutObj = map.get(GLOBAL_TIMEOUT);
            if (timeoutObj instanceof Number timeout) {
                model.setGlobalTimeoutMs(timeout.intValue());
            }
        }
        if (map.containsKey(HOST_TIMEOUTS) && map.get(HOST_TIMEOUTS) instanceof Map<?, ?> hostMap) {
            java.util.Map<String, Integer> converted = new java.util.LinkedHashMap<>();
            hostMap.forEach((k, v) -> {
                if (k instanceof String key && v instanceof Number timeout) {
                    converted.put(key, timeout.intValue());
                }
            });
            model.setHostTimeoutMs(converted);
        }
        if (map.containsKey(GLOBAL_SUCCESS_RATE)) {
            Object successRateObj = map.get(GLOBAL_SUCCESS_RATE);
            if (successRateObj instanceof Number successRate) {
                model.setGlobalSuccessRate(successRate.doubleValue());
            }
        }
        if (map.containsKey(HOST_SUCCESS_RATES) && map.get(HOST_SUCCESS_RATES) instanceof Map<?, ?> hostMap) {
            java.util.Map<String, Double> converted = new java.util.LinkedHashMap<>();
            hostMap.forEach((k, v) -> {
                if (k instanceof String key && v instanceof Number successRate) {
                    converted.put(key, successRate.doubleValue());
                }
            });
            model.setHostSuccessRates(converted);
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
