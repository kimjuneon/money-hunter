package com.money_hunter;

import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.config.AdminProperties;
import com.money_hunter.infrastructure.config.AdProperties;
import com.money_hunter.infrastructure.config.CorsProperties;
import com.money_hunter.infrastructure.config.EconomyProperties;
import com.money_hunter.infrastructure.config.IapProperties;
import com.money_hunter.infrastructure.config.PromotionProperties;
import com.money_hunter.infrastructure.config.ShareRewardProperties;
import com.money_hunter.infrastructure.config.SmartMessageProperties;
import com.money_hunter.infrastructure.config.TossApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
		AppProperties.class,
		EconomyProperties.class,
		TossApiProperties.class,
		AdminProperties.class,
		AdProperties.class,
		IapProperties.class,
		PromotionProperties.class,
		ShareRewardProperties.class,
		SmartMessageProperties.class,
		CorsProperties.class
})
public class MoneyHunterApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoneyHunterApplication.class, args);
	}

}
