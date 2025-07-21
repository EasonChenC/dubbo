package com.ck.consumer.service.impl;

import com.ck.api.service.DemoDubboService;
import com.ck.consumer.service.DemoService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * @Author: ck
 * @CreateTime: 2025-06-12
 * @Description: 实现类
 * @Version: 1.0
 */
@Service
public class DemoServiceImpl implements DemoService {

    @DubboReference(loadbalance = "roundrobin")
    private DemoDubboService demoDubboService;

    @Override
    public String demo() {
        return demoDubboService.demo("张三丰");
    }
}
