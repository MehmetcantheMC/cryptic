package com.memoji.cryptic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
    
    // Job packet tracking
    var completedPackets by remember { mutableStateOf(0) }
    var currentPacket by remember { mutableStateOf<PasswordCracker.JobPacket?>(null) }
    var simulatedNextPacket by remember { mutableStateOf("0") }
    
    // Cracking configuration
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
                .verticalScroll(rememberScrollState())
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
                        text = "Simulation Controls",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = simulatedNextPacket,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                simulatedNextPacket = it
                            }
                        },
                        label = { Text("Next Job Packet Number") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCalculating
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

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
                        text = "Password Cracking Progress",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (isCalculating) {
                        currentPacket?.let { packet ->
                            Text(
                                text = "Current Job Packet: #${packet.packetNumber}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Testing password length: ${packet.passwordLength}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Range: ${packet.startPosition} - ${packet.endPosition}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "Completed Packets: $completedPackets",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Current password: ${currentResult?.password ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Attempts: $currentAttempt ($attemptsPerSecond/s)",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isCalculating) {
                            calculationJob?.cancel()
                            isCalculating = false
                        } else if (targetHash.isNotEmpty()) {
                            try {
                                // Get next job packet
                                val packetNumber = simulatedNextPacket.toIntOrNull() ?: 0
                                val jobPacket = PasswordCracker.getNextJobPacket(packetNumber)
                                
                                if (!jobPacket.isValid) {
                                    currentResult = PasswordCracker.CrackResult(
                                        password = "All possible lengths tried up to ${PasswordCracker.MAX_PASSWORD_LENGTH}",
                                        hash = "",
                                        attempt = 0,
                                        found = false
                                    )
                                    return@Button
                                }
                                
                                currentPacket = jobPacket
                                currentAttempt = jobPacket.startPosition
                                attemptsPerSecond = 0
                                lastAttemptTime = System.currentTimeMillis()
                                
                                calculationJob = scope.launch(Dispatchers.Default) {
                                    isCalculating = true
                                    val startTime = System.currentTimeMillis()
                                    var position = jobPacket.startPosition
                                    var lastPosition = position
                                    
                                    while (isActive && position <= jobPacket.endPosition) {
                                        val batchEndPosition = minOf(
                                            position + PasswordCracker.BATCH_SIZE,
                                            jobPacket.endPosition + 1
                                        )
                                        
                                        val result = PasswordCracker.attemptCrackParallel(
                                            targetHash,
                                            position,
                                            batchEndPosition,
                                            jobPacket.passwordLength,
                                            this
                                        )
                                        
                                        currentResult = result
                                        if (result?.found == true) {
                                            break
                                        }
                                        
                                        position = batchEndPosition
                                        currentAttempt = position
                                        
                                        val now = System.currentTimeMillis()
                                        if (now - lastAttemptTime >= 1000) {
                                            val attemptsDone = position - lastPosition
                                            attemptsPerSecond = attemptsDone * 1000 / (now - lastAttemptTime)
                                            lastAttemptTime = now
                                            lastPosition = position
                                        }
                                    }
                                    
                                    if (!currentResult?.found!! && currentAttempt > jobPacket.endPosition) {
                                        completedPackets++
                                        currentResult = currentResult?.copy(
                                            password = "Packet completed - no match found"
                                        )
                                    }
                                    isCalculating = false
                                }
                            } catch (e: NumberFormatException) {
                                // Handle invalid input
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCalculating) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isCalculating && targetHash.isNotEmpty()
                ) {
                    Text(
                        text = "Request New Job Packet",
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Button(
                    onClick = {
                        calculationJob?.cancel()
                        isCalculating = false
                        currentPacket = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = isCalculating
                ) {
                    Text(
                        text = "Stop Current Job",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
} 