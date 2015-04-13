package field.app;

import field.graphics.Scene;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by marc on 3/25/14.
 */
public class RunLoop {

	static public final RunLoop main = new RunLoop();
	static public long tick = 0;

	static public final ReentrantLock lock = new ReentrantLock(true);

	public Scene mainLoop = new Scene();
	Thread mainThread = null;
	static public final ExecutorService workerPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2);

	protected final Thread shutdownThread;

	protected RunLoop() {
		Runtime.getRuntime().addShutdownHook(shutdownThread = new Thread(() -> exit()));
	}

	public Scene getLoop() {
		return mainLoop;
	}

	public boolean isMainThread() {
		return Thread.currentThread() == mainThread;
	}

	public Set<Object> shouldSleep = Collections.synchronizedSet(new LinkedHashSet<>());

	public void enterMainLoop() {
		mainThread = Thread.currentThread();
		while (true) {
			try {
				tick++;

				if (lock.tryLock(1, TimeUnit.DAYS)) {
					mainLoop.updateAll();
					ThreadSync.get().serviceAndCull();
				} else {
				}
				if (shouldSleep.size()==0)
					Thread.sleep(5);
			} catch (Throwable t) {
				System.err.println(" exception thrown in main loop");
				t.printStackTrace();
			} finally {
				RunLoop.lock.unlock();
			}

		}
	}

	public void once(Runnable r) {
		mainLoop.attach(i -> {
			r.run();
			return false;
		});
	}

	public void nTimes(Runnable p0, int n) {
		mainLoop.attach(new Scene.Perform() {
			int t = 0;

			@Override
			public boolean perform(int pass) {
				p0.run();
				return t++ < n;
			}
		});
	}

	public void delay(Runnable p0, int ms) {
		long now = System.currentTimeMillis();
		mainLoop.attach(pass -> {
			if (System.currentTimeMillis() - now > ms) {
				p0.run();
				return false;
			}
			return true;
		});
	}


	List<Runnable> onExit = new LinkedList<>();
	AtomicBoolean exitStarted = new AtomicBoolean(false);

	public void exit() {
		try {
			if (exitStarted.compareAndSet(false, true)) {
				for (Runnable r : onExit) {
					try {
						r.run();
					} catch (Throwable t) {
						System.err.println(" exception thrown during exit (will continue on regardless)");
						t.printStackTrace();
					}
				}
				if (Thread.currentThread() != shutdownThread) System.exit(0);
			}
		} catch (Throwable t) {
			System.err.println(" unexpected exception thrown during exit ");
			t.printStackTrace();
		}
	}

	/**
	 * adds a Runnable to be executed on exit. This will run before anything else that's been added.
	 */
	public void onExit(Runnable r) {
		// we add this to the start of the list, it will be run before anything that's already there.
		onExit.add(0, r);
	}

}