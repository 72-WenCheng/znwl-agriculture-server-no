package com.frog.agriculture.controller;

import com.frog.common.annotation.Log;
import com.frog.common.core.controller.BaseController;
import com.frog.common.core.domain.AjaxResult;
import com.frog.common.core.page.TableDataInfo;
import com.frog.common.enums.BusinessType;
import com.frog.agriculture.domain.MallProductConfig;
import com.frog.agriculture.service.IMallProductConfigService;
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

    @GetMapping("/list")
    public TableDataInfo list(MallProductConfig config) {
        startPage();
        List<MallProductConfig> list = configService.selectMallProductConfigList(config);
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

