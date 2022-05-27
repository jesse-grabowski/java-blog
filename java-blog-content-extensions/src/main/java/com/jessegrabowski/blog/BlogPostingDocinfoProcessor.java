package com.jessegrabowski.blog;

import com.google.schemaorg.JsonLdSerializer;
import com.google.schemaorg.core.BlogPosting;
import com.google.schemaorg.core.CoreFactory;
import com.google.schemaorg.core.ItemList;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.DocinfoProcessor;
import org.jsoup.Jsoup;

public class BlogPostingDocinfoProcessor extends DocinfoProcessor {

  private static final int EXPECTED_WPM = 200;

  @Override
  public String process(Document document) {
    if (!"article".equals(document.getAttribute("doctype"))) {
      return "";
    }

    BlogPosting.Builder builder = CoreFactory.newBlogPostingBuilder();

    addAsciidoctorAttribute(document, builder, "abstract", (b, a) -> b.addProperty("abstract", a));
    addAsciidoctorAttribute(
        document, builder, "backstory", (b, a) -> b.addProperty("backstory", a));
    addAsciidoctorAttribute(
        document, builder, "description", (b, a) -> b.addProperty("description", a));
    addAsciidoctorAttribute(
        document,
        builder,
        "audience",
        (b, a) -> b.addAudience(CoreFactory.newAudienceBuilder().addAudienceType(a)));
    addAsciidoctorAttribute(document, builder, "author", (b, a) -> b.addAuthor(Authors.lookup(a)));
    addAsciidoctorAttribute(document, builder, "docdate", BlogPosting.Builder::addDateModified);
    addAsciidoctorAttribute(
        document, builder, "published_at", BlogPosting.Builder::addDatePublished);
    addAsciidoctorAttribute(document, builder, "created_at", BlogPosting.Builder::addDateCreated);
    addAsciidoctorAttribute(document, builder, "lang", BlogPosting.Builder::addInLanguage);
    addAsciidoctorAttribute(
        document,
        builder,
        "keywords",
        (b, a) -> Arrays.stream(a.split(",\\s*")).forEach(b::addKeywords));
    addAsciidoctorAttribute(document, builder, "teaches", (b, a) -> b.addProperty("teaches", a));

    addAccessMode(document, builder);
    addAccessibilityFeatures(document, builder);
    addAccessibilityHazards(document, builder);
    addWordCount(document, builder);
    addTimeRequired(document, builder);
    addText(document, builder);

    builder.addCopyrightHolder(Authors.JESSE_GRABOWSKI);
    addAsciidoctorAttribute(
        document, builder, "copyright", (b, a) -> b.addProperty("copyrightNotice", a));

    builder.addProperty("creativeWorkStatus", "Published");

    builder.addHeadline(document.getTitle());
    builder.addProperty("isAccessibleForFree", "true");

    try {
      return """
              <script type="application/ld+json">
              %s
              </script>"""
          .formatted(new JsonLdSerializer(true).serialize(builder.build()));
    } catch (Exception e) {
      return "";
    }
  }

  private void addAsciidoctorAttribute(
      Document document,
      BlogPosting.Builder builder,
      String attribute,
      BiConsumer<BlogPosting.Builder, String> builderMethod) {
    if (document.hasAttribute(attribute)) {
      builderMethod.accept(builder, String.valueOf(document.getAttribute(attribute)));
    }
  }

  private void addAccessMode(Document document, BlogPosting.Builder builder) {
    Set<String> accessModes = new HashSet<>();
    accessModes.add("textual");
    for (StructuralNode node : document.findBy(Map.of("context", ":image"))) {
      if ("plantuml".equals(node.getAttributes().get("1"))) {
        accessModes.add("diagramOnVisual");
      }
    }
    if (!document.findBy(Map.of("context", ":stem")).isEmpty()) {
      accessModes.add("mathOnVisual");
    }

    for (String accessMode : accessModes) {
      builder.addProperty("accessMode", accessMode);
    }
    if (accessModes.size() > 1) {
      ItemList.Builder itemListBuilder = CoreFactory.newItemListBuilder();
      for (String accessMode : accessModes) {
        itemListBuilder.addItemListElement(accessMode);
      }
      builder.addProperty(
          "accessModeSufficient", itemListBuilder.addDescription("Full experience"));
    }
    builder.addProperty(
        "accessModeSufficient",
        CoreFactory.newItemListBuilder().addItemListElement("textual").addDescription("Text only"));
  }

  private void addAccessibilityFeatures(Document document, BlogPosting.Builder builder) {
    builder.addAccessibilityFeature("tableOfContents");
    builder.addAccessibilityFeature("structuralNavigation");
    builder.addAccessibilityFeature("unlocked");
    builder.addAccessibilityFeature("highContrastDisplay");

    if (!document.findBy(Map.of("context", ":image")).isEmpty()) {
      builder.addAccessibilityFeature("alternativeText");
    }

    if (!document.findBy(Map.of("context", ":stem")).isEmpty()) {
      builder.addAccessibilityFeature("describedMath");
      builder.addAccessibilityFeature("MathML");
    }
  }

  private void addAccessibilityHazards(Document document, BlogPosting.Builder builder) {
    builder.addAccessibilityHazard("none");
  }

  private void addWordCount(Document document, BlogPosting.Builder builder) {
    int wordCount = 0;
    for (StructuralNode node : document.findBy(Map.of("context", ":paragraph"))) {
      wordCount += Jsoup.parse(String.valueOf(node.getContent())).text().split("\\s+").length;
    }
    builder.addWordCount("" + wordCount);
  }

  private void addText(Document document, BlogPosting.Builder builder) {
    builder.addText(
        document.findBy(Map.of("context", ":paragraph")).stream()
            .map(StructuralNode::getContent)
            .map(String::valueOf)
            .map(Jsoup::parse)
            .map(org.jsoup.nodes.Document::text)
            .collect(Collectors.joining(" ")));
  }

  private void addTimeRequired(Document document, BlogPosting.Builder builder) {
    int wordCount = 0;
    for (StructuralNode node : document.findBy(Map.of("context", ":paragraph"))) {
      wordCount += Jsoup.parse(String.valueOf(node.getContent())).text().split("\\s+").length;
    }
    builder.addTimeRequired(Duration.ofMinutes(wordCount / EXPECTED_WPM + 5).toString());
  }
}
