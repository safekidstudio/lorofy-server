package com.lorofy.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LorofyApplication {

	public static void main(String[] args) {
		SpringApplication.run(LorofyApplication.class, args);
	}

}

