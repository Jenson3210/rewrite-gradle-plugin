package org.openrewrite.gradle.reporting;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.openrewrite.Result;
import org.openrewrite.config.RecipeDescriptor;

import java.util.Objects;

import static java.util.stream.Collectors.joining;

public class Slf4jWriter implements ResultWriter {

    private static final Logger logger = Logging.getLogger(Slf4jWriter.class);

    @Override
    public void generated(final Result result) {
        logger.warn("These recipes would generate new file {}:", result.getAfter().getSourcePath());
        logRecipesThatMadeChanges(result);
    }

    @Override
    public void deleted(final Result result) {
        logger.warn("These recipes would delete file {}:", result.getBefore().getSourcePath());
        logRecipesThatMadeChanges(result);
    }

    @Override
    public void moved(final Result result) {
        logger.warn("These recipes would move file from {} to {}:", result.getBefore().getSourcePath(), result.getAfter().getSourcePath());
        logRecipesThatMadeChanges(result);
    }

    @Override
    public void altered(final Result result) {
        logger.warn("These recipes would make changes to {}:", result.getBefore().getSourcePath());
        logRecipesThatMadeChanges(result);
    }

    private void logRecipesThatMadeChanges(final Result result) {
        String indent = "    ";
        String prefix = "    ";
        for (RecipeDescriptor recipeDescriptor : result.getRecipeDescriptorsThatMadeChanges()) {
            logRecipe(recipeDescriptor, prefix);
            prefix = prefix + indent;
        }
    }

    private void logRecipe(RecipeDescriptor rd, String prefix) {
        StringBuilder recipeString = new StringBuilder(prefix + rd.getName());
        if (!rd.getOptions().isEmpty()) {
            String opts = rd.getOptions().stream().map(option -> {
                                                           if (option.getValue() != null) {
                                                               return option.getName() + "=" + option.getValue();
                                                           }
                                                           return null;
                                                       }
            ).filter(Objects::nonNull).collect(joining(", "));
            if (!opts.isEmpty()) {
                recipeString.append(": {").append(opts).append("}");
            }
        }
        logger.warn("{}", recipeString);
        for (RecipeDescriptor rChild : rd.getRecipeList()) {
            logRecipe(rChild, prefix + "    ");
        }
    }
}
