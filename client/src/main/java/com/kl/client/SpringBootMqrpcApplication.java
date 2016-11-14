package com.kl.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("applicationContext-client.xml")
public class SpringBootMqrpcApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootMqrpcApplication.class, args);
	}
}
