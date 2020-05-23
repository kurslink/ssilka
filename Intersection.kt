package com.example.app

import io.kaen.dagger.ExpressionParser
import kotlin.math.abs

class Intersection(firstFunc: String, secondFunc: String) {
    private val first = firstFunc
    private val second = secondFunc
    private val expressionParser = ExpressionParser()

    fun findIntersectionPoint(from: Float, to: Float): Pair<Float, Float>? {
        val faultValue = soluteFaultValue(from, to)

        when {
            abs(soluteValue(from)) < faultValue -> {
                return Pair(from, soluteFirstValue(from))
            }
            abs(soluteValue(to)) < faultValue -> {
                return Pair(to, soluteFirstValue(to))
            }
            isIntersect(from, to) -> {
                return soluteIntersectionPoint(from, to, faultValue)
            }
        }
        return null
    }

    private fun soluteIntersectionPoint(left: Float, right: Float, fault: Float): Pair<Float, Float> {
        var leftValue = left
        var rightValue = right
        while (abs(soluteValue(leftValue)) > fault && abs(soluteValue(rightValue)) > fault) {
            val middleValue = soluteMiddleValue(leftValue, rightValue)

            if (isIntersect(leftValue, middleValue)) {
                rightValue = middleValue
            } else {
                leftValue = middleValue
            }
        }
        if (abs(soluteValue(rightValue)) < fault) {
            return Pair(rightValue, soluteFirstValue(rightValue))
        }
        return Pair(leftValue, soluteFirstValue(leftValue))
    }

    private fun isIntersect(left: Float, right: Float): Boolean {
        val leftValue = soluteValue(left)
        val rightValue = soluteValue(right)
        if (leftValue > 0 && rightValue < 0) return true
        if (leftValue < 0 && rightValue > 0) return true
        return false
    }

    private fun soluteValue(x: Float): Float {
        return  soluteFirstValue(x) - soluteSecondValue(x)
    }

    private fun soluteFirstValue(x: Float): Float {
        return expressionParser.evaluate(first.replace("x", x.toString())).toFloat()
    }

    private fun soluteSecondValue(x: Float): Float {
        return expressionParser.evaluate(second.replace("x", x.toString())).toFloat()
    }

    private fun soluteMiddleValue(from: Float, to: Float): Float {
        return (to + from) / 2
    }

    private fun soluteFaultValue(from: Float, to: Float): Float {
        return (to - from) / 10000
    }
}