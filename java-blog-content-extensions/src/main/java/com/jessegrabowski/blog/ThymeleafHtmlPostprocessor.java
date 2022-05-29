package com.jessegrabowski.blog;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class ThymeleafHtmlPostprocessor extends Postprocessor {
  @Override
  public String process(Document document, String output) {
    org.jsoup.nodes.Document doc = Jsoup.parse(output, "UTF-8");

    System.out.println("thymleaftest");

    for (Element script : doc.getElementsByTag("script")) {
      String src = script.attr("src");
      System.out.println(src);
      if ("text/x-mathjax-config".equals(script.attr("type"))) {
        script.attr("th:inline", "none");
      }
    }

    for (Element linked : doc.getElementsByAttribute("href")) {
      String url = linked.attr("href");
      linked.removeAttr("href");
      linked.attr("th:href", "@{%s}".formatted(url));
    }

    return doc.html();
  }
}
