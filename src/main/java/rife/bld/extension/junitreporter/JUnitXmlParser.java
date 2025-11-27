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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Utility class to parse JUnit XML reports.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public final class JUnitXmlParser {
    public static final String MESSAGE_ATTR = "message";
    public static final String NAME_ATTR = "name";
    public static final String TESTCASE_TAG = "testcase";
    private static final String CLASSNAME_ATTR = "classname";
    private static final String DEFAULT_MESSAGE = "No message provided";
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("display-name:\\s*([^\\r\\n]+)");
    private static final String ERROR_TAG = "error";
    private static final DocumentBuilderFactory FACTORY = createFactory();
    private static final String FAILURE_TAG = "failure";
    private static final String SYSTEM_OUT_TAG = "system-out";
    private static final String TIME_ATTR = "time";
    private static final String TYPE_ATTR = "type";

    private JUnitXmlParser() {
        // Prevent instantiation
    }

    @SuppressWarnings({"HttpUrlsUsage", "PMD.AvoidCatchingGenericException"})
    private static DocumentBuilderFactory createFactory() {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
            // Features might not be supported by all parsers
        }
        return factory;
    }

    /**
     * Creates a {@link TestFailure} instance by extracting relevant failure information from a JUnit XML test case
     * element and its associated failure element.
     *
     * @param testCaseElement The XML element representing the test case.
     * @param failureElement  The XML element representing the test failure
     * @param defaultType     The default failure type to use if the type is not specified in the failure element.
     * @return A {@code TestFailure} object containing the extracted failure details
     */
    public static TestFailure createTestFailure(Element testCaseElement,
                                                Element failureElement,
                                                String defaultType) {
        var testName = testCaseElement.getAttribute(NAME_ATTR);
        var className = testCaseElement.getAttribute(CLASSNAME_ATTR);
        var timeStr = testCaseElement.getAttribute(TIME_ATTR);
        var time = parseTime(timeStr);
        var displayName = extractDisplayName(testCaseElement);

        var failureType = getAttributeOrDefault(failureElement, TYPE_ATTR, defaultType);
        var failureMessage = getAttributeOrDefault(failureElement, MESSAGE_ATTR, DEFAULT_MESSAGE);
        var stackTrace = getTextContentTrimmed(failureElement);

        return new TestFailure(testName, displayName, className, failureType, failureMessage, stackTrace, time);
    }

    /**
     * Extracts the display-name from the system-out element within a test case.
     *
     * @param testCaseElement The test case element to search within
     * @return The display name if found, or empty if not present
     */
    public static String extractDisplayName(Element testCaseElement) {
        var systemOutNodes = testCaseElement.getElementsByTagName(SYSTEM_OUT_TAG);

        for (int i = 0; i < systemOutNodes.getLength(); i++) {
            var systemOutElement = (Element) systemOutNodes.item(i);
            var content = getTextContentTrimmed(systemOutElement);

            var matcher = DISPLAY_NAME_PATTERN.matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return "";
    }

    /**
     * Extracts test failure information from a JUnit XML report and groups by class name.
     *
     * @param xmlFilePath Path to the JUnit XML file
     * @return Map of class names to {@link TestClassFailures TestClassFailures} objects containing grouped failures
     * @throws JUnitXmlParserException if parsing fails or the file doesn't exist
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    @SuppressFBWarnings({"EXS_EXCEPTION_SOFTENING_NO_CONSTRAINTS", "PATH_TRAVERSAL_IN", "XXE_DOCUMENT"})
    public static Map<String, TestClassFailures> extractTestFailuresGrouped(String xmlFilePath) {
        Objects.requireNonNull(xmlFilePath, "XML file path cannot be null");

        var path = Paths.get(xmlFilePath);
        validateFile(path);

        try {
            var builder = FACTORY.newDocumentBuilder();
            var document = builder.parse(path.toFile());
            document.getDocumentElement().normalize();

            return parseTestCases(document.getElementsByTagName(TESTCASE_TAG));

        } catch (Exception e) {
            throw new JUnitXmlParserException("Failed to parse XML file: " + xmlFilePath, e);
        }
    }

    /**
     * Retrieves the value of a specified attribute from the provided XML element.
     *
     * @param element       The XML element from which to retrieve the attribute
     * @param attributeName The name of the attribute to retrieve
     * @param defaultValue  The default value to return if the attribute is absent or blank
     * @return The value of the attribute if present and non-blank; otherwise, the default value
     */
    public static String getAttributeOrDefault(Element element, String attributeName, String defaultValue) {
        var value = element.getAttribute(attributeName);
        return (value.isBlank()) ? defaultValue : value;
    }

    /**
     * Retrieves the trimmed text content of the specified XML element.
     *
     * @param element The XML element from which to extract and trim the text content
     * @return The trimmed text content of the element
     */
    public static String getTextContentTrimmed(Element element) {
        return element.getTextContent().trim();
    }

    /**
     * Parses a list of test case nodes from a JUnit XML report and groups the failures by their respective test
     * class names.
     *
     * @param testCases The list of test case nodes to parse
     */
    public static Map<String, TestClassFailures> parseTestCases(NodeList testCases) {
        var length = testCases.getLength();
        var groupedFailures = new ConcurrentHashMap<String, TestClassFailures>(length / 4);

        // Use parallel processing for large XML files only
        var useParallel = length > 1000; // Higher threshold for meaningful parallel benefit
        var stream = useParallel ?
                IntStream.range(0, length).parallel() :
                IntStream.range(0, length);

        stream.mapToObj(testCases::item)
                .map(Element.class::cast)
                .forEach(testCaseElement -> processTestCase(testCaseElement, groupedFailures));

        // Sort failures within each TestClassFailures before returning
        groupedFailures.values().forEach(TestClassFailures::sortFailures);

        // Convert to TreeMap as we need sorted results
        return new TreeMap<>(groupedFailures);
    }

    /**
     * Parses a time value represented as a {@link String} and returns its numeric equivalent.
     *
     * @param timeStr The time value as a string to be parsed
     * @return The parsed time value as a double, or {@code 0.0} if the input is invalid
     */
    public static double parseTime(String timeStr) {
        var defaultTime = 0.0;
        if (timeStr == null || timeStr.isEmpty()) {
            return defaultTime;
        }

        try {
            double parsedTime = Double.parseDouble(timeStr);
            return Double.isFinite(parsedTime) ? parsedTime : defaultTime;
        } catch (NumberFormatException e) {
            return defaultTime;
        }
    }

    /**
     * Processes failure nodes within a JUnit test case XML report and groups the extracted failures
     * by their corresponding test class names.
     */
    private static void processFailureNodes(NodeList nodes,
                                            Element testCaseElement,
                                            Map<String, TestClassFailures> groupedFailures,
                                            String type) {
        // Use computeIfAbsent for thread-safe grouping
        IntStream.range(0, nodes.getLength()).mapToObj(nodes::item)
                .map(failureNode -> (Element) failureNode)
                .map(failureElement ->
                        createTestFailure(testCaseElement, failureElement, type)).forEach(failure ->
                        groupedFailures.computeIfAbsent(failure.className(), TestClassFailures::new)
                                .addFailure(failure));
    }

    /**
     * Processes a single test case element and extracts failure/error information.
     */
    private static void processTestCase(Element testCaseElement,
                                        Map<String, TestClassFailures> groupedFailures) {
        // Process failures
        var failures = testCaseElement.getElementsByTagName(FAILURE_TAG);
        if (failures.getLength() > 0) {
            processFailureNodes(failures, testCaseElement, groupedFailures, FAILURE_TAG);
        }

        // Process errors (treating them as failures)
        var errors = testCaseElement.getElementsByTagName(ERROR_TAG);
        if (errors.getLength() > 0) {
            processFailureNodes(errors, testCaseElement, groupedFailures, ERROR_TAG);
        }
    }

    /**
     * Validates file existence and readability.
     *
     * @throws JUnitXmlParserException if the file does not exist or is not readable
     */
    @SuppressWarnings("PMD.ExceptionAsFlowControl")
    public static void validateFile(Path path) throws JUnitXmlParserException {
        try {
            if (!Files.exists(path)) {
                throw new JUnitXmlParserException("File does not exist: " + path);
            }
            if (!Files.isReadable(path)) {
                throw new JUnitXmlParserException("File is not readable: " + path);
            }
        } catch (SecurityException e) {
            throw new JUnitXmlParserException("Cannot access file: " + path, e);
        }
    }
}