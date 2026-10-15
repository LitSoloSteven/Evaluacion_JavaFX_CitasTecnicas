package ni.edu.uam.evaluacionpractica.modelocrud.dao;

import ni.edu.uam.evaluacionpractica.modelocrud.interfaces.CrudRepository;
import ni.edu.uam.evaluacionpractica.modelocrud.model.CitaTecnica;
import ni.edu.uam.evaluacionpractica.utils.ConexionBD;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso JDBC a citas. Las SQLException se propagan al controlador para mostrar alertas. */
public class CitaTecnicaDAO implements CrudRepository<CitaTecnica, Integer> {

    private static final String SQL_SELECT =
            "SELECT ct.id_cita, ct.id_cliente, " +
            "c.nombres || ' ' || c.apellidos AS nombre_cliente, " +
            "ct.fecha, ct.hora, ct.tipo_servicio, ct.descripcion, ct.estado " +
            "FROM cita_tecnica ct JOIN cliente c ON c.id_cliente = ct.id_cliente";

    private static final String SQL_ORDEN = " ORDER BY ct.fecha, ct.hora, ct.id_cita";
    private static final String SQL_INSERT =
            "INSERT INTO cita_tecnica " +
            "(id_cliente, fecha, hora, tipo_servicio, descripcion, estado) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE =
            "UPDATE cita_tecnica SET id_cliente = ?, fecha = ?, hora = ?, " +
            "tipo_servicio = ?, descripcion = ?, estado = ? WHERE id_cita = ?";
    private static final String SQL_DELETE = "DELETE FROM cita_tecnica WHERE id_cita = ?";

