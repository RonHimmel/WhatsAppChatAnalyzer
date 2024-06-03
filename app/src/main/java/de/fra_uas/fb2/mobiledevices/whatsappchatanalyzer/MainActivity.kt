package de.fra_uas.fb2.mobiledevices.whatsappchatanalyzer

import android.net.Uri
import android.os.Bundle
import android.widget.Button
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

        openDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                readTextFromUri(uri)
            } else {
                Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
            }
        }

        val buttonOpenFile: Button = findViewById(R.id.button)
        buttonOpenFile.setOnClickListener {
            performFileSearch()
        }
    }

    private fun performFileSearch() {
        // Trigger the document picker
        openDocumentLauncher.launch(arrayOf("text/plain"))
    }

    private fun readTextFromUri(uri: Uri) {
        val contentResolver = contentResolver
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        stringBuilder.append('\n')
                        line = reader.readLine()
                    }
                    // Display the text in the TextView
                    textViewFileContent.text = stringBuilder.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}