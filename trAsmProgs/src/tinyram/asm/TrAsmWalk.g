tree grammar TrAsmWalk ;
options {
    output=AST;
    tokenVocab=TrAssembler;
    ASTLabelType=CommonTree;
    backtrack=true;
    memoize=true;
}

@members {
    AsmState asmState; // this needs to be provided for us elsewhere

    // implementation is provided in subclass
    protected void asmOpR(CommonTree mnemonic, CommonTree r1, CommonTree r2, CommonTree r3) { ; }
    protected void asmOpI(CommonTree mnemonic, CommonTree r1, CommonTree r2, BigInteger im) { ; }

    protected void asmOpR(CommonTree mnemonic, CommonTree r1, CommonTree r2) { ; }
    protected void asmOpI(CommonTree mnemonic, CommonTree r1, BigInteger im) { ; }

    protected void asmOpR(CommonTree mnemonic, CommonTree r1) { ; }
    protected void asmOpI(CommonTree mnemonic, BigInteger im) { ; }

    protected void tapeOp(CommonTree mnemonic, BigInteger iter, BigInteger tapeNum, BigInteger value) { ; }
    protected void tapeOp(CommonTree mnemonic, BigInteger tapeNum, BigInteger value) { ; }
}

@header {
    package tinyram.asm;
    import java.math.BigInteger;
}

asmFile : ( directive | labelDecl | instruction ) + ;

// handle the different instruction formats
instruction   
// 3-operand instructions
            : ^( mm=MNEMONIC r1=REG r2=REG r3=REG   ) { asmOpR($mm,$r1,$r2,$r3); }
            | ^( mm=MNEMONIC r1=REG r2=REG ex=opExp ) { asmOpI($mm,$r1,$r2,$ex.value); }
// 2-operand instructions
            | ^( mm=MNEMONIC r1=REG r2=REG   ) { asmOpR($mm,$r1,$r2); }
            | ^( mm=MNEMONIC r1=REG ex=opExp ) { asmOpI($mm,$r1,$ex.value); }
// 1-operand instructions
            | ^( mm=MNEMONIC r1=REG   ) { asmOpR($mm,$r1); }
            | ^( mm=MNEMONIC ex=opExp ) { asmOpI($mm,$ex.value); }
// tape-related instructions
            | ^( mm=MNEMONIC e1=opExp e2=opExp e3=opExp ) { tapeOp($mm,$e1.value,$e2.value,$e3.value); }
            | ^( mm=MNEMONIC e1=opExp e2=opExp )          { tapeOp($mm,$e1.value,$e2.value); }
            ;
/*
    NOTE: the above is non-LL* because of the recursion inherent in having opExps.
    We can fix this either by left factoring, e.g.
            | ^( mm=MNEMONIC exl+=opExp ( exl+=opExp )* ) { figureOutWhatToDo($mm,$exl); }
    or by turning on backtracking. In this case, let's just be lazy and backtrack.
*/

opExp returns [BigInteger value]
        : atom { $value = $atom.value; }
        | ^( OP e1=opExp e2=opExp ) { $value = asmState.expOp($OP.text,$e1.value,$e2.value); }
        | ^( UNOP e1=opExp ) { $value = asmState.expOp($UNOP.text,$e1.value); }
        ;

atom returns [BigInteger value]
        : LABEL { $value = asmState.getLabel($LABEL); }
        | IMMED { $value = asmState.getImmed($IMMED); }
        ;

directive 
scope { List<BigInteger> dirArgs; }
@init { $directive::dirArgs = new ArrayList<BigInteger>(); }
        : ^( '$' MNEMONIC (opExp {$directive::dirArgs.add($opExp.value);})* )
           { asmState.asmDirective($MNEMONIC,$directive::dirArgs); } ;

// these are used during the prewalk; nothing to do here
labelDecl : ^( LABDEF LABEL (.)? ) ;

