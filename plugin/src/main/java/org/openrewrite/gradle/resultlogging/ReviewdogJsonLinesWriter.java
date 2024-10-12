package org.openrewrite.gradle.resultlogging;

import org.openrewrite.Result;

import java.io.IOException;

public class ReviewdogJsonLinesWriter extends ResultOutputFileWriter {

    public ReviewdogJsonLinesWriter(String outputFilePath) throws IOException {
        super(outputFilePath == null ? "rewrite.jsonl" : outputFilePath);
    }

    @Override
    public void generated(final Result result) {
    }

    @Override
    public void deleted(final Result result) {

    }

    @Override
    public void moved(final Result result) {

    }

    @Override
    public void altered(final Result result) {

    }
}
