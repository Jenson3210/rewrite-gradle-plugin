/*
 * Copyright 2024 the original author or authors.
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

import org.gradle.internal.service.ServiceRegistry;
import org.openrewrite.gradle.resultlogging.ResultWriter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface GradleProjectParser {
    List<String> getActiveRecipes();

    List<String> getActiveStyles();

    @Nullable
    String getReportPath();

    @Nullable
    String getReportFormat();

    ResultWriter getResultWriter() throws IOException;

    List<String> getAvailableStyles();

    Collection<Path> listSources();

    void discoverRecipes(ServiceRegistry serviceRegistry);

    /**
     * @deprecated Use {@link #discoverRecipes(ServiceRegistry)} instead.
     */
    @Deprecated
    default void discoverRecipes(boolean interactive, ServiceRegistry serviceRegistry) {
        discoverRecipes(serviceRegistry);
    }

    void run(Consumer<Throwable> onError);

    void dryRun(boolean dumpGcActivity, Consumer<Throwable> onError);

    void shutdownRewrite();
}
