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
import com.github.mikephil.charting.charts.LineChart                                                //imports of the library that will be used for the graph
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()                                            //implements the view model
    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>                //used to open the file picker
    private lateinit var infoTextViewContent: TextView                                              //the text view that shows the main text
    private var messageCounts = mutableMapOf<String, Int>()                                         //holds key value pairs of the name and the amount of messages
    private var cutMessages = StringBuilder()                                                       //holds all messages without the timestamps
    private var uncutMessages = StringBuilder()                                                     //holds messages with timestamp
    private lateinit var ratioMessages: TextView                                                    //the text view that shows the ratio of messages
    private lateinit var progressBar: ProgressBar                                                   //one of the progress bars to show the ratio
    private lateinit var progressBarBackground: ProgressBar                                         //second progress bar to show the ratio
    private lateinit var monthlyGraph: LineChart                                                    //the chart that shows the monthly messages


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        infoTextViewContent = findViewById(R.id.text_view_file_content)                             //now the values are getting to now their view IDs
        ratioMessages = findViewById(R.id.ratio_of_messages)
        progressBar = findViewById(R.id.stats_progressbar)
        progressBarBackground = findViewById(R.id.background_progressbar)
        progressBarBackground.visibility = View.INVISIBLE
        progressBar.visibility = View.INVISIBLE
        monthlyGraph = findViewById(R.id.chart)
        monthlyGraph.visibility = View.INVISIBLE

        openDocumentLauncher = registerForActivityResult(                                           // Set up the file picker
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                readTextFromUri(uri)
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        if(viewModel.infoTextViewContent=="") {                                                         //view model will not be used at first -> info text is shown
            infoTextViewContent.text = getString(R.string.info_text)
        }else if (viewModel.messageCounts.isNotEmpty()&& viewModel.cutMessages.isNotEmpty()&&viewModel.graph){
            val text = viewModel.infoTextViewContent                                                    //the first line is not made by the build diagram function so we have to add it first
            infoTextViewContent.text = text
            cutMessages=viewModel.cutMessages
            uncutMessages=viewModel.uncutMessages
            messageCounts=viewModel.messageCounts
            buildGraph()
        }else if(viewModel.messageCounts.isNotEmpty()&& viewModel.cutMessages.isNotEmpty()&&!viewModel.graph){
            val text = viewModel.infoTextViewContent.split("\n")                              //the first line is not made by the build diagram function so we have to add it first
            infoTextViewContent.text = text[0]+"\n"
            cutMessages=viewModel.cutMessages
            uncutMessages=viewModel.uncutMessages
            messageCounts=viewModel.messageCounts
            buildDiagram()
        }else infoTextViewContent.text = viewModel.infoTextViewContent

    }

    override fun onSaveInstanceState(outState: Bundle) {                                            //called whenever the activity is destroyed to save the data
        super.onSaveInstanceState(outState)
        viewModel.infoTextViewContent = infoTextViewContent.text.toString()
        if(messageCounts.isNotEmpty()) {
            viewModel.messageCounts = messageCounts
        }
        if(cutMessages.isNotEmpty()) {
            viewModel.cutMessages = cutMessages
        }
        if(uncutMessages.isNotEmpty()){
            viewModel.uncutMessages = uncutMessages
        }
    }
    fun openFileButton(view: View){                                                                 //button to open a file and read the content
        performFileSearch()
    }
    fun questionButton(view: View) {
        infoTextViewContent.text = getString(R.string.question_text)
        progressBar.visibility = View.INVISIBLE
        progressBarBackground.visibility = View.INVISIBLE
        ratioMessages.visibility = View.INVISIBLE
        monthlyGraph.visibility = View.INVISIBLE
    }

    fun charButton(view: View) {                                                                    //button to select character
        if(cutMessages.isEmpty()) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("char")
        }
    }
    fun emojiButton(view: View) {                                                                   //button to select emoji
        if(cutMessages.isEmpty()) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("emoji")
        }
    }
    fun wordButton(view: View) {                                                                    //button to select a word
        if(cutMessages.isEmpty()) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("word")
        }
    }
    fun graphButton(view: View) {                                                                   //button to build the graph
        if(cutMessages.isEmpty()) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("all")
        }
    }

    private fun countChars(char: Char) {                                                            //gets a character and iterates through all the messages
        messageCounts.clear()
        var lines = cutMessages.lines()
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
        infoTextViewContent.text ="You searched for '$char':\n"
        buildDiagram()
    }


    private fun countWords(word: String) {                                                          //gets a string and iterates through all the messages
        messageCounts.clear()
        var lines = cutMessages.lines()
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
        infoTextViewContent.text ="You searched for '$word':\n"
        buildDiagram()
    }

    private fun isEmoji(codePoint: Int): Boolean {
        return codePoint in 0x1F300..0x1F6FF ||                                                 // Miscellaneous Symbols and Pictographs
                codePoint in 0x2600..0x26FF ||                                                  // Miscellaneous Symbols
                codePoint in 0x1F900..0x1F9FF ||                                                // Supplemental Symbols and Pictographs
                codePoint in 0x1F1E6..0x1F1FF                                                   // Flags
    }

    private fun countEmoji(emoji: String) {
        messageCounts.clear()
        val lines = cutMessages.lines()
        val emojiCodePoints = emoji.codePoints().toArray()                                          //puts the emoji in an array of codepoints (some need it e.g. 👍🏽 (thumbs up with medium skin tone): U+1F44D U+1F3FD)
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            if (nameEndIndex != -1) {
                val name = line.substring(0, nameEndIndex)
                val message = line.substring(nameEndIndex + 1).trim()

                var i = 0
                while (i < message.length) {
                    if (matchesEmojiAtIndex(message, i, emojiCodePoints)) {
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                        i += emojiCodePoints.size                                                   //if emoji was found we move the index by the length of the emoji
                    } else {
                        i++                                                                         //if not we just move the index by one
                        messageCounts[name] = messageCounts.getOrDefault(name, 0)
                    }
                }
            }
        }

        infoTextViewContent.text = "You searched for '$emoji':\n"
        buildDiagram()
    }

    private fun matchesEmojiAtIndex(message: String, startIndex: Int, emojiCodePoints: IntArray): Boolean {
        var j = startIndex
        for (i in emojiCodePoints.indices) {
            if (j >= message.length) return false
            val codePoint = message.codePointAt(j)
            if (codePoint != emojiCodePoints[i]) return false
            j += Character.charCount(codePoint)                                                     //we need to increment j by 2 if we have a surrogate pair
        }
        return true
    }

    private fun countMessages(){
        messageCounts.clear()
        var lines = cutMessages.lines()
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            if(nameEndIndex!=-1){                                                                   //indexOf returns -1 if there is no match
                val name = line.substring(0, nameEndIndex)
                messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
            }
        }
        infoTextViewContent.text ="This is the amount of messages:\n"
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
                if (input.isEmpty() || input.length<2||isEmoji(input.codePointAt(0))) {
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
                buildInfoText()
                buildGraph()
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }
    }

    private fun buildInfoText(){
        var index = 1
        var stringBuilder = StringBuilder()
        for ((name, count) in messageCounts) {
            if (index == messageCounts.size) {
                stringBuilder.append("Total number for $name: $count")
            } else stringBuilder.append("Total number for $name: $count\n")
            index++
        }
        val text = infoTextViewContent.text.toString()
        infoTextViewContent.text = buildString {
            append(text)
            append(stringBuilder.toString())
        }
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
                val index = value.toInt()
                return if (index >= 0 && index < months.size) {
                    months[index]
                } else {
                    ""  // or return a default value
                }
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


    private fun performFileSearch() {
        // Trigger the document picker
        openDocumentLauncher.launch(arrayOf("text/plain"))
    }

    private fun buildDiagram(){
        val ratio = StringBuilder()
        var one = 0
        var first = true
        var two = 0
        val stringBuilder = StringBuilder()
        if(messageCounts.values.max()==0){                                                          //if all values are 0 there are no messages containing the search
            progressBar.visibility = View.INVISIBLE
            progressBarBackground.visibility = View.INVISIBLE
            ratioMessages.visibility = View.INVISIBLE
            stringBuilder.append("There were no messages containing your search")
        }else {
            ratioMessages.visibility = View.VISIBLE
            infoTextViewContent.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            progressBarBackground.visibility = View.VISIBLE
            var index = 1
            for ((name, count) in messageCounts) {
                if(index==messageCounts.size) {
                    stringBuilder.append("Total number for $name: $count")
                }else stringBuilder.append("Total number for $name: $count\n")
                index++
                if (first) {
                    one = count
                    first = false
                    ratio.append("$count/")
                } else{
                    two = count
                    ratio.append("$count")
                }
            }
        }
        ratioMessages.text = ratio.toString()
        if(messageCounts.size>2) {
            progressBar.visibility = View.INVISIBLE
            progressBarBackground.visibility = View.INVISIBLE
            ratioMessages.visibility = View.VISIBLE
            ratioMessages.text = "no pie chart for group chats"
        }
        val text = infoTextViewContent.text.toString()
        infoTextViewContent.text = buildString {
            append(text)
            append(stringBuilder.toString())
        }
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
                    cutMessages.clear()
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.length>17&&line[0].isDigit()&& line[16] == '-') {
                            uncutMessages.append(line)
                            uncutMessages.append('\n')
                            val trimmedLine = line.removeRange(0, 18)
                            cutMessages.append(trimmedLine)
                            cutMessages.append('\n')
                            val nameEndIndex = trimmedLine.indexOf(':')
                            if (nameEndIndex != -1) {
                                val name = trimmedLine.substring(0, nameEndIndex)
                                messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                            }
                        }
                        line = reader.readLine()
                    }
                }
                infoTextViewContent.text ="This is the amount of messages:\n"
                buildDiagram()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
