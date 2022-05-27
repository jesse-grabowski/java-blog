package com.jessegrabowski.blog;

import com.google.schemaorg.core.CoreFactory;
import com.google.schemaorg.core.Person;
import java.util.Locale;

public class Authors {

  public static Person JESSE_GRABOWSKI =
      CoreFactory.newPersonBuilder()
          .addName("Jesse Grabowski")
          .addGivenName("Jesse")
          .addFamilyName("Grabowski")
          .addGender("male")
          .addJobTitle("Software Architect")
          .build();

  public static Person lookup(String author) {
    return switch (author.toLowerCase(Locale.ROOT)) {
      case "jesse grabowski" -> JESSE_GRABOWSKI;
      default -> CoreFactory.newPersonBuilder().addName(author).build();
    };
  }
}
