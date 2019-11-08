package utility;

import java.io.*;
import java.util.*;

import org.mentaqueue.AtomicQueue;
import org.mentaqueue.BatchingQueue;
import org.mentaqueue.util.Builder;
import org.mentaqueue.wait.ParkWaitStrategy;
import org.mentaqueue.wait.WaitStrategy;

public class AsynLoggedVector<E> extends Vector<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static int BLOCKSIZE = 100000;
	private int queueSize = 2;
	private String statFileName;
	private Vector<E> blockData;

	private Thread consumerThread;
	private WaitStrategy waitStrategy;
	final BatchingQueue<Vector<E>> queue = new AtomicQueue<Vector<E>>(
			queueSize, new Builder<Vector<E>>() {
				@Override
				public Vector<E> newInstance() {
					// get a vector with size BLOCKSIZE
					return new Vector<E>(BLOCKSIZE);
				}
			});

	public AsynLoggedVector(String fileName) {
		this.statFileName = fileName;
		this.waitStrategy = new ParkWaitStrategy();
		// initialize the consumer thread
		consumerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					long avail;
					while ((avail = queue.availableToPoll()) == 0) {
						waitStrategy.waitForOtherThread();
					}

					waitStrategy.reset();
					File statFile = new File(statFileName);

					if (statFile.exists()) {
						// delete the old stats file
						statFile.delete();
					}

					for (int i = 0; i < avail; i++) {
						// write the vector pair to file
						Vector<E> block = queue.poll();

						try {

							FileWriter fw = new FileWriter(statFile, true);
							BufferedWriter bw = new BufferedWriter(fw);
							// bw.write(String.format("%-15s", "time")
							// + String.format("%-15s", "queuesize"));
							// bw.newLine();

							// write history to file
							for (E e : block) {
								String line = e.toString();

								bw.write(line);
								bw.newLine();
							}
							bw.newLine();
							bw.close();

						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}
			}
		});

		consumerThread.start();
	}

	private Vector<E> getBlockToWrite() {
		blockData = null;
		while ((blockData = queue.nextToDispatch()) == null) {
			// the queue is already full, we have to wait

		}
		return blockData;
	}

	public boolean add(E element) {
		if (blockData == null) {
			blockData = getBlockToWrite();
		}

		if (blockData.size() < BLOCKSIZE) {
			return blockData.add(element);
		}

		else {
			// flush the old data
			queue.flush(true);

			blockData = getBlockToWrite();
			return blockData.add(element);

		}

	}

	public void finalFlush() {
		if (blockData != null) {
			queue.flush();
		}
	}

	public void joinThread() {
		try {
			consumerThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public E lastElement(){
		return blockData.lastElement();
	}

}
