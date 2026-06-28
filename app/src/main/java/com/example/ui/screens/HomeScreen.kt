package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversionItem
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyItems by viewModel.historyItems.collectAsState()
    val isConverting by viewModel.isConverting.collectAsState()
    val progressValue by viewModel.progressValue.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Encoding choice state
    val encodingOptions = listOf(
        "ISO-8859-6" to "ISO-8859-6 (معيار البنوك القديم)",
        "WINDOWS-1256" to "Windows-1256 (العربية الافتراضية)",
        "UTF-8" to "UTF-8 (الترميز الموحد الحديث)"
    )
    var selectedEncodingIdx by remember { mutableStateOf(0) }
    var showEncodingMenu by remember { mutableStateOf(false) }

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Multi-file selection launcher
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            selectedUris = uris
        }
    }

    val selectedFilesInfo = remember(selectedUris) {
        selectedUris.map { uri ->
            uri to getFileNameFromUri(context, uri)
        }
    }

    // Design Colors Matching tailwind specs
    val bgPrimary = Color(0xFFF8FAFF) // Soft blue-slate background
    val accentBlue = Color(0xFF1C4587) // Theme Primary deep blue
    val slate800 = Color(0xFF1E293B) // Dark text
    val slate700 = Color(0xFF334155) // Slate 700 text
    val slate600 = Color(0xFF475569) // Subtitle text
    val slate500 = Color(0xFF64748B) // Subtle text
    val slate400 = Color(0xFF94A3B8) // Borders/Hinte
    val borderSlate = Color(0xFFF1F5F9) // Border color
    val emerald100 = Color(0xFFD1FAE5) // Badge green bg
    val emerald700 = Color(0xFF047857) // Badge green text
    val accentGreen = Color(0xFF4CAF50) // Status indicator green

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = {
                // Header mimicking HTML exactly
                Surface(
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title with Icon on Right (in RTL first element in Row represents right side)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(accentBlue, RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "شعار التطبيق",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "محول التقارير",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = slate800
                            )
                        }

                        // Left Action Icon Button (Settings)
                        IconButton(
                            onClick = { showEncodingMenu = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات",
                                tint = slate600
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(bgPrimary)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "النظام البنكي الذكي",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "تحويل التقارير النصية إلى Excel بدقة عالية",
                                fontSize = 13.sp,
                                color = slate500,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Upload Files Card Container (rounded-3xl = 24.dp)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Double outer border style via canvas or simply a light blue circle
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(88.dp)
                                        .background(Color(0xFFEFF6FF), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "تحميل الملفات",
                                        tint = accentBlue,
                                        modifier = Modifier.size(44.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (!isConverting) {
                                                fileLauncher.launch(arrayOf("*/*"))
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("select_files_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentBlue,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        enabled = !isConverting
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "📂 اختر الملفات من الجهاز",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    // Display list of selected files
                                    if (selectedFilesInfo.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "الملفات المحددة (${selectedFilesInfo.size}):",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = slate800,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            selectedFilesInfo.forEach { (uri, name) ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Description,
                                                            contentDescription = null,
                                                            tint = accentBlue,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Text(
                                                            text = name,
                                                            fontSize = 13.sp,
                                                            color = slate800,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            selectedUris = selectedUris.filter { it != uri }
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "إزالة",
                                                            tint = Color.Red,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Start conversion button
                                        Button(
                                            onClick = {
                                                if (!isConverting) {
                                                    viewModel.convertFiles(selectedUris, encodingOptions[selectedEncodingIdx].first)
                                                    selectedUris = emptyList()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .testTag("import_and_convert_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF10B981), // Emerald green
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            enabled = !isConverting
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "🚀 بدء عملية التحويل الآن",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "دعم ملفات ISO-8859-6",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = slate400
                                        )
                                        Text(
                                            text = "تنسيق RTL تلقائي",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = slate400
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Process Card & Progress Tracking
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "حالة العمليات",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = slate700
                                    )

                                    val statusText = if (isConverting) "جاري التحويل..." else "جاهز"
                                    val badgeBg = if (isConverting) Color(0xFFFEF3C7) else emerald100
                                    val badgeText = if (isConverting) Color(0xFFD97706) else emerald700

                                    Box(
                                        modifier = Modifier
                                            .background(badgeBg, RoundedCornerShape(50.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeText
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { if (isConverting) progressValue else 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .testTag("conversion_progress_bar"),
                                        color = accentGreen,
                                        trackColor = borderSlate,
                                    )

                                    val progressPct = (progressValue * 105).toInt().coerceAtMost(100)
                                    val helpText = if (isConverting) {
                                        "جاري تنقية المسافات وتحويل الأسطر وتشكيل ملف Excel... $progressPct%"
                                    } else {
                                        "بانتظار تحديد الملفات لبدء عملية التحويل والفلترة..."
                                    }

                                    Text(
                                        text = helpText,
                                        fontSize = 12.sp,
                                        color = slate500,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Conversion Configurations Menu
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "تحديد الترميز",
                                            tint = accentBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "ترميز الملف النصي (Encoding)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = slate800
                                        )
                                    }
                                    Box {
                                        Button(
                                            onClick = { showEncodingMenu = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentBlue.copy(alpha = 0.08f),
                                                contentColor = accentBlue
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = encodingOptions[selectedEncodingIdx].first,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "خيارات",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = showEncodingMenu,
                                            onDismissRequest = { showEncodingMenu = false }
                                        ) {
                                            encodingOptions.forEachIndexed { idx, pair ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = pair.second,
                                                            fontSize = 13.sp,
                                                            textAlign = TextAlign.Right,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedEncodingIdx = idx
                                                        showEncodingMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "💡 نصيحة: إذا ظهرت الحروف العربية غير مفهومة أو مكسرة في ملف الاكسل، يرجى تغيير الترميز إلى ISO-8859-6 للملفات البنكية القديمة، أو UTF-8 للملفات الحديثة.",
                                    fontSize = 11.sp,
                                    color = slate500,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // History Section
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 السجلات والملفات المحولة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = slate800
                            )
                            if (historyItems.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearHistory() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = "مسح السجلات",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("مسح السجل", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Empty State or Conversion History list items
                    if (historyItems.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = "السجل فارغ",
                                    tint = slate400,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "لا يوجد ملفات محولة حالياً",
                                    fontWeight = FontWeight.Bold,
                                    color = slate500,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "اضغط على زر الاستيراد بالأعلى لاختيار التقارير وبدء تحويلها.",
                                    color = slate500.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        items(historyItems, key = { it.id }) { item ->
                            HistoryCard(
                                item = item,
                                onOpen = { viewModel.openExcelFile(item) },
                                onShare = { viewModel.shareExcelFile(item) },
                                onSave = { viewModel.saveToDownloads(item) },
                                onDelete = { viewModel.deleteHistoryItem(item) }
                            )
                        }
                    }

                    // Footer exactly matching HTML styling
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "تصميم وتطوير : سامي القادري",
                                color = slate400,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "v2.0.1 • 777484160",
                                color = Color(0xFFCBD5E1),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // HTML dynamic style decorative bar
                            Box(
                                modifier = Modifier
                                    .size(width = 48.dp, height = 6.dp)
                                    .background(Color(0xFFE2E8F0), CircleShape)
                            )
                        }
                    }
                }

                // Error Dialog Alert logic
                if (errorMessage != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissError() },
                        title = {
                            Text(
                                text = "حدثت بعض الأخطاء",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        text = {
                            Box(modifier = Modifier.heightIn(max = 240.dp)) {
                                LazyColumn {
                                    item {
                                        Text(
                                            text = errorMessage ?: "",
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { viewModel.dismissError() }) {
                                Text("حسناً")
                            }
                        }
                    )
                }

                // Animated Success overlay at the top of screen
                AnimatedVisibility(
                    visible = statusMessage != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    statusMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = accentGreen),
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "تم",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = msg,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissStatus() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "اغلاق",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: ConversionItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(item.timestamp) {
        val date = Date(item.timestamp)
        SimpleDateFormat("yyyy/MM/dd | hh:mm a", Locale("ar")).format(date)
    }

    val isSuccess = item.status == "SUCCESS"
    val accentBlue = Color(0xFF1C4587)
    val slate800 = Color(0xFF1E293B)
    val slate500 = Color(0xFF64748B)
    val slate400 = Color(0xFF94A3B8)
    val borderSlate = Color(0xFFF1F5F9)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Status icon, name, date
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Decorative Badge Icon based on status
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (isSuccess) Color(0xFFEFF6FF) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.Description else Icons.Default.ErrorOutline,
                        contentDescription = "نوع الملف",
                        tint = if (isSuccess) accentBlue else Color(0xFFF44336),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSuccess) item.convertedFileName else item.originalFileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "الملف الأصلي: ${item.originalFileName}",
                        fontSize = 11.sp,
                        color = slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateText,
                        fontSize = 10.sp,
                        color = slate400
                    )
                }

                if (isSuccess && item.fileSize > 0L) {
                    val sizeKb = remember(item.fileSize) {
                        "${String.format(Locale.US, "%.1f", item.fileSize / 1024.0)} KB"
                    }
                    Text(
                        text = sizeKb,
                        fontSize = 11.sp,
                        color = accentBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (!isSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                // Error Details banner
                Text(
                    text = "سبب الفشل: ${item.errorMessage ?: "خطأ غير معروف"}",
                    color = Color(0xFFD32F2F),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = borderSlate, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSuccess) {
                    // Open Button
                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEFF6FF),
                            contentColor = accentBlue
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = "عرض", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فتح", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Share Button
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Save Button
                    Button(
                        onClick = onSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFF3E0),
                            contentColor = Color(0xFFEF6C00)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FileDownload, contentDescription = "حفظ للجهاز", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ للجهاز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // If failed, let the empty space align cleanly
                    Spacer(modifier = Modifier.weight(3f))
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFFFFF0F0), RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "ملف غير معروف"
}
