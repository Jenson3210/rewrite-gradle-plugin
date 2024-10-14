package org.openrewrite.gradle.reporting;

import org.openrewrite.Result;

public interface ResultWriter {

    default void open() {};

    void generated(Result result);

    void deleted(Result result);

    void moved(Result result);

    void altered(Result result);

    default void close() {};
}
