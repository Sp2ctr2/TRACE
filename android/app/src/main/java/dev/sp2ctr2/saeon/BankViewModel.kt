package dev.sp2ctr2.saeon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.sp2ctr2.saeon.data.AppOptions
import dev.sp2ctr2.saeon.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class BankViewModel(application: Application) : AndroidViewModel(application) {
    private val app=application as SaeonApplication
    private val repo=app.repository
    private val prefs=app.preferences
    private val signer=app.signer
    val state=repo.state
    val storageFailure=repo.failure
    val options=prefs.options.stateIn(viewModelScope,SharingStarted.Eagerly,AppOptions())
    val busy=MutableStateFlow(false)
    val error=MutableStateFlow<String?>(null)
    val sharedText=MutableStateFlow("")
    val analysisResult=MutableStateFlow<List<RiskEvent>?>(null)
    private val routes=Channel<String>(Channel.BUFFERED)
    val navigation=routes.receiveAsFlow()
    init { viewModelScope.launch { repo.initialize() } }
    fun go(route:String){routes.trySend(route)}
    fun dismissError(){error.value=null}
    private fun task(block:suspend ()->Unit) {
        if(busy.value)return
        busy.value=true
        viewModelScope.launch {
            try{block()}
            catch(c:CancellationException){throw c}
            catch(e:BankRuleException){error.value=e.message}
            catch(_:Exception){error.value="요청을 완료하지 못했어요. 거래 내역을 확인한 뒤 다시 시도해 주세요."}
            finally{busy.value=false}
        }
    }
    fun retryLoad()=task{repo.initialize()}
    fun start(recipient:Recipient)=task{
        val updated=repo.update{BankEngine.begin(it,recipient,Fixtures.defaultAmount(it.scenario),it.now())}
        go(if(updated.draft?.phase==Phase.HOLD)"result" else "amount")
    }
    fun prepareReview(amount:Long,purpose:Purpose)=task{repo.update{BankEngine.review(BankEngine.edit(it,amount,purpose))};go("review")}
    fun editDraft()=task{repo.update{s->val d=s.draft ?: return@update s;BankEngine.edit(s,d.amount)};go("amount")}
    fun confirm(id:String,binding:String)=task{
        repo.update{s->
            val d=s.draft ?: throw BankRuleException("송금을 다시 시작해 주세요.")
            if(d.id!=id || BankEngine.binding(s,d)!=binding || d.phase!=Phase.REVIEW)throw BankRuleException("거래 정보를 다시 확인해 주세요.")
            s.copy(draft=d.copy(phase=Phase.EVALUATING))
        }
        go("result")
        try{repo.confirm(id,binding,signer)}
        catch(c:CancellationException){throw c}
        catch(e:Exception){repo.update{s->
            val d=s.draft
            if(d!=null && d.id==id && d.phase==Phase.EVALUATING)s.copy(draft=d.copy(phase=Phase.ERROR,error=if(e is BankRuleException)e.message else "거래 확인 정보를 검증하지 못했어요. 돈은 나가지 않았습니다.")) else s
        }}
    }
    fun acknowledge()=task{repo.update(BankEngine::acknowledgeWarning);go("review")}
    fun routeLookup(forceFailure:Boolean?=null)=task{repo.update{s->BankEngine.lookupRoute(s,s.now(),forceFailure ?: (s.scenario==Scenario.LOOKUP_FAILURE))};go("result")}
    fun acceptRoute()=task{repo.update{BankEngine.acceptRoute(it,it.now())};go("review")}
    fun cancelTransfer()=task{repo.update(BankEngine::cancel);go("home")}
    fun reset(scenario:Scenario)=task{sharedText.value="";analysisResult.value=null;repo.reset(scenario);go("home")}
    fun setOption(key:String,enabled:Boolean)=task{prefs.set(key,enabled)}
    fun bring(amount:Long)=task{repo.update{BankEngine.bringFromSavings(it,amount,it.now())};go("account/living")}
    fun favorite(id:String)=task{repo.update{s->s.copy(favorites=if(id in s.favorites)s.favorites-id else s.favorites+id)}}
    fun notice(id:String,route:String)=task{repo.update{s->s.copy(notices=s.notices.map{if(it.id==id)it.copy(read=true) else it})};go(route)}
    fun markNoticesRead()=task{repo.update{s->s.copy(notices=s.notices.map{it.copy(read=true)})}}
    fun scheduleToggle(id:String)=task{repo.update{s->s.copy(schedules=s.schedules.map{if(it.id==id)it.copy(active=!it.active) else it})}}
    fun scheduleAdd(recipient:Recipient,amount:Long,day:Int)=task{
        if(amount<=0 || day !in 1..28)throw BankRuleException("금액과 예약일을 다시 확인해 주세요.")
        repo.update{it.copy(schedules=it.schedules+ScheduledTransfer(UUID.randomUUID().toString(),recipient,amount,day))};go("schedules")
    }
    fun scheduleDelete(id:String)=task{repo.update{it.copy(schedules=it.schedules.filterNot{p->p.id==id})}}
    fun setLimit(limit:Long)=task{if(limit !in 10_000L..10_000_000L)throw BankRuleException("1회 한도는 1만원부터 1,000만원까지 설정할 수 있어요.");repo.update{it.copy(dailyLimit=limit)}}
    fun receiveShared(text:String){sharedText.value=text.take(LocalRiskAnalyzer.MAX_INPUT);analysisResult.value=null;go("share")}
    fun clearShared(){sharedText.value="";analysisResult.value=null}
    fun analyze(text:String)=task{
        if(text.isBlank())throw BankRuleException("확인할 내용을 입력해 주세요.")
        val s=state.value ?: return@task
        val events=LocalRiskAnalyzer.analyze(text,s.now())
        repo.update{it.copy(risks=(it.risks+events).takeLast(100))}
        sharedText.value="";analysisResult.value=events
    }
    fun clearExpired()=task{repo.update{s->s.copy(risks=BankEngine.activeRisks(s,s.now()))}}
    fun advanceDemoClock()=task{repo.update{it.copy(clockOrigin=it.clockOrigin+20*60_000)}}
}
