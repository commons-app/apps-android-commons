package fr.free.nrw.commons.data;

public class DAOException extends RuntimeException {

    public DAOException(String message) {
        super(message);
    }

    public DAOException(Throwable cause) {
        super(cause);
    }
}
