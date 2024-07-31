package com.example.credential

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.credential.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PortCredentialScreen()
                }
            }
        }
    }
}

@Composable
fun PortCredentialScreen() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageLoadError by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    var showButtons by remember { mutableStateOf(false) }


    //Test if the image exist on the path
    LaunchedEffect(Unit) {
        val savedUri = getSavedImageUri(context)
        if (savedUri != null) {
            try {
                // Attempt to load the image
                context.contentResolver.openInputStream(savedUri)?.close()
                imageUri = savedUri
            } catch (e: Exception) {
                // If loading fails, clear the saved path
                saveImagePath(context, null)
                imageLoadError = true
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempImageUri?.let {
                val copiedUri = copyImageToPrivateStorage(context, it)
                imageUri = copiedUri
                saveImagePath(context, copiedUri)
            }
        }
    }

    // Function to create a file for the camera image
    fun createImageFile(context: Context): Uri {
        val imageName = "camera_photo_${System.currentTimeMillis()}.jpg"
        val storageDir = context.getExternalFilesDir(null)
        val imageFile = File(storageDir, imageName)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedUri = copyImageToPrivateStorage(context, it)
            imageUri = copiedUri
            saveImagePath(context, copiedUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Camera permission granted, you can launch camera intent here
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    tempImageUri = createImageFile(context)
                    cameraLauncher.launch(tempImageUri!!)
                }
            }
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Gallery permission granted, you can launch gallery intent here
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_IMAGES
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            galleryLauncher.launch("image/*")
                        }
                    }
                }
                else -> {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            galleryLauncher.launch("image/*")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box( // Use a Box to control image stretching
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Image takes up remaining space
        ) {
            if (imageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Selected image",
                    modifier = Modifier.fillMaxSize(), // Fill the Box
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp)
                ) {
                    Text("No image selected", Modifier.align(Alignment.Center))
                }
            }
        }

        if(showButtons){
            Button(onClick = {
                showButtons = false
            }){
                Text("X")
            }

            Button(onClick = {
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        tempImageUri = createImageFile(context)
                        cameraLauncher.launch(tempImageUri!!)
                        showButtons = false
                    }
                    else -> {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }) {
                Text("Take Photo")
            }

            Button(onClick = {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_MEDIA_IMAGES
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                galleryLauncher.launch("image/*")
                                showButtons = false
                            }
                            else -> {
                                galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                            }
                        }
                    }
                    else -> {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                galleryLauncher.launch("image/*")
                            }
                            else -> {
                                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    }
                }
            }) {
                Text("Upload Photo")
            }

        }else{
            Button(onClick = {
                showButtons = true  // Show "Take Photo" and "Upload Photo" buttons
            }) {
                Text("Change Photo")
            }
        }

    }
}

private fun getSavedImageUri(context: Context): Uri? {
    val sharedPref = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
    val imagePath = sharedPref.getString("saved_image_path", null)
    return if (imagePath != null) {
        val file = File(imagePath)
        if (file.exists()) Uri.fromFile(file) else null
    } else null
}

private fun copyImageToPrivateStorage(context: Context, uri: Uri): Uri? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val fileName = "saved_image_${System.currentTimeMillis()}.jpg"
    val outputFile = File(context.filesDir, fileName)

    inputStream.use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    return Uri.fromFile(outputFile)
}

private fun saveImagePath(context: Context, uri: Uri?) {
    val sharedPref = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
    with(sharedPref.edit()) {
        putString("saved_image_path", uri?.path)
        apply()
    }
}