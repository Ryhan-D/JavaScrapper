package eus.aaronduque.panelempresas.compartido;

import eus.aaronduque.panelempresas.compartido.excepciones.ErrorRespuestaDto;
import eus.aaronduque.panelempresas.compartido.excepciones.ErrorRespuestaDto.ErrorCampo;
import eus.aaronduque.panelempresas.compartido.excepciones.RecursoNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * Captura excepciones lanzadas en cualquier controller y las transforma
 */
@RestControllerAdvice
@Slf4j
public class ManejadorErroresGlobal {

    /**
     * Devuelve 400 con la lista de campos problemáticos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuestaDto> manejarErroresValidacion(
            MethodArgumentNotValidException ex) {

        List<ErrorCampo> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorCampo.builder()
                        .campo(error.getField())
                        .mensaje(error.getDefaultMessage())
                        .build())
                .toList();

        ErrorRespuestaDto respuesta = ErrorRespuestaDto.builder()
                .estado(HttpStatus.BAD_REQUEST.value())
                .mensaje("Datos de entrada inválidos")
                .errores(errores)
                .build();

        return ResponseEntity.badRequest().body(respuesta);
    }

    /**
     * Errores de negocio: validaciones de dominio que lanzamos a mano.
     * Ejemplo: CIF duplicado al crear una empresa.
     * Devuelve 409 Conflict.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRespuestaDto> manejarErrorNegocio(IllegalArgumentException ex) {
        log.warn("Error de negocio: {}", ex.getMessage());

        ErrorRespuestaDto respuesta = ErrorRespuestaDto.builder()
                .estado(HttpStatus.CONFLICT.value())
                .mensaje(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    /**
     * Archivo subido demasiado grande (CSV gigantes).
     * Devuelve 413 Payload Too Large.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorRespuestaDto> manejarArchivoGrande(MaxUploadSizeExceededException ex) {
        ErrorRespuestaDto respuesta = ErrorRespuestaDto.builder()
                .estado(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .mensaje("El archivo es demasiado grande")
                .build();

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(respuesta);
    }

    /**
     * Cualquier otra excepción no controlada.
     * Devuelve 500 con mensaje generico.
     * El error completo lo registramos en logs para debug.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuestaDto> manejarErrorGenerico(Exception ex) {
        log.error("Error no controlado", ex);

        ErrorRespuestaDto respuesta = ErrorRespuestaDto.builder()
                .estado(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .mensaje("Ha ocurrido un error inesperado. Inténtalo más tarde.")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }

    /**
     * Devuelve 404 Not Found.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuestaDto> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());

        ErrorRespuestaDto respuesta = ErrorRespuestaDto.builder()
                .estado(HttpStatus.NOT_FOUND.value())
                .mensaje(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

}