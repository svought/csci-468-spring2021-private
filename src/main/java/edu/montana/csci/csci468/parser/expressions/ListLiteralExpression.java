package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ListLiteralExpression extends Expression {
    List<Expression> values;
    private CatscriptType type;

    public ListLiteralExpression(List<Expression> values) {
        this.values = new LinkedList<>();
        for (Expression value : values) {
            this.values.add(addChild(value));
        }
    }

    public List<Expression> getValues() {
        return values;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        for (Expression value : values) {
            value.validate(symbolTable);
        }
        if (values.size() > 0) {
            CatscriptType rollingType = CatscriptType.NULL;

            for (Expression value : values) {
                if(!rollingType.isAssignableFrom(value.getType())){
                    if(rollingType == CatscriptType.NULL) rollingType = value.getType();
                    else rollingType = CatscriptType.OBJECT;
                }
            }
            type = CatscriptType.getListType(values.get(0).getType());
        } else {
            type = CatscriptType.getListType(CatscriptType.OBJECT);
        }
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        ArrayList<Object> list = new ArrayList();
        for (Expression value : values) {
            Object temp = value.evaluate(runtime);
            list.add(temp);
        }
        return list;
    }

    @Override
    public void transpile(StringBuilder javascript) { javascript.append(values); }

    @Override
    public void compile(ByteCodeGenerator code) { code.pushConstantOntoStack(values); }


}
