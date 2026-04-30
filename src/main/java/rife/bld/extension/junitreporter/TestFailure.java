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

import java.util.Objects;

/**
 * Immutable record representing a test case failure extracted from JUnit XML.
 * <p>
 * Instances are naturally ordered by class name, then test name for consistent
 * sorting within {@link TestClassFailures}.
 *
 * @param testName       the test method name; never null
 * @param displayName    the JUnit display name extracted from {@code system-out}; empty string if not present
 * @param className      the fully qualified test class name; never null
 * @param failureType    the exception class name from the {@code type} attribute; never null
 * @param failureMessage the failure message from the {@code message} attribute; never null
 * @param stackTrace     the stack trace content of the {@code failure} or {@code error} element; empty string if not present
 * @param time           the execution time in seconds from the {@code time} attribute; must be >= 0
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public record TestFailure(String testName, String displayName, String className, String failureType,
                          String failureMessage, String stackTrace, double time)
        implements Comparable<TestFailure> {

    /**
     * Constructs a new {@code TestFailure} instance.
     * <p>
     * Normalizes {@code null} values for {@code displayName} and {@code stackTrace} to empty strings.
     *
     * @param testName       the test method name; must not be null
     * @param displayName    the JUnit display name; may be null, normalized to empty string
     * @param className      the fully qualified test class name; must not be null
     * @param failureType    the exception class name; must not be null
     * @param failureMessage the failure message; must not be null
     * @param stackTrace     the stack trace; may be null, normalized to empty string
     * @param time           the execution time in seconds; must be >= 0
     * @throws NullPointerException     if {@code testName}, {@code className}, {@code failureType},
     *                                  or {@code failureMessage} is null
     * @throws IllegalArgumentException if {@code time} is negative
     */
    public TestFailure {
        Objects.requireNonNull(testName, "Test name cannot be null");
        Objects.requireNonNull(className, "Class name cannot be null");
        Objects.requireNonNull(failureType, "Failure type cannot be null");
        Objects.requireNonNull(failureMessage, "Failure message cannot be null");
        displayName = displayName != null ? displayName : "";
        stackTrace = stackTrace != null ? stackTrace : "";

        if (time < 0) {
            throw new IllegalArgumentException("Time cannot be negative");
        }
    }

    /**
     * Compares this failure to another by class name, then test name.
     * <p>
     * This ordering is consistent with {@code equals} and ensures failures
     * are grouped by class when sorted.
     *
     * @param other the other {@code TestFailure} to compare to
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object
     */
    @Override
    public int compareTo(TestFailure other) {
        var result = className.compareTo(other.className);
        return result != 0 ? result : testName.compareTo(other.testName);
    }
}