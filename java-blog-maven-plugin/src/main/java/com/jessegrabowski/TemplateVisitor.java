package com.jessegrabowski;

import com.squareup.javapoet.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import javax.lang.model.element.Modifier;

public class TemplateVisitor implements FileVisitor<Path> {

  private final MethodSpec.Builder methodSpecBuilder;
  private final Deque<CodeBlock.Builder> chain;
  private final Deque<String> path;

  public TemplateVisitor() {
    this.methodSpecBuilder =
        MethodSpec.methodBuilder("templateRouterFunction")
            .addAnnotation(ClassName.bestGuess("org.springframework.context.annotation.Bean"))
            .returns(
                ParameterizedTypeName.get(
                    ClassName.bestGuess("org.springframework.web.servlet.function.RouterFunction"),
                    ClassName.bestGuess(
                        "org.springframework.web.servlet.function.ServerResponse")));

    this.chain = new ArrayDeque<>();
    this.path = new ArrayDeque<>();
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    path.push(dir.getFileName().toString());
    chain.push(CodeBlock.builder());
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    if (!".html".equals(getFileExtension(file))) {
      return FileVisitResult.CONTINUE;
    }

    CodeBlock.Builder builder = chain.peek();
    builder.add(
        "$L($L, request -> $T.ok().render($S))",
        chainIfRequired(builder, "route", "andRoute"),
        getFilePredicate(file),
        ClassName.bestGuess("org.springframework.web.servlet.function.ServerResponse"),
        getTemplate(file));

    return FileVisitResult.CONTINUE;
  }

  private CodeBlock getFilePredicate(Path file) {
    String fileName = getFileName(file);
    String fileExtension = getFileExtension(file);

    CodeBlock.Builder builder =
        CodeBlock.builder()
            .add(
                "$T.path($S)",
                ClassName.bestGuess("org.springframework.web.servlet.function.RequestPredicates"),
                "/" + fileName)
            .add(
                ".or($T.path($S))",
                ClassName.bestGuess("org.springframework.web.servlet.function.RequestPredicates"),
                "/" + fileName + fileExtension);

    if (fileName.equals("index")) {
      builder.add(
          ".or($T.path($S))",
          ClassName.bestGuess("org.springframework.web.servlet.function.RequestPredicates"),
          "/");
    }

    return CodeBlock.builder()
        .add(
            "$T.method($T.GET).and($L)",
            ClassName.bestGuess("org.springframework.web.servlet.function.RequestPredicates"),
            ClassName.bestGuess("org.springframework.http.HttpMethod"),
            builder.build())
        .build();
  }

  private String getTemplate(Path file) {
    Iterator<String> itr = path.iterator();
    StringBuilder builder = new StringBuilder();
    while (itr.hasNext()) {
      String part = itr.next();
      if (itr.hasNext()) {
        builder.append(part);
        builder.append('/');
      }
    }
    builder.append(getFileName(file));
    return builder.toString();
  }

  private String getFileName(Path file) {
    String fileNameString = file.getFileName().toString();
    return fileNameString.substring(0, fileNameString.lastIndexOf('.'));
  }

  private String getFileExtension(Path file) {
    String fileNameString = file.getFileName().toString();
    return fileNameString.substring(fileNameString.lastIndexOf('.'));
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    throw exc;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    CodeBlock.Builder directoryContents = chain.pop();

    if (directoryContents == null) {
      return FileVisitResult.CONTINUE;
    }

    if (chain.isEmpty()) {
      methodSpecBuilder.addStatement("return $L", directoryContents.build());
      return FileVisitResult.CONTINUE;
    }

    CodeBlock.Builder builder = chain.peek();
    builder.add(
        "$L($T.path($S), $L)",
        chainIfRequired(builder, "nest", "andNest"),
        ClassName.bestGuess("org.springframework.web.servlet.function.RequestPredicates"),
        "/" + dir.getFileName().toString(),
        directoryContents.build());

    path.pop();

    return FileVisitResult.CONTINUE;
  }

  private CodeBlock chainIfRequired(CodeBlock.Builder builder, String unchained, String chained) {
    return builder.isEmpty()
        ? CodeBlock.builder()
            .add(
                "$T.$L",
                ClassName.bestGuess("org.springframework.web.servlet.function.RouterFunctions"),
                unchained)
            .build()
        : CodeBlock.builder().add(".$L", chained).build();
  }

  public JavaFile toJavaFile(String packageName) {
    return JavaFile.builder(
            packageName,
            TypeSpec.classBuilder("TemplateRouterConfiguration")
                .addAnnotation(
                    ClassName.bestGuess("org.springframework.context.annotation.Configuration"))
                .addModifiers(Modifier.PUBLIC)
                .addMethod(methodSpecBuilder.build())
                .build())
        .build();
  }
}
