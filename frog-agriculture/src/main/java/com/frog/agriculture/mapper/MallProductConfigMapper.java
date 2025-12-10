package com.frog.agriculture.mapper;

import com.frog.agriculture.domain.MallProductConfig;
import java.util.List;

public interface MallProductConfigMapper {

    MallProductConfig selectMallProductConfigById(Long id);

    List<MallProductConfig> selectMallProductConfigList(MallProductConfig config);

    int insertMallProductConfig(MallProductConfig config);

    int updateMallProductConfig(MallProductConfig config);

    int deleteMallProductConfigById(Long id);

    int deleteMallProductConfigByIds(Long[] ids);

    /**
     * 按 trace_code 前缀匹配（配置的 trace_code 是商品 trace_code 的前缀）
     */
    List<MallProductConfig> selectByTraceCodePrefix(String traceCode);
}

