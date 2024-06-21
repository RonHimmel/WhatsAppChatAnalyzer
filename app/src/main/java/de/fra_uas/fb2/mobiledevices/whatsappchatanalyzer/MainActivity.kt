package de.fra_uas.fb2.mobiledevices.whatsappchatanalyzer

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var textViewFileContent: TextView
    private val messageCounts = mutableMapOf<String, Int>()
    private val allMessages = StringBuilder()
    private lateinit var ratioMessages: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarBackground: ProgressBar

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

        textViewFileContent.text = getString(R.string.info_text)

        openDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                readTextFromUri(uri)
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
            }
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
                if(c==char)
                    if (nameEndIndex != -1) {
                        val name = line.substring(0, nameEndIndex)
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
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
                if(words==word)
                    if (nameEndIndex != -1) {
                        val name = line.substring(0, nameEndIndex)
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                        Log.d("line", line)
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
                if(word.contains(emoji))
                    if (nameEndIndex != -1) {
                        val name = line.substring(0, nameEndIndex)
                        messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                        Log.d("line", line)
                    }
            }
        }
        textViewFileContent.text ="You searched for '$emoji':\n"
        buildDiagram()
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
                if (input.isEmpty()) {
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
        }
    }

    fun openFileButton(view: View) {
        performFileSearch()
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
        if(messageCounts.isEmpty()){
            progressBar.visibility = View.INVISIBLE
            progressBarBackground.visibility = View.INVISIBLE
            ratioMessages.visibility = View.INVISIBLE
            stringBuilder.append("Nothing found")
        }else {
            for ((name, count) in messageCounts) {
                stringBuilder.append("Total number for $name: $count \n")
                if (one == 0) {
                    one = count
                    ratio.append("$count/")
                } else if (two == 0) {
                    two = count
                    ratio.append("$count")
                    textViewFileContent.visibility = View.VISIBLE
                    progressBar.visibility = View.VISIBLE
                    progressBarBackground.visibility = View.VISIBLE
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
    }

    private fun readTextFromUri(uri: Uri) {
        val contentResolver = contentResolver
        messageCounts.clear()
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    allMessages.clear()
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.length>17&&line[0].isDigit()&& line[16] == '-') {
                            val trimmedLine = line.removeRange(0, 17)
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
                textViewFileContent.text =""
                buildDiagram()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
