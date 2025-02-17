parser grammar RegParser;

@ header
{
	package it.unipr.reg.antlr;
}

options { tokenVocab = RegLexer; }

program
    : e (SEQ e | NON_DET_CHOICE e)*
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
