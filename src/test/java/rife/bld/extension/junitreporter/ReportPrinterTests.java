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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static rife.bld.extension.junitreporter.JUnitXmlParser.EOL;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ReportPrinterTests {
    @Nested
    @DisplayName("Failures By Group Index Tests")
    class FailuresByGroupIndexTests {
        @Test
        void failuresByGroupIndexWithEmptyMap() {
            assertThatThrownBy(() -> ReportPrinter.getFailuresByGroupIndex(Map.of(), 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("The grouped failures cannot be null or empty");
        }

        @Test
        void failuresByGroupIndexWithNegativeIndex() {
            var groupedFailures = Map.of(
                    "TestClass1", new TestClassFailures("TestClass1")
            );

            assertThatThrownBy(() -> ReportPrinter.getFailuresByGroupIndex(groupedFailures, -1))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("The group index is out of bounds");
        }

        @ParameterizedTest
        @NullSource
        void failuresByGroupIndexWithNullMap(Map<String, TestClassFailures> input) {
            assertThatThrownBy(() -> ReportPrinter.getFailuresByGroupIndex(input, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("The grouped failures cannot be null or empty");
        }

        @Test
        void failuresByGroupIndexWithOutOfBoundsIndex() {
            var groupedFailures = Map.of(
                    "TestClass1", new TestClassFailures("TestClass1")
            );

            assertThatThrownBy(() -> ReportPrinter.getFailuresByGroupIndex(groupedFailures, 2))
                    .isInstanceOf(IndexOutOfBoundsException.class)
                    .hasMessage("The group index is out of bounds");
        }

        @Test
        void failuresByGroupIndexWithValidIndex() {
            var groupedFailures = new TreeMap<String, TestClassFailures>();
            groupedFailures.put("TestClass1", new TestClassFailures("TestClass1"));
            groupedFailures.put("TestClass2", new TestClassFailures("TestClass2"));

            var result = ReportPrinter.getFailuresByGroupIndex(groupedFailures, 1);

            assertThat(result.getClassName()).isEqualTo("TestClass2");
        }
    }

    @Nested
    @DisplayName("Indent Method Tests")
    class IndentMethodTests {
        @Test
        void identWithZeroIndentSize() {
            var input = "test";
            var expected = "test";
            assertThat(ReportPrinter.indent(input, 0)).isEqualTo(expected);
        }

        @Test
        void indentEmptyStringWithNonDefaultSize() {
            var input = "";
            var expected = "";

            assertThat(ReportPrinter.indent(input, 6)).isEqualTo(expected);
        }

        @Test
        void indentHandlesSpecialCharacters() {
            var input = "text with special characters:" + EOL + "\t- newline" + EOL + "\t- tab";
            var expected = "  text with special characters:" + EOL + "  \t- newline" + EOL + "  \t- tab";

            assertThat(ReportPrinter.indent(input, 2)).isEqualTo(expected);
        }

        @Test
        void indentMultiLineTextWithNonDefaultSize() {
            var input = "line1" + EOL + "line2" + EOL + "line3";
            var expected = "    line1" + EOL + "    line2" + EOL + "    line3";

            assertThat(ReportPrinter.indent(input, 4)).isEqualTo(expected);
        }

        @Test
        void indentSingleLineTextWithNonDefaultSize() {
            var input = "sample";
            var expected = "     sample";

            assertThat(ReportPrinter.indent(input, 5)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{index} ''{0}''")
        @ValueSource(strings = {" ", "  "})
        void indentWithBlankString(String input) {
            assertThat(ReportPrinter.indent(input)).isEqualTo("        " + input);
        }

        @Test
        void indentWithCustomSize() {
            var input = "test";
            var expected = "   test";

            assertThat(ReportPrinter.indent(input, 3)).isEqualTo(expected);
        }

        @Test
        void indentWithDefaultSize() {
            var input = "line1" + EOL + "line2";
            var expected = "        line1" + EOL + "        line2";

            assertThat(ReportPrinter.indent(input)).isEqualTo(expected);
        }

        @Test
        void indentWithEmptyString() {
            var input = "";
            var expected = "";
            assertThat(ReportPrinter.indent(input)).isEqualTo(expected);
        }

        @Test
        void indentWithNegativeIndentSize() {
            var input = "test";

            assertThatThrownBy(() -> ReportPrinter.indent(input, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Indent size cannot be negative");
        }

        @ParameterizedTest
        @NullSource
        void indentWithNullString(String input) {
            assertThat(ReportPrinter.indent(input)).isNull();
        }
    }

    @Nested
    @DisplayName("Print Failure Tests")
    class PrintFailureTests {
        @Test
        void printFailureWithNullFailureIndex() {
            var failure = new TestFailure("testMethod", "Test Method", "TestClass", "AssertionError",
                    "Test failed message", "", 0.123);

            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            ReportPrinter.printFailure(failure, 1, null);

            var expectedOutput = """
                    Test: testMethod
                        - Name: Test Method
                        - Type: AssertionError
                        - Message:
                            Test failed message
                        - Time: 0.123
                    """;

            assertThat(outContent.toString().trim()).isEqualTo(expectedOutput.trim());

            System.setOut(System.out);
        }

        @Test
        void printFailureWithNullIndices() {
            var failure = new TestFailure("testMethod", "Test Method", "TestClass", "AssertionError",
                    "Test failed message", "", 0.123);

            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            ReportPrinter.printFailure(failure, null, null);

            var expectedOutput = """
                    Test: testMethod
                        - Name: Test Method
                        - Type: AssertionError
                        - Message:
                            Test failed message
                        - Time: 0.123
                    """;

            assertThat(outContent.toString().trim()).isEqualTo(expectedOutput.trim());

            System.setOut(System.out);
        }

        @Test
        void printFailureWithValidIndices() {
            var failure = new TestFailure("testMethod", "Test Method", "TestClass", "AssertionError",
                    "Test failed message", "", 0.123);

            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            ReportPrinter.printFailure(failure, 1, 2);

            var expectedOutput = """
                    [1.2] Test: testMethod
                        - Name: Test Method
                        - Type: AssertionError
                        - Message:
                            Test failed message
                        - Time: 0.123
                    """;

            assertThat(outContent.toString().trim()).isEqualTo(expectedOutput.trim());

            System.setOut(System.out);
        }
    }

    @Nested
    @DisplayName("Print Stack Trace Tests")
    class PrintStackTraceTests {
        @Test
        void doesNotPrintWhenStackTraceEmpty() {
            var failureWithEmptyStackTrace = new TestFailure(
                    "testMethod",
                    "Test Method",
                    "TestClass",
                    "AssertionError",
                    "Message: Test failed",
                    "",
                    0.123
            );

            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            ReportPrinter.printStackTrace(failureWithEmptyStackTrace);

            assertThat(outContent.toString()).isEmpty();

            System.setOut(System.out);
        }

        @Test
        void printsStackTraceWithIndentation() {
            var failure = new TestFailure(
                    "testMethod",
                    "Test Method",
                    "TestClass",
                    "AssertionError",
                    "Message: Test failed",
                    "line1" + EOL + "line2" + EOL + "line3",
                    0.123
            );

            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            ReportPrinter.printStackTrace(failure);

            var expectedOutput = "    - Trace:" + EOL + "        line1" + EOL + "        line2" + EOL
                    + "        line3" + EOL;
            assertThat(outContent.toString()).isEqualTo(expectedOutput);

            System.setOut(System.out);
        }
    }

    @Nested
    @DisplayName("Print Summary Tests")
    class PrintSummaryTests {
        @Test
        void printSummaryWithEmptyGroupedFailures() {
            var groupedFailures = Map.<String, TestClassFailures>of();

            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            ReportPrinter.printSummary(groupedFailures);

            var expectedOutput = String.format(
                    "%n--------------------------------------------------%n" +
                            "JUnit Failures Summary%n" +
                            "--------------------------------------------------%n%n" +
                            "%nTotal Failures: 0%n"
            );
            assertThat(outContent.toString()).isEqualTo(expectedOutput);

            System.setOut(System.out);
        }

        @Test
        @SuppressWarnings("ExtractMethodRecommender")
        void printSummaryWithNonEmptyGroupedFailures() {
            var failure1 = new TestFailure("testMethod1", "Test Method 1", "TestClass1", "AssertionError",
                    "Message: Test failed 1", "", 0.101);
            var failure2 = new TestFailure("testMethod2", "", "TestClass1", "AssertionError",
                    "Message: Test failed 2", "", 0.202);

            var failuresForClass1 = new TestClassFailures("TestClass1");
            failuresForClass1.addFailure(failure1);
            failuresForClass1.addFailure(failure2);

            var groupedFailures = Map.of("TestClass1", failuresForClass1);

            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            ReportPrinter.printSummary(groupedFailures);

            var expectedOutput = String.format(
                    "%n--------------------------------------------------%n" +
                            "JUnit Failures Summary%n" +
                            "--------------------------------------------------%n%n" +
                            "[1] TestClass1 (2 failures, 0.303s)%n" +
                            "  - [1.1] testMethod1 (Test Method 1)%n" +
                            "  - [1.2] testMethod2%n" +
                            "%nTotal Failures: 2%n"
            );
            assertThat(outContent.toString()).isEqualTo(expectedOutput);

            System.setOut(System.out);
        }
    }
}
