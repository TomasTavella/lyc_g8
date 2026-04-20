package lyc.compiler;

import java_cup.runtime.Symbol;
import lyc.compiler.factories.ParserFactory;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;
import static lyc.compiler.Constants.EXAMPLES_ROOT_DIRECTORY;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class ParserTest {

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void compilationSuccessful(String input) throws Exception {
        assertThat(scan(input).sym).isEqualTo(ParserSym.EOF);
    }

    private void compilationFails(String input) {
        assertThrows(Exception.class, () -> scan(input));
    }

    private Symbol scan(String input) throws Exception {
        return ParserFactory.create(input).parse();
    }


    // =========================================================================
    // 1. PROGRAMA MÍNIMO
    // =========================================================================

    /** init con bloque vacío y sin statements es válido (statements -> vacío) */
    @Test
    public void emptyProgram() throws Exception {
        compilationSuccessful("init { a : Int }");
    }

    /** Falta el bloque init -> error */
    @Test
    public void missingInitBlock() {
        compilationFails("a = 1");
    }

    /** init sin llaves -> error */
    @Test
    public void initWithoutBraces() {
        compilationFails("init a : Int");
    }

    /** Programa completamente vacío -> error (falta init) */
    @Test
    public void emptyInput() {
        compilationFails("");
    }


    // =========================================================================
    // 2. DECLARACIONES (var_decl / id_list / type)
    // =========================================================================

    @Test
    public void singleIntDeclaration() throws Exception {
        compilationSuccessful("init { x : Int }");
    }

    @Test
    public void singleFloatDeclaration() throws Exception {
        compilationSuccessful("init { x : Float }");
    }

    @Test
    public void singleBooleanDeclaration() throws Exception {
        compilationSuccessful("init { x : Boolean }");
    }

    @Test
    public void singleStringDeclaration() throws Exception {
        compilationSuccessful("init { x : String }");
    }

    /** Múltiples variables del mismo tipo separadas por coma */
    @Test
    public void multipleVarsSameType() throws Exception {
        compilationSuccessful("init { a, b, c : Int }");
    }

    /** Múltiples declaraciones de distintos tipos encadenadas */
    @Test
    public void multipleDeclarationsDifferentTypes() throws Exception {
        compilationSuccessful("init { a : Int b : Float c : Boolean d : String }");
    }

    /** Mezcla: varios ids en un tipo, luego otro tipo */
    @Test
    public void mixedDeclarations() throws Exception {
        compilationSuccessful("init { a, b : Int x, y : Float flag : Boolean }");
    }

    /** Declaración sin tipo -> error */
    @Test
    public void declarationWithoutType() {
        compilationFails("init { a }");
    }

    /** Declaración sin identificador -> error */
    @Test
    public void declarationWithoutIdentifier() {
        compilationFails("init { : Int }");
    }

    /** Tipo inválido -> error */
    @Test
    public void invalidType() {
        compilationFails("init { a : Double }");
    }


    // =========================================================================
    // 3. ASSIGNMENT
    // =========================================================================

    @Test
    public void assignmentWithIntegerConstant() throws Exception {
        compilationSuccessful("init { a : Int } a = 1");
    }

    @Test
    public void assignmentWithFloatConstant() throws Exception {
        compilationSuccessful("init { x : Float } x = 3.14");
    }

    @Test
    public void assignmentWithStringConstant() throws Exception {
        compilationSuccessful("init { s : String } s = \"hola\"");
    }

    @Test
    public void assignmentWithIdentifier() throws Exception {
        compilationSuccessful("init { a, b : Int } a = b");
    }

    @Test
    public void multipleAssignments() throws Exception {
        compilationSuccessful("init { a, b, c : Int } a = 1 b = 2 c = 3");
    }

    /** Asignación sin el lado derecho -> error */
    @Test
    public void assignmentMissingRhs() {
        compilationFails("init { a : Int } a =");
    }

    /** Asignación sin operador -> error */
    @Test
    public void assignmentMissingOperator() {
        compilationFails("init { a : Int } a 1");
    }


    // =========================================================================
    // 4. EXPRESIONES ARITMÉTICAS
    // =========================================================================

    @Test
    public void additionExpression() throws Exception {
        compilationSuccessful("init { a : Int } a = 1 + 2");
    }

    @Test
    public void subtractionExpression() throws Exception {
        compilationSuccessful("init { a : Int } a = 5 - 3");
    }

    @Test
    public void multiplicationExpression() throws Exception {
        compilationSuccessful("init { a : Int } a = 2 * 3");
    }

    @Test
    public void divisionExpression() throws Exception {
        compilationSuccessful("init { a : Int } a = 10 / 2");
    }

    @Test
    public void chainedArithmeticExpression() throws Exception {
        compilationSuccessful("init { a : Int } a = 1 + 2 * 3 - 4 / 2");
    }

    @Test
    public void parenthesizedExpression() throws Exception {
        compilationSuccessful("init { a : Int } a = (1 + 2) * 3");
    }

    @Test
    public void deeplyNestedParentheses() throws Exception {
        compilationSuccessful("init { a : Int } a = ((1 + 2) * (3 - 1))");
    }

    /** Unario negativo */
    @Test
    public void unaryMinusExpression() throws Exception {
        compilationSuccessful("init { a : Int } a = -5");
    }

    /** Unario positivo */
    @Test
    public void unaryPlusExpression() throws Exception {
        compilationSuccessful("init { a : Int } a = +5");
    }

    @Test
    public void unaryMinusWithIdentifier() throws Exception {
        compilationSuccessful("init { a, b : Int } a = -b");
    }

    /** Expresión incompleta -> error */
    @Test
    public void incompleteExpression() {
        compilationFails("init { a : Int } a = 1 +");
    }

    /** Paréntesis sin cerrar -> error */
    @Test
    public void unclosedParenthesis() {
        compilationFails("init { a : Int } a = (1 + 2");
    }

    /** Función long() con lista de expresiones */
    @Test
    public void longFunctionSingleArg() throws Exception {
        compilationSuccessful("init { a : Int } a = long([1])");
    }

    @Test
    public void longFunctionMultipleArgs() throws Exception {
        compilationSuccessful("init { a : Int } a = long([1, 2, 3])");
    }

    @Test
    public void longFunctionWithExpressions() throws Exception {
        compilationSuccessful("init { a, b : Int } a = long([b + 1, 2 * 3])");
    }


    // =========================================================================
    // 5. WHILE
    // =========================================================================

    @Test
    public void whileWithSimpleCondition() throws Exception {
        compilationSuccessful("init { a : Int } while (a > 0) { a = a - 1 }");
    }

    @Test
    public void whileWithEmptyBody() throws Exception {
        compilationSuccessful("init { a : Int } while (a > 0) { }");
    }

    @Test
    public void whileWithMultipleStatements() throws Exception {
        compilationSuccessful("init { a, b : Int } while (a > 0) { a = a - 1 b = b + 1 }");
    }

    @Test
    public void nestedWhile() throws Exception {
        compilationSuccessful("init { a, b : Int } while (a > 0) { while (b > 0) { b = b - 1 } a = a - 1 }");
    }

    /** While sin condición -> error */
    @Test
    public void whileWithoutCondition() {
        compilationFails("init { a : Int } while () { a = 1 }");
    }

    /** While sin llaves -> error */
    @Test
    public void whileWithoutBraces() {
        compilationFails("init { a : Int } while (a > 0) a = a - 1");
    }


    // =========================================================================
    // 6. IF / IF-ELSE
    // =========================================================================

    @Test
    public void ifWithoutElse() throws Exception {
        compilationSuccessful("init { a : Int } if (a > 0) { a = 1 }");
    }

    @Test
    public void ifWithElse() throws Exception {
        compilationSuccessful("init { a : Int } if (a > 0) { a = 1 } else { a = 0 }");
    }

    @Test
    public void ifWithEmptyBodies() throws Exception {
        compilationSuccessful("init { a : Int } if (a > 0) { } else { }");
    }

    @Test
    public void nestedIf() throws Exception {
        compilationSuccessful("init { a, b : Int } if (a > 0) { if (b > 0) { a = 1 } }");
    }

    @Test
    public void ifInsideWhile() throws Exception {
        compilationSuccessful("init { a : Int } while (a > 0) { if (a > 5) { a = a - 2 } else { a = a - 1 } }");
    }

    /** if sin condición -> error */
    @Test
    public void ifWithoutCondition() {
        compilationFails("init { a : Int } if { a = 1 }");
    }


    // =========================================================================
    // 7. CONDICIONES
    // =========================================================================

    @Test
    public void conditionGreaterThan() throws Exception {
        compilationSuccessful("init { a : Int } if (a > 0) { }");
    }

    @Test
    public void conditionLessThan() throws Exception {
        compilationSuccessful("init { a : Int } if (a < 10) { }");
    }

    @Test
    public void conditionLessThanOrEqual() throws Exception {
        compilationSuccessful("init { a : Int } if (a <= 10) { }");
    }

    @Test
    public void conditionGreaterThanOrEqual() throws Exception {
        compilationSuccessful("init { a : Int } if (a >= 0) { }");
    }

    @Test
    public void conditionDoubleEqual() throws Exception {
        compilationSuccessful("init { a : Int } if (a == 5) { }");
    }

    @Test
    public void conditionNotEqual() throws Exception {
        compilationSuccessful("init { a : Int } if (a != 0) { }");
    }

    @Test
    public void conditionWithAnd() throws Exception {
        compilationSuccessful("init { a, b : Int } if (a > 0 AND b > 0) { }");
    }

    @Test
    public void conditionWithOr() throws Exception {
        compilationSuccessful("init { a, b : Int } if (a > 0 OR b > 0) { }");
    }

    @Test
    public void conditionWithNot() throws Exception {
        compilationSuccessful("init { a : Int } if (NOT a > 0) { }");
    }


    // =========================================================================
    // 8. READ / WRITE
    // =========================================================================

    @Test
    public void readStatement() throws Exception {
        compilationSuccessful("init { a : Int } read(a)");
    }

    @Test
    public void writeWithIdentifier() throws Exception {
        compilationSuccessful("init { a : Int } write(a)");
    }

    @Test
    public void writeWithExpression() throws Exception {
        compilationSuccessful("init { a : Int } write(a + 1)");
    }

    @Test
    public void writeWithStringConstant() throws Exception {
        compilationSuccessful("init { a : Int } write(\"resultado\")");
    }

    @Test
    public void readAndWriteCombined() throws Exception {
        compilationSuccessful("init { a : Int } read(a) write(a)");
    }

    /** read sin argumentos -> error */
    @Test
    public void readWithoutArgument() {
        compilationFails("init { a : Int } read()");
    }

    /** write sin argumentos -> error */
    @Test
    public void writeWithoutArgument() {
        compilationFails("init { a : Int } write()");
    }


    // =========================================================================
    // 9. SPECIAL CYCLE (while ... IN [...] do ... endwhile)
    // =========================================================================

    @Test
    public void specialCycleSimple() throws Exception {
        compilationSuccessful("init { a : Int } while a IN [1, 2, 3] do a = a + 1 endwhile");
    }

    @Test
    public void specialCycleEmptyBody() throws Exception {
        compilationSuccessful("init { a : Int } while a IN [1, 2, 3] do endwhile");
    }

    @Test
    public void specialCycleWithExpressionList() throws Exception {
        compilationSuccessful("init { a, b : Int } while a IN [b, b + 1, b * 2] do a = a + 1 endwhile");
    }

    /** Falta endwhile -> error */
    @Test
    public void specialCycleMissingEndwhile() {
        compilationFails("init { a : Int } while a IN [1, 2, 3] do a = 1");
    }

    /** Falta do -> error */
    @Test
    public void specialCycleMissingDo() {
        compilationFails("init { a : Int } while a IN [1, 2, 3] a = 1 endwhile");
    }


    // =========================================================================
    // 10. PROGRAMA COMPLETO (integración)
    // =========================================================================

    @Test
    public void fullProgramWithAllStatementTypes() throws Exception {
        compilationSuccessful(
                "init { a, b : Int result : Float msg : String } " +
                        "read(a) " +
                        "read(b) " +
                        "if (a > b) { " +
                        "  result = a - b " +
                        "} else { " +
                        "  result = b - a " +
                        "} " +
                        "write(result) " +
                        "write(\"fin\")"
        );
    }

    @Test
    public void fullProgramWithWhileAndAssignments() throws Exception {
        compilationSuccessful(
                "init { i, suma : Int } " +
                        "i = 1 " +
                        "suma = 0 " +
                        "while (i <= 10) { " +
                        "  suma = suma + i " +
                        "  i = i + 1 " +
                        "} " +
                        "write(suma)"
        );
    }

    @Test
    public void fullProgramWithSpecialCycle() throws Exception {
        compilationSuccessful(
                "init { x : Int } " +
                        "while x IN [1, 2, 3, 4, 5] do " +
                        "  write(x) " +
                        "endwhile"
        );
    }

    @Test
    public void fullProgramWithNestedStructures() throws Exception {
        compilationSuccessful(
                "init { a, b, max : Int } " +
                        "read(a) " +
                        "read(b) " +
                        "max = a " +
                        "if (b > max) { max = b } " +
                        "write(max)"
        );
    }



}
