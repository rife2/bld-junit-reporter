package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExampleTests {
    @ParameterizedTest
    @ValueSource(strings = {"foo", "bar"})
    void verifyFail(String input) {
        assertEquals(input, new Example().getMessage());
    }

    @Test
    void verifyHello() {
        assertEquals("Hello World!", new Example().getMessage());
    }

    @Test
    void verifyHelloFoo() {
        assertEquals("Hello Foo!", new Example().getMessage());
    }
}
