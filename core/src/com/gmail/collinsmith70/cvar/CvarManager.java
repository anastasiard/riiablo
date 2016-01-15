package com.gmail.collinsmith70.cvar;

import com.gmail.collinsmith70.util.StringSerializer;
import com.gmail.collinsmith70.util.Validator;
import com.gmail.collinsmith70.util.serializer.BooleanStringSerializer;
import com.gmail.collinsmith70.util.serializer.DoubleStringSerializer;
import com.gmail.collinsmith70.util.serializer.IntegerStringSerializer;
import com.gmail.collinsmith70.util.serializer.ObjectStringSerializer;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CvarManager implements CvarChangeListener {

private static final Map<Class<?>, StringSerializer<?>> DEFAULT_SERIALIZERS;
static {
    DEFAULT_SERIALIZERS = new HashMap<Class<?>, StringSerializer<?>>();
    DEFAULT_SERIALIZERS.put(String.class, ObjectStringSerializer.INSTANCE);
    DEFAULT_SERIALIZERS.put(Boolean.class, BooleanStringSerializer.INSTANCE);
    DEFAULT_SERIALIZERS.put(Integer.class, IntegerStringSerializer.INSTANCE);
    DEFAULT_SERIALIZERS.put(Double.class, DoubleStringSerializer.INSTANCE);
}

private final Trie<String, Cvar<?>> CVARS;
private final Map<Class<?>, StringSerializer<?>> SERIALIZERS;

public CvarManager() {
    this.CVARS = new PatriciaTrie<Cvar<?>>();
    this.SERIALIZERS = new ConcurrentHashMap<Class<?>, StringSerializer<?>>(DEFAULT_SERIALIZERS);
}

@Override
public Object beforeChanged(Cvar cvar, Object from, Object to) {
    return to;
}

@Override
public void afterChanged(Cvar cvar, Object from, Object to) {

}

public <T> Cvar<T> create(String alias, Class<T> type, T defaultValue) {
    return create(alias, type, defaultValue, Validator.ACCEPT_ALL);
}

public <T> Cvar<T> create(String alias, Class<T> type, T defaultValue, Validator<T> validator) {
    Cvar<T> cvar = new Cvar<T>(alias, type, defaultValue, validator);
    load(cvar);
    return add(cvar);
}

public <T> Cvar<T> add(Cvar<T> cvar) {
    if (isManagingCvar(cvar)) {
        return cvar;
    } else if (containsAlias(cvar.getAlias())) {
        throw new DuplicateCvarException(cvar, String.format(
                "A Cvar with the alias %s is already registered. Cvar aliases must be unique!",
                cvar.getAlias()));
    }

    CVARS.put(cvar.getAlias().toLowerCase(), cvar);
    cvar.addCvarChangeListener(this);
    return cvar;
}

public <T> boolean remove(Cvar<T> cvar) {
    if (!isManagingCvar(cvar)) {
        return false;
    }

    return CVARS.remove(cvar.getAlias().toLowerCase()) == null;
}

public <T> void load(Cvar<T> cvar) {
    checkIfManaged(cvar);
}

public <T> void save(Cvar<T> cvar) {
    checkIfManaged(cvar);

}

public <T> void commit(Cvar<T> cvar) {
    checkIfManaged(cvar);
}

private <T> void checkIfManaged(Cvar<T> cvar) throws UnmanagedCvarException {
    if (isManagingCvar(cvar)) {
        return;
    }

    throw new UnmanagedCvarException(cvar, String.format(
            "Cvar %s is not managed by this %s",
            cvar.getAlias(),
            getClass().getSimpleName()));
}

public <T> boolean isManagingCvar(Cvar<T> cvar) {
    Cvar value = CVARS.get(cvar.getAlias().toLowerCase());
    return cvar.equals(value);
}

public boolean containsAlias(String alias) {
    return CVARS.containsKey(alias.toLowerCase());
}

public <T> StringSerializer<T> getSerializer(Class<T> type) {
    return (StringSerializer<T>)SERIALIZERS.get(type);
}

public <T> StringSerializer<T> getSerializer(Cvar<T> cvar) {
    return getSerializer(cvar.getType());
}

public <T> void putSerializer(Class<T> type, StringSerializer<T> serializer) {
    SERIALIZERS.put(type, serializer);
}

public static abstract class CvarException extends RuntimeException {

    public final Cvar CVAR;

    private CvarException() {
        this(null, null);
    }

    private CvarException(Cvar cvar) {
        this(cvar, null);
    }

    private CvarException(String message) {
        this(null, message);
    }

    private CvarException(Cvar cvar, String message) {
        super(message);
        this.CVAR = cvar;
    }

    private Cvar getCvar() {
        return CVAR;
    }

}

public static class DuplicateCvarException extends CvarException {

    private DuplicateCvarException() {
        this(null, null);
    }

    private DuplicateCvarException(Cvar cvar) {
        this(cvar, null);
    }

    private DuplicateCvarException(String message) {
        this(null, message);
    }

    private DuplicateCvarException(Cvar cvar, String message) {
        super(cvar, message);
    }

}

public static class UnmanagedCvarException extends CvarException {

    private UnmanagedCvarException() {
        this(null, null);
    }

    private UnmanagedCvarException(Cvar cvar) {
        this(cvar, null);
    }

    private UnmanagedCvarException(String message) {
        this(null, message);
    }

    private UnmanagedCvarException(Cvar cvar, String message) {
        super(cvar, message);
    }

}

}
