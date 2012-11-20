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
 * Factory interface for the creation of new objects within a container.
 * @param <T> type of the object to be created.
 */
public interface Factory<T>
{
    /**
     * Create a new instance of a typed object.
     *
     * @return a new instance of the object type declared for the factory.
     */
    T newInstance();
}
