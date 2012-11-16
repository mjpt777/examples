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
package uk.co.real_logic.intrinsics;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 *      An effective large array of complex types that behaves like an array of structures in the C language.
 * </p>
 * <p>
 *      A JVM can optimise the implementation to give a compact contiguous layout to facilitate stride based memory access.
 * </p>
 * @param <E> complex type occupying each element.
 */
public class ComplexArray<E> implements Iterable<E>
{
    private static final int PARTITION_POWER_OF_TWO = 30;
    private static final int MAX_PARTITION_SIZE = 1 << PARTITION_POWER_OF_TWO;
    private static final int MASK = MAX_PARTITION_SIZE  - 1;

    private final long length;
    private final E[][] partitions;
    private final Field[] fields;
    private final Class<E> componentClass;

    /**
     * Create an array of complex types to be laid out like a contagious array of structures.
     *
     * @param length of the array to create.
     * @param componentClass of each element in the array
     */
    @SuppressWarnings("unchecked")
    public ComplexArray(final long length, final Class<E> componentClass)
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("length cannot be negative");
        }

        if (null == componentClass)
        {
            throw new NullPointerException("componentClass cannot be null");
        }

        this.length = length;
        this.componentClass = componentClass;
        this.fields = componentClass.getDeclaredFields();
        for (final Field field : fields)
        {
            field.setAccessible(true);
        }

        final int numFullPartitions = (int)(length / MAX_PARTITION_SIZE);
        final int lastPartitionSize = (int)(length % MAX_PARTITION_SIZE);

        partitions = (E[][])new Object[numFullPartitions + 1][];
        for (int i = 0; i < numFullPartitions; i++)
        {
            partitions[i] = (E[])new Object[MAX_PARTITION_SIZE];
        }
        partitions[numFullPartitions] = (E[])new Object[lastPartitionSize];

        try
        {
            for (final E[] partition : partitions)
            {
                for (int i = 0, size = partition.length; i < size; i++)
                {
                    partition[i] = componentClass.newInstance();
                }
            }
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the length of the array by number of elements.
     *
     * @return the number of elements in the array.
     */
    public long getLength()
    {
        return length;
    }

    /**
     * Get a reference to an element in the array.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public E get(final long index)
    {
        final int partitionIndex = (int)(index >>> PARTITION_POWER_OF_TWO);
        final int partitionOffset = (int)index & MASK;

        return partitions[partitionIndex][partitionOffset];
    }

    /**
     * Clone and element in the array making a new copy.
     *
     * @param index of the element to be cloned.
     * @return a copy of the indexed element.
     */
    public E clone(final long index)
    {
        try
        {
            final E clone = componentClass.newInstance();
            copyFromArray(index, clone);

            return clone;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Shallow copy a structure into an indexed element.
     *
     * @param index of the element to be copied over.
     * @param source structure to copy over the indexed element.
     */
    public void copyToArray(final long index, E source)
    {
        final E destination = get(index);
        shallowCopy(source, destination);
    }

    /**
     * Shallow copy a structure from an indexed element.
     *
     * @param index of the element to be copied.
     * @param destination structure into which the element should be copied.
     */
    public void copyFromArray(final long index, E destination)
    {
        final E source = get(index);
        shallowCopy(source, destination);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<E> iterator()
    {
        return new ElementIterator();
    }

    private void shallowCopy(final E source, final E destination)
    {
        try
        {
            for (final Field field : fields)
            {
                field.set(destination, field.get(source));
            }
        }
        catch (final IllegalAccessException shouldNotHappen)
        {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    private class ElementIterator implements Iterator<E>
    {
        private long cursor = 0;

        public boolean hasNext()
        {
            return cursor < length;
        }

        public E next()
        {
            if (cursor >= length)
            {
                throw new NoSuchElementException();
            }

            return get(cursor++);
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
