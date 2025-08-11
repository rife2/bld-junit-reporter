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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Custom log handler for capturing log messages during tests
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
class TestLogHandler extends Handler {
    private final List<String> logMessages = new ArrayList<>();
    private final List<LogRecord> logRecords = new ArrayList<>();

    public void clear() {
        logRecords.clear();
        logMessages.clear();
    }

    public boolean containsExactMessage(String message) {
        return logMessages.contains(message);
    }

    public boolean containsMessage(String message) {
        return logMessages.stream().anyMatch(msg -> msg != null && msg.contains(message));
    }

    public long countMessagesContaining(String message) {
        return logMessages.stream()
                .filter(msg -> msg != null && msg.contains(message))
                .count();
    }

    public LogRecord getFirstRecordContaining(String message) {
        return logRecords.stream()
                .filter(record -> record.getMessage() != null && record.getMessage().contains(message))
                .findFirst()
                .orElse(null);
    }

    public List<String> getLogMessages() {
        return new ArrayList<>(logMessages);
    }

    public List<LogRecord> getLogRecords() {
        return new ArrayList<>(logRecords);
    }

    public boolean hasLogLevel(Level level) {
        return logRecords.stream().anyMatch(record -> record.getLevel().equals(level));
    }

    @Override
    public void publish(LogRecord record) {
        logRecords.add(record);
        logMessages.add(record.getMessage());
    }

    @Override
    public void flush() {
        // no-op
    }

    @Override
    public void close() throws SecurityException {
        //no-op
    }
}
