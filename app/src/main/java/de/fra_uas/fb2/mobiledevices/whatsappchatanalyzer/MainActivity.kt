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
import com.github.mikephil.charting.charts.LineChart                                                //imports of the library that are used for the graph
import com.github.mikephil.charting.components.AxisBase                                             //https://weeklycoding.com/mpandroidchart-documentation/
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
        infoTextViewContent = findViewById(R.id.text_view_file_content)                             //now the views are getting their id
        ratioMessages = findViewById(R.id.ratio_of_messages)
        progressBar = findViewById(R.id.user_two_progressbar)
        progressBarBackground = findViewById(R.id.user_one_progressbar)
        monthlyGraph = findViewById(R.id.chart)

        progressBarBackground.visibility = View.INVISIBLE                                           //this is to hide the progress bars and graph when the app starts/reloads
        progressBar.visibility = View.INVISIBLE
        monthlyGraph.visibility = View.INVISIBLE

        openDocumentLauncher = registerForActivityResult(                                           // Set up the file picker
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                readTextFromUri(uri)                                                                //opens the file and reads the content
            } else {
                Toast.makeText(this, "File selection cancelled",
                    Toast.LENGTH_SHORT).show()
            }
        }

        if(viewModel.infoTextViewContent=="") {                                                     //view model will not be used at first -> info text is shown
            infoTextViewContent.text = getString(R.string.info_text)
        }else if (viewModel.messageCounts.isNotEmpty()&& viewModel.cutMessages.isNotEmpty()         //checks if the view model is not empty and if we used the graph before
                    &&viewModel.graph){
            val text = viewModel.infoTextViewContent                                                //assign the text back to the text view
            infoTextViewContent.text = text
            cutMessages=viewModel.cutMessages                                                       //assigns the values back from view model
            uncutMessages=viewModel.uncutMessages
            messageCounts=viewModel.messageCounts
            buildGraph()                                                                            //builds the graph again
        }else if(viewModel.messageCounts.isNotEmpty()&& viewModel.cutMessages.isNotEmpty()          //this is the case when the diagram was shown before
                    &&!viewModel.graph){
            val text = viewModel.infoTextViewContent.split("\n")                          //the first line is not made by the build diagram function so we have to add it first
            infoTextViewContent.text = text[0]+"\n"
            cutMessages=viewModel.cutMessages
            uncutMessages=viewModel.uncutMessages
            messageCounts=viewModel.messageCounts
            buildDiagram()                                                                          //builds the chart again
        }else infoTextViewContent.text = viewModel.infoTextViewContent                              //if there was no chart nor graph only the text is recreated

    }

    override fun onSaveInstanceState(outState: Bundle) {                                            //called whenever the activity is destroyed to save the data
        super.onSaveInstanceState(outState)                                                         //saves the data to the view model
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
        performFileSearch()                                                                         //function to open the file picker
    }
    fun questionButton(view: View) {                                                                //button to open the Q&A
        infoTextViewContent.text = getString(R.string.question_text)
        progressBar.visibility = View.INVISIBLE
        progressBarBackground.visibility = View.INVISIBLE
        ratioMessages.visibility = View.INVISIBLE
        monthlyGraph.visibility = View.INVISIBLE
    }

    fun charButton(view: View) {                                                                    //button to select character
        if(cutMessages.isEmpty()) {                                                                 //only if file selected it will show the popup dialogue
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("char")
        }
    }
    fun emojiButton(view: View) {                                                                   //button to select emoji
        if(cutMessages.isEmpty()) {                                                                 //only if file selected it will show the popup dialogue
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("emoji")
        }
    }
    fun wordButton(view: View) {                                                                    //button to select a word
        if(cutMessages.isEmpty()) {                                                                 //only if file selected it will show the popup dialogue
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("word")
        }
    }
    fun graphButton(view: View) {                                                                   //button to build the graph
        if(cutMessages.isEmpty()) {                                                                 //only if file selected it will show the popup dialogue
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }else{
            showInputDialog("all")
        }
    }

    private fun countChars(char: Char) {                                                            //gets a character and iterates through all the messages
        messageCounts.clear()                                                                       //clear the map to avoid duplicates
        var lines = cutMessages.lines()
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            val trimmedLine = line.removeRange(0, nameEndIndex+1)                                   //remove the name to only count the message chars
            for (c in trimmedLine) {                                                                //iterates through the chars
                if (nameEndIndex != -1) {                                                           //indexOf returns -1 if there is no match
                    val name = line.substring(0, nameEndIndex)
                    if(c==char) {
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1    //count increments by 1 if character was found
                    }else messageCounts[name] = messageCounts.getOrDefault(name, 0)      //make sure that the name is in the map
                }
            }
        }
        infoTextViewContent.text ="You searched for '$char':\n"                                     //shows the character in the text view
        buildDiagram()                                                                              //at the end the pie chart is build
    }
    private fun countWords(word: String) {                                                          //gets a string and iterates through all the messages
        messageCounts.clear()                                                                       //clear the map to avoid duplicates
        var lines = cutMessages.lines()
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            val trimmedLine = line.removeRange(0, nameEndIndex+1)                                   //remove the name to only count the message words
            val words = trimmedLine.split(" ")                                            //split by spaces to get every word
            for (words in words) {
                if (nameEndIndex != -1) {
                    val name = line.substring(0, nameEndIndex)                                      //get the name
                    if(words==word) {
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1    //count increments by 1 if word was found
                    }else messageCounts[name] = messageCounts.getOrDefault(name, 0)      //make sure that the name is in the map
                }
            }
        }
        infoTextViewContent.text ="You searched for '$word':\n"
        buildDiagram()
    }
    private fun isEmoji(codePoint: Int): Boolean {                                                  //checks if a codepoint is an emoji
        return codePoint in 0x1F300..0x1F6FF ||                                               //source:  https://unicode.org/emoji/charts/full-emoji-list.html
                codePoint in 0x2600..0x26FF ||
                codePoint in 0x1F900..0x1F9FF ||
                codePoint in 0x1F1E6..0x1F1FF||
                codePoint == 0x2764                                                                 //heart emoji was not in that range so we add it
    }

    private fun countEmoji(emoji: String) {                                                         //gets a emoji and iterates through all the messages
        messageCounts.clear()
        val lines = cutMessages.lines()
        val emojiCodePoints = emoji.codePoints().toArray()                                          //puts the emoji in an array of codepoints (because of combined emoji)
        for (line in lines) {                                                                       //e.g. 👍🏽 (thumbs up with medium skin tone): 0x1F44D 0x1F3FD) ->two code points
            val nameEndIndex = line.indexOf(':')
            if (nameEndIndex != -1) {
                val name = line.substring(0, nameEndIndex)
                val message = line.substring(nameEndIndex + 1).trim()
                var i = 0
                while (i < message.length) {                                                        //iterates through the message by character
                    if (matchesEmojiAtIndex(message, i, emojiCodePoints)) {
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1    //count increments by 1 if emoji was found
                        i += emojiCodePoints.size                                                   //if emoji was found we move the index by the length of the emoji
                    } else {
                        i++                                                                         //if not we just move the index by one
                        messageCounts[name] = messageCounts.getOrDefault(name, 0)        //make sure that the name is in the map
                    }
                }
            }
        }
        infoTextViewContent.text = "You searched for '$emoji':\n"                                   //shows the emoji in the text view
        buildDiagram()                                                                              //at the end the pie chart is build
    }
    private fun matchesEmojiAtIndex(message: String, startIndex: Int, emojiCodePoints: IntArray)    //checks if the emoji matches the message
    : Boolean {
        var j = startIndex
        for (expectedCodePoint in emojiCodePoints) {                                                //iterates through the emoji code points
            if (j >= message.length) return false                                                   //if the index is out of bounds we return false
            val actualCodePoint = message.codePointAt(j)                                            //gets the code point at the index
            if (actualCodePoint != expectedCodePoint) return false                                  //if the code points do not match we return false
            j += Character.charCount(actualCodePoint)                                               //move the index by the length of the code point
        }
        return true                                                                                 //if we get here we have a match
    }

    private fun countMessages(){                                                                    //gets all the messages and counts the messages per person
        messageCounts.clear()                                                                       //clear the map to avoid duplicates
        val lines = cutMessages.lines()
        for (line in lines) {
            val nameEndIndex = line.indexOf(':')
            if(nameEndIndex!=-1){                                                                   //indexOf returns -1 if there is no match
                val name = line.substring(0, nameEndIndex)
                messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1            //count increments by 1 for each message for a person
            }
        }
        infoTextViewContent.text ="This is the amount of messages:\n"                               //shows the amount of messages per person in the text view
    }

    private fun showInputDialog(method: String) {                                                   // based on the method it will show the input dialog
        val dialogView = layoutInflater.inflate(R.layout.popup, null)                          //used information from https://developer.android.com/develop/ui/views/components/dialogs
        val searchText = dialogView.findViewById<EditText>(R.id.editText_value)
        if (method == "char") {                                                                     //dialog for character
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select a character")
            searchText.hint = "Character"
            builder.setView(dialogView)
            builder.setPositiveButton("Confirm") { dialog, _ ->
                val input = searchText.text.toString()
                if (input.isEmpty()||input.length>1) {                                              //check if the input is empty or more than one character
                    Toast.makeText(this, "Please enter a character",
                        Toast.LENGTH_SHORT).show()
                } else {
                    val character = input[0]                                                        //get the character
                    countChars(character)                                                           //count the character and display the result
                    dialog.dismiss()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }else if(method=="word"){                                                                   //dialog for word
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select a word")
            searchText.hint = "Word"
            builder.setView(dialogView)
            builder.setPositiveButton("Confirm") { dialog, _ ->
                val input = searchText.text.toString()
                if (input.isEmpty() || input.length<2||isEmoji(input.codePointAt(0))) {       //check if the input is empty or less than two characters or an emoji
                    Toast.makeText(this, "Please enter a word",
                        Toast.LENGTH_SHORT).show()
                } else {
                    val word = searchText.text.toString()                                           //get the word
                    countWords(word)                                                                //count the word and display the result
                    dialog.dismiss()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }else if(method=="emoji"){                                                                  //dialog for emoji
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select a emoji")
            searchText.hint = "Emoji"
            builder.setView(dialogView)
            builder.setPositiveButton("Confirm") { dialog, _ ->
                val input = searchText.text.toString()
                if (input.isEmpty()||!isEmoji(input.codePointAt(0))) {                        //check if the input is empty or not an emoji
                    Toast.makeText(this, "Please enter an emoji",
                        Toast.LENGTH_SHORT).show()
                } else {
                    val emoji = searchText.text.toString()                                          //get the emoji
                    countEmoji(emoji)                                                               //count the emoji and display the result
                    dialog.dismiss()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }else if(method=="all"){                                                                    //if the user wants to build the graph
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm your selection")
            searchText.visibility= View.INVISIBLE
            builder.setView(dialogView)
            builder.setPositiveButton("Confirm") { dialog, _ ->
                countMessages()                                                                     //count the messages
                buildInfoText()                                                                     //build the info text
                buildGraph()                                                                        //build the graph
                dialog.dismiss()
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            builder.create().show()
        }
    }
    private fun buildInfoText(){                                                                    //builds the info text
        var index = 1
        val stringBuilder = StringBuilder()
        for ((name, count) in messageCounts) {                                                      //iterates through the map
            if (index == messageCounts.size) {                                                      //if it is the last element we add a new line
                stringBuilder.append("Total number for $name: $count")
            } else stringBuilder.append("Total number for $name: $count\n")
            index++
        }
        val text = infoTextViewContent.text.toString()                                              //get the text from info text view
        infoTextViewContent.text = buildString {                                      //append the text to the info text view
            append(text)
            append(stringBuilder.toString())
        }
    }
    private fun buildGraph() {                                                                      //builds the graph
        messageCounts.clear()
        val firstMonth = uncutMessages.lines()[0].substring(3, 8)                                   //gets the first month (first message timestamp)
        val count = uncutMessages.lines().size                                                      //gets the amount of messages to know where the last one should be
        val lastMonth = uncutMessages.lines()[count-2].substring(3, 8)                              //gets the last month (last message timestamp)
        var date=""
        var month = firstMonth.substring(0, 2).toInt()                                              //turn first month to int
        var year = firstMonth.substring(3, 5).toInt()                                               //turn first year to int
        while (date != lastMonth) {                                                                 //algorithm to get the months
            if(month<10){
                date = "0$month.$year"
            }else{
                date = "$month.$year"
            }
            messageCounts[date] = 0                                                                 //initialize the map with the months with 0 messages
            if(month<12){
                month++
            }else{
                month=1
                year++
            }
        }
        val lines = uncutMessages.lines()
        for (line in lines) {
            if(line.length>8) {
                val month = line.substring(3, 8)
                messageCounts[month] = messageCounts[month]!! + 1                                   //count increments by 1 for each message for a month
            }
        }
        val months = Array(messageCounts.size) { "" }                                               // the labels that should be drawn on the XAxis are the months
        val entries = ArrayList<Entry>()
        var index = 0
        for ((key, value) in messageCounts) {                                                       // Convert the data to entries for the chart
            entries.add(Entry(index.toFloat(), value.toFloat()))
            months[index] = key                                                                     //add the month in MM:YY format to the array
            index++
        }

        val formatter: ValueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase): String {                       //to show the months instead of integers
                val index = value.toInt()
                return if (index >= 0 && index < months.size) {
                    months[index]
                } else {
                    ""                                                                              //or return a default value (does not really matter)
                }
            }
        }

        val dataSet = LineDataSet(entries, "Monthly # of messages")                           //create the data set
        val lineData = LineData(dataSet)                                                            //create the line data
        dataSet.color = ContextCompat.getColor(this, R.color.progressColor2)                //Set line color
        dataSet.setCircleColor(Color.RED)                                                           //Set circle color
        dataSet.circleRadius = 1f                                                                   //Set circle radius
        dataSet.valueTextColor = ContextCompat.getColor(this, R.color.font)
        dataSet.valueTextSize = 7f                                                                  //Set text size of points
        dataSet.lineWidth= 3f
        monthlyGraph.data = lineData                                                                //Set data
        monthlyGraph.xAxis.position = XAxis.XAxisPosition.BOTTOM                                    //Set x-axis position to bottom
        monthlyGraph.xAxis.textColor = ContextCompat.getColor(this, R.color.font)            //Set x-axis text color
        monthlyGraph.axisLeft.axisMinimum = 0f                                                      //Set y-axis minimum value to 0
        monthlyGraph.axisLeft.textColor = ContextCompat.getColor(this, R.color.font)        //Set y-axis text color
        monthlyGraph.axisRight.isEnabled = false                                                    //Disable right y-axis
        monthlyGraph.description.isEnabled = false                                                  //Disable description text
        monthlyGraph.xAxis.granularity = 1f                                                         //Set x-axis granularity to 1 -> steps of 1 month
        monthlyGraph.xAxis.valueFormatter= formatter                                                //uses the overwritten formatter to show the months instead of integers


        progressBar.visibility = View.INVISIBLE                                                     //turn off the progress bars
        progressBarBackground.visibility = View.INVISIBLE
        ratioMessages.visibility = View.INVISIBLE
        monthlyGraph.visibility = View.VISIBLE
        viewModel.graph = true                                                                      //save the graph state

        monthlyGraph.invalidate()                                                                   //Refresh the chart
    }
    private fun performFileSearch() {
        openDocumentLauncher.launch(arrayOf("text/plain"))                                          //Trigger the document picker
    }
    private fun buildDiagram(){                                                                     //builds the pie chart
        val ratio = StringBuilder()
        var one = 0                                                                                 //holds the amount of messages for the first person
        var first = true                                                                            //shows if the first person is selected
        var two = 0                                                                                 //holds the amount of messages for the second person
        val stringBuilder = StringBuilder()
        if(messageCounts.values.max()==0){                                                          //if all values are 0 there are no messages containing the search
            progressBar.visibility = View.INVISIBLE                                                 //we can hide the progress bars and show the result
            progressBarBackground.visibility = View.INVISIBLE
            ratioMessages.visibility = View.INVISIBLE
            stringBuilder.append("There were no messages containing your search")
        }else {                                                                                     //else we show the ratio and the charts
            ratioMessages.visibility = View.VISIBLE
            infoTextViewContent.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            progressBarBackground.visibility = View.VISIBLE
            var index = 1
            for ((name, count) in messageCounts) {                                                  //iterates through the map
                if(index==messageCounts.size) {                                                     //if it is the last element we add a new line
                    stringBuilder.append("Total number for $name: $count")
                }else stringBuilder.append("Total number for $name: $count\n")
                index++
                if (first) {                                                                        //if the first person is selected
                    one = count                                                                     //save the amount of messages for the first person
                    first = false                                                                   //make sure that the first person is not selected anymore
                    ratio.append("$count/")                                                         //add the amount of messages for the first person to the ratio
                } else{
                    two = count                                                                     //save the amount of messages for the second person
                    ratio.append("$count")                                                          //add the amount of messages for the second person to the ratio
                }
            }
        }
        ratioMessages.text = ratio.toString()
        if(messageCounts.size>2) {                                                                  //if there are more than 2 people we cannot show the diagram
            progressBar.visibility = View.INVISIBLE
            progressBarBackground.visibility = View.INVISIBLE
            ratioMessages.visibility = View.VISIBLE
            ratioMessages.text = getString(R.string.error_for_group_chats)                          //informs the user that we cannot show the diagram
        }
        val text = infoTextViewContent.text.toString()
        infoTextViewContent.text = buildString {
            append(text)
            append(stringBuilder.toString())
        }
        progressBar.progress= two.toFloat().div((two+one).toFloat()).times(100).toInt()       //sets the progress bars to the ratio
        monthlyGraph.visibility = View.INVISIBLE                                                    //hides the graph
        viewModel.graph = false
    }

    private fun readTextFromUri(uri: Uri) {                                                         //reads the text from the file
        val contentResolver = contentResolver
        messageCounts.clear()
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    uncutMessages.clear()
                    cutMessages.clear()
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.length>17&&line[0].isDigit()&& line[16] == '-') {                  //checks if the line has a timestamp
                            uncutMessages.append(line)                                              //adds the line to the uncut messages for the graph
                            uncutMessages.append('\n')
                            val trimmedLine = line.removeRange(0, 18)                               //removes the timestamp from the line
                            cutMessages.append(trimmedLine)                                         //adds the line to the cut messages for the charts
                            cutMessages.append('\n')
                            val nameEndIndex = trimmedLine.indexOf(':')
                            if (nameEndIndex != -1) {
                                val name = trimmedLine.substring(0, nameEndIndex)
                                messageCounts[name] = messageCounts.getOrDefault(name,
                                    0) + 1
                            }
                        }
                        line = reader.readLine()                                                    //reads the next line from the txt file
                    }
                }
                infoTextViewContent.text = getString(R.string.amount_of_messages)
                buildDiagram()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}