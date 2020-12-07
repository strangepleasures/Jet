parser grammar JetParser;

options { tokenVocab=JetLexer; }

program : statement+ EOF;

statement : VAR ID ASSIGN expression # varDeclarationStatement
          | OUT expression           # outStatement
          | PRINT STRING             # printStatement ;

lambda : arguments=argument+ ARROW expression ;
argument : ID ;

expression : left=expression operator=POWER right=expression                             # binaryOperation
           | left=expression operator=(MULTIPLICATION|DIVISION) right=expression         # binaryOperation
           | left=expression operator=(PLUS|MINUS) right=expression                      # binaryOperation
           | LPAREN expression RPAREN                                                    # parenExpression
           | LCURLY left=expression operator=COMMA right=expression RCURLY               # binaryOperation
           | ID                                                                          # varReference
           | (MINUS|PLUS)? NUMBER                                                        # numberLiteral
           | MAP LPAREN sequence=expression COMMA lambda RPAREN                          # mapExpression
           | REDUCE LPAREN sequence=expression COMMA unit=expression COMMA lambda RPAREN # reduceExpression ;
