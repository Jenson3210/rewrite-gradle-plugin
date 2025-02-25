/*
 * Copyright 2025 the original author or authors.
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

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.function.UnaryOperator;

/**
 * A {@link PrintOutputCapture} that sanitizes the diff of informational markers,
 * so these aren't accidentally committed to source control.
 */
public class SanitizedMarkerPrinter implements PrintOutputCapture.MarkerPrinter {
    @Override
    public String beforeSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
        if (marker instanceof SearchResult) {
            return DEFAULT.beforeSyntax(marker, cursor, commentWrapper);
        }
        return "";
    }

    @Override
    public String afterSyntax(Marker marker, Cursor cursor, UnaryOperator<String> commentWrapper) {
        if (marker instanceof SearchResult) {
            return DEFAULT.afterSyntax(marker, cursor, commentWrapper);
        }
        return "";
    }
}
