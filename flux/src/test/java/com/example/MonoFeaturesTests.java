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

import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

public class MonoFeaturesTests {
    private Mono<String> mono;

    private static Logger log = LoggerFactory.getLogger(MonoFeaturesTests.class);

    @Before
    public void generate() throws Exception {
        this.mono = Mono.just("red");
    }

    @Test
    public void operate() throws Exception {
        this.mono.log().map(String::toUpperCase);
        // Nothing happened. No logs, nothing.
    }

    @Test
    public void subscribe() throws Exception {
        this.mono.log().map(String::toUpperCase).subscribe();
        // Logs the subscription, an unbounded request, all elements and finally
        // completion.
    }

    @Test
    public void gem22() throws Exception {
        Callable<String> callable = () -> "foo";
        Mono.just("irrelevant").log().map(unused -> {
            try {
                return callable.call();
            } catch (Exception ex) {
                throw Exceptions.bubble(ex); // or Exceptions.propagate(ex)
            }
        }).subscribe(log::info, Throwable::printStackTrace);
    }

    @Test
    public void notGem22() throws Exception {
        Callable<String> callable = () -> "foo";
        Mono.fromCallable(callable).log().subscribe(log::info, Throwable::printStackTrace);
    }

}
