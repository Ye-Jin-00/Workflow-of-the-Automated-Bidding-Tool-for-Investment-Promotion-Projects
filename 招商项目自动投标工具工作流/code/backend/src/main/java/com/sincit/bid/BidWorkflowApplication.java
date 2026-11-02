package com.sincit.bid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 招商项目自动投标工具工作流服务入口。
 */
@EnableScheduling
@SpringBootApplication
public class BidWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(BidWorkflowApplication.class, args);
    }
}
