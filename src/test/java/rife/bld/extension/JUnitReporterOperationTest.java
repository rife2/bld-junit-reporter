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

package rife.bld.extension;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import rife.bld.BaseProject;
import rife.bld.extension.junitreporter.JUnitXmlParser;
import rife.bld.extension.testing.LoggingExtension;
import rife.bld.extension.testing.TestLogHandler;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TestClassWithoutTestCases"})
class JUnitReporterOperationTest {
    @SuppressWarnings("LoggerInitializedWithForeignClass")
    private static final Logger LOGGER = Logger.getLogger(JUnitReporterOperation.class.getName());
    private static final TestLogHandler TEST_LOG_HANDLER = new TestLogHandler();

    @RegisterExtension
    @SuppressWarnings("unused")
    private static final LoggingExtension LOGGING_EXTENSION = new LoggingExtension(LOGGER, TEST_LOG_HANDLER);

    private void assertLogContains(String message) {
        assertThat(TEST_LOG_HANDLER.containsMessage(message))
                .as("Expected log to contain message: '%s'. Actual log messages: %s",
                        message, TEST_LOG_HANDLER.getLogMessages())
                .isTrue();
    }

    @Test
    @Order(1)
    void exampleReportMustExist() {
        var reportFile = new File("example/build/test-results/test/TEST-junit-jupiter.xml");
        assertThat(reportFile).exists();
        assertThat(reportFile).isFile();
        assertThat(reportFile.length()).isGreaterThan(0);
    }

    @Nested
    @DisplayName("Argument Parsing Tests")
    class ArgumentParsingTests {
        @Test
        void argIndexWithInvalidFormat() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--invalid-arg")));
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThat(operation).extracting("argIndex_").isNull();
        }

