package ni.edu.uam.evaluacionpractica.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;

public final class AlertUtil {

    private AlertUtil() {
    }

    public static void mostrarInfo(String titulo, String mensaje) {
        crearAlerta(Alert.AlertType.INFORMATION, titulo, mensaje).showAndWait();
    }

    public static void mostrarAdvertencia(String titulo, String mensaje) {
        crearAlerta(Alert.AlertType.WARNING, titulo, mensaje).showAndWait();
    }

    public static void mostrarError(String titulo, String mensaje) {
        crearAlerta(Alert.AlertType.ERROR, titulo, mensaje).showAndWait();
    }

    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        Alert alert = crearAlerta(Alert.AlertType.CONFIRMATION, titulo, mensaje);
        ButtonType confirmar = new ButtonType("Confirmar", ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmar, cancelar);
        return alert.showAndWait().filter(confirmar::equals).isPresent();
    }

    private static Alert crearAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.setResizable(true);
        return alert;
    }
}

