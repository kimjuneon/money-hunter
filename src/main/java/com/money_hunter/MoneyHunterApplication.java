package com.money_hunter;

import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.config.EconomyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AppProperties.class, EconomyProperties.class})
public class MoneyHunterApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyHunterApplication.class, args);
	}

}
