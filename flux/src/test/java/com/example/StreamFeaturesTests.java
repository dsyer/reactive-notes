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
