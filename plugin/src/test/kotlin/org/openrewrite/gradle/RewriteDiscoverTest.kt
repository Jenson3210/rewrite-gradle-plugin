/*
 * Licensed under the Moderne Source Available License.
 * See https://docs.moderne.io/licensing/moderne-source-available-license
 */
package org.openrewrite.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIf
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import java.io.File

class RewriteDiscoverTest : RewritePluginTest {

    override fun taskName(): String = "rewriteDiscover"

    @Issue("https://github.com/openrewrite/rewrite-gradle-plugin/issues/33")
    @Test
    fun `rewriteDiscover prints recipes from external dependencies`(
        @TempDir projectDir: File
    ) {
        gradleProject(projectDir) {
            buildGradle("""
                plugins {
                    id("java")
                    id("org.openrewrite.rewrite")
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                    maven {
                        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
                    }
                }

                dependencies {
                    rewrite("org.openrewrite.recipe:rewrite-testing-frameworks:latest.release")
                }

                rewrite {
                     activeRecipe("org.openrewrite.java.testing.junit5.JUnit5BestPractices")
                     activeRecipe("org.openrewrite.java.format.AutoFormat")
                     activeStyle("org.openrewrite.java.SpringFormat")
                }
            """)

        }
        val result = runGradle(projectDir, taskName())
        val rewriteDiscoverResult = result.task(":${taskName()}")!!
        assertThat(rewriteDiscoverResult.outcome).isEqualTo(TaskOutcome.SUCCESS)

        assertThat(result.output).contains("Configured with 2 active recipes and 1 active styles.")
    }
}