    @Override
    public boolean insertar(CitaTecnica cita) throws SQLException {
        validar(cita);
        try (Connection cn = ConexionBD.abrirConexion()) {
            comprobarCliente(cn, cita.getIdCliente());
            try (PreparedStatement ps = cn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                asignarDatos(ps, cita);
                if (ps.executeUpdate() == 0) return false;
                try (ResultSet claves = ps.getGeneratedKeys()) {
                    if (claves.next()) cita.setIdCita(claves.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            throw explicarRelacion(e);
        }
    }

    @Override
    public boolean actualizar(CitaTecnica cita) throws SQLException {
        validar(cita);
        validarId(cita.getIdCita(), "cita");
        try (Connection cn = ConexionBD.abrirConexion()) {
            comprobarCliente(cn, cita.getIdCliente());
            try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {
                asignarDatos(ps, cita);
                ps.setInt(7, cita.getIdCita());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw explicarRelacion(e);
        }
    }

    /** Eliminación física de la cita, sin eliminar al cliente. */
    @Override
    public boolean eliminar(Integer id) throws SQLException {
        validarId(id, "cita");
        try (Connection cn = ConexionBD.abrirConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<CitaTecnica> buscarPorId(Integer id) throws SQLException {
        validarId(id, "cita");
        try (Connection cn = ConexionBD.abrirConexion();
             PreparedStatement ps = cn.prepareStatement(SQL_SELECT + " WHERE ct.id_cita = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<CitaTecnica> listarTodos() throws SQLException {
        return filtrar(null, null);
    }

    /**
     * Combina ambos filtros con AND. Fecha nula o estado nulo/vacío desactivan
     * el filtro correspondiente; filtrar(null, null) recupera todas las citas.
     */
    public List<CitaTecnica> filtrar(LocalDate fecha, String estado) throws SQLException {
        boolean porEstado = estado != null && !estado.isBlank();
        if (porEstado) validarEstado(estado);

        // Solo se concatenan fragmentos SQL fijos, nunca valores del usuario.
        String sql = SQL_SELECT + " WHERE 1 = 1";
        if (fecha != null) sql += " AND ct.fecha = ?";
        if (porEstado) sql += " AND ct.estado = ?";
        sql += SQL_ORDEN;

        try (Connection cn = ConexionBD.abrirConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int indice = 1;
            if (fecha != null) ps.setDate(indice++, Date.valueOf(fecha));
            if (porEstado) ps.setString(indice, estado);
            return leerLista(ps);
        }
    }

    public List<CitaTecnica> filtrarPorFecha(LocalDate fecha) throws SQLException {
        return filtrar(fecha, null);
    }

    public List<CitaTecnica> filtrarPorEstado(String estado) throws SQLException {
        return filtrar(null, estado);
    }

    public List<CitaTecnica> listarPorCliente(Integer idCliente) throws SQLException {
        validarId(idCliente, "cliente");
        try (Connection cn = ConexionBD.abrirConexion();
             PreparedStatement ps = cn.prepareStatement(
                     SQL_SELECT + " WHERE ct.id_cliente = ?" + SQL_ORDEN)) {
            ps.setInt(1, idCliente);
            return leerLista(ps);
        }
    }

    private void asignarDatos(PreparedStatement ps, CitaTecnica cita) throws SQLException {
        ps.setInt(1, cita.getIdCliente());
        ps.setDate(2, Date.valueOf(cita.getFecha()));
        ps.setTime(3, Time.valueOf(cita.getHora()));
        ps.setString(4, cita.getTipoServicio().trim());
        ps.setString(5, cita.getDescripcion().trim());
        ps.setString(6, cita.getEstado());
    }

    private List<CitaTecnica> leerLista(PreparedStatement ps) throws SQLException {
        List<CitaTecnica> citas = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) citas.add(mapear(rs));
        }
        return citas;
    }

    private CitaTecnica mapear(ResultSet rs) throws SQLException {
        return CitaTecnica.builder()
                .idCita(rs.getInt("id_cita"))
                .idCliente(rs.getInt("id_cliente"))
                .nombreCliente(rs.getString("nombre_cliente"))
                .fecha(rs.getDate("fecha").toLocalDate())
                .hora(rs.getTime("hora").toLocalTime())
                .tipoServicio(rs.getString("tipo_servicio"))
                .descripcion(rs.getString("descripcion"))
                .estado(rs.getString("estado"))
                .build();
    }

    private void comprobarCliente(Connection cn, Integer idCliente) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT 1 FROM cliente WHERE id_cliente = ?")) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("El cliente seleccionado no existe.", "23503");
                }
            }
        }
        // La FK también protege si otro usuario elimina al cliente después de esta consulta.
    }

    private SQLException explicarRelacion(SQLException e) {
        if ("23503".equals(e.getSQLState())) {
            return new SQLException(
                    "No se puede guardar la cita: el cliente seleccionado no existe o fue eliminado.",
                    e.getSQLState(), e.getErrorCode(), e);
        }
        return e;
    }

    private void validar(CitaTecnica cita) throws SQLException {
        if (cita == null) throw new SQLException("Debe proporcionar una cita.", "22023");
        validarId(cita.getIdCliente(), "cliente");
        if (cita.getFecha() == null) throw new SQLException("La fecha es obligatoria.", "22023");
        if (cita.getHora() == null) throw new SQLException("La hora es obligatoria.", "22023");
        validarTexto(cita.getTipoServicio(), "El tipo de servicio", 80);
        validarTexto(cita.getDescripcion(), "La descripción", 255);
        validarEstado(cita.getEstado());
    }

    private void validarId(Integer id, String entidad) throws SQLException {
        if (id == null || id <= 0) {
            throw new SQLException("Debe seleccionar un ID de " + entidad + " válido.", "22023");
        }
    }

    private void validarTexto(String texto, String campo, int maximo) throws SQLException {
        if (texto == null || texto.isBlank()) {
            throw new SQLException(campo + " es obligatorio.", "22023");
        }
        if (texto.trim().codePointCount(0, texto.trim().length()) > maximo) {
            throw new SQLException(campo + " no puede superar " + maximo + " caracteres.", "22023");
        }
    }

    private void validarEstado(String estado) throws SQLException {
        if (estado == null || !CitaTecnica.ESTADOS_PERMITIDOS.contains(estado)) {
            throw new SQLException(
                    "El estado debe ser Pendiente, En proceso, Finalizada o Cancelada.", "22023");
        }
    }
}
