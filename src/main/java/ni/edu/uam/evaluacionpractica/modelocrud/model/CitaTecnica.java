package ni.edu.uam.evaluacionpractica.modelocrud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaTecnica {

    public static final String PENDIENTE = "Pendiente";
    public static final String EN_PROCESO = "En proceso";
    public static final String FINALIZADA = "Finalizada";
    public static final String CANCELADA = "Cancelada";
    public static final List<String> ESTADOS_PERMITIDOS =
            List.of(PENDIENTE, EN_PROCESO, FINALIZADA, CANCELADA);

    private Integer idCita;
    private Integer idCliente;
    // Se obtiene mediante JOIN; no se duplica en la tabla cita_tecnica.
    private String nombreCliente;
    private LocalDate fecha;
    private LocalTime hora;
    private String tipoServicio;
    private String descripcion;
    private String estado;
}
