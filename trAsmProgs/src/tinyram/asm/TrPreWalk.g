tree grammar TrPreWalk ;
options {
    output=AST;
    tokenVocab=TrAssembler;
    ASTLabelType=CommonTree;
    backtrack=true;
    memoize=true;
}

@members {
    // state interface
    AsmState asmState = new AsmState();
}

@header {
    package tinyram.asm;
    import java.math.BigInteger;
}

asmFile : ( directive | labelDecl | instruction ) + ;

directive 
scope { List<BigInteger> dirArgs; }
@init { $directive::dirArgs = new ArrayList<BigInteger>(); }
        : ^( '$' MNEMONIC (opExp {$directive::dirArgs.add($opExp.value);})* )
           { asmState.asmDirective($MNEMONIC,$directive::dirArgs); } ;

labelDecl : ^( LABDEF LABEL ) { asmState.addLabel($LABEL.text); }
          | ^( LABDEF LABEL opExp ) { asmState.addLabel($LABEL.text,$opExp.value); }
          ;

instruction : ^( MNEMONIC REG (operand)* ) { asmState.incPC(); }
            | ^( MNEMONIC opExpNR )        { asmState.incPC(); }
            | ^( MNEMONIC opExpNR ( opExpNR )+ )
            ;

// NR - no retval
// for instructions, we do not attempt to resolve expressions until the 2nd pass

opExpNR : atomNR
        | ^( OP opExpNR opExpNR )
        | ^( UNOP opExpNR )
        ;

atomNR : LABEL | IMMED;

// resolve expressions for label declarations and directives
// these will generate a warning if we have forward references
opExp returns [BigInteger value]
       : atom { $value = $atom.value; }
       | ^( OP e1=opExp e2=opExp ) { $value = asmState.expOp($OP.text,$e1.value,$e2.value); }
       | ^( UNOP e1=opExp ) { $value = asmState.expOp($UNOP.text,$e1.value); }
       ;

atom returns [BigInteger value]
     : LABEL { $value = asmState.getLabel($LABEL); }
     | IMMED { $value = asmState.getImmed($IMMED); }
     ;

// operands aren't important in the prewalk; nothing to do here
operand : REG
        | opExpNR
        ;
