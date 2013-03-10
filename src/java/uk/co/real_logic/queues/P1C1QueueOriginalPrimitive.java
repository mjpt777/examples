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

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public final class P1C1QueueOriginalPrimitive implements Queue<Integer> {
	private final int capacity;
	private final int mask;
	private final int[] buffer;

	private final AtomicLong tail = new PaddedAtomicLong(0);
	private final AtomicLong head = new PaddedAtomicLong(0);

	public static class PaddedLong {
		public long value = 0, p1, p2, p3, p4, p5, p6;
	}

	private final PaddedLong tailCache = new PaddedLong();
	private final PaddedLong headCache = new PaddedLong();

	@SuppressWarnings("unchecked")
	public P1C1QueueOriginalPrimitive(final int capacity) {
		this.capacity = findNextPositivePowerOfTwo(capacity);
		mask = this.capacity - 1;
		buffer = new int[this.capacity];
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

		final long currentTail = tail.get();
		final long wrapPoint = currentTail - capacity;
		if (headCache.value <= wrapPoint) {
			headCache.value = head.get();
			if (headCache.value <= wrapPoint) {
				return false;
			}
		}

		buffer[(int) currentTail & mask] = e;
		tail.lazySet(currentTail + 1);

		return true;
	}

	public Integer poll() {
		final long currentHead = head.get();
		if (currentHead >= tailCache.value) {
			tailCache.value = tail.get();
			if (currentHead >= tailCache.value) {
				return null;
			}
		}

		final int index = (int) currentHead & mask;
		final Integer e = buffer[index];
		head.lazySet(currentHead + 1);

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
		return buffer[(int) head.get() & mask];
	}

	public int size() {
		return (int) (tail.get() - head.get());
	}

	public boolean isEmpty() {
		return tail.get() == head.get();
	}

	public boolean contains(final Object o) {
		if (null == o) {
			return false;
		}

		for (long i = head.get(), limit = tail.get(); i < limit; i++) {
			final Integer e = buffer[(int) i & mask];
			if (o.equals(e)) {
				return true;
			}
		}

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
}
