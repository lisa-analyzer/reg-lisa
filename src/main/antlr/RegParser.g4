parser grammar RegParser;

@ header
{
    package it.unipr.reg.antlr;
}

options { tokenVocab = RegLexer; }

program
    : e ((SEQ | NON_DET_CHOICE) e)* EOF
    ;

e
    : LPAR e RPAR
    | SHKIP
    | ID ASSIGN a
    | b COND
    | e (SEQ | NON_DET_CHOICE) e
    | e TIMES
    ;

a
    : LPAR a RPAR
    | ID
    | NUM
    | a PLUS a
    | a MINUS a
    | a TIMES a
    ;

b
    : LPAR b RPAR
    | TRUE
    | FALSE
    | a EQ a
    | a LEQ a
    | a LE a
    | b AND b
    | NOT b
    ;
