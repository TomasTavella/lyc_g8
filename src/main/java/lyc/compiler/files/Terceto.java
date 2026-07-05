package lyc.compiler.files;

public class Terceto {
    private int nroTerceto;
    private String operando;
    private String operador1;
    private String operador2;

    // Variable estática para llevar el conteo automático de cada terceto instanciado
    private static int contador = 1; 

    public Terceto(String operando, String operador1, String operador2) {
        this.nroTerceto = contador++;
        this.operando = operando != null ? operando : "";
        this.operador1 = operador1 != null ? operador1 : "";
        this.operador2 = operador2 != null ? operador2 : "";
    }

    // Getters
    public int getNroTerceto() { return nroTerceto; }
    public String getOperando() { return operando; }
    public String getOperador1() { return operador1; }
    public String getOperador2() { return operador2; }

    // Setters (especialmente útiles para completar saltos más adelante)
    public void setOperando(String operando) { this.operando = operando; }
    public void setOperador1(String operador1) { this.operador1 = operador1; }
    public void setOperador2(String operador2) { this.operador2 = operador2; }

    // Para poder resetear el compilador si se procesan múltiples archivos
    public static void resetContador() {
        contador = 1;
    }

    @Override
    public String toString() {
        // Formato clásico y prolijo: [1] (+, a, b)
        return String.format("[%d] (%s, %s, %s)", nroTerceto, operando, operador1, operador2);
    }
}