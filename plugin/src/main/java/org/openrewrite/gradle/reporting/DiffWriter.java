package org.openrewrite.gradle.reporting;

import org.openrewrite.Result;
import org.openrewrite.binary.Binary;
import org.openrewrite.quark.Quark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DiffWriter extends ResultOutputFileWriter {

    private static final Logger logger = LoggerFactory.getLogger(DiffWriter.class);
    public static final String FORMAT_DIFF = "diff";

    public DiffWriter(final String outputFilePath) throws IOException {
        super(outputFilePath == null ? "rewrite.patch" : outputFilePath);
    }

    @Override
    public void generated(final Result result) {
        writeDiff(result);
    }

    @Override
    public void deleted(final Result result) {
        writeDiff(result);
    }

    @Override
    public void moved(final Result result) {
        writeDiff(result);
    }

    @Override
    public void altered(final Result result) {
        writeDiff(result);
    }

    @Override
    public void close() {
        logger.warn("Output diff-report available:");
        logger.warn("    {}", getReportPath().normalize());
    }

    private void writeDiff(final Result result) {
        if (result.getAfter() instanceof Binary || result.getAfter() instanceof Quark) {
            // cannot meaningfully display diffs of these things. Console output notes that they were touched by a recipe.
            return;
        }
        write(result.diff() + "\n");
    }
}
