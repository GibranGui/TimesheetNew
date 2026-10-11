package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ui.theme.MiningAmber
import com.example.ui.theme.MiningAccentBlue
import com.example.ui.theme.MiningGreenActive

private data class OnboardPage(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val body: String
)

/**
 * Tutorial singkat untuk pengguna baru: 4 halaman geser yang menjelaskan
 * cara pakai aplikasi dengan bahasa sederhana.
 */
@Composable
fun OnboardingScreen(onSkip: () -> Unit, onFinished: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardPage(
                Icons.Default.LocalShipping, MiningAmber,
                "Catat Shift Harian",
                "Tekan \"Mulai Shift\" di awal kerja, isi unit dan jam mesin. " +
                    "Saat selesai kerja, tekan \"Akhiri Shift\". Semua tercatat otomatis."
            ),
            OnboardPage(
                Icons.Default.GpsFixed, MiningGreenActive,
                "Ritase Terhitung Otomatis",
                "HP cukup ditaruh di kabin. Aplikasi menghitung ritase sendiri lewat GPS " +
                    "saat dump truck keluar area muatan. Pastikan lokasi/GPS HP aktif agar hitungan akurat."
            ),
            OnboardPage(
                Icons.Default.CalendarMonth, MiningAccentBlue,
                "Jadwal Kerja & Rekapan",
                "Buka tab Jadwal untuk melihat jadwal kerja Anda (D/N/OFF), dan tab Rekapan " +
                    "untuk melihat total jam kerja, ritase, dan BBM per bulan."
            ),
            OnboardPage(
                Icons.Default.BatterySaver, MiningAmber,
                "Tetap Jalan Tanpa Kuota",
                "Aplikasi bisa dipakai sepenuhnya tanpa internet. Saat ada sinyal, catatan Anda " +
                    "otomatis tersimpan juga ke server pusat atas nama Anda — aman jika HP hilang atau rusak."
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    var lastPage by remember { mutableStateOf(false) }
    if (pagerState.currentPage == pages.size - 1) lastPage = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Selamat Datang!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Cara pakai aplikasi ini dalam 4 langkah singkat",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val item = pages[page]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(item.tint.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = item.tint,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        item.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        item.body,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Titik halaman
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(pages.size) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 10.dp else 7.dp)
                            .background(
                                if (i == pagerState.currentPage) MiningAmber
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinished()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MiningAmber),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (lastPage) "Mulai Menggunakan" else "Lanjut",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            if (!lastPage) {
                Text(
                    "Lewati tutorial",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { onSkip() }
                )
            }
        }
    }
}
