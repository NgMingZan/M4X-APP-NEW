package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(onLogin: (String, String) -> Unit) {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF07111F), Color(0xFF111936), Color(0xFF0F172A)))
        ), contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE1E293B)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = Color(0xFF06B6D4), shape = RoundedCornerShape(18.dp)) {
                    Text("M4X", Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(if (register) "Tạo tài khoản" else "Chào mừng trở lại", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Kho theme HyperOS & MIUI dành cho cộng đồng", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Spacer(Modifier.height(22.dp))
                if (register) {
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Tên hiển thị") }, leadingIcon = { Icon(Icons.Default.Person, null) }, singleLine = true)
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Mail, null) }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Mật khẩu") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, singleLine = true)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { onLogin(email.ifBlank { "user@m4x.vn" }, password) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                    shape = RoundedCornerShape(16.dp)
                ) { Text(if (register) "Đăng ký" else "Đăng nhập", fontWeight = FontWeight.Bold) }
                TextButton(onClick = { register = !register }) {
                    Text(if (register) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký", color = Color(0xFF38BDF8))
                }
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(Modifier.height(12.dp))
                Text("Bản giai đoạn 1 • Dữ liệu mẫu cục bộ", color = Color(0xFF64748B), fontSize = 11.sp)
            }
        }
    }
}
