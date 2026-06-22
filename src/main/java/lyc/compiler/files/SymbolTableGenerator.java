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
            // Evitamos redeclaraciones: emitimos error si ya existe
            if (findEntryByName(symbolName) != null) {
                System.err.println("Semantic error: variable already declared -> " + nameStr);
                System.exit(1);
            }
            entries.add(new SymbolEntry(symbolName, type, "-", nameStr.length()));
        }
    }

    public void addConstant(Object name, Object value) {
        String symbolName = "_" + name.toString();
        String valStr = value.toString();
        entries.add(new SymbolEntry(symbolName, "-", valStr, valStr.length()));
    }

    public void addConstant(Object name, Object value, String type, Integer length) {
        String symbolName = "_" + name.toString();
        String valStr = value.toString();
        // Longitud solo para strings
        Integer len = type.equals("CTE_STRING") ? length : null;
        entries.add(new SymbolEntry(symbolName, type, valStr, len));
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

    // Consulta si un nombre (sin o con guion bajo) está declarado
    public boolean isDeclared(String name) {
        if (name == null) return false;
        SymbolEntry e = findEntryByName(name.startsWith("_") ? name : "_" + name);
        return e != null;
    }

    // Obtiene el tipo almacenado para un símbolo dado. Acepta "a" o "_a".
    public String getTypeOfName(String name) {
        if (name == null) return null;
        SymbolEntry e = findEntryByName(name.startsWith("_") ? name : "_" + name);
        return e == null ? null : e.type;
    }

    private SymbolEntry findEntryByName(String symbolName) {
        for (SymbolEntry e : entries) {
            if (e.name.equals(symbolName)) return e;
        }
        return null;
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
