/*
 * Copyright 2012 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.ipc;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Queue;

import uk.co.real_logic.queues.P1C1Queue2CacheLinesHeapBuffer;
import uk.co.real_logic.queues.P1C1Queue4CacheLinesHeapBuffer;
import uk.co.real_logic.queues.P1C1Queue4CacheLinesHeapBufferUnsafe;
import uk.co.real_logic.queues.P1C1Queue4CacheLinesOffHeapBuffer;
import uk.co.real_logic.queues.P1C1QueueOriginal;
import uk.co.real_logic.queues.P1C1QueueOriginalPrimitive;

public class IpcPerfTestProducer {
	public static final int QUEUE_CAPACITY = 1024 * 1024;
	public static final int REPETITIONS = 500 * 1024 * 1024;
	public static final Integer TEST_VALUE = Integer.valueOf(777);

	public static void main(final String[] args) throws Exception {
		FileChannel channel = new RandomAccessFile("queue.ipc", "rw").getChannel();
		int minimumBuffSize = QUEUE_CAPACITY*4 + 64*5;
		if(channel.size()< minimumBuffSize){
			ByteBuffer temp = ByteBuffer.allocateDirect(64*1024);
			while(channel.size()< minimumBuffSize){
				channel.write(temp);
				temp.clear();
			}
		}
		MappedByteBuffer ipcBuffer = channel.map(MapMode.READ_WRITE, 0, minimumBuffSize);
		ipcBuffer.load();
		final Queue<Integer> queue = new P1C1Queue4CacheLinesOffHeapBuffer(ipcBuffer, QUEUE_CAPACITY);

		for (int i = 0; i < 20; i++) {
			System.gc();
			performanceRun(i, queue);
			System.out.println(i);
		}
	}

	private static void performanceRun(final int runNumber,
	        final Queue<Integer> queue) throws Exception {
		int i = REPETITIONS;
		do {
			while (!queue.offer(TEST_VALUE)) {
				Thread.yield();
			}
//			System.out.println(i);
		} while (0 != --i);

	}
}
