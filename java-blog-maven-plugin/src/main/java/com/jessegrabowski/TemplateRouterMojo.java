package com.jessegrabowski;

import java.io.Writer;
import java.nio.file.*;
import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Setter
@Mojo(name = "generate-template-router", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class TemplateRouterMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}")
  private MavenProject project;

  @Parameter(required = true)
  private String templateDirectory;

  @Parameter(required = true)
  private String packageName;

  @Parameter(defaultValue = "${project.build.directory}/generated-sources", required = true)
  private String generatedSourcesDirectory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Path outputPath = Paths.get(generatedSourcesDirectory, packageName.split("\\."));

    try {
      Files.createDirectories(outputPath);
    } catch (Exception e) {
      throw new MojoFailureException(e);
    }

    try (Writer writer =
        Files.newBufferedWriter(outputPath.resolve("TemplateRouterConfiguration.java"))) {
      TemplateVisitor visitor = new TemplateVisitor();
      Files.walkFileTree(Paths.get(templateDirectory), visitor);
      visitor.toJavaFile(packageName).writeTo(writer);
    } catch (Exception e) {
      throw new MojoFailureException(e);
    }

    project.addCompileSourceRoot(generatedSourcesDirectory);
  }
}
