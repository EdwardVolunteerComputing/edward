package pl.joegreen.edward.charles.configuration.model;

import java.util.List;
import java.util.Map;

public class PhaseConfiguration {
	public List<String> codeFiles;
	public boolean useVolunteerComputing;
	public Map<Object, Object> parameters;

	@Override
	public String toString() {
		return "PhaseConfiguration [codePath=" + codeFiles
				+ ", useVolunteerComputing=" + useVolunteerComputing
				+ ", parameters=" + parameters + "]";
	}

	public boolean isValid() {
		return Utils.noNulls(codeFiles, useVolunteerComputing, parameters)
				&& !codeFiles.isEmpty();
	}
}
