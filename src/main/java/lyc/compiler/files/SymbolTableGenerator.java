package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SymbolTableGenerator implements FileGenerator {

    private final Set<SymbolEntry> entries = new LinkedHashSet<>();

    public void addVariables(List<Object> names, String type) {
        for (Object name : names) {
            String nameStr = name.toString();
            String symbolName = "_" + nameStr;
            // Chequeo semántico: no permitir redeclaración
            if (findEntryByName(symbolName) != null) {
                throw new RuntimeException("Error semantico: variable ya declarada -> " + nameStr);
            }
            // Los identificadores NO guardan valor (columna Valor en "-")
            entries.add(new SymbolEntry(symbolName, type, "-", nameStr.length()));
        }
    }

    // Indica si un identificador (con o sin guion bajo) está declarado
    public boolean isDeclared(String name) {
        if (name == null) return false;
        String key = name.startsWith("_") ? name : "_" + name;
        return findEntryByName(key) != null;
    }

    // Devuelve el tipo almacenado para un símbolo dado. Acepta "a" o "_a".
    public String getTypeOfName(String name) {
        if (name == null) return null;
        String key = name.startsWith("_") ? name : "_" + name;
        SymbolEntry e = findEntryByName(key);
        return e == null ? null : e.type;
    }

    private SymbolEntry findEntryByName(String symbolName) {
        for (SymbolEntry e : entries) {
            if (e.name.equals(symbolName)) return e;
        }
        return null;
    }

    public void addConstant(Object name, Object value) {
        String symbolName = "_" + name.toString();
        String valStr = value.toString();
        // El tipo de la constante se infiere del tipo Java del valor.
        // La longitud se aplica SOLO a las constantes string.
        String tipo;
        Integer length;
        if (value instanceof String) {
            tipo = "CTE_STRING";
            length = valStr.length();
        } else if (value instanceof Float || value instanceof Double) {
            tipo = "CTE_FLOAT";
            length = null;
        } else {
            tipo = "CTE_INTEGER";
            length = null;
        }
        entries.add(new SymbolEntry(symbolName, tipo, valStr, length));
    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        fileWriter.write(String.format("%-15s %-10s %-15s %-10s%n", "Nombre", "TipoDato", "Valor", "Longitud"));
        for (SymbolEntry entry : entries) {
            String lengthStr = entry.length == null ? "-" : entry.length.toString();
            fileWriter.write(String.format("%-15s %-10s %-15s %-10s%n",
                    entry.name, entry.type, entry.value, lengthStr));
        }
    }

    private static class SymbolEntry {
        String name;
        String type;
        String value;
        Integer length;

        SymbolEntry(String name, String type, String value, Integer length) {
            this.name = name;
            this.type = type;
            this.value = value;
            this.length = length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SymbolEntry)) return false;
            SymbolEntry that = (SymbolEntry) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
