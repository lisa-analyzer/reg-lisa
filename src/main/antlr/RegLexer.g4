lexer grammar RegLexer;

@ lexer :: header
{
    package it.unipr.reg.antlr;
}
SEQ
   : ';'
   ;

NON_DET_CHOICE
   : '|'
   ;

COND
   : '?'
   ;

NOOP
   : 'skip'
   ;

TRUE
   : 'true'
   ;

FALSE
   : 'false'
   ;

NUM
   : [0-9]+
   ;

ID
   : [a-zA-Z0-9]+
   ;

ASSIGN
   : ':='
   ;

PLUS
   : '+'
   ;

MINUS
   : '-'
   ;

TIMES
   : '*'
   ;

EQ
   : '='
   ;

LEQ
   : '<='
   ;

LE
   : '<'
   ;

AND
   : '&'
   ;

NOT
   : '!'
   ;

LPAR
   : '('
   ;

RPAR
   : ')'
   ;

WS
   : [ \n\t\r] -> skip
   ;

