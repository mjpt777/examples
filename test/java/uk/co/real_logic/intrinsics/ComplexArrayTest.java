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

import org.junit.Test;

import static java.lang.Long.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class ComplexArrayTest
{
    @Test
    public void shouldConstructArrayOfGivenLength()
    {
        final long length = 7;
        final ComplexArray<MockStructure> complexArray = new ComplexArray<MockStructure>(length, MockStructure.class);

        assertThat(valueOf(complexArray.getLength()), is(valueOf(length)));
    }

    @Test
    public void shouldGetCorrectValueAtGivenIndex()
    {
        final long length = 11;
        final ComplexArray<MockStructure> complexArray = new ComplexArray<MockStructure>(length, MockStructure.class);

        for (long i = 0; i < length; i++)
        {
            final MockStructure mockStructure = complexArray.get(i);
            mockStructure.setIndex(i);
            mockStructure.setTestValue(i * 2);
        }

        for (long i = 0; i < length; i++)
        {
            final MockStructure mockStructure = complexArray.get(i);

            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(i * 2)));
        }
    }

    @Test
    public void shouldIterateOverArray()
    {
        final long length = 11;
        final ComplexArray<MockStructure> complexArray = new ComplexArray<MockStructure>(length, MockStructure.class);

        for (long i = 0; i < length; i++)
        {
            final MockStructure mockStructure = complexArray.get(i);
            mockStructure.setIndex(i);
            mockStructure.setTestValue(i * 2);
        }

        int i = 0;
        for (final MockStructure mockStructure : complexArray)
        {
            assertThat(valueOf(mockStructure.getIndex()), is(valueOf(i)));
            assertThat(valueOf(mockStructure.getTestValue()), is(valueOf(i * 2)));
            i++;
        }

        assertThat(valueOf(i), is(valueOf(length)));
    }

    @Test
    public void shouldCopyToArrayElement()
    {
        final long index = 7;
        final long length = 11;
        final ComplexArray<MockStructure> complexArray = new ComplexArray<MockStructure>(length, MockStructure.class);

        final MockStructure expectedValue = new MockStructure();
        expectedValue.setIndex(31);
        expectedValue.setTestValue(257);

        complexArray.copyToArray(index, expectedValue);

        assertThat(complexArray.get(index), is(expectedValue));
    }

    @Test
    public void shouldCopyFromArrayElement()
    {
        final long index = 7;
        final long length = 11;
        final ComplexArray<MockStructure> complexArray = new ComplexArray<MockStructure>(length, MockStructure.class);

        final MockStructure expectedValue = complexArray.get(index);
        expectedValue.setIndex(31);
        expectedValue.setTestValue(257);

        final MockStructure testValue = new MockStructure();
        complexArray.copyFromArray(index, testValue);

        assertThat(testValue, is(expectedValue));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void shouldThrowOutOfBoundExceptionForAccessesOutOfBounds()
    {
        final long length = 11;
        final ComplexArray<MockStructure> complexArray = new ComplexArray<MockStructure>(length, MockStructure.class);

        complexArray.get(length);
    }

    @Test
    public void shouldCloneElementForGivenIndex()
    {
        final long index = 7;
        final long length = 11;
        final ComplexArray<MockStructure> complexArray = new ComplexArray<MockStructure>(length, MockStructure.class);

        final MockStructure expectedValue = complexArray.get(index);
        expectedValue.setIndex(31);
        expectedValue.setTestValue(257);

        final MockStructure clone = complexArray.clone(index);

        assertThat(clone, is(expectedValue));
        assertFalse("Clone must be a different instance", clone == expectedValue);
    }

    public static class MockStructure
    {
        private long index = -1;
        private long testValue = Long.MIN_VALUE;

        public long getIndex()
        {
            return index;
        }

        public void setIndex(final long index)
        {
            this.index = index;
        }

        public long getTestValue()
        {
            return testValue;
        }

        public void setTestValue(final long testValue)
        {
            this.testValue = testValue;
        }

        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final MockStructure that = (MockStructure)o;

            return index == that.index && testValue == that.testValue;
        }

        public int hashCode()
        {
            int result = (int)(index ^ (index >>> 32));
            result = 31 * result + (int)(testValue ^ (testValue >>> 32));
            return result;
        }

        public String toString()
        {
            return "MockStructure{" +
                "index=" + index +
                ", testValue=" + testValue +
                '}';
        }
    }
}
