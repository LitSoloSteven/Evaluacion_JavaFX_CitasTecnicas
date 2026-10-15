package ni.edu.uam.evaluacionpractica.modelocrud.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    private Integer idCliente;
    private String  nombres;
    private String  apellidos;
    private String  telefono;
    private String  correo;
    private boolean activo;

    /** Útil para mostrar en ComboBox y tablas. */
    public String getNombreCompleto() {
        return (nombres == null ? "" : nombres) + " " +
                (apellidos == null ? "" : apellidos);
    }

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}