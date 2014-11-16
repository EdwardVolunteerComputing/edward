package pl.joegreen.edward.management.service;

public class TimeoutControllingThread extends Thread {

	public static final long SLEEP_TIME = 2000;

	private final TimeoutControllingService service;

	public TimeoutControllingThread(TimeoutControllingService service) {
		this.service = service;
	}

	@Override
	public void run() {
		while (!service.isInterrupted()) {
			try {
				service.checkTimeouts();
				sleep(SLEEP_TIME);
			} catch (InterruptedException e) {
				service.stopThread();
			}

		}
	}

}
