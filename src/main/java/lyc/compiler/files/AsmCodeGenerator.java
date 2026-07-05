package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Genera el código Assembler (final.asm) a partir de la lista de tercetos.
 *
 * Enfoque (según la teórica "Generación de código ASM - Coprocesador Matemático"):
 *  - Se recorre el código intermedio (tercetos) desde el primero al último.
 *  - Toda la aritmética se resuelve con el coprocesador matemático (FPU): fld/fadd/fsub/fmul/fdiv.
 *  - Los números se tratan como reales de 32 bits (dd), igual que en el ejemplo provisto (ejemplo.asm).
 *  - Cada terceto de operación deja su resultado en una variable auxiliar @auxN.
 *  - Las comparaciones usan fcomp + fstsw + sahf, y los saltos condicionales usan las
 *    instrucciones sin signo (jb/jbe/ja/jae/je/jne) según la tabla de la cátedra.
 *
 * Prefijos de etiquetas en .DATA (para evitar colisiones con variables del usuario, que
 * siempre empiezan con "_" + letra):
 *  - _nombre     : variable del usuario
 *  - @kN         : constante numérica
 *  - @sN         : constante string
 *  - @auxN       : resultado temporal del terceto N
 */
public class AsmCodeGenerator implements FileGenerator {

    private final SymbolTableGenerator symbolTable;
    private final List<Terceto> tercetos;

    // Mapea el nombre crudo de una constante en la tabla de símbolos (_5, _3.14, _"Hola")
    // a una etiqueta ASM válida (@k0, @k1, @s0). Los nombres crudos tienen puntos/comillas,
    // que no son válidos como etiquetas en TASM.
    private final Map<String, String> constLabel = new LinkedHashMap<>();
    // Datos de cada constante string: etiqueta -> contenido (sin comillas)
    private final Map<String, String> stringConst = new LinkedHashMap<>();
    // Constantes numéricas: etiqueta -> literal float (ej "5.0")
    private final Map<String, String> numericConst = new LinkedHashMap<>();

    private static final String INDENT = "    ";

