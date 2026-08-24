package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.utils.QrCodeAnalyzer
import com.example.utils.QrCodeUtils
import java.util.concurrent.Executors
import com.example.ImmersiveDialogEffect

@Composable
fun QrCodeDisplayDialog(
    deviceId: String,
    sharedSecret: String,
    onDismiss: () -> Unit
) {
    val qrPayload = "$deviceId|$sharedSecret"
    val qrBitmap = remember(qrPayload) { QrCodeUtils.generateQrCode(qrPayload) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ImmersiveDialogEffect()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF042416).copy(alpha = 0.95f),
                    border = BorderStroke(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = 380.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "رمز الربط السريع",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "امسح هذا الرمز من الجهاز الآخر للربط فوراً",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (qrBitmap != null) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .border(2.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(220.dp).padding(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "ID: $deviceId",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4AF37),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("إغلاق", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrScannerDialog(
    onQrScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ImmersiveDialogEffect()
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    val previewView = remember { PreviewView(context) }
                    
                    DisposableEffect(lifecycleOwner) {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        var cameraProvider: ProcessCameraProvider? = null
                        
                        cameraProviderFuture.addListener({
                            try {
                                cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build()
                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                    .build()
                                preview.surfaceProvider = previewView.surfaceProvider
                                
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                imageAnalysis.setAnalyzer(
                                    ContextCompat.getMainExecutor(context),
                                    QrCodeAnalyzer { result ->
                                        onQrScanned(result)
                                    }
                                )
                                
                                cameraProvider?.unbindAll()
                                cameraProvider?.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(context))
                        
                        onDispose {
                            try {
                                cameraProvider?.unbindAll()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Laser scanning vertical animation variables
                    val infiniteTransition = rememberInfiniteTransition(label = "laser_animation")
                    val laserY by infiniteTransition.animateFloat(
                        initialValue = 0.05f,
                        targetValue = 0.95f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laser_y"
                    )

                    // Scanner Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        // Central scanning area
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .align(Alignment.Center)
                                .background(Color.Transparent)
                        ) {
                            // Beautiful target box with Gold styling
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFD4AF37)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Animated Laser scanning line
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .align(Alignment.TopCenter)
                                            .offset(y = 260.dp * laserY)
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xFFD4AF37).copy(alpha = 0.1f),
                                                        Color(0xFFD4AF37),
                                                        Color(0xFFD4AF37).copy(alpha = 0.1f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "يجب تفعيل صلاحية الكاميرا للمسح",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Close Controls
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                
                // Beautiful translucent guidance instruction pill at the bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "وجه الكاميرا نحو رمز QR لربط الجهاز الآخر تلقائياً",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        }
    }
}
