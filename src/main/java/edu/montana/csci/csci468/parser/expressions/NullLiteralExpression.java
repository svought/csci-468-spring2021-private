package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;

public class NullLiteralExpression extends Expression {

    @Override
    public CatscriptType getType() {
        return CatscriptType.NULL;
    }

    @Override
    public void validate(SymbolTable symbolTable) {}

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) { return null; }

    @Override
    public void transpile(StringBuilder javascript) { javascript.append(getType());}

    @Override
    public void compile(ByteCodeGenerator code) { }


}
