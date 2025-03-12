parser grammar RegParser;

@ header
{
    package it.unipr.reg.antlr;
}

options { tokenVocab = RegLexer; }

program
    : e (SEQ e)* EOF
    ;

e
    : LPAR e RPAR                                               #e_par
    | LPAR e NON_DET_CHOICE e RPAR                              #e_ndc
    | NOOP                                                      #noop
    // procediamo con l'assign e la add
    | ID ASSIGN a                                               #assign
    | b COND                                                    #cond
    | e SEQ e                                                   #seq
    // kleene lasciamola per ultima
    | e TIMES                                                   #kleene
    ;

a
    : LPAR a RPAR                                               #a_par
    | ID                                                        #id
    | NUM                                                       #num
    | a TIMES a                                                 #times
    | a op=(PLUS | MINUS) a                                     #plus_minus
    ;

b
    : LPAR b RPAR                                               #b_par
    | TRUE                                                      #true
    | FALSE                                                     #false
    | a op=(EQ | LEQ | LE) a                                    #eq_leq_le
    | b AND b                                                   #and
    | NOT b                                                     #not
    ;
