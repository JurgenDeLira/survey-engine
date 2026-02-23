package com.batteryplus.survey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SurveyEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(SurveyEngineApplication.class, args);
	}

}
