package com.alibaba.jvm.sandbox.repeater.plugin.core.serialize;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Identity;
import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import org.kohsuke.MetaInfServices;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@MetaInfServices(Serializer.class)
public class JSonStringSerializer implements Serializer {

    public static SerializerFeature[] features = new SerializerFeature[]{
            SerializerFeature.IgnoreErrorGetter,
            SerializerFeature.IgnoreNonFieldGetter,
            SerializerFeature.WriteMapNullValue,
            SerializerFeature.SkipTransientField,
    };

    @Override
    public Type type() {
        return Type.JSON_STRING;
    }

    @Override
    public String serialize2String(Object object) throws SerializeException {
        return serialize2String(object, null);
    }

    @Override
    public String serialize2String(Object object, ClassLoader classLoader) throws SerializeException {
        byte[] bytes = serialize(object, classLoader);
        return new String(bytes, Charsets.UTF_8);
    }

    @Override
    public byte[] serialize(Object object) throws SerializeException {
        return serialize(object, null);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) throws SerializeException {
        return bytes == null ? null : deserialize(bytes, type, null);
    }

    @Override
    public <T> T deserialize(String sequence, Class<T> type) throws SerializeException {
        return sequence == null ? null : deserialize(sequence, type, null);
    }

    @Override
    public <T> T deserialize(String sequence, Class<T> type, ClassLoader classLoader) throws SerializeException {
        return sequence == null ? null : deserialize(sequence.getBytes(Charsets.UTF_8), type, classLoader);
    }

    @Override
    public Object deserialize(String sequence) throws SerializeException{
        return sequence == null ? null : deserialize(sequence.getBytes(Charsets.UTF_8));
    }


    @Override
    public byte[] serialize(Object object, ClassLoader classLoader) throws SerializeException {
        ClassLoader swap = Thread.currentThread().getContextClassLoader();
        try {
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            return JSON.toJSONBytes(object, features);
        } catch (Throwable t) {
            // may produce sof exception
            throw new SerializeException("[Error-1001]-json-serialize-error", t);
        } finally {
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(swap);
            }
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type, ClassLoader classLoader) throws SerializeException {
        ClassLoader swap = Thread.currentThread().getContextClassLoader();
        try {
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            return JSON.parseObject(bytes, type);
        } catch (Throwable t) {
            throw new SerializeException("[Error-1002]-json-deserialize-error", t);
        } finally {
            if (classLoader != null) {
                Thread.currentThread().setContextClassLoader(swap);
            }
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializeException {
        try {
            return JSON.parse(bytes);
        } catch (Throwable t) {
            throw new SerializeException("[Error-1002]-json-deserialize-error", t);
        }
    }

    public static class Holder{
        public Holder(byte[] bytes) {
            this.bytes = bytes;
        }

        public byte[] bytes;
    }

    public static void main(String[] args) {
        JSonStringSerializer serializer = new JSonStringSerializer();
        Map<String, Object> map = new HashMap<String, Object>(10);
        map.put("p1", new Holder(new byte[]{1,2,3}));
        try {
            byte[] bytes = serializer.serialize(map);
            System.out.println(new String(bytes, Charsets.UTF_8));
            Map<String, Object> map1 = serializer.deserialize(bytes, Map.class);
            JSONObject ret = (JSONObject)map1.get("p1");
            System.out.println(ret.get("bytes"));
            final String bytes1 = (String)ret.get("bytes");
            System.out.println(Arrays.toString(BaseEncoding.base64().decode(bytes1)));
        } catch (SerializeException e) {
            e.printStackTrace();
        }
        byte[] a = new byte[]{1,2,3};
        System.out.println(a.getClass().getName());
    }
}
