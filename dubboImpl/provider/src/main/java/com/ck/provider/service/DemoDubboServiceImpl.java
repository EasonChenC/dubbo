package com.ck.provider.service;

import com.ck.api.service.DemoDubboService;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * @Author: ck
 * @CreateTime: 2025-06-12
 * @Description: 提供api实现类
 * @Version: 1.0
 */
@DubboService
public class DemoDubboServiceImpl implements DemoDubboService {


    @Override
    public String demo(String param) {
        System.out.println("provider的demo方法被调用。。。");
        return param + "123";
    }
}
