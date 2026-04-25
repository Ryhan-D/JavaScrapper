package eus.aaronduque.panelempresas.compartido.excepciones;

/**
 * El manejador global la traduce a HTTP 404 Not Found.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}