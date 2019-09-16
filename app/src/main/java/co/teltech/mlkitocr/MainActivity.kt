package co.teltech.mlkitocr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_TAKE_PICTURE_ID = 32

    private var currentImageFilePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView.setImageBitmap(null)
        imageView.visibility = View.GONE

        textView.visibility = View.VISIBLE
        recognizedTextView.visibility = View.GONE
        recognizedTextView.movementMethod = ScrollingMovementMethod()

        buttonTakePhoto.setOnClickListener {
            takePhoto()
        }

        buttonOcr.setOnClickListener {
            ocrPhoto()
        }

        buttonOcr.isEnabled = false
    }

    fun takePhoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            intent.resolveActivity(packageManager)?.also {
                val photoFile = createImageFile()
                photoFile?.let {
                    val photoUri = FileProvider.getUriForFile(this, "co.teltech.mlkitocr.android.fileprovider", it)
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(intent, REQUEST_TAKE_PICTURE_ID)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAKE_PICTURE_ID && resultCode == Activity.RESULT_OK) {
            showTakenImage()
        }
    }

    private fun showTakenImage() {
        val exifInterface = ExifInterface(currentImageFilePath)
        val photoOrientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val image = BitmapFactory.decodeFile(currentImageFilePath)

        if (image != null) {
            recognizedTextView.visibility = View.GONE
            recognizedTextView.text = ""

            var matrix = Matrix()
            when(photoOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            val rotatedImage = Bitmap.createBitmap(image, 0,0, image.width, image.height, matrix, true)

            textView.visibility = View.GONE
            imageView.setImageBitmap(rotatedImage)
            imageView.visibility = View.VISIBLE

            buttonOcr.isEnabled = true
        } else {
            textView.visibility = View.VISIBLE
            imageView.visibility = View.GONE

            buttonOcr.isEnabled = false
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("Photo_$timeStamp", ".jpg", storageDirectory).apply {
            currentImageFilePath = absolutePath
        }
    }

    fun ocrPhoto() {
        val image = FirebaseVisionImage.fromBitmap((imageView.drawable as BitmapDrawable).bitmap)
        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

        val result = detector.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->
                // Task completed successfully
                // ...
                Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()

                runOnUiThread {
                    recognizedTextView.text = firebaseVisionText.text
                    recognizedTextView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                // Task failed with an exception
                // ...
                Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
            }
    }
}
