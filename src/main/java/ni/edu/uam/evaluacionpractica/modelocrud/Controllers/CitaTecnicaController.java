package ni.edu.uam.evaluacionpractica.modelocrud.Controllers;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import ni.edu.uam.evaluacionpractica.modelocrud.dao.CitaTecnicaDAO;
import ni.edu.uam.evaluacionpractica.modelocrud.dao.ClienteDAO;
import ni.edu.uam.evaluacionpractica.modelocrud.model.CitaTecnica;
import ni.edu.uam.evaluacionpractica.modelocrud.model.Cliente;
import ni.edu.uam.evaluacionpractica.utils.AlertUtil;
import ni.edu.uam.evaluacionpractica.utils.Validador;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Objects;

public class CitaTecnicaController {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final System.Logger LOG = System.getLogger(CitaTecnicaController.class.getName());

    private final CitaTecnicaDAO citaDAO = new CitaTecnicaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<CitaTecnica> citas = FXCollections.observableArrayList();
    private CitaTecnica citaSeleccionada;

    @FXML private ComboBox<Cliente> cmbCliente;
    @FXML private DatePicker dpFecha;
    @FXML private TextField txtHora;
    @FXML private TextField txtTipoServicio;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private DatePicker dpFiltroFecha;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private TableView<CitaTecnica> tblCitas;
    @FXML private TableColumn<CitaTecnica, Integer> colId;
    @FXML private TableColumn<CitaTecnica, String> colCliente;
    @FXML private TableColumn<CitaTecnica, String> colFecha;
    @FXML private TableColumn<CitaTecnica, String> colHora;
    @FXML private TableColumn<CitaTecnica, String> colTipoServicio;
    @FXML private TableColumn<CitaTecnica, String> colDescripcion;
    @FXML private TableColumn<CitaTecnica, String> colEstado;
    @FXML private Button btnGuardar;
    @FXML private Button btnActualizar;
    @FXML private Button btnEliminar;
    @FXML private Label lblTotal;

    @FXML
    private void initialize() {
        configurarFecha(dpFecha);
        configurarFecha(dpFiltroFecha);
        cmbEstado.getItems().setAll(CitaTecnica.ESTADOS_PERMITIDOS);
        // Un valor vacío representa todos los estados.
        cmbFiltroEstado.getItems().setAll(CitaTecnica.ESTADOS_PERMITIDOS);
        cmbCliente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cliente cliente) {
                return cliente == null ? "" : cliente.getIdCliente() + " - " + cliente.getNombreCompleto();
            }

