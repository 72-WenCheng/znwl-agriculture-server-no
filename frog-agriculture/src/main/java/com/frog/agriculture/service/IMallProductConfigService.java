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
     * 按溯源码匹配（精确 -> 前缀），未提供溯源码则返回 null
     */
    MallProductConfig matchConfig(String traceCode);
}

