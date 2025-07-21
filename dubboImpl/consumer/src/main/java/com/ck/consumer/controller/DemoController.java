package com.ck.consumer.controller;

import com.ck.consumer.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: ck
 * @CreateTime: 2025-06-12
 * @Description: 控制器
 * @Version: 1.0
 */
@RestController
public class DemoController {

    @Autowired
    private DemoService demoService;

    @RequestMapping("/demo")
    public String demo(){
        System.out.println("消费者开始远程调用。。。。");
        return demoService.demo();
    }

}
