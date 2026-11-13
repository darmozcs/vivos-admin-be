package com.vivos.api.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumbersTest {

    @Test
    void normalizesTheWaysAdminsWritePhones() {
        assertThat(PhoneNumbers.normalize("099 123 456")).isEqualTo("59899123456");
        assertThat(PhoneNumbers.normalize("+598 99 123 456")).isEqualTo("59899123456");
        assertThat(PhoneNumbers.normalize("0059899123456")).isEqualTo("59899123456");
        assertThat(PhoneNumbers.normalize("99123456")).isEqualTo("59899123456");
        assertThat(PhoneNumbers.normalize("59899123456")).isEqualTo("59899123456");
        assertThat(PhoneNumbers.normalize("099-123-456")).isEqualTo("59899123456");
    }

    @Test
    void leavesOtherNumbersAsDigits() {
        assertThat(PhoneNumbers.normalize("+54 9 11 1234 5678")).isEqualTo("5491112345678");
        assertThat(PhoneNumbers.normalize("2900 1234")).isEqualTo("29001234");
        assertThat(PhoneNumbers.normalize("")).isEmpty();
        assertThat(PhoneNumbers.normalize(null)).isEmpty();
    }

    @Test
    void formatsUruguayanMobilesLikeTheAdmins() {
        assertThat(PhoneNumbers.toLocal("59899123456")).isEqualTo("099 123 456");
        assertThat(PhoneNumbers.toLocal("5491112345678")).isEqualTo("+5491112345678");
    }
}
