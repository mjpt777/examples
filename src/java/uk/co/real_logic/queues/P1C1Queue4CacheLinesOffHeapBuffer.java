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
package uk.co.real_logic.queues;

import static uk.co.real_logic.queues.UnsafeDirectByteBuffer.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public final class P1C1Queue4CacheLinesOffHeapBuffer implements Queue<Integer> {
	// 24b,8b,32b | 24b,8b,32b | 24b,8b,32b | 24b,8b,32b
	private final ByteBuffer buffy;
	private final long headAddress;
	private final long tailCacheAddress;
	private final long tailAddress;
	private final long headCacheAddress;

	private final int capacity;
	private final int mask;
	private final long arrayBase;
	private static final int arrayScale = 2;
//
//	static {
//		try {
//			final int scale = UnsafeAccess.unsafe
//			        .arrayIndexScale(Object[].class);
//
//			if (4 == scale) {
//				arrayScale = 2;
//			} else if (8 == scale) {
//				arrayScale = 3;
//			} else {
//				throw new IllegalStateException("Unknown pointer size");
//			}
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}

	@SuppressWarnings("unchecked")
	public P1C1Queue4CacheLinesOffHeapBuffer(final int capacity) {
		this.capacity = findNextPositivePowerOfTwo(capacity);
		buffy = allocateAlignedByteBuffer(
		        4 * CACHE_LINE_SIZE + this.capacity<<arrayScale, CACHE_LINE_SIZE);

		long alignedAddress = UnsafeDirectByteBuffer.getAddress(buffy);

		headAddress = alignedAddress + (CACHE_LINE_SIZE / 2 - 8);
		tailCacheAddress = headAddress + CACHE_LINE_SIZE;
		tailAddress = tailCacheAddress + CACHE_LINE_SIZE;
		headCacheAddress = tailAddress + CACHE_LINE_SIZE;
		arrayBase = alignedAddress + 4* CACHE_LINE_SIZE;
		mask = this.capacity - 1;
	}
	public P1C1Queue4CacheLinesOffHeapBuffer(final ByteBuffer buff, final int capacity) {
		this.capacity = findNextPositivePowerOfTwo(capacity);
		buffy = alignedSlice(4 * CACHE_LINE_SIZE + this.capacity<<arrayScale, 
									CACHE_LINE_SIZE, buff);

		long alignedAddress = UnsafeDirectByteBuffer.getAddress(buffy);

		headAddress = alignedAddress + (CACHE_LINE_SIZE / 2 - 8);
		tailCacheAddress = headAddress + CACHE_LINE_SIZE;
		tailAddress = tailCacheAddress + CACHE_LINE_SIZE;
		headCacheAddress = tailAddress + CACHE_LINE_SIZE;
		arrayBase = alignedAddress + 4* CACHE_LINE_SIZE;
		setTail(0);
		setHead(0);
		setHeadCache(0);
		setTailCache(0);
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
		        + ((currentTail & mask) << arrayScale);
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

		final long offset = arrayBase + ((currentHead & mask) << arrayScale);
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
		return null;//buffer[(int) getHead() & mask];
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
//	if (null == o) {
//			return false;
//		}
//
//		for (long i = getHead(), limit = getTail(); i < limit; i++) {
//			final E e = buffer[(int) i & mask];
//			if (o.equals(e)) {
//				return true;
//			}
//		}
//
//		return false;
//	}

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
