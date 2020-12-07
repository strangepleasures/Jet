lexer grammar JetLexer;

// Whitespace
WS                 : [\r\n\t ]+ -> skip ;

// Keywords
VAR                : 'var' ;
OUT                : 'out';
PRINT              : 'print';
MAP                : 'map';
REDUCE             : 'reduce';

// Literals
NUMBER             : ('0'|[1-9][0-9]*) ('.' [0-9]+)? (('e'|'E')('+'|'-')?[0-9]+)? ;
STRING             : '"' ~ ["\r\n]* '"' ;

// Operators
PLUS               : '+' ;
MINUS              : '-' ;
MULTIPLICATION     : '*' ;
DIVISION           : '/' ;
POWER              : '^' ;
ASSIGN             : '=' ;
LPAREN             : '(' ;
RPAREN             : ')' ;
LCURLY             : '{' ;
RCURLY             : '}' ;
COMMA              : ',' ;
ARROW              : '->' ;

// Identifiers
ID                 : [A-Za-z_][A-Za-z0-9_]* ;
