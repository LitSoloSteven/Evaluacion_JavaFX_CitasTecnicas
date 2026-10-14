package ni.edu.uam.evaluacionpractica.modelocrud.dao;


import ni.edu.uam.evaluacionpractica.modelocrud.interfaces.CrudRepository;
import ni.edu.uam.evaluacionpractica.modelocrud.model.Cliente;
import ni.edu.uam.evaluacionpractica.utils.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ClienteDAO implements CrudRepository<Cliente, Integer> {

    private static final String SQL_INSERT =
            "INSERT INTO cliente (nombres, apellidos, telefono, correo, activo) " +
                    "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE cliente SET nombres = ?, apellidos = ?, telefono = ?, " +
                    "correo = ?, activo = ? WHERE id_cliente = ?";

    private static final String SQL_DELETE =
            "DELETE FROM cliente WHERE id_cliente = ?";

    private static final String SQL_FIND_BY_ID =
            "SELECT id_cliente, nombres, apellidos, telefono, correo, activo " +
                    "FROM cliente WHERE id_cliente = ?";

    private static final String SQL_FIND_ALL =
            "SELECT id_cliente, nombres, apellidos, telefono, correo, activo " +
                    "FROM cliente ORDER BY id_cliente";

    private static final String SQL_EXISTS_CORREO =
            "SELECT 1 FROM cliente WHERE LOWER(correo) = LOWER(?) LIMIT 1";

    private static final String SQL_EXISTS_CORREO_OTRO =
            "SELECT 1 FROM cliente WHERE LOWER(correo) = LOWER(?) " +
                    "AND id_cliente <> ? LIMIT 1";

    private static final String SQL_BUSCAR =
            "SELECT id_cliente, nombres, apellidos, telefono, correo, activo " +
                    "FROM cliente " +
                    "WHERE LOWER(nombres)   LIKE LOWER(?) " +
                    "   OR LOWER(apellidos) LIKE LOWER(?) " +
                    "   OR LOWER(correo)    LIKE LOWER(?) " +
                    "ORDER BY apellidos, nombres";


    @Override
    public boolean insertar(Cliente cliente) throws SQLException {
        Connection cn = ConexionBD.getConexion();
        try (PreparedStatement ps = cn.prepareStatement(
                SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombres());
            ps.setString(2, cliente.getApellidos());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getCorreo());
            ps.setBoolean(5, cliente.isActivo());

            int filas = ps.executeUpdate();
            if (filas == 0) return false;

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    cliente.setIdCliente(keys.getInt(1));
                }
            }
            return true;
        }
    }

    @Override
    public boolean actualizar(Cliente cliente) throws SQLException {
        if (cliente.getIdCliente() == null) {
            throw new SQLException("No se puede actualizar un cliente sin id_cliente.");
        }
        Connection cn = ConexionBD.getConexion();
        try (PreparedStatement ps = cn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, cliente.getNombres());
            ps.setString(2, cliente.getApellidos());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getCorreo());
            ps.setBoolean(5, cliente.isActivo());
            ps.setInt(6, cliente.getIdCliente());

            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminar(Integer id) throws SQLException {
        Connection cn = ConexionBD.getConexion();
        try (PreparedStatement ps = cn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(Integer id) throws SQLException {
        Connection cn = ConexionBD.getConexion();
        try (PreparedStatement ps = cn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Cliente> listarTodos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        Connection cn = ConexionBD.getConexion();
        try (PreparedStatement ps = cn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }


    public List<Cliente> buscar(String termino) throws SQLException {
        if (termino == null || termino.isBlank()) {
            return listarTodos();
        }
        String patron = "%" + termino.trim() + "%";
        List<Cliente> lista = new ArrayList<>();
        Connection cn = ConexionBD.getConexion();

        try (PreparedStatement ps = cn.prepareStatement(SQL_BUSCAR)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public boolean existeCorreo(String correo) throws SQLException {
        if (correo == null || correo.isBlank()) return false;
        Connection cn = ConexionBD.getConexion();
        try (PreparedStatement ps = cn.prepareStatement(SQL_EXISTS_CORREO)) {
            ps.setString(1, correo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existeCorreoEnOtro(String correo, Integer idExcluir) throws SQLException {
        if (correo == null || correo.isBlank()) return false;
        Connection cn = ConexionBD.getConexion();
        try (PreparedStatement ps = cn.prepareStatement(SQL_EXISTS_CORREO_OTRO)) {
            ps.setString(1, correo.trim());
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    public boolean tieneCitasAsociadas(Integer idCliente) throws SQLException {
        String sql = "SELECT 1 FROM cita_tecnica WHERE id_cliente = ? LIMIT 1";
        Connection cn = ConexionBD.getConexion();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    private Cliente mapear(ResultSet rs) throws SQLException {
        return Cliente.builder()
                .idCliente(rs.getInt("id_cliente"))
                .nombres(rs.getString("nombres"))
                .apellidos(rs.getString("apellidos"))
                .telefono(rs.getString("telefono"))
                .correo(rs.getString("correo"))
                .activo(rs.getBoolean("activo"))
                .build();
    }
}
