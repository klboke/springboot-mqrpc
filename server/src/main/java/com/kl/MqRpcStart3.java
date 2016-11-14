package com.kl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

/**
 * Created by kl on 2016/11/14.
 * Content :启动mq rpc服务端
 */
@SpringBootApplication
@ImportResource("applicationContext-server.xml")
public class MqRpcStart3 {
    public static void main(String[] args) {
        SpringApplication.run(MqRpcStart3.class, args);
    }
}
