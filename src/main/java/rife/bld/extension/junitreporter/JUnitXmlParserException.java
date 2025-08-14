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

import java.io.Serial;

/**
 * Custom exception for XML parsing errors.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class JUnitXmlParserException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code JUnitXmlParserException} with the specified detail message.
     *
     * @param message the detail message describing the error
     */
    public JUnitXmlParserException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code JUnitXmlParserException} with the specified detail message and cause.
     *
     * @param message the detail message describing the error
     * @param cause   the cause of the exception. A {@code null} value is permitted and indicates that the cause is
     *                nonexistent or unknown
     */
    public JUnitXmlParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
