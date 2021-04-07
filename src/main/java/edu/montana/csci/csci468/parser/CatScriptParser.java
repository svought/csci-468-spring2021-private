package edu.montana.csci.csci468.parser;

import com.sun.jdi.connect.Connector;
import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;
import org.apache.commons.lang.ObjectUtils;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.security.Policy;
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
        Statement statement = parseStatement();
        if (statement != null) return statement;
        if(currentFunctionDefinition != null)
            parseReturnStatement();
        statement = parseFunctionDefinitionStatement();
        if (statement != null) return statement;
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseFunctionDefinitionStatement() {
        if(tokens.match(FUNCTION)) {
            FunctionDefinitionStatement functionDefinitionStatement = new FunctionDefinitionStatement();
            Token start = tokens.consumeToken();
            functionDefinitionStatement.setStart(start);

            Token functionName = require(IDENTIFIER, functionDefinitionStatement);
            functionDefinitionStatement.setName(functionName.getStringValue());
            require(LEFT_PAREN, functionDefinitionStatement);

            if (tokens.match(IDENTIFIER)) {
                String currentName = tokens.consumeToken().getStringValue();
                TypeLiteral literal = new TypeLiteral();

                if (tokens.matchAndConsume(COLON)) {
                    literal = parseTypeExpression();
//                    functionDefinitionStatement.addParameter(currentName, literal)
                } else {
                    literal.setType(CatscriptType.OBJECT);
//                    functionDefinitionStatement.addParameter(currentName, null);
                }

                functionDefinitionStatement.addParameter(currentName, literal);

                while (tokens.matchAndConsume(COMMA)) {
                    currentName = tokens.consumeToken().getStringValue();

                    if (tokens.matchAndConsume(COLON)) {
                        literal = parseTypeExpression();
                    } else {
                        literal.setType(CatscriptType.OBJECT);
                    }

                    functionDefinitionStatement.addParameter(currentName, literal);
                    literal = new TypeLiteral();
                }
            }

            require(RIGHT_PAREN, functionDefinitionStatement);

            if (tokens.matchAndConsume(COLON)){
                TypeLiteral explicitType = parseTypeExpression();
                functionDefinitionStatement.setType(explicitType);
            } else {
                TypeLiteral voidType = new TypeLiteral();
                voidType.setType(CatscriptType.VOID);
                functionDefinitionStatement.setType(voidType);
            }

            require(LEFT_BRACE, functionDefinitionStatement);

            currentFunctionDefinition = functionDefinitionStatement;
            List<Statement> statements = new ArrayList<>();

            try {
                Statement currentStatement;
                while (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
                    currentStatement = parseStatement();
                    statements.add(currentStatement);
                }
            } finally {
                currentFunctionDefinition = null;
            }

            functionDefinitionStatement.setEnd(require(RIGHT_BRACE, functionDefinitionStatement));
            functionDefinitionStatement.setBody(statements);

            return functionDefinitionStatement;
        } else {
            return null;
        }
    }

    private Statement parseStatement() {
        if(tokens.match(FOR))             return parseForStatement();
        else if(tokens.match(PRINT))      return parsePrintStatement();
        else if(tokens.match(VAR))        return parseVarStatement();
        else if(tokens.match(IF))         return parseIfStatement();
        else if(tokens.match(RETURN))     return parseReturnStatement();
        else if(tokens.match(IDENTIFIER)) {
            String name = tokens.consumeToken().getStringValue();
            if(tokens.match(EQUAL)){
                return parseAssignmentStatement(name);
            }else {
                return parseFunctionCallStatement(name);
            }
        }
        else return null;
    }

    private Statement parseFunctionCallStatement(String name) {
        FunctionCallStatement functionCallStatement = null;

        require(LEFT_PAREN, functionCallStatement);
        List<Expression> exprs = new ArrayList<>();

        if (!tokens.match(RIGHT_PAREN) && !tokens.match(EOF)) {
            Expression val = parseExpression();
            exprs.add(val);

            do {
                tokens.consumeToken();
                val = parseExpression();
                exprs.add(val);
            } while (tokens.match(COMMA));
        }

        require(RIGHT_PAREN, functionCallStatement);

        FunctionCallExpression functionCallExpression = new FunctionCallExpression(name, exprs);
        functionCallStatement = new FunctionCallStatement(functionCallExpression);

        return functionCallStatement;
    }

    private Statement parseAssignmentStatement(String name) {
        AssignmentStatement assignmentStatement = new AssignmentStatement();

        assignmentStatement.setVariableName(name);
        require(EQUAL, assignmentStatement);
        Expression expression = parseExpression();

        assignmentStatement.setExpression(expression);

        return assignmentStatement;
    }

    private Statement parseReturnStatement() {
        if(tokens.match(RETURN)) {
            ReturnStatement returnStatement = new ReturnStatement();

            returnStatement.setFunctionDefinition(currentFunctionDefinition);
            returnStatement.setStart(require(RETURN, returnStatement));

            if (!tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
                Expression expression = parseExpression();
                returnStatement.setExpression(expression);
            }

            return returnStatement;
        } else {
            return null;
        }
    }

    private Statement parseForStatement() {
        ForStatement forStatement = new ForStatement();
        forStatement.setStart(tokens.consumeToken());

        require(LEFT_PAREN, forStatement);
        Token varName = require(IDENTIFIER, forStatement);
        forStatement.setVariableName(varName.getStringValue());
        require(IN, forStatement);
        Expression expression = parseExpression();
        forStatement.setExpression(expression);

        require(RIGHT_PAREN, forStatement);
        require(LEFT_BRACE, forStatement);

        List<Statement> body = new ArrayList<>();

        if (tokens.match(LEFT_BRACE)) {
            do {
                tokens.consumeToken();
                Statement statement = parseStatement();
                body.add(statement);
            } while (tokens.match(LEFT_BRACE));

            require(RIGHT_BRACE, forStatement);
        }

        do {
            Statement statement = parseStatement();
            body.add(statement);
        } while(!tokens.match(RIGHT_BRACE) && !tokens.match(EOF));

        forStatement.setBody(body);
        require(RIGHT_BRACE, forStatement);

        return forStatement;
    }

    private Statement parseVarStatement() {
        TypeLiteral explicitType = null;
        VariableStatement variableStatement = new VariableStatement();
        variableStatement.setStart(tokens.consumeToken());

        Token varName = require(IDENTIFIER, variableStatement);
        variableStatement.setVariableName(varName.getStringValue());

        if(tokens.matchAndConsume(COLON)) {
            explicitType = parseTypeExpression();
            variableStatement.setExplicitType(explicitType.getType());
        }

        require(EQUAL, variableStatement);

        Expression varExpression = parseExpression();
        variableStatement.setExpression(varExpression);

        return variableStatement;
    }

    private Statement parseIfStatement() {
        IfStatement ifStatement = new IfStatement();
        ifStatement.setStart(tokens.consumeToken());

        require(LEFT_PAREN, ifStatement);
        ifStatement.setExpression((parseExpression()));
        require(RIGHT_PAREN, ifStatement);
        require(LEFT_BRACE, ifStatement);


        List<Statement> statements = new ArrayList<>();
        Statement statement;
        do {
            if(tokens.match(EOF)){
                break;
            } else {
                statement = parseStatement();
                statements.add(statement);
            }
        } while (!tokens.match(RIGHT_BRACE));

        require(RIGHT_BRACE, ifStatement);
        ifStatement.setTrueStatements(statements);

        if(tokens.matchAndConsume(ELSE)){
            List<Statement> elseStatements = new ArrayList<>();
            Statement elseStatement;
            if(tokens.matchAndConsume(LEFT_BRACE)){
                do {
                    if(tokens.match(EOF)){
                        break;
                    } else {
                        elseStatement = parseStatement();
                        elseStatements.add(elseStatement);
                    }
                } while (!tokens.match(RIGHT_BRACE));

                require(RIGHT_BRACE, ifStatement);
                ifStatement.setElseStatements(elseStatements);
            } else {
                parseIfStatement();
            }
        }

        return ifStatement;
    }

    private Statement parsePrintStatement() {
        PrintStatement printStatement = new PrintStatement();
        printStatement.setStart(tokens.consumeToken());

        require(LEFT_PAREN, printStatement);

        Expression expression = parseExpression();
        printStatement.setExpression(expression);
        printStatement.setEnd(require(RIGHT_PAREN, printStatement));

        return printStatement;
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private TypeLiteral parseTypeExpression() {
        if(tokens.match(IDENTIFIER)) {
            TypeLiteral literal = new TypeLiteral();
            Token token = tokens.consumeToken();

            if(tokens.matchAndConsume(LESS)) {
                CatscriptType listType = CatscriptType.getListType(parseTypeExpression().getType());
                literal.setType(listType);
                require(GREATER, literal);
            }
            else if(token.getStringValue().equals("int")) literal.setType(CatscriptType.INT);
            else if(token.getStringValue().equals("string")) literal.setType(CatscriptType.STRING);
            else if(token.getStringValue().equals("bool")) literal.setType(CatscriptType.BOOLEAN);
            else if(token.getStringValue().equals("object")) literal.setType(CatscriptType.OBJECT);

            return literal;
        }else {
            return null;
        }
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
        } else if (tokens.matchAndConsume(NULL)) {
            Expression nullLiteralExpression = new NullLiteralExpression();
            return nullLiteralExpression;
        } else if (tokens.match(LEFT_PAREN)) {
            tokens.consumeToken();
            Expression expr = parseExpression();
            Expression parenthesizedExpression = new ParenthesizedExpression(expr);

            require(RIGHT_PAREN, parenthesizedExpression);

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
            else {
                tokens.consumeToken();
            }

            return listLiteralExpression;
        } else if (tokens.match(IDENTIFIER)) {
            String identifier = tokens.consumeToken().getStringValue();
            IdentifierExpression identifierExpression = new IdentifierExpression(identifier);

            if (tokens.match(LEFT_PAREN)) {

                tokens.consumeToken();
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
