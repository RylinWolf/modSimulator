package com.wolfhouse.modbus_simulator.model.persistent.strategy;

import com.wolfhouse.mod4j.utils.ModbusTcpSimulator;
import com.wolfhouse.modbus_simulator.model.MockResponseModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 虚拟响应映射策略
 *
 * @author Rylin Wolf
 */
@SuppressWarnings({"unchecked", "unused", "UnusedReturnValue"})
public class MockResponseMappingStrategy implements ModelMappingStrategy<MockResponseModel> {
    public static final String NAME           = "name";
    public static final String REMARK         = "remark";
    public static final String SLAVE_ID       = "slaveId";
    public static final String ENABLED        = "enabled";
    public static final String ADDR           = "addr";
    public static final String DATA_SIZE      = "dataSize";
    public static final String DATA           = "data";
    public static final String RAND_DATA      = "randData";
    public static final String MOCK_RESP_PAIR = "pair";

    private MockResponseMappingStrategy() {
    }

    public static ModelMappingStrategy<MockResponseModel> getInstance() {
        return SingletonHolder.getInstance();
    }

    private static Map<String, Object> getPairMap(ModbusTcpSimulator.MockRespPair pair) {
        HashMap<String, Object> pairMap = HashMap.newHashMap(4);
        pairMap.put(ADDR, pair.registerAddr());
        pairMap.put(DATA_SIZE, pair.dataSize());
        pairMap.put(RAND_DATA, pair.randData());
        pairMap.put(DATA, pair.data());
        return pairMap;
    }

    private static ModbusTcpSimulator.MockRespPair getPair(Map<String, Object> map) {
        return new ModbusTcpSimulator.MockRespPair((int) map.get(ADDR),
                                                   (int) map.get(DATA_SIZE),
                                                   (boolean) map.get(RAND_DATA),
                                                   (byte[]) map.get(DATA));
    }

    @Override
    public Map<String, Object> map(MockResponseModel model) {
        HashMap<String, Object> map = HashMap.newHashMap(6);
        map.put(NAME, model.getName());
        map.put(REMARK, model.getRemark());
        map.put(SLAVE_ID, model.getSlaveId());
        map.put(ENABLED, model.isEnabled());
        map.put(ADDR, model.getRegAddr());
        map.put(MOCK_RESP_PAIR, getPairMap(model.getPair()));
        return map;
    }

    @Override
    public List<Map<String, Object>> mapAll(Collection<MockResponseModel> models) {
        return models.stream().map(this::map).toList();
    }

    @Override
    public MockResponseModel read(Map<String, Object> map) {
        MockResponseModel model = new MockResponseModel((String) map.get(SLAVE_ID),
                                                        (String) map.get(ADDR),
                                                        getPair((Map<String, Object>) map.get(MOCK_RESP_PAIR)));
        model.setName((String) map.get(NAME));
        model.setRemark((String) map.get(REMARK));
        model.setEnabled((boolean) map.get(ENABLED));
        return model;
    }

    @Override
    public List<MockResponseModel> readAll(List<Map<String, Object>> maps) {
        return maps.stream().map(this::read).toList();
    }

    private static class SingletonHolder {
        private static final MockResponseMappingStrategy INSTANCE = new MockResponseMappingStrategy();

        public static MockResponseMappingStrategy getInstance() {
            return INSTANCE;
        }
    }
}
