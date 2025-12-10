package com.frog.agriculture.domain;

import com.frog.common.annotation.Excel;
import com.frog.common.core.domain.BaseEntity;
import lombok.Data;

/**
 * 商城商品配置（价格/封面/分类）对象 agriculture_mall_product_config
 */
@Data
public class MallProductConfig extends BaseEntity {
    private Long id;

    /** 名称（商品/批次名） */
    @Excel(name = "名称")
    private String name;

    /** 分类：鱼/农作物 */
    @Excel(name = "分类")
    private String category;

    /** 分区/鱼塘ID，可选，用于精确匹配 */
    private String partitionId;

    /** 价格 */
    @Excel(name = "价格")
    private Double price;

    /** 封面图 */
    private String cover;

    /** 状态（0停用 1启用） */
    private String status;

    /** 备注 */
    private String remark;

    /** 溯源码（优先匹配） */
    private String traceCode;
}

