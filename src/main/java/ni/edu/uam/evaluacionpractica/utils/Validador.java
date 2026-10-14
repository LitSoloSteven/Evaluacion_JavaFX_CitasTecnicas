package ni.edu.uam.evaluacionpractica.utils;

import java.util.regex.Pattern;

public class Validador {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final String TELEFONO_REGEX = "^[0-9]{8}$"; // 8 dígitos (Ajustar según requerimiento)

    public static boolean esVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    public static boolean esEmailValido(String email) {
        if (esVacio(email)) return false;
        return Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }

    public static boolean esTelefonoValido(String telefono) {
        if (esVacio(telefono)) return false;
        return Pattern.compile(TELEFONO_REGEX).matcher(telefono).matches();
    }

    public static boolean esNumeroEntero(String texto) {
        if (esVacio(texto)) return false;
        try {
            Integer.parseInt(texto.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean esNumeroDecimal(String texto) {
        if (esVacio(texto)) return false;
        try {
            Double.parseDouble(texto.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
