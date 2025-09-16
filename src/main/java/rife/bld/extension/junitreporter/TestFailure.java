/*
 * Copyright 2025 the original author or authors.
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

import java.util.Objects;

/**
 * Represents a failure that occurred during the execution of a specific test case in a test suite.
 *
 * @param testName       The name of the test case.
 * @param displayName    The display name of the test case.
 * @param className      The name of the test class.
 * @param failureType    The type of the failure.
 * @param failureMessage The failure message.
 * @param stackTrace     The stack trace of the failure.
 * @param time           The execution time of the test case.
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings({"PMD.OverrideBothEqualsAndHashCodeOnComparable", "PMD.DanglingJavadoc"})
public record TestFailure(String testName, String displayName, String className, String failureType,
                          String failureMessage, String stackTrace, double time)
        implements Comparable<TestFailure> {
    /**
     * Constructs a new {@code TestFailure} object.
     *
     * @param testName       The name of the test case.
     * @param displayName    The display name of the test case.
     * @param className      The name of the test class.
     * @param failureType    The type of failure that occurred.
     * @param failureMessage The detailed message describing the failure.
     * @param stackTrace     The stack trace of the failure.
     * @param time           The execution time of the test case.
     * @throws NullPointerException     If any of the required parameters are null.
     * @throws IllegalArgumentException If the execution time is negative.
     */
    public TestFailure {
        Objects.requireNonNull(testName, "Test name cannot be null");
        Objects.requireNonNull(className, "Class name cannot be null");
        Objects.requireNonNull(failureType, "Failure type cannot be null");
        Objects.requireNonNull(failureMessage, "Failure message cannot be null");
        stackTrace = stackTrace != null ? stackTrace : ""; // Allow null, convert to empty

        if (time < 0) {
            throw new IllegalArgumentException("Time cannot be negative");
        }
    }

    /**
     * Compares this object to another object.
     *
     * @param other the object to compare to
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     * the specified object
     */
    @Override
    public int compareTo(TestFailure other) {
        var result = className.compareTo(other.className);
        return result != 0 ? result : testName.compareTo(other.testName);
    }
}
