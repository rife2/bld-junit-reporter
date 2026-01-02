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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TestClassFailuresTest {
    @Test
    void checkFailuresAreSorted() {
        var testClassFailures = new TestClassFailures("TestClass");

        var failure1 = new TestFailure(
                "testB", "Test B", "TestClass", "ExceptionType",
                "Message", "StackTrace", 1.0
        );

        var failure2 = new TestFailure(
                "testA", "Test A", "TestClass", "ExceptionType",
                "Message", "StackTrace", 1.5
        );

        testClassFailures.addFailure(failure1);
        testClassFailures.addFailure(failure2);

        testClassFailures.sortFailures();
        var failures = testClassFailures.getFailures();

        assertThat(failures.get(0)).isEqualTo(failure2);
        assertThat(failures.get(1)).isEqualTo(failure1);
    }

    @Test
    void handleEmptyFailures() {
        var testClassFailures = new TestClassFailures("TestClass");

        var failures = testClassFailures.getFailures();

        assertThat(failures).isEmpty();
        assertThat(testClassFailures.getTotalFailures()).isZero();
        assertThat(testClassFailures.getTotalTime()).isZero();
    }

    @Nested
    @DisplayName("Add Failure Tests")
    class addFailureTests {
        @Test
        void addMultipleFailures() {
            var testClassFailures = new TestClassFailures("TestClass");

            var failure1 = new TestFailure(
                    "test1", "Test 1", "TestClass", "AssertionError",
                    "First failure", "StackTrace1", 1.2
            );

            var failure2 = new TestFailure(
                    "test2", "Test 2", "TestClass", "NullPointerException",
                    "Second failure", "StackTrace2", 0.8
            );

            testClassFailures.addFailure(failure1);
            testClassFailures.addFailure(failure2);

            List<TestFailure> failures = testClassFailures.getFailures();

            assertThat(testClassFailures.getTotalFailures()).isEqualTo(2);
            assertThat(testClassFailures.getTotalTime()).isEqualTo(2.0);
            assertThat(failures.get(0)).isEqualTo(failure1);
            assertThat(failures.get(1)).isEqualTo(failure2);
        }

        @Test
        void addNullFailureThrowsException() {
            var testClassFailures = new TestClassFailures("TestClass");

            assertThatThrownBy(() -> testClassFailures.addFailure(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void addSingleFailure() {
            var testClassFailures = new TestClassFailures("TestClass");

            var failure = new TestFailure(
                    "test1", "Test 1", "TestClass", "AssertionError",
                    "Expected value to be true", "StackTrace", 1.5
            );

            testClassFailures.addFailure(failure);

            var failures = testClassFailures.getFailures();

            assertThat(testClassFailures.getTotalFailures()).isOne();
            assertThat(testClassFailures.getTotalTime()).isEqualTo(1.5);
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0)).isEqualTo(failure);
        }
    }

    @Nested
    @DisplayName("Equals Method Tests")
    class equalsMethodTest {
        @Test
        void equalsDifferentInstances() {
            var instance1 = new TestClassFailures("TestClass1");
            var instance2 = new TestClassFailures("TestClass2");

            assertThat(instance1).isNotEqualTo(instance2);
        }

        @Test
        void equalsMatchingInstances() {
            var instance1 = new TestClassFailures("TestClass");
            var instance2 = new TestClassFailures("TestClass");

            assertThat(instance1).isEqualTo(instance2);
        }

        @Test
        void equalsSameInstances() {
            var instance = new TestClassFailures("TestClass");

            assertThat(instance).isEqualTo(instance);
        }

        @Test
        void equalsWithDifferentClassNames() {
            var instance1 = new TestClassFailures("TestClass1");
            var instance2 = new TestClassFailures("TestClass2");

            assertThat(instance1).isNotEqualTo(instance2);
        }

        @Test
        void equalsWithDifferentType() {
            var instance = new TestClassFailures("TestClass");
            var otherObject = new Object();

            assertThat(instance).isNotEqualTo(otherObject);
        }

        @Test
        void equalsWithNull() {
            var instance = new TestClassFailures("TestClass");
            assertThat(instance).isNotEqualTo(null);
        }

        @Test
        void equalsWithSameClassNamesDifferentFailures() {
            var instance1 = new TestClassFailures("TestClass");
            var instance2 = new TestClassFailures("TestClass");

            var failure1 = new TestFailure(
                    "test1", "Test 1", "TestClass", "TypeError",
                    "Error Message", "StackTrace", 1.0
            );

            instance1.addFailure(failure1);

            assertThat(instance1).isEqualTo(instance2);
        }
    }

    @Nested
    @DisplayName("HashCode Method Tests")
    class hashCodeTest {
        @Test
        void hashCodeDifferentForDifferentInstances() {
            var instance1 = new TestClassFailures("TestClass1");
            var instance2 = new TestClassFailures("TestClass2");

            assertThat(instance1.hashCode()).isNotEqualTo(instance2.hashCode());
        }

        @Test
        void hashCodeMatchingForEqualInstances() {
            var instance1 = new TestClassFailures("TestClass");
            var instance2 = new TestClassFailures("TestClass");

            assertThat(instance1.hashCode()).isEqualTo(instance2.hashCode());
        }
    }

    @Nested
    @DisplayName("Sort Failures Method Tests")
    class sortFailuresTests {
        @Test
        void sortFailuresWithAlreadySortedList() {
            var testClassFailures = new TestClassFailures("TestClass");

            var failure1 = new TestFailure(
                    "testA", "Test A", "TestClass", "ExceptionType",
                    "Message", "StackTrace", 0.5
            );

            var failure2 = new TestFailure(
                    "testB", "Test B", "TestClass", "ExceptionType",
                    "Message", "StackTrace", 1.0
            );

            testClassFailures.addFailure(failure1);
            testClassFailures.addFailure(failure2);

            testClassFailures.sortFailures();
            var failures = testClassFailures.getFailures();

            assertThat(failures.get(0)).isEqualTo(failure1);
            assertThat(failures.get(1)).isEqualTo(failure2);
        }

        @Test
        void sortFailuresWithNoFailures() {
            var testClassFailures = new TestClassFailures("TestClass");

            testClassFailures.sortFailures();
            var failures = testClassFailures.getFailures();

            assertThat(failures).isEmpty();
        }

        @Test
        void sortFailuresWithUnorderedList() {
            var testClassFailures = new TestClassFailures("TestClass");

            var failure1 = new TestFailure(
                    "testB", "Test B", "TestClass", "ExceptionType",
                    "Message", "StackTrace", 1.0
            );

            var failure2 = new TestFailure(
                    "testA", "Test A", "TestClass", "ExceptionType",
                    "Message", "StackTrace", 0.5
            );

            testClassFailures.addFailure(failure1);
            testClassFailures.addFailure(failure2);

            testClassFailures.sortFailures();
            var failures = testClassFailures.getFailures();

            assertThat(failures.get(0)).isEqualTo(failure2);
            assertThat(failures.get(1)).isEqualTo(failure1);
        }
    }

    @Nested
    @DisplayName("ToString Method Tests")
    class toStringMethodTests {
        @Test
        void validateToStringForEmptyFailures() {
            var testClassFailures = new TestClassFailures("TestClass");

            var result = testClassFailures.toString();

            assertThat(result).isEqualTo(
                    "TestClassFailures{className='TestClass', totalFailures=0, totalTime=0.0}"
            );
        }

        @Test
        void validateToStringForNonEmptyFailures() {
            var testClassFailures = new TestClassFailures("TestClass");

            var failure1 = new TestFailure(
                    "test1", "Test 1", "TestClass", "ExceptionType",
                    "Message", "StackTrace", 1.0
            );

            var failure2 = new TestFailure(
                    "test2", "Test 2", "TestClass", "AnotherExceptionType",
                    "Another Message", "Another StackTrace", 2.0
            );

            testClassFailures.addFailure(failure1);
            testClassFailures.addFailure(failure2);

            var result = testClassFailures.toString();

            assertThat(result).isEqualTo(
                    "TestClassFailures{className='TestClass', totalFailures=2, totalTime=3.0}"
            );
        }
    }
}