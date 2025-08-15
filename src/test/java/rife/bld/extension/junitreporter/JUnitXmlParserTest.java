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
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.io.File.separatorChar;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class JUnitXmlParserTest {
    @Nested
    @DisplayName("Extract Display Name Tests")
    class ExtractDisplayNameTests {
        @Test
        void extractDisplayNameWithMultipleSystemOutElements() throws Exception {
            var xmlContent = """
                        <testcase>
                            <system-out>Some unrelated output</system-out>
                            <system-out>display-name: Desired Display Name</system-out>
                            <system-out>Another unrelated output</system-out>
                        </testcase>
                    """;
            var document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes()));

            var testCaseElement = (Element) document.getElementsByTagName("testcase").item(0);

            assertThat(JUnitXmlParser.extractDisplayName(testCaseElement))
                    .isEqualTo("Desired Display Name");
        }

        @Test
        void extractDisplayNameWithNoDisplayName() throws Exception {
            var xmlContent = """
                        <testcase>
                            <system-out>Some unrelated output</system-out>
                        </testcase>
                    """;
            var document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes()));

            var testCaseElement = (Element) document.getElementsByTagName("testcase").item(0);

            assertThat(JUnitXmlParser.extractDisplayName(testCaseElement))
                    .isEmpty();
        }

        @Test
        void extractDisplayNameWithValidDisplayName() throws Exception {
            var xmlContent = """
                        <testcase>
                            <system-out>display-name: Valid Display Name</system-out>
                        </testcase>
                    """;
            var document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes()));

            var testCaseElement = (Element) document.getElementsByTagName("testcase").item(0);

            assertThat(JUnitXmlParser.extractDisplayName(testCaseElement))
                    .isEqualTo("Valid Display Name");
        }
    }

    @Nested
    @DisplayName("Extract Test Failures Grouped Tests")
    class ExtractTestFailuresGroupedTests {
        @Test
        void extractTestFailuresGroupedWithBlankAttributes(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="   ">
                                <failure message="   " type="   ">Stack trace here</failure>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var failure = result.get("TestClass1").getFailures().get(0);
            assertThat(failure.time()).isEqualTo(0.0);
            assertThat(failure.failureMessage()).isEqualTo("No message provided");
            assertThat(failure.failureType()).isEqualTo("failure");
        }

        @Test
        void extractTestFailuresGroupedWithBothFailuresAndErrors(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="1.0">
                                <failure message="Assertion failed" type="AssertionError">Failure stack trace</failure>
                                <error message="Null pointer" type="NullPointerException">Error stack trace</error>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var classFailures = result.get("TestClass1");
            assertThat(classFailures.getTotalFailures()).isEqualTo(2);

            var failures = classFailures.getFailures();
            assertThat(failures).hasSize(2);

            // Should contain both failure and error
            assertThat(failures).anyMatch(f -> "AssertionError".equals(f.failureType()));
            assertThat(failures).anyMatch(f -> "NullPointerException".equals(f.failureType()));
        }

        @Test
        void extractTestFailuresGroupedWithDisplayName(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="1.0">
                                <system-out>display-name: Custom Test Display Name
                                Some other output</system-out>
                                <failure message="Test failed" type="AssertionError">Stack trace here</failure>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var failures = result.get("TestClass1").getFailures();
            assertThat(failures.get(0).displayName()).isEqualTo("Custom Test Display Name");
        }

        @Test
        void extractTestFailuresGroupedWithEmptyAttributes(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="">
                                <failure message="" type="">Stack trace here</failure>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var failure = result.get("TestClass1").getFailures().get(0);
            assertThat(failure.time()).isEqualTo(0.0);
            assertThat(failure.failureMessage()).isEqualTo("No message provided");
            assertThat(failure.failureType()).isEqualTo("failure");
        }

        @Test
        void extractTestFailuresGroupedWithEmptyDisplayName(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="1.0">
                                <system-out>display-name:   </system-out>
                                <failure message="Test failed" type="AssertionError">Stack trace here</failure>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var failures = result.get("TestClass1").getFailures();
            assertThat(failures.get(0).displayName()).isEmpty();
        }

        @Test
        void extractTestFailuresGroupedWithEmptyTestCases(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="EmptySuite"></testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).isEmpty();
        }

        @Test
        void extractTestFailuresGroupedWithInvalidTimeFormats(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="invalid">
                                <failure message="Test failed" type="AssertionError">Stack trace</failure>
                            </testcase>
                            <testcase classname="TestClass1" name="testMethod2" time="1.5e-3">
                                <failure message="Test failed" type="AssertionError">Stack trace</failure>
                            </testcase>
                            <testcase classname="TestClass1" name="testMethod3" time="  2.5  ">
                                <failure message="Test failed" type="AssertionError">Stack trace</failure>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var failures = result.get("TestClass1").getFailures();

            // Invalid time should default to 0.0
            assertThat(failures.get(0).time()).isEqualTo(0.0);

            // Scientific notation should be parsed correctly
            assertThat(failures.get(1).time()).isEqualTo(0.0015);

            // Trimmed time should be parsed correctly
            assertThat(failures.get(2).time()).isEqualTo(2.5);
        }

        @Test
        void extractTestFailuresGroupedWithInvalidXmlPath() {
            var invalidPath = "nonexistent" + separatorChar + "file.xml";

            assertThatThrownBy(() -> JUnitXmlParser.extractTestFailuresGrouped(invalidPath))
                    .isInstanceOf(JUnitXmlParserException.class)
                    .hasMessage("File does not exist: " + invalidPath);
        }

        @Test
        @SuppressWarnings("ExtractMethodRecommender")
        void extractTestFailuresGroupedWithLargeFileParallelProcessing(@TempDir Path tempDir) throws IOException {
            // Create XML with more than 1000 test cases to trigger parallel processing
            var xmlBuilder = new StringBuilder(66);
            xmlBuilder.append("<testsuites><testsuite name=\"LargeSuite\">");

            for (int i = 1; i <= 1001; i++) {
                xmlBuilder.append(String.format("""
                        <testcase classname="TestClass%d" name="testMethod%d" time="0.1">
                            <failure message="Test %d failed" type="AssertionError">Stack trace %d</failure>
                        </testcase>
                        """, i, i, i, i));
            }

            xmlBuilder.append("</testsuite></testsuites>");

            var tempFile = tempDir.resolve("large-test.xml");
            Files.writeString(tempFile, xmlBuilder.toString());

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1001);
            assertThat(result.get("TestClass1").getFailures().get(0).testName()).isEqualTo("testMethod1");
            assertThat(result.get("TestClass1001").getFailures().get(0).testName()).isEqualTo("testMethod1001");
        }

        @Test
        void extractTestFailuresGroupedWithMalformedXml(@TempDir Path tempDir) throws IOException {
            var malformedXmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1"
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, malformedXmlContent);

            assertThatThrownBy(() -> JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString()))
                    .isInstanceOf(JUnitXmlParserException.class)
                    .hasMessageStartingWith("Failed to parse XML file: ");
        }

        @Test
        void extractTestFailuresGroupedWithMultipleSystemOutElements(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="1.0">
                                <system-out>First system out</system-out>
                                <system-out>display-name: Found Display Name</system-out>
                                <failure message="Test failed" type="AssertionError">Stack trace here</failure>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var failures = result.get("TestClass1").getFailures();
            assertThat(failures.get(0).displayName()).isEqualTo("Found Display Name");
        }

        @Test
        void extractTestFailuresGroupedWithNoDisplayName(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="1.0">
                                <system-out>Some output without display name</system-out>
                                <failure message="Test failed" type="AssertionError">Stack trace here</failure>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var failures = result.get("TestClass1").getFailures();
            assertThat(failures.get(0).displayName()).isEmpty();
        }

        @Test
        void extractTestFailuresGroupedWithNonElementNodes(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <!-- This is a comment -->
                        <testsuite name="TestSuite1">
                            <!-- Another comment -->
                            <testcase classname="TestClass1" name="testMethod1" time="1.0">
                                <!-- Comment in testcase -->
                                <failure message="Test failed" type="AssertionError">Stack trace</failure>
                            </testcase>
                            <!-- More comments -->
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            assertThat(result.get("TestClass1").getTotalFailures()).isEqualTo(1);
        }

        @Test
        void extractTestFailuresGroupedWithNullPath() {
            assertThatThrownBy(() -> JUnitXmlParser.extractTestFailuresGrouped(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("XML file path cannot be null");
        }

        @Test
        void extractTestFailuresGroupedWithNullTextContent(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="1.0">
                                <failure message="Test failed" type="AssertionError"></failure>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result).hasSize(1);
            var failure = result.get("TestClass1").getFailures().get(0);
            assertThat(failure.stackTrace()).isEmpty();
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void extractTestFailuresGroupedWithUnreadableFile(@TempDir Path tempDir) throws IOException {
            var tempFile = tempDir.resolve("test.xml");
            Files.createFile(tempFile);
            assertThat(tempFile.toFile().setReadable(false)).isTrue();

            assertThatThrownBy(() -> JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString()))
                    .isInstanceOf(JUnitXmlParserException.class)
                    .hasMessage("File is not readable: " + tempFile);
        }

        @Test
        void extractTestFailuresGroupedWithValidXml(@TempDir Path tempDir) throws IOException {
            var xmlContent = """
                    <testsuites>
                        <testsuite name="TestSuite1">
                            <testcase classname="TestClass1" name="testMethod1" time="1.23">
                                <failure message="Assertion failed" type="AssertionError">StackTrace1</failure>
                            </testcase>
                            <testcase classname="TestClass2" name="testMethod2" time="0.45">
                                <error message="Null pointer exception" type="NullPointerException">StackTrace2</error>
                            </testcase>
                        </testsuite>
                    </testsuites>
                    """;
            var tempFile = tempDir.resolve("test.xml");
            Files.writeString(tempFile, xmlContent);

            var result = JUnitXmlParser.extractTestFailuresGrouped(tempFile.toString());

            assertThat(result)
                    .hasSize(2)
                    .containsKeys("TestClass1", "TestClass2");

            var classFailures1 = result.get("TestClass1");
            assertThat(classFailures1)
                    .isNotNull()
                    .satisfies(failures -> {
                        assertThat(failures.getTotalFailures()).isEqualTo(1);
                        assertThat(failures.getTotalTime()).isEqualTo(1.23);
                        assertThat(failures.getFailures().get(0).testName()).isEqualTo("testMethod1");
                        assertThat(failures.getFailures().get(0).failureType()).isEqualTo("AssertionError");
                    });

            var classFailures2 = result.get("TestClass2");
            assertThat(classFailures2)
                    .isNotNull()
                    .satisfies(failures -> {
                        assertThat(failures.getTotalFailures()).isEqualTo(1);
                        assertThat(failures.getTotalTime()).isEqualTo(0.45);
                        assertThat(failures.getFailures().get(0).testName()).isEqualTo("testMethod2");
                        assertThat(failures.getFailures().get(0).failureType()).isEqualTo("NullPointerException");
                    });
        }
    }

    @Nested
    @DisplayName("Parse Test Cases Tests")
    class ParseTestCasesTests {
        private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

        @Test
        void parseTestCasesWithEmptyNodeList() throws Exception {
            var xml = XML_HEADER + "<testsuite></testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        void parseTestCasesWithFailuresWithinClassAreSorted() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"zebra_test\" classname=\"com.example.TestClass\" time=\"0.1\">" +
                    "    <failure type=\"AssertionError\" message=\"Zebra failure\">Stack trace</failure>" +
                    "  </testcase>" +
                    "  <testcase name=\"alpha_test\" classname=\"com.example.TestClass\" time=\"0.2\">" +
                    "    <failure type=\"AssertionError\" message=\"Alpha failure\">Stack trace</failure>" +
                    "  </testcase>" +
                    "  <testcase name=\"beta_test\" classname=\"com.example.TestClass\" time=\"0.3\">" +
                    "    <failure type=\"AssertionError\" message=\"Beta failure\">Stack trace</failure>" +
                    "  </testcase>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            var classFailures = result.get("com.example.TestClass");
            assertThat(classFailures.getFailures())
                    .extracting(TestFailure::testName)
                    .containsExactly("alpha_test", "beta_test", "zebra_test"); // Should be sorted
        }

        @Test
        @SuppressWarnings("ExtractMethodRecommender")
        void parseTestCasesWithLargeNumberOfTestCases() throws Exception {
            var xmlBuilder = new StringBuilder(XML_HEADER + "<testsuite>");

            // Create many test cases with failures
            for (int i = 0; i < 1500; i++) {
                // Above the 1000 threshold for parallel processing
                xmlBuilder.append(String.format(
                        "<testcase name=\"test%d\" classname=\"com.example.TestClass%d\" time=\"0.%d\">" +
                                "<failure type=\"AssertionError\" message=\"Failure %d\">" +
                                "Stack trace for test %d" +
                                "</failure></testcase>",
                        i, i % 3, i % 100, i, i
                ));
            }
            xmlBuilder.append("</testsuite>");

            var testCases = parseXml(xmlBuilder.toString()).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .as("Test cases should be sorted by test name")
                    .hasSize(3) // 3 different classes
                    .containsKeys("com.example.TestClass0", "com.example.TestClass1", "com.example.TestClass2");

            // Each class should have 500 failures (1500 total / 3 classes)
            assertThat(result.values())
                    .as("Each class should have 500 failures")
                    .allSatisfy(classFailures ->
                            assertThat(classFailures.getFailures()).hasSize(500));

            // Verify the total failure count
            int totalFailures = result.values().stream()
                    .mapToInt(cf -> cf.getFailures().size())
                    .sum();
            assertThat(totalFailures).as("Total count should be 1500").isEqualTo(1500);
        }

        @Test
        void parseTestCasesWithMissingAttributes() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"\" classname=\"\" time=\"\">" +
                    "    <failure type=\"\" message=\"\">" +
                    "    </failure>" +
                    "  </testcase>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .hasSize(1)
                    .containsKey(""); // Empty classname becomes key

            var failure = result.get("").getFailures().get(0);
            assertThat(failure.testName()).isEmpty();
            assertThat(failure.className()).isEmpty();
            assertThat(failure.failureType()).isEqualTo("failure"); // default type
            assertThat(failure.failureMessage()).isEqualTo("No message provided"); // default message
            assertThat(failure.time()).isZero();
        }

        @Test
        void parseTestCasesWithMixedSuccessAndFailures() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"successfulTest\" classname=\"com.example.TestClass\" time=\"0.1\"/>" +
                    "  <testcase name=\"failingTest\" classname=\"com.example.TestClass\" time=\"0.2\">" +
                    "    <failure type=\"AssertionError\" message=\"This test failed\">" +
                    "      Stack trace here" +
                    "    </failure>" +
                    "  </testcase>" +
                    "  <testcase name=\"anotherSuccessfulTest\" classname=\"com.example.TestClass\" time=\"0.3\"/>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .hasSize(1)
                    .containsKey("com.example.TestClass");

            var classFailures = result.get("com.example.TestClass");
            assertThat(classFailures.getFailures())
                    .hasSize(1)
                    .extracting(TestFailure::testName)
                    .containsExactly("failingTest");
        }

        @Test
        void parseTestCasesWithMultipleClasses() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"testAlpha\" classname=\"com.example.AlphaTest\" time=\"0.1\">" +
                    "    <failure type=\"AssertionError\" message=\"Alpha failure\">" +
                    "      Alpha stack trace" +
                    "    </failure>" +
                    "  </testcase>" +
                    "  <testcase name=\"testBeta\" classname=\"com.example.BetaTest\" time=\"0.2\">" +
                    "    <failure type=\"RuntimeException\" message=\"Beta error\">" +
                    "      Beta stack trace" +
                    "    </failure>" +
                    "  </testcase>" +
                    "  <testcase name=\"testGamma\" classname=\"com.example.AlphaTest\" time=\"0.3\">" +
                    "    <failure type=\"IllegalStateException\" message=\"Another alpha failure\">" +
                    "      Another alpha stack trace" +
                    "    </failure>" +
                    "  </testcase>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .hasSize(2)
                    .containsKeys("com.example.AlphaTest", "com.example.BetaTest");

            // Verify AlphaTest has 2 failures
            var alphaFailures = result.get("com.example.AlphaTest");
            assertThat(alphaFailures.getFailures())
                    .hasSize(2)
                    .extracting(TestFailure::testName)
                    .containsExactly("testAlpha", "testGamma");

            // Verify BetaTest has 1 failure
            var betaFailures = result.get("com.example.BetaTest");
            assertThat(betaFailures.getFailures())
                    .hasSize(1)
                    .extracting(TestFailure::testName)
                    .containsExactly("testBeta");
        }

        @Test
        void parseTestCasesWithMultipleFailuresInSameClass() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"testMethod1\" classname=\"com.example.TestClass\" time=\"0.1\">" +
                    "    <failure type=\"AssertionError\" message=\"First failure\">" +
                    "      First stack trace" +
                    "    </failure>" +
                    "  </testcase>" +
                    "  <testcase name=\"testMethod2\" classname=\"com.example.TestClass\" time=\"0.2\">" +
                    "    <failure type=\"AssertionError\" message=\"Second failure\">" +
                    "      Second stack trace" +
                    "    </failure>" +
                    "  </testcase>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .hasSize(1)
                    .containsKey("com.example.TestClass");

            var classFailures = result.get("com.example.TestClass");
            assertThat(classFailures.getFailures())
                    .hasSize(2)
                    .extracting(TestFailure::testName)
                    .containsExactly("testMethod1", "testMethod2");

            assertThat(classFailures.getFailures())
                    .extracting(TestFailure::failureMessage)
                    .containsExactly("First failure", "Second failure");
        }

        @Test
        void parseTestCasesWithResultMapIsSorted() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"test1\" classname=\"zebra.TestClass\" time=\"0.1\">" +
                    "    <failure type=\"AssertionError\" message=\"Failure\">Stack trace</failure>" +
                    "  </testcase>" +
                    "  <testcase name=\"test2\" classname=\"alpha.TestClass\" time=\"0.2\">" +
                    "    <failure type=\"AssertionError\" message=\"Failure\">Stack trace</failure>" +
                    "  </testcase>" +
                    "  <testcase name=\"test3\" classname=\"beta.TestClass\" time=\"0.3\">" +
                    "    <failure type=\"AssertionError\" message=\"Failure\">Stack trace</failure>" +
                    "  </testcase>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result).isInstanceOf(java.util.TreeMap.class);
            assertThat(result.keySet())
                    .containsExactly("alpha.TestClass", "beta.TestClass", "zebra.TestClass");
        }

        @Test
        void parseTestCasesWithSingleError() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"testMethod\" classname=\"com.example.TestClass\" time=\"0.456\">" +
                    "    <error type=\"RuntimeException\" message=\"Unexpected error occurred\">" +
                    "      java.lang.RuntimeException: Unexpected error occurred" +
                    "        at com.example.TestClass.testMethod(TestClass.java:15)" +
                    "    </error>" +
                    "  </testcase>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .hasSize(1)
                    .containsKey("com.example.TestClass");

            TestClassFailures classFailures = result.get("com.example.TestClass");
            assertThat(classFailures.getFailures())
                    .hasSize(1)
                    .first()
                    .satisfies(failure -> {
                        assertThat(failure.testName()).isEqualTo("testMethod");
                        assertThat(failure.failureType()).isEqualTo("RuntimeException");
                        assertThat(failure.failureMessage()).isEqualTo("Unexpected error occurred");
                        assertThat(failure.stackTrace()).contains("java.lang.RuntimeException");
                    });
        }

        @Test
        void parseTestCasesWithSingleFailure() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"testMethod\" classname=\"com.example.TestClass\" time=\"0.123\">" +
                    "    <failure type=\"AssertionError\" message=\"Expected true but was false\">" +
                    "      at com.example.TestClass.testMethod(TestClass.java:42)" +
                    "    </failure>" +
                    "  </testcase>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .hasSize(1)
                    .containsKey("com.example.TestClass");

            var classFailures = result.get("com.example.TestClass");
            assertThat(classFailures.getClassName()).isEqualTo("com.example.TestClass");
            assertThat(classFailures.getFailures())
                    .hasSize(1)
                    .first()
                    .satisfies(failure -> {
                        assertThat(failure.testName()).isEqualTo("testMethod");
                        assertThat(failure.className()).isEqualTo("com.example.TestClass");
                        assertThat(failure.failureType()).isEqualTo("AssertionError");
                        assertThat(failure.failureMessage()).isEqualTo("Expected true but was false");
                        assertThat(failure.stackTrace()).contains("at com.example.TestClass.testMethod(TestClass.java:42)");
                        assertThat(failure.time()).isEqualTo(0.123);
                    });
        }

        @Test
        void parseTestCasesWithSingleSuccessfulTest() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"testMethod\" classname=\"com.example.TestClass\" time=\"0.123\"/>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .isNotNull()
                    .isEmpty();
        }

        @Test
        void parseTestCasesWithTestWithBothFailureAndError() throws Exception {
            String xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"complexTest\" classname=\"com.example.TestClass\" time=\"0.5\">" +
                    "    <failure type=\"AssertionError\" message=\"Assertion failed\">" +
                    "      Failure stack trace" +
                    "    </failure>" +
                    "    <error type=\"RuntimeException\" message=\"Runtime error\">" +
                    "      Error stack trace" +
                    "    </error>" +
                    "  </testcase>" +
                    "</testsuite>";
            NodeList testCases = parseXml(xml).getElementsByTagName("testcase");

            Map<String, TestClassFailures> result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .hasSize(1)
                    .containsKey("com.example.TestClass");

            var classFailures = result.get("com.example.TestClass");
            assertThat(classFailures.getFailures())
                    .hasSize(2)
                    .extracting(TestFailure::failureType)
                    .containsExactlyInAnyOrder("AssertionError", "RuntimeException");

            assertThat(classFailures.getFailures())
                    .extracting(TestFailure::failureMessage)
                    .containsExactlyInAnyOrder("Assertion failed", "Runtime error");
        }

        @Test
        void parseTestCasesWithTestWithDisplayName() throws Exception {
            var xml = String.format("%s<testsuite>" +
                    "  <testcase name=\"testMethod\" classname=\"com.example.TestClass\" time=\"0.1\">" +
                    "    <system-out>" +
                    "      <system-out><![CDATA[" +
                    "          unique-id: [engine:junit-jupiter]%n" +
                    "          display-name: Custom Test Display Name%n" +
                    "      ]]></system-out>" +
                    "    </system-out>" +
                    "    <failure type=\"AssertionError\" message=\"Test failed\">" +
                    "      Stack trace" +
                    "    </failure>" +
                    "  </testcase>" +
                    "</testsuite>", XML_HEADER);
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            var failure = result.get("com.example.TestClass").getFailures().get(0);
            assertThat(failure.displayName()).isEqualTo("Custom Test Display Name");
        }

        @Test
        void parseTestCasesWithTestWithNoFailure() throws Exception {
            String xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"\" classname=\"\" time=\"\">" +
                    "    <failure type=\"\" message=\"\">" +
                    "    </failure>" +
                    "  </testcase>" +
                    "</testsuite>";
            NodeList testCases = parseXml(xml).getElementsByTagName("testcase");

            Map<String, TestClassFailures> result = JUnitXmlParser.parseTestCases(testCases);

            assertThat(result)
                    .hasSize(1)
                    .containsKey(""); // Empty classname becomes key

            var failure = result.get("").getFailures().get(0);
            assertThat(failure.testName()).isEmpty();
            assertThat(failure.className()).isEmpty();
            assertThat(failure.failureType()).isEqualTo("failure"); // default type
            assertThat(failure.failureMessage()).isEqualTo("No message provided"); // default message
            assertThat(failure.time()).isZero();
        }

        @Test
        void parseTestCasesWithTestWithoutDisplayName() throws Exception {
            var xml = XML_HEADER +
                    "<testsuite>" +
                    "  <testcase name=\"testMethod\" classname=\"com.example.TestClass\" time=\"0.1\">" +
                    "    <failure type=\"AssertionError\" message=\"Test failed\">" +
                    "      Stack trace" +
                    "    </failure>" +
                    "  </testcase>" +
                    "</testsuite>";
            var testCases = parseXml(xml).getElementsByTagName("testcase");

            var result = JUnitXmlParser.parseTestCases(testCases);

            var failure = result.get("com.example.TestClass").getFailures().get(0);
            assertThat(failure.displayName()).isEmpty();
        }

        /**
         * Helper method to parse XML string into a Document
         */
        private Document parseXml(String xmlContent)
                throws ParserConfigurationException, IOException, SAXException {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
        }
    }

    @Nested
    @DisplayName("Parse Time Tests")
    class ParseTimeTests {
        @ParameterizedTest(name = "{index} ''{0}''")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        void parseTimeWithBlankAndNullValues(String input) {
            assertThat(JUnitXmlParser.parseTime(input)).isEqualTo(0.0);
        }

        @ParameterizedTest(name = "{index} ''{0}''")
        @ValueSource(strings = {"NaN", "Infinity", "-Infinity", "foo"})
        void parseTimeWithInvalidFormats(String input) {
            assertThat(JUnitXmlParser.parseTime(input)).isEqualTo(0.0);
        }

        @ParameterizedTest
        @CsvSource({
                "'1e308', 1.0E308",
                "'4.9e-324', 4.9E-324",
                "'1.7976931348623157E308', 1.7976931348623157E308"
        })
        void parseTimeWithLargeAndEdgeCaseValues(String input, double expected) {
            assertThat(JUnitXmlParser.parseTime(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "'   3.14', 3.14",
                "'2.718   ', 2.718",
                "'   1.0   ', 1.0"
        })
        void parseTimeWithLeadingAndTrailingSpaces(String input, double expected) {
            assertThat(JUnitXmlParser.parseTime(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "-1.0, -1.0",
                "-100.5, -100.5",
                "-3.14, -3.14"
        })
        void parseTimeWithNegativeValues(String input, double expected) {
            assertThat(JUnitXmlParser.parseTime(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
                "0.0, 0.0",
                "1, 1.0",
                "1.23, 1.23",
                "'  2.5  ', 2.5",
                "1.5e-3, 0.0015"
        })
        void parseTimeWithValidFormats(String input, double expected) {
            assertThat(JUnitXmlParser.parseTime(input)).isEqualTo(expected);
            assertThat(JUnitXmlParser.parseTime("1")).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Text Content Trimmed Tests")
    class TextContentTrimmedTests {


        @Test
        void textContentTrimmedWithDeeplyNestedText() throws Exception {
            var xmlContent = """
                        <root>
                            <outer>
                                <inner>  Nested Content  </inner>
                            </outer>
                        </root>
                    """;
            var document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes()));

            var element = (Element) document.getElementsByTagName("inner").item(0);

            assertThat(JUnitXmlParser.getTextContentTrimmed(element)).isEqualTo("Nested Content");
        }

        @Test
        void textContentTrimmedWithLeadingAndTrailingWhitespace() throws Exception {
            var xmlContent = """
                        <root>
                            <element>   Trimmed Content   </element>
                        </root>
                    """;
            var document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes()));

            var element = (Element) document.getElementsByTagName("element").item(0);

            assertThat(JUnitXmlParser.getTextContentTrimmed(element)).isEqualTo("Trimmed Content");
        }

        @Test
        void textContentTrimmedWithNoTextContent() throws Exception {
            var xmlContent = """
                        <root>
                            <element></element>
                        </root>
                    """;
            var document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlContent.getBytes()));

            var element = (Element) document.getElementsByTagName("element").item(0);

            assertThat(JUnitXmlParser.getTextContentTrimmed(element)).isEmpty();
        }

        @Test
        void textContentTrimmedWithNullContent() throws Exception {
            var document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .newDocument();

            var element = document.createElement("element");

            assertThat(JUnitXmlParser.getTextContentTrimmed(element)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validate File Test")
    class ValidateFileTest {
        @Test
        void validateFilePassesWhenFileIsValid(@TempDir Path tempDir) throws IOException {
            var tempFile = tempDir.resolve("valid-file.xml");
            Files.createFile(tempFile);

            assertThatCode(() -> JUnitXmlParser.validateFile(tempFile)).doesNotThrowAnyException();
        }

        @Test
        void validateFileThrowsOnSecurityException(@TempDir Path tempDir)
                throws IOException {
            var testFile = tempDir.resolve("security-test.xml");
            Files.createFile(testFile);
            Files.writeString(testFile, "<testsuite></testsuite>");

            // Use MockedStatic to simulate SecurityException
            try (var mockedFiles = Mockito.mockStatic(Files.class)) {
                // Mock Files.exists to throw SecurityException
                mockedFiles.when(() -> Files.exists(any(Path.class)))
                        .thenThrow(new SecurityException("Access denied by security policy"));

                assertThatThrownBy(() -> JUnitXmlParser.validateFile(testFile))
                        .isInstanceOf(JUnitXmlParserException.class)
                        .hasMessageContaining("Cannot access file")
                        .hasMessageContaining(testFile.toString())
                        .hasCauseInstanceOf(SecurityException.class)
                        .hasRootCauseMessage("Access denied by security policy");
            }
        }

        @Test
        void validateFileThrowsWhenFileDoesNotExist() {
            var invalidPath = Path.of("nonexistent/path/to/file.xml");

            assertThatThrownBy(() -> JUnitXmlParser.validateFile(invalidPath))
                    .isInstanceOf(JUnitXmlParserException.class)
                    .hasMessage("File does not exist: " + invalidPath);
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void validateFileThrowsWhenFileIsNotReadable(@TempDir Path tempDir) throws IOException {
            var tempFile = tempDir.resolve("test.xml");
            Files.createFile(tempFile);
            assertThat(tempFile.toFile().setReadable(false)).isTrue();

            assertThatThrownBy(() -> JUnitXmlParser.validateFile(tempFile))
                    .isInstanceOf(JUnitXmlParserException.class)
                    .hasMessage("File is not readable: " + tempFile);
        }
    }
}