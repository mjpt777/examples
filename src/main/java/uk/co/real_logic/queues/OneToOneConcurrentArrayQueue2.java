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

public final class OneToOneConcurrentArrayQueue2<E>
    implements Queue<E>
{
    private final int mask;
    private final E[] buffer;

    private final AtomicLong tail = new AtomicLong(0);
    private final AtomicLong head = new AtomicLong(0);

    @SuppressWarnings("unchecked")
    public OneToOneConcurrentArrayQueue2(int capacity)
    {
        capacity = findNextPositivePowerOfTwo(capacity);
        mask = capacity - 1;
        buffer = (E[])new Object[capacity];
    }

    public static int findNextPositivePowerOfTwo(final int value)
    {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public boolean add(final E e)
    {
        if (offer(e))
        {
            return true;
        }

        throw new IllegalStateException("Queue is full");
    }

    public boolean offer(final E e)
    {
        if (null == e)
        {
            throw new NullPointerException("Null is not a valid element");
        }

        final long currentTail = tail.get();
        final long wrapPoint = currentTail - buffer.length;
        if (head.get() <= wrapPoint)
        {
            return false;
        }

        buffer[(int)currentTail & mask] = e;
        tail.lazySet(currentTail + 1);

        return true;
    }

    public E poll()
    {
        final long currentHead = head.get();
        if (currentHead >= tail.get())
        {
             return null;
        }

        final int index = (int)currentHead & mask;
        final E e = buffer[index];
        buffer[index] = null;
        head.lazySet(currentHead + 1);

        return e;
    }

    public E remove()
    {
        final E e = poll();
        if (null == e)
        {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    public E element()
    {
        final E e = peek();
        if (null == e)
        {
            throw new NoSuchElementException("Queue is empty");
        }

        return e;
    }

    public E peek()
    {
        return buffer[(int)head.get() & mask];
    }

    public int size()
    {
        return (int)(tail.get() - head.get());
    }

    public boolean isEmpty()
    {
        return tail.get() == head.get();
    }

    public boolean contains(final Object o)
    {
        if (null == o)
        {
            return false;
        }

        for (long i = head.get(), limit = tail.get(); i < limit; i++)
        {
            final E e = buffer[(int)i & mask];
            if (o.equals(e))
            {
                return true;
            }
        }

        return false;
    }

    public Iterator<E> iterator()
    {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray()
    {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(final T[] a)
    {
        throw new UnsupportedOperationException();
    }

    public boolean remove(final Object o)
    {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(final Collection<?> c)
    {
        for (final Object o : c)
        {
            if (!contains(o))
            {
                return false;
            }
        }

        return true;
    }

    public boolean addAll(final Collection<? extends E> c)
    {
        for (final E e : c)
        {
            add(e);
        }

        return true;
    }

    public boolean removeAll(final Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(final Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    public void clear()
    {
        Object value;
        do
        {
            value = poll();
        }
        while (null != value);
    }
}

