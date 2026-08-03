package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadThemeScreen(
    viewModel: MainViewModel
) {
    val uploadTitle by viewModel.uploadTitle.collectAsState()
    val uploadOs by viewModel.uploadOs.collectAsState()
    val uploadCategory by viewModel.uploadCategory.collectAsState()
    val uploadDescription by viewModel.uploadDescription.collectAsState()
    val uploadTags by viewModel.uploadTags.collectAsState()
    val uploadFileName by viewModel.uploadFileName.collectAsState()
    val uploadFileSizeMb by viewModel.uploadFileSizeMb.collectAsState()
    val validationResult by viewModel.uploadValidationResult.collectAsState()

    val osOptions = listOf("HyperOS 2.0", "HyperOS 1.0", "MIUI 14", "MIUI 13")
    val categoryOptions = listOf("Cyberpunk", "iOS Style", "Minimal", "Dark Mode", "Anime", "Dynamic", "Abstract")

    var expandedOs by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Tải lên Theme (.mtz / .zip)", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Dành cho Creator: Tải lên & kiểm tra cấu trúc file tự động", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Title
            item {
                OutlinedTextField(
                    value = uploadTitle,
                    onValueChange = { viewModel.uploadTitle.value = it },
                    label = { Text("Tên Theme (Ví dụ: CyberGlass HyperOS)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("upload_title_field")
                )
            }

            // OS Compatibility Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = expandedOs,
                    onExpandedChange = { expandedOs = !expandedOs }
                ) {
                    OutlinedTextField(
                        value = uploadOs,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tương thích OS") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOs) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("upload_os_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = expandedOs,
                        onDismissRequest = { expandedOs = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        osOptions.forEach { os ->
                            DropdownMenuItem(
                                text = { Text(os, color = Color.White) },
                                onClick = {
                                    viewModel.uploadOs.value = os
                                    expandedOs = false
                                }
                            )
                        }
                    }
                }
            }

            // Category Dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = uploadCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Danh mục Theme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFA855F7),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("upload_cat_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        categoryOptions.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    viewModel.uploadCategory.value = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
            }

            // Description
            item {
                OutlinedTextField(
                    value = uploadDescription,
                    onValueChange = { viewModel.uploadDescription.value = it },
                    label = { Text("Mô tả chi tiết Theme & tính năng chính") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("upload_desc_field")
                )
            }

            // Tags
            item {
                OutlinedTextField(
                    value = uploadTags,
                    onValueChange = { viewModel.uploadTags.value = it },
                    label = { Text("Thẻ (Phân cách bằng dấu phẩy, ví dụ: HyperOS,Dark,3D)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("upload_tags_field")
                )
            }

            // MTZ File Details & Automated Validation Tool Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FolderZip, contentDescription = null, tint = Color(0xFF06B6D4))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kiểm tra File .MTZ Tự động", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = { viewModel.testValidateMtz() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("run_validation_button")
                            ) {
                                Text("Kiểm tra ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uploadFileName,
                            onValueChange = { viewModel.uploadFileName.value = it },
                            label = { Text("Tên File (.mtz / .zip)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF06B6D4),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Validation Result Output Log
                        if (validationResult != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val res = validationResult!!
                            Surface(
                                color = if (res.isValid) Color(0xFF0F291E) else Color(0xFF2D1215),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (res.isValid) Color(0xFF059669) else Color(0xFFDC2626)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (res.isValid) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (res.isValid) Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (res.isValid) "Xác thực cấu trúc .MTZ hợp lệ!" else "Phát hiện lỗi cấu trúc .MTZ!",
                                            color = if (res.isValid) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(res.log, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Submit Upload Button
        Button(
            onClick = { viewModel.submitUploadTheme() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_upload_button")
        ) {
            Icon(Icons.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tải lên & Gửi duyệt Admin", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
