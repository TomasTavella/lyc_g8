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
            entries.add(new SymbolEntry(symbolName, type, nameStr, nameStr.length()));
        }
    }

    public void addConstant(Object name, Object value) {
        String symbolName = "_" + name.toString();
        String valStr = value.toString();
        entries.add(new SymbolEntry(symbolName, "-", valStr, valStr.length()));
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
