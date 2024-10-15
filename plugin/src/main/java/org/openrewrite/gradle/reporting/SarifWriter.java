package org.openrewrite.gradle.reporting;

import com.contrastsecurity.sarif.Artifact;
import com.contrastsecurity.sarif.ArtifactChange;
import com.contrastsecurity.sarif.ArtifactContent;
import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Fix;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.Message;
import com.contrastsecurity.sarif.MultiformatMessageString;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Replacement;
import com.contrastsecurity.sarif.ReportingDescriptor;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.contrastsecurity.sarif.Tool;
import com.contrastsecurity.sarif.ToolComponent;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.openrewrite.Result;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.marker.GitProvenance;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SarifWriter extends ResultOutputFileWriter {

    public static final String FORMAT_SARIF = "sarif";

    private static final Logger logger = Logging.getLogger(SarifWriter.class);
    private static final Region START_OF_FILE = new Region().withStartLine(1).withEndLine(1);
    private static final Pattern DIFF_HEADER = Pattern.compile("^@@ -(?<contextStart>\\d+),(?<rows>\\d+) \\+(\\d+),\\d+ @@.*$");

    private final List<com.contrastsecurity.sarif.Result> results;
    private final Set<ReportingDescriptor> rules;
    private final Set<Artifact> artifacts;

    public SarifWriter(final String outputFilePath, final Collection<RecipeDescriptor> activeRecipes, final GitProvenance gitProvenance) throws IOException {
        super(outputFilePath == null ? "sarif.json" : outputFilePath);
        this.results = new ArrayList<>();
        this.rules = initializeRules(activeRecipes);
        this.artifacts = initializeArtifacts(gitProvenance);
    }

    @Override
    public void generated(final Result result) {
        results.addAll(parse(result));
    }

    @Override
    public void deleted(final Result result) {
        results.addAll(parse(result));
    }

    @Override
    public void moved(final Result result) {
        results.addAll(parse(result));
    }

    @Override
    public void altered(final Result result) {
        results.addAll(parse(result));
    }

    @Override
    public void close() {
        super.write(initializeSchema());
        logger.warn("Output sarif-report available:");
        logger.warn("    {}", getReportPath().normalize());
    }

    private SarifSchema210 initializeSchema() {
        return new SarifSchema210().withVersion(SarifSchema210.Version._2_1_0)
                                   .withRuns(initializeRuns())
                                   .with$schema(URI.create("http://json.schemastore.org/sarif-2.1.0-rtm.4"));
    }

    private List<Run> initializeRuns() {
        return Collections.singletonList(new Run().withTool(initializeTool()).withArtifacts(artifacts).withResults(results));
    }

    private Tool initializeTool() {
        return new Tool().withDriver(initializeToolComponent());
    }

    private ToolComponent initializeToolComponent() {
        return new ToolComponent().withName("OpenRewrite")
                                  .withFullName("OpenRewrite")
                                  .withInformationUri(URI.create("https://docs.openrewrite.org/"))
                                  .withRules(rules);
    }

    private Set<ReportingDescriptor> initializeRules(final Collection<RecipeDescriptor> activeRecipes) {
        if (activeRecipes == null) {
            return null;
        }

        return activeRecipes.stream()
                            .flatMap(r -> Stream.concat(Stream.of(r), r.getRecipeList().stream()))
                            .map(this::initializeRule)
                            .collect(Collectors.toSet());
    }

    private ReportingDescriptor initializeRule(RecipeDescriptor recipe) {
        return new ReportingDescriptor().withId(recipe.getName())
                                        .withName(recipe.getDisplayName())
                                        .withFullDescription(new MultiformatMessageString().withText(recipe.getDescription()));
    }

    private Set<Artifact> initializeArtifacts(final GitProvenance gitProvenance) {
        if (gitProvenance == null || gitProvenance.getGitRemote() == null) {
            return null;
        }
        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(new Artifact().withLocation(new ArtifactLocation().withUri(gitProvenance.getGitRemote().getUrl())));

        return artifacts;
    }

    // A result typically represents a change in a file. We need to parse the diff and create a SARIF result for each block of changes.
    // We base ourselves on the diff header to determine if it is a new file, a deleted file, a renamed/moved file or a file that was altered.
    // We use the diff header to determine the start line and number of lines added in the block of changes.
    // We then create a SARIF result with a message that indicates the file was altered by a recipe.
    // If multiple recipes made changes to the file, we list them in the message.
    // We also create a fix for the block of changes if the file was altered, so that suggestions can be generated.
    // We do not set a level on these results as we do not have a concept of change severity in OpenRewrite (yet?)
    private List<com.contrastsecurity.sarif.Result> parse(Result recipeResult) {
        List<com.contrastsecurity.sarif.Result> results = new ArrayList<>();
        String diff = recipeResult.diff();
        List<RecipeDescriptor> recipesThatMadeChanges = recipeResult.getRecipeDescriptorsThatMadeChanges();
        String beforeFile = recipeResult.getBefore() != null ? recipeResult.getBefore().getSourcePath().toString() : null;
        String afterFile = recipeResult.getAfter() != null ? recipeResult.getAfter().getSourcePath().toString() : null;

        int i = 0;
        int blockIndex;
        String[] diffLines = diff.split("\n");
        boolean changesStarted = false;
        while (i < diffLines.length) {
            String line = diffLines[i];
            // Keep processing the diff header
            if (!changesStarted && !line.startsWith("@@")) {
                // If the file was deleted, we skip parsing
                if (line.startsWith("deleted")) {
                    results.add(new com.contrastsecurity.sarif.Result().withMessage(getDeletedMessage(beforeFile, recipesThatMadeChanges))
                                                                       .withLocations(locationsForRegion(beforeFile, START_OF_FILE)));
                    return results;
                }
                // If the file was renamed, we need to add a result for that
                if (line.startsWith("rename from")) {
                    com.contrastsecurity.sarif.Result result = new com.contrastsecurity.sarif.Result().withMessage(getRenamedMessage(beforeFile, afterFile, recipesThatMadeChanges))
                                                                                                      .withLocations(locationsForRegion(beforeFile, START_OF_FILE));
                    results.add(result);
                }
                // If the file was newly created, we need to add a result for that {
                if (line.startsWith("new file")) {
                    // TODO where would we put the comment/suggestion for a new file?
                }
                i++;
                continue;
            }
            // We are now processing the changes.
            // We need to create suggestions per block of changes in order to avoid creating a suggestion per line or from first changed line till last in big files
            changesStarted = true;
            int startRow, addedRows;
            if (line.startsWith("@@")) {
                Matcher matcher = DIFF_HEADER.matcher(line);
                if (matcher.find()) {
                    startRow = Integer.parseInt(matcher.group("contextStart"));
                    addedRows = Integer.parseInt(matcher.group("rows"));
                    blockIndex = 0;
                } else {
                    throw new IllegalArgumentException("Invalid diff header: " + line);
                }
                StringBuilder changes = new StringBuilder();
                while (blockIndex < addedRows) {
                    line = diffLines[++i];
                    if (!line.startsWith("-") && !line.startsWith("\\")) {
                        changes.append(line.substring(1)).append("\n");
                        blockIndex++;
                    }
                }
                Region region = new Region().withStartLine(startRow).withEndLine(startRow + addedRows - 1);
                com.contrastsecurity.sarif.Result result = new com.contrastsecurity.sarif.Result().withMessage(getAlteredMessage(beforeFile, recipesThatMadeChanges))
                                                                                                  .withLocations(locationsForRegion(beforeFile, region))
                                                                                                  .withFixes(fixesForRegion(beforeFile, region, changes.toString()));
                results.add(result);
            }
            i++;
        }

        return results;
    }

    private Message getRenamedMessage(String beforeFile, String afterFile, List<RecipeDescriptor> recipesThatMadeChanges) {
        StringBuilder builder = new StringBuilder("File was renamed from " + beforeFile + " to " + afterFile + " ");
        if (recipesThatMadeChanges.size() > 1) {
            builder.append("by one (or multiple) recipe(s) of:");
            recipesThatMadeChanges.forEach(recipe -> builder.append(String.format("  - %s\n", recipe.getDisplayName())));
        } else {
            builder.append("by recipe ").append(recipesThatMadeChanges.get(0).getDisplayName()).append(".\n");
        }
        return new Message().withText(builder.toString());
    }

    private Message getAlteredMessage(String beforeFile, List<RecipeDescriptor> recipesThatMadeChanges) {
        StringBuilder message = new StringBuilder();
        if (recipesThatMadeChanges.size() > 1) {
            message.append("One (or multiple) recipe(s) of this list made changes to ").append(beforeFile).append(":\n\n");
            recipesThatMadeChanges.forEach(recipe -> message.append(String.format("  - %s\n", recipe.getDisplayName())));
        } else {
            message.append(String.format("According to recipe **%s**, %s was altered", recipesThatMadeChanges.get(0).getDisplayName(), beforeFile));
        }
        return new Message().withText(message.toString());
    }

    private Message getDeletedMessage(String beforeFile, List<RecipeDescriptor> recipesThatMadeChanges) {
        StringBuilder builder = new StringBuilder(beforeFile + " was deleted ");
        if (recipesThatMadeChanges.size() > 1) {
            builder.append("by one (or multiple) recipe(s) of:");
            recipesThatMadeChanges.forEach(recipe -> builder.append(String.format("  - %s\n", recipe.getDisplayName())));
        } else {
            builder.append("by recipe ").append(recipesThatMadeChanges.get(0).getDisplayName()).append(".\n");
        }
        return new Message().withText(builder.toString());
    }

    private List<Location> locationsForRegion(String file, Region region) {
        return listOf(new Location().withPhysicalLocation(new PhysicalLocation().withArtifactLocation(new ArtifactLocation().withUri(file))
                                                                                .withRegion(region)));
    }

    private Set<Fix> fixesForRegion(String file, Region region, String changes) {
        return setOf(new Fix().withArtifactChanges(setOf(new ArtifactChange().withReplacements(listOf(new Replacement().withDeletedRegion(region)
                                                                                                                       .withInsertedContent(new ArtifactContent().withText(
                                                                                                                               changes))))
                                                                             .withArtifactLocation(new ArtifactLocation().withUri(file)))));
    }

    private <T> Set<T> setOf(T... elements) {
        Set<T> set = new HashSet<>();
        Collections.addAll(set, elements);
        return set;
    }

    private <T> List<T> listOf(T... elements) {
        List<T> list = new ArrayList<>();
        Collections.addAll(list, elements);
        return list;
    }
}
