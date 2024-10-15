/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;

import static org.openrewrite.gradle.reporting.DiffWriter.FORMAT_DIFF;
import static org.openrewrite.gradle.reporting.SarifWriter.FORMAT_SARIF;

public class RewriteDryRunTask extends AbstractRewriteTask {

    private static final Logger logger = Logging.getLogger(RewriteDryRunTask.class);

    @Inject
    public RewriteDryRunTask() {
        setGroup("rewrite");
        setDescription("Run the active refactoring recipes, producing a patch file. No source files will be changed.");
        getOutputs().upToDateWhen(Specs.SATISFIES_NONE);
    }

    @TaskAction
    public void run() {
        getProjectParser().dryRun(dumpGcActivity, throwable -> logger.info("Error during rewrite dry run", throwable));
    }

    @OutputFile
    public Path getReportPath() {
        GradleProjectParser parser = getProjectParser();
        String path = parser.getReportPath();
        if (path != null && !path.isEmpty()) {
            return new File(path).toPath();
        }
        String format = parser.getReportFormat();
        Path defaultPath = getProjectLayout()
                .getBuildDirectory()
                .get()
                .getAsFile()
                .toPath()
                .resolve("reports")
                .resolve("rewrite");
        if (format == null || format.isEmpty()) {
            return defaultPath.resolve("rewrite.patch");
        }
        switch (format) {
            case FORMAT_SARIF:
                return defaultPath.resolve("sarif.json");
            case FORMAT_DIFF:
            default:
                return defaultPath.resolve("rewrite.patch");
        }
    }
}
