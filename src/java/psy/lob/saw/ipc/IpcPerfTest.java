package psy.lob.saw.ipc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.IllegalSelectorException;
import java.util.Queue;

import psy.lob.saw.queues.P1C1OffHeapQueue;
import psy.lob.saw.queues.UnsafeAccess;
import psy.lob.saw.queues.UnsafeDirectByteBuffer;


/**
 * @author Nitsan
 *
 */
public class IpcPerfTest {
	public static final int QUEUE_CAPACITY = 32 * 1024;
	public static final int REPETITIONS = 500 * 1024 * 1024;
	public static final Integer TEST_VALUE = Integer.valueOf(777);
	public static ByteBuffer syncArea;
	public static void main(final String[] args) throws Exception {
		byte type= P1C1OffHeapQueue.CONSUMER;
		if (args.length > 1
		        || (args.length == 1 && !args[0].equalsIgnoreCase("p") && !args[0]
		                .equalsIgnoreCase("c"))) {
			throw new IllegalArgumentException(
			        "Expecting 0 or 1 arguments: p for producer, c for consumer");
		}
		else if(args.length == 1 && args[0].equalsIgnoreCase("p")){
			type= P1C1OffHeapQueue.PRODUCER;
		}
		MappedByteBuffer ipcBuffer;
		if(type != P1C1OffHeapQueue.CONSUMER){
			ipcBuffer = createAndLoadMappedFile();
		} else {
			ipcBuffer = waitForFileCreateMappedBuffer();
		}
		final Queue<Integer> queue = new P1C1OffHeapQueue(
		        ipcBuffer, QUEUE_CAPACITY, type);
		for (int i = 0; i < 20; i++) {
			System.gc();
			if(type == P1C1OffHeapQueue.CONSUMER){
				consumerRun(i, queue);
			}
			else {
				producerRun(i, queue);
			}
		}
	}

	private static MappedByteBuffer createAndLoadMappedFile()
	        throws FileNotFoundException, IOException {
		File ipcFile = new File("queue.ipc");
		if (ipcFile.exists()) {
			ipcFile.delete();
		}
		ipcFile.deleteOnExit();
		FileChannel channel = new RandomAccessFile(ipcFile, "rw").getChannel();
		int minimumBuffSize = QUEUE_CAPACITY * 4 + 64 * 6;
		if (channel.size() < minimumBuffSize) {
			ByteBuffer temp = ByteBuffer.allocateDirect(64 * 1024);
			while (channel.size() < minimumBuffSize) {
				channel.write(temp);
				temp.clear();
			}
		}
		MappedByteBuffer ipcBuffer = channel.map(MapMode.READ_WRITE, 64,
		        minimumBuffSize-64);
		syncArea = channel.map(MapMode.READ_WRITE, 0, 64);
		syncArea.putInt(-1);
		ipcBuffer.load();
		return ipcBuffer;
	}

	private static MappedByteBuffer waitForFileCreateMappedBuffer()
	        throws InterruptedException, FileNotFoundException, IOException {
		File ipcFile = new File("queue.ipc");
		while (!ipcFile.exists()) {
			Thread.sleep(1000);
		}

		FileChannel channel = new RandomAccessFile(ipcFile, "rw").getChannel();
		int minimumBuffSize = QUEUE_CAPACITY * 4 + 64 * 6;
		while (channel.size() < minimumBuffSize) {
			Thread.sleep(1000);
		}
		MappedByteBuffer ipcBuffer = channel.map(MapMode.READ_WRITE, 64,
		        minimumBuffSize-64);
		ipcBuffer.load();
		syncArea = channel.map(MapMode.READ_WRITE, 0, 64);
		return ipcBuffer;
	}
	private static void producerRun(final int runNumber,
	        final Queue<Integer> queue) throws Exception {
		long syncAddress = UnsafeDirectByteBuffer.getAddress(syncArea);
		while(!UnsafeAccess.unsafe.compareAndSwapInt(null, syncAddress, 3*runNumber, 3*runNumber))
			Thread.yield();
		final long start = System.nanoTime();
		int i = REPETITIONS;
		do {
			while (!queue.offer(i & 31)) {
//				Thread.yield();
			}
		} while (0 != --i);
		while(!UnsafeAccess.unsafe.compareAndSwapInt(null, syncAddress, 3*runNumber+1, 3*runNumber+2))
			Thread.yield();
		final long duration = System.nanoTime() - start;
		final long ops = (REPETITIONS * 1000L * 1000L * 1000L) / duration;
		System.out.format("%d - ops/sec=%,d - %s result=%d\n", Integer
		        .valueOf(runNumber), Long.valueOf(ops), queue.getClass()
		        .getSimpleName(), queue.size());
	}

	private static void consumerRun(final int runNumber,
	        final Queue<Integer> queue) throws Exception {
		long syncAddress = UnsafeDirectByteBuffer.getAddress(syncArea);
		while(!UnsafeAccess.unsafe.compareAndSwapInt(null, syncAddress, 3*runNumber-1, 3*runNumber))
			Thread.yield();
		Integer result;
		int i = REPETITIONS;
		do {
			while (null == (result = queue.poll())) {

//				Thread.yield();
			}
			if(result != (i & 31)){
				throw new IllegalStateException();
			}
		} while (0 != --i);
		while(!UnsafeAccess.unsafe.compareAndSwapInt(null, syncAddress, 3*runNumber, 3*runNumber+1))
			Thread.yield();
	}

}
