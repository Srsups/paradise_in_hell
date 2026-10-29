package br.com.srsups.paradiseinhell;

import com.badlogic.gdx.utils.JsonValue;
import java.util.HashSet;
import java.util.Set;

/** Explicit, bounded JSON readers shared by checkpoint models. No reflective field access. */
public final class SnapshotJson {
    private SnapshotJson() {}
    public static void object(JsonValue value,String... names) {
        if(value==null||!value.isObject())throw invalid("objeto");
        Set<String> expected=Set.of(names),found=new HashSet<>();
        for(JsonValue field:value)
            if(field.name==null||!expected.contains(field.name)||!found.add(field.name))throw invalid("campo desconhecido ou repetido");
        if(!found.equals(expected))throw invalid("campo ausente");
    }
    public static JsonValue required(JsonValue object,String name) {
        if(object==null||!object.isObject()||!object.has(name))throw invalid(name);
        return object.get(name);
    }
    public static JsonValue array(JsonValue object,String name) {
        JsonValue value=required(object,name);
        if(!value.isArray())throw invalid(name);
        return value;
    }
    public static float number(JsonValue object,String name,float minimum,float maximum) {
        JsonValue value=required(object,name);
        if(!value.isNumber())throw invalid(name);
        float number=value.asFloat();
        if(!Float.isFinite(number)||number<minimum||number>maximum)throw invalid(name);
        return number;
    }
    public static float nonNegative(JsonValue object,String name) {return number(object,name,0,Float.MAX_VALUE);}
    public static int integer(JsonValue object,String name,int minimum,int maximum) {
        JsonValue value=required(object,name);
        if(!value.isLong()||value.asLong()<minimum||value.asLong()>maximum)throw invalid(name);
        return value.asInt();
    }
    public static long longInteger(JsonValue object,String name,long minimum,long maximum) {
        JsonValue value=required(object,name);
        if(!value.isLong()||value.asLong()<minimum||value.asLong()>maximum)throw invalid(name);
        return value.asLong();
    }
    public static boolean flag(JsonValue object,String name) {
        JsonValue value=required(object,name);
        if(!value.isBoolean())throw invalid(name);
        return value.asBoolean();
    }
    public static String text(JsonValue object,String name) {
        JsonValue value=required(object,name);
        if(!value.isString())throw invalid(name);
        return value.asString();
    }
    public static <E extends Enum<E>> E enumValue(JsonValue object,String name,Class<E> type) {
        try{return Enum.valueOf(type,text(object,name));}
        catch(IllegalArgumentException invalid){throw invalid(name);}
    }
    public static void put(JsonValue object,String name,float value) {
        if(!Float.isFinite(value))throw invalid(name);
        object.addChild(name,new JsonValue((double)value));
    }
    public static void put(JsonValue object,String name,long value){object.addChild(name,new JsonValue(value));}
    public static void put(JsonValue object,String name,boolean value){object.addChild(name,new JsonValue(value));}
    public static void put(JsonValue object,String name,String value){object.addChild(name,new JsonValue(value));}
    public static IllegalArgumentException invalid(String name){return new IllegalArgumentException("Estado de checkpoint inválido: "+name+".");}
}
