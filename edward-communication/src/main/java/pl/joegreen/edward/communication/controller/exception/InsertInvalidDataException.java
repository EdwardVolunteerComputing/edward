package pl.joegreen.edward.communication.controller.exception;

public class InsertInvalidDataException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InsertInvalidDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public InsertInvalidDataException(String message) {
		super(message);
	}

}
