parser grammar RegParser;

@ header
{
    package it.unipr.reg.antlr;
}

options { tokenVocab = RegLexer; }

// main rule, at least one expression
program
   : expr EOF
   ;

// expression can be a sequence of expressions
expr
   : e (SEQ e)* # seq
   ;

// singular expression
e
   : LPAR expr RPAR # e_par
   | LPAR e NON_DET_CHOICE e RPAR # e_ndc
   | NOOP # noop
   | ID ASSIGN a # assign
   | LPAR b COND ((SEQ e)+)? RPAR # cond
   | LPAR b COND (SEQ e)+ RPAR TIMES # kleene
   ;

// arithmetic expression
a
   : LPAR a RPAR # a_par
   | ID # id
   | NUM # num
   | a TIMES a # times
   | a op = (PLUS | MINUS) a # plus_minus
   ;

// boolean expression
b
   : LPAR b RPAR # b_par
   | TRUE # true
   | FALSE # false
   | a op = (EQ | LEQ | LE) a # eq_leq_le
   | b AND b # and
   | NOT b # not
   ;

