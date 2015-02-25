package pl.joegreen.edward.core.model;

public class Task extends IdentifierProvider {

	private Long jobId;
	private Long inputDataId;
	private long priority = 0;
	private long concurrentExecutionsCount = 1;
	private Long creationTime = System.currentTimeMillis();
	private boolean isAborted;

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public Long getInputDataId() {
		return inputDataId;
	}

	public void setInputDataId(Long inputDataId) {
		this.inputDataId = inputDataId;
	}

	public boolean isAborted() {
		return isAborted;
	}

	public void setAborted(boolean isAborted) {
		this.isAborted = isAborted;
	}

	public long getPriority() {
		return priority;
	}

	public void setPriority(long priority) {
		this.priority = priority;
	}

	public long getConcurrentExecutionsCount() {
		return concurrentExecutionsCount;
	}

	public void setConcurrentExecutionsCount(long concurrentExecutionsCount) {
		this.concurrentExecutionsCount = concurrentExecutionsCount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((inputDataId == null) ? 0 : inputDataId.hashCode());
		result = prime * result + (isAborted ? 1231 : 1237);
		result = prime * result + ((jobId == null) ? 0 : jobId.hashCode());
		result = prime
				* result
				+ (int) (concurrentExecutionsCount ^ (concurrentExecutionsCount >>> 32));
		result = prime * result + (int) (priority ^ (priority >>> 32));
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
		Task other = (Task) obj;
		if (inputDataId == null) {
			if (other.inputDataId != null)
				return false;
		} else if (!inputDataId.equals(other.inputDataId))
			return false;
		if (isAborted != other.isAborted)
			return false;
		if (jobId == null) {
			if (other.jobId != null)
				return false;
		} else if (!jobId.equals(other.jobId))
			return false;
		if (concurrentExecutionsCount != other.concurrentExecutionsCount)
			return false;
		if (priority != other.priority)
			return false;
		return true;
	}

	public Long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Long creationTime) {
		this.creationTime = creationTime;
	}

}