    public AsmCodeGenerator(SymbolTableGenerator symbolTable) {
        this.symbolTable = symbolTable;
        this.tercetos = IntermediateCodeGenerator.getListaTercetos();
    }

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        fileWriter.write(build());
    }

    private String build() {
        prepararConstantes();

        // Auxiliares necesarias (una por cada terceto que es operación aritmética)
        Set<Integer> auxes = new TreeSet<>();
        for (Terceto t : tercetos) {
            if (esOperacionAritmetica(t)) {
                auxes.add(t.getNroTerceto());
            }
        }

        // Etiquetas destino de saltos (terceto índice -> ET_indice)
        Set<Integer> targets = recolectarDestinosDeSalto();

        String code = generarCodigo(targets);
        String data = generarData(auxes);

        StringBuilder asm = new StringBuilder();
        asm.append("include macros2.asm\n");
        asm.append("include number.asm\n\n");
        asm.append(".MODEL LARGE\n");
        asm.append(".386\n");
        asm.append(".387\n");
        asm.append(".STACK 200h\n\n");
        asm.append("MAXTEXTSIZE equ 50\n\n");
        asm.append(data);
        asm.append("\n.CODE\n");
        asm.append("START:\n");
        asm.append(INDENT).append("mov AX,@DATA\n");
        asm.append(INDENT).append("mov DS,AX\n");
        asm.append(INDENT).append("mov ES,AX\n");
        asm.append(code);
        // Pausa final para que la consola no se cierre inmediatamente en DOSBox
        asm.append(INDENT).append("mov dx,OFFSET @NEWLINE\n");
        asm.append(INDENT).append("mov ah,09h\n");
        asm.append(INDENT).append("int 21h\n");
        asm.append(INDENT).append("mov dx,OFFSET @MSGFIN\n");
        asm.append(INDENT).append("mov ah,09h\n");
        asm.append(INDENT).append("int 21h\n");
        asm.append(INDENT).append("mov ah,01h\n");
        asm.append(INDENT).append("int 21h\n");
        asm.append(INDENT).append("mov ax,4C00h\n");
        asm.append(INDENT).append("int 21h\n");
        asm.append("END START\n");
        return asm.toString();
    }

    // ---------------------------------------------------------------
    //  Preparación de constantes: asigna etiquetas ASM válidas
    // ---------------------------------------------------------------
    private void prepararConstantes() {
        int k = 0, s = 0;
        for (SymbolTableGenerator.SymbolEntry e : symbolTable.getEntries()) {
            if (!esConstante(e)) continue;
            if ("CTE_STRING".equals(e.type)) {
                String label = "@s" + (s++);
                constLabel.put(e.name, label);
                stringConst.put(label, contenidoString(e.value));
            } else {
                String label = "@k" + (k++);
                constLabel.put(e.name, label);
                numericConst.put(label, aFloatLiteral(e.value));
            }
        }
    }

    private boolean esConstante(SymbolTableGenerator.SymbolEntry e) {
        return e.type != null && e.type.startsWith("CTE_");
    }

    // ---------------------------------------------------------------
    //  Sección .DATA
    // ---------------------------------------------------------------
    private String generarData(Set<Integer> auxes) {
        StringBuilder d = new StringBuilder();
        d.append(".DATA\n");

        // Variables del usuario
        for (SymbolTableGenerator.SymbolEntry e : symbolTable.getEntries()) {
            if (esConstante(e)) continue;
            if ("String".equals(e.type)) {
                d.append(INDENT).append(String.format("%-14s db MAXTEXTSIZE dup (?),'$'\n", e.name));
            } else {
                // Int / Float / Boolean -> real de 32 bits (coprocesador)
                d.append(INDENT).append(String.format("%-14s dd ?\n", e.name));
            }
        }

        // Constantes numéricas
        for (Map.Entry<String, String> c : numericConst.entrySet()) {
            d.append(INDENT).append(String.format("%-14s dd %s\n", c.getKey(), c.getValue()));
        }
        // Constantes string
        for (Map.Entry<String, String> c : stringConst.entrySet()) {
            d.append(INDENT).append(String.format("%-14s db \"%s\",'$'\n", c.getKey(), c.getValue()));
        }
        // Auxiliares de tercetos
        for (Integer i : auxes) {
            d.append(INDENT).append(String.format("%-14s dd ?\n", "@aux" + i));
        }

        // Helpers fijos
        d.append(INDENT).append("@NEWLINE       db 0DH,0AH,'$'\n");
        d.append(INDENT).append("@MSGFIN        db 0DH,0AH,\"Fin. Presione una tecla...\",'$'\n");
        return d.toString();
    }

    // ---------------------------------------------------------------
    //  Sección .CODE: recorre los tercetos y traduce cada uno
    // ---------------------------------------------------------------
    private String generarCodigo(Set<Integer> targets) {
        StringBuilder c = new StringBuilder();
        int n = tercetos.size();

        for (Terceto t : tercetos) {
            int idx = t.getNroTerceto();
            if (targets.contains(idx)) {
                c.append("ET_").append(idx).append(":\n");
            }
            traducirTerceto(t, c);
        }
        // Etiqueta final si algún salto apunta más allá del último terceto
        if (targets.contains(n + 1)) {
            c.append("ET_").append(n + 1).append(":\n");
        }
        return c.toString();
    }

    private void traducirTerceto(Terceto t, StringBuilder c) {
        String op = t.getOperando();
        String o1 = t.getOperador1();
        String o2 = t.getOperador2();

        // Tercetos hoja (carga de operando): no generan código, se resuelven al referenciarlos
        if (esVacio(o1) && esVacio(o2)) {
            return;
        }

        switch (op) {
            case "=":
                traducirAsignacion(t, c);
                break;
            case "+": case "-": case "*": case "/":
                traducirAritmetica(t, c);
                break;
            case "CMP":
                c.append(INDENT).append("fld ").append(mem(entero(o1))).append("\n");
                c.append(INDENT).append("fcomp ").append(mem(entero(o2))).append("\n");
                c.append(INDENT).append("fstsw ax\n");
                c.append(INDENT).append("sahf\n");
                break;
            case "BLT": c.append(salto("jb", o1)); break;
            case "BLE": c.append(salto("jbe", o1)); break;
            case "BGT": c.append(salto("ja", o1)); break;
            case "BGE": c.append(salto("jae", o1)); break;
            case "BEQ": c.append(salto("je", o1)); break;
            case "BNE": c.append(salto("jne", o1)); break;
            case "BI":  c.append(salto("jmp", o1)); break;
            case "WRITE":
                traducirWrite(entero(o1), c);
                break;
            case "READ":
                traducirRead(o1, c);
                break;
            default:
                c.append(INDENT).append("; TODO terceto no soportado: ")
                 .append(op).append(" ").append(o1).append(" ").append(o2).append("\n");
        }
    }

    private void traducirAsignacion(Terceto t, StringBuilder c) {
        int src = entero(t.getOperador1());
        String dest = t.getOperador2();          // nombre de variable destino
        if (esStringOperando(src)) {
            // Copia de string (constante) hacia el buffer de la variable
            String srcLabel = memString(src);
            String content = stringConst.get(srcLabel);
            int count = (content == null ? 0 : content.length()) + 1; // +1 por el '$'
            c.append(INDENT).append("lea si, ").append(srcLabel).append("\n");
            c.append(INDENT).append("lea di, _").append(dest).append("\n");
            c.append(INDENT).append("mov cx, ").append(count).append("\n");
            c.append(INDENT).append("cld\n");
            c.append(INDENT).append("rep movsb\n");
        } else {
            c.append(INDENT).append("fld ").append(mem(src)).append("\n");
            c.append(INDENT).append("fstp _").append(dest).append("\n");
        }
    }

    private void traducirAritmetica(Terceto t, StringBuilder c) {
        int idx = t.getNroTerceto();
        String op = t.getOperando();
        int a = entero(t.getOperador1());
        c.append(INDENT).append("fld ").append(mem(a)).append("\n");
        if (esVacio(t.getOperador2())) {
            // Menos unario
            c.append(INDENT).append("fchs\n");
        } else {
            int b = entero(t.getOperador2());
            String instr;
            switch (op) {
                case "+": instr = "fadd";  break;
                case "-": instr = "fsub";  break;
                case "*": instr = "fmul";  break;
                default:  instr = "fdiv";  break;
            }
            c.append(INDENT).append(instr).append(" ").append(mem(b)).append("\n");
        }
        c.append(INDENT).append("fstp @aux").append(idx).append("\n");
    }

    private void traducirWrite(int idx, StringBuilder c) {
        if (esStringOperando(idx)) {
            c.append(INDENT).append("displayString ").append(memString(idx)).append("\n");
        } else {
            c.append(INDENT).append("DisplayFloat ").append(mem(idx)).append(", 2\n");
        }
        c.append(INDENT).append("newLine 1\n");
    }

    private void traducirRead(String var, StringBuilder c) {
        String tipo = symbolTable.getTypeOfName(var);
        if ("String".equals(tipo)) {
            c.append(INDENT).append("getString _").append(var).append("\n");
        } else {
            c.append(INDENT).append("GetFloat _").append(var).append("\n");
        }
    }

    private String salto(String instr, String destTerceto) {
        return INDENT + instr + " ET_" + destTerceto + "\n";
    }

    // ---------------------------------------------------------------
    //  Resolución de operandos
    // ---------------------------------------------------------------
    /** Nombre de memoria (dd) del resultado del terceto idx, para usar con la FPU. */
    private String mem(int idx) {
        Terceto t = tercetos.get(idx - 1);
        String op = t.getOperando();
        if (esOperacionAritmetica(t)) {
            return "@aux" + idx;
        }
        if (op.startsWith("_")) {
            // constante numérica
            String label = constLabel.get(op);
            return label != null ? label : op;
        }
        // variable
        return "_" + op;
    }

    /** Etiqueta ASM de un operando string (constante o variable). */
    private String memString(int idx) {
        Terceto t = tercetos.get(idx - 1);
        String op = t.getOperando();
        if (op.startsWith("_")) {
            String label = constLabel.get(op); // @sN
            return label != null ? label : op;
        }
        return "_" + op; // variable string
    }

    private boolean esStringOperando(int idx) {
        Terceto t = tercetos.get(idx - 1);
        String op = t.getOperando();
        if (esOperacionAritmetica(t)) return false;
        if (op.startsWith("_\"")) return true;           // constante string
        String tipo = symbolTable.getTypeOfName(op);
        return "String".equals(tipo) || "CTE_STRING".equals(tipo);
    }

    // ---------------------------------------------------------------
    //  Utilidades
    // ---------------------------------------------------------------
    private Set<Integer> recolectarDestinosDeSalto() {
        Set<Integer> targets = new TreeSet<>();
        for (Terceto t : tercetos) {
            String op = t.getOperando();
            if (esSalto(op)) {
                Integer dest = enteroOrNull(t.getOperador1());
                if (dest != null) targets.add(dest);
            }
        }
        return targets;
    }

    private boolean esSalto(String op) {
        switch (op) {
            case "BLT": case "BLE": case "BGT": case "BGE":
            case "BEQ": case "BNE": case "BI":
                return true;
            default:
                return false;
        }
    }

    private boolean esOperacionAritmetica(Terceto t) {
        switch (t.getOperando()) {
            case "+": case "-": case "*": case "/":
                return true;
            default:
                return false;
        }
    }

    private String contenidoString(String valorConComillas) {
        // valor viene como  "Hola mundo"  (con comillas) -> Hola mundo
        if (valorConComillas != null && valorConComillas.length() >= 2
                && valorConComillas.startsWith("\"") && valorConComillas.endsWith("\"")) {
            return valorConComillas.substring(1, valorConComillas.length() - 1);
        }
        return valorConComillas;
    }

    private String aFloatLiteral(String value) {
        // TASM interpreta "5" como entero; para la FPU necesitamos "5.0"
        if (value == null) return "0.0";
        return value.contains(".") ? value : value + ".0";
    }

    private boolean esVacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    private int entero(String s) {
        return Integer.parseInt(s.trim());
    }

    private Integer enteroOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
