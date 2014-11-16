package pl.joegreen.edward.communication.controller.exception;

public class DeleteNonExistingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public DeleteNonExistingException(String message, Throwable cause) {
		super(message, cause);
	}

	public DeleteNonExistingException(String message) {
		super(message);
	}

}
