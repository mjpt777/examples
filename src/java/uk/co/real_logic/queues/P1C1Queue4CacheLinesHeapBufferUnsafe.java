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
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

public final class P1C1Queue4CacheLinesHeapBufferUnsafe<E> implements Queue<E> {
    // 24b,8b,32b | 24b,8b,32b | 24b,8b,32b | 24b,8b,32b
    private final ByteBuffer buffy = allocateAlignedByteBuffer(4*CACHE_LINE_SIZE,
	    CACHE_LINE_SIZE);
    private final long headAddress;
    private final long tailCacheAddress;
    private final long tailAddress;
    private final long headCacheAddress;

    private final int capacity;
    private final int mask;
    private final E[] buffer;
    private static final long arrayBase;
    private static final int arrayScale;

    static
    {
        try
        {
            arrayBase = UnsafeAccess.unsafe.arrayBaseOffset(Object[].class);
            final int scale = UnsafeAccess.unsafe.arrayIndexScale(Object[].class);

            if (4==scale)
            {
                arrayScale = 2;
            }
            else if (8==scale)
            {
                arrayScale = 3;
            }
            else
            {
                throw new IllegalStateException("Unknown pointer size");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public P1C1Queue4CacheLinesHeapBufferUnsafe(final int capacity) {
	long alignedAddress = UnsafeDirectByteBuffer.getAddress(buffy);

	headAddress = alignedAddress+(CACHE_LINE_SIZE/2 - 8);
	tailCacheAddress = headAddress + CACHE_LINE_SIZE;
	tailAddress = tailCacheAddress + CACHE_LINE_SIZE;
	headCacheAddress = tailAddress + CACHE_LINE_SIZE;

	this.capacity = findNextPositivePowerOfTwo(capacity);
	mask = this.capacity - 1;
	buffer = (E[]) new Object[this.capacity];
    }

    public static int findNextPositivePowerOfTwo(final int value) {
	return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public boolean add(final E e) {
	if (offer(e)) {
	    return true;
	}

	throw new IllegalStateException("Queue is full");
    }

    public boolean offer(final E e) {
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

    UnsafeAccess.unsafe.putObject(buffer,arrayBase+((currentTail & mask)<<arrayScale),e);

	setTail(currentTail + 1);

	return true;
    }

    public E poll() {
	final long currentHead = getHead();
	if (currentHead >= getTailCache()) {
	    setTailCache(getTail());
	    if (currentHead >= getTailCache()) {
		return null;
	    }
	}

	final long offset = arrayBase+((currentHead & mask)<<arrayScale);
	final E e = (E)UnsafeAccess.unsafe.getObject(buffer,offset);
	UnsafeAccess.unsafe.putObject(buffer,offset,null);
	setHead(currentHead + 1);

	return e;
    }

    public E remove() {
	final E e = poll();
	if (null == e) {
	    throw new NoSuchElementException("Queue is empty");
	}

	return e;
    }

    public E element() {
	final E e = peek();
	if (null == e) {
	    throw new NoSuchElementException("Queue is empty");
	}

	return e;
    }

    public E peek() {
	return buffer[(int) getHead() & mask];
    }

    public int size() {
	return (int) (getTail() - getHead());
    }

    public boolean isEmpty() {
	return getTail() == getHead();
    }

    public boolean contains(final Object o) {
	if (null == o) {
	    return false;
	}

	for (long i = getHead(), limit = getTail(); i < limit; i++) {
	    final E e = buffer[(int) i & mask];
	    if (o.equals(e)) {
		return true;
	    }
	}

	return false;
    }

    public Iterator<E> iterator() {
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

    public boolean addAll(final Collection<? extends E> c) {
	for (final E e : c) {
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
