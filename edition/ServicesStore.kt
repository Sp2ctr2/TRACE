package app.saeon.trace.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.bankServices by preferencesDataStore("saeon_services_v1")
data class LocalDocument(val id:String,val kind:String,val created:Long,val amount:Long)
data class LocalRequest(val id:String,val kind:String,val text:String,val created:Long)
data class LocalSchedule(val id:String,val name:String,val recipientId:String,val amount:Long,val day:Int,val enabled:Boolean=true)
data class ServiceData(val documents:List<LocalDocument> = emptyList(),val requests:List<LocalRequest> = emptyList(),
    val schedules:List<LocalSchedule> = emptyList(),val watch:Set<String> = emptySet(),val creditAlerts:Boolean=true,
    val cardLost:Boolean=false,val reissueRequested:Boolean=false,val error:String?=null)

/** Demo-host persistence only. This store cannot execute a payment. */
class ServicesStore(context:Context) {
    private val store=context.applicationContext.bankServices
    private val payload=stringPreferencesKey("payload")
    val flow:Flow<ServiceData> = store.data.map{decode(it[payload])}.catch{emit(ServiceData(error="부가 서비스 기록을 읽지 못했어요. 저장소 상태를 확인해 주세요."))}
    private suspend fun change(f:(ServiceData)->ServiceData){store.edit{it[payload]=encode(f(decode(it[payload])))}}
    suspend fun issue(kind:String,amount:Long):LocalDocument {
        require(kind in setOf("balance","account","history"))
        val document=LocalDocument("DEMO-${UUID.randomUUID().toString().take(8).uppercase()}",kind,System.currentTimeMillis(),amount)
        change{it.copy(documents=(listOf(document)+it.documents).take(100))};return document
    }
    suspend fun request(kind:String,text:String):LocalRequest {
        require(kind in setOf("문의","피해 신고","카드 재발급"));require(text.trim().length in 2..1000)
        val request=LocalRequest("SR-${UUID.randomUUID().toString().take(8).uppercase()}",kind,text.trim(),System.currentTimeMillis())
        change{it.copy(requests=(listOf(request)+it.requests).take(100),reissueRequested=it.reissueRequested||kind=="카드 재발급")};return request
    }
    suspend fun addSchedule(name:String,recipientId:String,amount:Long,day:Int) {
        require(name.trim().length in 1..24);require(amount in 1..100000000);require(day in 1..28)
        val item=LocalSchedule(UUID.randomUUID().toString(),name.trim(),recipientId,amount,day)
        change{require(it.schedules.size<30);it.copy(schedules=it.schedules+item)}
    }
    suspend fun toggleSchedule(id:String,value:Boolean){change{it.copy(schedules=it.schedules.map{x->if(x.id==id)x.copy(enabled=value)else x})}}
    suspend fun removeSchedule(id:String){change{it.copy(schedules=it.schedules.filterNot{x->x.id==id})}}
    suspend fun watch(id:String,value:Boolean){change{it.copy(watch=if(value)it.watch+id else it.watch-id)}}
    suspend fun credit(value:Boolean){change{it.copy(creditAlerts=value)}}
    suspend fun lost(value:Boolean){change{it.copy(cardLost=value)}}
    suspend fun deleteRequests(){change{it.copy(requests=emptyList())}}
    private fun decode(text:String?):ServiceData {
        if(text==null)return ServiceData()
        val j=JSONObject(text);require(j.getInt("version")==1)
        fun array(key:String)=j.optJSONArray(key)?:JSONArray()
        val docs=array("documents");val req=array("requests");val schedules=array("schedules");val watch=array("watch")
        return ServiceData(
            (0 until docs.length()).map{docs.getJSONObject(it).let{x->LocalDocument(x.getString("id"),x.getString("kind"),x.getLong("created"),x.getLong("amount"))}},
            (0 until req.length()).map{req.getJSONObject(it).let{x->LocalRequest(x.getString("id"),x.getString("kind"),x.getString("text"),x.getLong("created"))}},
            (0 until schedules.length()).map{schedules.getJSONObject(it).let{x->LocalSchedule(x.getString("id"),x.getString("name"),x.getString("recipient"),x.getLong("amount"),x.getInt("day"),x.getBoolean("enabled"))}},
            (0 until watch.length()).map{watch.getString(it)}.toSet(),j.optBoolean("credit",true),j.optBoolean("lost",false),j.optBoolean("reissue",false))
    }
    private fun encode(s:ServiceData):String {
        fun <T> arr(items:Iterable<T>,convert:(T)->Any)=JSONArray().apply{items.forEach{put(convert(it))}}
        return JSONObject().put("version",1)
            .put("documents",arr(s.documents){JSONObject().put("id",it.id).put("kind",it.kind).put("created",it.created).put("amount",it.amount)})
            .put("requests",arr(s.requests){JSONObject().put("id",it.id).put("kind",it.kind).put("text",it.text).put("created",it.created)})
            .put("schedules",arr(s.schedules){JSONObject().put("id",it.id).put("name",it.name).put("recipient",it.recipientId).put("amount",it.amount).put("day",it.day).put("enabled",it.enabled)})
            .put("watch",arr(s.watch){it}).put("credit",s.creditAlerts).put("lost",s.cardLost).put("reissue",s.reissueRequested).toString()
    }
}
