package pl.joegreen.edward.rest.client;
public class RestException extends Exception {

	public RestException(String arg0) {
		super(arg0);
	}

	public RestException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public RestException(Throwable cause) {
		super(cause);
	}

}
