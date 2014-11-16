package pl.joegreen.edward.core.model;

public class JsonData extends IdentifierProvider {

	private String data;

	public JsonData(String data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "JsonData [data=" + data + "]";
	}

	@SuppressWarnings("unused")
	/* for Jackson */
	private JsonData() {
	}

	public String getData() {
		return data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		JsonData other = (JsonData) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}
}
