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
            obj.addProperty("type", src.getClass().getSimpleName());
            obj.addProperty("value", src.getValue());
            return obj;
        }
    }

    static public class ValueBooleanAdapter implements JsonSerializer<ValueBoolean> {
        @Override
        public JsonElement serialize(ValueBoolean src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            obj.addProperty("value", src.getValue());
            return obj;
        }
    }
    static public class ValueDoubleAdapter implements JsonSerializer<ValueDouble> {
        @Override
        public JsonElement serialize(ValueDouble src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            obj.addProperty("value", src.getValue());
            return obj;
        }
    }

    static public class ValueStringAdapter implements JsonSerializer<ValueString> {
        @Override
        public JsonElement serialize(ValueString src, Type typeOfSrc,
                                     JsonSerializationContext context) {

            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            obj.addProperty("value", src.getValue());
            return obj;
        }
    }

    static public class ValueNullAdapter implements JsonSerializer<ValueNull> {
        @Override
        public JsonElement serialize(ValueNull src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            return obj;
        }
    }

    static public class ValueDurationAdapter implements JsonSerializer<ValueDuration> {
        @Override
        public JsonElement serialize(ValueDuration src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            obj.addProperty("value", src.getValue());
            return obj;
        }
    }

    static public class ValueTimeAdapter implements JsonSerializer<ValueTime> {
        @Override
        public JsonElement serialize(ValueTime src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            obj.addProperty("value", src.getValue());
            return obj;
        }
    }

    static public class ValueContactAdapter implements JsonSerializer<ValueContact> {
        @Override
        public JsonElement serialize(ValueContact src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            obj.addProperty("name", src.getName().getName());
            obj.addProperty("address", src.getAddress().getHostName());
            return obj;
        }
    }

    static public class PathNameAdapter implements JsonSerializer<PathName> {
        @Override
        public JsonElement serialize(PathName src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            obj.addProperty("name", src.getName());
            return obj;
        }
    }

    static public class ValueSetAdapter implements JsonSerializer<ValueSet> {
        @Override
        public JsonElement serialize(ValueSet src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            JsonArray array = new JsonArray();
            for (Value v : src.getValue()) {
                final JsonElement value = context.serialize(v);
                array.add(value);
            }

            obj.add("value", array);
            return obj;
        }
    }

    static public class ValueListAdapter implements JsonSerializer<ValueList> {
        @Override
        public JsonElement serialize(ValueList src, Type typeOfSrc,
                                     JsonSerializationContext context) {

            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getClass().getSimpleName());
            JsonArray array = new JsonArray();
            for (Value v : src.getValue()) {
                final JsonElement value = context.serialize(v);
                array.add(value);
            }

            obj.add("value", array);
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
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().serializeNulls();
        gsonBuilder.registerTypeAdapter(ValueInt.class, new ValueIntAdapter());
        gsonBuilder.registerTypeAdapter(ValueString.class, new ValueStringAdapter());
        gsonBuilder.registerTypeAdapter(ValueDouble.class, new ValueDoubleAdapter());
        gsonBuilder.registerTypeAdapter(ValueBoolean.class, new ValueBooleanAdapter());
        gsonBuilder.registerTypeAdapter(ValueNull.class, new ValueNullAdapter());
        gsonBuilder.registerTypeAdapter(ValueDuration.class, new ValueDurationAdapter());
        gsonBuilder.registerTypeAdapter(ValueTime.class, new ValueTimeAdapter());
        gsonBuilder.registerTypeAdapter(ValueContact.class, new ValueContactAdapter());
        gsonBuilder.registerTypeAdapter(PathName.class, new PathNameAdapter());

        gsonBuilder.registerTypeAdapter(ValueList.class, new ValueListAdapter());
        gsonBuilder.registerTypeAdapter(ValueSet.class, new ValueSetAdapter());

        gsonBuilder.registerTypeAdapter(ZMI.class, new ZMIAdapter());
        gsonBuilder.registerTypeAdapter(Attribute.class, new AttributeAdapter());
        gsonBuilder.registerTypeAdapter(AttributesMap.class, new AttributesMapAdapter());


        return gsonBuilder.create();
    }


}
