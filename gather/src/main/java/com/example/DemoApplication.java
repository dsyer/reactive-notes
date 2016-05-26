package com.example;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
public class DemoApplication {

    private static Logger log = LoggerFactory.getLogger(DemoApplication.class);
    private RestTemplate restTemplate = new RestTemplate();

    @RequestMapping("/parallel")
    public CompletableFuture<Result> parallel() {
        Scheduler scheduler = Computations.parallel();
        log.info("Handling /parallel");
        return Flux.range(1, 10) // make 10 calls
                .log() //
                .flatMap( // drop down to a new publisher to process in parallel
                        value -> Mono.fromCallable(() -> restTemplate
                                .getForEntity("http://example.com", String.class, value).getStatusCode())
                                .subscribeOn(scheduler), // subscribe to the
                                                         // slow publisher
                        4) // concurrency hint in flatMap
                .collect(Result::new, Result::add) // collect results
                .doOnSuccess(Result::stop) // at the end stop the clock
                .toCompletableFuture();
    }

    @RequestMapping("/serial")
    public CompletableFuture<Result> serial() {
        Scheduler scheduler = Computations.parallel();
        log.info("Handling /serial");
        return Flux.range(1, 10) // make 10 calls
                .log() //
                .map( // stay in the same publisher chain
                        value -> restTemplate.getForEntity("http://example.com", String.class, value).getStatusCode())
                .collect(Result::new, Result::add) // collect results
                .doOnSuccess(Result::stop) // at the end stop the clock
                .subscribeOn(scheduler)
                .toCompletableFuture();
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
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
