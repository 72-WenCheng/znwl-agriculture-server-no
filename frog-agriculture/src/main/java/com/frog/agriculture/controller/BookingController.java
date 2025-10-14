package com.frog.agriculture.controller;

import com.frog.common.core.controller.BaseController;
import com.frog.common.core.domain.AjaxResult;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 参观预约
 */
@RestController
@RequestMapping("/booking")
public class BookingController extends BaseController {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/create")
    public AjaxResult create(@RequestBody BookingDTO dto) {
        if (dto.getDate() == null || dto.getStartTime() == null || dto.getEndTime() == null) {
            return AjaxResult.error("请选择日期与时段");
        }
        if (dto.getPeople() == null || dto.getPeople() <= 0) {
            return AjaxResult.error("人数必须大于0");
        }
        if (jdbcTemplate != null) {
            // 冲突校验：同一日期且时间段相交
            String clashSql = "SELECT COUNT(1) FROM booking WHERE date=? AND NOT (end_time<=? OR start_time>=?)";
            Integer cnt = jdbcTemplate.queryForObject(clashSql, Integer.class, new java.sql.Date(dto.getDate().getTime()), new java.sql.Time(dto.getStartTime().getTime()), new java.sql.Time(dto.getEndTime().getTime()));
            if (cnt != null && cnt > 0) {
                return AjaxResult.error("该时段已有预约，请选择其它时间");
            }
            jdbcTemplate.update("INSERT INTO booking(date,start_time,end_time,people,contact,phone,remark,create_time) VALUES(?,?,?,?,?,?,?,NOW())",
                    new java.sql.Date(dto.getDate().getTime()), new java.sql.Time(dto.getStartTime().getTime()), new java.sql.Time(dto.getEndTime().getTime()),
                    dto.getPeople(), dto.getContact(), dto.getPhone(), dto.getRemark());
        }
        return AjaxResult.success("预约成功");
    }

    @GetMapping("/list")
    public AjaxResult list(@RequestParam("date") java.sql.Date date) {
        if (jdbcTemplate == null) return AjaxResult.success();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id,date,start_time,end_time,people,contact,phone,remark,create_time FROM booking WHERE date=? ORDER BY start_time", date);
        return AjaxResult.success(rows);
    }

    @GetMapping("/month")
    public AjaxResult month(@RequestParam("year") int year, @RequestParam("month") int month) {
        if (jdbcTemplate == null) return AjaxResult.success();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.YEAR, year);
        cal.set(java.util.Calendar.MONTH, month - 1);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        java.sql.Date start = new java.sql.Date(cal.getTimeInMillis());
        cal.add(java.util.Calendar.MONTH, 1);
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
        java.sql.Date end = new java.sql.Date(cal.getTimeInMillis());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT date, COUNT(*) cnt FROM booking WHERE date BETWEEN ? AND ? GROUP BY date", start, end);
        return AjaxResult.success(rows);
    }

    @GetMapping("/capacity")
    public AjaxResult capacity() {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("dailyCapacity", 20);
        return AjaxResult.success(resp);
    }

    @Data
    public static class BookingDTO {
        private Date date;
        private Date startTime;
        private Date endTime;
        private Integer people;
        private String contact;
        private String phone;
        private String remark;
    }
} 