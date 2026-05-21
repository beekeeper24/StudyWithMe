package com.studywithme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StudyWithMeApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudyWithMeApplication.class, args);
	}

}
