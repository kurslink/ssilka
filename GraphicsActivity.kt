package com.example.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import co.csadev.kellocharts.listener.LineChartOnValueSelectListener
import co.csadev.kellocharts.model.*
import co.csadev.kellocharts.util.ChartUtils
import co.csadev.kellocharts.view.LineChartView
import io.kaen.dagger.ExpressionParser

class GraphicsActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_graphics)
        val actionBar = supportActionBar

        actionBar!!.title = "Графики"
        actionBar.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val bundle = Bundle()
            bundle.putStringArray("FUNCTIONS_LIST", intent.getStringArrayExtra("FUNCTIONS_LIST"))
            bundle.putFloat("FROM_VALUE", intent.getFloatExtra("FROM_VALUE", 0f))
            bundle.putFloat("TO_VALUE", intent.getFloatExtra("TO_VALUE", 0f))
            bundle.putFloat("STEP_VALUE", intent.getFloatExtra("STEP_VALUE", 1f))

            val placeholderFragment = PlaceholderFragment()
            placeholderFragment.arguments = bundle

            supportFragmentManager
                .beginTransaction()
                .add(R.id.container, placeholderFragment)
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * A fragment containing a line chart.
     */
    class PlaceholderFragment : Fragment() {

        private var fromValue: Float = 0f
        private var toValue: Float = 0f
        private var stepValue: Float = 1f

        private var arrayFunctions: Array<String> = emptyArray()

        private var chart: LineChartView? = null
        private var data: LineChartData? = null
        private val maxNumberOfLines = 4
        private val numberOfPoints = 12

        private var numbersTab = Array(maxNumberOfLines) { FloatArray(numberOfPoints) }

        private var hasAxes = true
        private var hasAxesNames = true
        private var hasLines = true
        private var hasPoints = false
        private var shape = ValueShape.CIRCLE
        private var isFilled = false
        private var hasLabels = false
        private var isCubic = false
        private var hasLabelForSelected = false
        private var pointsHaveDifferentColor: Boolean = false

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            setHasOptionsMenu(true)

            arrayFunctions = this.arguments!!.getStringArray("FUNCTIONS_LIST") as Array<String>

            fromValue = this.arguments!!.getFloat("FROM_VALUE")
            toValue = this.arguments!!.getFloat("TO_VALUE")
            stepValue = this.arguments!!.getFloat("STEP_VALUE")

            val rootView = inflater.inflate(R.layout.fragment_line_chart, container, false)

            chart = rootView.findViewById<View>(R.id.chart) as LineChartView
            chart?.onValueTouchListener = ValueTouchListener()

            generateValues()

            generateData()

            // Disable viewport recalculations, see toggleCubic() method for more info.
            chart?.isViewportCalculationEnabled = false

            resetViewport()

            return rootView
        }

        private fun getCountPoints(): Int {
            return ((toValue - fromValue) / stepValue).toInt()
        }

        private fun getAxisXValues(): Array<Float> {
            val iterator = generateSequence(
                Pair(fromValue, fromValue + stepValue), { Pair(it.second, it.second + stepValue) }
            ).map { it.first }.iterator()
            return Array(getCountPoints()) { iterator.next() }
        }

        private fun generateValues() {
            val expressionParser = ExpressionParser()
            val xValues = getAxisXValues()

            numbersTab = Array(arrayFunctions.size) { FloatArray(xValues.size) }

            for (i in arrayFunctions.indices) {
                for (xIndex in xValues.indices) {
                    val preparedString = arrayFunctions[i].replace("x", xValues[xIndex].toString())
                    numbersTab[i][xIndex] = expressionParser.evaluate(preparedString).toFloat()
                }
            }
        }

        private fun generateData() {

            val xValues = getAxisXValues()

            val lines = ArrayList<Line>()

            for (i in numbersTab.indices) {

                val values = ArrayList<PointValue>()

                for (xIndex in xValues.indices) {
                    values.add(PointValue(xValues[xIndex], numbersTab[i][xIndex]))
                }

                val line = Line(values)
                line.color = ChartUtils.COLORS[i]
                line.shape = shape
                line.isCubic = isCubic
                line.isFilled = isFilled
                line.hasLabels = hasLabels
                line.hasLabelsOnlyForSelected = hasLabelForSelected
                line.hasLines = hasLines
                line.hasPoints = hasPoints
                if (pointsHaveDifferentColor) {
                    line.pointColor = ChartUtils.COLORS[(i + 1) % ChartUtils.COLORS.size]
                }
                lines.add(line)
            }

            if (arrayFunctions.size > 1) {
                lines.add(getIntersectionLine())
            }

            val newData = LineChartData(lines)

            if (hasAxes) {
                val axisX = Axis(hasLines = true)
                val axisY = Axis(hasLines = true)
                if (hasAxesNames) {
                    axisX.name = "Axis X"
                    axisY.name = "Axis Y"
                }
                newData.axisXBottom = axisX
                newData.axisYLeft = axisY
            } else {
                newData.axisXBottom = null
                newData.axisYLeft = null
            }

            newData.baseValue = java.lang.Float.NEGATIVE_INFINITY
            chart?.lineChartData = newData
            data = newData
        }

        private fun getIntersectionLine(): Line {
            val values = ArrayList<PointValue>()
            // TODO Исправить - т.к. работает не корректно (0:10:1 - 1/x, x)

            val xValues = getAxisXValues()

            for (f1 in numbersTab.indices) {
                for (f2 in numbersTab.indices) {
                    if (f2 > f1) {
                        for (xIndex in 0 until xValues.size - 1) {
                            val firstFunction = findFunByDots(
                                Pair(xValues[xIndex], numbersTab[f1][xIndex]),
                                Pair(xValues[xIndex + 1], numbersTab[f1][xIndex + 1])
                            )
                            val secondFunction = findFunByDots(
                                Pair(xValues[xIndex], numbersTab[f2][xIndex]),
                                Pair(xValues[xIndex + 1], numbersTab[f2][xIndex + 1])
                            )
                            val intersectionPoint = Intersection(firstFunction, secondFunction)
                                .findIntersectionPoint(xValues[xIndex], xValues[xIndex + 1])
                            if (intersectionPoint != null) {
                                Log.d("INTERSECION", intersectionPoint.first.toString())
                                Log.d("INTERSECION", intersectionPoint.second.toString())

                                values.add(PointValue(intersectionPoint.first, intersectionPoint.second))
                            }
                        }
                    }
                }
            }

            val line = Line(values)
            line.color = ChartUtils.COLOR_BLUE
            line.shape = shape
            line.isCubic = isCubic
            line.isFilled = isFilled
            line.hasLabels = hasLabels
            line.hasLabelsOnlyForSelected = hasLabelForSelected
            line.hasLines = false
            line.hasPoints = true

            return line
        }

        private fun findFunByDots(first: Pair<Float, Float>, second: Pair<Float, Float>): String {
            val k = (second.second - first.second) / (second.first - first.first)
            val b = first.second - k * first.first
            return "${k}*x+$b"
        }

        private fun resetViewport() {
            val soluteFault: (Float, Float) -> Float = {a, b -> (a - b) / 8}
            val xValues = getAxisXValues()
            val faultX = soluteFault(xValues.last(), xValues.first())

            val minY: Float = numbersTab.map { it.min() }.minBy { it!!.toFloat() }!!
            val maxY: Float = numbersTab.map { it.max() }.maxBy { it!!.toFloat() }!!
            val faultY = soluteFault(maxY, minY)
            val v = chart?.maximumViewport.copy()
            v.bottom = minY - faultY
            v.top = maxY + faultY
            v.left = fromValue - faultX
            v.right = toValue + faultX
            chart?.maximumViewport = v
            chart?.currentViewport = v
        }

        private inner class ValueTouchListener : LineChartOnValueSelectListener {

            override fun onValueSelected(lineIndex: Int, pointIndex: Int, value: PointValue) {
                Toast.makeText(activity, "Selected: $value", Toast.LENGTH_SHORT).show()
            }

            override fun onValueDeselected() {
                // TODO Auto-generated method stub
            }

        }
    }
}