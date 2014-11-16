package pl.joegreen.edward.persistence.dao;

public abstract class EdwardPersistenceException extends Exception {

	public EdwardPersistenceException() {
		super();
	}

	public EdwardPersistenceException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public EdwardPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	public EdwardPersistenceException(String message) {
		super(message);
	}

	public EdwardPersistenceException(Throwable cause) {
		super(cause);
	}

}
