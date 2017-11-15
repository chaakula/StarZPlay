package com.starzplay.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring boot initializer
 * @author Chandra Sekhar Babu A
 *
 */
@SpringBootApplication
@ComponentScan("com.starzplay")
public class SpringbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootApplication.class, args);
		System.out.println("Application started.......");
	}

}