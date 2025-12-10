package com.frog.agriculture.controller;

import com.frog.common.core.controller.BaseController;
import com.frog.common.core.domain.AjaxResult;
import com.frog.agriculture.domain.TraceSellpro;
import com.frog.agriculture.domain.MallProductConfig;
import com.frog.agriculture.service.IMallProductConfigService;
import com.frog.agriculture.service.ITraceSellproService;
import com.frog.agriculture.service.ITraceTemplateService;
import com.frog.agriculture.service.ITraceCodeService;
import com.github.pagehelper.PageInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 线上商城 - 商品列表
 */
@RestController
@RequestMapping("/mall/product")
public class MallController extends BaseController {

    @Autowired
    private ITraceSellproService traceSellproService;
    @Autowired
    private ITraceTemplateService traceTemplateService;
    @Autowired
    private ITraceCodeService traceCodeService;
    @Autowired
    private IMallProductConfigService mallProductConfigService;

    /**
     * 商品分页列表（复用溯源产品数据）
     */
    @GetMapping("/list")
    public AjaxResult list(TraceSellpro query,
                           @RequestParam(value = "keyword", required = false) String keyword,
                           @RequestParam(value = "category", required = false) String category,
                           @RequestParam(value = "minPrice", required = false) Double minPrice,
                           @RequestParam(value = "maxPrice", required = false) Double maxPrice,
                           @RequestParam(value = "order", required = false) String order) {
        startPage();
        List<TraceSellpro> list = traceSellproService.selectTraceSellproList(query);
        // 先套用商品配置（价格、封面），提高匹配宽容度
        applyConfig(list);
        // 简单过滤：关键字匹配名称/规格/产地；分类以产地充当；价格区间
        List<TraceSellpro> filtered = list.stream().filter(p -> {
            boolean ok = true;
            if (StringUtils.isNotBlank(keyword)) {
                String k = keyword.toLowerCase();
                ok = (StringUtils.containsIgnoreCase(p.getSellproName(), k)
                        || StringUtils.containsIgnoreCase(p.getSellproGuige(), k)
                        || StringUtils.containsIgnoreCase(p.getSellproArea(), k));
            }
            if (ok && StringUtils.isNotBlank(category)) {
                ok = StringUtils.equals(category, defaultCategory(p));
            }
            if (ok && (minPrice != null || maxPrice != null)) {
                Double price = null;
                try { price = (Double) p.getClass().getMethod("getPrice").invoke(p); } catch (Exception ignored) {}
                if (minPrice != null && (price == null || price < minPrice)) ok = false;
                if (maxPrice != null && (price == null || price > maxPrice)) ok = false;
            }
            return ok;
        }).collect(Collectors.toList());

        // 排序
        if (StringUtils.isNotBlank(order)) {
            switch (order) {
                case "priceAsc":
                    filtered.sort(Comparator.comparing(o -> safePrice(o)));
                    break;
                case "priceDesc":
                    filtered.sort(Comparator.comparing((TraceSellpro o) -> safePrice(o)).reversed());
                    break;
                case "name":
                    filtered.sort(Comparator.comparing(TraceSellpro::getSellproName, Comparator.nullsLast(String::compareTo)));
                    break;
                default:
            }
        }

        // 类目
        List<Map<String, String>> categories = list.stream()
                .map(this::defaultCategory)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .map(c -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("label", c);
                    m.put("value", c);
                    return m;
                }).collect(Collectors.toList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("rows", filtered);
        payload.put("total", new PageInfo<>(list).getTotal());
        payload.put("categories", categories);
        return AjaxResult.success(payload);
    }

    /** 商品详情 */
    @GetMapping("/detail")
    public AjaxResult detail(@RequestParam("id") Long id) {
        TraceSellpro p = traceSellproService.selectTraceSellproBySellproId(id);
        applyConfig(p);
        return AjaxResult.success(p);
    }

    /**
     * 溯源聚合信息（直接给商城前端使用）
     */
    @GetMapping("/traceInfo")
    public AjaxResult traceInfo(@RequestParam(value = "traceCode", required = false) String traceCode,
                                @RequestParam(value = "templateId", required = false) Long templateId) {
        Map<String, Object> data = new HashMap<>();
        if (StringUtils.isNotBlank(traceCode)) {
            data.put("code", traceCodeService.selectTraceCodeByTraceCode(traceCode));
            data.put("template", traceTemplateService.getTraceTemplateByTraceCode(traceCode));
        } else if (templateId != null) {
            data.put("template", traceTemplateService.selectTraceTemplateByTemplateId(templateId));
        }
        return AjaxResult.success(data);
    }

    /** 简单推荐：返回前 N 条 */
    @GetMapping("/recommend")
    public AjaxResult recommend(@RequestParam(value = "limit", required = false, defaultValue = "8") Integer limit) {
        List<TraceSellpro> all = traceSellproService.selectTraceSellproList(new TraceSellpro());
        List<TraceSellpro> rows = all.stream().limit(limit).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        return AjaxResult.success(data);
    }

    private String defaultCategory(TraceSellpro p) {
        return StringUtils.defaultString(p.getSellproArea(), "其它");
    }

    private Double safePrice(TraceSellpro p) {
        try { Object v = p.getClass().getMethod("getPrice").invoke(p); return v==null?0.0:((Number)v).doubleValue(); } catch (Exception e) { return 0.0; }
    }

    /**
     * 套用商品配置：优先名称+分类+分区，其次名称+分类（分区留空）
     */
    private void applyConfig(List<TraceSellpro> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(this::applyConfig);
    }

    private void applyConfig(TraceSellpro p) {
        if (p == null) return;
        MallProductConfig cfg = mallProductConfigService.matchConfig(p.getSellproName(), p.getCategory(), p.getSellproArea(), p.getTraceCode());
        if (cfg != null) {
            if (cfg.getPrice() != null) {
                p.setPrice(cfg.getPrice());
            }
            if (StringUtils.isNotBlank(cfg.getCover())) {
                p.setSellproImg(cfg.getCover());
            }
            // 备注/描述同步
            if (StringUtils.isNotBlank(cfg.getRemark())) {
                p.setRemark(cfg.getRemark());
            }
            // 保证可展示
            p.setStatus("1");
            p.setDelFlag("0");
        }
    }
} 