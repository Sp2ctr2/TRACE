package app.saeon.trace.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ScheduledTransfer(val id:String,val title:String,val amount:Long,val day:Int,val active:Boolean=true)
data class ServiceCase(val id:String,val category:String,val body:String,val createdAt:Long)
data class IssuedDocument(val id:String,val kind:String,val text:String,val createdAt:Long)
data class ServiceState(val schedules:List<ScheduledTransfer> = emptyList(),val cases:List<ServiceCase> = emptyList(),
    val documents:List<IssuedDocument> = emptyList(),val cardLost:Boolean=false,val replacementRequested:Boolean=false)

/** Local demonstration services; no background debit, outbound report, or real certificate. */
class StudioServices(context:Context) {
    private val prefs=context.applicationContext.getSharedPreferences("studio_services_v1",Context.MODE_PRIVATE)
    private val mutable=MutableStateFlow(read())
    val state=mutable.asStateFlow()
    private fun read():ServiceState {
        val raw=prefs.getString("state",null) ?: return ServiceState(schedules=listOf(ScheduledTransfer("phone","통신비",68000,25)))
        val o=JSONObject(raw)
        fun items(key:String)=o.optJSONArray(key)?.let{a->(0 until a.length()).map{a.getJSONObject(it)}}?:emptyList()
        return ServiceState(items("schedules").map{ScheduledTransfer(it.getString("id"),it.getString("title"),it.getLong("amount"),it.getInt("day"),it.getBoolean("active"))},
            items("cases").map{ServiceCase(it.getString("id"),it.getString("category"),it.getString("body"),it.getLong("time"))},
            items("documents").map{IssuedDocument(it.getString("id"),it.getString("kind"),it.getString("text"),it.getLong("time"))},
            o.optBoolean("lost"),o.optBoolean("replacement"))
    }
    @Synchronized private fun update(change:(ServiceState)->ServiceState) {
        val s=change(mutable.value)
        val o=JSONObject().put("lost",s.cardLost).put("replacement",s.replacementRequested)
            .put("schedules",JSONArray().apply{s.schedules.forEach{put(JSONObject().put("id",it.id).put("title",it.title).put("amount",it.amount).put("day",it.day).put("active",it.active))}})
            .put("cases",JSONArray().apply{s.cases.forEach{put(JSONObject().put("id",it.id).put("category",it.category).put("body",it.body).put("time",it.createdAt))}})
            .put("documents",JSONArray().apply{s.documents.forEach{put(JSONObject().put("id",it.id).put("kind",it.kind).put("text",it.text).put("time",it.createdAt))}})
        check(prefs.edit().putString("state",o.toString()).commit()){"로컬 서비스 정보를 저장하지 못했어요."}
        mutable.value=s
    }
    fun resetFixtures()=update{ServiceState(schedules=listOf(ScheduledTransfer("phone","통신비",68000,25)))}
    fun schedule(title:String,amount:Long,day:Int) {
        require(title.trim().length in 1..24 && amount in 1..100_000_000 && day in 1..28)
        update{it.copy(schedules=it.schedules+ScheduledTransfer(UUID.randomUUID().toString(),title.trim(),amount,day))}
    }
    fun toggleSchedule(id:String)=update{it.copy(schedules=it.schedules.map{s->if(s.id==id)s.copy(active=!s.active)else s})}
    fun deleteSchedule(id:String)=update{it.copy(schedules=it.schedules.filterNot{s->s.id==id})}
    fun cardLost(lost:Boolean)=update{it.copy(cardLost=lost,replacementRequested=if(lost)it.replacementRequested else false)}
    fun replacement()=update{require(it.cardLost);it.copy(replacementRequested=true)}
    fun addCase(category:String,body:String){require(body.trim().length in 5..500);update{it.copy(cases=(listOf(ServiceCase("SIM-${UUID.randomUUID().toString().take(8)}",category,body.trim(),System.currentTimeMillis()))+it.cases).take(100))}}
    fun document(kind:String,text:String){require(kind.isNotBlank());update{it.copy(documents=(listOf(IssuedDocument("SIM-${UUID.randomUUID().toString().take(8)}",kind,text,System.currentTimeMillis()))+it.documents).take(50))}}
}
