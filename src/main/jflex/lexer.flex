package lyc.compiler;

import java_cup.runtime.Symbol;
import lyc.compiler.ParserSym;
import lyc.compiler.model.*;
import static lyc.compiler.constants.Constants.*;

%%

%public
%class Lexer
%unicode
%cup
%line
%column
%throws CompilerException
%eofval{
  return symbol(ParserSym.EOF);
%eofval}

%{
  int commentLevel = 0;
%}

%x COMMENT

%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
%}


/* === Expresiones regulares === */
LineTerminator = \r|\n|\r\n
Whitespace     = [ \t\f]
Letter = [a-zA-Z]
Digit = [0-9]


Identifier     = {Letter}({Letter}|{Digit})*
IntegerConstant   = {Digit}+
FloatConstant = {Digit}+\.{Digit}*|\.{Digit}+
StringConstant  = \"({Letter}|{Digit}|{Whitespace})*\"

%%

/* === Palabras reservadas === */
"Int"            { return symbol(ParserSym.INT); }
"Boolean"        { return symbol(ParserSym.BOOLEAN); }
"Float"          { return symbol(ParserSym.FLOAT); }
"String"         { return symbol(ParserSym.STRING); }
"init"           { return symbol(ParserSym.INIT); }
"while"          { return symbol(ParserSym.WHILE); }
"if"             { return symbol(ParserSym.IF); }
"else"           { return symbol(ParserSym.ELSE); }
"read"           { return symbol(ParserSym.READ); }
"write"          { return symbol(ParserSym.WRITE); }
"AND"            { return symbol(ParserSym.AND); }
"OR"             { return symbol(ParserSym.OR); }
"NOT"            { return symbol(ParserSym.NOT); }
"long"           { return symbol(ParserSym.LONG); }
"IN"             { return symbol(ParserSym.IN); }
"do"             { return symbol(ParserSym.DO); }
"endwhile"       { return symbol(ParserSym.ENDWHILE); }

/* === Operadores y símbolos === */
"="   { return symbol(ParserSym.ASSIG); }
"+"   { return symbol(ParserSym.PLUS); }
"-"   { return symbol(ParserSym.SUB); }
"*"   { return symbol(ParserSym.MULT); }
"/"   { return symbol(ParserSym.DIV); }
">"   { return symbol(ParserSym.GREATER_THAN); }
"<"   { return symbol(ParserSym.LESS_THAN); }
"<="  { return symbol(ParserSym.LESS_THAN_EQUAL); }
">="  { return symbol(ParserSym.GREATER_THAN_EQUAL); }
"=="  { return symbol(ParserSym.DOBLE_EQUAL); }
"!="  { return symbol(ParserSym.NOT_EQUAL); }
"("   { return symbol(ParserSym.OPEN_BRACKET); }
")"   { return symbol(ParserSym.CLOSE_BRACKET); }
","   { return symbol(ParserSym.COMMA); }
":"   { return symbol(ParserSym.COLON); }
";"   { return symbol(ParserSym.SEMICOLON); }
"{"   { return symbol(ParserSym.OPEN_BRACE); }
"}"   { return symbol(ParserSym.CLOSE_BRACE); }
"["   { return symbol(ParserSym.OPEN_SQUARE_BRACKET); }
"]"   { return symbol(ParserSym.CLOSE_SQUARE_BRACKET); }

/* === Constantes numericas === */
{IntegerConstant} {
    try {
            int value = Integer.parseInt(yytext());
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) { //Valido enteros de hasta 16 bits
                throw new InvalidIntegerException("Constante fuera de rango: " + yytext());
            }
            return symbol(ParserSym.INTEGER_CONSTANT, value);
        } catch (NumberFormatException ex) {
            throw new InvalidNumericException("Constante inválida: " + yytext());
        }
}

{FloatConstant} {
    try {
        double value = Double.parseDouble(yytext());
        if (value > Float.MAX_VALUE || value < -Float.MAX_VALUE) { //VaLido Floats de hasta 32 bits
            throw new InvalidNumericException("Constante flotante fuera de rango: " + yytext());
        }
        return symbol(ParserSym.FLOAT_CONSTANT, (float) value);
    } catch (NumberFormatException ex) {
        throw new InvalidNumericException("Constante flotante inválida: " + yytext());
    }
}

/* === Constantes string === */
{StringConstant} {
    if (yytext().length() - 2 > MAX_LENGTH)
        throw new InvalidLengthException(yytext());
    return symbol(ParserSym.STRING_CONSTANT, yytext());
}

/* === Identificadores === */
{Identifier} {
    if (yytext().length() > MAX_LENGTH) {
        throw new InvalidLengthException("Identificador demasiado largo: " + yytext());
    }
    return symbol(ParserSym.IDENTIFIER, yytext());
}

/* === Whitespace y comentarios === */
{Whitespace}       { /* ignorar */ }
{LineTerminator}   { /* ignorar */ }

"#+" {
    commentLevel = 1;
    yybegin(COMMENT);
}

<COMMENT>{
    "#+" {
        if(commentLevel == 1) {
            commentLevel++;
        } else {
            throw new RuntimeException("Anidamiento de comentarios no permitido");
        }
    }

    "+#" {
        commentLevel--;
        if(commentLevel == 0) {
            yybegin(YYINITIAL);
        }
    }

    [^] { /* cualquier otro caracter dentro del comentario */ }

    <<EOF>> {
        throw new RuntimeException("Comentario no cerrado antes del EOF");
    }
}


/* error fallback */
[^]                              { throw new UnknownCharacterException(yytext()); }
