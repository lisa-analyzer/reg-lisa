parser grammar RegParser;

@ header
{
    package it.unipr.reg.antlr;
}

options { tokenVocab = RegLexer; }

program
    : e (op=(SEQ | NON_DET_CHOICE) e)* EOF
    ;

e
    : LPAR e RPAR #e_par
    | NOOP #noop
    | ID ASSIGN a #assign
    | b COND #cond
    | e op=(SEQ | NON_DET_CHOICE) e #op
    | e TIMES #kleene
    ;

a
    : LPAR a RPAR #a_par
    | ID    #id
    | NUM  #num
    | a op=(PLUS | MINUS | TIMES) a #plus_minus_times
    ;

b
    : LPAR b RPAR #b_par
    | TRUE #true
    | FALSE #false
    | a op=(EQ | LEQ | LE) a #eq_leq_le
    | b AND b #and
    | NOT b #not
    ;
