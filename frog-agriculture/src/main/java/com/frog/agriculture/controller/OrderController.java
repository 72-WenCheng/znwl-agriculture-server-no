package com.frog.agriculture.controller;

import com.frog.common.core.controller.BaseController;
import com.frog.common.core.domain.AjaxResult;
import com.frog.common.core.page.TableDataInfo;
import com.frog.agriculture.domain.TraceSellpro;
import com.frog.agriculture.service.ITraceSellproService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/mall/order")
public class OrderController extends BaseController {

    private static final Map<String, OrderDTO> ORDERS = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ITraceSellproService traceSellproService;

    @PostMapping("/create")
    public AjaxResult create(@RequestBody CreateOrderReq req) {
        OrderDTO order = new OrderDTO();
        order.setId(UUID.randomUUID().toString().replace("-", ""));
        order.setCreateTime(new Date());
        order.setStatus("CREATED");
        order.setItems(Optional.ofNullable(req.getItems()).orElse(Collections.emptyList()));
        double amount = order.getItems().stream().mapToDouble(i -> Optional.ofNullable(i.getPrice()).orElse(0.0) * Optional.ofNullable(i.getQuantity()).orElse(1)).sum();
        order.setAmount(amount);

        boolean dbOk = false;
        try {
            if (jdbcTemplate != null) {
                // 校验与扣减库存（当存在 stock 字段时）
                for (OrderItem i : order.getItems()) {
                    if (i.getSellproId() == null || i.getQuantity() == null) continue;
                    Integer updated = jdbcTemplate.update("UPDATE agriculture_trace_sellpro SET stock = stock - ? WHERE sellpro_id = ? AND (stock IS NULL OR stock >= ?)", i.getQuantity(), i.getSellproId(), i.getQuantity());
                    if (updated == 0) {
                        return AjaxResult.error("库存不足或商品不存在: " + i.getSellproName());
                    }
                }
                jdbcTemplate.update("INSERT INTO mall_order(id, amount, status, create_time) VALUES(?,?,?,NOW())",
                        order.getId(), order.getAmount(), order.getStatus());
                for (OrderItem i : order.getItems()) {
                    jdbcTemplate.update("INSERT INTO mall_order_item(order_id, sellpro_id, sellpro_name, sellpro_guige, sellpro_area, sellpro_img, price, quantity) VALUES(?,?,?,?,?,?,?,?)",
                            order.getId(), i.getSellproId(), i.getSellproName(), i.getSellproGuige(), i.getSellproArea(), i.getSellproImg(), i.getPrice(), i.getQuantity());
                }
                dbOk = true;
            }
        } catch (Exception e) {
            return AjaxResult.error("下单失败: " + e.getMessage());
        }

        if (!dbOk) {
            ORDERS.put(order.getId(), order);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", order.getId());
        return AjaxResult.success(data);
    }

    @GetMapping("/detail")
    public AjaxResult detail(@RequestParam("id") String id) {
        if (jdbcTemplate != null) {
            try {
                Map<String, Object> orderRow = jdbcTemplate.queryForMap("SELECT id, amount, status, create_time FROM mall_order WHERE id=?", id);
                List<Map<String, Object>> items = jdbcTemplate.queryForList("SELECT sellpro_id, sellpro_name, sellpro_guige, sellpro_area, sellpro_img, price, quantity FROM mall_order_item WHERE order_id=?", id);
                return AjaxResult.success(enrichTrace(mapToOrder(orderRow, items)));
            } catch (Exception ignored) {}
        }
        return AjaxResult.success(enrichTrace(ORDERS.get(id)));
    }

    @GetMapping("/list")
    public TableDataInfo list() {
        if (jdbcTemplate != null) {
            startPage();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, amount, status, create_time FROM mall_order ORDER BY create_time DESC");
            List<OrderDTO> list = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                list.add(enrichTrace(mapToOrder(r, Collections.emptyList())));
            }
            return getDataTable(list);
        } else {
            List<OrderDTO> list = new ArrayList<>(ORDERS.values());
            list.sort(Comparator.comparing(OrderDTO::getCreateTime).reversed());
            list.replaceAll(this::enrichTrace);
            return getDataTable(list);
        }
    }

