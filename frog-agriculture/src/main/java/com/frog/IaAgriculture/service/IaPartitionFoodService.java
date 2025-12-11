package com.frog.IaAgriculture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frog.agriculture.domain.CropBatch;
import com.frog.agriculture.mapper.CropBatchMapper;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.transaction.model.dto.TransactionResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.frog.IaAgriculture.config.BarcodeConfig;
import com.frog.IaAgriculture.dto.ErrorCodeEnum;
import com.frog.IaAgriculture.dto.IaPartitionFoodPageDTO;
import com.frog.IaAgriculture.exception.ServerException;
import com.frog.IaAgriculture.mapper.IaPartitionFoodMapper;
import com.frog.IaAgriculture.mapper.IaPartitionMapper;
import com.frog.IaAgriculture.model.IaPartitionFood;
import com.frog.agriculture.domain.TraceSellpro;
import com.frog.agriculture.service.ITraceSellproService;
import com.frog.agriculture.service.IMallProductConfigService;
import com.frog.agriculture.domain.MallProductConfig;
import com.frog.agriculture.mapper.GermplasmMapper;
import com.frog.agriculture.domain.Germplasm;
import vip.blockchain.agriculture.model.bo.PartitionsAddFoodInputBO;
import vip.blockchain.agriculture.model.bo.PartitionsModifyFoodInputBO;
import vip.blockchain.agriculture.model.bo.PartitionsRemoverFoodInputBO;
import com.frog.IaAgriculture.model.entity.IaPartition;
import vip.blockchain.agriculture.service.PartitionsService;
import vip.blockchain.agriculture.utils.*;
import com.frog.IaAgriculture.vo.CommonContant;
import com.frog.IaAgriculture.vo.ResultVO;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

@Service
public class IaPartitionFoodService extends ServiceImpl<IaPartitionFoodMapper, IaPartitionFood> {

    @Autowired
    private Client client;
    @Autowired
    private IaPartitionMapper iaPartitionMapper;
    @Autowired
    private BarcodeConfig barcodeConfig;
    @Autowired
    private CropBatchMapper cropBatchMapper;
    @Autowired
    private ITraceSellproService traceSellproService;
    @Autowired
    private IMallProductConfigService mallProductConfigService;
    @Autowired
    private GermplasmMapper germplasmMapper;


