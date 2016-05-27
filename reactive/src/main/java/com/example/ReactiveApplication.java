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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Computations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@SpringBootApplication
@RestController
public class ReactiveApplication {

    private static Logger log = LoggerFactory.getLogger(ReactiveApplication.class);
    private RestTemplate restTemplate = new RestTemplate();
    // private Scheduler scheduler = Computations.parallel("sub", 16, 40);

    @RequestMapping("/parallel")
    public Mono<Result> parallel() {
        log.info("Handling /parallel");
        Scheduler scheduler = Computations.parallel();
        return Flux.range(1, 10) // <1>
                .log() //
                .flatMap( // <2>
                        value -> Mono.fromCallable(() -> block(value)) // <3>
                                .subscribeOn(scheduler), // <4>
                        4) // <5>
                .collect(Result::new, Result::add) // <6>
                .doOnSuccess(result -> { scheduler.shutdown(); result.stop(); }); // <7>

        // <1> make 10 calls
        // <2> drop down to a new publisher to process in parallel
        // <3> blocking code here inside a Callable to defer execution
        // <4> subscribe to the slow publisher on a background thread
        // <5> concurrency hint in flatMap
        // <6> collect results and aggregate into a single object
        // <7> at the end stop the clock

    }

    @RequestMapping("/serial")
    public Mono<Result> serial() {
        Scheduler scheduler = Computations.parallel();
                log.info("Handling /serial");
        return Flux.range(1, 10) // <1>
                .log() //
                .map( // <2>
                        value -> block(value)) // <3>
                .collect(Result::new, Result::add) // <4>
                .doOnSuccess(Result::stop) // <5>
                .subscribeOn(scheduler); // <6>
        // <1> make 10 calls
        // <2> stay in the same publisher chain
        // <3> blocking call not deferred (no point in this case)
        // <4> collect results and aggregate into a single object
        // <5> at the end stop the clock
        // <6> subscribe on a background thread
    }

    private HttpStatus block(int value) {
        return restTemplate.getForEntity("http://example.com", String.class, value).getStatusCode();
    }

    public static void main(String[] args) {
        SpringApplication.run(ReactiveApplication.class, args);
    }

}

class Result {

    private ConcurrentMap<HttpStatus, AtomicLong> counts = new ConcurrentHashMap<>();

    private long timestamp = System.currentTimeMillis();

    private long duration;

    public long add(HttpStatus status) {
        AtomicLong value = counts.getOrDefault(status, new AtomicLong());
        counts.putIfAbsent(status, value);
        return value.incrementAndGet();
    }

    public void stop() {
        this.duration = System.currentTimeMillis() - timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public Map<HttpStatus, AtomicLong> getCounts() {
        return counts;
    }

}
