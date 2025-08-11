package com.example;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class MoreTests {
    @ParameterizedTest
    @ValueSource(strings = {"Hello", "World", "foo", "bar"})
    void verifyFail(String input) {
        assertTrue(new Example().getMessage().contains(input));
    }
}