    @Transactional(rollbackFor = Exception.class)
    public ResultVO create(IaPartitionFood iaPartitionFood) {

        CropBatch cropBatch= cropBatchMapper.selectCropBatchByBatchId(Long.valueOf(iaPartitionFood.getIaPartitionId()));
        IaPartition iaPartition = this.iaPartitionMapper.selectById(iaPartitionFood.getIaPartitionId());
        if (Objects.isNull(cropBatch)) {
            return ResultVO.failed("分区不存在");
        }
        if (cropBatch.getStatus().equals( "0")) {
            return ResultVO.failed("未成熟");
        }
        IaPartitionFood insertBean = new IaPartitionFood();
        insertBean.setIaPartitionId(cropBatch.getLandId().toString());
        BeanUtils.copyProperties(iaPartitionFood, insertBean);
        insertBean.setId(BaseUtil.getSnowflakeId());
        super.save(insertBean);
        // 同步商城商品
        syncTraceSellpro(insertBean, iaPartition, cropBatch);
        PartitionsService partitionsService = new PartitionsService(client, client.getCryptoSuite().getCryptoKeyPair(), cropBatch.getContractAddress());
        PartitionsAddFoodInputBO input = new PartitionsAddFoodInputBO();
        input.set_foodName(insertBean.getName());
        input.set_id(insertBean.getId());
        input.set_notes(insertBean.getDescription());
        input.set_weight(insertBean.getWeight().toEngineeringString());
        input.set_quality(BigInteger.valueOf(insertBean.getStatus()));

        try {
            TransactionResponse transactionResponse = partitionsService.addFood(input);
            if (Objects.equals(transactionResponse.getReceiptMessages(), CommonContant.SUCCESS_MESSAGE)) {

            } else {
                throw new ServerException(transactionResponse.getReceiptMessages());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerException(ErrorCodeEnum.CONTENT_SERVER_ERROR);
        }
        return ResultVO.succeed(insertBean);
    }

    //删除
    @Transactional(rollbackFor = Exception.class)
    public ResultVO delete(String id) {
        IaPartitionFood partitionFood = super.getById(id);
        if (Objects.isNull(partitionFood)) {
            return ResultVO.failed();
        }
        IaPartition iaPartition = this.iaPartitionMapper.selectById(partitionFood.getIaPartitionId());
        super.removeById(id);
        try {

            PartitionsService partitionsService = new PartitionsService(client, client.getCryptoSuite().getCryptoKeyPair(), iaPartition.getContractAddr());

            PartitionsRemoverFoodInputBO input = new PartitionsRemoverFoodInputBO();
            input.set_id(partitionFood.getId());

            TransactionResponse transactionResponse = partitionsService.removerFood(input);
            if (Objects.equals(transactionResponse.getReceiptMessages(), CommonContant.SUCCESS_MESSAGE)) {

            } else {
                throw new ServerException(transactionResponse.getReceiptMessages());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerException(ErrorCodeEnum.CONTENT_SERVER_ERROR);
        }
        return ResultVO.succeed();
    }


    @Transactional(rollbackFor = Exception.class)
    public ResultVO update(IaPartitionFood iaPartitionFood) {
        iaPartitionFood.setIaPartitionId(null);
        String id = iaPartitionFood.getId();
        IaPartitionFood feeding = super.getById(id);
        if (Objects.isNull(feeding)) {
            return ResultVO.failed();
        }
        String iaPartitionId = feeding.getIaPartitionId();
        IaPartition iaPartition = this.iaPartitionMapper.selectById(iaPartitionId);
        super.updateById(iaPartitionFood);

        try {
            PartitionsService partitionsService = new PartitionsService(client, client.getCryptoSuite().getCryptoKeyPair(), iaPartition.getContractAddr());
            PartitionsModifyFoodInputBO input = new PartitionsModifyFoodInputBO();
            input.set_foodName(iaPartitionFood.getName());
            input.set_id(iaPartitionFood.getId());
            input.set_weight(iaPartitionFood.getWeight().toEngineeringString());
            input.set_notes(iaPartitionFood.getDescription());
            input.set_quality(BigInteger.valueOf(iaPartitionFood.getStatus()));

            TransactionResponse transactionResponse = partitionsService.modifyFood(input);
            if (Objects.equals(transactionResponse.getReceiptMessages(), CommonContant.SUCCESS_MESSAGE)) {

            } else {
                throw new ServerException(transactionResponse.getReceiptMessages());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServerException(ErrorCodeEnum.CONTENT_SERVER_ERROR);
        }

        return ResultVO.succeed();
    }

    public ResultVO detail(String id) {
        IaPartitionFood iaFeeding = this.baseMapper.selectById(id);

        return ResultVO.succeed(iaFeeding);
    }

    public ResultVO page(IaPartitionFoodPageDTO baseDTO) {
        QueryWrapper wrapper = new QueryWrapper<Map<String, Object>>();

        if (StringUtils.isNoneBlank(baseDTO.getKeyword())) {
            wrapper.like("name", baseDTO.getKeyword());
        }
        wrapper.eq("ia_partition_id", baseDTO.getPartitionId());

        Page<IaPartitionFood> page = new Page<>(baseDTO.getCurrentPage(), baseDTO.getPageSize());
        Page<IaPartitionFood> p = this.baseMapper.selectPage(page, wrapper);

        //生成前端访问页面的条形二维码
        p.getRecords().forEach(bean -> {
            try {
                bean.setBarcode(BarcodeUtil.getQRCode(barcodeConfig.getPath() + bean.getId()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return ResultVO.succeed(p);
    }

    /**
     * 将采摘记录同步为商城溯源商品
     */
    private void syncTraceSellpro(IaPartitionFood food, IaPartition iaPartition, CropBatch cropBatch) {
        try {
            TraceSellpro exist = traceSellproService.selectByTraceCode(food.getId());
            TraceSellpro bean = exist == null ? new TraceSellpro() : exist;
            bean.setSellproName(food.getName());
            bean.setSellproArea(iaPartition == null ? null : iaPartition.getPartitionName());
            bean.setSellproWeight(food.getWeight() == null ? null : food.getWeight().toEngineeringString());
            bean.setSellproGuige(food.getWeight() == null ? null : food.getWeight().toEngineeringString() + "kg");
            bean.setStock(food.getWeight() == null ? null : food.getWeight().intValue());
            bean.setCategory("农作物");
            bean.setTraceCode(food.getId());
            bean.setStatus("1");
            // 匹配配置补充价格/封面（优先按溯源码匹配）
            MallProductConfig cfg = mallProductConfigService.matchConfig(food.getId());
            if (cfg != null) {
                if (cfg.getPrice() != null) bean.setPrice(cfg.getPrice());
                if (cfg.getCover() != null) bean.setSellproImg(cfg.getCover());
            }
            // 如果封面为空或默认图，尝试用种质图片兜底
            if (bean.getSellproImg() == null || bean.getSellproImg().contains("/profile/default.jpg") || bean.getSellproImg().isEmpty()) {
                if (cropBatch != null && cropBatch.getGermplasmId() != null) {
                    Germplasm germplasm = germplasmMapper.selectGermplasmByGermplasmId(cropBatch.getGermplasmId());
                    if (germplasm != null && org.apache.commons.lang3.StringUtils.isNotBlank(germplasm.getGermplasmImg())) {
                        bean.setSellproImg(germplasm.getGermplasmImg());
                    }
                }
            }
            if (exist == null) {
                traceSellproService.insertTraceSellpro(bean);
            } else {
                traceSellproService.updateTraceSellpro(bean);
            }
        } catch (Exception e) {
            // 同步异常不影响主流程
        }
    }

}
