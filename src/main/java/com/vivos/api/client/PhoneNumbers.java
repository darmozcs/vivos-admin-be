package com.vivos.api.client;

/**
 * Telefonos de Uruguay. Las admins los cargan como quieren ("099 123 456",
 * "+598 99 123 456"...) y WhatsApp los manda como 59899123456: {@link #normalize}
 * los lleva a ese formato para poder compararlos.
 */
public final class PhoneNumbers {

    private static final String COUNTRY_CODE = "598";

    private PhoneNumbers() {
    }

    /** Solo digitos y con el 598 adelante si es un numero uruguayo. Null o sin digitos: "". */
    public static String normalize(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.startsWith(COUNTRY_CODE)) {
            return digits;
        }
        if (digits.matches("0[0-9]{8}")) {
            return COUNTRY_CODE + digits.substring(1);
        }
        if (digits.matches("9[0-9]{7}")) {
            return COUNTRY_CODE + digits;
        }
        return digits;
    }

    /**
     * Como lo cargan las admins: 59899123456 queda "099 123 456". Los que no son
     * celulares uruguayos quedan con "+" y los digitos.
     */
    public static String toLocal(String normalized) {
        if (normalized.matches(COUNTRY_CODE + "9[0-9]{7}")) {
            String local = "0" + normalized.substring(COUNTRY_CODE.length());
            return local.substring(0, 3) + " " + local.substring(3, 6) + " " + local.substring(6);
        }
        return "+" + normalized;
    }
}
