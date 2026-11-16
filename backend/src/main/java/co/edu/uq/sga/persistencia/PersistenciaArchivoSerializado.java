package co.edu.uq.sga.persistencia;

import co.edu.uq.sga.excepciones.SgaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Guarda el estado completo en un archivo binario serializado. La escritura es atómica:
 * primero un archivo temporal y luego se mueve sobre el definitivo.
 */
@Component
public class PersistenciaArchivoSerializado implements PersistenciaEstado {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenciaArchivoSerializado.class);

    private final Path archivo;

    public PersistenciaArchivoSerializado(@Value("${sga.datos.archivo:./datos/sga.dat}") String ruta) {
        this.archivo = Path.of(ruta).toAbsolutePath();
    }

    @Override
    public EstadoSistema cargar() {
        if (!Files.exists(archivo)) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(archivo)))) {
            return (EstadoSistema) in.readObject();
        } catch (InvalidClassException e) {
            LOG.warn("El archivo de datos {} es de una versión incompatible; se ignorará", archivo);
            return null;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new SgaException("No se pudo leer el archivo de datos: " + e.getMessage());
        }
    }

    @Override
    public void guardar(EstadoSistema estado) {
        try {
            Files.createDirectories(archivo.getParent());
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(temporal)))) {
                out.writeObject(estado);
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicoNoSoportado) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new SgaException("No se pudo guardar el archivo de datos: " + e.getMessage());
        }
    }

    public Path getArchivo() {
        return archivo;
    }
}
