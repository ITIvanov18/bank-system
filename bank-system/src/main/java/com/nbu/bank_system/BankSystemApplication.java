package com.nbu.bank_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point за Spring Boot backend приложението.
 * Стартира application context-а, регистрира конфигурациите и вдига REST API слоя.
 */

@SpringBootApplication
public class BankSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankSystemApplication.class, args);
	}

}
