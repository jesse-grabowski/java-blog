package com.jessegrabowski;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ContentSecurityPolicyGenerator {

  private static final String HASH_ALG = "sha256";
  private final HashMap<String, String> inlineHashes = new HashMap<>();

  public ContentSecurityPolicy generate(Path file) {
    Document document;
    try {
      document = Jsoup.parse(file.toFile());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Set<String> styleSources = new HashSet<>();
    Set<String> attributeStyleSources = new HashSet<>();
    Set<String> elementStyleSources = new HashSet<>();
    Set<String> scriptSources = new HashSet<>();
    Set<String> attributeScriptSources = new HashSet<>();
    Set<String> elementScriptSources = new HashSet<>();
    boolean hasExternalScript = false;
    boolean hasExternalStyle = false;
    boolean hasExternalImage = false;
    boolean hasExternalObject = false;
    boolean hasScriptNonce = false;
    boolean hasStyleNonce = false;

    for (Element element : document.getElementsByAttribute("onload")) {
      String script = element.attr("onload");
      String hash = inlineHashes.computeIfAbsent(script, this::calculateHash);
      attributeScriptSources.add(quoteForCSP(hash));
      scriptSources.add(quoteForCSP(hash));
    }

    for (Element element : document.getElementsByAttribute("style")) {
      String style = element.attr("style");
      String hash = inlineHashes.computeIfAbsent(style, this::calculateHash);
      attributeStyleSources.add(quoteForCSP(hash));
      styleSources.add(quoteForCSP(hash));
    }

    for (Element element : document.getElementsByTag("link")) {
      if ("stylesheet".equals(element.attr("rel"))
          || ("preload".equals(element.attr("rel")) && "style".equals(element.attr("as")))) {
        hasExternalStyle = true;
      } else if ("preload".equals(element.attr("rel")) && "script".equals(element.attr("as"))) {
        hasExternalScript = true;
      }
    }

    for (Element element : document.getElementsByTag("style")) {
      String text = element.data();
      String hash = inlineHashes.computeIfAbsent(text, this::calculateHash);

      elementStyleSources.add(quoteForCSP(hash));
      styleSources.add(quoteForCSP(hash));

      if (element.hasAttr("nonce")) {
        element.attr("th:attr", "nonce=${styleNonce}");
        hasStyleNonce = true;
      }
    }

    for (Element element : document.getElementsByTag("script")) {
      if (element.hasAttr("th:src")) {
        hasExternalScript = true;
      }

      if (StringUtils.isNotEmpty(element.data())) {
        String text = element.data();
        String hash = inlineHashes.computeIfAbsent(text, this::calculateHash);

        elementScriptSources.add(quoteForCSP(hash));
        scriptSources.add(quoteForCSP(hash));
      }

      if (element.hasAttr("nonce")) {
        element.attr("th:attr", "nonce=${scriptNonce}");
        hasScriptNonce = true;
      }
    }

    for (Element element : document.getElementsByTag("img")) {
      hasExternalImage = true;
    }

    for (Element element : document.getElementsByTag("object")) {
      hasExternalObject = true;
    }

    StringBuilder cspBuilder = new StringBuilder();
    cspBuilder.append("default-src 'none';");
    cspBuilder.append("font-src 'self';");

    int scriptNonceFormatCount = 0;

    if (!scriptSources.isEmpty() || hasExternalScript) {
      cspBuilder.append("script-src 'unsafe-inline' ");
      if (hasScriptNonce) {
        cspBuilder.append("'nonce-%s' ");
        scriptNonceFormatCount++;
      }
      cspBuilder.append(String.join(" ", scriptSources));
      cspBuilder.append(";");
    }

    if (!attributeScriptSources.isEmpty()) {
      cspBuilder.append("script-src-attr 'unsafe-hashes' ");
      cspBuilder.append(String.join(" ", attributeScriptSources));
      cspBuilder.append(";");
    }

    if (!elementScriptSources.isEmpty()) {
      cspBuilder.append("script-src-elem ");
      if (hasScriptNonce) {
        cspBuilder.append("'nonce-%s' ");
        scriptNonceFormatCount++;
      }
      cspBuilder.append(String.join(" ", elementScriptSources));
      cspBuilder.append(";");
    }

    int styleNonceFormatCount = 0;

    if (!styleSources.isEmpty() || hasExternalStyle) {
      cspBuilder.append("style-src 'self' ");
      if (hasStyleNonce) {
        cspBuilder.append("'nonce-%s' ");
        styleNonceFormatCount++;
      }
      cspBuilder.append(String.join(" ", styleSources));
      cspBuilder.append(";");
    }

    if (!attributeStyleSources.isEmpty()) {
      cspBuilder.append("style-src-attr 'unsafe-hashes' ");
      cspBuilder.append(String.join(" ", attributeStyleSources));
      cspBuilder.append(";");
    }

    if (!elementStyleSources.isEmpty()) {
      cspBuilder.append("style-src-elem 'self' ");
      if (hasStyleNonce) {
        cspBuilder.append("'nonce-%s' ");
        styleNonceFormatCount++;
      }
      cspBuilder.append(String.join(" ", elementStyleSources));
      cspBuilder.append(";");
    }

    if (hasExternalImage) {
      cspBuilder.append("img-src 'self';");
    }

    if (hasExternalObject) {
      cspBuilder.append("object-src 'self';");
      cspBuilder.append("frame-src 'self';");
    }

    cspBuilder.append("base-uri 'self';");
    cspBuilder.append("form-action 'self';");
    cspBuilder.append("frame-ancestors 'none';");
    cspBuilder.append("require-trusted-types-for 'script';");

    ContentSecurityPolicy csp = new ContentSecurityPolicy();
    CodeBlock.Builder preludeBuilder = CodeBlock.builder();
    if (hasScriptNonce || hasStyleNonce) {
      preludeBuilder.addStatement("byte[] nonceBytes = new byte[16]");
    }
    if (hasScriptNonce) {
      preludeBuilder.addStatement(
          "$T.current().nextBytes(nonceBytes)",
          ClassName.bestGuess("java.util.concurrent.ThreadLocalRandom"));
      preludeBuilder.addStatement(
          "$T scriptNonce = $T.getEncoder().encodeToString(nonceBytes)",
          ClassName.bestGuess("java.lang.String"),
          ClassName.bestGuess("java.util.Base64"));
    }
    if (hasStyleNonce) {
      preludeBuilder.addStatement(
          "$T.current().nextBytes(nonceBytes)",
          ClassName.bestGuess("java.util.concurrent.ThreadLocalRandom"));
      preludeBuilder.addStatement(
          "$T styleNonce = $T.getEncoder().encodeToString(nonceBytes)",
          ClassName.bestGuess("java.lang.String"),
          ClassName.bestGuess("java.util.Base64"));
    }
    csp.setPrelude(preludeBuilder.build());

    CodeBlock.Builder policyBuilder = CodeBlock.builder();
    policyBuilder.add(".header($S, $S", "Content-Security-Policy", cspBuilder.toString());
    List<CodeBlock> blocks = new ArrayList<>();
    if (hasScriptNonce) {
      csp.getModelEntries()
          .add(
              CodeBlock.of(
                  "$T.entry($S, scriptNonce)",
                  ClassName.bestGuess("java.util.Map"),
                  "scriptNonce"));
      for (int i = 0; i < scriptNonceFormatCount; i++) {
        blocks.add(CodeBlock.of("scriptNonce"));
      }
    }
    if (hasStyleNonce) {
      csp.getModelEntries()
          .add(
              CodeBlock.of(
                  "$T.entry($S, styleNonce)", ClassName.bestGuess("java.util.Map"), "styleNonce"));
      for (int i = 0; i < scriptNonceFormatCount; i++) {
        blocks.add(CodeBlock.of("styleNonce"));
      }
    }
    if (!blocks.isEmpty()) {
      policyBuilder.add(".formatted($L)", CodeBlock.join(blocks, ", "));
    }
    policyBuilder.add(CodeBlock.of(")"));
    csp.setPolicy(policyBuilder.build());

    try {
      Files.writeString(file, document.html());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return csp;
  }

  private String calculateHash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALG);
      return HASH_ALG
          + "-"
          + Base64.getEncoder()
              .encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String quoteForCSP(String value) {
    return "'" + value + "'";
  }

  @Data
  public static class ContentSecurityPolicy {
    private CodeBlock prelude;
    private CodeBlock policy;
    private List<CodeBlock> modelEntries = new ArrayList<>();
  }
}
