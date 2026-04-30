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

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for printing detailed and structured reports of test failures.
 * <p>
 * All index parameters are 0-based internally. Public method {@link #printDetails}
 * accepts 1-based indices from CLI input.
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
     * Returns the {@code TestClassFailures} at the given 0-based index.
     * <p>
     * Assumes {@code groupedFailures} iteration order is stable.
     *
     * @param groupedFailures a map of test class failures
     * @param index           the 0-based index
     * @return the {@link TestClassFailures} at the specified index
     * @throws IllegalArgumentException  if the map is {@code null} or empty
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public static TestClassFailures getFailuresByIndex(Map<String, TestClassFailures> groupedFailures, int index) {
        if (groupedFailures == null || groupedFailures.isEmpty()) {
            throw new IllegalArgumentException("The grouped failures cannot be null or empty");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("The group index cannot be negative");
        }

        var i = 0;
        for (var entry : groupedFailures.values()) {
            if (i == index) {
                return entry;
            }
            i++;
        }
        throw new IndexOutOfBoundsException("The group index is out of bounds");
    }

    /**
     * Indents each line of the input text by the specified number of spaces.
     *
     * @param text       the input text; may be null or empty
     * @param indentSize the number of spaces to indent each line
     * @return the indented text, or empty string if input is null/empty
     * @throws IllegalArgumentException if {@code indentSize} is negative
     */
    public static String indent(String text, int indentSize) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (indentSize < 0) {
            throw new IllegalArgumentException("Indent size cannot be negative");
        }
        if (indentSize == 0) {
            return text;
        }

        var indent = " ".repeat(indentSize);
        return text.lines()
                .map(line -> indent + line)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Indents each line of the input text by 8 spaces.
     *
     * @param text the input text
     * @return the indented text with each line prefixed by 8 spaces
     */
    public static String indent(String text) {
        return indent(text, 8);
    }

    /**
     * Prints the details of test failures based on 1-based CLI argument.
     * <p>
     * Accepts {@code groupIndex} or {@code groupIndex.failureIndex}.
     * Converts 1-based input to 0-based internally.
     *
     * @param arg             a string representing 1-based indices
     * @param groupedFailures a map of test class failures
     * @throws NumberFormatException     if the numeric portion cannot be parsed
     * @throws IndexOutOfBoundsException if the specified indices are out of bounds
     */
    public static void printDetails(String arg, Map<String, TestClassFailures> groupedFailures) {
        var dotIndex = arg.indexOf('.');
        if (dotIndex == -1) {
            var groupIndex = Integer.parseInt(arg) - 1;
            var classFailures = getFailuresByIndex(groupedFailures, groupIndex);
            printFailures(classFailures, groupIndex);
        } else {
            var groupIndex = Integer.parseInt(arg.substring(0, dotIndex)) - 1;
            var failureIndex = Integer.parseInt(arg.substring(dotIndex + 1)) - 1;
            var classFailures = getFailuresByIndex(groupedFailures, groupIndex);

            var failures = classFailures.getFailures();
            if (failureIndex < 0 || failureIndex >= failures.size()) {
                throw new IndexOutOfBoundsException("The failure index is out of bounds");
            }

            printFailureWithStackTrace(failures.get(failureIndex), groupIndex, failureIndex);
        }
    }

    /**
     * Prints details of a single test failure with optional indices.
     *
     * @param failure      the {@link TestFailure} to print
     * @param groupIndex   the 0-based group index, or {@code null}
     * @param failureIndex the 0-based failure index, or {@code null}
     */
    public static void printFailure(TestFailure failure, Integer groupIndex, Integer failureIndex) {
        var prefix = (groupIndex != null && failureIndex != null)
                ? String.format("[%d.%d] ", groupIndex + 1, failureIndex + 1)
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
     * Prints failure details with header and stack trace using 0-based indices.
     *
     * @param failure      the {@link TestFailure} to print
     * @param groupIndex   the 0-based group index
     * @param failureIndex the 0-based failure index
     */
    public static void printFailureWithStackTrace(TestFailure failure, int groupIndex, int failureIndex) {
        printHeader(String.format("[%d] %s", groupIndex + 1, failure.className()));
        printFailure(failure, groupIndex, failureIndex);
        printStackTrace(failure);
    }

    /**
     * Prints all failures for a test class using 0-based group index.
     *
     * @param failures   the {@link TestClassFailures} to print
     * @param groupIndex the 0-based group index
     */
    public static void printFailures(TestClassFailures failures, int groupIndex) {
        printHeader(String.format("[%d] %s", groupIndex + 1, failures.getClassName()));

        var failureList = failures.getFailures();
        for (var i = 0; i < failureList.size(); i++) {
            printFailure(failureList.get(i), groupIndex, i);
            System.out.println();
        }
    }

    /**
     * Prints a header with the specified title.
     *
     * @param title the title to print
     */
    public static void printHeader(String title) {
        var separatorLength = Math.max(title.length(), 50);
        var separator = "-".repeat(separatorLength);
        System.out.printf("%s%n%s%n%s%n%n", separator, title, separator);
    }

    /**
     * Prints the stack trace of the provided test failure, if available.
     *
     * @param failure the {@link TestFailure} containing the stack trace
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
     * @param groupedFailures a map of test class failures
     */
    public static void printSummary(Map<String, TestClassFailures> groupedFailures) {
        printHeader("JUnit Failures Summary");

        var sb = new StringBuilder(1024);
        var groupCount = 0;

        for (var classFailures : groupedFailures.values()) {
            groupCount++;
            sb.append(String.format("[%d] %s (%d failures, %.3fs)%n",
                    groupCount,
                    classFailures.getClassName(),
                    classFailures.getTotalFailures(),
                    classFailures.getTotalTime()));

            var failures = classFailures.getFailures();
            for (var i = 0; i < failures.size(); i++) {
                var failure = failures.get(i);
                String testName;
                if (failure.displayName().isBlank() || failure.displayName().equals(failure.testName())) {
                    testName = failure.testName();
                } else {
                    testName = failure.testName() + " (" + failure.displayName() + ')';
                }
                sb.append(String.format("  - [%d.%d] %s%n", groupCount, i + 1, testName));
            }
        }

        System.out.print(sb);

        var totalFailures = groupedFailures.values().stream()
                .mapToInt(TestClassFailures::getTotalFailures)
                .sum();
        System.out.printf("%nTotal Failures: %d%n", totalFailures);
    }
}