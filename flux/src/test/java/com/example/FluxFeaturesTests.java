package com.example;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import reactor.core.publisher.Flux;

public class FluxFeaturesTests {
    private Flux<String> flux;

    @Before
    public void generate() throws Exception {
        flux = Flux.just("red", "white", "blue");
    }

    @Test
    public void operate() throws Exception {
        flux.log().map(value -> value.toUpperCase());
        // Nothing happened. No logs, nothing.
    }

    @Test
    public void subscribe() throws Exception {
        flux.log().map(value -> value.toUpperCase()).subscribe();
        // Logs the subscription, an unbounded request, all elements and finally completion.
    }

    @Test
    public void consume() throws Exception {
        flux.log().map(value -> value.toUpperCase()).subscribe(System.out::println);
        // Same as above but items are printed as they emerge from the end of the operator chain
    }

    @Test
    public void subscription() throws Exception {
        flux.log().map(value -> value.toUpperCase()).subscribe(new Subscriber<String>() {

            private long count = 0;
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(2);
            }

            @Override
            public void onNext(String t) {
                count++;
                if (count>=2) {
                    count = 0;
                    subscription.request(2);
                }
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });
        // Logs the subscription, requests 2 at a time, all elements and completion.
    }

    @Test
    public void batching() throws Exception {
        flux.log().map(value -> value.toUpperCase()).useCapacity(2).subscribe();
        // Logs the subscription, requests 2 at a time, all elements and finally completion.
    }


}
