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
package psy.lob.saw.queues;

import static psy.lob.saw.queues.UnsafeDirectByteBuffer.CACHE_LINE_SIZE;
import static psy.lob.saw.queues.UnsafeDirectByteBuffer.alignedSlice;
import static psy.lob.saw.queues.UnsafeDirectByteBuffer.allocateAlignedByteBuffer;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public final class P1C1OffHeapQueue implements Queue<Integer> {
	public final static byte PRODUCER = 1;
	public final static byte CONSUMER = 2;
	// 24b,8b,32b | 24b,8b,32b | 24b,8b,32b | 24b,8b,32b
	private final ByteBuffer buffy;
	private final long headAddress;
	private final long tailCacheAddress;
	private final long tailAddress;
	private final long headCacheAddress;

	private final int capacity;
	private final int mask;
	private final long arrayBase;
	private static final int arrayElementScale = 2;

	public P1C1OffHeapQueue(final int capacity) {
		this(allocateAlignedByteBuffer(
		        4 * CACHE_LINE_SIZE + findNextPositivePowerOfTwo(capacity)<<arrayElementScale, CACHE_LINE_SIZE),
		        findNextPositivePowerOfTwo(capacity),(byte)(PRODUCER | CONSUMER));
	}
	/**
	 * This is to be used for an IPC queue with the direct buffer used being a memory
	 * mapped file.
	 * 
	 * @param buff
	 * @param capacity
	 * @param viewMask 
	 */
	public P1C1OffHeapQueue(final ByteBuffer buff, 
			final int capacity, byte viewMask) {
		this.capacity = findNextPositivePowerOfTwo(capacity);
		buffy = alignedSlice(4 * CACHE_LINE_SIZE + this.capacity<<arrayElementScale, 
									CACHE_LINE_SIZE, buff);

		long alignedAddress = UnsafeDirectByteBuffer.getAddress(buffy);

		headAddress = alignedAddress + (CACHE_LINE_SIZE / 2 - 8);
		tailCacheAddress = headAddress + CACHE_LINE_SIZE;
		tailAddress = tailCacheAddress + CACHE_LINE_SIZE;
		headCacheAddress = tailAddress + CACHE_LINE_SIZE;
		arrayBase = alignedAddress + 4* CACHE_LINE_SIZE;
		// producer owns tail and headCache
		if((viewMask & PRODUCER) == PRODUCER){
    		setHeadCache(0);
    		setTail(0);
		}
		// consumer owns head and tailCache 
		if((viewMask & CONSUMER) == CONSUMER){
	    	setTailCache(0);
			setHead(0);
		}
		mask = this.capacity - 1;
	}
	public static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}

	public boolean add(final Integer e) {
		if (offer(e)) {
			return true;
		}

		throw new IllegalStateException("Queue is full");
	}

	public boolean offer(final Integer e) {
		if (null == e) {
			throw new NullPointerException("Null is not a valid element");
		}

		final long currentTail = getTail();
		final long wrapPoint = currentTail - capacity;
		if (getHeadCache() <= wrapPoint) {
			setHeadCache(getHead());
			if (getHeadCache() <= wrapPoint) {
				return false;
			}
		}

		long offset = arrayBase
		        + ((currentTail & mask) << arrayElementScale);
		UnsafeAccess.unsafe.putInt(offset, e.intValue());

		setTail(currentTail + 1);

		return true;
	}

	public Integer poll() {
		final long currentHead = getHead();
		if (currentHead >= getTailCache()) {
			setTailCache(getTail());
			if (currentHead >= getTailCache()) {
				return null;
			}
		}

		final long offset = arrayBase + ((currentHead & mask) << arrayElementScale);
		final int e = UnsafeAccess.unsafe.getInt(offset);
//		UnsafeAccess.unsafe.putInt(null, offset, 0);
		setHead(currentHead + 1);

		return e;
	}

	public Integer remove() {
		final Integer e = poll();
		if (null == e) {
			throw new NoSuchElementException("Queue is empty");
		}

		return e;
	}

	public Integer element() {
		final Integer e = peek();
		if (null == e) {
			throw new NoSuchElementException("Queue is empty");
		}

		return e;
	}

	public Integer peek() {
		return null;
	}

	public int size() {
		return (int) (getTail() - getHead());
	}

	public boolean isEmpty() {
		return getTail() == getHead();
	}

	public boolean contains(final Object o) {
		return false;
	}

	public Iterator<Integer> iterator() {
		throw new UnsupportedOperationException();
	}

	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	public <T> T[] toArray(final T[] a) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(final Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(final Collection<?> c) {
		for (final Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}

		return true;
	}

	public boolean addAll(final Collection<? extends Integer> c) {
		for (final Integer e : c) {
			add(e);
		}

		return true;
	}

	public boolean removeAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(final Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		Object value;
		do {
			value = poll();
		} while (null != value);
	}

	private long getHead() {
		return UnsafeAccess.unsafe.getLongVolatile(null, headAddress);
	}

	private void setHead(final long value) {
		UnsafeAccess.unsafe.putOrderedLong(null, headAddress, value);
	}

	private long getTail() {
		return UnsafeAccess.unsafe.getLongVolatile(null, tailAddress);
	}

	private void setTail(final long value) {
		UnsafeAccess.unsafe.putOrderedLong(null, tailAddress, value);
	}

	private long getHeadCache() {
		return UnsafeAccess.unsafe.getLong(null, headCacheAddress);
	}

	private void setHeadCache(final long value) {
		UnsafeAccess.unsafe.putLong(headCacheAddress, value);
	}

	private long getTailCache() {
		return UnsafeAccess.unsafe.getLong(null, tailCacheAddress);
	}

	private void setTailCache(final long value) {
		UnsafeAccess.unsafe.putLong(tailCacheAddress, value);
	}
}
