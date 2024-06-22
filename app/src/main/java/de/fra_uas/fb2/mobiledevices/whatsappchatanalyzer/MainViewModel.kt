package de.fra_uas.fb2.mobiledevices.whatsappchatanalyzer

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var messageCounts = mutableMapOf<String, Int>()
    var allMessages = StringBuilder()
    var textViewContent: String = ""
}