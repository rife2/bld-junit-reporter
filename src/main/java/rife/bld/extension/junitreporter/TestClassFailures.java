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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents the aggregated test failures for a specific test class.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class TestClassFailures {
    private final String className;
    private final List<TestFailure> failures;
    private final Lock instanceLock = new ReentrantLock();
    private boolean isSorted; // Removed volatile - protected by lock
    private int totalFailures; // Removed volatile - protected by lock
    private double totalTime; // Removed volatile - protected by lock

    /**
     * Constructs a new {@code TestClassFailures} object for a specific test class.
     *
     * @param className the name of the test class; must not be null
     */
    public TestClassFailures(String className) {
        this.className = Objects.requireNonNull(className, "Class name cannot be null");
        this.failures = new ArrayList<>();
        this.totalFailures = 0;
        this.totalTime = 0.0;
        this.isSorted = true; // Empty list is considered sorted
    }

    /**
     * Adds a test failure to the collection of failures for this specific test class.
     *
     * @param failure the {@link TestFailure} object to be added; must not be null
     * @throws NullPointerException if the failure is null
     */
    public void addFailure(TestFailure failure) {
        Objects.requireNonNull(failure, "Failure cannot be null");
        instanceLock.lock();
        try {
            failures.add(failure);
            totalFailures++;
            totalTime += failure.time();
            isSorted = false; // Mark as unsorted when adding a new failure
        } finally {
            instanceLock.unlock();
        }
    }

    /**
     * Retrieves the name of the test class associated with the test failures.
     *
     * @return the name of the test class as a non-null string
     */
    public String getClassName() {
        return className;
    }

    /**
     * Retrieves a sorted list of all test failures associated with this test class.
     * <p>
     * The failures are sorted by class name first, then by test name.
     *
     * @return a sorted list of {@link TestFailure} objects representing all test failures for this test class
     */
    public List<TestFailure> getFailures() {
        instanceLock.lock();
        try {
            if (!isSorted) {
                failures.sort(null); // Uses the natural ordering defined by Comparable
                isSorted = true;
            }
            return new ArrayList<>(failures); // Return copy to avoid concurrent modification
        } finally {
            instanceLock.unlock();
        }
    }

    /**
     * Retrieves the total number of test failures associated with a specific test class.
     *
     * @return the total count of test failures as an integer
     */
    public int getTotalFailures() {
        instanceLock.lock();
        try {
            return totalFailures;
        } finally {
            instanceLock.unlock();
        }
    }

    /**
     * Retrieves the total execution time of the test failures associated with a specific test class.
     *
     * @return the total execution time as a double
     */
    public double getTotalTime() {
        instanceLock.lock();
        try {
            return totalTime;
        } finally {
            instanceLock.unlock();
        }
    }

    /**
     * Calculates the hash code for this object.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(className);
    }

    /**
     * Determines if this object is equal to another object.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise
     */
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

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        instanceLock.lock();
        try {
            return "TestClassFailures{" +
                    "className='" + className + '\'' +
                    ", totalFailures=" + totalFailures +
                    ", totalTime=" + totalTime +
                    '}';
        } finally {
            instanceLock.unlock();
        }
    }

    /**
     * Sorts the failures in-place. This method is called internally to ensure
     * failures are sorted before the final results are returned.
     */
    public void sortFailures() {
        instanceLock.lock();
        try {
            if (!isSorted) {
                failures.sort(null); // Uses the natural ordering defined by Comparable
                isSorted = true;
            }
        } finally {
            instanceLock.unlock();
        }
    }
}