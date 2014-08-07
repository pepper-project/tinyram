grammar TrAssembler;

options { output=AST; }

tokens { LABDEF; }

@parser::header { package tinyram.asm; }
@lexer::header { package tinyram.asm; }

asmFile : ( directive | labelDecl | instruction | EOL! )+ EOF! ;

directive : '$' MNEMONIC opExp* -> ^( '$' MNEMONIC opExp* ) ;

labelDecl : LABEL ':'       -> ^( LABDEF LABEL )
          | LABEL '=' opExp -> ^( LABDEF LABEL opExp )
          ;

instruction : MNEMONIC ( operand ( ',' operand )* )
                                    -> ^( MNEMONIC operand+ ) ;

operand : REG
        | opExp
        ;

opExp : atom
      | '['! opExp OP^ opExp ']'!
      | '['! UNOP^ opExp ']'!
      ;

atom : LABEL
     | IMMED
     ;

MNEMONIC : (LETTER)+ ;

REG : Rl (DIGIT)+ ;

// 0{b,B} is implicitly allowed by HEXDIGIT+
IMMED : ('-')? (('0' Xl) | ('0' Ol))? (HEXDIGIT)+ ;

LABEL : '_' (LABELCHAR)+ ;

COMMENT : ( ';' | '#' ) (COMMENTCHAR)* { skip(); } ;

WS : WSfrag+ { skip(); } ;

EOL : NEWLINE | EOF ;

OP : '+' | '-' | '/' | '*' | '%' | '&' | '|' | '^' | ('>' '>') | ('<' '<') ;

UNOP : '!' | '~' | '>' ;

fragment COMMENTCHAR : ~('\r' | '\n') ;
fragment NEWLINE : ('\r'? '\n') ;
fragment WSfrag : (' ' | '\t') ;
fragment LETTER : 'a'..'z' | 'A'..'Z' ;
fragment DIGIT : '0'..'9' ;
fragment HEXDIGIT : DIGIT | 'A'..'F' | 'a'..'f' ;
fragment LABELCHAR : '_' | LETTER | DIGIT;
fragment Rl : 'R' | 'r' ;
fragment Xl : 'X' | 'x' ;
fragment Ol : 'O' | 'o' ;
