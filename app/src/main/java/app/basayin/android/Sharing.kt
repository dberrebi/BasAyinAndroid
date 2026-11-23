package app.basayin.android

import android.content.Context
import android.content.Intent

fun sharePlainText(context: Context, text: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
