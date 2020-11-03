package edu.montana.csci.csci466.js;

import edu.montana.csci.csci466.parser.ParseTreeVisitor;
import edu.montana.csci.csci466.parser.expressions.*;
import edu.montana.csci.csci466.parser.statements.CatScriptProgram;
import edu.montana.csci.csci466.parser.statements.PrintStatement;
import edu.montana.csci.csci466.parser.statements.SyntaxErrorStatement;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.StringWriter;

public class JSTranspiler {

    public static String evalJS(String javascript) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
            ScriptContext context = engine.getContext();
            StringWriter writer = new StringWriter();
            context.setWriter(writer);
            engine.eval(javascript);
            return writer.toString();
        } catch (ScriptException e) {
            return e.getMessage();
        }
    }

    public static String transpile(CatScriptProgram program) {
        StringBuilder sb = new StringBuilder();
        if (program.getExpression() != null) {
            sb.append("print(");
            transpileExpression(sb, program.getExpression());
            sb.append(");\n");
        } else {
            // TODO implement statements
        }
        return sb.toString();
    }

    private static void transpileExpression(StringBuilder buffer, Expression expression) {
        if (expression instanceof AdditiveExpression) {
            AdditiveExpression additiveExpression = (AdditiveExpression) expression;
            transpileExpression(buffer, additiveExpression.getLeftHandSide());
            buffer.append(additiveExpression.isAdd() ? " + " : " - ");
            transpileExpression(buffer, additiveExpression.getRightHandSide());
        } else if (expression instanceof IntegerLiteralExpression) {
            IntegerLiteralExpression integerLiteral = (IntegerLiteralExpression) expression;
            buffer.append(integerLiteral.evaluate());
        } else {
            throw new UnsupportedOperationException("Don't know how to transpile : " + expression);
        }
    }
}
