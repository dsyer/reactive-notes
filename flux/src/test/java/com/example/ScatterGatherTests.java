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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Computations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class ScatterGatherTests {

    private static Logger log = LoggerFactory.getLogger(ScatterGatherTests.class);

    private static List<String> COLORS = Arrays.asList("red", "white", "blue");

    private Random random = new Random();

    @Test
    public void subscribe() throws Exception {
        Scheduler scheduler = Computations.parallel();
        System.err.println( //
                Flux.range(1, 10) //
                        .map(i -> COLORS.get(random.nextInt(3))) //
                        .log() //
                        .flatMap(value -> Mono.fromCallable(() -> {
                            Thread.sleep(1000L);
                            return value;
                        }).subscribeOn(scheduler), 4) //
                        .collect(Result::new, Result::add) //
                        .doOnNext(Result::stop) //
                        .get() //
        );
    }

    @Test
    public void subscribeWithBackgroundPublisherExtractedToMethod() throws Exception {
        System.err.println( //
                Flux.range(1, 10)//
                        .map(i -> COLORS.get(random.nextInt(3))) //
                        .log() //
                        .flatMap(background(Computations.parallel()), 4) //
                        .collect(Result::new, Result::add) //
                        .doOnNext(Result::stop) //
                        .get() //
        );
    }

    @Test
    public void publish() throws Exception {
        Result result = new Result();
        System.err.println(Flux.range(1, 10).map(i -> COLORS.get(random.nextInt(3))).log().doOnNext(value -> {
            log.info("Next: " + value);
            sleep(1000L);
            result.add(value);
        }).doOnComplete(() -> result.stop()).subscribeOn(Computations.parallel("sub"))
                .publishOn(Computations.parallel("pub"), 4).then().then(Mono.just(result)).get());
    }

    @Test
    public void just() throws Exception {
        Scheduler scheduler = Computations.parallel();
        System.err.println(Flux.range(1, 10).map(i -> COLORS.get(random.nextInt(3))).log()
                .flatMap(value -> Mono.just(value.toUpperCase()).subscribeOn(scheduler), 2)
                .collect(Result::new, Result::add).doOnNext(Result::stop).get());
    }

    private Function<? super String, ? extends Publisher<? extends String>> background(Scheduler scheduler) {
        return value -> Mono.fromCallable(() -> {
            Thread.sleep(1000L);
            return value;
        }).subscribeOn(scheduler);
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted");
        }
    }

}

class Result {

    private ConcurrentMap<String, AtomicLong> counts = new ConcurrentHashMap<>();

    private long timestamp = System.currentTimeMillis();

    private long duration;

    public long add(String colour) {
        AtomicLong value = counts.getOrDefault(colour, new AtomicLong());
        counts.putIfAbsent(colour, value);
        return value.incrementAndGet();
    }

    public void stop() {
        this.duration = System.currentTimeMillis() - timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public Map<String, AtomicLong> getCounts() {
        return counts;
    }

    @Override
    public String toString() {
        return "Result [duration=" + duration + ", counts=" + counts + "]";
    }

}
