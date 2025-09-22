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

import rife.bld.BaseProject;
import rife.bld.extension.junitreporter.JUnitXmlParser;
import rife.bld.extension.junitreporter.JUnitXmlParserException;
import rife.bld.extension.junitreporter.ReportPrinter;
import rife.bld.operations.AbstractProcessOperation;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
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
public class JUnitReporterOperation extends AbstractProcessOperation<JUnitReporterOperation> {
    private static final String ARG_ALL = "--all";
    private static final Pattern ARG_MATCH_PATTERN = Pattern.compile("^--(i|index)=(\\d+(?:\\.\\d+)?)$");
    private static final Logger LOGGER = Logger.getLogger(JUnitReporterOperation.class.getName());
    private String argIndex_;
    private boolean failOnSummary_;
    private boolean isPrintAll_;
    private BaseProject project_;
    private Path reportFile_;

    /**
     * Preforms the operation.
     *
     * @throws ExitStatusException if the operation fails
     */
    @Override
    public void execute() throws ExitStatusException {
        var status = EXIT_FAILURE;
        if (project_ == null) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.log(Level.SEVERE, "A project is required to run this operation.");
            }
        } else if (reportFile_ == null) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.log(Level.SEVERE, "A report file is required to run this operation.");
            }
        } else {
            try {
                var groupedFailures =
                        JUnitXmlParser.extractTestFailuresGrouped(reportFile_.toString());

                if (!groupedFailures.isEmpty()) {
                    if (isPrintAll_) {
                        for (var i = 0; i < groupedFailures.size(); i++) {
                            ReportPrinter.printDetails(String.valueOf(i + 1), groupedFailures);
                        }
                    } else if (argIndex_ != null) {
                        ReportPrinter.printDetails(argIndex_, groupedFailures);
                    } else {
                        ReportPrinter.printSummary(groupedFailures);
                    }
                    if (!failOnSummary_) {
                        status = EXIT_SUCCESS;
                    }
                } else {
                    status = EXIT_SUCCESS;
                }
            } catch (JUnitXmlParserException e) {
                if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                    LOGGER.log(Level.SEVERE, "Failed to parse JUnit report: " + e.getMessage());
                }
            } catch (IndexOutOfBoundsException e) {
                if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                    LOGGER.log(Level.SEVERE, e.getMessage());
                }
            } catch (Exception e) {
                if (!silent()) {
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.log(Level.FINE, "Unexpected error", e);
                    } else if (LOGGER.isLoggable(Level.SEVERE)) {
                        LOGGER.log(Level.SEVERE, "Unexpected error: " + e.getMessage());
                    }
                }
            }
        }

        ExitStatusException.throwOnFailure(status);
    }

    /**
     * Constructs the command list to use for the operation.
     *
     * @return the command list
     */
    @Override
    protected List<String> executeConstructProcessCommandList() {
        return List.of();
    }

    /**
     * Configures the operation from a {@link BaseProject}.
     * <p>
     * If not set, the {@link #reportFile() report file} is set to the default location for JUnit reports.
     *
     * @param project the project to use as the context for this operation;
     * @return this operation
     */
    @Override
    public JUnitReporterOperation fromProject(BaseProject project) {
        project_ = project;

        if (project_ != null) {
            if (reportFile_ == null) {
                reportFile_ = Path.of(project_.buildDirectory().getAbsolutePath(), "test-results", "test",
                        "TEST-junit-jupiter.xml");
            }

            // parse the run arguments if any
            var args = project.arguments();
            if (!args.isEmpty()) {
                var arg = args.get(0);
                if (ARG_ALL.equals(arg)) {
                    isPrintAll_ = true;
                    args.remove(0);
                } else {
                    var matcher = ARG_MATCH_PATTERN.matcher(arg);
                    if (matcher.matches()) {
                        args.remove(0);
                        argIndex_ = matcher.group(2);
                    }
                }
            }
        }
        return this;
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
    public JUnitReporterOperation reportFile(Path reportFile) {
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
    public JUnitReporterOperation reportFile(File reportFile) {
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
        reportFile_ = Path.of(reportFile);
        return this;
    }
}