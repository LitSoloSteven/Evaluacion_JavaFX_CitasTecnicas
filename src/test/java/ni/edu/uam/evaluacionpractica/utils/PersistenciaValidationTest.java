package ni.edu.uam.evaluacionpractica.utils;

import ni.edu.uam.evaluacionpractica.modelocrud.dao.CitaTecnicaDAO;
import ni.edu.uam.evaluacionpractica.modelocrud.dao.ClienteDAO;
import ni.edu.uam.evaluacionpractica.modelocrud.model.CitaTecnica;
import ni.edu.uam.evaluacionpractica.modelocrud.model.Cliente;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.*;
import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Solo se ejecuta contra una base desechable preparada con gestion_citas.sql. */
@EnabledIfEnvironmentVariable(named = "CITAS_TEST_DB", matches = "true")
class PersistenciaValidationTest {
    private final ClienteDAO clientes = new ClienteDAO();
    private final CitaTecnicaDAO citas = new CitaTecnicaDAO();

    @BeforeAll
    static void exigirBaseAislada() {
        assertTrue(System.getenv("DB_URL").matches(
                "jdbc:postgresql://localhost:5432/gestion_citas_auditoria_[a-z0-9]+"));
    }

    @AfterAll
    static void cerrar() { ConexionBD.cerrar(); }

    private Cliente cliente() throws SQLException {
        Cliente c = Cliente.builder().nombres("Prueba").apellidos("Auditoría")
                .telefono("88881111").correo(UUID.randomUUID() + "@example.com").activo(true).build();
        assertTrue(clientes.insertar(c));
        return c;
    }

    private CitaTecnica cita(Cliente cliente) {
        return CitaTecnica.builder().idCliente(cliente.getIdCliente())
                .fecha(LocalDate.now().plusDays(2)).hora(LocalTime.of(10, 0))
                .tipoServicio("Diagnóstico").descripcion("Prueba aislada")
                .estado(CitaTecnica.PENDIENTE).build();
    }

    @Test
    void crudBusquedaFiltrosEIntegridad() throws SQLException {
        Cliente cliente = cliente();
        assertTrue(clientes.existeCorreo(cliente.getCorreo().toUpperCase()));
        assertFalse(clientes.existeCorreoEnOtro(cliente.getCorreo(), cliente.getIdCliente()));
        assertTrue(clientes.buscar(cliente.getCorreo()).stream()
                .anyMatch(c -> c.getIdCliente().equals(cliente.getIdCliente())));
        cliente.setNombres("Editado");
        assertTrue(clientes.actualizar(cliente));
        assertEquals("Editado", clientes.buscarPorId(cliente.getIdCliente()).orElseThrow().getNombres());
        CitaTecnica cita = cita(cliente);
        assertTrue(citas.insertar(cita));
        assertTrue(clientes.tieneCitasAsociadas(cliente.getIdCliente()));
        String restriccion = assertThrows(SQLException.class,
                () -> clientes.eliminar(cliente.getIdCliente())).getSQLState();
        assertTrue(java.util.Set.of("23503", "23001").contains(restriccion));
        assertTrue(citas.filtrar(cita.getFecha(), CitaTecnica.PENDIENTE).stream()
                .anyMatch(c -> c.getIdCita().equals(cita.getIdCita())));
        assertTrue(citas.filtrar(cita.getFecha(), CitaTecnica.CANCELADA).stream()
                .noneMatch(c -> c.getIdCita().equals(cita.getIdCita())));
        cita.setEstado(CitaTecnica.CANCELADA);
        assertTrue(citas.actualizar(cita));
        assertEquals(CitaTecnica.CANCELADA, citas.buscarPorId(cita.getIdCita()).orElseThrow().getEstado());
        assertTrue(citas.eliminar(cita.getIdCita()));
        assertTrue(clientes.eliminar(cliente.getIdCliente()));
    }

    @Test
    void correoDuplicadoSinDistinguirMayusculas() throws SQLException {
        Cliente c = cliente();
        c.setCorreo(c.getCorreo().toUpperCase());
        assertEquals("23505", assertThrows(SQLException.class, () -> clientes.insertar(c)).getSQLState());
    }

    @Test
    void clienteInexistente() {
        Cliente c = Cliente.builder().idCliente(Integer.MAX_VALUE).build();
        assertEquals("23503", assertThrows(SQLException.class, () -> citas.insertar(cita(c))).getSQLState());
    }

    @Test
    void actualizarHistoricaYRechazarReprogramacionAlPasado() throws SQLException {
        CitaTecnica c = cita(cliente());
        citas.insertar(c);
        // Simula el paso del tiempo sin depender del reloj ni modificar datos del usuario.
        try (Connection cn = ConexionBD.abrirConexion(); PreparedStatement ps = cn.prepareStatement(
                "UPDATE cita_tecnica SET fecha = CURRENT_DATE - 1 WHERE id_cita = ?")) {
            ps.setInt(1, c.getIdCita());
            ps.executeUpdate();
        }
        c = citas.buscarPorId(c.getIdCita()).orElseThrow();
        c.setEstado(CitaTecnica.FINALIZADA);
        assertTrue(citas.actualizar(c));
        CitaTecnica editada = c;
        editada.setHora(LocalTime.of(11, 0));
        assertEquals("22023", assertThrows(SQLException.class,
                () -> citas.actualizar(editada)).getSQLState());
        assertEquals(LocalTime.of(10, 0), citas.buscarPorId(c.getIdCita()).orElseThrow().getHora());
    }

    // Caracterización de defectos pendientes de aprobación: estos tests DOCUMENTAN
    // discrepancias actuales; no significan que esas reglas sean correctas.
    @Test
    void documentaCorreoAceptadoEnJavaPeroRechazadoPorSQL() throws SQLException {
        Cliente c = cliente();
        c.setCorreo("usuario@dominio.c");
        assertTrue(Validador.esEmailValido(c.getCorreo()));
        assertEquals("23514", assertThrows(SQLException.class, () -> clientes.actualizar(c)).getSQLState());
    }

    @Test
    void documentaTelefonoInvalidoQueElDaoPermite() throws SQLException {
        Cliente c = cliente();
        c.setTelefono("abcdefgh");
        assertFalse(Validador.esTelefonoValido(c.getTelefono()));
        assertTrue(clientes.actualizar(c));
        assertEquals("abcdefgh", clientes.buscarPorId(c.getIdCliente()).orElseThrow().getTelefono());
    }

    @Test
    void documentaDatosDePruebaIncompatiblesConFormulario() throws SQLException {
        Cliente c = clientes.buscar("carlos.rodriguez@uam.edu.ni").getFirst();
        assertEquals("8888-1111", c.getTelefono());
        assertFalse(Validador.esTelefonoValido(c.getTelefono()));
    }
}
