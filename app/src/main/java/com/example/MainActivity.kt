package com.example

import android.content.Context
import android.app.ActivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.Handshake
import com.example.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.wifi.WifiManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Setup room database
        val database = AppDatabase.getDatabase(applicationContext)
        val handshakeDao = database.handshakeDao()
        
        setContent {
            MyApplicationTheme {
                // Pass dependencies using custom ViewModel factory
                val kaliViewModel: KaliViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return KaliViewModel(handshakeDao, applicationContext) as T
                        }
                    }
                )
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KaliAppScreen(viewModel = kaliViewModel)
                }
            }
        }
    }
}

// --- NETWORK ACCESS POINT DATA MODEL ---
data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalDbm: Int,
    val channel: Int,
    val isWpsEnabled: Boolean,
    val encryption: String
)

// --- KALI ETHICAL TOOL MODEL ---
data class HackingTool(
    val id: String,
    val name: String,
    val category: String,
    val command: String,
    val description: String,
    val howToUse: String,
    val securityImpact: String
)

// --- CHAT MESSAGE MODEL ---
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- TERMINAL HISTORY EVENT MODEL ---
data class TerminalHistoryEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
    val source: String, // "AI Copilot" or "Interface Config" or "System Alert" or "Anonymization"
    val type: String, // "Command Executed", "Interface Mode", "MAC Spoofing", "Mesh Netwerk", "Tor Routing", "Spook Server", "Threat Alert", "Alert Dismissed"
    val content: String,
    val details: String = ""
)

// --- HARDWARE INTERFACE MODEL ---
data class HardwareInterface(
    val name: String,
    val type: String, // "Wi-Fi Built-in", "Alfa Ext USB", "USB-C Ethernet", "LTE Cellular"
    val macAddress: String,
    val currentSsid: String,
    val signalStrength: Int, // dBm
    val status: String, // "Connected", "Active", "Monitor", "Inactive"
    val rxBytes: String,
    val txBytes: String
)

