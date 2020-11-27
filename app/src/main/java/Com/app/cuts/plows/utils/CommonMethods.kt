package Com.app.cuts.plows.utils

import android.content.Context
import android.content.CursorLoader
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Patterns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CommonMethods {
    companion object {
        fun isValidEmail(target: CharSequence): Boolean {
            return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }

        fun getRealPathFromURI(contentUri: Uri, context: Context): String? {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val loader = CursorLoader(context, contentUri, proj, null, null, null)
            val cursor: Cursor = loader.loadInBackground()
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val result = cursor.getString(column_index)
            cursor.close()
            return result
        }

        fun compressImage(inContext: Context, inImage: Bitmap): Uri {
            val bytes = ByteArrayOutputStream()
            inImage.compress(Bitmap.CompressFormat.JPEG, 70, bytes)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val path = MediaStore.Images.Media.insertImage(
                inContext.contentResolver,
                inImage,
                File.separator + "IMG_" + timeStamp + ".jpg",
                null
            )
            return Uri.parse(path)
        }

        fun getUriToBitmapImage(imageUri: Uri?, context: Context): Bitmap? {
            var bitmapImage: Bitmap? = null
            try {
                bitmapImage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(
                        context.contentResolver,
                        imageUri!!
                    )
                    ImageDecoder.decodeBitmap(source)
                } else MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return bitmapImage
        }
    }


}