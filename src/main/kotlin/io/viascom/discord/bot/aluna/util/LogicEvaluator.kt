/*
 * Copyright 2024 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.viascom.discord.bot.aluna.util

/**
 * A class for evaluating logical expressions in string format.
 * LogicEvaluator supports boolean keywords (true, false), numbers, comparisons (>, <, =),
 * boolean compositions (AND: &&, OR: ||), and negation (!). It also allows for nested
 * expressions using parentheses.
 *
 * Usage example:
 * <pre>
 * val inputString = "!(3 < 5) && (false || 6 > 1)";
 * val result = LogicEvaluator.eval(inputString);
 * </pre>
 *
 */
object LogicEvaluator {

    /**
     * Evaluates a logical expression given as a string.
     *
     * @param inputString the string representation of the logical expression to be evaluated
     *
     * @return the boolean result of the evaluated expression
     * @throws IllegalArgumentException if the input string contains syntax errors or unsupported operations
     */
    @Throws(IllegalArgumentException::class)
    fun eval(inputString: String) = evaluate(parse(scan(inputString)))

    private fun scan(inputString: String): List<Token> = sequence {
        var index = 0
        while (index < inputString.length) {
            val char = inputString[index]
            if (char == '&' || char == '|' || char == '=') {
                check(inputString[++index] == char)
            }
            when {
                char.isDigit() -> {
                    var end = index
                    while (end < inputString.length && inputString[end].isDigit()) {
                        end += 1
                    }
                    val numToken = Token.Number(inputString.substring(index, end).toInt())
                    index = end - 1
                    yield(numToken)
                }

                char == 't' && inputString.substring(index, index + 4) == "true" -> {
                    index += 3
                    yield(Token.BooleanKeyword(true))
                }

                char == 'f' && inputString.substring(index, index + 5) == "false" -> {
                    index += 4
                    yield(Token.BooleanKeyword(false))
                }

                char in setOf('&', '|', '=', '<', '>', '!') -> {
                    when (char) {
                        '&' -> yield(Token.BooleanComposition(char))
                        '|' -> yield(Token.BooleanComposition(char))
                        '=', '<', '>' -> yield(Token.NumberComparison(char))
                        '!' -> yield(Token.Negation)
                    }
                }

                char == '(' -> yield(Token.ParenLeft)
                char == ')' -> yield(Token.ParenRight)
                else -> check(char.isWhitespace())
            }
            index += 1
        }
        yield(Token.EOF)
    }.toList()

    private fun parse(inputTokens: List<Token>): Expression {
        var index = 0

        fun expression(): Expression {
            val lastExpression = when (val firstToken = inputTokens[index++]) {
                is Token.ParenLeft -> {
                    val nestedExpression = expression()
                    val closingToken = inputTokens[index++]
                    check(closingToken is Token.ParenRight) { "Found $closingToken" }
                    nestedExpression
                }

                is Token.Number -> {
                    val opToken = inputTokens[index++]
                    check(opToken is Token.NumberComparison)
                    val op = when (opToken.c) {
                        '<' -> ::lessThan
                        '>' -> ::greaterThan
                        '=' -> ::equal
                        else -> error("Bad op $opToken")
                    }
                    val secondToken = inputTokens[index++]
                    check(secondToken is Token.Number)
                    Expression.NumberExpression(firstToken.value, op, secondToken.value)
                }

                is Token.BooleanKeyword -> {
                    Expression.BooleanKeywordExpression(firstToken.value)
                }

                is Token.Negation -> {
                    val nestedExpression = expression()
                    Expression.NegationExpression(nestedExpression)
                }

                else -> error("Parse error on $firstToken")
            }

            return when (val lookAhead = inputTokens[index]) {
                is Token.EOF, is Token.ParenRight -> lastExpression // pushback
                is Token.BooleanComposition -> {
                    // use lookAhead
                    index += 1
                    val op = when (lookAhead.c) {
                        '&' -> ::and
                        '|' -> ::or
                        else -> error("Bad op $lookAhead")
                    }
                    val secondExpression = expression()
                    Expression.BooleanExpression(lastExpression, op, secondExpression)
                }

                else -> error("Parse error on $lookAhead")
            }
        }

        return expression()
    }

    private fun and(b1: Boolean, b2: Boolean) = b1 && b2
    private fun or(b1: Boolean, b2: Boolean) = b1 || b2
    private fun lessThan(n1: Int, n2: Int): Boolean = n1 < n2
    private fun greaterThan(n1: Int, n2: Int): Boolean = n1 > n2
    private fun equal(n1: Int, n2: Int): Boolean = n1 == n2

    private fun evaluate(expr: Expression): Boolean = when (expr) {
        is Expression.BooleanTerm -> evaluate(expr.numExpr)
        is Expression.BooleanExpression -> expr.cmp.invoke(evaluate(expr.b1), evaluate(expr.b2))
        is Expression.NegationExpression -> !evaluate(expr.expr)
        is Expression.NumberExpression -> expr.cmp.invoke(expr.n1, expr.n2)
        is Expression.BooleanKeywordExpression -> expr.value
    }

    private sealed class Token {
        data class BooleanKeyword(val value: Boolean) : Token()
        data class BooleanComposition(val c: Char) : Token()
        data class NumberComparison(val c: Char) : Token()
        data class Number(val value: Int) : Token()
        object ParenLeft : Token()
        object ParenRight : Token()
        object Negation : Token()
        object EOF : Token()
    }

    private sealed class Expression {
        data class BooleanTerm(val numExpr: NumberExpression) : Expression()
        data class BooleanExpression(val b1: Expression, val cmp: (Boolean, Boolean) -> Boolean, val b2: Expression) :
            Expression()

        data class NumberExpression(val n1: Int, val cmp: (Int, Int) -> Boolean, val n2: Int) : Expression()
        data class BooleanKeywordExpression(val value: Boolean) : Expression()
        data class NegationExpression(val expr: Expression) : Expression()
    }

}
