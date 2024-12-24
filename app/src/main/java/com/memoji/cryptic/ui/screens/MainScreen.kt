package com.memoji.cryptic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.memoji.cryptic.utils.PasswordCracker
import com.memoji.cryptic.utils.SystemMonitor
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val systemMonitor = remember { SystemMonitor(context) }
    var cpuUsage by remember { mutableStateOf(0f) }
    var totalCpuUsage by remember { mutableStateOf(0f) }
    var ramUsage by remember { mutableStateOf(0f) }
    var ramInfo by remember { mutableStateOf("0 MB / 0 MB") }
    var isCalculating by remember { mutableStateOf(false) }
    var currentAttempt by remember { mutableStateOf(0L) }
    var attemptsPerSecond by remember { mutableStateOf(0L) }
    var lastAttemptTime by remember { mutableStateOf(0L) }
    var currentResult by remember { mutableStateOf<PasswordCracker.CrackResult?>(null) }
    
    // Cracking configuration
    val totalNodes = 2 // Fixed to 2 instances
    var selectedNode by remember { mutableStateOf<Int?>(null) }
    var searchRange by remember { mutableStateOf<PasswordCracker.SearchRange?>(null) }
    var password by remember { mutableStateOf("") }
    var targetHash by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    var calculationJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            cpuUsage = systemMonitor.getCpuUsage()
            totalCpuUsage = systemMonitor.getTotalCpuUsage()
            ramUsage = systemMonitor.getRamUsage()
            ramInfo = systemMonitor.getTotalRamInfo()
            delay(500.milliseconds)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Cryptico",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Password Configuration",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            targetHash = PasswordCracker.generateMD5(it)
                        },
                        label = { Text("Password to Crack") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCalculating
                    )
                    if (targetHash.isNotEmpty()) {
                        Text(
                            text = "MD5 Hash: $targetHash",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        text = "Select Instance",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = selectedNode == 0,
                            onClick = { 
                                if (!isCalculating) selectedNode = 0 
                            },
                            label = { Text("Instance 1") },
                            enabled = !isCalculating
                        )
                        FilterChip(
                            selected = selectedNode == 1,
                            onClick = { 
                                if (!isCalculating) selectedNode = 1 
                            },
                            label = { Text("Instance 2") },
                            enabled = !isCalculating
                        )
                    }
                    
                    searchRange?.let { range ->
                        Text(
                            text = "Search Range: ${range.start} to ${range.end}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // System Monitoring Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CPU Usage",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(
                        progress = cpuUsage / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${cpuUsage.toInt()}% (App)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Total: ${totalCpuUsage.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RAM Usage",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LinearProgressIndicator(
                        progress = ramUsage,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(ramUsage * 100).toInt()}% (App)",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = ramInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            if (isCalculating || currentResult?.found == true) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (currentResult?.found == true) "Password Found!" else "Cracking Password",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (currentResult?.found == true) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        
                        Text(
                            text = "Target Hash: $targetHash",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        currentResult?.let { result ->
                            Text(
                                text = "Current attempt: ${result.password}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Attempts: $currentAttempt ($attemptsPerSecond/s)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            if (result.found) {
                                Text(
                                    text = "Found password: ${result.password}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (isCalculating) {
                        calculationJob?.cancel()
                        isCalculating = false
                    } else if (selectedNode != null && targetHash.isNotEmpty()) {
                        try {
                            searchRange = PasswordCracker.calculateRange(totalNodes, selectedNode!!, password.length)
                            currentAttempt = searchRange?.start ?: 0
                            attemptsPerSecond = 0
                            lastAttemptTime = System.currentTimeMillis()
                            
                            calculationJob = scope.launch(Dispatchers.Default) {
                                isCalculating = true
                                val startTime = System.currentTimeMillis()
                                val endAttempt = searchRange?.end ?: 0
                                
                                while (isActive && currentAttempt <= endAttempt) {
                                    val result = PasswordCracker.attemptCrack(targetHash, currentAttempt, password.length)
                                    currentResult = result
                                    
                                    if (result.found) {
                                        break
                                    }
                                    
                                    currentAttempt++
                                    
                                    val now = System.currentTimeMillis()
                                    if (now - lastAttemptTime >= 1000) {
                                        attemptsPerSecond = currentAttempt * 1000 / (now - startTime)
                                        lastAttemptTime = now
                                    }
                                }
                                
                                if (!currentResult?.found!! && currentAttempt > endAttempt) {
                                    currentResult = currentResult?.copy(
                                        password = "Not found in range ${searchRange?.start} to ${searchRange?.end}"
                                    )
                                }
                            }
                        } catch (e: NumberFormatException) {
                            // Handle invalid input
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCalculating) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                ),
                enabled = isCalculating || (selectedNode != null && targetHash.isNotEmpty())
            ) {
                Text(
                    text = if (isCalculating) "Stop Cracking" else "Start Cracking",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
} 