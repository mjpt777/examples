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

import java.lang.reflect.Constructor;

/**
 * {@link ComponentFactory} that uses reflection to call a constructor for creating new component objects.
 *
 * @param <T> type of the component object to be constructed.
 */
public class ConstructorComponentFactory<T> implements ComponentFactory<T>
{
    private final Constructor<T> constructor;

    /**
     * Create a new {@link ComponentFactory} that will create new components based on a constructor matching the argument
     * signature provided for the component class.
     *
     * @param componentClass component class of object to construct.
     * @param argTypes argument types to match the constructor signature.
     */
    public ConstructorComponentFactory(final Class<T> componentClass, final Class[] argTypes)
    {
        try
        {
            constructor = componentClass.getConstructor(argTypes);
        }
        catch (final NoSuchMethodException ex)
        {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public T newInstance(final Object[] initArgs)
    {
        try
        {
            return constructor.newInstance(initArgs);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException(ex);
        }
    }
}
