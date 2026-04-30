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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates test failures for a specific test class.
 * <p>
 * Instances are not thread-safe and intended for single-threaded parsing.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestClassFailures {

    private final String className;
    private final List<TestFailure> failures;

    /**
     * Constructs a new {@code TestClassFailures} for a specific test class.
     *
     * @param className the name of the test class; must not be null
     */
    public TestClassFailures(String className) {
        this.className = Objects.requireNonNull(className, "Class name cannot be null");
        this.failures = new ArrayList<>();
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        var that = (TestClassFailures) obj;
        return Objects.equals(className, that.className);
    }

    @Override
    public String toString() {
        return "TestClassFailures{" +
                "className='" + className + '\'' +
                ", totalFailures=" + getTotalFailures() +
                ", totalTime=" + getTotalTime() +
                '}';
    }

    /**
     * Adds a test failure to this class.
     *
     * @param failure the {@link TestFailure} to add; must not be null
     * @throws NullPointerException if the failure is null
     */
    public void addFailure(TestFailure failure) {
        failures.add(Objects.requireNonNull(failure, "Failure cannot be null"));
    }

    /**
     * Returns the test class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns an unmodifiable list of failures sorted by test name.
     *
     * @return a sorted, unmodifiable list of {@link TestFailure} objects
     */
    public List<TestFailure> getFailures() {
        Collections.sort(failures);
        return Collections.unmodifiableList(failures);
    }

    /**
     * Returns the total number of failures.
     *
     * @return the failure count
     */
    public int getTotalFailures() {
        return failures.size();
    }

    /**
     * Returns the total execution time of all failures.
     *
     * @return the sum of failure times
     */
    public double getTotalTime() {
        return failures.stream().mapToDouble(TestFailure::time).sum();
    }
}