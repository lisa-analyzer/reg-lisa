lexer grammar RegLexer;

@ lexer :: header
{
	package it.unipr.reg.antlr;
}

SEQ: ';';
NON_DET_CHOICE: '(+)';
COND: '?';
SHKIP: 'skip';
NUM: [0-9]+;
ID: [a-zA-Z0-9]+;
ASSIGN: ':=';
PLUS: '+';
MINUS: '-';
TIMES: '*';
EQ: '=';
LEQ: '<=';
LE: '<';
TRUE: 'tt';
FALSE: 'ff';
AND: '&';
NOT: '!';

WS
   : [ \n\t\r] -> skip
   ;