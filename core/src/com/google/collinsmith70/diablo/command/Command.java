package com.google.collinsmith70.diablo.command;

import com.badlogic.gdx.Gdx;
import com.google.collinsmith70.diablo.Client;
import com.google.common.base.MoreObjects;

import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class Command {

private static final String TAG = Command.class.getSimpleName();

private static final Trie<String, Command> COMMANDS = new PatriciaTrie<Command>();

public static SortedMap<String, Command> search(String command) {
    return Command.COMMANDS.prefixMap(command);
}

public static Command get(String command) {
    return Command.COMMANDS.get(command);
}

public static Collection<Command> getCommands() {
    return Command.COMMANDS.values();
}

private final Set<String> ALIASES;
private final ParameterResolver[] RESOLVERS;
private final Action ACTION;

public Command(String alias, Action action, ParameterResolver... resolvers) {
    this.ALIASES = new CopyOnWriteArraySet<String>();
    this.ACTION = MoreObjects.firstNonNull(action, Action.EMPTY_ACTION);
    this.RESOLVERS = resolvers;

    addAlias(alias);
    Gdx.app.log(TAG, "Registered " + toString());
}

public Command addAlias(String alias) {
    if (alias == null) {
        throw new IllegalArgumentException("Command alias should not be null");
    } else if (alias.isEmpty()) {
        throw new IllegalArgumentException("Command alias should not be empty");
    }

    if (Command.COMMANDS.containsKey(alias)) {
        Command.COMMANDS.get(alias).ALIASES.remove(alias);
    }

    Command.COMMANDS.put(alias, this);
    ALIASES.add(alias);
    return this;
}

public Set<String> getAliases() {
    return UnmodifiableSet.unmodifiableSet(ALIASES);
}

public boolean isAlias(String alias) {
    return ALIASES.contains(alias);
}

public void execute(Client client, String[] args) {
    for (int i = 1; i <= getNumRequiredParameters(); i++) {
        getResolver(i-1).resolve(args[i]);
    }

    ACTION.onActionExecuted(client, args);
}

public int getNumRequiredParameters() {
    int requiredParameters = 0;
    for (ParameterResolver pr : getResolvers()) {
        if (!(pr instanceof OptionalParameterResolver)) {
            requiredParameters++;
        }
    }

    return requiredParameters;
}

public ParameterResolver getResolver(int i) {
    return getResolvers()[i];
}

private ParameterResolver[] getResolvers() {
    return RESOLVERS;
}

private String getParametersHint() {
    StringBuilder sb = new StringBuilder();
    for (ParameterResolver pr : getResolvers()) {
        if (pr instanceof OptionalParameterResolver) {
            sb.append(String.format("[%s] ", pr));
        } else {
            sb.append(String.format("<%s> ", pr));
        }
    }

    return sb.toString().trim();
}

public void load() {
    //...
}

@Override
public String toString() {
    if (getNumRequiredParameters() == 0) {
        return getAliases().iterator().next();
    }

    return String.format("%s %s", getAliases().iterator().next(), getParametersHint());
}

}
