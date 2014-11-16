package pl.joegreen.edward.communication.controller.exception;

public class UpdateNonExistingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UpdateNonExistingException(String message, Throwable cause) {
		super(message, cause);
	}

	public UpdateNonExistingException(String message) {
		super(message);
	}

}
