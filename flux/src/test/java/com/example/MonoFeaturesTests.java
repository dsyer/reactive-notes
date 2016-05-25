package com.example;

import org.junit.Before;
import org.junit.Test;

import reactor.core.publisher.Mono;

public class MonoFeaturesTests {
    private Mono<String> mono;

    @Before
    public void generate() throws Exception {
        mono = Mono.just("red");
    }

    @Test
    public void operate() throws Exception {
        mono.log().map(value -> value.toUpperCase());
        // Nothing happened. No logs, nothing.
    }

    @Test
    public void subscribe() throws Exception {
        mono.log().map(value -> value.toUpperCase()).subscribe();
        // Logs the subscription, an unbounded request, all elements and finally completion.
    }

}