            @Override
            public Cliente fromString(String texto) {
                return null; // Solo se permite seleccionar clientes existentes.
            }
        });
        cmbCliente.setOnShowing(event -> cargarClientes());

        colId.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getIdCita()));
        colCliente.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getNombreCliente()));
        colFecha.setCellValueFactory(cell -> new ReadOnlyStringWrapper(FECHA.format(cell.getValue().getFecha())));
        colHora.setCellValueFactory(cell -> new ReadOnlyStringWrapper(HORA.format(cell.getValue().getHora())));
        colTipoServicio.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getTipoServicio()));
        colDescripcion.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getDescripcion()));
        colEstado.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getEstado()));
        tblCitas.setItems(citas);
        tblCitas.getSelectionModel().selectedItemProperty().addListener(
                (observable, anterior, seleccionada) -> mostrarCita(seleccionada));

        onLimpiar();
        cargarClientes();
        cargarCitas();
    }

    private void configurarFecha(DatePicker selector) {
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : FECHA.format(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {
                return Validador.esVacio(texto) ? null : LocalDate.parse(texto.trim(), FECHA);
            }
        });
    }

    private void cargarClientes() {
        Cliente seleccionado = cmbCliente.getValue();
        try {
            cmbCliente.getItems().setAll(clienteDAO.listarTodos());
            if (seleccionado != null) seleccionarCliente(seleccionado.getIdCliente());
        } catch (SQLException e) {
            mostrarErrorBD("No se pudo cargar la lista de clientes.", e);
        }
    }

    private void seleccionarCliente(Integer idCliente) {
        cmbCliente.setValue(cmbCliente.getItems().stream()
                .filter(cliente -> Objects.equals(cliente.getIdCliente(), idCliente))
                .findFirst().orElse(null));
    }

    private void cargarCitas() {
        try {
            // Si falla la consulta, se conserva la lista que estaba en pantalla.
            var resultado = citaDAO.filtrar(dpFiltroFecha.getValue(), cmbFiltroEstado.getValue());
            onLimpiar();
            citas.setAll(resultado);
            lblTotal.setText("Citas mostradas: " + citas.size());
        } catch (SQLException e) {
            mostrarErrorBD("No se pudo cargar la lista de citas.", e);
        }
    }

    private void mostrarCita(CitaTecnica cita) {
        citaSeleccionada = cita;
        btnGuardar.setDisable(cita != null);
        btnActualizar.setDisable(cita == null);
        btnEliminar.setDisable(cita == null);
        if (cita == null) {
            cmbCliente.setValue(null);
            dpFecha.setValue(null);
            dpFecha.getEditor().clear();
            txtHora.clear();
            txtTipoServicio.clear();
            txtDescripcion.clear();
            cmbEstado.setValue(CitaTecnica.PENDIENTE);
            return;
        }
        seleccionarCliente(cita.getIdCliente());
        dpFecha.setValue(cita.getFecha());
        dpFecha.getEditor().setText(FECHA.format(cita.getFecha()));
        txtHora.setText(HORA.format(cita.getHora()));
        txtTipoServicio.setText(cita.getTipoServicio());
        txtDescripcion.setText(cita.getDescripcion());
        cmbEstado.setValue(cita.getEstado());
    }

    private CitaTecnica leerFormulario() {
        Cliente cliente = cmbCliente.getValue();
        if (cliente == null || !cmbCliente.getItems().contains(cliente)) {
            throw new IllegalArgumentException("Seleccione un cliente existente.");
        }
        confirmarFecha(dpFecha, "La fecha de la cita");
        CitaTecnica cita = CitaTecnica.builder()
                .idCita(citaSeleccionada == null ? null : citaSeleccionada.getIdCita())
                .idCliente(cliente.getIdCliente())
                .nombreCliente(cliente.getNombreCompleto())
                .fecha(dpFecha.getValue())
                .hora(Validador.convertirHora(txtHora.getText()))
                .tipoServicio(txtTipoServicio.getText().trim())
                .descripcion(txtDescripcion.getText().trim())
                .estado(cmbEstado.getValue())
                .build();
        Validador.validarProgramacion(cita, citaSeleccionada);
        return cita;
    }

    private void confirmarFecha(DatePicker selector, String campo) {
        try {
            // Incluye el texto escrito manualmente, aunque aún no haya perdido el foco.
            selector.commitValue();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(campo + " debe ser válida y tener formato dd/MM/aaaa.");
        }
    }

    @FXML
    private void onGuardar() {
        if (citaSeleccionada != null) {
            AlertUtil.mostrarAdvertencia("Cita seleccionada", "Use Actualizar o pulse Nuevo / Limpiar para registrar otra cita.");
            return;
        }
        try {
            if (!citaDAO.insertar(leerFormulario())) {
                AlertUtil.mostrarAdvertencia("Sin cambios", "No se pudo registrar la cita.");
                return;
            }
            onLimpiar();
            AlertUtil.mostrarInfo("Cita registrada", "La cita se guardó correctamente.");
            cargarCitas();
        } catch (IllegalArgumentException e) {
            AlertUtil.mostrarAdvertencia("Datos inválidos", e.getMessage());
        } catch (SQLException e) {
            mostrarErrorBD("No se pudo guardar la cita.", e);
        }
    }

    @FXML
    private void onActualizar() {
        if (!haySeleccion()) return;
        try {
            CitaTecnica cita = leerFormulario();
            if (!AlertUtil.mostrarConfirmacion("Actualizar cita", "¿Desea guardar los cambios de la cita #" + cita.getIdCita() + "?")) return;
            if (!citaDAO.actualizar(cita)) {
                AlertUtil.mostrarAdvertencia("Sin cambios", "La cita ya no existe. Actualice el listado con Filtrar.");
                return;
            }
            onLimpiar();
            AlertUtil.mostrarInfo("Cita actualizada", "Los cambios se guardaron correctamente.");
            cargarCitas();
        } catch (IllegalArgumentException e) {
            AlertUtil.mostrarAdvertencia("Datos inválidos", e.getMessage());
        } catch (SQLException e) {
            mostrarErrorBD("No se pudo actualizar la cita.", e);
        }
    }

    @FXML
    private void onEliminar() {
        if (!haySeleccion()) return;
        Integer id = citaSeleccionada.getIdCita();
        if (!AlertUtil.mostrarConfirmacion("Eliminar cita", "¿Desea eliminar la cita #" + id + " de "
                + citaSeleccionada.getNombreCliente() + "?")) return;
        try {
            if (!citaDAO.eliminar(id)) {
                AlertUtil.mostrarAdvertencia("Sin cambios", "La cita ya no existe. Actualice el listado con Filtrar.");
                return;
            }
            onLimpiar();
            AlertUtil.mostrarInfo("Cita eliminada", "La cita se eliminó correctamente.");
            cargarCitas();
        } catch (SQLException e) {
            mostrarErrorBD("No se pudo eliminar la cita.", e);
        }
    }

    private boolean haySeleccion() {
        if (citaSeleccionada != null) return true;
        AlertUtil.mostrarAdvertencia("Seleccione una cita", "Seleccione una cita de la tabla para continuar.");
        return false;
    }

    @FXML
    private void onLimpiar() {
        tblCitas.getSelectionModel().clearSelection();
        mostrarCita(null);
    }

    @FXML
    private void onFiltrar() {
        try {
            confirmarFecha(dpFiltroFecha, "La fecha del filtro");
            String estado = cmbFiltroEstado.getValue();
            if (estado != null && !Validador.esEstadoValido(estado)) {
                throw new IllegalArgumentException("Seleccione un estado válido para filtrar.");
            }
            cargarCitas();
        } catch (IllegalArgumentException e) {
            AlertUtil.mostrarAdvertencia("Filtro inválido", e.getMessage());
        }
    }

    @FXML
    private void onLimpiarFiltros() {
        dpFiltroFecha.setValue(null);
        dpFiltroFecha.getEditor().clear();
        cmbFiltroEstado.setValue(null);
        cargarCitas();
    }

    private void mostrarErrorBD(String mensaje, SQLException error) {
        LOG.log(System.Logger.Level.ERROR, mensaje, error);
        String detalle = "Verifique la conexión y vuelva a intentarlo.";
        if ("23503".equals(error.getSQLState())) {
            detalle = "El cliente seleccionado ya no existe. Vuelva a seleccionar un cliente.";
        } else if ("22023".equals(error.getSQLState())) {
            detalle = error.getMessage();
        } else if ("23505".equals(error.getSQLState())) {
            detalle = "Ya existe un registro con esos datos.";
        } else if ("22001".equals(error.getSQLState())) {
            detalle = "Uno de los campos supera la longitud permitida.";
        }
        AlertUtil.mostrarError("Error de base de datos", mensaje + "\n" + detalle);
    }
}
