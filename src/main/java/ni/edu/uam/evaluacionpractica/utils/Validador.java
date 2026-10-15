package ni.edu.uam.evaluacionpractica.utils;

import ni.edu.uam.evaluacionpractica.modelocrud.model.CitaTecnica;
import ni.edu.uam.evaluacionpractica.modelocrud.model.Cliente;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Validador {

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9_+%-]+(?:\\.[A-Za-z0-9_+%-]+)*@"
                    + "[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");
    private static final Pattern TELEFONO = Pattern.compile("^[0-9]{8}$");


    public static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    public static boolean esEmailValido(String email) {
        if (esVacio(email)) return false;
        return EMAIL.matcher(email.trim()).matches();
    }

    public static boolean esTelefonoValido(String telefono) {
        if (esVacio(telefono)) return false;
        return TELEFONO.matcher(telefono.trim()).matches();
    }

    public static boolean esFechaHoraValida(LocalDate fecha, LocalTime hora) {
        return fecha != null && hora != null;
    }


    public static boolean esCorreoDuplicado(List<Cliente> clientes, String correo, Integer idClienteActual) {
        if (clientes == null || esVacio(correo)) return false;

        return clientes.stream().anyMatch(cliente -> {
            boolean mismoCorreo = cliente != null && cliente.getCorreo() != null &&
                    cliente.getCorreo().trim().equalsIgnoreCase(correo.trim());

            if (idClienteActual == null) {
                return mismoCorreo;
            }
            return mismoCorreo && !Objects.equals(cliente.getIdCliente(), idClienteActual);
        });
    }


    public static boolean tieneCitasAsociadas(List<CitaTecnica> listaCitas, int idCliente) {
        if (listaCitas == null || listaCitas.isEmpty()) return false;

        return listaCitas.stream()
                .anyMatch(cita -> cita != null && Objects.equals(cita.getIdCliente(), idCliente));
    }


    public static LocalTime convertirHora(String texto) {
        if (!esVacio(texto) && texto.trim().matches("[0-9]{2}:[0-9]{2}")) {
            try {
                return LocalTime.parse(texto.trim());
            } catch (DateTimeParseException e) {
                // Se usa el mismo mensaje para un formato o un rango incorrecto.
            }
        }
        throw new IllegalArgumentException("Ingrese una hora válida en formato HH:mm (00:00 a 23:59).");
    }

    public static boolean esEstadoValido(String estado) {
        return estado != null && CitaTecnica.ESTADOS_PERMITIDOS.contains(estado);
    }

    public static void validarCita(CitaTecnica cita) {
        if (cita == null) {
            throw new IllegalArgumentException("Debe proporcionar los datos de la cita.");
        }
        if (cita.getIdCliente() == null || cita.getIdCliente() <= 0) {
            throw new IllegalArgumentException("Seleccione un cliente existente.");
        }
        if (!esFechaHoraValida(cita.getFecha(), cita.getHora())) {
            throw new IllegalArgumentException("La fecha y la hora de la cita son obligatorias.");
        }
        validarTexto(cita.getTipoServicio(), "El tipo de servicio", 80);
        validarTexto(cita.getDescripcion(), "La descripción", 255);
        if (!esEstadoValido(cita.getEstado())) {
            throw new IllegalArgumentException(
                    "Seleccione un estado: Pendiente, En proceso, Finalizada o Cancelada.");
        }
    }

    private static void validarTexto(String texto, String campo, int maximo) {
        if (esVacio(texto)) {
            throw new IllegalArgumentException(campo + " es obligatorio.");
        }
        String valor = texto.trim();
        if (valor.codePointCount(0, valor.length()) > maximo) {
            throw new IllegalArgumentException(campo + " no puede superar " + maximo + " caracteres.");
        }
    }
}
