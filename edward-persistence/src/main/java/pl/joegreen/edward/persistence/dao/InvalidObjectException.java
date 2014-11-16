package pl.joegreen.edward.persistence.dao;

public class InvalidObjectException extends EdwardPersistenceException {

	public InvalidObjectException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidObjectException(String message) {
		super(message);
	}

	public InvalidObjectException(Throwable cause) {
		super(cause);
	}

}
