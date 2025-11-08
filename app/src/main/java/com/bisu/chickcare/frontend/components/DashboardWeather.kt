package com.bisu.chickcare.frontend.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisu.chickcare.backend.viewmodels.WeatherViewModel
import com.bisu.chickcare.frontend.utils.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@Composable
fun WeatherUpdateCard() {
    val weatherViewModel: WeatherViewModel = viewModel()
    val weatherState by weatherViewModel.weatherState.collectAsState()
    val useCelsius by weatherViewModel.useCelsius.collectAsState()
    val hourly by weatherViewModel.hourly.collectAsState()
    val context = LocalContext.current
    val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[fineLocationPermission] == true || permissions[coarseLocationPermission] == true
        if (granted) {
            weatherViewModel.fetchWeather(useCelsius = useCelsius)
        } else {
            weatherViewModel.fetchWeather(useCelsius = useCelsius)
        }
        permissionDenied = !granted
    }
    
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, coarseLocationPermission) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            weatherViewModel.fetchWeather(useCelsius = useCelsius)
            permissionDenied = false
        } else {
            permissionLauncher.launch(arrayOf(fineLocationPermission, coarseLocationPermission))
        }
    }
    
    if (weatherState.error != null && !weatherState.isLoading) {
        return
    }
    
    val hasValidData = weatherState.error == null && !weatherState.isLoading && weatherState.currentTemp > 0
    
    val currentTemp = if (hasValidData) {
        weatherState.currentTemp.toInt()
    } else {
        25
    }
    val humidity = if (hasValidData) weatherState.humidity else 60
    val windSpeed = if (hasValidData) weatherState.windSpeed.toInt() else 10
    val weatherCondition = if (hasValidData) weatherState.weatherCondition else "Clear sky"
    val feelsLike = if (hasValidData) {
        weatherState.feelsLike.toInt()
    } else {
        currentTemp
    }
    val location = if (hasValidData && weatherState.location.isNotEmpty()) {
        weatherState.location
    } else {
        ""
    }
    val uvIndex = 7
    
    val hotThreshold = if (useCelsius) 30 else 86
    val warmThreshold = if (useCelsius) 20 else 68

    val tempColor = when {
        currentTemp > hotThreshold -> Color(0xFFFF6B35)
        currentTemp > warmThreshold -> Color(0xFF4CAF50)
        else -> Color(0xFF2196F3)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White.copy(alpha = 0.9f))),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Weather Update",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (location.isNotEmpty() && location != "Loading...") {
                        Text(
                            location,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { weatherViewModel.fetchWeather(useCelsius = useCelsius) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh weather",
                            tint = Color(0xFF000000)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F1F1))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedBg = ThemeColorUtils.white()
                        val unselectedBg = Color(0xFFF1F1F1)
                        val selectedColor = Color(0xFF000000)
                        val unselectedColor = ThemeColorUtils.lightGray(Color(0xFF666666))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (useCelsius) selectedBg else unselectedBg)
                                .clickable {
                                    if (!useCelsius) {
                                        weatherViewModel.setUseCelsius(true)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("°C", color = if (useCelsius) selectedColor else unselectedColor, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (!useCelsius) selectedBg else unselectedBg)
                                .clickable {
                                    if (useCelsius) {
                                        weatherViewModel.setUseCelsius(false)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("°F", color = if (!useCelsius) selectedColor else unselectedColor, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            if (weatherState.isLoading) {
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE1C1A0),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Loading weather...",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
            } else {
                if (permissionDenied) {
                    Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Location is off. Using default city.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Use location",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { permissionLauncher.launch(arrayOf(fineLocationPermission, coarseLocationPermission)) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                "$currentTemp",
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = 56.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = tempColor
                            )
                            Text(
                                if (useCelsius) "°C" else "°F",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Text(
                            weatherCondition,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ThemeColorUtils.lightGray(Color.Gray),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WeatherInfoItem(
                        icon = "💧",
                        label = "Humidity",
                        value = "$humidity%",
                        modifier = Modifier
                    )
                    WeatherInfoItem(
                        icon = "💨",
                        label = "Wind",
                        value = if (useCelsius) "$windSpeed km/h" else "$windSpeed mph",
                        modifier = Modifier
                    )
                    WeatherInfoItem(
                        icon = "☀️",
                        label = "UV Index",
                        value = "$uvIndex",
                        modifier = Modifier
                    )
                    WeatherInfoItem(
                        icon = "🌡️",
                        label = "Feels like",
                        value = if (useCelsius) "$feelsLike°C" else "$feelsLike°F",
                        modifier = Modifier
                    )
                }
                
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

                if (hourly.isNotEmpty()) {
                    Text(
                        "Next hours",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemeColorUtils.lightGray(Color.Gray),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(hourly.size) { index ->
                            val item = hourly[index]
                            val hour = SimpleDateFormat("h a", Locale.getDefault()).format(Date(item.timestamp * 1000))
                            val t = item.main.temp.roundToInt()
                            val pop = (item.precipitationProb * 100).roundToInt()
                            val condition = item.weather.firstOrNull()?.main ?: ""
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(hour, style = MaterialTheme.typography.labelSmall, color = ThemeColorUtils.lightGray(Color.Gray))
                                Text("$t°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(condition, style = MaterialTheme.typography.labelSmall, color = ThemeColorUtils.lightGray(Color.Gray))
                                Text("$pop%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.WeatherInfoItem(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.weight(1f)
    ) {
        Text(
            icon,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = ThemeColorUtils.lightGray(Color.Gray)
        )
    }
}
