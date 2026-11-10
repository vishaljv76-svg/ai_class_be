package com.example.Simtas.data.remote

/**
 * Centralized API configuration for SmartNotes AI.
 *
 * Mode 1 (Active - USB Connected Physical Phone via ADB Reverse):
 * - Direct zero-latency USB tunnel from phone to PC XAMPP Apache.
 * - Port 8080 on device forwards to Port 80 on PC: `adb reverse tcp:8080 tcp:80`
 * - Completely immune to Wi-Fi drops, 4G/5G mobile data, or lab firewall restrictions!
 *git add .
 *
 * Mode 2 (LAN Wi-Fi - If testing wirelessly on same Wi-Fi router):
 * - Change to "http://172.23.176.83/ai_class_be" (or current PC LAN IP)
 *
 * Mode 3 (Android Studio Emulator):
 * - Change to "http://10.0.2.2/ai_class_be"
 */
object ApiConfig {
   var BASE_URL = "https://correct-retorted-skedaddle.ngrok-free.dev/ai_class_bebackend/"
}
