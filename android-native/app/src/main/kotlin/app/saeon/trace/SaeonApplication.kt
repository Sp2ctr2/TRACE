package app.saeon.trace

import android.app.Application
import android.content.Context
import androidx.room.Room
import app.saeon.trace.data.*
import app.saeon.trace.security.*
import kotlinx.coroutines.*

class AppGraph(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database: BankDatabase = Room.databaseBuilder(context.applicationContext, BankDatabase::class.java, "saeon-ledger.db").build()
    val clock = DemoClock()
    val keystore = KeystoreAttestation()
    val gateway = DemoBankGateway(keystore)
    val preferences = Preferences(context)
    val repository = BankRepository(database, clock, gateway, scope)
}
class SaeonApplication : Application() {
    lateinit var graph: AppGraph
        private set
    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        graph.scope.launch { graph.repository.initialize() }
    }
}
