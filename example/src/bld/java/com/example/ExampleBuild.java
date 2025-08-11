package com.example;

import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.extension.JUnitReporterOperation;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;

public class ExampleBuild extends Project {
    public ExampleBuild() {
        pkg = "com.example";
        name = "example";
        version = version(0, 1, 0);

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);
        scope(test)
                .include(dependency("org.junit.jupiter", "junit-jupiter", version(5, 13, 4)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1, 13, 4)));
    }

    public static void main(String[] args) {
        new ExampleBuild().start(args);
    }

    @BuildCommand(summary = "Test the project with JUnit and run the reporter")
    public void reporter() throws Exception {
        new JUnitReporterOperation()
                .fromProject(this)
                .failOnSummary(true)
                .execute();
    }

    @Override
    public void test() throws Exception {
        var op = testOperation().fromProject(this);
        // Set the reports directory
        op.testToolOptions().reportsDir(new File("build/test-results/test/"));

        try {
            op.execute();
        } catch (ExitStatusException ignore) {
            // Ignore to allow the reporter to run
        }
    }
}