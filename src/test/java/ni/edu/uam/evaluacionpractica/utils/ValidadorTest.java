package ni.edu.uam.evaluacionpractica.utils;

import ni.edu.uam.evaluacionpractica.modelocrud.dao.CitaTecnicaDAO;
import ni.edu.uam.evaluacionpractica.modelocrud.model.CitaTecnica;
import ni.edu.uam.evaluacionpractica.modelocrud.model.Cliente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.DynamicTest;

import java.sql.SQLException;
import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorTest {
    private static final Clock RELOJ = Clock.fixed(
            Instant.parse("2026-10-15T16:30:45Z"), ZoneId.of("America/Guatemala"));

    private CitaTecnica cita() {
        return CitaTecnica.builder().idCliente(1).fecha(LocalDate.of(2026, 10, 15))
                .hora(LocalTime.of(11, 0)).tipoServicio("Diagnóstico")
                .descripcion("Revisar equipo").estado(CitaTecnica.PENDIENTE).build();
    }

    @TestFactory
    Stream<DynamicTest> horasInvalidas() {
        return Arrays.asList(null, "", "  ", "24:00", "23:60", "-1:00", "9:30", "12:3",
                "12:30:00", "ab:cd").stream().map(hora -> DynamicTest.dynamicTest(
                "Hora inválida: " + hora, () -> assertThrows(IllegalArgumentException.class,
                        () -> Validador.convertirHora(hora))));
    }

    @TestFactory
    Stream<DynamicTest> horasValidas() {
        return Stream.of("00:00", "09:30", "23:59", " 12:00 ").map(hora ->
                DynamicTest.dynamicTest(hora, () -> assertEquals(LocalTime.parse(hora.trim()),
                        Validador.convertirHora(hora))));
    }

    @TestFactory
    Stream<DynamicTest> correosInvalidos() {
        return Arrays.asList(null, "", "a", "a@", "@ejemplo.com", "a..b@ejemplo.com",
                ".a@ejemplo.com", "a.@ejemplo.com", "a@-ejemplo.com", "a@ejemplo-.com",
                "a b@ejemplo.com", "a@localhost").stream().map(correo ->
                DynamicTest.dynamicTest("Correo inválido: " + correo,
                        () -> assertFalse(Validador.esEmailValido(correo))));
    }

    @TestFactory
    Stream<DynamicTest> telefonos() {
        return Arrays.asList(null, "", "1234567", "123456789", "8888-1111", "abcdefgh")
                .stream().map(telefono -> DynamicTest.dynamicTest("Teléfono inválido: " + telefono,
                        () -> assertFalse(Validador.esTelefonoValido(telefono))));
    }

    @Test
    void contactosValidosYDuplicados() {
        assertTrue(Validador.esTelefonoValido(" 88881111 "));
        assertTrue(Validador.esEmailValido(" Ana.Perez+trabajo@ejemplo.com "));
        Cliente cliente = Cliente.builder().idCliente(7).correo("ANA@ejemplo.com").build();
        assertTrue(Validador.esCorreoDuplicado(List.of(cliente), " ana@ejemplo.com ", null));
        assertFalse(Validador.esCorreoDuplicado(List.of(cliente), "ana@ejemplo.com", 7));
        assertTrue(Validador.esCorreoDuplicado(List.of(cliente), "ana@ejemplo.com", 8));
        assertFalse(Validador.esCorreoDuplicado(null, "ana@ejemplo.com", null));
    }

    @TestFactory
    Stream<DynamicTest> datosObligatoriosYLimites() {
        List<Consumer<CitaTecnica>> cambios = List.of(
                c -> c.setIdCliente(null), c -> c.setIdCliente(0), c -> c.setIdCliente(-1),
                c -> c.setFecha(null), c -> c.setHora(null), c -> c.setTipoServicio(null),
                c -> c.setTipoServicio(" \t "), c -> c.setTipoServicio("a".repeat(81)),
                c -> c.setDescripcion(null), c -> c.setDescripcion("\n"),
                c -> c.setDescripcion("a".repeat(256)), c -> c.setEstado(null),
                c -> c.setEstado("Inexistente"));
        return java.util.stream.IntStream.range(0, cambios.size()).mapToObj(i ->
                DynamicTest.dynamicTest("Campo inválido " + i, () -> {
                    CitaTecnica c = cita();
                    cambios.get(i).accept(c);
                    assertThrows(IllegalArgumentException.class, () -> Validador.validarCita(c));
                    SQLException e = assertThrows(SQLException.class, () -> new CitaTecnicaDAO().insertar(c));
                    assertEquals("22023", e.getSQLState());
                }));
    }

    @Test
    void limitesUnicodeYEstados() {
        CitaTecnica c = cita();
        c.setTipoServicio("á".repeat(80));
        c.setDescripcion("😀".repeat(255));
        for (String estado : CitaTecnica.ESTADOS_PERMITIDOS) {
            c.setEstado(estado);
            assertDoesNotThrow(() -> Validador.validarCita(c));
        }
        c.setDescripcion("😀".repeat(256));
        assertThrows(IllegalArgumentException.class, () -> Validador.validarCita(c));
        assertThrows(IllegalArgumentException.class, () -> Validador.validarCita(null));
    }

    @Test
    void rechazaAyerYHoraPasadaHoy() {
        CitaTecnica c = cita();
        c.setFecha(LocalDate.of(2026, 10, 14));
        assertThrows(IllegalArgumentException.class, () -> Validador.validarProgramacion(c, null, RELOJ));
        c.setFecha(LocalDate.of(2026, 10, 15));
        c.setHora(LocalTime.of(10, 29));
        assertThrows(IllegalArgumentException.class, () -> Validador.validarProgramacion(c, null, RELOJ));
    }

    @Test
    void aceptaMinutoActualYFuturo() {
        CitaTecnica c = cita();
        c.setHora(LocalTime.of(10, 30));
        assertDoesNotThrow(() -> Validador.validarProgramacion(c, null, RELOJ));
        c.setHora(LocalTime.of(10, 31));
        assertDoesNotThrow(() -> Validador.validarProgramacion(c, null, RELOJ));
        c.setFecha(LocalDate.of(2026, 10, 16));
        c.setHora(LocalTime.MIDNIGHT);
        assertDoesNotThrow(() -> Validador.validarProgramacion(c, null, RELOJ));
    }

    @Test
    void permiteCerrarHistoricaPeroNoReprogramarEnPasado() {
        CitaTecnica anterior = cita();
        anterior.setIdCita(3);
        anterior.setFecha(LocalDate.of(2026, 10, 14));
        CitaTecnica editada = cita();
        editada.setIdCita(3);
        editada.setFecha(anterior.getFecha());
        editada.setEstado(CitaTecnica.FINALIZADA);
        assertDoesNotThrow(() -> Validador.validarProgramacion(editada, anterior, RELOJ));
        editada.setHora(LocalTime.of(12, 0));
        assertThrows(IllegalArgumentException.class, () -> Validador.validarProgramacion(editada, anterior, RELOJ));
        editada.setFecha(LocalDate.of(2026, 10, 16));
        assertDoesNotThrow(() -> Validador.validarProgramacion(editada, anterior, RELOJ));
    }

    @Test
    void noPermiteUsarOtraCitaOClienteParaEvitarValidacion() {
        CitaTecnica anterior = cita();
        anterior.setIdCita(3);
        anterior.setFecha(LocalDate.of(2026, 10, 14));
        CitaTecnica editada = cita();
        editada.setFecha(anterior.getFecha());
        assertThrows(IllegalArgumentException.class, () -> Validador.validarProgramacion(editada, anterior, RELOJ));
        editada.setIdCita(3);
        editada.setIdCliente(2);
        assertThrows(IllegalArgumentException.class, () -> Validador.validarProgramacion(editada, anterior, RELOJ));
    }

    @Test
    void daoRechazaPasadoAntesDeConectar() {
        CitaTecnica c = cita();
        c.setFecha(LocalDate.now().minusDays(1));
        assertEquals("22023", assertThrows(SQLException.class,
                () -> new CitaTecnicaDAO().insertar(c)).getSQLState());
    }

    @Test
    void referenciasNulasEIdsInvalidos() {
        CitaTecnicaDAO dao = new CitaTecnicaDAO();
        assertThrows(SQLException.class, () -> dao.insertar(null));
        assertThrows(SQLException.class, () -> dao.eliminar(null));
        assertThrows(SQLException.class, () -> dao.buscarPorId(0));
        assertThrows(SQLException.class, () -> dao.listarPorCliente(-1));
        assertThrows(SQLException.class, () -> dao.filtrarPorEstado("Otro"));
        CitaTecnica c = cita();
        assertThrows(SQLException.class, () -> dao.actualizar(c));
        assertTrue(Validador.tieneCitasAsociadas(List.of(c), 1));
        assertFalse(Validador.tieneCitasAsociadas(List.of(c), 2));
        assertFalse(Validador.tieneCitasAsociadas(null, 1));
    }
}
