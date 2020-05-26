package bfroehlich.minesweeper;

public class Timer {

	private long startTime;
	private long pauseTime;
	
	public Timer() {
		
	}
	
	public void start() {
		startTime = System.currentTimeMillis();
	}
	
	public void pause() {
		pauseTime = System.currentTimeMillis();
	}
	
	public void resume() {
		if(pauseTime > 0 && startTime > 0) {
			long pauseElapsed = System.currentTimeMillis() - pauseTime;
			startTime += pauseElapsed;
			pauseTime = 0;
		}
	}
	
	public long read() {
		if(startTime == 0) {
			return 0;
		}
		return System.currentTimeMillis() - startTime;
	}
}