package model;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;

public class CustomJsonSerializer {
    static public class ValueIntAdapter implements JsonSerializer<ValueInt> {

        @Override
        public JsonElement serialize(ValueInt src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "ValueInt");
            obj.addProperty("value", src.getValue());
            return obj;
        }
    }


    static public class ValueListAdapter implements JsonSerializer<ValueList> {

        @Override
        public JsonElement serialize(ValueList src, Type typeOfSrc,
                                     JsonSerializationContext context) {

            JsonObject obj = new JsonObject();
            obj.addProperty("type", "ValueList");
            JsonArray array = new JsonArray();
            for (Value v : src.getValue()) {
                final JsonElement value = context.serialize(v);
                array.add(value);
            }

            obj.add("value", array);
            return obj;
        }
    }

    static public class ValueStringAdapter implements JsonSerializer<ValueString> {

        @Override
        public JsonElement serialize(ValueString src, Type typeOfSrc,
                                     JsonSerializationContext context) {

            JsonObject obj = new JsonObject();
            obj.addProperty("type", "ValueString");
            obj.addProperty("value", src.getValue());
            return obj;
        }
    }

    static public class AttributeAdapter implements JsonSerializer<Attribute> {
        @Override
        public JsonElement serialize(Attribute src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "Attribute");

            obj.addProperty("name", src.getName());
            return obj;
        }
    }

    static public class AttributesMapAdapter implements JsonSerializer<AttributesMap> {
        @Override
        public JsonElement serialize(AttributesMap src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "AttributesMap");
            JsonObject map = new JsonObject();

            for (Map.Entry<Attribute, Value> entry : src) {
                map.add(entry.getKey().getName(), context.serialize(entry.getValue()));
            }

            obj.add("values", map);
            return obj;
        }
    }

    static public class ZMIAdapter implements JsonSerializer<ZMI> {

        @Override
        public JsonElement serialize(ZMI src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", "ZMI");

            final JsonElement attributes = context.serialize(src.getAttributes());
            obj.add("attributes", attributes);
            
            return obj;
        }
    }

    static public Gson getSerializer() {
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(ValueInt.class, new ValueIntAdapter());
        gsonBuilder.registerTypeAdapter(ValueList.class, new ValueListAdapter());
        gsonBuilder.registerTypeAdapter(ZMI.class, new ZMIAdapter());
        gsonBuilder.registerTypeAdapter(Attribute.class, new AttributeAdapter());
        gsonBuilder.registerTypeAdapter(AttributesMap.class, new AttributesMapAdapter());


        return gsonBuilder.create();
    }


}
