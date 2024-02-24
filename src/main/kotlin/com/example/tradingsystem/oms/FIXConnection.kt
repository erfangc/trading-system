package com.example.tradingsystem.oms

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import quickfix.*
import quickfix.field.*
import java.io.ByteArrayInputStream
import java.nio.charset.Charset

@Service
class FIXConnection(private val executionReportListener: ExecutionReportListener) : ApplicationAdapter() {

    private val log = LoggerFactory.getLogger(FIXConnection::class.java)
    private lateinit var initiator: SocketInitiator
    
    companion object {
        private val senderCompID = System.getenv("FIX_SENDER_COMP_ID") ?: "SENDER"
        private val targetCompID = System.getenv("FIX_TARGET_COMP_ID") ?: "TARGET"
        private val socketConnectHost = System.getenv("FIX_SOCKET_CONNECT_HOST") ?: "localhost"
        private val socketConnectPort = System.getenv("FIX_SOCKET_CONNECT_PORT") ?: "9876"
        val sessionID: SessionID = SessionID("FIX.4.4", senderCompID, targetCompID)
    }

    override fun onLogon(sessionId: SessionID?) {
        log.info("Logon successful sessionId=$sessionId")
    }

    override fun fromApp(message: Message, sessionId: SessionID?) {
        log.info("Received FIX message=$message")
        if (message.header.getString(MsgType.FIELD) == MsgType.EXECUTION_REPORT) {
            executionReportListener.onExecutionReport(message)
        }
    }

    @PostConstruct
    fun start() {
        val sessionSettings = loadSessionSettingsFromEnvironment()
        val fileStoreFactory = FileStoreFactory(sessionSettings)
        val logFactory = ScreenLogFactory(true, true, true)
        val messageFactory = DefaultMessageFactory()
        initiator = SocketInitiator(this, fileStoreFactory, sessionSettings, logFactory, messageFactory)
        initiator.start()
    }

    @PreDestroy
    fun stop() {
        log.info("Stopping SocketInitiator on FIXOrderManager")
        initiator.stop()
    }

    private fun loadSessionSettingsFromEnvironment(): SessionSettings {
        val settings = """
        [DEFAULT]
        ConnectionType=initiator
        ReconnectInterval=5
        SocketConnectHost=$socketConnectHost
        SocketConnectPort=$socketConnectPort
        StartTime=00:00:00
        EndTime=00:00:00
        HeartBtInt=30
        FileStorePath=./fix_file_store

        [SESSION]
        BeginString=FIX.4.4
        SenderCompID=$senderCompID
        TargetCompID=$targetCompID
        SocketConnectPort=$socketConnectPort
        StartTime=00:00:00
        EndTime=00:00:00
        HeartBtInt=30
        """.trimIndent()
        val byteInputStream = ByteArrayInputStream(settings.toByteArray(Charset.defaultCharset()))
        val sessionSettings = SessionSettings(byteInputStream)
        return sessionSettings
    }

}
