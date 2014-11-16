package pl.joegreen.edward.persistence.dao;

import pl.joegreen.edward.core.model.IdentifierProvider;

public class ObjectDoesntExistException extends EdwardPersistenceException {

	private static final long serialVersionUID = 1L;

	public Long id;
	public Class<? extends IdentifierProvider> clazz;

	public ObjectDoesntExistException(Long id,
			Class<? extends IdentifierProvider> clazz) {
		super("Object of class " + clazz + " with id " + id
				+ " does not exist.");
		this.id = id;
		this.clazz = clazz;

	}
}
