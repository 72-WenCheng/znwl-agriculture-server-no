package com.frog.agriculture.service;

import com.frog.agriculture.domain.MallProductConfig;

import java.util.List;

public interface IMallProductConfigService {

    MallProductConfig selectMallProductConfigById(Long id);

    List<MallProductConfig> selectMallProductConfigList(MallProductConfig config);

    int insertMallProductConfig(MallProductConfig config);

    int updateMallProductConfig(MallProductConfig config);

    int deleteMallProductConfigByIds(Long[] ids);

    int deleteMallProductConfigById(Long id);

    /**
     * 按溯源码优先匹配，其次名称+分类+分区，再次名称+分类
     */
    MallProductConfig matchConfig(String name, String category, String partitionId, String traceCode);
}

