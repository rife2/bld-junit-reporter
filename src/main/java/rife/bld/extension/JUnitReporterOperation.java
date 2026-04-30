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

package rife.bld.extension;

import edu.umd.cs.findbugs.annotations.NonNull;
import rife.bld.BaseProject;
import rife.bld.extension.junitreporter.JUnitXmlParser;
import rife.bld.extension.junitreporter.JUnitXmlParserException;
import rife.bld.extension.junitreporter.ReportPrinter;
import rife.bld.extension.junitreporter.TestClassFailures;
import rife.bld.extension.tools.IOTools;
import rife.bld.extension.tools.ObjectTools;
import rife.bld.operations.AbstractOperation;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static rife.bld.operations.exceptions.ExitStatusException.EXIT_FAILURE;
import static rife.bld.operations.exceptions.ExitStatusException.EXIT_SUCCESS;

/**
 * An operation that parses JUnit XML reports and prints test failure information grouped by test class.
 *
 * <h4>Usage:</h4>
 *
 * <blockquote><pre>
 * &#64;BuildCommand(summary = "Runs the JUnit reporter")
 * public void reporter() throws Exception {
 *     new JUnitReporterOperation()
 *             .fromProject(this)
 *             .failOnSummary(true)
 *             .reportFile("build/test-results/test/TEST-junit-jupiter.xml")
 *             .execute();
 * }
 * </pre></blockquote>
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class JUnitReporterOperation extends AbstractOperation<JUnitReporterOperation> {

    private static final String ARG_ALL = "--all";
    private static final Pattern INDEX_PATTERN = Pattern.compile("^--(i|index)=(\\d+(?:\\.\\d+)?)$");
    private static final Logger logger = Logger.getLogger(JUnitReporterOperation.class.getName());

    private String argIndex_;
    private boolean failOnSummary_;
    private boolean printAll_;
    private Path reportFile_;

    /**
     * Performs the operation.
     *
     * @throws ExitStatusException if the operation fails
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void execute() throws Exception {
        if (IOTools.notExists(reportFile_)) {
            logSevere("A report file is required to run this operation.");
            ExitStatusException.throwOnFailure(EXIT_FAILURE);
        }

        var status = EXIT_FAILURE;

        try {
            var groupedFailures =
                    JUnitXmlParser.extractTestFailuresGrouped(reportFile_.toString());

            if (groupedFailures.isEmpty()) {
                status = EXIT_SUCCESS;
            } else {
                printFailures(groupedFailures);
                if (!failOnSummary_) {
                    status = EXIT_SUCCESS;
                }
            }
        } catch (JUnitXmlParserException e) {
            logSevere("Failed to parse JUnit report: " + e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
            logSevere(e.getMessage(), e); // message includes the exact cause
        } catch (Exception e) {
            logSevere("Unexpected error: " + e.getMessage(), e);
        }

        ExitStatusException.throwOnFailure(status);
    }

    /**
     * Configures whether the operation should fail if the summary is printed.
     *
     * @param failOnSummary {@code true} to fail on summary, {@code false} to not fail
     * @return this operation
     */
    public JUnitReporterOperation failOnSummary(boolean failOnSummary) {
        failOnSummary_ = failOnSummary;
        return this;
    }

    /**
     * Returns whether the operation should fail if the summary is printed.
     *
     * @return {@code true} if the operation should fail on summary, {@code false} otherwise
     */
    public boolean failOnSummary() {
        return failOnSummary_;
    }

    /**
     * Configures the operation from a {@link BaseProject}.
     * <p>
     * If not set, the {@link #reportFile() report file} is set to the default location for JUnit reports.
     *
     * @param project the project to use as the context for this operation
     * @return this operation
     */
    public JUnitReporterOperation fromProject(@NonNull BaseProject project) {
        Objects.requireNonNull(project, "The project must not be null");

        if (reportFile_ == null) {
            reportFile_ = Path.of(project.buildDirectory().getAbsolutePath(), "test-results", "test",
                    "TEST-junit-jupiter.xml");
        }

        parseArguments(project.arguments());

        return this;
    }

    /**
     * Returns the path to the report file.
     *
     * @return the path
     */
    public Path reportFile() {
        return reportFile_;
    }

    /**
     * Specifies the XML report file.
     *
     * @param reportFile the report file path
     * @return this operation
     * @see #reportFile(File)
     * @see #reportFile(String)
     */
    public JUnitReporterOperation reportFile(@NonNull Path reportFile) {
        Objects.requireNonNull(reportFile, "The report file must not be null");
        reportFile_ = reportFile;
        return this;
    }

    /**
     * Specifies the XML report file.
     *
     * @param reportFile the report file path
     * @return this operation
     * @see #reportFile(Path)
     * @see #reportFile(String)
     */
    public JUnitReporterOperation reportFile(@NonNull File reportFile) {
        Objects.requireNonNull(reportFile, "The report file must not be null");
        reportFile_ = reportFile.toPath();
        return this;
    }

    /**
     * Specifies the XML report file.
     *
     * @param reportFile the report file path
     * @return this operation
     * @see #reportFile(Path)
     * @see #reportFile(File)
     */
    public JUnitReporterOperation reportFile(String reportFile) {
        ObjectTools.requireNotEmpty(reportFile, "The report file must not be null or empty");
        reportFile_ = Path.of(reportFile);
        return this;
    }

    @SuppressWarnings("SameParameterValue")
    private void logSevere(String message) {
        if (logger.isLoggable(Level.SEVERE) && !silent()) {
            logger.log(Level.SEVERE, message);
        }
    }

    private void logSevere(String message, Exception e) {
        if (logger.isLoggable(Level.SEVERE) && !silent()) {
            logger.log(Level.SEVERE, message, e);
        }
    }

    /**
     * Parses the first run argument, setting {@link #printAll_} or {@link #argIndex_} as appropriate.
     * <p>
     * Valid arguments are moved after processing.
     *
     * @param args the project's argument list (mutated by removing a consumed argument)
     */
    private void parseArguments(List<String> args) {
        if (args.isEmpty()) {
            return;
        }

        var arg = args.get(0);

        if (ARG_ALL.equals(arg)) {
            printAll_ = true;
            args.remove(0);
            return;
        }

        var matcher = INDEX_PATTERN.matcher(arg);
        if (matcher.matches()) {
            argIndex_ = matcher.group(2);
            args.remove(0);
        }
    }

    /**
     * Prints failure details or a summary depending on the current mode flags.
     *
     * @param groupedFailures the non-empty grouped failures to print
     */
    private void printFailures(Map<String, TestClassFailures> groupedFailures) {
        if (printAll_) {
            for (var i = 0; i < groupedFailures.size(); i++) {
                ReportPrinter.printDetails(String.valueOf(i + 1), groupedFailures);
            }
        } else if (argIndex_ != null) {
            ReportPrinter.printDetails(argIndex_, groupedFailures);
        } else {
            ReportPrinter.printSummary(groupedFailures);
        }
    }
}
