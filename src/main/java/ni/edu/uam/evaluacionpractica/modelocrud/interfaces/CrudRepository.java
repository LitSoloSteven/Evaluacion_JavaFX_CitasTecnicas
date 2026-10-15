package ni.edu.uam.evaluacionpractica.modelocrud.interfaces;


import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


public interface CrudRepository<T, ID> {

    boolean insertar(T entidad) throws SQLException;

    boolean actualizar(T entidad) throws SQLException;

    boolean eliminar(ID id) throws SQLException;

    Optional<T> buscarPorId(ID id) throws SQLException;

    List<T> listarTodos() throws SQLException;
}
