package edu.montana.csci.csci468;

import edu.montana.csci.csci468.parser.expressions.AdditiveExpression;
import edu.montana.csci.csci468.parser.expressions.StringLiteralExpression;
import edu.montana.csci.csci468.parser.expressions.UnaryExpression;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class Tests extends CatscriptTestBase{

    @Test
    void funExpressionsWork() {
        assertEquals(-7, evaluateExpression("8 + 6 * (7 - 5) - 3 * (0 + 9)"));
        UnaryExpression expr = parseExpression("not not true");
        assertEquals(true, expr.isNot());
    }

    @Test
    void poohYoureEatingRecursion() {
        assertEquals("9\n6\n3\n0\n", executeProgram(
                "function threebees(x : int) {\n" +
                        "print(x)" +
                        "if(x > 0) {" +
                        "  threebees((x * 1)-3)" +
                        "}" +
                        "}\n" +
                        "threebees(9)"
        ));
    }

    @Test
    void beesKnees() {
        assertEquals("[8, 0, 0, 8, 5]\n", executeProgram("function mirror(x : list<int>) { print(x) }" +
                "mirror([8, 0, 0, 8, 5])"));
    }

//    @Test
//    void literalExpressionsEvaluatesProperly() {
//        StringLiteralExpression expr = parseExpression("\"pie guy\"");
//        assertEquals("pie guy", expr.getValue());
//        expr = parseExpression("\"There can only be " + 1 + " pie guy\"");
//        assertEquals("There can only be 1 pie guy", expr.getValue());
//    }
//
//    @Test
//    public void parseSubtractionExpressionWorks() {
//        AdditiveExpression expr = parseExpression("1 - -1");
//        assertFalse(expr.isAdd());
//        expr = parseExpression("(6+5) - 6");
//        assertTrue(!expr.isAdd());
//    }
//
//    @Test
//    void factorExpressionEvaluatesProperly() {
//        assertEquals(2, evaluateExpression("(10 - -2)/6 "));
//        assertEquals(5, evaluateExpression("-1 * -5"));
//        assertEquals(-8, evaluateExpression("100*2/25*-1"));
//    }


}
