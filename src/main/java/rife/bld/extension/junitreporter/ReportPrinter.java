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

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for printing detailed and structured reports of test failures.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.SystemPrintln")
public final class ReportPrinter {
    private ReportPrinter() {
        // Prevent instantiation
    }

    /**
     * Retrieves the test class failures associated with a specific group index from the provided map.
     *
     * @param groupedFailures a map where the key is a string representing the group name, and the value is a
     *                        {@link TestClassFailures} object representing the failures for that group
     * @param index           the index of the group whose failures are to be retrieved
     * @return the {@link TestClassFailures} object associated with the specified group index
     * @throws IllegalArgumentException  if the map is {@code null} or empty
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public static TestClassFailures getFailuresByGroupIndex(Map<String, TestClassFailures> groupedFailures,
                                                            int index)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        if (groupedFailures == null || groupedFailures.isEmpty()) {
            throw new IllegalArgumentException("The grouped failures cannot be null or empty");
        }

        if (index < 0 || index >= groupedFailures.size()) {
            throw new IndexOutOfBoundsException("The group index is out of bounds");
        }

        var values = new ArrayList<>(groupedFailures.values());
        return values.get(index);
    }

    /**
     * Indents each line of the input text by the specified number of spaces.
     *
     * @param text       The input text containing one or more lines
     * @param indentSize The number of spaces to indent each line
     * @return The indented text with each line prefixed by the specified number of spaces
     */
    public static String indent(String text, int indentSize) throws IllegalArgumentException {
        if (text == null) {
            return null;
        }

        if (indentSize <= 0) {
            if (indentSize == 0) {
                return text;
            }
            throw new IllegalArgumentException("Indent size cannot be negative");
        }

        if (text.isEmpty()) {
            return text;
        }

        var indent = " ".repeat(indentSize);

        return text.lines()  // More efficient than split() + stream()
                .map(line -> indent + line)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Indents each line of the input text by 8 spaces (default indentation).
     *
     * @param text The input text containing one or more lines
     * @return The indented text with each line prefixed by 8 spaces
     */
    public static String indent(String text) {
        return indent(text, 8);
    }


    /**
     * Prints the details of test failures based on the given argument and grouped failures.
     *
     * @param arg             a string representing the group index or group and failure indices
     *                        in the format {@code groupIndex} or {@code groupIndex.failureIndex}
     * @param groupedFailures a map where the key is a string representing the group name, and the
     *                        value is a {@link TestClassFailures} object containing the failures
     *                        for that group
     * @throws NumberFormatException     if the numeric portion of the argument cannot be parsed
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
     */
    public static void printDetails(String arg, Map<String, TestClassFailures> groupedFailures)
            throws NumberFormatException, IndexOutOfBoundsException {
        var dotIndex = arg.indexOf('.');
        if (dotIndex == -1) {
            var groupIndex = Integer.parseInt(arg) - 1;
            var classFailures = getFailuresByGroupIndex(groupedFailures, groupIndex);
            printFailures(classFailures, groupIndex);
        } else {
            var groupIndex = Integer.parseInt(arg.substring(0, dotIndex)) - 1;
            var failureIndex = Integer.parseInt(arg.substring(dotIndex + 1)) - 1;
            var classFailures = getFailuresByGroupIndex(groupedFailures, groupIndex);

            if (failureIndex >= classFailures.getFailures().size()) {
                throw new IndexOutOfBoundsException("The failure index is out of bounds");
            }

            printFailureWithStackTrace(classFailures.getFailures().get(failureIndex), groupIndex, failureIndex);
        }
    }

    /**
     * Prints details of a single test failure, including optional group and failure indices.
     *
     * @param failure      The {@link TestFailure} object that encapsulates the details of the test failure
     * @param groupIndex   The index of the group this failure belongs to, or {@code null} if not applicable
     * @param failureIndex The index of the failure within the group, or {@code null} if not applicable
     */
    public static void printFailure(TestFailure failure, Integer groupIndex, Integer failureIndex) {
        var prefix = (groupIndex != null && failureIndex != null)
                ? String.format("[%d.%d] ", groupIndex, failureIndex)
                : "";

        var output = String.format("%sTest: %s%n" +
                        "    - Name: %s%n" +
                        "    - Type: %s%n" +
                        "    - Message:%n%s%n" +
                        "    - Time: %ss",
                prefix,
                failure.testName(),
                failure.displayName(),
                failure.failureType(),
                indent(failure.failureMessage().trim()),
                failure.time());

        System.out.println(output);
    }

    /**
     * Prints the failure details, including its formatted group and failure indices, and displays the associated
     * stack trace.
     *
     * @param failure      The {@link TestFailure} object representing the test failure details
     * @param groupIndex   The index of the group this failure belongs to
     * @param failureIndex The index of the failure within the group
     */
    public static void printFailureWithStackTrace(TestFailure failure, int groupIndex, int failureIndex) {
        var groupIndexOffset = groupIndex + 1;
        printHeader(String.format("[%d] %s", groupIndexOffset, failure.className()));
        printFailure(failure, groupIndexOffset, failureIndex + 1);
        printStackTrace(failure);
    }

    /**
     * Prints the failures of a specific test class along with their group and failure indices.
     *
     * @param failures   the {@link TestClassFailures} object containing the test failures associated with a specific
     *                   test class
     * @param groupIndex the index of the group to which the test class belongs
     */
    public static void printFailures(TestClassFailures failures, int groupIndex) {
        var groupIndexOffset = groupIndex + 1;
        printHeader(String.format("[%d] %s", groupIndexOffset, failures.getClassName()));

        var failureCount = 1;
        for (var failure : failures.getFailures()) {
            printFailure(failure, groupIndexOffset, failureCount++);
            System.out.println();
        }
    }

    /**
     * Prints a header with the specified title.
     *
     * @param title the title to print
     */
    public static void printHeader(String title) {
        System.out.println();
        var separatorLength = Math.max(title.length(), 50);
        var separator = "-".repeat(separatorLength);

        var header = String.format("%s%n%s%n%s%n%n", separator, title, separator);

        System.out.print(header);
    }

    /**
     * Prints the stack trace of the provided test failure, if available.
     *
     * @param failure The {@link TestFailure} object containing the details and stack trace of the test failure to be
     *                printed
     */
    public static void printStackTrace(TestFailure failure) {
        var stackTrace = failure.stackTrace();
        if (!stackTrace.isEmpty()) {
            System.out.printf("    - Trace:%n%s%n", indent(stackTrace));
        }
    }

    /**
     * Prints a summary of JUnit test failures grouped by test class.
     *
     * @param groupedFailures a map where the key is a string representing the group name, and the value
     *                        is a {@link TestClassFailures} object that contains the failures for that group
     */
    public static void printSummary(Map<String, TestClassFailures> groupedFailures) {
        printHeader("JUnit Failures Summary");

        var sb = new StringBuilder(1024); // Pre-allocate for better performance
        var groupCount = 0;

        for (var classFailures : groupedFailures.values()) {
            groupCount++;
            sb.append(String.format("[%d] %s (%d failures, %.3fs)%n",
                    groupCount,
                    classFailures.getClassName(),
                    classFailures.getTotalFailures(),
                    classFailures.getTotalTime()));

            var failureCount = 0;
            for (var failure : classFailures.getFailures()) {
                failureCount++;
                String testName;
                if (failure.displayName().isBlank()
                        || failure.displayName().equals(failure.testName())) {
                    testName = failure.testName();
                } else {
                    testName = failure.testName() + " (" + failure.displayName() + ')';
                }
                sb.append(String.format("  - [%d.%d] %s%n", groupCount, failureCount, testName));
            }
        }

        System.out.print(sb);

        var totalFailures = groupedFailures.values().stream()
                .mapToInt(TestClassFailures::getTotalFailures)
                .sum();
        System.out.printf("%nTotal Failures: %d%n", totalFailures);
    }
}
