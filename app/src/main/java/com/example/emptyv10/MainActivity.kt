package com.example.emptyv10

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.emptyv10.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var ipAddress = "0.0.0.0"
    private var port = 3000
    private var message = "empty"
    private lateinit var udpPacket: String
    private lateinit var udpClient: UdpClient

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        udpClient = UdpClient()
        requestPermissions()

        binding.refresh.setOnClickListener {
            refresh()
        }

        binding.button.setOnClickListener {
            if (isValidIp(ipAddress) && isValidPort(port)) {
                Log.d("MainActivity", "button clicked with: ipAddress = $ipAddress, port = $port, message = $message")
                lifecycleScope.launch(Dispatchers.IO) {
                    udpClient.sendMessage(ipAddress, port, message)
                }
            } else {
                Toast.makeText(this, "Invalid IP or Port", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        } else {
            binding.permissions.text = getString(R.string.permission_granted)
        }
    }

    private fun refresh() {
        ipAddress = "192.168." + binding.ipAddress.text.toString()
        port = binding.port.text.toString().toIntOrNull() ?: 0
        message = binding.message.text.toString()
        udpPacket = getString(R.string.udp_message_format, message, ipAddress, port)
        binding.udpPacket.text = udpPacket
    }

    private fun isValidIp(ip: String): Boolean {
        return Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b").matches(ip)
    }

    private fun isValidPort(port: Int): Boolean {
        return port in 1..65535
    }

    override fun onDestroy() {
        super.onDestroy()
        udpClient.close()
    }
}

class UdpClient {
    private var socket: DatagramSocket? = null

    init {
        runCatching {
            socket = DatagramSocket()
        }.onFailure { e ->
            Log.e("UdpClient", "Socket initialization failed", e)
        }
    }

    fun sendMessage(ipAddress: String, port: Int, message: String) {
        Log.d("UdpClient", "sendMessage() called with: ipAddress = $ipAddress, port = $port, message = $message")

        runCatching {
            val messageBytes = message.toByteArray()
            val packet = DatagramPacket(
                messageBytes,
                messageBytes.size,
                InetAddress.getByName(ipAddress),
                port
            )

            socket?.send(packet)
            Log.d("UdpClient", "Message sent successfully")
        }.onFailure { e ->
            Log.e("UdpClient", "Error sending message", e)
        }
    }

    fun close() {
        socket?.close()
    }
}
