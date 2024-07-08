package de.fra_uas.fb2.mobiledevices.whatsappchatanalyzer

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var messageCounts = mutableMapOf<String, Int>()
    var cutMessages = StringBuilder()
    var infoTextViewContent: String = ""                                                                //stores the text from main text view
    var uncutMessages = StringBuilder()                                                             //stores all messages
    var graph = false                                                                               //checks if graph should be drawn
}