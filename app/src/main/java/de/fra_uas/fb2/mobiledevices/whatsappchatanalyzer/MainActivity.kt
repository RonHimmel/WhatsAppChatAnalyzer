package de.fra_uas.fb2.mobiledevices.whatsappchatanalyzer

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var textViewFileContent: TextView
    private var messageCounts = mutableMapOf<String, Int>()
    private var allMessages = StringBuilder()
    private var uncutMessages = StringBuilder()
    private lateinit var ratioMessages: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarBackground: ProgressBar
    private lateinit var monthlyGraph: LineChart


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        textViewFileContent = findViewById(R.id.text_view_file_content)
        ratioMessages = findViewById(R.id.ratio_of_messages)
        progressBar = findViewById(R.id.stats_progressbar)
        progressBarBackground = findViewById(R.id.background_progressbar)
        progressBarBackground.visibility = View.INVISIBLE
        progressBar.visibility = View.INVISIBLE
        monthlyGraph = findViewById(R.id.chart)
        monthlyGraph.visibility = View.INVISIBLE

        openDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                readTextFromUri(uri)
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        if(viewModel.textViewContent=="") {                                                         //view model will not be used at first -> info text is shown
            textViewFileContent.text = getString(R.string.info_text)
        }else if (viewModel.messageCounts.isNotEmpty()&& viewModel.allMessages.isNotEmpty()&&viewModel.graph){
            val text = viewModel.textViewContent                           //the first line is not made by the build diagram function so we have to add it first
            textViewFileContent.text = text
            allMessages=viewModel.allMessages
            uncutMessages=viewModel.uncutMessages
            messageCounts=viewModel.messageCounts
            buildGraph()
        }else if(viewModel.messageCounts.isNotEmpty()&& viewModel.allMessages.isNotEmpty()&&!viewModel.graph){
            val text = viewModel.textViewContent.split("\n")                              //the first line is not made by the build diagram function so we have to add it first
            textViewFileContent.text = text[0]+"\n"
            allMessages=viewModel.allMessages
            uncutMessages=viewModel.uncutMessages
            messageCounts=viewModel.messageCounts
            buildDiagram()
        }else textViewFileContent.text = viewModel.textViewContent

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.textViewContent = textViewFileContent.text.toString()
        if(messageCounts.isNotEmpty()) {
            viewModel.messageCounts = messageCounts
        }
        if(allMessages.isNotEmpty()) {
            viewModel.allMessages = allMessages
        }
        if(uncutMessages.isNotEmpty()){
            viewModel.uncutMessages = uncutMessages
        }
    }

    fun charButton(view: View) {
        if(allMessages.isEmpty()) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("char")
        }
    }

    private fun countChars(char: Char) {
        messageCounts.clear()
        var lines = allMessages.lines()
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            val trimmedLine = line.removeRange(0, nameEndIndex+1)                                   //remove the name to only count the message chars
            for (c in trimmedLine) {
                if (nameEndIndex != -1) {
                    val name = line.substring(0, nameEndIndex)
                    if(c==char) {
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                    }else messageCounts[name] = messageCounts.getOrDefault(name, 0)
                }
            }
        }
        textViewFileContent.text ="You searched for '$char':\n"
        buildDiagram()
    }


    private fun countWords(word: String) {
        messageCounts.clear()
        var lines = allMessages.lines()
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            val trimmedLine = line.removeRange(0, nameEndIndex+1)                                   //remove the name to only count the message chars
            val words = trimmedLine.split(" ")
            for (words in words) {
                if (nameEndIndex != -1) {
                    val name = line.substring(0, nameEndIndex)
                    if(words==word) {
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                    }else messageCounts[name] = messageCounts.getOrDefault(name, 0)
                }
            }
        }
        textViewFileContent.text ="You searched for '$word':\n"
        buildDiagram()
    }

    private fun countEmoji(emoji: String) {
        messageCounts.clear()
        var lines = allMessages.lines()
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            val trimmedLine = line.removeRange(0, nameEndIndex+1)                                   //remove the name to only count the message chars
            val words = trimmedLine.split(" ")
            for (word in words) {
                if (nameEndIndex != -1) {
                    val name = line.substring(0, nameEndIndex)
                    if(word.contains(emoji)) {
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                    }else messageCounts[name] = messageCounts.getOrDefault(name, 0)
                }
            }
        }
        textViewFileContent.text ="You searched for '$emoji':\n"
        buildDiagram()
    }

    private fun countMessages(){
        messageCounts.clear()
        var lines = allMessages.lines()
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            if(nameEndIndex!=-1){                                                               //indexOf returns -1 if there is no match
                val name = line.substring(0, nameEndIndex)
                messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
            }
        }
        textViewFileContent.text ="This is the amount of messages:\n"
    }

    private fun showInputDialog(method: String) {
        val dialogView = layoutInflater.inflate(R.layout.popup, null)
        val searchText = dialogView.findViewById<EditText>(R.id.editText_value)
        if (method == "char") {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select a character")
            searchText.hint = "Character"
            builder.setView(dialogView)
            builder.setPositiveButton("Confirm") { dialog, _ ->
                val input = searchText.text.toString()
                if (input.isEmpty()||input.length>1) {
                    Toast.makeText(this, "Please enter a character", Toast.LENGTH_SHORT).show()
                } else {
                    val character = input[0]
                    countChars(character)
                    dialog.dismiss()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }else if(method=="word"){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select a word")
            searchText.hint = "Word"
            builder.setView(dialogView)
            builder.setPositiveButton("Confirm") { dialog, _ ->
                val input = searchText.text.toString()
                if (input.isEmpty() || input.length<2) {
                    Toast.makeText(this, "Please enter a word", Toast.LENGTH_SHORT).show()
                } else {
                    val word = searchText.text.toString()
                    countWords(word)
                    dialog.dismiss()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }else if(method=="emoji"){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select a emoji")
            searchText.hint = "Emoji"
            builder.setView(dialogView)
            builder.setPositiveButton("Confirm") { dialog, _ ->
                val input = searchText.text.toString()
                if (input.isEmpty()) {
                    Toast.makeText(this, "Please enter a word", Toast.LENGTH_SHORT).show()
                } else {
                    val emoji = searchText.text.toString()
                    countEmoji(emoji)
                    dialog.dismiss()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }else if(method=="all"){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm your selection")
            searchText.visibility= View.INVISIBLE
            builder.setView(dialogView)
            builder.setPositiveButton("Confirm") { dialog, _ ->
                countMessages()
                var index = 1
                var stringBuilder = StringBuilder()
                for ((name, count) in messageCounts) {
                    if (index == messageCounts.size) {
                        stringBuilder.append("Total number for $name: $count")
                    } else stringBuilder.append("Total number for $name: $count\n")
                    index++
                }
                val text = textViewFileContent.text.toString()
                textViewFileContent.text = buildString {
                    append(text)
                    append(stringBuilder.toString())
                }
                buildGraph()
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }
    }

    fun emojiButton(view: View) {
        if(allMessages.isEmpty()) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("emoji")
        }
    }
    fun wordButton(view: View) {
        if(allMessages.isEmpty()) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("word")
        }
    }
    fun startButton(view: View) {
        if(allMessages.isEmpty()) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("all")
        }
    }

    fun openFileButton(view: View) {
        performFileSearch()
    }

    private fun buildGraph() {
        messageCounts.clear()
        var firstMonth = uncutMessages.lines()[0].substring(3, 8)
        val count = uncutMessages.lines().size
        val lastMonth = uncutMessages.lines()[count-2].substring(3, 8)
        var date=""
        var month = firstMonth.substring(0, 2).toInt()
        var year = firstMonth.substring(3, 5).toInt()
        while (date != lastMonth) {
            if(month<10){
                date = "0$month.$year"
            }else{
                date = "$month.$year"
            }
            messageCounts[date] = 0
            if(month<12){
                month++
            }else{
                month=1
                year++
            }
        }
        var lines = uncutMessages.lines()
        for (line in lines) {
            if(line.length>8) {
                val month = line.substring(3, 8)
                messageCounts[month] = messageCounts[month]!! + 1
            }
        }


        // the labels that should be drawn on the XAxis are the months
        val months = Array(messageCounts.size) { "" }

        // Convert the data to entries for the chart
        val entries = ArrayList<Entry>()
        var index = 0
        for ((key, value) in messageCounts) {
            entries.add(Entry(index.toFloat(), value.toFloat()))
            months[index] = key
            index++
        }

        val formatter: ValueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase): String {
                return months[value.toInt()]
            }
        }

        val dataSet = LineDataSet(entries, "Monthly # of messages")
        val lineData = LineData(dataSet)
        dataSet.color = ContextCompat.getColor(this, R.color.progressColor2)
        dataSet.setCircleColor(Color.RED) // Set circle color
        dataSet.circleRadius = 1f // Set circle radius
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.font)
        dataSet.valueTextSize = 7f // Set text size of points
        dataSet.lineWidth= 3f
        monthlyGraph.data = lineData

        // Customize XAxis to show month names
        monthlyGraph.xAxis.position = XAxis.XAxisPosition.BOTTOM
        monthlyGraph.xAxis.textColor = ContextCompat.getColor(this, R.color.font)
        monthlyGraph.axisLeft.axisMinimum = 0f
        monthlyGraph.axisLeft.textColor = ContextCompat.getColor(this, R.color.font)
        monthlyGraph.axisRight.isEnabled = false
        monthlyGraph.description.isEnabled = false
        monthlyGraph.xAxis.granularity = 1f
        monthlyGraph.xAxis.valueFormatter= formatter                                                //uses the overwritten formatter to show the months instead of integers


        progressBar.visibility = View.INVISIBLE
        progressBarBackground.visibility = View.INVISIBLE
        ratioMessages.visibility = View.INVISIBLE
        monthlyGraph.visibility = View.VISIBLE
        viewModel.graph = true

        // Refresh the chart
        monthlyGraph.invalidate()
    }

    fun questionButton(view: View) {
        textViewFileContent.text = getString(R.string.question_text)
        progressBar.visibility = View.INVISIBLE
        progressBarBackground.visibility = View.INVISIBLE
        ratioMessages.visibility = View.INVISIBLE
        monthlyGraph.visibility = View.INVISIBLE
    }

    private fun performFileSearch() {
        // Trigger the document picker
        openDocumentLauncher.launch(arrayOf("text/plain"))
    }

    private fun buildDiagram(){
        val ratio = StringBuilder()
        var one = 0
        var two = 0
        val stringBuilder = StringBuilder()
        if(messageCounts.values.max()==0){                                                          //if all values are 0 there are no messages containing the search
            progressBar.visibility = View.INVISIBLE
            progressBarBackground.visibility = View.INVISIBLE
            ratioMessages.visibility = View.INVISIBLE
            stringBuilder.append("There were no messages containing your search")
        }else {
            ratioMessages.visibility = View.VISIBLE
            textViewFileContent.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            progressBarBackground.visibility = View.VISIBLE
            var index = 1
            for ((name, count) in messageCounts) {
                if(index==messageCounts.size) {
                    stringBuilder.append("Total number for $name: $count")
                }else stringBuilder.append("Total number for $name: $count\n")
                index++
                if (one == 0) {
                    one = count
                    ratio.append("$count/")
                } else if (two == 0) {
                    two = count
                    ratio.append("$count")
                } else if (two != 0) {
                    progressBar.visibility = View.INVISIBLE
                    progressBarBackground.visibility = View.INVISIBLE
                    ratioMessages.visibility = View.INVISIBLE
                }
            }
        }
        val text = textViewFileContent.text.toString()
        textViewFileContent.text = buildString {
            append(text)
            append(stringBuilder.toString())
        }
        ratioMessages.text = ratio.toString()
        progressBar.progress= two.toFloat().div((two+one).toFloat()).times(100).toInt()
        monthlyGraph.visibility = View.INVISIBLE
        viewModel.graph = false
    }

    private fun readTextFromUri(uri: Uri) {
        val contentResolver = contentResolver
        messageCounts.clear()
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    uncutMessages.clear()
                    allMessages.clear()
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.length>17&&line[0].isDigit()&& line[16] == '-') {
                            uncutMessages.append(line)
                            uncutMessages.append('\n')
                            val trimmedLine = line.removeRange(0, 18)
                            allMessages.append(trimmedLine)
                            allMessages.append('\n')
                            val nameEndIndex = trimmedLine.indexOf(':')
                            if (nameEndIndex != -1) {
                                val name = trimmedLine.substring(0, nameEndIndex)
                                messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                            }
                        }
                        line = reader.readLine()
                    }
                }
                textViewFileContent.text ="This is the amount of messages:\n"
                buildDiagram()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
