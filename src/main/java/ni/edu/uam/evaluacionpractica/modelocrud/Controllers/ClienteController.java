package ni.edu.uam.evaluacionpractica.modelocrud.Controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import ni.edu.uam.evaluacionpractica.modelocrud.dao.ClienteDAO;
import ni.edu.uam.evaluacionpractica.modelocrud.model.Cliente;
import ni.edu.uam.evaluacionpractica.utils.AlertUtil;
import ni.edu.uam.evaluacionpractica.utils.Validador;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;


public class ClienteController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtCorreo;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnActualizar;
    @FXML private Button btnEliminar;

    @FXML private TextField txtBuscar;
    @FXML private Label lblTotal;
    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombres;
    @FXML private TableColumn<Cliente, String> colApellidos;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, String> colActivo;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Cliente> clientes = FXCollections.observableArrayList();
    private Cliente clienteSeleccionado;

    @FXML
    public void initialize() {
        configurarColumnas();
        tblClientes.setItems(clientes);


        restringir(txtNombres, 80, null);
        restringir(txtApellidos, 80, null);
        restringir(txtTelefono, 8, "[0-9]*");
        restringir(txtCorreo, 120, null);


        tblClientes.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nuevo) -> {
            clienteSeleccionado = nuevo;
            if (nuevo != null) {
                cargarFormulario(nuevo);
            }
        });

        var sinSeleccion = tblClientes.getSelectionModel().selectedItemProperty().isNull();
        btnGuardar.disableProperty().bind(sinSeleccion.not());
        btnActualizar.disableProperty().bind(sinSeleccion);
        btnEliminar.disableProperty().bind(sinSeleccion);

        limpiarFormulario();
        recargarTabla();
    }

    @FXML
    private void onGuardar() {
        if (!validarFormulario()) {
            return;
        }
        Cliente nuevo = leerFormulario();
        try {
            if (clienteDAO.existeCorreo(nuevo.getCorreo())) {
                invalido("Ya existe un cliente registrado con ese correo electrónico.", txtCorreo);
                return;
            }
            if (clienteDAO.insertar(nuevo)) {
                AlertUtil.mostrarInfo("Cliente registrado", "El cliente se registró correctamente.");
                limpiarFormulario();
                recargarTabla();
            } else {
                AlertUtil.mostrarError("Error", "No se pudo registrar el cliente.");
            }
        } catch (SQLException e) {
            manejarErrorSQL(e, "registrar");
        }
    }

    @FXML
    private void onActualizar() {
        if (clienteSeleccionado == null) {
            AlertUtil.mostrarAdvertencia("Sin selección", "Seleccione un cliente de la tabla para actualizarlo.");
            return;
        }
        if (!validarFormulario()) {
            return;
        }
        Cliente editado = leerFormulario();
        try {
            if (clienteDAO.existeCorreoEnOtro(editado.getCorreo(), editado.getIdCliente())) {
                invalido("Otro cliente ya utiliza ese correo electrónico.", txtCorreo);
                return;
            }
            if (clienteDAO.actualizar(editado)) {
                AlertUtil.mostrarInfo("Cliente actualizado", "Los datos del cliente se actualizaron correctamente.");
                limpiarFormulario();
                recargarTabla();
            } else {
                AlertUtil.mostrarError("Error", "No se pudo actualizar el cliente.");
            }
        } catch (SQLException e) {
            manejarErrorSQL(e, "actualizar");
        }
    }

    @FXML
    private void onEliminar() {
        if (clienteSeleccionado == null) {
            AlertUtil.mostrarAdvertencia("Sin selección", "Seleccione un cliente de la tabla para eliminarlo.");
            return;
        }
        Integer id = clienteSeleccionado.getIdCliente();
        try {
            if (clienteDAO.tieneCitasAsociadas(id)) {
                AlertUtil.mostrarAdvertencia("Eliminación no permitida",
                        "No se puede eliminar el cliente porque tiene citas técnicas asociadas.\n"
                                + "Elimine primero sus citas y vuelva a intentarlo.");
                return;
            }
            if (!AlertUtil.mostrarConfirmacion("Confirmar eliminación",
                    "¿Desea eliminar definitivamente al cliente " + clienteSeleccionado.getNombreCompleto() + "?")) {
                return;
            }
            if (clienteDAO.eliminar(id)) {
                AlertUtil.mostrarInfo("Cliente eliminado", "El cliente se eliminó correctamente.");
                limpiarFormulario();
                recargarTabla();
            } else {
                AlertUtil.mostrarError("Error", "No se pudo eliminar el cliente.");
            }
        } catch (SQLException e) {
            manejarErrorSQL(e, "eliminar");
        }
    }

    @FXML
    private void onLimpiar() {
        limpiarFormulario();
        txtNombres.requestFocus();
    }

    @FXML
    private void onBuscar() {
        recargarTabla();
    }

    @FXML
    private void onLimpiarBusqueda() {
        txtBuscar.clear();
        recargarTabla();
    }


    private void recargarTabla() {
        Integer idSeleccionado = clienteSeleccionado != null ? clienteSeleccionado.getIdCliente() : null;
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim();
        try {
            List<Cliente> lista = texto.isEmpty() ? clienteDAO.listarTodos() : clienteDAO.buscar(texto);
            clientes.setAll(lista);
            lblTotal.setText("Total de registros: " + lista.size());

            if (idSeleccionado != null) {
                clientes.stream()
                        .filter(c -> Objects.equals(idSeleccionado, c.getIdCliente()))
                        .findFirst()
                        .ifPresent(c -> tblClientes.getSelectionModel().select(c));
            }
        } catch (SQLException e) {
            AlertUtil.mostrarError("Error de base de datos",
                    "No se pudieron cargar los clientes.\nDetalle: " + e.getMessage());
        }
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getIdCliente()).asObject());
        colNombres.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombres()));
        colApellidos.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getApellidos()));
        colTelefono.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTelefono()));
        colCorreo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCorreo()));
        colActivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActivo() ? "Sí" : "No"));
    }


    private void cargarFormulario(Cliente c) {
        txtNombres.setText(c.getNombres());
        txtApellidos.setText(c.getApellidos());
        txtTelefono.setText(c.getTelefono());
        txtCorreo.setText(c.getCorreo());
        chkActivo.setSelected(c.isActivo());
    }

    private Cliente leerFormulario() {
        return Cliente.builder()
                .idCliente(clienteSeleccionado != null ? clienteSeleccionado.getIdCliente() : null)
                .nombres(txtNombres.getText().trim())
                .apellidos(txtApellidos.getText().trim())
                .telefono(txtTelefono.getText().trim())
                .correo(txtCorreo.getText().trim().toLowerCase(Locale.ROOT))
                .activo(chkActivo.isSelected())
                .build();
    }

    private void limpiarFormulario() {
        tblClientes.getSelectionModel().clearSelection();
        clienteSeleccionado = null;
        txtNombres.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        txtCorreo.clear();
        chkActivo.setSelected(true);
    }


    private boolean validarFormulario() {
        if (Validador.esVacio(txtNombres.getText())) {
            return invalido("Los nombres son obligatorios.", txtNombres);
        }
        if (Validador.esVacio(txtApellidos.getText())) {
            return invalido("Los apellidos son obligatorios.", txtApellidos);
        }
        if (Validador.esVacio(txtTelefono.getText())) {
            return invalido("El teléfono es obligatorio.", txtTelefono);
        }
        if (!Validador.esTelefonoValido(txtTelefono.getText().trim())) {
            return invalido("El teléfono debe tener exactamente 8 dígitos.", txtTelefono);
        }
        if (Validador.esVacio(txtCorreo.getText())) {
            return invalido("El correo electrónico es obligatorio.", txtCorreo);
        }
        if (!Validador.esEmailValido(txtCorreo.getText().trim())) {
            return invalido("El correo electrónico no tiene un formato válido (ejemplo: nombre@dominio.com).", txtCorreo);
        }
        return true;
    }

    private boolean invalido(String mensaje, Control campo) {
        AlertUtil.mostrarAdvertencia("Datos inválidos", mensaje);
        campo.requestFocus();
        return false;
    }


    private void manejarErrorSQL(SQLException e, String accion) {
        String estado = e.getSQLState();
        if ("23505".equals(estado)) {
            AlertUtil.mostrarError("Correo duplicado",
                    "Ya existe un cliente registrado con ese correo electrónico.");
        } else if ("23503".equals(estado)) {
            AlertUtil.mostrarAdvertencia("Eliminación no permitida",
                    "No se puede eliminar el cliente porque tiene citas técnicas asociadas.");
        } else {
            AlertUtil.mostrarError("Error de base de datos",
                    "No se pudo " + accion + " el cliente.\nDetalle: " + e.getMessage());
        }
    }


    private void restringir(TextField campo, int maxLongitud, String regexPermitido) {
        UnaryOperator<TextFormatter.Change> filtro = cambio -> {
            String nuevo = cambio.getControlNewText();
            if (nuevo.length() > maxLongitud) {
                return null;
            }
            if (regexPermitido != null && !nuevo.matches(regexPermitido)) {
                return null;
            }
            return cambio;
        };
        campo.setTextFormatter(new TextFormatter<>(filtro));
    }
}
