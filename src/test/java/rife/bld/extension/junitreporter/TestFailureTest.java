/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.extension.junitreporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TestFailureTest {
    @Nested
    @DisplayName("Compare Tests")
    class CompareTests {
        @Test
        void compareToDifferentClassDifferentTestName() {
            var failure1 = new TestFailure(
                    "testA", "Test A", "ClassA", "TypeError", "Failure message",
                    "StackTrace", 0.5);
            var failure2 = new TestFailure("testB", "Test B", "ClassB", "TypeError",
                    "Failure message", "StackTrace", 0.5);

            assertEquals(-1, failure1.compareTo(failure2), "Expected compareTo to return a negative value");
        }

        @Test
        void compareToDifferentClassName() {
            var failure1 = new TestFailure("test1", "Test 1", "ClassA", "TypeError",
                    "Failure message", "StackTrace", 0.5);
            var failure2 = new TestFailure("test1", "Test 2", "ClassB", "TypeError",
                    "Failure message", "StackTrace", 0.5);

            assertEquals(-1, failure1.compareTo(failure2), "Expected compareTo to return a negative value");
        }

        @Test
        void compareToIdenticalObjects() {
            var failure1 = new TestFailure("test1", "Test 1", "ClassA", "TypeError",
                    "Failure message", "StackTrace", 0.5);
            var failure2 = new TestFailure("test1", "Test 1", "ClassA", "TypeError",
                    "Failure message", "StackTrace", 0.5);

            assertEquals(0, failure1.compareTo(failure2), "Expected compareTo to return 0 for identical objects");
        }

        @Test
        void compareToSameClassAndTestName() {
            var failure1 = new TestFailure("test1", "Test 1", "ClassA", "TypeError",
                    "Failure message", "StackTrace", 0.5);
            var failure2 = new TestFailure("test1", "Test 1", "ClassA", "TypeError",
                    "Different failure message", "StackTrace", 0.2);

            assertEquals(0, failure1.compareTo(failure2), "Expected compareTo to return 0");
        }

        @Test
        void compareToSameClassDifferentTestName() {
            var failure1 = new TestFailure("test1", "Test 1", "ClassA", "TypeError",
                    "Failure message", "StackTrace", 0.5);
            var failure2 = new TestFailure("test2", "Test 2", "ClassA", "TypeError",
                    "Failure message", "StackTrace", 0.5);

            assertEquals(-1, failure1.compareTo(failure2), "Expected compareTo to return a negative value");
        }

        @Test
        void compareToSameClassDifferentTestNameDifferentOrder() {
            var failure1 = new TestFailure("test1", "Test 1", "ClassA", "TypeError",
                    "Failure message", "StackTrace", 0.5);
            var failure2 = new TestFailure("test2", "Test 2", "ClassA", "TypeError",
                    "Failure message", "StackTrace", 0.5);

            assertEquals(-1, failure1.compareTo(failure2), "Expected compareTo to return a negative value");

            assertEquals(1, failure2.compareTo(failure1), "Expected compareTo to return a positive value");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        @Test
        void constructorWithEmptyStackTrace() {
            assertThatCode(() -> new TestFailure("test1", "Test 1", "ClassA",
                    "TypeError", "Failure message", null, 1.0))
                    .doesNotThrowAnyException();
        }

        @Test
        void constructorWithTimeLessThanZero() {
            assertThrows(IllegalArgumentException.class, () -> new TestFailure("test1", "Test 1",
                    "ClassA", "TypeError", "Failure message", "StackTrace",
                    -0.5));
        }
    }
}