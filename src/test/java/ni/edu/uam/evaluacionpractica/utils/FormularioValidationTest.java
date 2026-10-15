package ni.edu.uam.evaluacionpractica.utils;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.fxml.FXMLLoader;
import ni.edu.uam.evaluacionpractica.modelocrud.Controllers.ClienteController;
import ni.edu.uam.evaluacionpractica.modelocrud.Controllers.CitaTecnicaController;
import ni.edu.uam.evaluacionpractica.modelocrud.model.CitaTecnica;
import ni.edu.uam.evaluacionpractica.modelocrud.HelloApplication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "CITAS_TEST_DB", matches = "true")
class FormularioValidationTest {
    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        assertTrue(System.getenv("DB_URL").matches(
                "jdbc:postgresql://localhost:5432/gestion_citas_auditoria_[a-z0-9]+"));
        CountDownLatch inicio = new CountDownLatch(1);
        Platform.startup(inicio::countDown);
        assertTrue(inicio.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void cerrar() {
        Platform.exit();
        ConexionBD.cerrar();
    }

    private void enJavaFx(Callable<Void> tarea) throws Exception {
        FutureTask<Void> resultado = new FutureTask<>(tarea);
        Platform.runLater(resultado);
        resultado.get(15, TimeUnit.SECONDS);
    }

    @Test
    void cargaTodasLasVistasYSusControllers() throws Exception {
        enJavaFx(() -> {
            assertNotNull(new FXMLLoader(HelloApplication.class.getResource("Views/MainView.fxml")).load());
            return null;
        });
    }

    @Test
    void formularioRechazaFechaPasadaFechaImposibleYHoraInvalida() throws Exception {
        enJavaFx(() -> {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("Views/CitaTecnicaView.fxml"));
            javafx.scene.Parent raiz = loader.load();
            new javafx.scene.Scene(raiz);
            raiz.applyCss();
            raiz.layout();
            CitaTecnicaController controller = loader.getController();
            var campos = loader.getNamespace();
            ((ComboBox<?>) campos.get("cmbCliente")).getSelectionModel().selectFirst();
            DatePicker fecha = (DatePicker) campos.get("dpFecha");
            TextField hora = (TextField) campos.get("txtHora");
            ((TextField) campos.get("txtTipoServicio")).setText("Diagnóstico");
            ((TextArea) campos.get("txtDescripcion")).setText("Revisión");
            hora.setText("12:00");
            var leer = CitaTecnicaController.class.getDeclaredMethod("leerFormulario");
            leer.setAccessible(true);
            fecha.setValue(java.time.LocalDate.now().minusDays(1));
            var pasado = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> leer.invoke(controller));
            assertInstanceOf(IllegalArgumentException.class, pasado.getCause());
            assertTrue(pasado.getCause().getMessage().contains("pasada"), pasado.getCause().getMessage());
            fecha.setValue(java.time.LocalDate.now().plusDays(1));
            assertInstanceOf(CitaTecnica.class, leer.invoke(controller));
            fecha.getEditor().setText("31/02/2026");
            var imposible = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> leer.invoke(controller));
            assertTrue(imposible.getCause().getMessage().contains("dd/MM/aaaa"));
            fecha.getEditor().setText(java.time.LocalDate.now().plusDays(1)
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu")));
            hora.setText("24:00");
            var invalida = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> leer.invoke(controller));
            assertTrue(invalida.getCause().getMessage().contains("HH:mm"));
            return null;
        });
    }

    @Test
    void documentaTelefonoAnteriorConservadoAlCargarDatoConGuion() throws Exception {
        enJavaFx(() -> {
            TextField campo = new TextField();
            var restringir = ClienteController.class.getDeclaredMethod(
                    "restringir", TextField.class, int.class, String.class);
            restringir.setAccessible(true);
            restringir.invoke(new ClienteController(), campo, 8, "[0-9]*");
            campo.setText("12345678");
            campo.setText("8888-1111");
            // Caracteriza el defecto pendiente: el filtro conserva el teléfono anterior.
            assertEquals("12345678", campo.getText());
            return null;
        });
    }
}
