package lyc.compiler;

import lyc.compiler.factories.LexerFactory;
import lyc.compiler.model.CompilerException;
import lyc.compiler.model.InvalidIntegerException;
import lyc.compiler.model.InvalidLengthException;
import lyc.compiler.model.UnknownCharacterException;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static lyc.compiler.constants.Constants.MAX_LENGTH;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class LexerTest {

    private Lexer lexer;


    // =========================================================================
    // 1. PALABRAS RESERVADAS
    // =========================================================================

    @Test
    public void reservedWords() throws Exception {
        scan("Int Boolean Float String init while if else read write AND OR NOT long IN do endwhile");
        assertThat(nextToken()).isEqualTo(ParserSym.INT);
        assertThat(nextToken()).isEqualTo(ParserSym.BOOLEAN);
        assertThat(nextToken()).isEqualTo(ParserSym.FLOAT);
        assertThat(nextToken()).isEqualTo(ParserSym.STRING);
        assertThat(nextToken()).isEqualTo(ParserSym.INIT);
        assertThat(nextToken()).isEqualTo(ParserSym.WHILE);
        assertThat(nextToken()).isEqualTo(ParserSym.IF);
        assertThat(nextToken()).isEqualTo(ParserSym.ELSE);
        assertThat(nextToken()).isEqualTo(ParserSym.READ);
        assertThat(nextToken()).isEqualTo(ParserSym.WRITE);
        assertThat(nextToken()).isEqualTo(ParserSym.AND);
        assertThat(nextToken()).isEqualTo(ParserSym.OR);
        assertThat(nextToken()).isEqualTo(ParserSym.NOT);
        assertThat(nextToken()).isEqualTo(ParserSym.LONG);
        assertThat(nextToken()).isEqualTo(ParserSym.IN);
        assertThat(nextToken()).isEqualTo(ParserSym.DO);
        assertThat(nextToken()).isEqualTo(ParserSym.ENDWHILE);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }


    @Test
    public void keywordCaseSensitiveIntLowercase() throws Exception {
        scan("INT");
        assertThat(nextToken()).isNotEqualTo(ParserSym.INT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void keywordCaseSensitiveAndLowercase() throws Exception {
        scan("and");
        assertThat(nextToken()).isNotEqualTo(ParserSym.AND);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }


    // =========================================================================
    // 2. IDENTIFICADORES
    // =========================================================================

    @Test
    public void simpleIdentifier() throws Exception {
        scan("myVar");
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void identifierWithDigits() throws Exception {
        scan("var1");
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void identifierMaxLength() throws Exception {
        String id = "a".repeat(MAX_LENGTH);
        scan(id);
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void invalidIdLength() {
        assertThrows(InvalidLengthException.class, () -> {
            scan(getRandomString());
            nextToken();
        });
    }

    /** Un identificador no puede empezar con dígito */
    @Test
    public void identifierStartingWithDigitIsNotIdentifier() throws Exception {
        scan("1abc");
        // El lexer debe tokenizar 1 como INTEGER_CONSTANT y abc como IDENTIFIER
        assertThat(nextToken()).isEqualTo(ParserSym.INTEGER_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }


    // =========================================================================
    // 3. CONSTANTES ENTERAS
    // =========================================================================

    @Test
    public void integerConstantZero() throws Exception {
        scan("0");
        assertThat(nextToken()).isEqualTo(ParserSym.INTEGER_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void integerConstantPositive() throws Exception {
        scan("42");
        assertThat(nextToken()).isEqualTo(ParserSym.INTEGER_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    /** Valor máximo permitido (Short.MAX_VALUE = 32767) */
    @Test
    public void integerConstantMaxValue() throws Exception {
        scan("32767");
        assertThat(nextToken()).isEqualTo(ParserSym.INTEGER_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void invalidPositiveIntegerConstantValue() {
        assertThrows(InvalidIntegerException.class, () -> {
            scan("999999999");
            nextToken();
        });
    }


    // =========================================================================
    // 4. CONSTANTES FLOTANTES
    // =========================================================================

    @Test
    public void floatConstantSimple() throws Exception {
        scan("3.14");
        assertThat(nextToken()).isEqualTo(ParserSym.FLOAT_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void floatConstantNoIntegerPart() throws Exception {
        scan(".5");
        assertThat(nextToken()).isEqualTo(ParserSym.FLOAT_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void floatConstantNoDecimalPart() throws Exception {
        scan("5.");
        assertThat(nextToken()).isEqualTo(ParserSym.FLOAT_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void floatConstantZero() throws Exception {
        scan("0.0");
        assertThat(nextToken()).isEqualTo(ParserSym.FLOAT_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }


    // =========================================================================
    // 5. CONSTANTES STRING
    // =========================================================================

    @Test
    public void stringConstantSimple() throws Exception {
        scan("\"hola\"");
        assertThat(nextToken()).isEqualTo(ParserSym.STRING_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void stringConstantWithSpaces() throws Exception {
        scan("\"hola mundo\"");
        assertThat(nextToken()).isEqualTo(ParserSym.STRING_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void stringConstantWithDigits() throws Exception {
        scan("\"abc123\"");
        assertThat(nextToken()).isEqualTo(ParserSym.STRING_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void stringConstantEmpty() throws Exception {
        scan("\"\"");
        assertThat(nextToken()).isEqualTo(ParserSym.STRING_CONSTANT);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void invalidStringConstantLength() {
        assertThrows(InvalidLengthException.class, () -> {
            scan("\"%s\"".formatted(getRandomString()));
            nextToken();
        });
    }


    // =========================================================================
    // 6. OPERADORES ARITMÉTICOS
    // =========================================================================

    @Test
    public void arithmeticOperators() throws Exception {
        scan("+ - * /");
        assertThat(nextToken()).isEqualTo(ParserSym.PLUS);
        assertThat(nextToken()).isEqualTo(ParserSym.SUB);
        assertThat(nextToken()).isEqualTo(ParserSym.MULT);
        assertThat(nextToken()).isEqualTo(ParserSym.DIV);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void assignmentOperator() throws Exception {
        scan("=");
        assertThat(nextToken()).isEqualTo(ParserSym.ASSIG);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }


    // =========================================================================
    // 7. OPERADORES RELACIONALES
    // =========================================================================

    @Test
    public void relationalOperators() throws Exception {
        scan("> < <= >= == !=");
        assertThat(nextToken()).isEqualTo(ParserSym.GREATER_THAN);
        assertThat(nextToken()).isEqualTo(ParserSym.LESS_THAN);
        assertThat(nextToken()).isEqualTo(ParserSym.LESS_THAN_EQUAL);
        assertThat(nextToken()).isEqualTo(ParserSym.GREATER_THAN_EQUAL);
        assertThat(nextToken()).isEqualTo(ParserSym.DOBLE_EQUAL);
        assertThat(nextToken()).isEqualTo(ParserSym.NOT_EQUAL);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    // =========================================================================
    // 8. SÍMBOLOS DE PUNTUACIÓN Y DELIMITADORES
    // =========================================================================

    @Test
    public void brackets() throws Exception {
        scan("( )");
        assertThat(nextToken()).isEqualTo(ParserSym.OPEN_BRACKET);
        assertThat(nextToken()).isEqualTo(ParserSym.CLOSE_BRACKET);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void braces() throws Exception {
        scan("{ }");
        assertThat(nextToken()).isEqualTo(ParserSym.OPEN_BRACE);
        assertThat(nextToken()).isEqualTo(ParserSym.CLOSE_BRACE);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void squareBrackets() throws Exception {
        scan("[ ]");
        assertThat(nextToken()).isEqualTo(ParserSym.OPEN_SQUARE_BRACKET);
        assertThat(nextToken()).isEqualTo(ParserSym.CLOSE_SQUARE_BRACKET);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    @Test
    public void punctuationTokens() throws Exception {
        scan(", ; :");
        assertThat(nextToken()).isEqualTo(ParserSym.COMMA);
        assertThat(nextToken()).isEqualTo(ParserSym.SEMICOLON);
        assertThat(nextToken()).isEqualTo(ParserSym.COLON);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }


    // =========================================================================
    // 9. COMENTARIOS
    // =========================================================================

    /** Comentario simple: debe ignorarse completamente */
    @Test
    public void simpleComment() throws Exception {
        scan("#+ This is a comment +#");
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    /** Código antes y después de un comentario */
    @Test
    public void commentBetweenTokens() throws Exception {
        scan("a #+ ignored +# b");
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    /** Comentario no cerrado antes del EOF -> RuntimeException */
    @Test
    public void unclosedComment() {
        assertThrows(RuntimeException.class, () -> {
            scan("#+ este comentario no cierra");
            nextToken();
        });
    }

    /** Doble anidamiento (más de un nivel) no está permitido -> RuntimeException */
    @Test
    public void doubleNestedCommentNotAllowed() {
        assertThrows(RuntimeException.class, () -> {
            scan("#+ #+ #+ triple +# +# +#");
            nextToken();
        });
    }


    // =========================================================================
    // 10. WHITESPACE Y SALTOS DE LÍNEA
    // =========================================================================

    /** Espacios entre tokens deben ignorarse */
    @Test
    public void spacesAreIgnored() throws Exception {
        scan("a   b");
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    /** Saltos de línea deben ignorarse */
    @Test
    public void newlinesAreIgnored() throws Exception {
        scan("a\nb\r\nc");
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }

    /** Tabulaciones deben ignorarse */
    @Test
    public void tabsAreIgnored() throws Exception {
        scan("a\tb");
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.IDENTIFIER);
        assertThat(nextToken()).isEqualTo(ParserSym.EOF);
    }


    // =========================================================================
    // 11. CARACTERES INVÁLIDOS
    // =========================================================================

    @Test
    public void unknownCharacter() {
        assertThrows(UnknownCharacterException.class, () -> {
            scan("#");
            nextToken();
        });
    }

    @Test
    public void unknownCharacterAt() {
        assertThrows(UnknownCharacterException.class, () -> {
            scan("@");
            nextToken();
        });
    }

    @Test
    public void unknownCharacterDollar() {
        assertThrows(UnknownCharacterException.class, () -> {
            scan("$");
            nextToken();
        });
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    @AfterEach
    public void resetLexer() {
        lexer = null;
    }

    private void scan(String input) {
        lexer = LexerFactory.create(input);
    }

    private int nextToken() throws IOException, CompilerException {
        return lexer.next_token().sym;
    }

    private static String getRandomString() {
        return new RandomStringGenerator.Builder()
                .filteredBy(CharacterPredicates.LETTERS)
                .withinRange('a', 'z')
                .build().generate(MAX_LENGTH * 2);
    }
}