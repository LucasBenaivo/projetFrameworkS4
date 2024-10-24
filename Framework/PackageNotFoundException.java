package exception;

public class PackageNotFoundException extends Exception {
    public PackageNotFoundException() {
        super();
    }

    public PackageNotFoundException(String message) {
        super(message);
    }

    public PackageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PackageNotFoundException(Throwable cause) {
        super(cause);
    }
}