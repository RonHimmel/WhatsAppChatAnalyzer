package de.fra_uas.fb2.mobiledevices.whatsappchatanalyzer

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var openDocumentLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var textViewFileContent: TextView
    private val messageCounts = mutableMapOf<String, Int>()
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

        textViewFileContent.text = "1. Export a Chat in Whatsapp\n\n2. Open it in this app\n\n3. " +
                "Choose which " +
                "parameters to analyze"

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
        if(progressBar.visibility == View.INVISIBLE) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }
    }
    fun emojiButton(view: View) {
        if(progressBar.visibility == View.INVISIBLE) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }
    }
    fun wordButton(view: View) {
        if(progressBar.visibility == View.INVISIBLE) {
            Toast.makeText(this, "No file selected yet", Toast.LENGTH_SHORT).show()
        }
    }
    fun startButton(view: View) {
        if(progressBar.visibility == View.INVISIBLE) {
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

    private fun readTextFromUri(uri: Uri) {
        val contentResolver = contentResolver
        messageCounts.clear()
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line = reader.readLine()
                    while (line != null) {
                        if (line.length>17&&line[0].isDigit()&& line[16] == '-') {
                            val trimmedLine = line.removeRange(0, 17)
                            stringBuilder.append(trimmedLine)
                            stringBuilder.append('\n')
                            val nameEndIndex = trimmedLine.indexOf(':')
                            if (nameEndIndex != -1) {
                                val name = trimmedLine.substring(0, nameEndIndex)
                                messageCounts[name] = messageCounts.getOrDefault(name, 0) + 1
                                stringBuilder.append("Message count for $name: ${messageCounts[name]}")
                                stringBuilder.append('\n')
                            }
                        }
                        line = reader.readLine()
                    }
                }
                val ratio = StringBuilder()
                var one = 0
                var two = 0
                val stringBuilder = StringBuilder()
                for ((name, count) in messageCounts) {
                    stringBuilder.append("Message count for $name: $count \n")

                    if(one==0) {
                        one = count
                        ratio.append("$count/")
                    }else if(two==0){
                        two = count
                        ratio.append("$count")
                        textViewFileContent.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE
                        progressBarBackground.visibility = View.VISIBLE
                    }else if(two!=0){
                        progressBar.visibility = View.INVISIBLE
                        progressBarBackground.visibility = View.INVISIBLE
                        ratioMessages.visibility = View.INVISIBLE
                    }
                }
                textViewFileContent.text = stringBuilder.toString()
                ratioMessages.text = ratio.toString()
                progressBar.progress= two.toFloat().div((two+one).toFloat()).times(100).toInt()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}