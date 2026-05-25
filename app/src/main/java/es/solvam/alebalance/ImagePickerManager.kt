import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ImagePickerManager(private val context: Context) {

    companion object {
        const val PICK_IMAGE_REQUEST_CODE = 1001
        private const val STORAGE_PERMISSION_CODE = 1
    }

    fun openImagePicker(activity: Activity) {
        if (isStoragePermissionGranted(activity)) {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            activity.startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        } else {
            requestStoragePermission(activity)
        }
    }

    fun handleImageSelection(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity): String? {
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri = data.data!!

            val imagePath = saveImageToInternalStorage(selectedImageUri)

            Toast.makeText(context, "Imagen guardada", Toast.LENGTH_SHORT).show()

            return imagePath
        }
        return null
    }

    private fun saveImageToInternalStorage(imageUri: Uri): String {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)

        val timeStamp = System.currentTimeMillis()
        val imageFile = File(context.filesDir, "imagen_$timeStamp.jpg")

        val outputStream = FileOutputStream(imageFile)
        inputStream?.copyTo(outputStream)

        return imageFile.absolutePath
    }

    fun isStoragePermissionGranted(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun openAppSettings(activity: Activity) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray, activity: Activity) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permiso concedido", Toast.LENGTH_SHORT).show()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(context, "Permiso denegado, habilítalo", Toast.LENGTH_SHORT).show()
                    openAppSettings(activity)
                }
            }
        }
    }
}