package com.money_hunter;

import org.springframework.boot.SpringApplication;

public class TestMoneyHunterApplication {

	public static void main(String[] args) {
		SpringApplication.from(MoneyHunterApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
