package net.oktawia.crazyae2addons.misc;

import net.oktawia.crazyae2addons.interfaces.ICustomNBTSerializable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class NBTContainer {
    private static final byte FIELD_SEP = 0x01;
    private static final byte ENTRY_SEP = 0x02;
    private final Map<String, Object> data = new HashMap<>();

    public void set(String key, Object value) {
        if (!(value instanceof ICustomNBTSerializable))
            throw new IllegalArgumentException("Value for key " + key + " must implement ICustomNBTSerializable");
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public void del(String key){
        data.remove(key);
    }

    public void clear(){
        data.clear();
    }

    public Integer size(){
        return data.size();
    }

    public Stream<?> toStream(){
        return data.entrySet().stream();
    }

    public static String serializeToString(NBTContainer container, boolean compressed) {
        byte[] bytes = container.serialize(compressed);
        if (compressed) {
            return Base64.getEncoder().encodeToString(bytes);
        } else {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static NBTContainer deserializeFromString(String data, boolean compressed) {
        byte[] bytes = compressed ? Base64.getDecoder().decode(data) : data.getBytes(StandardCharsets.UTF_8);
        NBTContainer container = new NBTContainer();
        container.deserialize(bytes);
        return container;
    }

    public byte[] serialize(boolean compressed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        data.forEach((key, val) -> {
            String type = val.getClass().getName();
            String payload = ((ICustomNBTSerializable) val).serialize();
            try {
                writeEscaped(out, key);
                out.write(FIELD_SEP);
                writeEscaped(out, type);
                out.write(FIELD_SEP);
                writeEscaped(out, payload);
                out.write(ENTRY_SEP);
            } catch (Exception ex) {
                throw new RuntimeException("Serialization failed", ex);
            }
        });
        byte[] raw = out.toByteArray();
        return compressed ? compress(raw) : raw;
    }

    public void deserialize(byte[] bytes) {
        data.clear();
        byte[] input = decompress(bytes);
        int start = 0, end;
        while ((end = indexOf(input, ENTRY_SEP, start)) != -1) {
            byte[] entry = Arrays.copyOfRange(input, start, end);
            int sep1 = indexOf(entry, FIELD_SEP, 0);
            int sep2 = indexOf(entry, FIELD_SEP, sep1 + 1);
            if (sep1 < 0 || sep2 < 0) {
                start = end + 1;
                continue;
            }
            String key = unescape(new String(entry, 0, sep1, StandardCharsets.UTF_8));
            String className = unescape(new String(entry, sep1 + 1, sep2 - sep1 - 1, StandardCharsets.UTF_8));
            String payload = unescape(new String(entry, sep2 + 1, entry.length - sep2 - 1, StandardCharsets.UTF_8));
            try {
                Class<?> cls = Class.forName(className);
                Object instance = cls.getDeclaredConstructor().newInstance();
                if (!(instance instanceof ICustomNBTSerializable))
                    throw new IllegalArgumentException("Class " + className + " must implement ICustomNBTSerializable");
                ((ICustomNBTSerializable) instance).deserialize(payload);
                data.put(key, instance);
            } catch (Exception ex) {
                throw new RuntimeException("Deserialization failed for key: " + key, ex);
            }
            start = end + 1;
        }
    }

    private static void writeEscaped(ByteArrayOutputStream out, String s) throws Exception {
        out.write(escape(s).getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] compress(byte[] data) {
        try {
            Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
            def.setInput(data);
            def.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            while (!def.finished()) {
                int count = def.deflate(buf);
                out.write(buf, 0, count);
            }
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Compression failed", ex);
        }
    }

    private static byte[] decompress(byte[] data) {
        try {
            Inflater inf = new Inflater();
            inf.setInput(data);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            while (!inf.finished()) {
                int count = inf.inflate(buf);
                out.write(buf, 0, count);
            }
            return out.toByteArray();
        } catch (Exception ex) {
            return data;
        }
    }

    private static int indexOf(byte[] data, int val, int from) {
        for (int i = from; i < data.length; i++) {
            if ((data[i] & 0xFF) == val)
                return i;
        }
        return -1;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\u0001", "\\x01")
                .replace("\u0002", "\\x02");
    }

    private static String unescape(String s) {
        return s.replace("\\x02", "\u0002")
                .replace("\\x01", "\u0001")
                .replace("\\\\", "\\");
    }
}
