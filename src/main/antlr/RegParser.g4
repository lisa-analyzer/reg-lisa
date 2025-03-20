parser grammar RegParser;

@ header
{
    package it.unipr.reg.antlr;
}

options { tokenVocab = RegLexer; }
program
   : expr EOF
   ;

expr
   : e (SEQ e)* # seq
   ;

e
   : LPAR expr RPAR # e_par
   | LPAR e NON_DET_CHOICE e RPAR # e_ndc
   | NOOP # noop
   | ID ASSIGN a # assign
   | LPAR b COND SEQ e (SEQ e)* RPAR # cond
   | LPAR b COND RPAR #assert
   | LPAR b COND SEQ e (SEQ e)* RPAR TIMES # kleene
   ;

a
   : LPAR a RPAR # a_par
   | ID # id
   | NUM # num
   | a TIMES a # times
   | a op = (PLUS | MINUS) a # plus_minus
   ;

b
   : LPAR b RPAR # b_par
   | TRUE # true
   | FALSE # false
   | a op = (EQ | LEQ | LE) a # eq_leq_le
   | b AND b # and
   | NOT b # not
   ;

