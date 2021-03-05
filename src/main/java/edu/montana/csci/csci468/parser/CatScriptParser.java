package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.security.spec.ECParameterSpec;
import java.util.ArrayList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

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

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
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
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression expression = parseComparisonExpression();

        while (tokens.match(BANG_EQUAL) || tokens.match(EQUAL_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(expression.getEnd());

            expression = equalityExpression;
        }
            return expression;
    }

    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();

        if (tokens.match(GREATER) || tokens.match(GREATER_EQUAL) || tokens.match(LESS) || tokens.match(LESS_EQUAL)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(expression.getEnd());

            expression = comparisonExpression;
        }
            return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
            return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(SLASH, STAR)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(expression.getEnd());
            expression = factorExpression;
        }
            return expression;
    }

    private Expression parseUnaryExpression() {
        if(tokens.match(MINUS, NOT)){
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        }
        else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(STRING)) {
            String stringToken = tokens.consumeToken().getStringValue();
            StringLiteralExpression stringLiteralExpression = new StringLiteralExpression(stringToken);
            return stringLiteralExpression;
        } else if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(TRUE) || tokens.match(FALSE)) {
            Token booleanToken = tokens.consumeToken();
            BooleanLiteralExpression booleanLiteralExpression = new BooleanLiteralExpression(booleanToken.getType() == TRUE);
            booleanLiteralExpression.setStart(booleanToken);
            return booleanLiteralExpression;
        } else if (tokens.match(NULL)) {
            Expression nullLiteralExpression = new NullLiteralExpression();
            return nullLiteralExpression;
        } else if (tokens.match(LEFT_PAREN)) {
            Token start = tokens.consumeToken();
            Expression expr = parseExpression();
            Expression parenthesizedExpression = new ParenthesizedExpression(expr);
            return parenthesizedExpression;

        } else if (tokens.match(LEFT_BRACKET)) {
            tokens.consumeToken();
            List<Expression> exprs = new ArrayList<>();

            if (!tokens.match(RIGHT_BRACKET)) {
                Expression val = parseExpression();
                exprs.add(val);

                do {
                    tokens.consumeToken();
                    val = parseExpression();
                    exprs.add(val);
                } while (tokens.match(COMMA));
            }

            ListLiteralExpression listLiteralExpression = new ListLiteralExpression(exprs);

            if (!tokens.match(RIGHT_BRACKET)) {
                listLiteralExpression.addError(ErrorType.UNTERMINATED_LIST);
            }

            return listLiteralExpression;
        } else if (tokens.match(IDENTIFIER)) {
            String identifier = tokens.consumeToken().getStringValue();
            IdentifierExpression identifierExpression = new IdentifierExpression(identifier);

            if (tokens.match(LEFT_PAREN)) {

                Token token = tokens.consumeToken();
                List<Expression> exprs = new ArrayList<>();

                if (!tokens.match(RIGHT_PAREN)) {
                    Expression val = parseExpression();
                    exprs.add(val);

                    do {
                        tokens.consumeToken();
                        val = parseExpression();
                        exprs.add(val);
                    } while (tokens.match(COMMA));
                }

                FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifier, exprs);

                if (!tokens.match(RIGHT_PAREN)) {
                    functionCallExpression.addError(ErrorType.UNTERMINATED_ARG_LIST);
                }

                return functionCallExpression;
            } else {
                return identifierExpression;
            }
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
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }
}
