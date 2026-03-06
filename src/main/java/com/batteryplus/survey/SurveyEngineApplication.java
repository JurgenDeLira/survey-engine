package com.batteryplus.survey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
//QUITAR exclude CUANDO YA HAGA STAGING
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class SurveyEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(SurveyEngineApplication.class, args);
	}

}
