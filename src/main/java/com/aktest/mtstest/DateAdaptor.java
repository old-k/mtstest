package com.aktest.mtstest;

import java.lang.reflect.Type;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DateAdaptor implements JsonSerializer<Timestamp>, JsonDeserializer<Timestamp>{
		@Override
		public Timestamp deserialize(JsonElement el, Type type, JsonDeserializationContext context) throws JsonParseException {
			try {
				return new Timestamp(df.parse(el.getAsJsonPrimitive().getAsString()).getTime());
			} catch (Exception e) {
				throw new JsonParseException(e);
			}
		}

		static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		@Override
		public JsonElement serialize(Timestamp cal, Type type, JsonSerializationContext context) {
			try {
				return new JsonPrimitive(df.format(cal));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
}
