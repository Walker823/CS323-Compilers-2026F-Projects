package impl.symbol;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Scope {
    protected final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private final Scope parent;
    public Scope(Scope parent) { this.parent = parent; }
    public Scope getParent() { return parent; }
    public Collection<Symbol> getLocalSymbols() {
        return Collections.unmodifiableCollection(symbols.values());
    }
    public boolean define(Symbol symbol) {
        return symbols.putIfAbsent(symbol.getName(), symbol) == null;
    }
    public Symbol resolveLocal(String name) {
        return symbols.get(name);
    }
    public Symbol resolve(String name) {
        for (Scope scope = this; scope != null; scope = scope.getParent()) {
            Symbol symbol = scope.resolveLocal(name);
            if (symbol != null) return symbol;
        }
        return null;
    }
}
