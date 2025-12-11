package com.frog.agriculture.controller;

import com.frog.common.annotation.Log;
import com.frog.common.core.controller.BaseController;
import com.frog.common.core.domain.AjaxResult;
import com.frog.common.core.page.TableDataInfo;
import com.frog.common.enums.BusinessType;
import org.apache.commons.lang3.StringUtils;
import com.frog.agriculture.domain.MallProductConfig;
import com.frog.agriculture.service.IMallProductConfigService;
import com.frog.agriculture.service.ITraceSellproService;
import com.frog.agriculture.service.impl.TraceSellproServiceImpl;
import com.frog.agriculture.domain.TraceSellpro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商城商品配置（价格/封面）Controller
 */
@RestController
@RequestMapping("/mall/config")
public class MallProductConfigController extends BaseController {

    @Autowired
    private IMallProductConfigService configService;
    @Autowired
    private ITraceSellproService traceSellproService;
    @Autowired
    private TraceSellproServiceImpl traceSellproServiceImpl;

    @GetMapping("/list")
    public TableDataInfo list(MallProductConfig config) {
        startPage();
        List<MallProductConfig> list = configService.selectMallProductConfigList(config);
        // 补齐名称/封面用于列表展示，避免必须点“确定”保存后才显示
        list.forEach(item -> {
            if (item == null) return;
            if ((StringUtils.isBlank(item.getName()) || StringUtils.isBlank(item.getCover()))
                    && StringUtils.isNotBlank(item.getTraceCode())) {
                TraceSellpro trace = traceSellproService.selectByTraceCode(item.getTraceCode());
                if (trace != null) {
                    if (StringUtils.isBlank(item.getName()) && StringUtils.isNotBlank(trace.getSellproName())) {
                        item.setName(trace.getSellproName());
                    }
                    if (StringUtils.isBlank(item.getCover())) {
                        String cover = trace.getSellproImg();
                        if (StringUtils.isBlank(cover) || cover.contains("/profile/default.jpg")) {
                            cover = traceSellproServiceImpl.findFishImgByTraceCode(item.getTraceCode());
                        }
                        if (StringUtils.isNotBlank(cover)) {
                            item.setCover(cover);
                        }
                    }
                }
            }
        });
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(configService.selectMallProductConfigById(id));
    }

    @Log(title = "商品配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallProductConfig config) {
        return toAjax(configService.insertMallProductConfig(config));
    }

    @Log(title = "商品配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallProductConfig config) {
        return toAjax(configService.updateMallProductConfig(config));
    }

    @Log(title = "商品配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(configService.deleteMallProductConfigByIds(ids));
    }
}