    @PostMapping("/pay")
    public AjaxResult pay(@RequestParam("id") String id) {
        return updateStatus(id, "PAID");
    }

    @PostMapping("/ship")
    public AjaxResult ship(@RequestParam("id") String id) {
        return updateStatus(id, "SHIPPED");
    }

    @PostMapping("/finish")
    public AjaxResult finish(@RequestParam("id") String id) {
        return updateStatus(id, "FINISHED");
    }

    @PostMapping("/cancel")
    public AjaxResult cancel(@RequestParam("id") String id) {
        // 回补库存
        if (jdbcTemplate != null) {
            try {
                List<Map<String, Object>> items = jdbcTemplate.queryForList("SELECT sellpro_id, quantity FROM mall_order_item WHERE order_id=?", id);
                for (Map<String, Object> r : items) {
                    Long pid = r.get("sellpro_id") == null ? null : ((Number) r.get("sellpro_id")).longValue();
                    Integer qty = r.get("quantity") == null ? 0 : ((Number) r.get("quantity")).intValue();
                    if (pid != null && qty > 0) {
                        jdbcTemplate.update("UPDATE agriculture_trace_sellpro SET stock = COALESCE(stock,0) + ? WHERE sellpro_id = ?", qty, pid);
                    }
                }
            } catch (Exception ignored) {}
        }
        return updateStatus(id, "CANCELLED");
    }

    private AjaxResult updateStatus(String id, String status) {
        if (jdbcTemplate != null) {
            int n = jdbcTemplate.update("UPDATE mall_order SET status=?, update_time=NOW() WHERE id=?", status, id);
            if (n > 0) return AjaxResult.success();
            return AjaxResult.error("订单不存在");
        } else {
            OrderDTO o = ORDERS.get(id);
            if (o == null) return AjaxResult.error("订单不存在");
            o.setStatus(status);
            return AjaxResult.success();
        }
    }

    private OrderDTO mapToOrder(Map<String, Object> orderRow, List<Map<String, Object>> itemRows) {
        OrderDTO order = new OrderDTO();
        order.setId((String) orderRow.get("id"));
        order.setAmount(orderRow.get("amount") == null ? 0.0 : ((Number) orderRow.get("amount")).doubleValue());
        order.setStatus((String) orderRow.get("status"));
        order.setCreateTime((Date) orderRow.get("create_time"));
        if (itemRows != null && !itemRows.isEmpty()) {
            List<OrderItem> list = new ArrayList<>();
            for (Map<String, Object> r : itemRows) {
                OrderItem it = new OrderItem();
                it.setSellproId(r.get("sellpro_id") == null ? null : ((Number) r.get("sellpro_id")).longValue());
                it.setSellproName((String) r.get("sellpro_name"));
                it.setSellproGuige((String) r.get("sellpro_guige"));
                it.setSellproArea((String) r.get("sellpro_area"));
                it.setSellproImg((String) r.get("sellpro_img"));
                it.setPrice(r.get("price") == null ? null : ((Number) r.get("price")).doubleValue());
                it.setQuantity(r.get("quantity") == null ? 1 : ((Number) r.get("quantity")).intValue());
                list.add(it);
            }
            order.setItems(list);
        }
        return order;
    }

    @Data
    public static class CreateOrderReq {
        private List<OrderItem> items;
    }

    @Data
    public static class OrderItem {
        private Long sellproId;
        private String sellproName;
        private String sellproGuige;
        private String sellproArea;
        private String sellproImg;
        private Double price;
        private Integer quantity;
        private String traceCode;
        private Long templateId;
    }

    @Data
    public static class OrderDTO {
        private String id;
        private Date createTime;
        private List<OrderItem> items;
        private Double amount;
        private String status;
    }

    private OrderDTO enrichTrace(OrderDTO order) {
        if (order == null || order.getItems() == null) return order;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getSellproId() == null) continue;
            try {
                TraceSellpro p = traceSellproService.selectTraceSellproBySellproId(item.getSellproId());
                if (p != null) {
                    item.setTraceCode(p.getTraceCode());
                    item.setTemplateId(p.getTemplateId());
                    if (item.getSellproImg() == null) item.setSellproImg(p.getSellproImg());
                }
            } catch (Exception ignored) {}
        }
        return order;
    }
} 