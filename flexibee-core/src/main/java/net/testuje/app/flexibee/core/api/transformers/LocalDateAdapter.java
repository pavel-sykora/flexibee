package net.testuje.app.flexibee.core.api.transformers;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateAdapter extends TypeAdapter<LocalDate> {
    public LocalDate read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }
        String s = reader.nextString();
        return LocalDate.parse(s, DateTimeFormatter.ISO_OFFSET_DATE);

    }
    public void write(JsonWriter writer, LocalDate value) throws IOException {
        if (value == null) {
            writer.nullValue();
            return;
        }

        writer.value(value.toString());
    }
}