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

    @Override
    public void generate(FileWriter fileWriter) throws IOException {
        // --- INICIO DEL DEBUG ---
        System.out.println("\n==========================================");
        System.out.println("DEBUG GENERADOR DE TERCETOS:");
        System.out.println("Cantidad de tercetos en memoria: " + listaTercetos.size());
        System.out.println("==========================================\n");
        // --- FIN DEL DEBUG ---

        for (Terceto t : listaTercetos) {
            fileWriter.write("[" + t.getNroTerceto() + "] " + t.toString() + "\n");
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
    
    public static List<Terceto> getListaTercetos() {
        return listaTercetos;
    }

    // Método de limpieza por si se ejecutan varios tests en la misma instancia de JVM
    public static void reset() {
        listaTercetos.clear();
        pilaSaltos.clear();
        Terceto.resetContador();
    }


}
