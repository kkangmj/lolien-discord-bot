package kr.webgori.lolien.discord.bot.spring;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;


import static java.time.format.DateTimeFormatter.ofPattern;

public class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
  public static final DateTimeFormatter FORMATTER = ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

  @Override
  public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    return LocalDateTime.parse(p.getValueAsString(), FORMATTER);
  }
}