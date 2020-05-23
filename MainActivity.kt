package com.example.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.kaen.dagger.ExpressionParser
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var addFunctionButton: Button? = null
    private var drawGraphicButton: Button? = null
    private var layout: LinearLayout? = null

    private var fromEditText: EditText? = null
    private var toEditText: EditText? = null
    private var stepEditText: EditText? = null

    private var arrayOfFunctionInputViews: ArrayList<View> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val actionBar = supportActionBar

        actionBar!!.title = "Главная страница"

        layout = findViewById(R.id.functionsLinearLayout)

        fromEditText = findViewById(R.id.from)
        toEditText = findViewById(R.id.to)
        stepEditText = findViewById(R.id.step)

        addFunctionButton = findViewById(R.id.addFunctionButton)

        addFunctionButton!!.setOnClickListener {
            val layoutInflater = LayoutInflater.from(this)
            val view = layoutInflater.inflate(R.layout.function_input, layout, false)
            view.id = View.generateViewId()

            val deleteButton: Button = view.findViewById(R.id.deleteFunctionButton)
            deleteButton.setOnClickListener(handleDeleteFunctionInputView(view.id))

            arrayOfFunctionInputViews.add(view)

            layout!!.addView(view)
        }

        drawGraphicButton = findViewById(R.id.drawGraphicsButton)

        drawGraphicButton!!.setOnClickListener {
            val functionsList: Array<String> = arrayOfFunctionInputViews
                .map { el -> el.findViewById<EditText>(R.id.functionInputEditText).text.toString() }
                .toTypedArray()

            val expressionParser = ExpressionParser()

            try {
                if (functionsList.isEmpty()) throw Exception("List is empty")

                for (func in functionsList) {
                    expressionParser.evaluate(func.replace("x", "0"))
                }
                dataValidation()

                val intent = Intent(this, GraphicsActivity::class.java)
                intent.putExtra("FUNCTIONS_LIST", functionsList)
                intent.putExtra("FROM_VALUE", fromEditText!!.text.toString().toFloat())
                intent.putExtra("TO_VALUE", toEditText!!.text.toString().toFloat())
                intent.putExtra("STEP_VALUE", stepEditText!!.text.toString().toFloat())
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Данные введены не корректно", Toast.LENGTH_SHORT).show()
                Log.e("ERROR PARSER", e.toString())
            }
        }
    }

    private fun handleDeleteFunctionInputView(id: Int): (v: View) -> Unit {
        return {
            val element = arrayOfFunctionInputViews.find { v -> v.id == id }
            val index = arrayOfFunctionInputViews.indexOf(element)
            layout!!.removeView(element)
            arrayOfFunctionInputViews.removeAt(index)
        }
    }

    private fun dataValidation() {
        val fromValueString = fromEditText!!.text.toString().trim()
        val toValueString = toEditText!!.text.toString().trim()
        val stepValueString = stepEditText!!.text.toString().trim()

        if (fromValueString.isEmpty() || toValueString.isEmpty() || stepValueString.isEmpty()) {
            throw Exception("Empty string")
        }

        if (fromValueString.toFloat() >= toValueString.toFloat()) throw Exception("From more then to")

        if (stepValueString.toFloat() <= 0) throw Exception("Step is very low")
    }
}
