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

/**
 * A factory for creating objects to be used as a component in another structure.
 *
 * @param <T> type of component to be created.
 */
public interface ComponentFactory<T>
{
    /**
     * Create a new instance of a component object based on initial arguments.  If no arguments are required then
     * and empty array is required.
     *
     * @param initArgs arguments for initialisation.
     * @return a newly initialised object.
     */
    T newInstance(Object[] initArgs);
}
