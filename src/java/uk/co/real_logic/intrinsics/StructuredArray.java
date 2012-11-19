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
 *      An array of structured types that behaves like an array of structures in the C language.
 *      The length of the array is a signed 64-bit value.
 * </p>
 * <p>
 *      A JVM can optimise the implementation to provide a compact contiguous layout
 *      that facilitates consistent stride based memory access.
 * </p>
 * @param <E> structured type occupying each element.
 */
public class StructuredArray<E> implements Iterable<E>
{
    private static final int PARTITION_POWER_OF_TWO = 30;
    private static final int MAX_PARTITION_SIZE = 1 << PARTITION_POWER_OF_TWO;
    private static final int MASK = MAX_PARTITION_SIZE  - 1;

    private final long length;
    private final E[][] partitions;
    private final Field[] fields;
    private final Class<E> componentClass;

    /**
     * Create an array of types to be laid out like a contiguous array of structures.
     *
     * @param length of the array to create.
     * @param componentClass of each element in the array
     */
    @SuppressWarnings("unchecked")
    public StructuredArray(final long length, final Class<E> componentClass)
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
     * Get the {@link Class} of elements stored as components of the array.
     *
     * @return the {@link Class} of elements stored as components of the array.
     */
    public Class<E> getComponentClass()
    {
        return componentClass;
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
     * Shallow copy a region of structures from one array to the other.  If the same array is both the source
     * and destination then the copy will happen as if a temporary intermediate array was used.
     *
     * @param source array to copy.
     * @param sourceOffset offset index in source where the region begins.
     * @param destination array into which the copy should occur.
     * @param destinationOffset offset index in the destination where the region begins.
     * @param count of structure elements to copy.
     * @param <E> type of the structure being copied.
     * @throws IllegalArgumentException if the {@link StructuredArray#getComponentClass()}s are not identical.
     */
    public static <E> void shallowCopy(final StructuredArray<E> source, final long sourceOffset,
                                       final StructuredArray<E> destination, final long destinationOffset,
                                       final long count)
    {
        if (source.componentClass != destination.componentClass)
        {
            final String msg = String.format("Only objects of the same class can be copied: %s != %s",
                                             source.getClass(), destination.getClass());

            throw new IllegalArgumentException(msg);
        }

        final Field[] fields = source.fields;

        if (destinationOffset < sourceOffset)
        {
            for (long sourceIndex = sourceOffset, destinationIndex = destinationOffset, limit = sourceOffset + count;
                 sourceIndex < limit;
                 sourceIndex++, destinationIndex++)
            {
                shallowCopy(source.get(sourceIndex), destination.get(destinationIndex), fields);
            }
        }
        else
        {
            for (long sourceIndex = sourceOffset + count, destinationIndex = destinationOffset + count, limit = sourceOffset - 1;
                 sourceIndex > limit;
                 sourceIndex--, destinationIndex--)
            {
                shallowCopy(source.get(sourceIndex), destination.get(destinationIndex), fields);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public StructureIterator iterator()
    {
        return new StructureIterator();
    }

    /**
     * Specialised {@link Iterator} with the ability to be {@link #reset()} enabling reuse.
     */
    public class StructureIterator implements Iterator<E>
    {

        private long cursor = 0;
        /**
         * {@inheritDoc}
         */
        public boolean hasNext()
        {
            return cursor < length;
        }

        /**
         * {@inheritDoc}
         */
        public E next()
        {
            if (cursor >= length)
            {
                throw new NoSuchElementException();
            }

            return get(cursor++);
        }

        /**
         * Remove operations are not supported on {@link StructuredArray}s.
         *
         * @throws UnsupportedOperationException if called.
         */
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Reset to the beginning of the collection enabling reuse of the iterator.
         */
        public void reset()
        {
            cursor = 0;
        }

    }

    private static void shallowCopy(final Object source, final Object destination, final Field[] fields)
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
}