        @Test
        void argIndexWithShortFormFlag() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--i=2.5")));
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThat(operation).extracting("argIndex_").isEqualTo("2.5");
        }

        @Test
        void multipleArgumentsOnlyFirstMatches() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--index=1", "--other-arg")));
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThat(operation).extracting("argIndex_").isEqualTo("1");
            // Verify only the first matching argument is removed
            assertThat(mockedProject.arguments()).containsExactly("--other-arg");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        @Test
        void executeWithNonExistentReportFileLogging() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation = new JUnitReporterOperation()
                    .fromProject(mockedProject)
                    .reportFile("does-not-exist.xml");

            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            // Should log the parser exception
            assertLogContains("Failed to parse JUnit report");
        }

        @Test
        void executeWithSilentMode() {
            var operation = new JUnitReporterOperation().silent(true);

            // This should not log any messages due to silent mode
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            // In silent mode, no log messages should be recorded
            assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Fail On Summary Tests")
    class FailOnSummaryTests {
        @Test
        void failOnSummaryIsFalse() {
            var operation = new JUnitReporterOperation().failOnSummary(false);
            assertThat(operation.failOnSummary()).isFalse();
        }

        @Test
        void failOnSummaryIsTrue() {
            var operation = new JUnitReporterOperation().failOnSummary(true);
            assertThat(operation.failOnSummary()).isTrue();
        }

        @Test
        void failOnSummaryWhenFalse() {
            var operation = new JUnitReporterOperation().failOnSummary(false);
            assertThat(operation.failOnSummary()).isFalse();
        }

        @Test
        void failOnSummaryWhenTrue() {
            var operation = new JUnitReporterOperation().failOnSummary(true);
            assertThat(operation.failOnSummary()).isTrue();
        }
    }

    @Nested
    @DisplayName("From Project Tests")
    class FromProjectTests {
        @Test
        void argAllIsSetFromProjectArguments() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--all")));
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);
            assertThat(operation).extracting("isPrintAll_").isEqualTo(true);
        }

        @Test
        void argIndexIsRemovedFromProjectArgumentsAfterUse() {
            var mockedProject = mock(BaseProject.class);
            var arguments = new ArrayList<>(List.of("--index=4"));
            when(mockedProject.arguments()).thenReturn(arguments);
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            new JUnitReporterOperation().fromProject(mockedProject);

            assertThat(arguments).isEmpty();
        }

        @Test
        void argIndexIsSetFromProjectArguments() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--index=3.5")));
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThat(operation).extracting("argIndex_").isEqualTo("3.5");
        }

        @Test
        void defaultReportFilePathIsSetFromProject() {
            var mockedProject = mock(BaseProject.class);
            var buildDir = new File("build");
            when(mockedProject.buildDirectory()).thenReturn(buildDir);

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThat(operation.reportFile()).isEqualTo(
                    Path.of(buildDir.getAbsolutePath(), "test-results", "test", "TEST-junit-jupiter.xml"));
        }

        @Test
        void fromProjectHandlesNullProject() {
            var operation = new JUnitReporterOperation().fromProject(null);

            assertThat(operation.reportFile()).isNull();
            assertThat(operation).extracting("argIndex_").isNull();
        }
    }

    @Nested
    @DisplayName("Logging Level Tests")
    class LoggingLevelTests {
        @Test
        void executeWithLoggingDisabled() {
            // Disable SEVERE logging to test the logging condition branches
            LOGGER.setLevel(Level.OFF);

            var operation = new JUnitReporterOperation();

            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            // No messages should be logged when logging is disabled
            assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
        }

        @Test
        void executeWithReportFileAndSilentMode() {
            var operation = new JUnitReporterOperation()
                    .fromProject(new BaseProject())
                    .reportFile((Path) null)
                    .silent(true);
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
        }

        @Test
        void executeWithReportFileNullAndLoggingDisabled() {
            LOGGER.setLevel(Level.OFF);

            var operation = new JUnitReporterOperation()
                    .fromProject(new BaseProject())
                    .reportFile((Path) null);
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Project Configuration Edge Cases")
    class ProjectConfigurationTests {
        @Test
        void fromProjectWithEmptyArguments() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>());
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThat(operation).extracting("argIndex_").isNull();
        }

        @Test
        void fromProjectWithExistingReportFile() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>());
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var existingReportFile = Path.of("custom-report.xml");
            var operation = new JUnitReporterOperation()
                    .reportFile(existingReportFile)
                    .fromProject(mockedProject);

            // Should keep the existing report file, not set the default
            assertThat(operation.reportFile()).isEqualTo(existingReportFile);
        }
    }

    @Nested
    @DisplayName("Execute Tests")
    class executeTests {
        @Test
        void executeConstructProcessCommandListMustBeEmpty() {
            assertThat(new JUnitReporterOperation().executeConstructProcessCommandList()).isEqualTo(List.of());
        }

        @Test
        void executeLargeReport() {
            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            var operation = new JUnitReporterOperation()
                    .fromProject(new BaseProject())
                    .failOnSummary(true)
                    .reportFile("src/test/resources/large.xml");
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertThat(outContent.toString().trim()).endsWith("Total Failures: 408");

            System.setOut(System.out);
        }

        @Test
        void executeThrowsUnexpectedRuntimeException() {
            LOGGER.setLevel(Level.WARNING);
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var reportPath = Path.of("test-report.xml");

            var operation = new JUnitReporterOperation()
                    .fromProject(mockedProject)
                    .reportFile(reportPath);

            try (var mockedParser = Mockito.mockStatic(JUnitXmlParser.class)) {
                var unexpectedException = new RuntimeException("Simulated unexpected error");
                mockedParser.when(() -> JUnitXmlParser.extractTestFailuresGrouped(anyString()))
                        .thenThrow(unexpectedException);

                assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

                var message = "Unexpected error: Simulated unexpected error";
                assertThat(TEST_LOG_HANDLER.getLogMessages()).contains(message);
            }
        }

        @Test
        void executeThrowsUnexpectedRuntimeExceptionWithFineLogging() {
            LOGGER.setLevel(Level.FINE);
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var reportPath = Path.of("test-report.xml");

            var operation = new JUnitReporterOperation()
                    .fromProject(mockedProject)
                    .reportFile(reportPath);

            try (var mockedParser = Mockito.mockStatic(JUnitXmlParser.class)) {
                var unexpectedException = new RuntimeException("Simulated unexpected error");
                mockedParser.when(() -> JUnitXmlParser.extractTestFailuresGrouped(anyString()))
                        .thenThrow(unexpectedException);

                assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

                var message = "Unexpected error";
                assertThat(TEST_LOG_HANDLER.getLogMessages()).contains(message);
            }
        }

        @Test
        void executeThrowsUnexpectedRuntimeExceptionWithNoLogging() {
            LOGGER.setLevel(Level.OFF);
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var reportPath = Path.of("test-report.xml");

            var operation = new JUnitReporterOperation()
                    .fromProject(mockedProject)
                    .reportFile(reportPath);

            try (var mockedParser = Mockito.mockStatic(JUnitXmlParser.class)) {
                var unexpectedException = new RuntimeException("Simulated unexpected error");
                mockedParser.when(() -> JUnitXmlParser.extractTestFailuresGrouped(anyString()))
                        .thenThrow(unexpectedException);

                assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

                assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
            }
        }

        @Test
        void executeThrowsUnexpectedRuntimeExceptionWithSilentMode() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var reportPath = Path.of("test-report.xml");

            var operation = new JUnitReporterOperation()
                    .fromProject(mockedProject)
                    .silent(true)
                    .reportFile(reportPath);

            try (var mockedParser = Mockito.mockStatic(JUnitXmlParser.class)) {
                var unexpectedException = new RuntimeException("Simulated unexpected error");
                mockedParser.when(() -> JUnitXmlParser.extractTestFailuresGrouped(anyString()))
                        .thenThrow(unexpectedException);

                assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

                assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
            }
        }

        @Test
        void executeWithAllArgument() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--all")));
            when(mockedProject.buildDirectory()).thenReturn(new File("src/test/resources"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            var outContent = new ByteArrayOutputStream();
            System.setOut(new PrintStream(outContent));

            assertThatCode(operation::execute).doesNotThrowAnyException();

            var expectedOutput = String.format(
                    "--------------------------------------------------%n" +
                            "[1] com.example.ExampleTests%n" +
                            "--------------------------------------------------%n%n" +
                            "[1.1] Test: verifyFail(String)[1]%n" +
                            "    - Name: [1] foo%n" +
                            "    - Type: org.opentest4j.AssertionFailedError%n" +
                            "    - Message:%n" +
                            "        expected: <foo> but was: <Hello World!>%n" +
                            "    - Time: 0.009s%n" +
                            "%n[1.2] Test: verifyFail(String)[2]%n" +
                            "    - Name: [2] bar%n" +
                            "    - Type: org.opentest4j.AssertionFailedError%n" +
                            "    - Message:%n" +
                            "        expected: <bar> but was: <Hello World!>%n" +
                            "    - Time: 0.001s%n%n" +
                            "[1.3] Test: verifyHelloFoo()%n" +
                            "    - Name: verifyHelloFoo()%n" +
                            "    - Type: org.opentest4j.AssertionFailedError%n" +
                            "    - Message:%n" +
                            "        expected: <Hello Foo!> but was: <Hello World!>%n" +
                            "    - Time: 0.001s%n%n" +
                            "--------------------------------------------------%n" +
                            "[2] com.example.MoreTests%n" +
                            "--------------------------------------------------%n%n" +
                            "[2.1] Test: verifyMore(String)[3]%n" +
                            "    - Name: [3] qux%n" +
                            "    - Type: org.opentest4j.AssertionFailedError%n" +
                            "    - Message:%n" +
                            "        expected: <true> but was: <false>%n" +
                            "    - Time: 0.001s%n%n" +
                            "[2.2] Test: verifyMore(String)[4]%n" +
                            "    - Name: [4] quux%n" +
                            "    - Type: org.opentest4j.AssertionFailedError%n" +
                            "    - Message:%n" +
                            "        expected: <true> but was: <false>%n" +
                            "    - Time: 0.0s%n%n"
            );
            assertThat(outContent.toString()).isEqualTo(expectedOutput);

            System.setOut(System.out);
        }

        @Test
        void executeWithInvalidFailureIndex() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--i=2.3")));
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertThat(TEST_LOG_HANDLER.getLogMessages()).contains("The failure index is out of bounds");
        }

        @Test
        void executeWithInvalidFailureIndexAndLoggingDisabled() {
            LOGGER.setLevel(Level.OFF);
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--i=2.3")));
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
        }

        @Test
        void executeWithInvalidFailureIndexAndSilentMode() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--i=2.3")));
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var operation = new JUnitReporterOperation()
                    .fromProject(mockedProject)
                    .silent(true);

            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
        }

        @Test
        void executeWithInvalidGroupIndex() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--i=3")));
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertThat(TEST_LOG_HANDLER.getLogMessages()).contains("The group index is out of bounds");
        }

        @Test
        void executeWithNoFailures() {
            var operation = new JUnitReporterOperation()
                    .fromProject(new BaseProject())
                    .reportFile("src/test/resources/clean.xml");
            assertThatCode(operation::execute).doesNotThrowAnyException();
        }

        @Test
        void executeWithNoProject() {
            var operation = new JUnitReporterOperation();
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertLogContains("A project is required to run this operation");
        }

        @Test
        void executeWithNoReportFile() {
            var operation = new JUnitReporterOperation().fromProject(new BaseProject());
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            assertLogContains("File does not exist");
        }

        @Test
        void executeWithParserException() {
            var project = mock(BaseProject.class);
            when(project.buildDirectory()).thenReturn(new File("build"));

            var operation =
                    new JUnitReporterOperation()
                            .fromProject(project)
                            .reportFile("src/test/resources/blank.xml");
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            // Verify parser error is logged
            assertLogContains("Failed to parse JUnit report");
        }

        @Test
        void executeWithParserExceptionAndLoggingDisabled() {
            LOGGER.setLevel(Level.OFF);
            var project = mock(BaseProject.class);
            when(project.buildDirectory()).thenReturn(new File("build"));

            var operation =
                    new JUnitReporterOperation()
                            .fromProject(project)
                            .reportFile("src/test/resources/blank.xml");
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            // Verify parser error is not logged when silent mode is enabled
            assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
        }

        @Test
        void executeWithParserExceptionAndSilentMode() {
            var project = mock(BaseProject.class);
            when(project.buildDirectory()).thenReturn(new File("build"));

            var operation =
                    new JUnitReporterOperation()
                            .fromProject(project)
                            .reportFile("src/test/resources/blank.xml")
                            .silent(true);
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);

            // Verify parser error is not logged when silent mode is enabled
            assertThat(TEST_LOG_HANDLER.getLogMessages()).isEmpty();
        }

        @Test
        void executeWithReportFileNull() {
            var operation = new JUnitReporterOperation().fromProject(new BaseProject());
            operation.reportFile((Path) null);
            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);
            assertThat(TEST_LOG_HANDLER.getLogMessages()).contains("A report file is required to run this operation.");
        }

        @Test
        void executeWithValidGroupAndFailureIndices() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--index=2.1")));
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThatCode(operation::execute).doesNotThrowAnyException();
        }

        @Test
        void executeWithValidGroupAndFailureIndicesAndLargeReport() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--index=14.2")));
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation =
                    new JUnitReporterOperation()
                            .reportFile("src/test/resources/large.xml")
                            .fromProject(mockedProject);

            assertThatCode(operation::execute).doesNotThrowAnyException();
        }

        @Test
        void executeWithValidGroupFailuresWithSummaryFailDisabled() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var operation =
                    new JUnitReporterOperation()
                            .fromProject(mockedProject)
                            .failOnSummary(false);

            assertThatCode(operation::execute).doesNotThrowAnyException();
        }

        @Test
        void executeWithValidGroupFailuresWithSummaryFailEnabled() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var operation =
                    new JUnitReporterOperation()
                            .fromProject(mockedProject)
                            .failOnSummary(true);

            assertThatThrownBy(operation::execute).isInstanceOf(ExitStatusException.class);
        }

        @Test
        void executeWithValidGroupIndex() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--index=2")));
            when(mockedProject.buildDirectory()).thenReturn(new File("example/build"));

            var operation = new JUnitReporterOperation().fromProject(mockedProject);

            assertThatCode(operation::execute).doesNotThrowAnyException();
        }

        @Test
        void executeWithValidGroupIndexAndLargeReport() {
            var mockedProject = mock(BaseProject.class);
            when(mockedProject.arguments()).thenReturn(new ArrayList<>(List.of("--index=14")));
            when(mockedProject.buildDirectory()).thenReturn(new File("build"));

            var operation =
                    new JUnitReporterOperation()
                            .reportFile("src/test/resources/large.xml")
                            .fromProject(mockedProject);

            assertThatCode(operation::execute).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Report File Tests")
    class reportFileTests {
        @Test
        void reportFileAsFile() {
            var reportFile = new File("mocked.xml");

            var operation = new JUnitReporterOperation().reportFile(reportFile);
            assertThat(operation).extracting("reportFile_").isEqualTo(reportFile.toPath());
        }

        @Test
        void reportFileAsPath() {
            var reportFile = Path.of("mocked.xml");

            var operation = new JUnitReporterOperation().reportFile(reportFile);
            assertThat(operation).extracting("reportFile_").isEqualTo(reportFile);
        }

        @Test
        void reportFileAsString() {
            var operation = new JUnitReporterOperation().reportFile("mocked.xml");
            assertThat(operation).extracting("reportFile_").isEqualTo(Path.of("mocked.xml"));
        }

        @Test
        void reportFileNotSet() {
            var operation = new JUnitReporterOperation();
            assertThat(operation).extracting("reportFile_").isNull();
        }
    }
}