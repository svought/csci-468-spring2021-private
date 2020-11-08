package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.AdditiveExpression;
import edu.montana.csci.csci468.parser.expressions.Expression;
import edu.montana.csci.csci468.parser.expressions.IntegerLiteralExpression;
import edu.montana.csci.csci468.parser.expressions.SyntaxErrorExpression;
import edu.montana.csci.csci468.parser.statements.CatScriptProgram;
import edu.montana.csci.csci468.parser.statements.PrintStatement;
import edu.montana.csci.csci468.parser.statements.Statement;
import edu.montana.csci.csci468.parser.statements.SyntaxErrorStatement;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    TokenList tokens;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        if (tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseAdditiveExpression();
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parsePrimaryExpression();
        if (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            return additiveExpression;
        } else {
            return expression;
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression();
            syntaxErrorExpression.setToken(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        if(tokens.getCurrentToken().getType().equals(type)){
            return tokens.consumeToken();
        } else {
            elt.addError("Unexpected Token", tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}