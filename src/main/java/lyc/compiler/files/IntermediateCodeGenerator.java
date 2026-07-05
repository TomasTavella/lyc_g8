package lyc.compiler.files;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class IntermediateCodeGenerator implements FileGenerator {

   // Lista secuencial de todos los tercetos generados
    private static final List<Terceto> listaTercetos = new ArrayList<>();
    
    // Pila exclusiva para guardar los índices de los tercetos incompletos (saltos de IF/WHILE)
    private static final Stack<Integer> pilaSaltos = new Stack<>();

    // Flag que indica que el próximo salto de simple_condition debe generarse invertido (usada por NOT)
    private static boolean invertir = false;

    // Índice del salto del primer operando de un OR, entre la detección del OR y el cierre de la condición
    private static int orPrimerSalto = -1;

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        // --- INICIO DEL DEBUG ---
        System.out.println("\n==========================================");
        System.out.println("DEBUG GENERADOR DE TERCETOS:");
        System.out.println("Cantidad de tercetos en memoria: " + listaTercetos.size());
        System.out.println("==========================================\n");
        // --- FIN DEL DEBUG ---

        for (Terceto t : listaTercetos) {
            fileWriter.write(t.toString() + "\n");
        }
    }

    /**
     * Crea un terceto, lo agrega a la lista y retorna su número.
     * Esto reemplaza por completo el uso de la clase Punteros.
     */
    public static int addTerceto(String operando, String op1, String op2) {
        Terceto t = new Terceto(operando, op1, op2);
        listaTercetos.add(t);
        return t.getNroTerceto();
    }

    /**
     * Permite obtener un terceto específico para modificarlo después (Backpatching)
     * Como el nroTerceto arranca en 1, el índice en la lista es nroTerceto - 1.
     */
    public static Terceto getTerceto(int nroTerceto) {
        return listaTercetos.get(nroTerceto - 1);
    }

    public static Stack<Integer> getPilaSaltos() {
        return pilaSaltos;
    }

    // --- Manejo de la flag de inversión (usada por NOT) ---
    public static boolean isInvertir() {
        return invertir;
    }

    public static void setInvertir(boolean valor) {
        invertir = valor;
    }

    public static int getOrPrimerSalto() {
        return orPrimerSalto;
    }

    public static void setOrPrimerSalto(int idx) {
        orPrimerSalto = idx;
    }

    /**
     * Devuelve el mnemónico de salto opuesto (negado <-> directo).
     * Se usa en el primer operando de un OR para saltar por la condición real.
     */
    public static String invertirMnemonico(String mnemonico) {
        switch (mnemonico) {
            case "BLT": return "BGE";
            case "BGE": return "BLT";
            case "BLE": return "BGT";
            case "BGT": return "BLE";
            case "BEQ": return "BNE";
            case "BNE": return "BEQ";
            default: throw new IllegalStateException("Mnemonico de salto inesperado: " + mnemonico);
        }
    }
    
    public static List<Terceto> getListaTercetos() {
        return listaTercetos;
    }

    /**
     * Copia los tercetos del rango [inicio, fin] al final de la lista, relocando
     * sus referencias internas. Se usa para desenrollar el cuerpo del ciclo especial:
     * cada copia del cuerpo debe apuntar a sus propios tercetos, no a los del original.
     */
    public static void copiarBloqueRelocado(int inicio, int fin) {
        // Desplazamiento entre la copia y el original (se calcula una sola vez).
        int offset = (listaTercetos.size() + 1) - inicio;
        for (int i = inicio; i <= fin; i++) {
            Terceto original = getTerceto(i);
            addTerceto(original.getOperando(),
                       relocar(original.getOperador1(), inicio, offset),
                       relocar(original.getOperador2(), inicio, offset));
        }
    }

    /**
     * Si el campo es una referencia a un terceto del bloque copiado (entero puro >= inicio),
     * le suma el desplazamiento. Nombres de variable, constantes (_x) o vacíos quedan igual.
     */
    private static String relocar(String campo, int inicio, int offset) {
        if (esEnteroPuro(campo)) {
            int ref = Integer.parseInt(campo);
            if (ref >= inicio) {
                return String.valueOf(ref + offset);
            }
        }
        return campo;
    }

    private static boolean esEnteroPuro(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // Método de limpieza por si se ejecutan varios tests en la misma instancia de JVM
    public static void reset() {
        listaTercetos.clear();
        pilaSaltos.clear();
        invertir = false;
        orPrimerSalto = -1;
        Terceto.resetContador();
    }


}
