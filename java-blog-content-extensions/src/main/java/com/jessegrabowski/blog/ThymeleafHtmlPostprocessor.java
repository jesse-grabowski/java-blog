package com.jessegrabowski.blog;

import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class ThymeleafHtmlPostprocessor extends Postprocessor {
  @Override
  public String process(Document document, String output) {
    String baseDir = String.valueOf(document.getAttribute("outroot"));
    String outFile = String.valueOf(document.getAttribute("outfile"));
    String normalizedBase =
        StringUtils.removeStart(
            Paths.get(outFile).normalize().toString().replace('\\', '/'),
            Paths.get(baseDir).toString().replace('\\', '/'));
    normalizedBase = StringUtils.removeEnd(normalizedBase, ".html");
    normalizedBase = StringUtils.removeEnd(normalizedBase, "index");

    org.jsoup.nodes.Document doc = Jsoup.parse(output, "http://localhost" + normalizedBase);

    for (Element script : doc.getElementsByTag("script")) {
      if ("text/x-mathjax-config".equals(script.attr("type"))) {
        script.attr("th:inline", "none");
      }
    }

    for (Element linked : doc.getElementsByTag("object")) {
      if ("image/svg+xml".equals(linked.attr("type"))) {
        String url = linked.absUrl("data");
        linked.removeAttr("data");
        linked.attr("th:data", "@{%s}".formatted(StringUtils.removeStart(url, "http://localhost")));
      }
    }

    for (Element linked : doc.getElementsByAttribute("href")) {
      String url = linked.absUrl("href");
      linked.removeAttr("href");
      linked.attr("th:href", "@{%s}".formatted(StringUtils.removeStart(url, "http://localhost")));
    }

    for (Element linked : doc.getElementsByAttribute("src")) {
      String url = linked.absUrl("src");
      linked.removeAttr("src");
      linked.attr("th:src", "@{%s}".formatted(StringUtils.removeStart(url, "http://localhost")));
    }

    return doc.html();
  }
}
