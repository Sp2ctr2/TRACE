package dev.sp2ctr2.saeon.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import dev.sp2ctr2.saeon.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BankRepository(context:Context) {
    private val db=Room.databaseBuilder(context.applicationContext,BankDatabase::class.java,"saeon-demo.db").build()
    private val json=Json{ignoreUnknownKeys=true;encodeDefaults=true}
    private val mutex=Mutex()
    private val _state=MutableStateFlow<BankState?>(null)
    val state=_state.asStateFlow()
    private val _failure=MutableStateFlow<String?>(null)
    val failure=_failure.asStateFlow()
    suspend fun initialize()=withContext(Dispatchers.IO){mutex.withLock{
        try{
            val current=db.bankDao().read()?.let{json.decodeFromString<BankState>(it.payload)} ?: Fixtures.seed()
            check(BankEngine.ledgerIsConsistent(current))
            val draft=current.draft
            val recovered=if(draft?.phase==Phase.EVALUATING)current.copy(draft=draft.copy(phase=Phase.REVIEW)) else current
            db.bankDao().write(BankRow(payload=json.encodeToString(recovered)))
            _state.value=recovered;_failure.value=null
        }catch(c:CancellationException){throw c}
        catch(_:Exception){_failure.value="저장된 시연 계좌를 불러오지 못했어요. 자동으로 송금하지 않았습니다."}
    }}
    suspend fun update(block:(BankState)->BankState):BankState=withContext(Dispatchers.IO){mutex.withLock{
        val next=db.withTransaction{
            val row=db.bankDao().read() ?: throw BankRuleException("계좌를 불러온 뒤 다시 시도해 주세요.")
            val current=json.decodeFromString<BankState>(row.payload)
            val changed=block(current)
            check(BankEngine.ledgerIsConsistent(changed)){"Ledger invariant violated"}
            db.bankDao().write(BankRow(payload=json.encodeToString(changed)))
            changed
        }
        _state.value=next;next
    }}
    suspend fun reset(scenario:Scenario=Scenario.NORMAL)=withContext(Dispatchers.IO){mutex.withLock{
        val seed=Fixtures.seed(scenario=scenario)
        db.withTransaction{db.bankDao().write(BankRow(payload=json.encodeToString(seed)))}
        _state.value=seed;_failure.value=null
    }}
    suspend fun confirm(id:String,expectedBinding:String,signer:ProofSigner):BankState=update{current->
        val d=current.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
        if(current.receipts.any{it.transactionId==id})return@update current
        if(d.id!=id || BankEngine.binding(current,d)!=expectedBinding)throw BankRuleException("거래 정보가 바뀌었어요. 다시 확인해 주세요.")
        if(d.phase !in setOf(Phase.REVIEW,Phase.EVALUATING))throw BankRuleException("거래 상태를 다시 확인해 주세요.")
        val assessed=BankEngine.assess(current.copy(draft=d.copy(phase=Phase.REVIEW)),current.now())
        if(assessed.draft?.phase==Phase.REVIEW){val proof=BankEngine.authorize(assessed,signer,assessed.now());BankEngine.commit(assessed,proof,signer,assessed.now())} else assessed
    }
    fun close()=db.close()
}
