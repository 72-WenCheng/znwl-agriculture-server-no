package com.frog.agriculture.service.impl;

import com.frog.agriculture.domain.MallProductConfig;
import com.frog.agriculture.mapper.MallProductConfigMapper;
import com.frog.agriculture.service.IMallProductConfigService;
import com.frog.common.utils.DateUtils;
import com.frog.common.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MallProductConfigServiceImpl implements IMallProductConfigService {

    @Autowired
    private MallProductConfigMapper configMapper;

    @Override
    public MallProductConfig selectMallProductConfigById(Long id) {
        return configMapper.selectMallProductConfigById(id);
    }

    @Override
    public List<MallProductConfig> selectMallProductConfigList(MallProductConfig config) {
        return configMapper.selectMallProductConfigList(config);
    }

    @Override
    public int insertMallProductConfig(MallProductConfig config) {
        config.setCreateBy(SecurityUtils.getUserId().toString());
        config.setCreateTime(DateUtils.getNowDate());
        return configMapper.insertMallProductConfig(config);
    }

    @Override
    public int updateMallProductConfig(MallProductConfig config) {
        config.setUpdateBy(SecurityUtils.getUserId().toString());
        config.setUpdateTime(DateUtils.getNowDate());
        return configMapper.updateMallProductConfig(config);
    }

    @Override
    public int deleteMallProductConfigByIds(Long[] ids) {
        return configMapper.deleteMallProductConfigByIds(ids);
    }

    @Override
    public int deleteMallProductConfigById(Long id) {
        return configMapper.deleteMallProductConfigById(id);
    }

    @Override
    public MallProductConfig matchConfig(String name, String category, String partitionId, String traceCode) {
        MallProductConfig query = new MallProductConfig();
        // 1) 溯源码优先（支持前缀匹配：配置的 trace_code 是商品 trace_code 的前缀也算匹配）
        if (StringUtils.isNotBlank(traceCode)) {
            // 先精确匹配
            query.setTraceCode(traceCode);
            List<MallProductConfig> list = configMapper.selectMallProductConfigList(query);
            if (!list.isEmpty()) {
                return list.get(0);
            }
            // 再前缀匹配：配置的 trace_code LIKE 商品 trace_code%
            List<MallProductConfig> prefixList = configMapper.selectByTraceCodePrefix(traceCode);
            if (!prefixList.isEmpty()) {
                return prefixList.get(0);
            }
        }

        // 2) 名称+分类+分区
        query = new MallProductConfig();
        query.setCategory(category);
        query.setName(name);
        if (StringUtils.isNotBlank(partitionId)) {
            query.setPartitionId(partitionId);
            List<MallProductConfig> list = configMapper.selectMallProductConfigList(query);
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }

        // 3) 名称+分类（分区留空）
        query.setPartitionId(null);
        List<MallProductConfig> list = configMapper.selectMallProductConfigList(query);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}

