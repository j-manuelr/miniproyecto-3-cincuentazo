package np.cincuentazo.exception;


public class EmptyDeckException extends RuntimeException {


    public EmptyDeckException(String message) {
        super(message);
    }

    public EmptyDeckException(String message, Throwable cause) {
        super(message, cause);
    }
}