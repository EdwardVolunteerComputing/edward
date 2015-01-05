package pl.joegreen.edward.charles.configuration.model;

public class Utils {
	public static boolean anyNull(Object... objects) {
		for (Object object : objects) {
			if (object == null) {
				return true;
			}
		}
		return false;
	}

	public static boolean noNulls(Object... objects) {
		return !anyNull(objects);
	}
}
