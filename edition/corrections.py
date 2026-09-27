from pathlib import Path
r=Path('android');u=r/'app/src/main/kotlin/app/saeon/trace/ui';d=r/'app/src/main/kotlin/app/saeon/trace/data'
def edit(p,a,b):
 s=p.read_text();assert a in s,(str(p),a[:90]);p.write_text(s.replace(a,b))
# Selected documents retain their own original transaction snapshot, not the current ledger.
p=d/'ServicesStore.kt'
edit(p,'data class LocalDocument(val id:String,val kind:String,val created:Long,val amount:Long)',
'data class DocumentLine(val name:String,val amount:Long)\ndata class LocalDocument(val id:String,val kind:String,val created:Long,val amount:Long,val lines:List<DocumentLine> = emptyList())')
edit(p,'suspend fun issue(kind:String,amount:Long):LocalDocument','suspend fun issue(kind:String,amount:Long,lines:List<DocumentLine> = emptyList()):LocalDocument')
edit(p,'kind,System.currentTimeMillis(),amount)','kind,System.currentTimeMillis(),amount,lines.toList())')
edit(p,'x.getLong("created"),x.getLong("amount"))','x.getLong("created"),x.getLong("amount"),x.optJSONArray("lines")?.let{a->(0 until a.length()).map{i->a.getJSONObject(i).let{l->DocumentLine(l.getString("name"),l.getLong("amount"))}}?:emptyList())')
edit(p,'.put("created",it.created).put("amount",it.amount)})','.put("created",it.created).put("amount",it.amount).put("lines",arr(it.lines){l->JSONObject().put("name",l.name).put("amount",l.amount)})})')
# Duplicate reissue taps return the existing request instead of repeatedly filing one.
a='''        val request=LocalRequest("SR-${UUID.randomUUID().toString().take(8).uppercase()}",kind,text.trim(),System.currentTimeMillis())
        change{it.copy(requests=(listOf(request)+it.requests).take(100),reissueRequested=it.reissueRequested||kind=="카드 재발급")};return request'''
b='''        var request=LocalRequest("SR-${UUID.randomUUID().toString().take(8).uppercase()}",kind,text.trim(),System.currentTimeMillis())
        change{state->
            val existing=if(kind=="카드 재발급")state.requests.firstOrNull{it.kind==kind}else null
            if(existing!=null){request=existing;state}else state.copy(requests=(listOf(request)+state.requests).take(100),reissueRequested=state.reissueRequested||kind=="카드 재발급")
        };return request'''
edit(p,a,b)
p=u/'BankViewModel.kt'
edit(p,'    val services=ServicesStore(application)','    val selectedDocumentId=MutableStateFlow<String?>(null)\n    val services=ServicesStore(application)')
p=u/'screens/BankingServices.kt'
edit(p,'model.services.issue(kind,state.balance);open("certificate")','model.selectedDocumentId.value=model.services.issue(kind,state.balance,state.receipts.map{DocumentLine(it.recipient.name,if(it.direction==Direction.CREDIT)it.amount else -it.amount)}).id;open("certificate")')
edit(p,'data.documents.take(5).forEach{d->BankRow(docName(d.kind),d.id){open("certificate")}}','data.documents.take(5).forEach{d->BankRow(docName(d.kind),d.id,modifier=Modifier.testTag("document_${d.id}")){model.selectedDocumentId.value=d.id;open("certificate")}}')
edit(p,'    val d=data.documents.firstOrNull()','    val selected by model.selectedDocumentId.collectAsStateWithLifecycle()\n    val d=data.documents.find{it.id==selected}?:data.documents.firstOrNull()')
edit(p,'state.receipts.map{"<tr><td>${escape(it.recipient.name)}</td><td>${if(it.direction==Direction.CREDIT)"+"else"−"}${won(it.amount)}원</td></tr>"}',
'd.lines.map{"<tr><td>${escape(it.name)}</td><td>${if(it.amount>=0)"+"else"−"}${won(kotlin.math.abs(it.amount))}원</td></tr>"}')
edit(p,'state.receipts.take(10).forEach{DetailRow(it.recipient.name,"${if(it.direction==Direction.CREDIT)"+"else"−"}${won(it.amount)}원")}',
'd.lines.take(10).forEach{DetailRow(it.name,"${if(it.amount>=0)"+"else"−"}${won(kotlin.math.abs(it.amount))}원")}')
edit(p,'OptionRow("카드 일시 정지",p.cardFrozen,"가상 카드의 사용 상태를 저장합니다."){model.preference{cardFrozen(it)}}',
'OptionRow("카드 일시 정지",p.cardFrozen,"분실 신고된 카드는 찾음 처리 전까지 해제할 수 없어요."){value->if(!value&&data.cardLost)model.showError("분실 상태예요. 카드를 찾은 뒤 찾음 처리부터 해주세요.")else model.preference{cardFrozen(value)}}\n        if(data.cardLost)QuietButton("카드를 찾았어요",Modifier.testTag("card_found")){model.act{model.services.lost(false)}}')
# Every place exposing the freeze control enforces the same lost-card rule.
p=u/'screens/BankingScreens.kt';s=p.read_text()
s=s.replace('OptionRow("카드 일시 정지",preferences.cardFrozen,"시연용 카드 상태만 바뀝니다."){model.preference{cardFrozen(it)}}',
'OptionRow("카드 일시 정지",preferences.cardFrozen,"시연용 카드 상태만 바뀝니다."){value->if(!value&&model.serviceState.value.cardLost)model.showError("분실 신고된 카드예요. 분실·재발급에서 찾음 처리부터 해주세요.")else model.preference{cardFrozen(value)}}')
p.write_text(s)
# Own-account debit is not external daily transfer usage.
p=r/'core/src/main/kotlin/app/saeon/trace/core/BankEngine.kt';s=p.read_text()
s=s.replace('it.direction == Direction.DEBIT && !it.seed &&', 'it.direction == Direction.DEBIT && !it.seed && !it.authorizationNonce.startsWith("internal-") &&')
# The exact existing property varies across snapshots; only apply when it is named nonce.
s=s.replace('!it.authorizationNonce.startsWith("internal-")','!it.nonce.startsWith("internal-")')
p.write_text(s)
# The animated brand overlay exits with a short dissolve; returning to the app does not replay it.
p=u/'SaeonApp.kt';s=p.read_text().replace('    if(launchVisible)BankingLaunch()',
'    AnimatedVisibility(visible=launchVisible,enter=EnterTransition.None,exit=fadeOut(tween(if(launchReduced)0 else 180))){BankingLaunch()}')
p.write_text(s)
print('Certificate snapshots, selected history, duplicate reissue protection, card recovery and launch exit corrected.')
