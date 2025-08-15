package com.example;

import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.extension.JUnitReporterOperation;

import java.io.File;
import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Repository.RIFE2_RELEASES;
import static rife.bld.dependencies.Scope.test;

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

    @BuildCommand(summary = "Runs the JUnit reporter (take option)",
            description = "Usage: reporter [--i[ndex]=GROUP_INDEX[.FAILURE_INDEX]]")
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
        op.execute();
    }
}