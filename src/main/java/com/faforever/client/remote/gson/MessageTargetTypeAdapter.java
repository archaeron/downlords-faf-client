package com.faforever.client.remote.gson;

import com.faforever.client.remote.domain.MessageTarget;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class MessageTargetTypeAdapter extends TypeAdapter<MessageTarget> {

  public static final MessageTargetTypeAdapter INSTANCE = new MessageTargetTypeAdapter();

  private MessageTargetTypeAdapter() {
  }

  @Override
  public void write(JsonWriter out, MessageTarget value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      out.value(value.getString());
    }
  }

  @Override
  public MessageTarget read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      return null;
    }
    return MessageTarget.fromString(in.nextString());
  }
}
