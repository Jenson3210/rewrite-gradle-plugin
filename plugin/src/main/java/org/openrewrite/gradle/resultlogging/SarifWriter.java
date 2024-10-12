package org.openrewrite.gradle.resultlogging;

import com.contrastsecurity.sarif.Artifact;
import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.MultiformatMessageString;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SarifWriter extends ResultOutputFileWriter {

    private static final Logger logger = Logging.getLogger(SarifWriter.class);

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

    @Override
    public void close() {
        super.write(initializeSchema());
        logger.warn("Output sarif-report available:");
        logger.warn("    {}", getReportPath().normalize());
    }

    private SarifSchema210 initializeSchema() {
        SarifSchema210 report = new SarifSchema210();
        report.setVersion(SarifSchema210.Version._2_1_0);
        report.setRuns(initializeRuns());
        report.set$schema(URI.create("http://json.schemastore.org/sarif-2.1.0-rtm.4"));

        return report;
    }

    private List<Run> initializeRuns() {
        Run run = new Run();
        run.setTool(initializeTool());
        run.setArtifacts(artifacts);
        run.setResults(results);

        return Collections.singletonList(run);
    }

    private Tool initializeTool() {
        Tool tool = new Tool();
        tool.setDriver(initializeToolComponent());

        return tool;
    }

    private ToolComponent initializeToolComponent() {
        ToolComponent toolComponent = new ToolComponent();
        toolComponent.setFullName("OpenRewrite");
        toolComponent.setName("OpenRewrite");
        toolComponent.setInformationUri(URI.create("https://docs.openrewrite.org/"));
        toolComponent.setRules(rules);

        return toolComponent;
    }

    private Set<ReportingDescriptor> initializeRules(final Collection<RecipeDescriptor> activeRecipes) {
        if (activeRecipes == null) {
            return null;
        }

        return activeRecipes.stream().flatMap(r -> Stream.concat(Stream.of(r), r.getRecipeList().stream())).map(this::initializeRule).collect(Collectors.toSet());
    }

    private ReportingDescriptor initializeRule(RecipeDescriptor recipe) {
        ReportingDescriptor rule = new ReportingDescriptor();
        rule.setId(recipe.getName());
        rule.setName(recipe.getDisplayName());
        rule.setHelpUri(recipe.getSource());
        rule.setFullDescription(new MultiformatMessageString().withText(recipe.getDescription()));

        return rule;
    }

    private Set<Artifact> initializeArtifacts(final GitProvenance gitProvenance) {
        if (gitProvenance == null || gitProvenance.getGitRemote() == null) {
            return null;
        }
        Set<Artifact> artifacts = new HashSet<>();

        Artifact artifact = new Artifact();
        artifact.setLocation(new ArtifactLocation().withUri(gitProvenance.getGitRemote().getUrl()));
        artifacts.add(artifact);

        return artifacts;
    }
}