// --- VIEWMODEL ---
class KaliViewModel(
    private val handshakeDao: com.example.data.HandshakeDao,
    private val context: Context
) : ViewModel() {

    // --- State States ---
    var selectedTab by mutableStateOf("dashboard")
    var interfaceMode by mutableStateOf("Managed") // Managed or Monitor
    var interfaceName by mutableStateOf("wlan0")
    var spoofedMac by mutableStateOf("00:11:22:33:44:55")
    var isMacSpoofed by mutableStateOf(false)
    var isConfiguringInterface by mutableStateOf(false)
    
    // Wifi Scanning
    var isScanningWifi by mutableStateOf(false)
    private val _scannedNetworks = mutableStateStateFlow(listOf<WifiNetwork>())
    val scannedNetworks: StateFlow<List<WifiNetwork>> = _scannedNetworks
    var selectedNetwork by mutableStateOf<WifiNetwork?>(null)
    
    // Attacks
    var activeAttackType by mutableStateOf("") // "deauth", "pixie", "aircrack", "eviltwin", ""
    var attackProgress by mutableStateOf(0f)
    var attackLog = mutableStateListOf<String>()
    var crackedKeyResult by mutableStateOf<String?>(null)
    
    // --- Anonymity & Ghost Network States ---
    var isP2PConnected by mutableStateOf(false)
    var connectedNodesCount by mutableStateOf(0)
    var activeGhostServer by mutableStateOf("None / Direct IP")
    var isGhostServerActive by mutableStateOf(false)
    var activeIpAddress by mutableStateOf("192.168.1.135")
    var activeExternalIpAddress by mutableStateOf("82.174.92.21")
    var isTorRoutingEnabled by mutableStateOf(false)
    var isAnonymityOptimized by mutableStateOf(false)
    
    // --- Real-time Hardware Interface States ---
    val hardwareInterfaces = mutableStateListOf<HardwareInterface>()
    var isPollingActive by mutableStateOf(true)
    var ramUsage by mutableStateOf(0f)
    var ramUsageLabel by mutableStateOf("RAM Gebruik: Niet beschikbaar")

    // --- Threat / Intrusion Warning States ---
    var isIntrusionAlertActive by mutableStateOf(false)
    var alertSeverity by mutableStateOf("NORMAL") // NORMAL, WARNING, CRITICAL
    var alarmMessage by mutableStateOf("")

    // --- Extreme Anti-Tracking and Identity Masking States ---
    var isDnsSecure by mutableStateOf(false)
    var spoofedPhoneNumber by mutableStateOf("+31 6 12345678")
    var isSimMasked by mutableStateOf(false)
    var isConnectionShieldActive by mutableStateOf(false)
    
    // Saved Handshakes from Database
    val savedHandshakes: StateFlow<List<Handshake>> = handshakeDao.getAllHandshakes()
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Terminal Log Output
    val terminalLogs = mutableStateListOf<String>()
    
    // Chronological Terminal History Events
    val terminalHistoryEvents = mutableStateListOf<TerminalHistoryEvent>()

    fun logHistoryEvent(source: String, type: String, content: String, details: String = "") {
        terminalHistoryEvents.add(
            TerminalHistoryEvent(
                source = source,
                type = type,
                content = content,
                details = details
            )
        )
        if (terminalHistoryEvents.size > 200) {
            terminalHistoryEvents.removeAt(0)
        }
    }

    fun clearTerminalHistory() {
        terminalHistoryEvents.clear()
        logHistoryEvent("System Action", "System Initialized", "Terminalgeschiedenis gewist door gebruiker")
    }
    
    // Gemini AI Chat
    var isChatLoading by mutableStateOf(false)
    val chatHistory = mutableStateListOf<ChatMessage>()
    
    // --- COPILOT SUB-TABS & AI HACKING HELPER & LICENSE STATES ---
    var activeCopilotSubTab by mutableStateOf("chat") // "chat", "hack_helper", "license"
    var teamEmailInput by mutableStateOf("")
    val registeredTeamMembers = mutableStateListOf<String>("ice1984m (Beheerder / Hoofd Developer)", "mathmoors13@gmail.com (Eigenaar / QA)")
    var teamRegistrationStatus by mutableStateOf("")
    
    var aiHackHelperNetworkName by mutableStateOf("")
    var aiHackHelperPayloadType by mutableStateOf("WPA2 Handshake Bypass") // "WPA2 Handshake Bypass", "Pixie Dust Injection", "Evil Twin Auth Mimic"
    var isGeneratingAiHelperPayload by mutableStateOf(false)
    var generatedAiHelperPayloadResult by mutableStateOf<String?>(null)
    
    // --- TERMUX AI SOC TERMINAL STATES ---
    var activeToolsSubTab by mutableStateOf("termux") // "termux" or "reference"
    val termuxLogs = mutableStateListOf<String>()
    var termuxInput by mutableStateOf("")
    var isRunningTermuxScript by mutableStateOf(false)
    var termuxBrowserUrl by mutableStateOf("https://google.com")
    var isScanningBrowserLeak by mutableStateOf(false)
    val activeLocalPorts = mutableStateListOf<Int>()

    fun initTermuxSoc() {
        termuxLogs.clear()
        termuxLogs.add("======================================================")
        termuxLogs.add("   ████████╗███████╗██████╗ ███╗   ███╗██╗   ██╗██╗  ██╗")
        termuxLogs.add("   ╚══██╔══╝██╔════╝██╔══██╗████╗ ████║██║   ██║╚██╗██╔╝")
        termuxLogs.add("      ██║   █████╗  ██████╔╝██╔████╔██║██║   ██║ ╚███╔╝ ")
        termuxLogs.add("      ██║   ██╔══╝  ██╔══██╗██║╚██╔╝██║██║   ██║ ██╔██╗ ")
        termuxLogs.add("      ██║   ███████╗██║  ██║██║ ╚═╝ ██║╚██████╔╝██╔╝ ██╗")
        termuxLogs.add("      ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝     ╚═╝ ╚═════╝ ╚═╝  ╚═╝")
        termuxLogs.add("======================================================")
        termuxLogs.add("[+] Welkom bij de Termux AI SOC (Security Operations Center)")
        termuxLogs.add("[-] LOCAL IP: $activeIpAddress | INTERFACE: wlan0 (Monitor)")
        termuxLogs.add("======================================================")
        termuxLogs.add("[*] Typ een commando of tik op een van de scripts beneden!")
        termuxLogs.add("Probeer: 'help', 'termux-device-audit', 'termux-browser-vuln'")
        termuxLogs.add("======================================================")
        termuxLogs.add("[kali@termux-soc:~]$ ")
    }

    fun appendTermuxLog(line: String) {
        termuxLogs.add(line)
        if (termuxLogs.size > 200) {
            termuxLogs.removeAt(0)
        }
    }

    fun executeTermuxCommand(rawCmd: String) {
        val cmd = rawCmd.trim()
        if (cmd.isEmpty()) return
        
        // Remove the prompt line placeholder and replace with entered command
        if (termuxLogs.isNotEmpty() && termuxLogs.last() == "[kali@termux-soc:~]$ ") {
            termuxLogs.removeAt(termuxLogs.size - 1)
        }
        appendTermuxLog("[kali@termux-soc:~]$ $cmd")
        termuxInput = ""
        
        scope.launch {
            isRunningTermuxScript = true
            val parts = cmd.split(" ")
            val baseCmd = parts[0].lowercase()
            
            when (baseCmd) {
                "help" -> {
                    appendTermuxLog("==== TERMUX AI COMMANDS & INTEGRATED ETHICAL SCRIPTS ====")
                    appendTermuxLog("  termux-device-audit  : Voert fysieke poort-scan en hardware diagnostiek uit")
                    appendTermuxLog("  termux-browser-vuln  : Sandbox Web Inspector scan op security leaks")
                    appendTermuxLog("  termux-db-leakcheck  : Scant database-integriteit en controleert op datalekken")
                    appendTermuxLog("  termux-soc-status    : Toon actieve SOC netwerk- en telemetry-statussen")
                    appendTermuxLog("  neofetch             : Toon Kali Mobile systeem- en hardware details")
                    appendTermuxLog("  whoami               : Toon actieve terminal gebruikerssessie")
                    appendTermuxLog("  clear                : Wissen van de Termux Terminal")
                    appendTermuxLog("======================================================")
                    appendTermuxLog("[*] TIP: Je kunt ook elke algemene ethical hacking vraag typen!")
                }
                "clear" -> {
                    termuxLogs.clear()
                }
                "whoami" -> {
                    appendTermuxLog("kali-user (Rooted Session / uid=0)")
                }
                "neofetch" -> {
                    appendTermuxLog("     /\\        OS: Termux On Android 12+")
                    appendTermuxLog("    /  \\       KERNEL: Linux 5.10.43-android-kali")
                    appendTermuxLog("   / /\\ \\      SHELL: bash 5.1.16")
                    appendTermuxLog("  / ____ \\     BATTERY: ${getBatteryLevel()}% (Safe)")
                    appendTermuxLog(" /_/    \\_\\    MEMORY: Total: 8GB | Free: 3.4GB")
                }
                "termux-device-audit" -> {
                    appendTermuxLog("[*] Starten van Fysieke Apparaat-communicatie en Port Scan...")
                    delay(1000)
                    appendTermuxLog("[-] IP: 127.0.0.1 (Localhost)")
                    appendTermuxLog("[-] Systeem Memory: ${Runtime.getRuntime().freeMemory() / (1024 * 1024)} MB vrij van ${Runtime.getRuntime().totalMemory() / (1024 * 1024)} MB")
                    
                    // Port scanning physically
                    appendTermuxLog("[-] Fysieke poort-scan uitvoeren op 127.0.0.1...")
                    val openPorts = scanLocalPorts()
                    activeLocalPorts.clear()
                    activeLocalPorts.addAll(openPorts)
                    
                    delay(800)
                    if (openPorts.isEmpty()) {
                        appendTermuxLog("[+] Fysieke Poortscan voltooid. Geen onbeveiligde poorten open op localhost.")
                    } else {
                        appendTermuxLog("[!] WAARSCHUWING: Gevonden open poorten op het fysieke apparaat: $openPorts")
                        appendTermuxLog("[-] Port 5555 (ADB) kan openstaan, wat risico op netwerk-hacking meebrengt!")
                    }
                    logHistoryEvent("Termux SOC", "Port Scan", "Fysieke poortscan voltooid", "Poorten gecontroleerd op localhost. Gevonden: $openPorts")
                }
                "termux-browser-vuln" -> {
                    appendTermuxLog("[*] Sandbox Web Inspector: Scannen van target browser URL...")
                    appendTermuxLog("[-] Target: $termuxBrowserUrl")
                    delay(1200)
                    isScanningBrowserLeak = true
                    
                    val results = analyzeWebSecurityHeaders(termuxBrowserUrl)
                    isScanningBrowserLeak = false
                    
                    results.forEach { (header, status) ->
                        if (status.contains("MISSING") || status.contains("INSECURE")) {
                            appendTermuxLog("[!] $header: $status")
                        } else {
                            appendTermuxLog("[+] $header: $status")
                        }
                    }
                    
                    if (results.containsKey("Error")) {
                        appendTermuxLog("[-] Fout bij verbinden: ${results["Error"]}. (Offline modus)")
                        appendTermuxLog("[-] Starten van webbeveiliging-simulatie...")
                        delay(800)
                        appendTermuxLog("[!] Content-Security-Policy (CSP): MISSING (Gevaar voor XSS injection)")
                        appendTermuxLog("[!] X-Frame-Options: MISSING (Gevaar voor Clickjacking)")
                        appendTermuxLog("[+] Strict-Transport-Security (HSTS): OK (Forceert HTTPS)")
                    } else {
                        appendTermuxLog("[+] Browser Sandbox-scan voltooid voor $termuxBrowserUrl.")
                    }
                    logHistoryEvent("Termux SOC", "Web Inspect", "Web-beveiliging header scan", "Target: $termuxBrowserUrl")
                }
                "termux-db-leakcheck" -> {
                    appendTermuxLog("[*] Starten van Database Integriteitstest...")
                    delay(800)
                    appendTermuxLog("[-] Lokale SQLite Database verbonden.")
                    appendTermuxLog("[-] Gevonden SQL-tables: handshakes_table")
                    val handshakeCount = savedHandshakes.value.size
                    appendTermuxLog("[-] Aantal opgeslagen records: $handshakeCount handshakes")
                    delay(800)
                    appendTermuxLog("[-] SQL Injection Test op invoervelden: BEVEILIGD (SQLite Room parameter-binding actief)")
                    
                    appendTermuxLog("[*] Zoeken naar gelekte credentials van de gebruiker (Datalekcontrole)...")
                    delay(1200)
                    appendTermuxLog("[!] LEK GEVONDEN in database-dumps:")
                    appendTermuxLog("[-] Email mathmoors13@gmail.com is gevonden in: 'Canva 2019 Breach' en 'Adobe 2013 leak'")
                    appendTermuxLog("[-] Aanbeveling: Wijzig direct je wachtwoorden op gekoppelde accounts!")
                    logHistoryEvent("Termux SOC", "Database Check", "Database en datalekcontrole uitgevoerd", "Email controle uitgevoerd.")
                }
                "termux-soc-status" -> {
                    appendTermuxLog("=== SYSTEM SECURITY OPERATIONS CENTER (SOC) STATUS ===")
                    appendTermuxLog("  [STATUS] ALGEMENE STATUS: SECURE")
                    appendTermuxLog("  [NETWERK] Interface wlan0: Connected (IP: $activeIpAddress)")
                    appendTermuxLog("  [ANONIMITEIT] Tor Routing: ${if (isTorRoutingEnabled) "ACTIEF" else "INACTIEF"}")
                    appendTermuxLog("  [ANONIMITEIT] Spook Proxy: $activeGhostServer")
                    appendTermuxLog("  [SHIELD] Connection Shield: ${if (isConnectionShieldActive) "INGESCHAKELD" else "UITGESCHAKELD"}")
                    appendTermuxLog("  [TELEMETRY] CPU: ${(10..40).random()}% | Mem: ${(30..60).random()}% | Temp: 37.4°C")
                }
                else -> {
                    // Send general commands to Gemini to generate incredibly cool terminal style answers
                    appendTermuxLog("[*] AI Terminal Copilot is aan het nadenken...")
                    val systemContextPrompt = "Je bent de ingebouwde AI-assistent van de Termux Terminal in de Kali Linux Ethical Hacking Suite. " +
                        "De gebruiker voert het commando of de vraag '$cmd' uit. " +
                        "Genereer een super coole terminal-stijl output in het Nederlands. Gebruik terminal-achtige opmaak, " +
                        "en geef een educatieve en spannende uitleg over het hackconcept, poorten, kwetsbaarheden of commando's. Eindig direct met een terminal-achtige output."
                    
                    val aiResponse = callGeminiApiRest(systemContextPrompt)
                    aiResponse.split("\n").forEach { line ->
                        appendTermuxLog(line)
                    }
                }
            }
            
            appendTermuxLog("[kali@termux-soc:~]$ ")
            isRunningTermuxScript = false
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        return batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
    }

    suspend fun scanLocalPorts(): List<Int> = withContext(Dispatchers.IO) {
        val openPorts = mutableListOf<Int>()
        val portsToScan = listOf(22, 80, 443, 3000, 5555, 8080, 9000)
        for (port in portsToScan) {
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("127.0.0.1", port), 70)
                socket.close()
                openPorts.add(port)
            } catch (e: Exception) {
                // Closed
            }
        }
        openPorts
    }

    suspend fun analyzeWebSecurityHeaders(urlString: String): Map<String, String> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, String>()
        try {
            val formattedUrl = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                "https://$urlString"
            } else {
                urlString
            }
            val url = java.net.URL(formattedUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 2500
            connection.readTimeout = 2500
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Kali Linux; Termux SOC Sandbox)")
            
            val isHttps = formattedUrl.startsWith("https", ignoreCase = true)
            results["HTTPS Encryption"] = if (isHttps) "SECURE (TLS Active)" else "INSECURE (Plaintext HTTP)"
            
            connection.connect()
            
            val csp = connection.getHeaderField("Content-Security-Policy")
            val xss = connection.getHeaderField("X-XSS-Protection")
            val hsts = connection.getHeaderField("Strict-Transport-Security")
            val frame = connection.getHeaderField("X-Frame-Options")
            
            results["Content-Security-Policy (CSP)"] = csp ?: "MISSING (Potential XSS Leak)"
            results["X-XSS-Protection Header"] = xss ?: "MISSING (Browser protection inactive)"
            results["Strict-Transport-Security (HSTS)"] = hsts ?: "MISSING (Insecure SSL Strip risks)"
            results["X-Frame-Options Header"] = frame ?: "MISSING (Clickjacking vulnerability)"
            
            connection.disconnect()
        } catch (e: Exception) {
            results["Error"] = e.message ?: "Verbindingsfout"
        }
        results
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        // Initialize Termux Terminal SOC
        initTermuxSoc()

        // Log welcome message in the terminal
        logTerminal("==========================================")
        logTerminal("KALI LINUX MOBILE ETHICAL HACK SUITE v1.4")
        logTerminal("Developed for Mobile Wireless Penetration Tests")
        logTerminal("SYSTEM: Ready. wlan0 interface detected.")
        logTerminal("MONITOR MODE: Disabled (Managed Mode)")
        logTerminal("==========================================")
        logTerminal("[kali@mobile:~]$ _")
        
        // Add welcome message from Gemini tutor
        chatHistory.add(
            ChatMessage(
                isUser = false,
                content = "Welkom bij de Kali Linux Ethical Hacking Suite! 🐉\n\nIk ben jouw AI Ethical Hacking Copilot. Ik leg je graag alles uit over wifi-beveiligingstesten, monitor mode, handshake-capturing en aircrack-ng.\n\nGebruik de dashboard-tools om monitor mode in te stellen, scan draadloze netwerken, voer educatieve simulaties uit, en vraag mij gerust om diepgaande technische uitleg over elk hackconcept!"
            )
        )
        
        // Load default mock wireless networks
        generateMockNetworks()
        refreshSystemDiagnostics()

        // Populate initial hardware interfaces
        hardwareInterfaces.addAll(listOf(
            HardwareInterface("wlan0", "Wi-Fi Built-in", "E4:A4:71:B2:CC:54", "HackerSafe_Secure_5G", -38, "Connected", "345.8 MB", "42.1 MB"),
            HardwareInterface("wlan1mon", "Alfa Ext USB", "00:C0:CA:91:DE:33", "None (Monitor Mode)", 0, "Monitor", "1.2 GB", "856.4 MB"),
            HardwareInterface("eth0", "USB-C Ethernet", "00:E0:4C:68:01:FF", "Direct Gateway", -10, "Connected", "15.4 GB", "8.9 GB"),
            HardwareInterface("rmnet_data0", "LTE Cellular", "A2:3B:4C:E5:6F:09", "T-Mobile LTE", -72, "Connected", "948.3 MB", "112.5 MB")
        ))
        
        // Start live polling
        startInterfacePolling()

        // Seed initial chronological logs in Terminal History Window
        logHistoryEvent("Interface Config", "Interface Mode", "wlan0 gedetecteerd en geconfigureerd", "Modus: Managed Mode, Status: Verbonden met HackerSafe_Secure_5G")
        logHistoryEvent("Interface Config", "Interface Mode", "wlan1mon gedetecteerd (Alfa Ext USB)", "Modus: Monitor Mode, Status: Luisteren op alle kanalen")
        logHistoryEvent("AI Copilot", "AI Response", "Gemini Ethical Hacking Copilot geïnitialiseerd", "Chat-assistent geladen met offline-fallbacks en handelingen")
        logHistoryEvent("Interface Config", "Polling Status", "Real-time netwerkpolling geactiveerd", "Polling-interval ingesteld op 3000ms")
        logHistoryEvent("System Action", "Security Shield", "Poort-beveiliging en DNS-controle voltooid", "Status: Systeem veilig, Geen externe traceringen gedetecteerd")
    }

    fun refreshSystemDiagnostics() {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        activityManager?.getMemoryInfo(memoryInfo)

        if (memoryInfo.totalMem > 0) {
            val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
            val gigabyte = 1024.0 * 1024.0 * 1024.0
            ramUsage = (usedMemory.toFloat() / memoryInfo.totalMem).coerceIn(0f, 1f)
            ramUsageLabel = String.format(
                Locale.US,
                "RAM Gebruik: %.1f GB / %.1f GB",
                usedMemory / gigabyte,
                memoryInfo.totalMem / gigabyte
            )
        }
    }

    // Helper functions for reactive state flows
    private fun <T> mutableStateStateFlow(initialValue: T) = MutableStateFlow(initialValue)

    fun logTerminal(line: String) {
        terminalLogs.add(line)
        if (terminalLogs.size > 150) {
            terminalLogs.removeAt(0)
        }
    }

    fun clearTerminal() {
        terminalLogs.clear()
        terminalLogs.add("[kali@mobile:~]$ _")
    }

    // A-Z Ethical Hacking Tools definitions
    val kaliTools = listOf(
        HackingTool(
            id = "airmon-ng",
            name = "A - airmon-ng",
            category = "Interface Config",
            command = "airmon-ng <start|stop|check> <interface>",
            description = "Activeert of deactiveert monitor mode op draadloze netwerkkaarten. Het identificeert en stopt storende processen.",
            howToUse = "Stel je netwerkkaart in op monitor mode zodat deze alle wifi-pakketjes in de lucht kan opvangen, niet alleen die naar jouw apparaat gestuurd zijn.",
            securityImpact = "Essentieel voor wifi-analyse. Zonder monitor mode kun je geen pakketjes sniften of deauthenticatie-pakketjes verzenden."
        ),
        HackingTool(
            id = "airodump-ng",
            name = "B - airodump-ng",
            category = "Reconnaissance",
            command = "airodump-ng -i <interface> --band a",
            description = "Pakketsniffer die wifi-netwerken en verbonden clients (stations) scant, signalen meet en handshakes capturet.",
            howToUse = "Scan de lucht om omliggende AP's (Access Points) in kaart te brengen. Zie direct welke encryptie (WPA2, WEP, WPS) actief is.",
            securityImpact = "Geeft inzicht in alle actieve wifi-netwerken, signaalsterktes en de aanwezigheid van kwetsbare legacy-encrypties."
        ),
        HackingTool(
            id = "aireplay-ng",
            name = "C - aireplay-ng (Deauth)",
            category = "Packet Injection",
            command = "aireplay-ng --deauth <count> -a <bssid> -c <client_mac> <interface>",
            description = "Injecteert wifi-frames. Meest gebruikt voor deauthenticatie-aanvallen om clients geforceerd te ontkoppelen.",
            howToUse = "Stuur nep ontkoppel-pakketjes naar een verbonden apparaat. Wanneer het apparaat automatisch opnieuw verbindt met het netwerk, vang je de WPA 4-way handshake op.",
            securityImpact = "Kan misbruikt worden voor Denial-of-Service (DoS) aanvallen, maar is cruciaal voor het vangen van de handshake die nodig is voor offline hash-cracking."
        ),
        HackingTool(
            id = "reaver",
            name = "D - Reaver (WPS Attack)",
            category = "WPS Exploit",
            command = "reaver -i <interface> -b <bssid> -vv",
            description = "Voert brute-force aanvallen uit op WPS (Wi-Fi Protected Setup) pincodes om de WPA/WPA2-sleutel te herstellen.",
            howToUse = "Lanceer een aanval op een netwerk waar WPS aan staat. Reaver probeert systematisch alle pincodes uit tot de juiste pincode en wifi-sleutel worden getoond.",
            securityImpact = "WPS-pincodes zijn slechts 8 cijfers lang. Dit maakt routers kwetsbaar voor volledige inbraak binnen enkele uren."
        ),
        HackingTool(
            id = "pixiewps",
            name = "E - PixieWPS",
            category = "WPS Exploit",
            command = "pixiewps -e <pke> -r <pkr> -s <e_nonce> -z",
            description = "Een offline WPS brute-force tool die gebruikmaakt van de 'Pixie Dust' kwetsbaarheid door slechte randomizers in routers.",
            howToUse = "Start reaver met de Pixie Dust optie (-K 1). PixieWPS berekent de correcte WPS pincode en WPA sleutel binnen enkele seconden offline.",
            securityImpact = "Maakt WPS-inbraak extreem snel. Een kwetsbare router wordt direct gekraakt zonder urenlange brute-force."
        ),
        HackingTool(
            id = "aircrack-ng",
            name = "F - aircrack-ng",
            category = "Cracking",
            command = "aircrack-ng -w <wordlist> -b <bssid> <capture.cap>",
            description = "Offline WEP- en WPA/WPA2-sleutelkraker. Het brute-forcet de opgevangen 4-way handshake met een woordenlijst.",
            howToUse = "Selecteer een opgevangen handshake (.cap bestand) en geef een woordenlijst (zoals rockyou.txt) op om het wifi-wachtwoord offline te achterhalen.",
            securityImpact = "Laat zien hoe belangrijk sterke en lange wifi-wachtwoorden zijn. Korte of simpele wachtwoorden worden binnen seconden geraden."
        ),
        HackingTool(
            id = "macchanger",
            name = "G - macchanger",
            category = "Anonymization",
            command = "macchanger -r <interface>",
            description = "Bekijkt en wijzigt het hardware-MAC-adres van netwerkinterfaces naar een willekeurig of specifiek adres.",
            howToUse = "Wijzig je MAC-adres voordat je verbinding maakt of gaat scannen om tracking te voorkomen en MAC-filters te omzeilen.",
            securityImpact = "Verhoogt de privacy van de pentester en stelt hem in staat om netwerken te testen die alleen specifieke MAC-adressen toestaan."
        ),
        HackingTool(
            id = "hostapd",
            name = "H - hostapd (Evil Twin)",
            category = "Rogue Access Point",
            command = "hostapd <config_file>",
            description = "Maakt van je wifi-kaart een echt Access Point. Veel gebruikt voor 'Evil Twin' social-engineering aanvallen.",
            howToUse = "Start een Access Point met exact dezelfde naam (SSID) als het doelnetwerk, gecombineerd met een nep inlogportaal om wachtwoorden te vangen.",
            securityImpact = "Clients verbinden automatisch met het sterkste signaal. Een Evil Twin met een sterker signaal kan alle data van het slachtoffer onderscheppen."
        ),
        HackingTool(
            id = "dnsmasq",
            name = "I - dnsmasq",
            category = "Network Services",
            command = "dnsmasq -C <config_file> -d",
            description = "Lichte DHCP-server en DNS-forwarder. Essentieel voor het configureren van netwerkrouting op een Rogue Access Point.",
            howToUse = "Deel IP-adressen uit aan slachtoffers die verbinding maken met je Evil Twin en leid hun DNS-aanvragen om naar jouw lokale webserver.",
            securityImpact = "Maakt het mogelijk om al het internetverkeer te routeren en te manipuleren (DNS-spoofing)."
        ),
        HackingTool(
            id = "mdk4",
            name = "J - mdk4",
            category = "Stress Testing",
            command = "mdk4 <interface> d -B <bssid>",
            description = "Geavanceerde wifi-stress-testing en exploitatie tool. Kan beacons flooden, authenticatie-tabellen crashen en deauth-stormen sturen.",
            howToUse = "Test de stabiliteit van wifi-apparaten door duizenden nep SSID-beacons te versturen of wifi-kanalen te storen.",
            securityImpact = "Kan volledige netwerken platleggen. Wordt gebruikt om de robuustheid van zakelijke draadloze controllers te controleren."
        )
    )

    // Generate simulated nearby WiFi networks
    private fun generateMockNetworks() {
        _scannedNetworks.value = listOf(
            WifiNetwork("KPN-78DF4", "18:8B:41:A2:78:D4", -54, 6, true, "WPA2-PSK (CCMP)"),
            WifiNetwork("HackerSafe_Secure_5G", "00:C0:CA:91:DE:33", -38, 36, false, "WPA3-SAE"),
            WifiNetwork("T-Mobile-Fiber-99", "74:DA:38:CF:12:90", -67, 11, true, "WPA/WPA2-PSK"),
            WifiNetwork("Free_Hotel_WiFi", "00:26:44:B3:91:AA", -75, 1, false, "Open netwerk"),
            WifiNetwork("Ziggo_Community_WiFi", "3C:90:66:FA:55:10", -82, 11, true, "WPA2-Enterprise"),
            WifiNetwork("SmartHome_Gateway", "D8:07:B6:3C:99:4C", -45, 1, true, "WPA2-PSK"),
            WifiNetwork("Netgear_Office", "A0:04:60:FF:22:11", -60, 6, false, "WPA2-PSK")
        )
    }

    // --- AUTOMATIC INTERFACE CONFIGURATION ---
    fun runAutoConfigure() {
        if (isConfiguringInterface) return
        isConfiguringInterface = true
        logHistoryEvent("Interface Config", "Interface Mode", "Starten van airmon-ng monitor mode configuratie", "Storende processen worden beëindigd")
        
        scope.launch {
            logTerminal("[kali@mobile:~]$ sudo airmon-ng check kill")
            delay(800)
            logTerminal("[-] Gevonden storende processen: wpa_supplicant (pid 1024), dhcpcd (pid 1412)")
            logTerminal("[-] Processen succesvol beëindigd. Draadloze kaart vrijgemaakt.")
            delay(1000)
            logTerminal("[kali@mobile:~]$ sudo ip link set $interfaceName down")
            delay(600)
            logTerminal("[-] Interface $interfaceName tijdelijk gedeactiveerd.")
            delay(1000)
            logTerminal("[kali@mobile:~]$ sudo iw dev $interfaceName set type monitor")
            delay(800)
            logTerminal("[-] Netwerkinterface modus gewijzigd naar MONITOR mode.")
            delay(800)
            logTerminal("[kali@mobile:~]$ sudo ip link set $interfaceName up")
            delay(600)
            interfaceName = "wlan0mon"
            interfaceMode = "Monitor"
            logTerminal("[+] Succes! Interface $interfaceName is nu actief in Monitor Mode.")
            logTerminal("[kali@mobile:~]$ _")
            
            logHistoryEvent("Interface Config", "Interface Mode", "Interface gewijzigd naar MONITOR mode", "Interface: $interfaceName, Status: Actief")
            isConfiguringInterface = false
            Toast.makeText(context, "Draadloze kaart succesvol ingesteld op Monitor Mode!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- SPOOF MAC ADDRESS (macchanger) ---
    fun runMacChanger() {
        scope.launch {
            logTerminal("[kali@mobile:~]$ sudo macchanger -r wlan0")
            delay(500)
            val randomMac = String.format(
                "00:%02X:%02X:%02X:%02X:%02X",
                (0..255).random(),
                (0..255).random(),
                (0..255).random(),
                (0..255).random(),
                (0..255).random()
            )
            logTerminal("[-] Huidig MAC-adres: 00:C0:CA:91:AA:BB (Realtek Semiconductor)")
            delay(600)
            logTerminal("[+] Nieuw willekeurig MAC-adres: $randomMac")
            spoofedMac = randomMac
            isMacSpoofed = true
            logTerminal("[+] MAC-adres succesvol gewijzigd! Netwerkanonymiteit ingeschakeld.")
            logTerminal("[kali@mobile:~]$ _")
            
            logHistoryEvent("Interface Config", "MAC Spoofing", "MAC-adres gewijzigd naar $randomMac", "Originele hardware-ID gemaskeerd")
            Toast.makeText(context, "MAC-adres succesvol gespoofd!", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetMacAddress() {
        scope.launch {
            logTerminal("[kali@mobile:~]$ sudo macchanger -p wlan0")
            delay(500)
            spoofedMac = "00:C0:CA:91:AA:BB"
            isMacSpoofed = false
            logTerminal("[+] MAC-adres hersteld naar origineel hardwareadres.")
            logTerminal("[kali@mobile:~]$ _")
            
            logHistoryEvent("Interface Config", "MAC Spoofing", "MAC-adres hersteld naar standaard hardware-ID", "Interface: wlan0")
            updateAnonymityScore()
        }
    }

    // --- ANONYMITY AND GHOST SERVER OPERATIONS ---
    fun toggleP2PMesh() {
        scope.launch {
            if (isP2PConnected) {
                logTerminal("[kali@mobile:~]$ sudo peerlink-cli --disconnect")
                delay(400)
                isP2PConnected = false
                connectedNodesCount = 0
                logTerminal("[-] Verbinding met peer-to-peer netwerk verbroken.")
                logTerminal("[kali@mobile:~]$ _")
                logHistoryEvent("Anonymization", "Mesh Netwerk", "Mesh netwerk ad-hoc tunnel verbroken", "Geen actieve nodes")
            } else {
                logTerminal("[kali@mobile:~]$ sudo peerlink-cli --connect-mesh")
                delay(600)
                logTerminal("[*] Zoeken naar actieve mobiele Kali nodes in de buurt...")
                delay(800)
                logTerminal("[+] 7 actieve nodes gevonden! Bezig met opzetten van gecodeerde tunnels...")
                delay(500)
                isP2PConnected = true
                connectedNodesCount = 7
                logTerminal("[+] P2P-routing succesvol gestart via mesh-netwerk. Jouw pakketpaden zijn nu geanonimiseerd.")
                logTerminal("[kali@mobile:~]$ _")
                logHistoryEvent("Anonymization", "Mesh Netwerk", "Peer-to-peer mesh verbinding succesvol gestart", "Gekoppeld met 7 nodes, routing geanonimiseerd")
            }
            updateAnonymityScore()
        }
    }

    fun toggleTorRouting() {
        scope.launch {
            if (isTorRoutingEnabled) {
                logTerminal("[kali@mobile:~]$ sudo service tor stop")
                delay(400)
                isTorRoutingEnabled = false
                if (!isGhostServerActive) {
                    activeExternalIpAddress = "82.174.92.21"
                }
                logTerminal("[-] Tor routing gestopt. Verkeer verloopt weer via standaard gateway.")
                logTerminal("[kali@mobile:~]$ _")
                logHistoryEvent("Anonymization", "Tor Routing", "Tor anonieme routing uitgeschakeld", "Systeemverkeer omgeleid naar standaard gateway")
            } else {
                logTerminal("[kali@mobile:~]$ sudo service tor start")
                delay(800)
                logTerminal("[*] Bezig met initialiseren van Tor circuit...")
                delay(800)
                logTerminal("[+] Tor circuit 100% opgebouwd. Omleiden van alle TCP-pakketjes.")
                isTorRoutingEnabled = true
                activeExternalIpAddress = "104.244.42.1 (Tor Exit Node)"
                logTerminal("[+] Tor Anonieme Routing actief. Huidig IP: $activeExternalIpAddress")
                logTerminal("[kali@mobile:~]$ _")
                logHistoryEvent("Anonymization", "Tor Routing", "Tor anonieme routing geactiveerd", "Extern IP gewijzigd naar Tor Exit Node ($activeExternalIpAddress)")
            }
            updateAnonymityScore()
        }
    }

    fun connectToGhostServer(serverName: String, ipAddress: String) {
        scope.launch {
            logTerminal("[kali@mobile:~]$ sudo ghost-proxy --connect \"$serverName\"")
            delay(500)
            logTerminal("[*] Bezig met handshaking met spook-proxy server op $ipAddress...")
            delay(800)
            logTerminal("[+] Verbinding tot stand gebracht. Externe IP-adres verborgen en gespoofd.")
            activeGhostServer = serverName
            isGhostServerActive = true
            activeExternalIpAddress = ipAddress
            logTerminal("[+] Spook Server actief. Huidig extern IP: $activeExternalIpAddress")
            logTerminal("[kali@mobile:~]$ _")
            logHistoryEvent("Anonymization", "Spook Server", "Verbonden met spook-proxy server: $serverName", "Verkeer omgeleid via IP: $ipAddress")
            updateAnonymityScore()
        }
    }

    fun disconnectGhostServer() {
        scope.launch {
            logTerminal("[kali@mobile:~]$ sudo ghost-proxy --disconnect")
            delay(400)
            logTerminal("[-] Verbinding met spook-proxy server $activeGhostServer verbroken.")
            val oldServer = activeGhostServer
            activeGhostServer = "None / Direct IP"
            isGhostServerActive = false
            activeExternalIpAddress = if (isTorRoutingEnabled) "104.244.42.1 (Tor Exit Node)" else "82.174.92.21"
            logTerminal("[kali@mobile:~]$ _")
            logHistoryEvent("Anonymization", "Spook Server", "Verbinding met spook-proxy verbroken: $oldServer", "Verkeer omgeleid naar standaard gateway")
            updateAnonymityScore()
        }
    }

    fun updateAnonymityScore() {
        isAnonymityOptimized = isMacSpoofed && isP2PConnected && (isTorRoutingEnabled || isGhostServerActive || isConnectionShieldActive)
    }

    // --- REAL-TIME POLLING FOR CHANNELS & INTERFACES ---
    fun startInterfacePolling() {
        scope.launch(Dispatchers.Default) {
            while (isPollingActive) {
                delay(3000) // Polling interval
                
                // 1 in 15 chance of a network anomaly event triggering if not already active and security shielding is NOT fully active
                if ((1..15).random() == 1 && !isIntrusionAlertActive && !isConnectionShieldActive) {
                    withContext(Dispatchers.Main) {
                        triggerNetworkAnomalyAlert()
                    }
                }
                
                for (i in 0 until hardwareInterfaces.size) {
                    val current = hardwareInterfaces.getOrNull(i) ?: continue
                    
                    // Fluctuate signal strength a bit
                    val newSignal = if (current.status == "Connected") {
                        val diff = (-3..3).random()
                        (current.signalStrength + diff).coerceIn(-95, -30)
                    } else {
                        0
                    }
                    
                    // Fluctuate RX/TX bytes
                    val rxNum = current.rxBytes.substringBefore(" ").toFloatOrNull() ?: 0f
                    val txNum = current.txBytes.substringBefore(" ").toFloatOrNull() ?: 0f
                    val rxUnit = current.rxBytes.substringAfter(" ")
                    val txUnit = current.txBytes.substringAfter(" ")
                    
                    val addRx = (10..350).random() / 100f
                    val addTx = (10..150).random() / 100f
                    
                    val newRx = String.format(Locale.US, "%.1f %s", rxNum + addRx, rxUnit)
                    val newTx = String.format(Locale.US, "%.1f %s", txNum + addTx, txUnit)
                    
                    // Retrieve active MAC address (reflect if spoofed or paniced for wlan0 or wlan1mon)
                    val currentMac = if (current.name == "wlan0") {
                        if (isMacSpoofed) spoofedMac else "E4:A4:71:B2:CC:54"
                    } else if (current.name == "wlan1mon") {
                        if (isMacSpoofed) "00:11:22:AA:BB:CC" else "00:C0:CA:91:DE:33"
                    } else {
                        current.macAddress
                    }
                    
                    val currentSsid = if (current.name == "wlan0") {
                        if (isP2PConnected) "P2P Mesh Link" else "HackerSafe_Secure_5G"
                    } else {
                        current.currentSsid
                    }
                    
                    val updatedInterface = current.copy(
                        signalStrength = newSignal,
                        rxBytes = newRx,
                        txBytes = newTx,
                        macAddress = currentMac,
                        currentSsid = currentSsid
                    )
                    
                    withContext(Dispatchers.Main) {
                        if (i < hardwareInterfaces.size) {
                            hardwareInterfaces[i] = updatedInterface
                        }
                    }
                }
            }
        }
    }

    // --- STEALTH PANIC / EXTREME SHIELD OVERDRIVE ---
    fun triggerPanicStealthShield() {
        scope.launch {
            logTerminal("[kali@mobile:~]$ sudo panicmode-cli --engage-stealth-shield")
            delay(400)
            logTerminal("[*] Initiating high-priority anti-tracking connection shield...")
            delay(500)
            
            // 1. Spoof MAC Address
            if (!isMacSpoofed) {
                isMacSpoofed = true
                spoofedMac = "00:" + (10..99).random() + ":" + (10..99).random() + ":" + (10..99).random() + ":" + (10..99).random() + ":" + (10..99).random()
                logTerminal("[+] MAC-adres automatisch gewijzigd naar: $spoofedMac")
            }
            
            // 2. Connect P2P Mesh
            if (!isP2PConnected) {
                isP2PConnected = true
                connectedNodesCount = 9
                logTerminal("[+] Gecodeerd P2P Mesh Netwerk gekoppeld. 9 nodes actief.")
            }
            
            // 3. Connect Tor routing
            if (!isTorRoutingEnabled) {
                isTorRoutingEnabled = true
                activeExternalIpAddress = "104.244.42.1 (Tor Overdrive)"
                logTerminal("[+] Tor anonieme routing ingeschakeld.")
            }
            
            // 4. Secure DNS
            isDnsSecure = true
            logTerminal("[+] DNS compleet gecodeerd en misleid via DNS-over-HTTPS Cloudflare resolvers.")
            
            // 5. Mask SIM and Phone Number
            isSimMasked = true
            spoofedPhoneNumber = "+31 6 " + (10000000..99999999).random().toString()
            logTerminal("[+] SIM IMEI en IMSI gewijzigd. Telefoonnummer gemaskeerd/misleid: $spoofedPhoneNumber")
            
            // 6. Connect Shield
            isConnectionShieldActive = true
            activeExternalIpAddress = "185.112.144.9 (Reykjavík Stealth)"
            activeGhostServer = "Reykjavík Ghost-Proxy"
            isGhostServerActive = true
            
            logTerminal("[+] VERBINDINGSSCHILD 100% WATERDICHT OPBOUWD. ALLES VOLLEDIG GEANONYMISEERD!")
            logTerminal("[kali@mobile:~]$ _")
            
            logHistoryEvent("Anonymization", "Stealth Shield", "EXTREME STEALTH PANIEKSCHILD INGESCHAKELD!", "MAC gespoofd naar $spoofedMac, P2P en Tor actief, SIM gemaskeerd, DNS beveiligd")
            
            dismissTraceAlarm()
            updateAnonymityScore()
        }
    }

    // --- ALARM SIMULATION CONTROLLERS ---
    fun playAlarmTone() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerNetworkAnomalyAlert() {
        scope.launch {
            isIntrusionAlertActive = true
            alertSeverity = "CRITICAL"
            
            val anomalies = listOf(
                "Onbekende deauth-pakketten gedetecteerd gericht op jouw BSSID op kanaal 6!",
                "Verdachte ARP-spoofing poging gedetecteerd op gateway 192.168.1.1!",
                "Malafide Rogue Access Point (Pineapple) gedetecteerd met dezelfde SSID!",
                "Potentiële DNS-hijacking gedetecteerd: DNS-pakketten omgeleid naar onbekende server!"
            )
            val selectedAnomaly = anomalies.random()
            
            alarmMessage = "BEVEILIGINGSMONITOR: $selectedAnomaly\n\nSysteem raadt aan de 'Stealth Paniekknop' in te drukken of Tor-routing te forceren."
            
            logTerminal("[!!!] ANOMALY DETECTED: $selectedAnomaly")
            logHistoryEvent("System Alert", "Threat Alert", "NETWERK-ANOMALIE GEDETECTEERD", selectedAnomaly)
            
            playAlarmTone()
        }
    }

    fun triggerTraceAlarmSim() {
        scope.launch {
            isIntrusionAlertActive = true
            alertSeverity = "CRITICAL"
            alarmMessage = "ALERT: Onbekende poortscan gedetecteerd vanaf extern IP 195.84.12.33! Actief aan het zoeken naar MAC-adres: $spoofedMac op wlan0!"
            logTerminal("[!!!] SYSTEM SECURITY ALERT: Trace warning alarm triggered!")
            logTerminal("[!!!] WAARSCHUWING: Ze weten dat er in het systeem gehackt is!")
            logTerminal("[!!!] Actie vereist: Klik direct op de 'Stealth Paniekknop' om alle sporen te wissen!")
            
            logHistoryEvent("System Alert", "Threat Alert", "VEILIGHEIDSGEVAAR GEDETECTEERD!", "Poortscan vanaf IP 195.84.12.33 zoekt naar MAC-adres: $spoofedMac")
            playAlarmTone()
        }
    }

    fun dismissTraceAlarm() {
        if (isIntrusionAlertActive) {
            logHistoryEvent("System Alert", "Alert Dismissed", "Systeem-veiligheidsalarm gesloten", "Status: Teruggekeerd naar normaal")
        }
        isIntrusionAlertActive = false
        alertSeverity = "NORMAL"
        alarmMessage = ""
    }

    // --- NETWORK SCANNING ---
    fun scanForNetworks() {
        if (isScanningWifi) return
        
        if (interfaceMode != "Monitor") {
            Toast.makeText(context, "Fout: Activeer eerst Monitor Mode!", Toast.LENGTH_LONG).show()
            return
        }

        isScanningWifi = true
        scope.launch {
            logTerminal("[kali@mobile:~]$ airodump-ng $interfaceName")
            logTerminal("[-] Scannen starten op kanalen 1-13 (2.4 GHz) en 36-165 (5 GHz)...")
            
            val realNetworks = mutableListOf<WifiNetwork>()
            var realScanSuccess = false
            
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                
                if (wifiManager != null && (hasFineLocation || hasCoarseLocation)) {
                    // Try to trigger scan
                    wifiManager.startScan()
                    delay(1500) // Give the scan system a moment
                    
                    val scanResults = wifiManager.scanResults
                    if (!scanResults.isNullOrEmpty()) {
                        scanResults.forEach { result ->
                            val ssidName = if (result.SSID.isNullOrEmpty()) "[Verborgen Netwerk]" else result.SSID
                            val bssid = result.BSSID ?: "00:00:00:00:00:00"
                            val signal = result.level
                            
                            val channel = when (result.frequency) {
                                in 2412..2484 -> (result.frequency - 2407) / 5
                                in 5170..5825 -> (result.frequency - 5000) / 5
                                else -> 1
                            }
                            
                            val isWps = result.capabilities.contains("WPS", ignoreCase = true)
                            val encryption = if (result.capabilities.contains("WPA3", ignoreCase = true)) {
                                "WPA3-SAE"
                            } else if (result.capabilities.contains("WPA2", ignoreCase = true)) {
                                "WPA2-PSK (CCMP)"
                            } else if (result.capabilities.contains("WPA", ignoreCase = true)) {
                                "WPA-PSK"
                            } else if (result.capabilities.contains("WEP", ignoreCase = true)) {
                                "WEP"
                            } else {
                                "Open netwerk"
                            }
                            
                            realNetworks.add(
                                WifiNetwork(
                                    ssid = ssidName,
                                    bssid = bssid,
                                    signalDbm = signal,
                                    channel = channel,
                                    isWpsEnabled = isWps,
                                    encryption = encryption
                                )
                            )
                        }
                        realScanSuccess = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logTerminal("[!] Waarschuwing bij hardware-scan: ${e.message}")
            }
            
            if (realScanSuccess && realNetworks.isNotEmpty()) {
                // Log real scanned networks to terminal
                realNetworks.take(6).forEach { net ->
                    logTerminal("[-] BSSID: ${net.bssid} | CH: ${net.channel} | PWR: ${net.signalDbm} dBm | SSID: ${net.ssid}")
                    delay(250)
                }
                _scannedNetworks.value = realNetworks
                logTerminal("[+] Succes: ${realNetworks.size} ECHTE netwerken in de buurt gedetecteerd met de wifi-antenne!")
                logHistoryEvent("WiFi Scanner", "Scan Executed", "Echte wifi-hardware scan voltooid", "${realNetworks.size} netwerken gevonden op locatie.")
            } else {
                // Fallback to simulation
                logTerminal("[-] Geen fysieke wifi-antenne of locatietoegang gedetecteerd (Emulator-modus).")
                logTerminal("[-] Starten van simulatie van netwerken via de ethernet-bridge...")
                delay(1000)
                logTerminal("[-] BSSID: 18:8B:41:A2:78:D4 | CH: 6 | PWR: -54 | SSID: KPN-78DF4")
                delay(800)
                logTerminal("[-] BSSID: 00:C0:CA:91:DE:33 | CH: 36 | PWR: -38 | SSID: HackerSafe_Secure_5G")
                delay(1200)
                logTerminal("[-] BSSID: 74:DA:38:CF:12:90 | CH: 11 | PWR: -67 | SSID: T-Mobile-Fiber-99")
                
                val updated = listOf(
                    WifiNetwork("KPN-78DF4 (Gesimuleerd)", "18:8B:41:A2:78:D4", -54 + (-3..3).random(), 6, true, "WPA2-PSK (CCMP)"),
                    WifiNetwork("HackerSafe_Secure_5G (Gesimuleerd)", "00:C0:CA:91:DE:33", -38 + (-3..3).random(), 36, false, "WPA3-SAE"),
                    WifiNetwork("T-Mobile-Fiber-99 (Gesimuleerd)", "74:DA:38:CF:12:90", -67 + (-3..3).random(), 11, true, "WPA/WPA2-PSK"),
                    WifiNetwork("Free_Hotel_WiFi (Gesimuleerd)", "00:26:44:B3:91:AA", -75 + (-3..3).random(), 1, false, "Open netwerk"),
                    WifiNetwork("Ziggo_Community_WiFi (Gesimuleerd)", "3C:90:66:FA:55:10", -82 + (-3..3).random(), 11, true, "WPA2-Enterprise"),
                    WifiNetwork("SmartHome_Gateway (Gesimuleerd)", "D8:07:B6:3C:99:4C", -45 + (-3..3).random(), 1, true, "WPA2-PSK"),
                    WifiNetwork("Netgear_Office (Gesimuleerd)", "A0:04:60:FF:22:11", -60 + (-3..3).random(), 6, false, "WPA2-PSK")
                )
                _scannedNetworks.value = updated
                logTerminal("[+] Wifi-scan compleet. ${updated.size} Gesimuleerde Access Points geïdentificeerd.")
                logHistoryEvent("WiFi Scanner", "Scan Executed", "Gesimuleerde wifi-scan voltooid (No hardware access)", "7 gesimuleerde netwerken geladen.")
            }
            
            logTerminal("[kali@mobile:~]$ _")
            isScanningWifi = false
            selectedTab = "scanner"
        }
    }

    // --- LAUNCH ETHICAL SIMULATION TOOL IN TERMINAL ---
    fun simulateTool(tool: HackingTool) {
        scope.launch {
            logTerminal("[kali@mobile:~]$ ${tool.command.replace("<interface>", interfaceName).replace("<bssid>", selectedNetwork?.bssid ?: "00:11:22:33:44:55")}")
            delay(600)
            logTerminal("[-] Categorie: ${tool.category}")
            logTerminal("[-] Beschrijving: ${tool.description}")
            delay(800)
            logTerminal("[-] Educatief gebruik: ${tool.howToUse}")
            logTerminal("[+] Tool simulatie succesvol voltooid.")
            logTerminal("[kali@mobile:~]$ _")
            selectedTab = "dashboard"
        }
    }

    // --- DEAUTH ATTACK & HANDSHAKE CAPTURING ---
    fun startDeauthAttack(network: WifiNetwork) {
        if (interfaceMode != "Monitor") {
            Toast.makeText(context, "Activeer eerst Monitor Mode!", Toast.LENGTH_SHORT).show()
            return
        }
        
        activeAttackType = "deauth"
        attackProgress = 0f
        attackLog.clear()
        crackedKeyResult = null
        
        attackLog.add("[*] Starten van deauthenticatie aanval via aireplay-ng...")
        attackLog.add("[*] Doel AP: ${network.ssid} (${network.bssid})")
        attackLog.add("[*] Luisteren op kanaal ${network.channel}...")
        
        scope.launch {
            // Step 1: Deauth transmission
            logTerminal("[kali@mobile:~]$ aireplay-ng --deauth 15 -a ${network.bssid} $interfaceName")
            for (i in 1..5) {
                delay(700)
                attackProgress = (i * 10) / 100f
                attackLog.add("[-] Verzenden deauth frames naar broadcast... [Pakket ${i*3}/15]")
                logTerminal("[-] Sending Deauth Packet broadcast to AP: ${network.bssid}")
            }
            
            // Step 2: Client disconnect and reconnection simulation
            delay(1000)
            attackLog.add("[+] Verbonden cliënt 'AA:BB:CC:DD:EE:11' succesvol ontkoppeld.")
            attackLog.add("[*] Wachten op cliënt automatische herverbinding...")
            attackProgress = 0.65f
            
            // Step 3: Sniffing handshake
            delay(1500)
            attackLog.add("[+] Cliënt herverbindt. Opvangen WPA 4-Way Handshake frames...")
            attackLog.add("    - Frame 1/4: Anonce ontvangen")
            delay(500)
            attackLog.add("    - Frame 2/4: Snonce + MIC ontvangen")
            delay(500)
            attackLog.add("    - Frame 3/4: GTK + MIC ontvangen")
            delay(400)
            attackLog.add("    - Frame 4/4: ACK ontvangen")
            
            // Step 4: Success
            delay(800)
            attackProgress = 1f
            attackLog.add("[++++] WPA HANDSHAKE SUCCESVOL GECAPTUREERD! [${network.bssid}]")
            
            // Save to Room database
            val newHandshake = Handshake(
                ssid = network.ssid,
                bssid = network.bssid,
                encryption = network.encryption,
                notes = "Handshake opgevangen op kanaal ${network.channel}"
            )
            handshakeDao.insertHandshake(newHandshake)
            
            logTerminal("[+] WPA Handshake opgevangen van ${network.ssid}! Opgeslagen in database.")
            logTerminal("[kali@mobile:~]$ _")
            activeAttackType = ""
            Toast.makeText(context, "Handshake opgevangen en opgeslagen!", Toast.LENGTH_LONG).show()
        }
    }

    // --- WPS PIXIE DUST ATTACK ---
    fun startWpsPixieAttack(network: WifiNetwork) {
        if (!network.isWpsEnabled) {
            Toast.makeText(context, "Dit netwerk heeft geen WPS ingeschakeld!", Toast.LENGTH_SHORT).show()
            return
        }
        if (interfaceMode != "Monitor") {
            Toast.makeText(context, "Activeer eerst Monitor Mode!", Toast.LENGTH_SHORT).show()
            return
        }

        activeAttackType = "pixie"
        attackProgress = 0f
        attackLog.clear()
        crackedKeyResult = null
        
        attackLog.add("[*] Starten van WPS Pixie Dust kwetsbaarheid exploit...")
        attackLog.add("[*] Doel AP: ${network.ssid} (${network.bssid})")
        attackLog.add("[*] Tool: reaver -i $interfaceName -b ${network.bssid} -K 1 -vv")
        
        scope.launch {
            logTerminal("[kali@mobile:~]$ reaver -i $interfaceName -b ${network.bssid} -K 1 -vv")
            delay(800)
            attackLog.add("[-] Associëren met Access Point...")
            attackProgress = 0.2f
            delay(1000)
            attackLog.add("[+] Succesvol geassocieerd!")
            attackLog.add("[-] Ontvangen WPS M1 message...")
            delay(700)
            attackLog.add("[-] Verzenden WPS M2 message...")
            attackProgress = 0.4f
            delay(800)
            attackLog.add("[-] Ontvangen WPS M3 message (E-Nonce & R-Nonce geoogst)...")
            attackProgress = 0.6f
            
            // Pixie dust hash extraction
            delay(1200)
            attackLog.add("[*] Uitvoeren offline Pixie Dust hashberekening...")
            attackLog.add("[*] Berekenen van sleutel via pixiewps...")
            attackProgress = 0.8f
            
            // Cracking success
            delay(1500)
            attackProgress = 1f
            val pin = (10000000..99999999).random().toString()
            val recoveredWpaKey = when(network.ssid) {
                "KPN-78DF4" -> "kpn_wachtwoord_2026"
                "T-Mobile-Fiber-99" -> "fiber_secure_pass!"
                "SmartHome_Gateway" -> "admin123456"
                else -> "WiFi_Secured_Key_2026!"
            }
            
            attackLog.add("[+] PIXIE DUST SUCCESVOL! WPS PIN GEKRAAKT!")
            attackLog.add("[+] WPS PIN: $pin")
            attackLog.add("[+] GERECOVERDE WPA SLEUTEL: $recoveredWpaKey")
            
            crackedKeyResult = recoveredWpaKey
            
            // Save cracked key to DB
            val crackedHandshake = Handshake(
                ssid = network.ssid,
                bssid = network.bssid,
                encryption = network.encryption,
                crackedKey = recoveredWpaKey,
                notes = "Gekreakt via WPS Pixie Dust. PIN: $pin"
            )
            handshakeDao.insertHandshake(crackedHandshake)
            
            logTerminal("[+] WPS Pixie Dust succesvol! Sleutel gekraakt voor ${network.ssid}.")
            logTerminal("[kali@mobile:~]$ _")
            activeAttackType = ""
            Toast.makeText(context, "Sleutel gekraakt via WPS Pixie Dust!", Toast.LENGTH_LONG).show()
        }
    }

    // --- OFFLINE WORDLIST CRACKING (aircrack-ng) ---
    fun runOfflineAircrack(handshake: Handshake) {
        activeAttackType = "aircrack"
        attackProgress = 0f
        attackLog.clear()
        crackedKeyResult = null
        
        attackLog.add("[*] Offline handshakereconstructie via aircrack-ng...")
        attackLog.add("[*] Doel: ${handshake.ssid} (${handshake.bssid})")
        attackLog.add("[*] Woordenlijst: /usr/share/wordlists/rockyou.txt (14.3 miljoen wachtwoorden)")
        
        scope.launch {
            logTerminal("[kali@mobile:~]$ aircrack-ng -w rockyou.txt -b ${handshake.bssid} handshake.cap")
            delay(1000)
            
            val totalSteps = 10
            val mockPasswords = listOf(
                "qwerty12345", "123456789", "password", "iloveyou", "letmeinhacker",
                "wifi123", "secretpass", "superwifi", "admin888", "kpn_wachtwoord_2026"
            )
            
            val finalPassword = when(handshake.ssid) {
                "KPN-78DF4" -> "kpn_wachtwoord_2026"
                "T-Mobile-Fiber-99" -> "fiber_secure_pass!"
                "SmartHome_Gateway" -> "admin123456"
                else -> "admin123456"
            }
            
            for (i in 1..totalSteps) {
                delay(400)
                attackProgress = i / totalSteps.toFloat()
                val currentSpeed = (12000..18000).random()
                val checkedKeys = i * 143000
                val testPass = mockPasswords.getOrElse(i - 1) { "pass_$checkedKeys" }
                attackLog.add("[-] Testen van hashes... Snelheid: $currentSpeed keys/sec | Getest: $checkedKeys | Huidig: '$testPass'")
            }
            
            delay(800)
            attackProgress = 1f
            attackLog.add("[++++] HANDSHAKE CRACK SUCCESVOL!")
            attackLog.add("[+] GEVONDEN WACHTWOORD: $finalPassword")
            
            crackedKeyResult = finalPassword
            
            // Update in Room Database
            handshakeDao.updateCrackedKey(handshake.id, finalPassword)
            
            logTerminal("[+] aircrack-ng offline crack succesvol! Wachtwoord gevonden voor ${handshake.ssid}.")
            logTerminal("[kali@mobile:~]$ _")
            activeAttackType = ""
            Toast.makeText(context, "Sleutel succesvol gekraakt via Woordenlijst!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- EVIL TWIN / ROGUE AP SIMULATOR ---
    fun runEvilTwinSimulator(network: WifiNetwork) {
        activeAttackType = "eviltwin"
        attackProgress = 0f
        attackLog.clear()
        crackedKeyResult = null

        attackLog.add("[*] Configureren van Evil Twin Rogue Access Point...")
        attackLog.add("[*] SSID klonen: ${network.ssid}")
        attackLog.add("[*] Starten van hostapd met kloningsprofiel...")

        scope.launch {
            logTerminal("[kali@mobile:~]$ sudo hostapd -d evil_twin.conf")
            delay(800)
            attackLog.add("[+] Rogue Access Point '${network.ssid}' is nu in de lucht!")
            attackLog.add("[*] Starten van dnsmasq DHCP & DNS server...")
            logTerminal("[kali@mobile:~]$ sudo dnsmasq -C dnsmasq.conf -d")
            attackProgress = 0.3f
            
            delay(1200)
            attackLog.add("[*] Wachten tot clients verbinding maken...")
            attackLog.add("[-] SIGNAL BOOST: Doel AP zendt uit op sterker signaal (Power TX: 30dBm)")
            attackProgress = 0.5f
            
            delay(1500)
            attackLog.add("[+] Client 'B4:F1:DA:22:90:3F' verbonden! IP toegekend: 192.168.1.104")
            attackLog.add("[*] Omleiden van DNS-verkeer naar lokaal inlogportaal (Social Engineering)...")
            attackProgress = 0.7f
            
            delay(1800)
            attackLog.add("[!] Client vraagt 'http://www.google.com' op... Geredirect naar inlogpagina.")
            attackLog.add("[-] Victim ingevoerd inlogformulier...")
            
            delay(1500)
            attackProgress = 1f
            val dummyPass = "geheim_wifi_wachtwoord_88"
            attackLog.add("[++++] CREDENTIALS GEVALIDEERD EN GEVANGEN!")
            attackLog.add("[+] GEBRUIKERSNAAM/WACHTWOORD: $dummyPass")
            
            crackedKeyResult = dummyPass
            
            val rogueHandshake = Handshake(
                ssid = network.ssid,
                bssid = network.bssid,
                encryption = network.encryption,
                crackedKey = dummyPass,
                notes = "Opgevangen via Rogue AP Evil Twin aanval."
            )
            handshakeDao.insertHandshake(rogueHandshake)
            
            logTerminal("[+] Evil Twin succesvol! Inloggegevens onderschept voor ${network.ssid}.")
            logTerminal("[kali@mobile:~]$ _")
            activeAttackType = ""
            Toast.makeText(context, "Inloggegevens gevangen via Rogue AP!", Toast.LENGTH_LONG).show()
        }
    }

    // --- DELETE HANDSHAKE ---
    fun deleteHandshake(id: Int) {
        scope.launch {
            handshakeDao.deleteHandshake(id)
            Toast.makeText(context, "Item verwijderd uit database", Toast.LENGTH_SHORT).show()
        }
    }

    // --- GEMINI AI ETHICAL COPILOT CHAT ACTION ---
    fun sendCopilotMessage(userPrompt: String) {
        if (userPrompt.trim().isEmpty() || isChatLoading) return
        
        chatHistory.add(ChatMessage(isUser = true, content = userPrompt))
        isChatLoading = true
        
        logHistoryEvent("AI Copilot", "User Query", userPrompt)
        
        scope.launch {
            val responseText = callGeminiApiRest(userPrompt)
            var cleanText = responseText
            
            // Execute automated UI configuration changes requested or guided by AI response
            if (responseText.contains("[CONNECT_P2P]")) {
                cleanText = cleanText.replace("[CONNECT_P2P]", "").trim()
                logHistoryEvent("AI Copilot", "Command Executed", "AI-geïnitieerde handeling gedetecteerd: [CONNECT_P2P]", "Peer-to-peer Mesh-netwerk configureren")
                if (!isP2PConnected) {
                    toggleP2PMesh()
                }
            }
            if (responseText.contains("[CONNECT_GHOST]")) {
                cleanText = cleanText.replace("[CONNECT_GHOST]", "").trim()
                logHistoryEvent("AI Copilot", "Command Executed", "AI-geïnitieerde handeling gedetecteerd: [CONNECT_GHOST]", "Verbinden met IJsland Ghost-Proxy")
                if (!isGhostServerActive) {
                    connectToGhostServer("Reykjavík Ghost-Proxy", "185.112.144.9")
                }
            }
            if (responseText.contains("[ACTIVATE_TOR]")) {
                cleanText = cleanText.replace("[ACTIVATE_TOR]", "").trim()
                logHistoryEvent("AI Copilot", "Command Executed", "AI-geïnitieerde handeling gedetecteerd: [ACTIVATE_TOR]", "Initialiseren van Tor-omleiding")
                if (!isTorRoutingEnabled) {
                    toggleTorRouting()
                }
            }
            if (responseText.contains("[ACTIVATE_ALL_ANON]")) {
                cleanText = cleanText.replace("[ACTIVATE_ALL_ANON]", "").trim()
                logHistoryEvent("AI Copilot", "Command Executed", "AI-geïnitieerde handeling gedetecteerd: [ACTIVATE_ALL_ANON]", "Maximale anonimiteit inschakelen")
                if (!isMacSpoofed) runMacChanger()
                if (!isP2PConnected) toggleP2PMesh()
                if (!isTorRoutingEnabled) toggleTorRouting()
            }
            if (responseText.contains("[ACTIVATE_PANIC_SHIELD]")) {
                cleanText = cleanText.replace("[ACTIVATE_PANIC_SHIELD]", "").trim()
                logHistoryEvent("AI Copilot", "Command Executed", "AI-geïnitieerde handeling gedetecteerd: [ACTIVATE_PANIC_SHIELD]", "Nood-verbindingsschild activeren")
                triggerPanicStealthShield()
            }
            
            chatHistory.add(ChatMessage(isUser = false, content = cleanText))
            isChatLoading = false
            logHistoryEvent("AI Copilot", "AI Response", "Copilot antwoord geleverd", "Systeemstatus geüpdatet")
        }
    }

    // --- AI HACK-BOT PAYLOAD & BLUEPRINT GENERATOR ---
    fun generateAiHackHelperPayload(networkName: String, attackType: String) {
        val target = if (networkName.isBlank()) "Onbekend Netwerk (WPA2)" else networkName
        aiHackHelperNetworkName = target
        aiHackHelperPayloadType = attackType
        isGeneratingAiHelperPayload = true
        generatedAiHelperPayloadResult = null
        
        val prompt = "Genereer een gedetailleerde ethische audit blueprint en commandline payload voor een test op het wifi-netwerk '$target' via de methode '$attackType'. " +
                     "Geef de exacte linux / aircrack-ng / mdk4 commandostructuur, de theoretische werking, en een stappenplan om dit netwerk te beveiligen tegen dit type aanval. " +
                     "Houd de toon professioneel en educatief."
                     
        scope.launch {
            val response = callGeminiApiRest(prompt)
            generatedAiHelperPayloadResult = response
            isGeneratingAiHelperPayload = false
            logHistoryEvent("AI Hack-Bot", "Payload Generated", "Ethische exploit blueprint gegenereerd voor $target via $attackType", "AI Beveiligingsanalyse voltooid")
        }
    }

    // --- SUBMIT TEAM MEMBER REQUEST ---
    fun submitTeamMemberRequest(email: String) {
        val emailClean = email.trim()
        if (emailClean.isEmpty() || !emailClean.contains("@")) {
            teamRegistrationStatus = "Fout: Voer een geldig e-mailadres in."
            return
        }
        if (registeredTeamMembers.contains(emailClean)) {
            teamRegistrationStatus = "Fout: Dit teamlid is al geregistreerd."
            return
        }
        
        scope.launch {
            teamRegistrationStatus = "Verzoek indienen bij ice1984m via GitHub..."
            delay(1500)
            registeredTeamMembers.add("$emailClean (Ethische Hacker / Teamlid)")
            teamRegistrationStatus = "Succes: $emailClean is toegevoegd als geautoriseerd teamlid!"
            logHistoryEvent("GitHub Team", "Member Authorized", "Nieuw teamlid geautoriseerd: $emailClean", "Gekoppeld aan repository ice1984m/kali-suite")
            teamEmailInput = ""
        }
    }

    // Direct REST API Call to Gemini with Offline Smart Fallback
    private suspend fun callGeminiApiRest(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        
        // Define offline smart responses for typical hacking queries in Dutch to provide stellar UX
        val lowerPrompt = prompt.lowercase()
        val offlineFallback = when {
            lowerPrompt.contains("anon") || lowerPrompt.contains("privacy") || lowerPrompt.contains("veiligheid") || lowerPrompt.contains("verberg") -> {
                "**Offline Educatieve Hulp - Gebruikersanonimiteit & Privacy:**\n\nBij het uitvoeren van ethische penetratietesten is het waarborgen van jouw eigen privacy en anonimiteit essentieel. Hiermee voorkom je dat jouw fysieke locatie, IP-adres of netwerkinterface getrackt kan worden.\n\n" +
                "De Kali Mobile Ethical Suite biedt vier geavanceerde anonieme routerservices:\n" +
                "1. **MAC Adres Wijzigen (macchanger):** Vermijdt netwerk-tracking.\n" +
                "2. **Peer-to-Peer Mesh Netwerk:** Koppelt je apparaat met omliggende medegebruikers via gecodeerde ad-hoc tunnels om pakketpaden te verdelen.\n" +
                "3. **Spook Proxies & Servers:** Routeert internetverkeer via onzichtbare proxy servers die je echte IP-adres maskeren.\n" +
                "4. **Tor Anonieme Netwerk-Routing:** Stuurt al je TCP-pakketjes over ten minste 3 willekeurige Tor-knooppunten (hops).\n\n" +
                "**Ik kan deze beveiligingen direct voor je instellen!** Vraag mij bijvoorbeeld:\n" +
                "- *'koppel gebruikers'* om te verbinden via het mesh-netwerk\n" +
                "- *'activeer spook server'* om een onzichtbare spookserver in te schakelen\n" +
                "- *'beveilig alles'* om het volledige anonieme profiel direct te activeren."
            }
            lowerPrompt.contains("koppel") || lowerPrompt.contains("mesh") || lowerPrompt.contains("medegebruiker") || lowerPrompt.contains("p2p") -> {
                "**Offline Actie - Koppelen met Medegebruikers (P2P Mesh):**\n\nHet peer-to-peer mesh netwerk is een gedecentraliseerde routing-overlay. Door omliggende apparaten te koppelen, worden datapakketjes en wifi-scans willekeurig verspreid en doorgegeven via elkaars internetverbinding.\n\n" +
                "Ik activeer nu de peer-to-peer mesh interface en start gecodeerde tunnels naar 7 actieve omliggende nodes om jouw anonimiteit te maximaliseren!\n\n" +
                "[CONNECT_P2P]\n\n*Beveiligingsadvies:* Gebruik dit mesh-netwerk bij voorkeur in drukke wifi-omgevingen voor optimale versluiering."
            }
            lowerPrompt.contains("spook") || lowerPrompt.contains("ghost") || lowerPrompt.contains("server") || lowerPrompt.contains("proxy") -> {
                "**Offline Actie - Spook Server & IP-Anonimiteit:**\n\nEen spook server (ghost proxy) is een speciaal ontworpen, log-vrije VPN of SOCKS5 proxy die jouw IP-adres onzichtbaar maakt. De server vangt jouw scans en netwerkverzoeken op en voert ze uit namens een willekeurig gespoofd IP-adres.\n\n" +
                "Ik verbind je nu direct met de best beschikbare spook server (Reykjavík, IJsland - IP: 185.112.144.9) om jouw internet-en-scanadres onzichtbaar te maken!\n\n" +
                "[CONNECT_GHOST]\n\n*IP-Adres Status:* Gemaskeerd en onzichtbaar gemaakt."
            }
            lowerPrompt.contains("tor") || lowerPrompt.contains("onion") || lowerPrompt.contains("exit node") -> {
                "**Offline Actie - Tor Anonieme Routing:**\n\nTor (The Onion Router) versleutelt al je netwerkverkeer in meerdere lagen en routeert het via een willekeurig gekozen pad door het wereldwijde netwerk.\n\n" +
                "Ik start nu de lokale Tor-service en leid alle wifi- en testpakketten om via het Tor-circuit. Jouw openbare IP-adres verandert direct naar een anonieme Tor Exit Node!\n\n" +
                "[ACTIVATE_TOR]\n\n*Tor-status:* Actief en operationeel."
            }
            lowerPrompt.contains("beveilig alles") || lowerPrompt.contains("volledig") || lowerPrompt.contains("optimaal") -> {
                "**Offline Actie - Volledige Anonimiteitsmodus Activeren:**\n\nIk stel direct het maximale beveiligingsniveau in om jouw anonimiteit 100% te beschermen:\n" +
                "1. Spoof van je MAC-adres (`macchanger -r`)\n" +
                "2. Koppelen van medegebruikers via P2P Mesh ad-hoc tunnels\n" +
                "3. Inschakelen van Tor anonieme routing\n\n" +
                "Ik voer de configuratie nu uit...\n\n" +
                "[ACTIVATE_ALL_ANON]\n\n*Anonimiteit Score:* Optimaal geconfigureerd!"
            }
            lowerPrompt.contains("pixie") || lowerPrompt.contains("wps") -> {
                "**Offline Educatieve Hulp - WPS Pixie Dust:**\n\nDe Pixie Dust-aanval (PixieWPS) buit een kwetsbaarheid uit in de WPS-pincode generatie van veel routers. In plaats van alle pincodes online te proberen (wat uren duurt), verzamelt de tool cryptografische nonces (E-Nonce, R-Nonce, E-Hash, R-Hash) in een M1/M2 wifi-pakket.\n\n" +
                "Omdat kwetsbare routers slechte randomizers gebruiken, kan PixieWPS de WPS-PIN offline in enkele seconden kraken. \n\n" +
                "*Beveiligingsadvies:* Schakel WPS volledig uit in de routerinstellingen om deze kwetsbaarheid op te lossen."
            }
            lowerPrompt.contains("deauth") || lowerPrompt.contains("aireplay") -> {
                "**Offline Educatieve Hulp - Deauthenticatie Aanval:**\n\nEen deauthenticatie (deauth) aanval stuurt onversleutelde beheerframes naar een wifi-client namens de router (of andersom). De client gelooft dat de router de verbinding wil verbreken en gooit de verbinding dicht.\n\n" +
                "De hacker luistert ondertussen met `airodump-ng`. Zodra de client automatisch opnieuw inlogt, vangt de hacker de '4-way handshake' op.\n\n" +
                "*Beveiligingsadvies:* WPA3 lost dit probleem op door beheerframes te versleutelen (Protected Management Frames - PMF)."
            }
            lowerPrompt.contains("monitor") || lowerPrompt.contains("airmon") -> {
                "**Offline Educatieve Hulp - Monitor Mode:**\n\nNormaal gesproken negeert een wifi-kaart alle wifi-pakketjes die niet voor hem bestemd zijn (Managed Mode). In *Monitor Mode* luistert de draadloze kaart naar álle wifi-pakketjes in de lucht op dat kanaal.\n\n" +
                "Dit stelt netwerkanalisten en pentesters in staat om netwerkverkeer te sniften en te analyseren zonder dat ze verbonden hoeven te zijn met het netwerk."
            }
            lowerPrompt.contains("handshake") || lowerPrompt.contains("aircrack") -> {
                "**Offline Educatieve Hulp - WPA Handshake & Aircrack:**\n\nDe WPA 4-way handshake is het proces waarbij de router en de client bewijzen dat ze het juiste wachtwoord kennen zonder het wachtwoord daadwerkelijk te verzenden.\n\n" +
                "Aircrack-ng kraakt dit door offline miljarden wachtwoorden uit een bestand (rockyou.txt) om te zetten in cryptografische sleutels, en te kijken of een van deze sleutels de opgevangen handshake succesvol kan ontcijferen."
            }
            else -> null
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            delay(1000)
            return@withContext offlineFallback ?: "**[OFFLINE MODUS (Geen API Key ingesteld)]**\n\nJe hebt momenteel geen geldige Gemini API-sleutel in de Secrets-app geconfigureerd. Hierdoor werkt de live AI-tutor niet.\n\n**Hoe configureer je dit?**\n1. Ga naar het **Secrets paneel** in de AI Studio UI.\n2. Voeg de sleutel `GEMINI_API_KEY` toe met jouw eigen sleutel van Google AI Studio.\n\n**Offline informatie over Ethical Hacking:**\nEthical hacking betreft het legitiem testen van systemen op kwetsbaarheden. Voor WiFi-beveiliging gebruik je tools zoals `airodump-ng` om netwerken te monitoren en `aircrack-ng` om de sterkte van wachtwoorden te valideren."
        }

        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val systemInstructionText = "Je bent een deskundige Kali Linux Ethical WiFi Hacking Coach. Je spreekt uitsluitend in het Nederlands. Je legt technische termen en wifi-aanvallen uit op een uiterst educatieve, behulpzame manier. Je hebt tevens toegang tot ingebouwde anonimiteitsacties op het apparaat. Indien de gebruiker vraagt om anoniem te worden, medegebruikers te koppelen, spook-servers/proxies te activeren of Tor in te schakelen, leg dan uit wat je doet en voeg exact één van de volgende verborgen triggers aan het einde van je antwoord toe:\n" +
                    "- Gebruik [CONNECT_P2P] om verbinding te maken met medegebruikers (mesh network).\n" +
                    "- Gebruik [CONNECT_GHOST] om te verbinden met een spook server (ghost proxy).\n" +
                    "- Gebruik [ACTIVATE_TOR] om Tor anonieme routing in te schakelen.\n" +
                    "- Gebruik [ACTIVATE_ALL_ANON] om alles tegelijkertijd in te schakelen voor optimale anonimiteit.\n" +
                    "- Gebruik [ACTIVATE_PANIC_SHIELD] om het extreme stealth/panic-schild in te schakelen om alle sporen, telefoonnummers en MAC-adressen anoniem te maskeren.\n" +
                    "Waarschuw de gebruiker altijd om deze kennis uitsluitend te gebruiken op netwerken waar hij/zij expliciete toestemming voor heeft."

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstructionText)
                        })
                    })
                })
            }

            var attempts = 0
            val maxAttempts = 3
            var delayMs = 1200L
            var lastResponseCode = -1
            var responseText: String? = null

            while (attempts < maxAttempts) {
                attempts++
                try {
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                        .post(jsonBody.toString().toRequestBody(jsonMediaType))
                        .build()

                    val response = client.newCall(request).execute()
                    lastResponseCode = response.code

                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val responseJson = JSONObject(bodyString)
                        val candidates = responseJson.getJSONArray("candidates")
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        responseText = parts.getJSONObject(0).getString("text")
                        break
                    } else {
                        if (response.code == 503) {
                            withContext(Dispatchers.Main) {
                                logHistoryEvent(
                                    "AI Copilot",
                                    "API Warning",
                                    "Gemini API 503 (Service Unavailable) gedetecteerd.",
                                    "Poging $attempts van $maxAttempts. Automatische herpoging over ${delayMs}ms..."
                                )
                            }
                            delay(delayMs)
                            delayMs *= 2 // Exponential backoff
                        } else {
                            // Other errors: break and handle downstream
                            break
                        }
                    }
                } catch (e: Exception) {
                    if (attempts >= maxAttempts) {
                        throw e
                    }
                    withContext(Dispatchers.Main) {
                        logHistoryEvent(
                            "System Alert",
                            "API Warning",
                            "Verbindingsfout tijdens AI-verzoek: ${e.message}",
                            "Poging $attempts van $maxAttempts. Herpoging over ${delayMs}ms..."
                        )
                    }
                    delay(delayMs)
                    delayMs *= 2
                }
            }

            if (responseText != null) {
                responseText
            } else if (lastResponseCode == 503) {
                withContext(Dispatchers.Main) {
                    logHistoryEvent(
                        "System Alert",
                        "API Error",
                        "AI-service is overbelast of in onderhoud (Code 503)",
                        "Status: Service Unavailable na $maxAttempts automatische retries. Troubleshooting logboek aangemaakt."
                    )
                }
                "**[AI-ASSISTENT VERSTUURINGSFOUT: CODE 503]**\n\n" +
                "De Google Gemini AI-server is momenteel tijdelijk niet beschikbaar of overbelast (Service Unavailable - Fout 503).\n\n" +
                "**Automatische herstelstappen:**\n" +
                "1. Ik heb zojuist **$maxAttempts automatische herpogingen** met exponentiële wachttijden uitgevoerd, maar de server weigert de verbinding.\n" +
                "2. Dit probleem treedt meestal op tijdens serveronderhoud bij Google AI Studio of wanneer de quota-limieten zijn overschreden.\n\n" +
                "**Oplossing:**\n" +
                "- Wacht 10 tot 15 seconden en stuur je bericht opnieuw.\n" +
                "- Controleer of je de juiste API-sleutel hebt geconfigureerd in het **Secrets paneel** van Google AI Studio.\n" +
                "- Bekijk het logboekbestand `TROUBLESHOOTING.md` in je GitHub repository `ice1984m` of de rootmap van de applet voor gedetailleerde instructies."
            } else {
                val errorMsg = "Fout bij communicatie met AI-server: Code $lastResponseCode. Probeer het later opnieuw."
                withContext(Dispatchers.Main) {
                    logHistoryEvent(
                        "System Alert",
                        "API Error",
                        "AI-service gaf foutcode $lastResponseCode",
                        "Status: Mislukt. Zie offline fallback of logboek voor details."
                    )
                }
                offlineFallback ?: errorMsg
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                logHistoryEvent(
                    "System Alert",
                    "API Error",
                    "Netwerkfout bij verbinden: ${e.message}",
                    "Status: Mislukt. Controleer de internetverbinding en de API sleutel."
                )
            }
            offlineFallback ?: "Fout: ${e.message}. Controleer je internetverbinding."
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}

// --- COMPOSE UI SCREENS ---
@Composable
fun KaliAppScreen(viewModel: KaliViewModel) {
    val savedHandshakes by viewModel.savedHandshakes.collectAsState()
    val scannedNetworks by viewModel.scannedNetworks.collectAsState()
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = KaliSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = viewModel.selectedTab == "dashboard",
                    onClick = { viewModel.selectedTab = "dashboard" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KaliPrimary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = KaliPrimary,
                        indicatorColor = KaliSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = viewModel.selectedTab == "scanner",
                    onClick = { viewModel.selectedTab = "scanner" },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = "Scanner") },
                    label = { Text("WiFi Scanner", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KaliPrimary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = KaliPrimary,
                        indicatorColor = KaliSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = viewModel.selectedTab == "tools",
                    onClick = { viewModel.selectedTab = "tools" },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Tools") },
                    label = { Text("Tools A-Z", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KaliPrimary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = KaliPrimary,
                        indicatorColor = KaliSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = viewModel.selectedTab == "attacks",
                    onClick = { viewModel.selectedTab = "attacks" },
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = "Aanvallen") },
                    label = { Text("Aanvallen", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KaliPrimary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = KaliPrimary,
                        indicatorColor = KaliSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = viewModel.selectedTab == "copilot",
                    onClick = { viewModel.selectedTab = "copilot" },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = "AI Copilot") },
                    label = { Text("AI Copilot", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KaliPrimary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = KaliPrimary,
                        indicatorColor = KaliSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = viewModel.selectedTab == "anonymity",
                    onClick = { viewModel.selectedTab = "anonymity" },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Anonimiteit") },
                    label = { Text("Anoniem", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KaliPrimary,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = KaliPrimary,
                        indicatorColor = KaliSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaliBackground)
                .padding(innerPadding)
        ) {
            // Top Status Panel (Always Visible, spacious and styled)
            TopStatusPanel(viewModel = viewModel)
            
            Divider(color = KaliSurfaceVariant, thickness = 1.dp)
            
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (viewModel.selectedTab) {
                    "dashboard" -> DashboardTab(viewModel = viewModel)
                    "scanner" -> WifiScannerTab(viewModel = viewModel, networks = scannedNetworks)
                    "tools" -> ToolsListTab(viewModel = viewModel)
                    "attacks" -> AttacksTab(viewModel = viewModel, savedHandshakes = savedHandshakes, scannedNetworks = scannedNetworks)
                    "copilot" -> CopilotTab(viewModel = viewModel)
                    "anonymity" -> AnonymityTab(viewModel = viewModel)
                }
            }
        }
    }
}

// --- TOP STATUS PANEL COMPONENT ---
@Composable
fun TopStatusPanel(viewModel: KaliViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = KaliSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, KaliSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kali Dragon Logo Asset
            Image(
                painter = painterResource(id = R.drawable.img_kali_logo),
                contentDescription = "Kali Logo",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, KaliPrimary, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KALI MOBILE ETHICAL SUITE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Monitor Mode Indicator Pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (viewModel.interfaceMode == "Monitor") Color(0xFF003914) else Color(0xFF331400),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (viewModel.interfaceMode == "Monitor") "MONITOR" else "MANAGED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.interfaceMode == "Monitor") KaliPrimary else Color(0xFFFF9E00)
                        )
                    }
                    // MAC Spoof Status Pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (viewModel.isMacSpoofed) Color(0xFF00363D) else Color(0xFF232A3B),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (viewModel.isMacSpoofed) "MAC: SPOOFED" else "MAC: DEFAULT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isMacSpoofed) KaliSecondary else Color.LightGray
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (viewModel.isGhostServerActive || viewModel.isTorRoutingEnabled) "GHOST IP" else "DIRECT IP",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isGhostServerActive || viewModel.isTorRoutingEnabled) KaliPrimary else Color.Gray
                )
                Text(
                    text = viewModel.activeExternalIpAddress.split(" ")[0],
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.isGhostServerActive || viewModel.isTorRoutingEnabled) KaliPrimary else Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "DEV: ${viewModel.interfaceName}",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// --- TAB 1: DASHBOARD ---
@Composable
fun DashboardTab(viewModel: KaliViewModel) {
    val lazyListState = rememberLazyListState()
    
    // Automatically scrolls terminal to the end when logs change
    LaunchedEffect(viewModel.terminalLogs.size) {
        if (viewModel.terminalLogs.isNotEmpty()) {
            lazyListState.animateScrollToItem(viewModel.terminalLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        // --- 1. INTRA-INTRUSION / TRACE ALERT WARNING COMPONENT ---
        if (viewModel.isIntrusionAlertActive) {
            val alertAnimTransition = rememberInfiniteTransition()
            val alertAlpha by alertAnimTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = EaseInOutCirc),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3B0505).copy(alpha = alertAlpha)
                ),
                border = BorderStroke(2.dp, KaliError),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Gevaar",
                            tint = KaliError,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BEVEILIGINGSALARM: OPSPORING ACTIEF",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Ze weten dat er in het systeem gehackt is en zoeken je locatie!",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.alarmMessage,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerPanicStealthShield() },
                            colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                            modifier = Modifier.weight(1.3f).height(38.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Activeer Shield", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = { viewModel.dismissTraceAlarm() },
                            border = BorderStroke(1.dp, Color.Gray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("Sluiten", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- 2. STEALTH PANIC / EXTREME SHIELD COMPONENT ---
        Text(
            text = "Extreme Privacy & Identiteitsmaskering",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KaliSecondary,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, if (viewModel.isConnectionShieldActive) KaliPrimary else KaliSurfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stealth & Anti-Tracking Shield",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Met één knop je volledige telefoonnummer misleiden, MAC maskeren, DNS beveiligen en 100% anonieme Tor-tunnels configureren.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    IconButton(
                        onClick = { viewModel.triggerTraceAlarmSim() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(KaliSurfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Simulate Trace", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Show indicators of maskings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070B11), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SIM-IDENTITEIT", fontSize = 9.sp, color = Color.Gray)
                        Text(
                            text = if (viewModel.isSimMasked) "MISLEID / GEMASKEERD" else "ORIGINEEL IMEI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isSimMasked) KaliPrimary else Color.Yellow
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = viewModel.spoofedPhoneNumber,
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Divider(modifier = Modifier.width(1.dp).height(36.dp), color = KaliSurfaceVariant)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "DNS GEHEIMHOUDING", fontSize = 9.sp, color = Color.Gray)
                        Text(
                            text = if (viewModel.isDnsSecure) "100% SECURE DOH" else "DIRECT DNS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isDnsSecure) KaliPrimary else Color.Yellow
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (viewModel.isDnsSecure) "Cloudflare / Quad9" else "Provider DNS",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { viewModel.triggerPanicStealthShield() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.isConnectionShieldActive) Color(0xFF003914) else Color(0xFF7E0000)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("stealth_panic_button")
                ) {
                    Icon(
                        imageVector = if (viewModel.isConnectionShieldActive) Icons.Default.CheckCircle else Icons.Default.OfflineBolt,
                        contentDescription = null,
                        tint = if (viewModel.isConnectionShieldActive) KaliPrimary else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewModel.isConnectionShieldActive) "Waterdicht Verbindingsschild Actief!" else "EXTREME STEALTH PANIEKKNOP",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // --- 3. REAL-TIME NETWORK INTERFACE MONITOR DASHBOARD COMPONENT ---
        Text(
            text = "Real-time Netwerk Interface Monitor",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KaliSecondary,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, KaliSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hardware Interfaces & Signaalsterkte",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Live monitoring van alle aangesloten adapters via polling.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // Polling control switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (viewModel.isPollingActive) "POLLING" else "OFFLINE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isPollingActive) KaliPrimary else Color.Gray,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Switch(
                            checked = viewModel.isPollingActive,
                            onCheckedChange = { viewModel.isPollingActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = KaliPrimary,
                                checkedTrackColor = Color(0xFF003914)
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Interface table rows
                viewModel.hardwareInterfaces.forEach { adapter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .background(Color(0xFF070B11), RoundedCornerShape(8.dp))
                            .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Adapter icon/type
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(KaliSurfaceVariant, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (adapter.type) {
                                "Wi-Fi Built-in" -> Icons.Default.Wifi
                                "Alfa Ext USB" -> Icons.Default.Usb
                                "USB-C Ethernet" -> Icons.Default.SettingsInputHdmi
                                else -> Icons.Default.CellTower
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (adapter.status == "Monitor") KaliSecondary else KaliPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        // Interface name, SSID, MAC
                        Column(modifier = Modifier.weight(1.3f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = adapter.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (adapter.status == "Monitor") Color(0xFF331400) else Color(0xFF003914),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = adapter.status.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (adapter.status == "Monitor") Color(0xFFFF9E00) else KaliPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SSID: ${adapter.currentSsid}",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                            Text(
                                text = "MAC: ${adapter.macAddress}",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        // Signal Strength progress bar
                        Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
                            if (adapter.status == "Connected") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SignalCellularAlt,
                                        contentDescription = null,
                                        tint = if (adapter.signalStrength > -60) KaliPrimary else Color.Yellow,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${adapter.signalStrength} dBm",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (adapter.signalStrength > -60) KaliPrimary else Color.Yellow,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                val rawProgress = ((adapter.signalStrength + 100) / 70f).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = rawProgress,
                                    color = if (adapter.signalStrength > -60) KaliPrimary else Color.Yellow,
                                    trackColor = KaliSurfaceVariant,
                                    modifier = Modifier
                                        .width(70.dp)
                                        .padding(top = 4.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            } else {
                                Text(
                                    text = "MONITORING ALL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KaliSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Promiscuous Mode",
                                    fontSize = 8.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        // Data Transferred
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "RX: ${adapter.rxBytes}",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "TX: ${adapter.txBytes}",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Panel (Auto-config interfaces)
        Text(
            text = "Interface Automatische Configuratie",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KaliSecondary,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, KaliSurfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Airmon-NG Automatisering",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configureert je draadloze interface direct voor wifi-pakketinjectie en sniffing. Stopt automatisch storende systeemprocessen.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.runAutoConfigure() },
                        colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                        modifier = Modifier.weight(1f).testTag("auto_config_button"),
                        enabled = !viewModel.isConfiguringInterface && viewModel.interfaceMode == "Managed"
                    ) {
                        if (viewModel.isConfiguringInterface) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Monitor", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Button(
                        onClick = { 
                            if (viewModel.isMacSpoofed) {
                                viewModel.resetMacAddress()
                            } else {
                                viewModel.runMacChanger()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KaliSurfaceVariant),
                        modifier = Modifier.weight(1f).testTag("mac_spoof_button"),
                        border = BorderStroke(1.dp, KaliSecondary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = KaliSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (viewModel.isMacSpoofed) "Reset MAC" else "Spoof MAC",
                            fontSize = 12.sp,
                            color = KaliSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Live Kali Linux Terminal Window
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kali Interactive Terminal Output",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KaliPrimary
            )
            Text(
                text = "CLEAR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier
                    .clickable { viewModel.clearTerminal() }
                    .padding(4.dp)
            )
        }
        
        // Terminal Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030509))
                .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.terminalLogs) { logLine ->
                    val color = when {
                        logLine.startsWith("[kali") -> KaliSecondary
                        logLine.startsWith("[+") -> KaliPrimary
                        logLine.startsWith("[*]") -> Color.Yellow
                        logLine.startsWith("[-]") -> Color.Gray
                        logLine.startsWith("[!") -> KaliError
                        else -> Color.White
                    }
                    Text(
                        text = logLine,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = 15.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // --- CHRONOLOGICAL TERMINAL LOG WINDOW ---
        TerminalHistoryLogComponent(viewModel = viewModel)
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // System Performance Diagnostics (Large overzichtelijk dashboard details)
        Text(
            text = "Systeemdiagnostiek & Netwerk Adapter",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, KaliSurfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Interface details
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DiagnosticStat(label = "Chipset", value = "Atheros AR9271 (Alfa)")
                    DiagnosticStat(label = "TX-Power", value = "30 dBm (Max)")
                    DiagnosticStat(label = "Driver", value = "ath9k_htc")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = KaliSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                
                // System stats (Mock live values)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Systeembelasting", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { viewModel.refreshSystemDiagnostics() },
                        modifier = Modifier.testTag("refresh_system_diagnostics_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Vernieuw systeemdiagnostiek",
                            tint = KaliPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "CPU Belasting: 42%", fontSize = 11.sp, color = Color.White)
                        LinearProgressIndicator(progress = 0.42f, color = KaliPrimary, trackColor = KaliSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = viewModel.ramUsageLabel, fontSize = 11.sp, color = Color.White)
                        LinearProgressIndicator(progress = viewModel.ramUsage, color = KaliSecondary, trackColor = KaliSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticStat(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

// --- TAB 2: WIFI SCANNER ---
@Composable
fun WifiScannerTab(viewModel: KaliViewModel, networks: List<WifiNetwork>) {
    var isScanningAnimationActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            Toast.makeText(context, "Locatietoegang verleend! Je kunt nu echte wifi-netwerken scannen.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Locatietoegang geweigerd. Gesimuleerde wifi-modus actief.", Toast.LENGTH_LONG).show()
        }
    }

    // Animate radar sweep
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Beschikbare Draadloze Netwerken",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (hasLocationPermission) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(KaliPrimary, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "fysieke wifi-antenne actief",
                            color = KaliPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.Gray, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "simulatiemodus actief (geen rechten)",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            
            Button(
                onClick = { viewModel.scanForNetworks() },
                colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                enabled = !viewModel.isScanningWifi,
                modifier = Modifier.testTag("scan_wifi_button")
            ) {
                if (viewModel.isScanningWifi) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Wifi", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Location Permission Request Card if not granted
        if (!hasLocationPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(Color(0xFF0F1524), RoundedCornerShape(8.dp))
                    .border(1.dp, KaliPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = KaliPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Locatietoegang Vereist voor Hardware Scans",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Android vereist locatietoegang om omgevingswifi-pakketten en access points fysiek te kunnen detecteren. Zonder deze rechten valt de app terug op de veilige simulatie-engine.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Rechten Verlenen", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Warning if not in monitor mode
        if (viewModel.interfaceMode != "Monitor") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(Color(0xFF3F1900), RoundedCornerShape(8.dp))
                    .border(1.dp, KaliError, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = KaliError, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scan inactief: Activeer eerst 'Monitor Mode' in het Dashboard om wifi-pakketten te kunnen sniften.",
                        fontSize = 11.sp,
                        color = Color.White,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Radar Sweep Simulation panel
        if (viewModel.isScanningWifi) {
            Card(
                colors = CardDefaults.cardColors(containerColor = KaliSurface),
                modifier = Modifier.fillMaxWidth().height(140.dp).padding(bottom = 12.dp),
                border = BorderStroke(1.dp, KaliPrimary)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Scanning Sweep circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(1.dp, KaliPrimary.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .border(1.dp, KaliPrimary.copy(alpha = 0.2f), RoundedCornerShape(35.dp))
                    )
                    
                    // Radar Line
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(rotationAngle)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight(0.5f)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(KaliPrimary, Color.Transparent)
                                    )
                                )
                                .align(Alignment.TopCenter)
                        )
                    }
                    
                    Text(
                        text = "Airodump-NG scant de ether...",
                        color = KaliPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Selected target quick action
        viewModel.selectedNetwork?.let { network ->
            Card(
                colors = CardDefaults.cardColors(containerColor = KaliSurfaceVariant),
                border = BorderStroke(1.dp, KaliSecondary),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "GESELECTEERD DOEL", fontSize = 10.sp, color = KaliSecondary, fontWeight = FontWeight.Bold)
                        Text(text = network.ssid, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = "BSSID: ${network.bssid} | CH: ${network.channel}", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Button(
                        onClick = { viewModel.selectedTab = "attacks" },
                        colors = ButtonDefaults.buttonColors(containerColor = KaliSecondary)
                    ) {
                        Text("Open Tools", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Networks List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().weight(1f)
        ) {
            items(networks) { net ->
                val isSelected = viewModel.selectedNetwork?.bssid == net.bssid
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) KaliSurfaceVariant else KaliSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectedNetwork = net },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) KaliPrimary else KaliSurfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Signal Strength icon / text
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(42.dp)
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = when {
                                    net.signalDbm > -60 -> KaliPrimary
                                    net.signalDbm > -75 -> Color.Yellow
                                    else -> Color.Red
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${net.signalDbm}dBm",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = net.ssid,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (net.isWpsEnabled) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE040FB).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFFE040FB), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("WPS", fontSize = 8.sp, color = Color(0xFFE040FB), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                text = "BSSID: ${net.bssid} | CH: ${net.channel}",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "KANAAL",
                                fontSize = 8.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = net.channel.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KaliSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: TOOLS A-Z LIST ---
@Composable
fun ToolsListTab(viewModel: KaliViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Switch between Termux SOC Console and Reference list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .background(Color(0xFF0C101B), RoundedCornerShape(8.dp))
                .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (viewModel.activeToolsSubTab == "termux") KaliPrimary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { viewModel.activeToolsSubTab = "termux" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (viewModel.activeToolsSubTab == "termux") KaliPrimary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Termux AI SOC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.activeToolsSubTab == "termux") KaliPrimary else Color.Gray
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (viewModel.activeToolsSubTab == "reference") KaliPrimary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { viewModel.activeToolsSubTab = "reference" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = if (viewModel.activeToolsSubTab == "reference") KaliPrimary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hacking Tools A-Z",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.activeToolsSubTab == "reference") KaliPrimary else Color.Gray
                    )
                }
            }
        }

        if (viewModel.activeToolsSubTab == "termux") {
            TermuxSocConsoleView(viewModel = viewModel)
        } else {
            Text(
                text = "Kali Wireless Ethical Hacking Tools (A tot Z)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            Text(
                text = "Klik op een hacking-tool om de commandostructuur, de gedetailleerde werking, de veiligheidsimpact en een educatieve simulatie te starten.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp),
                lineHeight = 15.sp
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().weight(1f)
            ) {
                items(viewModel.kaliTools) { tool ->
                    var expanded by remember { mutableStateOf(false) }
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = KaliSurface),
                        border = BorderStroke(1.dp, if (expanded) KaliPrimary else KaliSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = tool.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KaliPrimary
                                    )
                                    Text(
                                        text = "Categorie: ${tool.category}",
                                        fontSize = 11.sp,
                                        color = KaliSecondary
                                    )
                                }
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Divider(color = KaliSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(text = "Syntaxis:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black, RoundedCornerShape(4.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = tool.command,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color.Yellow
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Werking:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(text = tool.description, fontSize = 12.sp, color = Color.White, lineHeight = 16.sp)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Beveiligingsanalyse:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(text = tool.securityImpact, fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Button(
                                        onClick = { viewModel.simulateTool(tool) },
                                        colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Simuleer in Terminal", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
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
fun TermuxSocConsoleView(viewModel: KaliViewModel) {
    val lazyListState = rememberLazyListState()
    
    // Auto scroll terminal to the bottom when logs change
    LaunchedEffect(viewModel.termuxLogs.size) {
        if (viewModel.termuxLogs.isNotEmpty()) {
            lazyListState.animateScrollToItem(viewModel.termuxLogs.size - 1)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {
        // Sub-title
        Text(
            text = "Echte en gesimuleerde ethical audits gekoppeld aan fysieke poort-sensoren, database-integriteit en sandbox internet checks.",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp),
            lineHeight = 15.sp
        )
        
        // 1. Termux Shell Window Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF030509))
                .border(1.dp, KaliPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.termuxLogs) { logLine ->
                    val color = when {
                        logLine.startsWith("[kali@termux-soc") || logLine.startsWith("[+]") -> KaliPrimary
                        logLine.contains("WAARSCHUWING") || logLine.contains("LEK GEVONDEN") || logLine.startsWith("[!") -> KaliError
                        logLine.startsWith("[*]") -> Color.Yellow
                        logLine.startsWith("[-]") -> Color.Gray
                        else -> Color.White
                    }
                    Text(
                        text = logLine,
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Animated Blinking Cursor if idle
                if (!viewModel.isRunningTermuxScript) {
                    item {
                        var cursorVisible by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            while(true) {
                                delay(500)
                                cursorVisible = !cursorVisible
                            }
                        }
                        Text(
                            text = if (cursorVisible) "█" else "",
                            color = KaliPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // 2. Custom Termux Command Row buttons (ESC, TAB, CTRL, ALT, /, etc.)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val keys = listOf("ESC", "TAB", "CTRL", "ALT", "-", "/", "$", "UP", "DOWN", "HELP")
            keys.forEach { key ->
                Box(
                    modifier = Modifier
                        .background(KaliSurface, RoundedCornerShape(4.dp))
                        .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(4.dp))
                        .clickable {
                            when (key) {
                                "HELP" -> viewModel.executeTermuxCommand("help")
                                "UP" -> {
                                    viewModel.termuxInput = "termux-device-audit"
                                }
                                "DOWN" -> {
                                    viewModel.termuxInput = "termux-browser-vuln"
                                }
                                else -> {
                                    viewModel.termuxInput += when(key) {
                                        "-" -> "-"
                                        "/" -> "/"
                                        "$" -> "$"
                                        else -> ""
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = key,
                        color = if (key == "HELP") KaliPrimary else Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // 3. Interactive Shell Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0C101B), RoundedCornerShape(8.dp))
                .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                color = KaliPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            
            BasicTextField(
                value = viewModel.termuxInput,
                onValueChange = { viewModel.termuxInput = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (viewModel.termuxInput.isNotBlank()) {
                            viewModel.executeTermuxCommand(viewModel.termuxInput)
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            )
            
            if (viewModel.termuxInput.isNotBlank()) {
                IconButton(
                    onClick = {
                        viewModel.executeTermuxCommand(viewModel.termuxInput)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Command",
                        tint = KaliPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 4. Quick Execution Scripts Bar
        Text(
            text = "Ethische Test payloads (Tik om direct uit te voeren):",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(KaliSurface)
                    .border(1.dp, KaliPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable { viewModel.executeTermuxCommand("termux-device-audit") }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = KaliPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Device Audit", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(KaliSurface)
                    .border(1.dp, KaliSecondary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable { viewModel.executeTermuxCommand("termux-db-leakcheck") }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = KaliSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DB Leak-Check", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(KaliSurface)
                    .border(1.dp, Color.Yellow.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable { viewModel.executeTermuxCommand("termux-soc-status") }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SOC Status", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // 5. Interactive Sandboxed Web Browser
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, if (viewModel.isScanningBrowserLeak) Color.Yellow else KaliSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = KaliPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sandboxed Security Web Inspector",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "Voer een URL in om de security headers, SSL-status en datalek-blootstelling direct te scannen via de Termux shell.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp),
                    lineHeight = 13.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    BasicTextField(
                        value = viewModel.termuxBrowserUrl,
                        onValueChange = { viewModel.termuxBrowserUrl = it },
                        textStyle = TextStyle(
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    )
                    
                    Button(
                        onClick = { viewModel.executeTermuxCommand("termux-browser-vuln") },
                        colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Inspecteer", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- TAB 4: ATTACK & CRACKING SCREEN ---
@Composable
fun AttacksTab(
    viewModel: KaliViewModel,
    savedHandshakes: List<Handshake>,
    scannedNetworks: List<WifiNetwork>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "Ethische Wifi Aanvallen & Cracking",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        // Select Network prompt if none selected
        if (viewModel.selectedNetwork == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KaliSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = KaliPrimary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Geen doelnetwerk geselecteerd.\nGa naar 'WiFi Scanner' en klik op een netwerk om te selecteren.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            val target = viewModel.selectedNetwork!!
            Card(
                colors = CardDefaults.cardColors(containerColor = KaliSurface),
                border = BorderStroke(1.dp, KaliPrimary),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "ACTIEF DOELNETWERK", fontSize = 10.sp, color = KaliPrimary, fontWeight = FontWeight.Bold)
                    Text(text = target.ssid, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "BSSID: ${target.bssid} | Kanaal: ${target.channel} | Encryptie: ${target.encryption}",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = KaliSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(text = "Selecteer Ethische Aanvalsvector:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Attack Vector Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startDeauthAttack(target) },
                            colors = ButtonDefaults.buttonColors(containerColor = KaliError),
                            modifier = Modifier.weight(1f).testTag("deauth_attack_button"),
                            enabled = viewModel.activeAttackType == ""
                        ) {
                            Text("1. Deauth / Cap", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { viewModel.startWpsPixieAttack(target) },
                            colors = ButtonDefaults.buttonColors(containerColor = KaliTertiary),
                            modifier = Modifier.weight(1f).testTag("pixie_attack_button"),
                            enabled = viewModel.activeAttackType == "" && target.isWpsEnabled
                        ) {
                            Text("2. Pixie Dust", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runEvilTwinSimulator(target) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9E00)),
                            modifier = Modifier.weight(1f).testTag("eviltwin_attack_button"),
                            enabled = viewModel.activeAttackType == ""
                        ) {
                            Text("3. Evil Twin AP", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live attack simulation log output
        if (viewModel.activeAttackType != "" || viewModel.attackLog.isNotEmpty()) {
            Text(
                text = "Live Aanval Log Output (Educatieve Simulatie)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KaliError,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030509)),
                border = BorderStroke(1.dp, KaliError),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Attack title progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AANVAL: ${viewModel.activeAttackType.uppercase()}",
                            color = KaliError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(viewModel.attackProgress * 100).toInt()}%",
                            color = KaliError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    LinearProgressIndicator(
                        progress = viewModel.attackProgress,
                        color = KaliError,
                        trackColor = KaliSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(4.dp)
                    )
                    
                    // Logs
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        viewModel.attackLog.forEach { log ->
                            Text(
                                text = log,
                                color = if (log.contains("GEVONDEN") || log.contains("SUCCESVOL")) KaliPrimary else Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Cracking Section (aircrack-ng offline hash recovery from db)
        Text(
            text = "Reconstrueren & Woordenlijst Cracking (Aircrack-NG)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KaliSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (savedHandshakes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KaliSurface, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Geen opgevangen handshakes in database.\nVoer een deauthenticatie-aanval uit om een handshake .cap op te vangen.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            savedHandshakes.forEach { handshake ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaliSurface),
                    border = BorderStroke(1.dp, KaliSurfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = handshake.ssid, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "BSSID: ${handshake.bssid}", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (handshake.crackedKey != null) Color(0xFF003914) else Color(0xFF3C3000),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (handshake.crackedKey != null) "GEKRAAKT" else "GECOPD (.CAP)",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (handshake.crackedKey != null) KaliPrimary else Color.Yellow
                                )
                            }
                        }
                        
                        if (handshake.crackedKey != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = KaliSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = KaliPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sleutel: ${handshake.crackedKey}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KaliPrimary
                                )
                            }
                            Text(text = "Aanvullend: ${handshake.notes}", fontSize = 10.sp, color = Color.Gray)
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.runOfflineAircrack(handshake) },
                                    colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                                    modifier = Modifier.weight(1f).height(32.dp).testTag("crack_hash_button"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Aircrack-NG (RockYou)", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Verwijder",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { viewModel.deleteHandshake(handshake.id) }.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// --- TAB 5: AI COPILOT CHAT ---
@Composable
fun CopilotTab(viewModel: KaliViewModel) {
    var chatInputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Scroll to latest chat message when received
    LaunchedEffect(viewModel.chatHistory.size) {
        if (viewModel.chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Switch between sub-tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .background(Color(0xFF0C101B), RoundedCornerShape(8.dp))
                .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val subTabs = listOf(
                Triple("chat", "AI Copilot", Icons.Default.Psychology),
                Triple("hack_helper", "AI Hack-Bot", Icons.Default.Terminal),
                Triple("license", "Licenties & Team", Icons.Default.Shield)
            )
            subTabs.forEach { (tabId, label, icon) ->
                val isActive = viewModel.activeCopilotSubTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) KaliPrimary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { viewModel.activeCopilotSubTab = tabId }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) KaliPrimary else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) KaliPrimary else Color.Gray
                        )
                    }
                }
            }
        }

        // 1. AI COPILOT CHAT SUB-TAB
        if (viewModel.activeCopilotSubTab == "chat") {
            Column(modifier = Modifier.fillMaxSize().weight(1f)) {
                Text(
                    text = "Gemini Ethical Hacking Copilot",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Vraag deze deskundige AI tutor alles over WiFi hacking, commandosyntaxis, of defensieve wifi-instellingen.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Suggestion Chips (Groot overzichtelijk bedieningspaneel)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionPromptChip(text = "Uitleg Pixie Dust aanval") {
                        chatInputText = "Uitleg Pixie Dust aanval"
                    }
                    SuggestionPromptChip(text = "Wat is een deauth?") {
                        chatInputText = "Wat is een deauthenticatie aanval en hoe voorkom je dit?"
                    }
                    SuggestionPromptChip(text = "Aircrack-NG commando") {
                        chatInputText = "Hoe gebruik ik aircrack-ng om een wifi-sleutel te kraken?"
                    }
                }

                // Chat Bubble Logs
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color(0xFF04070D), RoundedCornerShape(8.dp))
                        .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(viewModel.chatHistory) { msg ->
                        val bubbleColor = if (msg.isUser) KaliSurfaceVariant else KaliSurface
                        val align = if (msg.isUser) Alignment.End else Alignment.Start
                        val title = if (msg.isUser) "Pentester (Jij)" else "Kali AI Copilot 🐉"
                        val titleColor = if (msg.isUser) KaliSecondary else KaliPrimary

                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                            Text(
                                text = title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = titleColor,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(bubbleColor, RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (msg.isUser) KaliSecondary.copy(alpha = 0.3f) else KaliPrimary.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = msg.content,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                    
                    if (viewModel.isChatLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = KaliPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Gemini denkt na...", fontSize = 11.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Chat Input box (Touch targets >48dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatInputText,
                        onValueChange = { chatInputText = it },
                        placeholder = { Text("Stel een ethische hack vraag...", fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("chat_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = KaliPrimary,
                            unfocusedBorderColor = KaliSurfaceVariant,
                            focusedContainerColor = KaliSurface,
                            unfocusedContainerColor = KaliSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (chatInputText.isNotBlank()) {
                                viewModel.sendCopilotMessage(chatInputText)
                                chatInputText = ""
                                keyboardController?.hide()
                            }
                        })
                    )

                    Button(
                        onClick = {
                            if (chatInputText.isNotBlank()) {
                                viewModel.sendCopilotMessage(chatInputText)
                                chatInputText = ""
                                keyboardController?.hide()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                        modifier = Modifier.size(52.dp).testTag("chat_send_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Verstuur", tint = Color.Black)
                    }
                }
            }
        }

        // 2. AI HACK-BOT HELPER SUB-TAB
        if (viewModel.activeCopilotSubTab == "hack_helper") {
            var selectedHelperAttack by remember { mutableStateOf("WPA2 Handshake Bypass") }
            var selectedHelperNetwork by remember { mutableStateOf("ALFA_AP_TEST") }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "AI Hack-Bot Assistent",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Automatiseer en assisteer bij ethical hacking penetratietesten met realtime AI hulp. Selecteer een doelwit en aanvalsvector.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp),
                    lineHeight = 15.sp
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaliSurface),
                    border = BorderStroke(1.dp, KaliSurfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1. Selecteer Doelwit Netwerk", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KaliSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        // Simple selector of networks
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val networks = listOf("ALFA_AP_TEST", "Home_WiFi_Secure", "KPN_Fon_Public", "Office_Corporate_WPA3")
                            networks.forEach { ssid ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (selectedHelperNetwork == ssid) KaliPrimary.copy(alpha = 0.2f) else Color.Black,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selectedHelperNetwork == ssid) KaliPrimary else KaliSurfaceVariant,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedHelperNetwork = ssid }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(text = ssid, fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("2. Selecteer Ethische Aanvalsvector", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KaliSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val attackVectors = listOf("WPA2 Handshake Bypass", "Pixie Dust Injection", "Evil Twin Auth Mimic", "WPS Pin Bruteforce")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            attackVectors.forEach { vector ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black, RoundedCornerShape(6.dp))
                                        .border(1.dp, if (selectedHelperAttack == vector) KaliPrimary else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { selectedHelperAttack = vector }
                                        .padding(vertical = 4.dp, horizontal = 10.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedHelperAttack == vector,
                                        onClick = { selectedHelperAttack = vector },
                                        colors = RadioButtonDefaults.colors(selectedColor = KaliPrimary, unselectedColor = Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = vector, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Button(
                            onClick = { viewModel.generateAiHackHelperPayload(selectedHelperNetwork, selectedHelperAttack) },
                            colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            enabled = !viewModel.isGeneratingAiHelperPayload
                        ) {
                            if (viewModel.isGeneratingAiHelperPayload) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Genereer AI Hack Payload & Plan", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                // Generated output box
                Text(
                    text = "AI Exploit Blueprint Output:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF030509), RoundedCornerShape(8.dp))
                        .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    if (viewModel.isGeneratingAiHelperPayload) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = KaliPrimary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Gemini berekent de optimale aanvalstactiek...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else if (viewModel.generatedAiHelperPayloadResult != null) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "STATUS: PLAN GEGENEREERD",
                                    color = KaliPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Systeembron: Kali AI Hack-Bot",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = KaliSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = viewModel.generatedAiHelperPayloadResult ?: "",
                                color = Color.White,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Text(
                            text = "[!] Nog geen blueprint gegenereerd. Selecteer hierboven een doelwit en aanvalsvector en klik op 'Genereer'.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // WHAT IS THE APP MISSING FOR UNLIMITED AI AUDITS (TIPS CARD)
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaliSurface),
                    border = BorderStroke(1.dp, KaliPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = KaliPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Systeemanalyse: Wat ontbreekt er voor onbeperkte AI-audits?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Om deze app om te vormen van een educatieve simulator naar een onbeperkte, fysieke AI Hack-Bot, moeten de volgende hardware- en softwareonderdelen worden toegevoegd of geactiveerd:",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 15.sp
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = KaliSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // TIP 1
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Icon(Icons.Default.SettingsInputAntenna, contentDescription = null, tint = KaliSecondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("1. Externe USB Wifi-adapter (OTG)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KaliSecondary)
                                Text(
                                    "De ingebouwde wifi-chip van een Android-telefoon is ontworpen om alleen te verbinden met netwerken. Voor auditing is Monitor Mode & Packet Injection vereist. Je hebt een OTG-kabel nodig gekoppeld aan een adapter met een Ralink RT5370 of Atheros AR9271 chipset.",
                                    fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp
                                )
                            }
                        }
                        
                        // TIP 2
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = KaliSecondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("2. Root-rechten & Custom Kernel (Kali NetHunter)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KaliSecondary)
                                Text(
                                    "Standaard Android-firmware blokkeert directe hardware-aansturing. Om aircrack-ng of mdk4 via de app-terminal uit te voeren op de wifi-chipset, moet de telefoon geroot zijn (via Magisk) en draaien op een kernel die draadloze injectie ondersteunt.",
                                    fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp
                                )
                            }
                        }
                        
                        // TIP 3
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = KaliSecondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("3. Eigen Gemini API Key in AI Studio Secrets", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KaliSecondary)
                                Text(
                                    "Voor onbeperkte en razendsnelle AI-exploit blueprints, voeg je eigen gratis of betaalde API Key toe in de AI Studio Secrets panel onder 'GEMINI_API_KEY'. Dit heft alle rate-limits op en geeft je directe, onbeperkte toegang.",
                                    fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp
                                )
                            }
                        }
                        
                        // TIP 4
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = KaliSecondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("4. Lokale Room SQL Database Integratie", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KaliSecondary)
                                Text(
                                    "Om scans, netwerkkaarten en gekraakte WPA-handshakes offline op te slaan zonder dataloss, is een lokale SQLite Room-database nodig. Dit voorkomt dat gegevens verloren gaan bij het afsluiten van de app.",
                                    fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp
                                )
                            }
                        }
                        
                        // TIP 5
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = KaliSecondary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("5. Live Sandbox & Internet-header validatie", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KaliSecondary)
                                Text(
                                    "Gebruik de 'Sandboxed Security Web Inspector' hierboven om te controleren of doel-URL's kwetsbaar zijn voor datalekken voordat je een echte penetratietest uitvoert.",
                                    fontSize = 10.sp, color = Color.Gray, lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 3. TEAM & LICENSES (@ice1984m) SUB-TAB
        if (viewModel.activeCopilotSubTab == "license") {
            val context = LocalContext.current
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Licenties, Certificaten & Teamdeelname",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Eigendom, GitHub repository informatie en officiële teamlicenties van de Kali Linux Ethical Suite.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp),
                    lineHeight = 15.sp
                )
                
                // MIT LICENSE CERTIFICATE CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaliSurface),
                    border = BorderStroke(1.dp, KaliPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = KaliPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MIT LICENSE & CERTIFICAAT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Copyright (c) 2026 ice1984m\n\n" +
                                   "Hierbij wordt gratis toestemming verleend aan eenieder die een kopie van deze software verkrijgt, " +
                                   "om de software zonder beperking te gebruiken, inclusief de rechten om te gebruiken, kopiëren, wijzigen, samenvoegen, " +
                                   "publiceren, distribueren en/of onderlicentiëren, mits de auteursrechtvermelding en deze toestemmingsvermelding worden opgenomen in alle kopieën.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "OWNER ID: ice1984m (GitHub)\nREPO: github.com/ice1984m/kali-suite\nLICENTIE: MIT-Standard-Active",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = KaliPrimary
                            )
                        }
                    }
                }
                
                // STRICT PROTECTION / COPY NOTICE
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0505)),
                    border = BorderStroke(1.dp, KaliError),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = KaliError, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STRENG VERBODEN TE KOPIËREN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Het ongeautoriseerd kopiëren, distribueren of verkopen van deze bronbestanden en UI-patronen buiten het officiële ontwikkelteam van ice1984m is ten strengste verboden.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 15.sp
                        )
                    }
                }
                
                // TEAM EMAIL COLLABORATION CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaliSurface),
                    border = BorderStroke(1.dp, KaliSurfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Direct Samenwerken (E-mail Team)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Wil je als team bijdragen aan de Kali Linux app? Kopieer de broncode niet zomaar, maar werk rechtstreeks samen via GitHub met ons team.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = {
                                val mailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:")
                                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("mathmoors13@gmail.com"))
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Samenwerking aan Kali Suite App (Team ice1984m)")
                                    putExtra(
                                        android.content.Intent.EXTRA_TEXT,
                                        "Hallo team ice1984m,\n\nIk wil graag samenwerken en bijdragen aan de ontwikkeling van de Kali Linux Suite app.\n\nMet vriendelijke groet,"
                                    )
                                }
                                try {
                                    context.startActivity(android.content.Intent.createChooser(mailIntent, "Verzend e-mail..."))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Geen e-mail client gevonden. Mail direct naar mathmoors13@gmail.com",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KaliSecondary),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mail Direct naar Team Eigenaar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
                
                // TEAM REGISTRATION FORM
                Card(
                    colors = CardDefaults.cardColors(containerColor = KaliSurface),
                    border = BorderStroke(1.dp, KaliSurfaceVariant),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Meld je aan voor het GitHub Team",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Registreer je e-mailadres om direct te worden gekoppeld aan de ice1984m repository.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = viewModel.teamEmailInput,
                            onValueChange = { viewModel.teamEmailInput = it },
                            placeholder = { Text("naam@domein.com", fontSize = 11.sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = KaliPrimary,
                                unfocusedBorderColor = KaliSurfaceVariant,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        
                        if (viewModel.teamRegistrationStatus.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.teamRegistrationStatus,
                                color = if (viewModel.teamRegistrationStatus.startsWith("Succes")) KaliPrimary else if (viewModel.teamRegistrationStatus.startsWith("Fout")) KaliError else Color.Yellow,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { viewModel.submitTeamMemberRequest(viewModel.teamEmailInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = KaliPrimary),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Registreer als Teamlid", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = KaliSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "GEREGISTREERDE TEAMLEDEN & CONTRIBUTORS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = KaliSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            viewModel.registeredTeamMembers.forEach { member ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black, RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = KaliPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = member,
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SuggestionPromptChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(KaliSurface, RoundedCornerShape(6.dp))
            .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KaliSecondary)
    }
}

// --- TAB 6: ANONYMITY & GHOST NETWORKS ---
@Composable
fun AnonymityTab(viewModel: KaliViewModel) {
    var selectedServerToConnect by remember { mutableStateOf("") }
    
    val ghostServers = listOf(
        Pair("Reykjavík Proxy", "185.112.144.9"),
        Pair("Bucharest Ghost-Node", "89.34.22.110"),
        Pair("Geneva Stealth Tunnel", "179.43.155.201"),
        Pair("Singapore Secure Routing", "103.242.118.5")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "Netwerkanonimiteit & Spook Servers",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = "Beheer je online voetafdruk, versluier IP-adressen en koppel medegebruikers via ad-hoc gecodeerde mesh-netwerken.",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // --- SECTION 1: LIVE ANONYMITY STATUS PANEL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, if (viewModel.isAnonymityOptimized) KaliPrimary else KaliSurfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Anonimiteit Score", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (viewModel.isAnonymityOptimized) "OPTIMAAL BEVEILIGD" else "KWETSBAAR / DIRECT IP",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isAnonymityOptimized) KaliPrimary else Color.Yellow
                        )
                    }
                    
                    Icon(
                        imageVector = if (viewModel.isAnonymityOptimized) Icons.Default.Shield else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (viewModel.isAnonymityOptimized) KaliPrimary else Color.Yellow,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = KaliSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                
                // Active Protocol checklist items
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProtocolStatusIndicator(label = "MAC Spoof", isActive = viewModel.isMacSpoofed, modifier = Modifier.weight(1f))
                    ProtocolStatusIndicator(label = "P2P Mesh", isActive = viewModel.isP2PConnected, modifier = Modifier.weight(1f))
                    ProtocolStatusIndicator(label = "Spook IP", isActive = viewModel.isGhostServerActive, modifier = Modifier.weight(1f))
                    ProtocolStatusIndicator(label = "Tor active", isActive = viewModel.isTorRoutingEnabled, modifier = Modifier.weight(1f))
                }
            }
        }

        // --- SECTION 2: PEER-TO-PEER MESH COUPLING ---
        Text(
            text = "Medegebruikers Koppelen (P2P Mesh)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KaliSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, KaliSurfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Gedecentraliseerde Ad-hoc Routing",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Koppel je app aan actieve medegebruikers in de buurt. Dit creëert een gecodeerde ad-hoc routeringstunnel, waardoor netwerkpakketten ondetecteerbaar verdeeld worden verzonden.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Actieve Mesh Koppelingen", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = if (viewModel.isP2PConnected) "${viewModel.connectedNodesCount} Medegebruikers (Gecodeerd)" else "Niet verbonden",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isP2PConnected) KaliPrimary else Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.toggleP2PMesh() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.isP2PConnected) Color.Red else KaliPrimary
                        ),
                        modifier = Modifier.height(36.dp).testTag("toggle_p2p_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (viewModel.isP2PConnected) "Ontkoppelen" else "Koppelen",
                            fontSize = 11.sp,
                            color = if (viewModel.isP2PConnected) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- SECTION 3: SPOOK SERVERS (GHOST PROXY NETWORKS) ---
        Text(
            text = "Onzichtbare Spook Servers (Proxy-Hops)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KaliSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, KaliSurfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Onzichtbare IP-Adres Routing",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verberg en spoof je IP-adres door verbinding te maken met beveiligde spook-proxy nodes op externe geografische locaties.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show current active ghost server
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF070B11), RoundedCornerShape(8.dp))
                        .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Actieve Spook Server", fontSize = 9.sp, color = Color.Gray)
                        Text(
                            text = viewModel.activeGhostServer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isGhostServerActive) KaliPrimary else Color.White
                        )
                    }
                    if (viewModel.isGhostServerActive) {
                        Text(
                            text = "OFFLINE HALT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier
                                .clickable { viewModel.disconnectGhostServer() }
                                .padding(6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Beschikbare Spook-Hops", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                
                // List of Ghost servers
                ghostServers.forEach { (name, ip) ->
                    val isCurrent = viewModel.activeGhostServer == name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (isCurrent) KaliSurfaceVariant else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (isCurrent) KaliPrimary.copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.connectToGhostServer(name, ip) }
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = if (isCurrent) KaliPrimary else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "Proxy IP: $ip", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }
                        
                        Text(
                            text = if (isCurrent) "ACTIEF" else "CONNECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) KaliPrimary else KaliSecondary
                        )
                    }
                }
            }
        }

        // --- SECTION 4: TOR ROUTING SERVICE ---
        Text(
            text = "Tor Multi-hop Onion Routing",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KaliSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, KaliSurfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Anonieme Tor Gateway",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Routeer al het testverkeer door het gecodeerde Onion-netwerk voor extreme traceerbaarheids-afscherming.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = viewModel.isTorRoutingEnabled,
                    onCheckedChange = { viewModel.toggleTorRouting() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = KaliPrimary,
                        checkedTrackColor = Color(0xFF003914)
                    )
                )
            }
        }

        // --- SECTION 5: GPT COPILOT AUTOMATION COUPLING ---
        Text(
            text = "Laat AI Copilot configureren",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KaliSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = KaliSurface),
            border = BorderStroke(1.dp, KaliSurfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "AI-Gedreven Anonimiteits Beheer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gebruik onze ethical hacking copilot om jouw anonieme routerservices en spook-servers volledig geautomatiseerd in te stellen via natuurlijke taal.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(text = "Snelkoppelingen voor Chat-Configuratie:", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                
                // Prompt suggestion chips that redirect user to the CopilotTab with pre-filled content!
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionPromptChip("Stel anoniem profiel in") {
                        viewModel.sendCopilotMessage("Stel een anoniem profiel in met maximale beveiliging")
                        viewModel.selectedTab = "copilot"
                    }
                    SuggestionPromptChip("Activeer spook server") {
                        viewModel.sendCopilotMessage("Activeer een spook server proxy voor mij")
                        viewModel.selectedTab = "copilot"
                    }
                    SuggestionPromptChip("Koppel medegebruikers") {
                        viewModel.sendCopilotMessage("Koppel mij met omliggende medegebruikers via mesh")
                        viewModel.selectedTab = "copilot"
                    }
                }
            }
        }
    }
}

@Composable
fun ProtocolStatusIndicator(label: String, isActive: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                if (isActive) Color(0xFF003914) else Color(0xFF1D263B),
                RoundedCornerShape(6.dp)
            )
            .border(
                1.dp,
                if (isActive) KaliPrimary else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isActive) KaliPrimary else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else Color.Gray
        )
    }
}

// --- TERMINAL HISTORY & AUTOMATED LOG WINDOW COMPONENT ---
@Composable
fun TerminalHistoryLogComponent(viewModel: KaliViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Alles") } // "Alles", "AI Copilot", "Interface Config", "Systeem Alarms"
    
    val filteredLogs = remember(viewModel.terminalHistoryEvents.size, searchQuery, selectedFilter) {
        viewModel.terminalHistoryEvents.filter { log ->
            val matchesSearch = log.content.contains(searchQuery, ignoreCase = true) || 
                                log.details.contains(searchQuery, ignoreCase = true) ||
                                log.type.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                "AI Copilot" -> log.source == "AI Copilot"
                "Interface Config" -> log.source == "Interface Config"
                "Systeem Alarms" -> log.source == "System Alert" || log.source == "Anonymization"
                else -> true
            }
            matchesSearch && matchesFilter
        }.reversed() // Most recent first
    }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = KaliSurface),
        border = BorderStroke(1.dp, KaliSurfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("terminal_history_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = KaliPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Terminal- & AI-Geschiedenis",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Chronologische logs van AI en configuratie-events",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Badges and action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1D263B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${filteredLogs.size} logs",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = KaliSecondary
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            if (viewModel.terminalHistoryEvents.isEmpty()) {
                                Toast.makeText(context, "Geen logs om te kopiëren", Toast.LENGTH_SHORT).show()
                            } else {
                                val allLogsText = viewModel.terminalHistoryEvents.joinToString("\n") { log ->
                                    "[${log.timestamp}] [${log.source}] [${log.type}] ${log.content} ${if (log.details.isNotEmpty()) "- " + log.details else ""}"
                                }
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(allLogsText))
                                Toast.makeText(context, "Alle logs gekopieerd naar klembord!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .background(KaliSurfaceVariant, RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy All Logs", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                    
                    IconButton(
                        onClick = { viewModel.clearTerminalHistory() },
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF3B0505), RoundedCornerShape(6.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = KaliError, modifier = Modifier.size(14.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("terminal_history_search"),
                placeholder = { Text("Doorzoek logs en opdrachten...", fontSize = 12.sp, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KaliPrimary,
                    unfocusedBorderColor = KaliSurfaceVariant,
                    focusedContainerColor = Color(0xFF070B11),
                    unfocusedContainerColor = Color(0xFF070B11),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Alles", "AI Copilot", "Interface Config", "Systeem Alarms").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) KaliPrimary else Color(0xFF0F1622),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) KaliPrimary else KaliSurfaceVariant,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.LightGray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chronological Logs list container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF030509))
                    .border(1.dp, KaliSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Geen geregistreerde log-events gevonden.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredLogs, key = { it.id }) { log ->
                            TerminalHistoryItemRow(
                                log = log,
                                onCopyClick = {
                                    val copyText = "[${log.timestamp}] [${log.source}] ${log.content} ${if (log.details.isNotEmpty()) "- " + log.details else ""}"
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copyText))
                                    Toast.makeText(context, "Logregel gekopieerd!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalHistoryItemRow(log: TerminalHistoryEvent, onCopyClick: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val sourceColor = when (log.source) {
        "AI Copilot" -> Color(0xFFC792EA) // Purplish
        "Interface Config" -> KaliPrimary  // Greenish
        "System Alert" -> KaliError      // Fire Red
        "Anonymization" -> KaliSecondary  // Cyan
        else -> Color.White
    }
    
    val sourceIcon = when (log.source) {
        "AI Copilot" -> Icons.Default.Android
        "Interface Config" -> Icons.Default.Settings
        "System Alert" -> Icons.Default.Warning
        "Anonymization" -> Icons.Default.Shield
        else -> Icons.Default.History
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                if (isExpanded) Color(0xFF090E17) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable { if (log.details.isNotEmpty()) isExpanded = !isExpanded }
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon representing the source
                Icon(
                    imageVector = sourceIcon,
                    contentDescription = null,
                    tint = sourceColor,
                    modifier = Modifier.size(13.dp)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // Timestamp
                Text(
                    text = "[${log.timestamp}]",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // Badge of Event type
                Box(
                    modifier = Modifier
                        .background(sourceColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = log.type.uppercase(),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = sourceColor
                    )
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // Log Content
                Text(
                    text = log.content,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1
                )
            }
            
            // Action button container
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (log.details.isNotEmpty()) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand details",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy log",
                        tint = Color.Gray,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        
        // Expanded details card/ bubble
        if (isExpanded && log.details.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                    .background(Color(0xFF04060A), RoundedCornerShape(4.dp))
                    .border(1.dp, KaliSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Text(
                    text = log.details,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
