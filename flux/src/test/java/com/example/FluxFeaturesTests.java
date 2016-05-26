package com.example;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Computations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

public class FluxFeaturesTests {

    private static Logger log = LoggerFactory.getLogger(FluxFeaturesTests.class);

    private static List<String> COLORS = Arrays.asList("red", "white", "blue");

    private Random random = new Random();

    private Flux<String> flux;

    @Before
    public void generate() throws Exception {
        // this.flux = Flux.range(1, 10).map(i -> COLORS.get(random.nextInt(3)));
        this.flux = Flux.fromIterable(COLORS);
    }

    @Test
    public void operate() throws Exception {
        this.flux.log().map(value -> value.toUpperCase());
        // Nothing happened. No logs, nothing.
    }

    @Test
    public void subscribe() throws Exception {
        this.flux.log().map(value -> value.toUpperCase()).subscribe();
        // Logs the subscription, an unbounded request, all elements and finally
        // completion.
    }

    @Test
    public void consume() throws Exception {
        this.flux.log().map(value -> value.toUpperCase()).subscribe(System.out::println);
        // Same as above but items are printed as they emerge from the end of
        // the operator
        // chain
    }

    @Test
    public void subscription() throws Exception {
        this.flux.log().map(value -> value.toUpperCase()).subscribe(new Subscriber<String>() {

            private long count = 0;
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(2);
            }

            @Override
            public void onNext(String t) {
                this.count++;
                if (this.count >= 2) {
                    this.count = 0;
                    this.subscription.request(2);
                }
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
        // Logs the subscription, requests 2 at a time, all elements and
        // completion.
    }

    @Test
    public void batching() throws Exception {
        this.flux.log().map(value -> value.toUpperCase()).useCapacity(2).subscribe();
        // Logs the subscription, requests 2 at a time, all elements and finally
        // completion.
    }

    @Test
    public void parallel() throws Exception {
        this.flux.log().map(value -> value.toUpperCase()).subscribeOn(Computations.parallel()).useCapacity(2)
                .subscribe();
        // Logs the subscription, requests 2 at a time, all elements and finally
        // completion.
        Thread.sleep(500L);
    }

    @Test
    public void concurrent() throws Exception {
        Scheduler scheduler = Computations.parallel();
        this.flux.log().flatMap(value -> Mono.just(value.toUpperCase()).subscribeOn(scheduler), 2).subscribe(value -> {
            log.info("Consumed: " + value);
        });
        // Logs the subscription, requests 2 at a time, all elements and finally
        // completion.
        Thread.sleep(500L);
    }

    @Test
    public void publish() throws Exception {
        this.flux.log().map(value -> value.toUpperCase()).subscribeOn(Computations.parallel("sub"))
                .publishOn(Computations.parallel("pub"), 2).subscribe(value -> {
                    log.info("Consumed: " + value);
                });
        // Logs the consumed messages in a separate thread.
        Thread.sleep(500L);
    }

}