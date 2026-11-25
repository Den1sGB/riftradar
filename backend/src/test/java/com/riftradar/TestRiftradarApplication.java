package com.riftradar;

import org.springframework.boot.SpringApplication;

public class TestRiftradarApplication {

	public static void main(String[] args) {
		SpringApplication.from(RiftradarApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
