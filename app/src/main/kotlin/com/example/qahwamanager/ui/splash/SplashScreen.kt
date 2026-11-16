package com.example.qahwamanager.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.common.QahwaBrandDots
import com.example.qahwamanager.ui.common.QahwaBrandEmblem
import com.example.qahwamanager.ui.theme.BrandAmberGold
import com.example.qahwamanager.ui.theme.BrandCoffeeBrown
import com.example.qahwamanager.ui.theme.BrandCream
import com.example.qahwamanager.ui.theme.BrandForestDeep
import com.example.qahwamanager.ui.theme.QahwaTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
) {
    val qColors = QahwaTheme.colors
    var started by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0.15f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "splashProgress",
    )

    LaunchedEffect(Unit) {
        started = true
        delay(1150.milliseconds)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = qColors.splashBackgroundBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashFinished,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // Emblem: Photo 1 style in Light mode, Photo 2 concentric rings in Dark mode
            QahwaBrandEmblem(
                size = if (qColors.isDark) 210.dp else 168.dp,
                concentricRingStyle = qColors.isDark,
            )

            Spacer(modifier = Modifier.height(if (qColors.isDark) 12.dp else 28.dp))

            // Brand Title: "Qahwa manager"
            Text(
                text = "Qahwa manager",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = if (qColors.isDark) BrandCream else BrandForestDeep,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Brand Subtitle: "YOUR CAFÉ, PERFECTLY RUN"
            Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.2.sp,
                ),
                color = if (qColors.isDark) BrandAmberGold else BrandCoffeeBrown,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(44.dp))

            if (qColors.isDark) {
                // Photo 2 bottom progress indicator + "Brewing your dashboard..."
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BrandCream.copy(alpha = 0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BrandAmberGold),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.brewing_dashboard),
                    style = MaterialTheme.typography.bodySmall,
                    color = qColors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            } else {
                // Photo 1 three signature palette dots
                QahwaBrandDots(dotSize = 14.dp, spacing = 14.dp)
            }
        }
    }
}
