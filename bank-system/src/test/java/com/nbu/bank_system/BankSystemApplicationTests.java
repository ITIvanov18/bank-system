package com.nbu.bank_system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Лек smoke test за application class-а
 * Не се стартира пълен Spring context тук, защото това би изисквало database.
 * Реалният context + database flow се покрива от integration тестовете.
 */

class BankSystemApplicationTests {

	@Test
	void applicationClassIsAvailable() {
		assertThat(BankSystemApplication.class).isNotNull();
	}

}
