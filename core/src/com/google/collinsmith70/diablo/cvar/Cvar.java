package com.google.collinsmith70.diablo.cvar;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.google.collinsmith70.diablo.cvar.listener.load.BooleanCvarLoadListener;
import com.google.collinsmith70.diablo.cvar.listener.load.ByteCvarLoadListener;
import com.google.collinsmith70.diablo.cvar.listener.load.DoubleCvarLoadListener;
import com.google.collinsmith70.diablo.cvar.listener.load.FloatCvarLoadListener;
import com.google.collinsmith70.diablo.cvar.listener.load.IntegerCvarLoadListener;
import com.google.collinsmith70.diablo.cvar.listener.load.LongCvarLoadListener;
import com.google.collinsmith70.diablo.cvar.listener.load.ShortCvarLoadListener;
import com.google.collinsmith70.diablo.cvar.listener.load.StringCvarLoadListener;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class Cvar<T> {

private static final String TAG = Cvar.class.getSimpleName();
private static final Preferences PREFERENCES = Gdx.app.getPreferences(Cvar.class.getName());
private static final Trie<String, Cvar<?>> CVARS = new PatriciaTrie<Cvar<?>>();

public static void saveAll() {
    Gdx.app.log(TAG, "Saving Cvars...");
    Cvar.PREFERENCES.flush();
}

public static SortedMap<String, Cvar<?>> search(String key) {
    return CVARS.prefixMap(key);
}

public static Cvar<?> get(String key) {
    return CVARS.get(key.toLowerCase());
}

public static Collection<Cvar<?>> getCvars() {
    return CVARS.values();
}

private static final Set<CvarChangeListener<Object>> GLOBAL_CHANGE_LISTENERES
        = new CopyOnWriteArraySet<CvarChangeListener<Object>>();

public static void addGlobalCvarChangeListener(CvarChangeListener<Object> l) {
    GLOBAL_CHANGE_LISTENERES.add(l);
}

public static boolean containsGlobalCvarChangeListener(CvarChangeListener<Object> l) {
    return GLOBAL_CHANGE_LISTENERES.contains(l);
}

public static boolean removeGlobalCvarChangeListener(CvarChangeListener<Object> l) {
    return GLOBAL_CHANGE_LISTENERES.remove(l);
}

private final String KEY;
private final Class<T> TYPE;
private final T DEFAULT_VALUE;
private final CvarLoadListener<T> LOAD_LISTENER;
private final Set<CvarChangeListener<T>> CHANGE_LISTENERS;

private T value;

public Cvar(String key, Class<T> type, T defaultValue) {
    this(key, type, defaultValue, null);
}

public Cvar(String key, Class<T> type, T defaultValue, CvarLoadListener<T> l) {
    this.KEY = key;
    if (getKey() == null) {
        throw new IllegalArgumentException("Key aliases cannot be null");
    } else if (getKey().isEmpty()) {
        throw new IllegalArgumentException("Key aliases cannot be empty");
    } else if (Cvar.CVARS.containsKey(getKey())) {
        throw new IllegalArgumentException(String.format(
                "Cvar %s already exists. Cvar keys must be unique!",
                getKey()));
    }

    this.DEFAULT_VALUE = defaultValue;
    this.TYPE = type != null ? type : (Class<T>)getDefaultValue().getClass();
    this.CHANGE_LISTENERS = new CopyOnWriteArraySet<CvarChangeListener<T>>();

    if (l == null) {
        if (getType().equals(String.class)) {
            this.LOAD_LISTENER = (CvarLoadListener<T>) StringCvarLoadListener.INSTANCE;
        } else if (getType().equals(Boolean.class)) {
            this.LOAD_LISTENER = (CvarLoadListener<T>) BooleanCvarLoadListener.INSTANCE;
        } else if (getType().equals(Float.class)) {
            this.LOAD_LISTENER = (CvarLoadListener<T>) FloatCvarLoadListener.INSTANCE;
        } else if (getType().equals(Double.class)) {
            this.LOAD_LISTENER = (CvarLoadListener<T>) DoubleCvarLoadListener.INSTANCE;
        } else if (getType().equals(Byte.class)) {
            this.LOAD_LISTENER = (CvarLoadListener<T>) ByteCvarLoadListener.INSTANCE;
        } else if (getType().equals(Short.class)) {
            this.LOAD_LISTENER = (CvarLoadListener<T>) ShortCvarLoadListener.INSTANCE;
        } else if (getType().equals(Integer.class)) {
            this.LOAD_LISTENER = (CvarLoadListener<T>) IntegerCvarLoadListener.INSTANCE;
        } else if (getType().equals(Long.class)) {
            this.LOAD_LISTENER = (CvarLoadListener<T>) LongCvarLoadListener.INSTANCE;
        } else {
            this.LOAD_LISTENER = null;
            this.value = getDefaultValue();
            Gdx.app.log(TAG, String.format(
                    "%s defaulted to %s [%s]",
                    getKey(),
                    getValue().toString(),
                    getType().getName()));
        }
    } else {
        this.LOAD_LISTENER = l;
    }

    if (LOAD_LISTENER != null) {
        String stringVal = LOAD_LISTENER.toString(getDefaultValue());
        this.value = LOAD_LISTENER.onCvarLoaded(
                PREFERENCES.getString(getKey(), stringVal));
        Gdx.app.log(TAG, String.format(
                "%s loaded as %s [%s]",
                getKey(),
                stringVal,
                getType().getName()));
    }

    Cvar.CVARS.put(getKey().toLowerCase(), this); // potentially unsafe (technically object is not constructed yet)
}

public void addCvarChangeListener(CvarChangeListener<T> l) {
    CHANGE_LISTENERS.add(l);
    l.onCvarChanged(this, null, this.value);
}

public boolean containsCvarChangeListener(CvarChangeListener<T> l) {
    return CHANGE_LISTENERS.contains(l);
}

public boolean removeCvarChangeListener(CvarChangeListener<T> l) {
    return CHANGE_LISTENERS.remove(l);
}

public String getKey() {
    return KEY;
}

public Class<T> getType() {
    return TYPE;
}

public T getValue() {
    return value;
}

private static <T> String getStringValue(Cvar<T> cvar, T value) {
    if (cvar.LOAD_LISTENER != null) {
        return cvar.LOAD_LISTENER.toString(value);
    }

    return value.toString();
}

public String getStringValue() {
    return getStringValue(this, this.getValue());
}

public T getDefaultValue() {
    return DEFAULT_VALUE;
}

public void setValue(T value) {
    if (this.value.equals(value)) {
        return;
    }

    T oldValue = this.value;
    this.value = value;
    if (LOAD_LISTENER != null) {
        Cvar.PREFERENCES.putString(getKey(), LOAD_LISTENER.toString(getValue()));
    }

    Gdx.app.log(TAG, String.format(
            "%s changed from %s to %s",
            getKey(),
            getStringValue(this, oldValue),
            getStringValue()));
    for (CvarChangeListener<T> l : CHANGE_LISTENERS) {
        l.onCvarChanged(this, oldValue, getValue());
    }

    for (CvarChangeListener<Object> l : GLOBAL_CHANGE_LISTENERES) {
        l.onCvarChanged((Cvar<Object>)this, oldValue, getValue());
    }
}

public void setValue(String value) {
    if (LOAD_LISTENER == null) {
        throw new IllegalStateException(String.format(
                "No CvarLoadListener configured for this Cvar " +
                "which can parse the given value \"%s\"", value));
    }

    setValue(LOAD_LISTENER.onCvarLoaded(value));
}


public void reset() {
    setValue(getDefaultValue());
}

public void load() {
    //...
}

@Override
public String toString() {
    return String.format("%s:%s=%s", TYPE.getName(), getKey(), getStringValue());
}

}
