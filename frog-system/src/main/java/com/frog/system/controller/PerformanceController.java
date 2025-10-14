package com.frog.system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final SystemInfo systemInfo = new SystemInfo();
    private final CentralProcessor processor = systemInfo.getHardware().getProcessor();
    private long[] prevTicks = processor.getSystemCpuLoadTicks();

    @GetMapping
    public Map<String, Object> getPerformance() {
        Map<String, Object> map = new HashMap<>();
        GlobalMemory memory = systemInfo.getHardware().getMemory();

        // CPU 使用率
        long[] ticks = processor.getSystemCpuLoadTicks();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = ticks;

        // 内存使用率
        long totalMem = memory.getTotal();
        long usedMem = totalMem - memory.getAvailable();
        double memUsage = (usedMem * 1.0 / totalMem) * 100;

        // 可选：线程数
        int threadCount = processor.getLogicalProcessorCount();

        map.put("cpu", String.format("%.2f", cpuLoad));
        map.put("mem", String.format("%.2f", memUsage));
        map.put("threads", threadCount);
        return map;
    }
}