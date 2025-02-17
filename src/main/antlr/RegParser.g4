parser grammar RegParser;

@ header
{
	package it.unipr.reg.antlr;
}

options { tokenVocab = RegLexer; }

program
    : e
    | program SEQ program
    | program NON_DET_CHOICE program
    //| program*
    ;

e
    : SHKIP
    | ID ASSIGN a
    | b COND
    ;

a
    : ID
    | NUM
    | a PLUS a
    | a MINUS a
    | a TIMES a
    ;

b
    : TRUE
    | FALSE
    | a EQ a
    | a LEQ a
    | a LE a
    | b AND b
    | NOT b
    ;
