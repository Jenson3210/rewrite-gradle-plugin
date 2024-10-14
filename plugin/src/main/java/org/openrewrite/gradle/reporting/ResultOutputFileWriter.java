package org.openrewrite.gradle.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class ResultOutputFileWriter implements ResultWriter {

    private final Path reportPath;
    private final BufferedWriter writer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResultOutputFileWriter(final String reportPath) throws IOException {
        assert reportPath != null;
        File targetFile = new File(reportPath);
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }
        this.reportPath = targetFile.toPath();
        this.writer = Files.newBufferedWriter(this.reportPath);
    }

    public Path getReportPath() {
        return reportPath;
    }

    protected void write(String s) {
        try(BufferedWriter bufferedWriter = writer) {
            bufferedWriter.write(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void write(Object o) {
        try {
            write(objectMapper.writeValueAsString(o));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
