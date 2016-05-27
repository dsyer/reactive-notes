/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class StreamFeaturesTests {
    private Stream<String> stream;

    @Before
    public void generate() throws Exception {
        stream = Stream.of("red", "white", "blue");
    }

    @Test
    public void operate() throws Exception {
        stream.map(value -> {
            System.out.println(value);
            return value.toUpperCase();
        });
        // Nothing happened. No logs, nothing.
    }

    @Test
    public void subscribe() throws Exception {
        stream.map(value -> {
            System.out.println(value);
            return value.toUpperCase();
        }).count();
        // Prints the values (count() is just a quick way to subscribe to the stream).
    }

}
