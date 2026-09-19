package dev.sp2ctr2.saeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.sp2ctr2.saeon.BankViewModel
import dev.sp2ctr2.saeon.data.AppOptions
import dev.sp2ctr2.saeon.domain.*

fun purposeLabel(p: Purpose) = when(p) {
    Purpose.SETTLEMENT -> "정산"; Purpose.LIVING -> "생활비"; Purpose.FAMILY -> "가족"
    Purpose.REPAYMENT -> "대출 상환"; Purpose.PURCHASE -> "상품·서비스"; Purpose.OTHER -> "기타"
}
fun pending(state: BankState) = state.draft?.phase in setOf(Phase.HOLD,Phase.VERIFY,Phase.UNKNOWN,Phase.WARN,Phase.ROUTE,Phase.REVIEW,Phase.EVALUATING,Phase.ERROR)

@Composable fun HomeScreen(state: BankState, options: AppOptions, go: (String)->Unit) {
    Screen("새온은행",tag="home_screen",brand=true,actions={GlyphButton("bell","알림","home_notifications"){go("notifications")}}) {
        Gap(12)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Copy("새온 생활통장",Modifier.weight(1f),size=15,color=TraceColors.Ink,weight=FontWeight.Medium)
            TextButton({go("account/living")},colors=ButtonDefaults.textButtonColors(contentColor=TraceColors.Muted)){Text("계좌 관리")}
        }
        Gap(6); Amount(state.balance,"home_balance",hidden=options.hideBalance)
        Gap(8); Copy("새온은행 110-***-0001",size=12)
        Gap(28); Primary("송금","home_transfer"){go("transfer")}
        Gap(4)
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)){Secondary("가져오기","home_bring"){go("bring")}}
            Box(Modifier.weight(1f)){Secondary("거래 내역","home_history"){go("history")}}
        }
        Gap(18); Rule(); Gap(18)
        val needs=pending(state) || state.heldCase
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            TraceMark(Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Copy(if(needs) "확인을 기다리는 송금이 있어요" else "필요한 순간에, TRACE",size=15,color=TraceColors.Ink,weight=FontWeight.SemiBold)
                Copy(if(needs) "돈은 나가지 않았습니다" else "송금 전에 맥락을 함께 확인해요",size=12)
            }
            GlyphButton("chevron","안전 센터","home_safety"){go("safety")}
        }
        Gap(22); Rule(); Gap(16)
        SectionHeading("최근 거래","전체 보기"){go("history")}
        state.ledger.take(3).forEach { item ->
            ListRow(item.title,dateLabel(item.at,"MM.dd")+" · "+item.category,(if(item.signedAmount>0) "+" else "−")+money(kotlin.math.abs(item.signedAmount))+"원",tag="home_ledger_${item.id}",accent=item.signedAmount>0,onClick={go("receipt/${item.receiptId ?: item.id}")})
        }
        if(state.schedules.any{it.active}) {
            Gap(20); Rule(); Gap(12)
            SectionHeading("다가오는 자동이체","관리"){go("schedules")}
            state.schedules.filter{it.active}.take(1).forEach { ListRow(it.recipient.name,"매월 ${it.day}일",money(it.amount)+"원",icon="clock",onClick={go("schedules")}) }
        }
        Gap(22); Copy("시연 계좌 · 실제 자금은 이동하지 않습니다",size=11)
    }
}

@Composable fun AssetsScreen(state: BankState, go: (String)->Unit) {
    Screen("내 자산",tag="assets_screen") {
        Gap(12); Eyebrow("보유 자산"); Gap(8); Amount(state.totalAssets,"assets_total")
        Gap(8); Copy("입출금과 적금을 합친 금액이에요",size=13)
        Gap(28); Rule()
        DetailRow("대출 잔액",money(state.loan)+"원")
        DetailRow("순자산",money(state.netAssets)+"원")
        Gap(24)
        SectionHeading("계좌")
        ListRow("새온 생활통장","입출금 · 0001",money(state.balance)+"원",icon="bank",tag="asset_primary_account",onClick={go("account/living")})
        Rule()
        ListRow("새온 모아적금","차곡차곡 모으는 돈",money(state.savings)+"원",icon="assets",tag="asset_savings",onClick={go("account/savings")})
        Gap(24); SectionHeading("카드")
        val spending=state.ledger.filter{it.category=="카드 결제" && it.signedAmount<0}.sumOf{-it.signedAmount}
        ListRow("새온 체크카드","이번 달 이용 금액",money(spending)+"원",icon="card",tag="asset_card",onClick={go("account/card")})
        Gap(24); SectionHeading("대출")
        ListRow("생활안심대출","남은 원금",money(state.loan)+"원",icon="receipt",tag="asset_loan",onClick={go("account/loan")})
        Gap(24); QuietNotice("자산과 대출은 시연용 가상 내역입니다. 카드 이용 금액은 자산 합계에 더하지 않습니다.")
    }
}

@Composable fun AccountScreen(state: BankState, id: String, go: (String)->Unit, back: ()->Unit) {
    val title=when(id){"savings"->"새온 모아적금";"loan"->"생활안심대출";"card"->"새온 체크카드";else->"새온 생활통장"}
    Screen(title,tag="account_detail",onBack=back) {
        Gap(12)
        Eyebrow(when(id){"loan"->"남은 대출 원금";"card"->"이번 달 이용 금액";else->"현재 잔액"})
        Gap(12)
        Amount(when(id){"savings"->state.savings;"loan"->state.loan;"card"->state.ledger.filter{it.category=="카드 결제"}.sumOf{kotlin.math.abs(it.signedAmount)};else->state.balance},"account_balance")
        Gap(12)
        Copy(when(id){"loan"->"상환은 은행에 등록된 공식 경로로만 진행하세요.";"card"->"새온은행 · 체크카드 4581";"savings"->"새온은행 200-***-0024";else->"새온은행 110-***-0001"},size=13)
        Gap(28)
        when(id) {
            "living" -> {
                Primary("송금"){go("transfer")}; Secondary("내 적금에서 가져오기"){go("bring")}
                Gap(16); Rule(); Gap(14); SectionHeading("최근 거래","전체 내역"){go("history")}
                state.ledger.take(5).forEach { LedgerRow(it,go) }
            }
            "savings" -> {
                Primary("생활통장으로 가져오기"){go("bring")}; Gap(24)
                DetailRow("연결 계좌","새온 생활통장")
                DetailRow("가져올 수 있는 금액",money(state.savings)+"원")
                Gap(); QuietNotice("시연에서는 수수료 없이 내 계좌 사이의 가상 잔액을 옮깁니다. 실제 적금 중도해지 조건을 나타내지 않습니다.")
            }
            "loan" -> {
                Primary("상환 안내 확인","loan_official_guide"){go("guide")}; Gap(24)
                DetailRow("상환 기관","새온은행")
                DetailRow("공식 수취인","새온은행 대출상환센터")
                DetailRow("등록 상환 계좌","200-***-3014")
                Gap(); QuietNotice("개인 계좌로 상환하라는 연락을 받았다면, 송금 전에 TRACE로 내용을 확인하세요.","shield",true)
                ListRow("받은 내용 확인하기",icon="message",onClick={go("share")})
            }
            "card" -> {
                SectionHeading("카드 이용 내역")
                state.ledger.filter{it.category=="카드 결제"}.forEach { LedgerRow(it,go) }
                Gap(24); DetailRow("결제 계좌","새온 생활통장")
                DetailRow("이용 상태","시연용 체크카드")
                Gap(); QuietNotice("실물 카드와 연결되지 않은 가상 이용 내역입니다.")
            }
        }
    }
}

@Composable fun LedgerRow(item: LedgerEntry, go: (String)->Unit) {
    ListRow(item.title,dateLabel(item.at)+" · "+item.category,(if(item.signedAmount>0) "+" else "−")+money(kotlin.math.abs(item.signedAmount))+"원",tag="ledger_${item.id}",accent=item.signedAmount>0,onClick={go("receipt/${item.receiptId ?: item.id}")})
}

@Composable fun RecipientScreen(state: BankState, vm: BankViewModel, go: (String)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val suggested=Fixtures.defaultRecipient(state.scenario)
    Screen("송금",tag="transfer_recipient") {
        Title("누구에게\n보낼까요?"); Gap(24)
        LabeledField(query,{query=it},"이름 또는 계좌 검색","recipient_search")
        Gap(12)
        ListRow("계좌번호로 보내기",icon="plus",tag="manual_account",onClick={go("manual-account")})
        Gap(12); Rule(); Gap(16)
        if(query.isBlank()) {
            SectionHeading(if(state.scenario==Scenario.NORMAL) "자주 보내는 사람" else "이번에 보낼 사람")
            ListRow(suggested.name,suggested.displayAccount,icon="profile",tag="recipient_${suggested.id}",onClick={vm.start(suggested)})
            Gap(14); SectionHeading("최근 수취인")
        } else SectionHeading("검색 결과")
        val list=Fixtures.recipients.filter { (query.isNotBlank() || it.id!=suggested.id) && (query.isBlank() || it.name.contains(query) || it.account.contains(query) || it.bank.contains(query)) }
        if(list.isEmpty()) { Gap(18); Copy("일치하는 시연 수취인이 없어요.\n이름이나 계좌 뒷자리를 다시 확인해 주세요.") }
        list.forEach { r -> ListRow(r.name,r.displayAccount,icon="profile",tag="recipient_${r.id}",onClick={vm.start(r)}) }
        Gap(24); Copy("등록된 가상 수취인에게만 보낼 수 있어요.",size=12)
    }
}

@Composable fun HistoryScreen(state: BankState, go: (String)->Unit, back: ()->Unit) {
    var filter by rememberSaveable { mutableStateOf("전체") }
    var query by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableIntStateOf(30) }
    var periodSheet by remember { mutableStateOf(false) }
    Screen("거래 내역",tag="history_screen",onBack=back,actions={GlyphButton("filter","기간 선택","history_period"){periodSheet=true}}) {
        Eyebrow("새온 생활통장"); Gap(8); Amount(state.balance,size=32); Gap(24)
        LabeledField(query,{query=it},"거래 이름 검색","history_search")
        Gap(12)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { listOf("전체","입금","출금").forEach { value -> FilterChip(selected=filter==value,onClick={filter=value},label={Text(value)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=TraceColors.Soft,selectedLabelColor=TraceColors.Action)) } }
        Gap(10); Copy("최근 ${days}일",size=12); Gap(4)
        val items=state.ledger.filter { item -> item.at>=state.now()-days*86_400_000L && item.title.contains(query) && when(filter){"입금"->item.signedAmount>0;"출금"->item.signedAmount<0;else->true} }
        if(items.isEmpty()){Gap(32);Title("해당 내역이 없어요",23);Gap(8);Copy("다른 기간이나 검색어로 확인해 보세요.")}
        items.forEach { LedgerRow(it,go); Rule() }
        Gap(24); Copy("보류되거나 확인 중인 송금은 완료 내역에 포함하지 않습니다.",size=12)
    }
    if(periodSheet) TraceSheet("조회 기간",{periodSheet=false}) {
        listOf(7,30,90,365).forEach { n -> ListRow("최근 ${n}일",value=if(n==days) "선택됨" else null,onClick={days=n;periodSheet=false}) }
    }
}

@Composable fun ReceiptScreen(state: BankState, id: String, back: ()->Unit) {
    val receipt=state.receipts.firstOrNull{it.id==id}
    val ledger=state.ledger.firstOrNull{it.id==id || it.receiptId==id}
    Screen("거래 상세",tag="receipt_detail",onBack=back,footer={Primary("확인"){back()}}) {
        if(receipt==null && ledger==null) {Title("내역을 찾을 수 없어요");Gap();Copy("시연 데이터가 초기화됐을 수 있습니다.")}
        else {
            Eyebrow(if(receipt!=null) "송금 완료" else ledger!!.category);Gap(16)
            Amount(receipt?.amount ?: kotlin.math.abs(ledger!!.signedAmount),size=36)
            Gap(12); Title(receipt?.recipient?.name ?: ledger!!.title,24); Gap(30); Rule(); Gap(10)
            if(receipt!=null) {
                DetailRow("받는 계좌",receipt.recipient.displayAccount)
                DetailRow("출금 계좌","새온 생활통장")
                DetailRow("송금 목적",purposeLabel(receipt.purpose))
                DetailRow("수수료","0원")
                DetailRow("거래 일시",dateLabel(receipt.at,"yyyy.MM.dd HH:mm:ss"))
                DetailRow("남은 잔액",money(receipt.balanceAfter)+"원")
                DetailRow("거래 번호",receipt.id)
            } else {
                DetailRow("거래 구분",ledger!!.category)
                DetailRow("거래 일시",dateLabel(ledger.at,"yyyy.MM.dd HH:mm"))
                DetailRow("계좌","새온 생활통장")
                DetailRow("기록 구분","초기 시연 내역")
            }
            Gap(24); QuietNotice("시연용 가상 거래입니다. 실제 자금은 이동하지 않습니다.","receipt")
        }
    }
}

@Composable fun BringScreen(state: BankState, vm: BankViewModel, back: ()->Unit) {
    var input by rememberSaveable { mutableStateOf("100000") }
    var confirm by remember { mutableStateOf(false) }
    val amount=input.toLongOrNull() ?: 0
    Screen("내 계좌에서 가져오기",tag="bring_screen",onBack=back,footer={Primary("${money(amount)}원 가져오기","bring_confirm",amount>0 && amount<=state.savings){confirm=true}}) {
        Title("모아둔 돈을\n생활통장으로");Gap(16)
        Copy("가상 자산 합계는 그대로이고, 두 계좌의 잔액만 바뀝니다.")
        Gap(28); DetailRow("가져올 계좌","새온 모아적금")
        DetailRow("사용 가능한 금액",money(state.savings)+"원");Gap(20)
        LabeledField(input,{input=it.filter(Char::isDigit).take(9)},"가져올 금액","bring_amount",error=amount>state.savings)
        if(amount>state.savings){Gap(8);Copy("적금 잔액 안에서 입력해 주세요.",color=TraceColors.Action,size=13)}
        Gap(24); DetailRow("받을 계좌","새온 생활통장")
        DetailRow("이동 후 잔액",money(state.balance+amount)+"원")
    }
    if(confirm) TraceSheet("이 금액을 가져올까요?",{confirm=false}) {
        Amount(amount,size=32);Gap(16);Copy("새온 모아적금에서 생활통장으로 이동합니다.");Gap(24)
        Primary("가상 자금 이동 확인","bring_execute"){confirm=false;vm.bring(amount)}
    }
}

@Composable fun NoticesScreen(state: BankState, vm: BankViewModel, back: ()->Unit) {
    Screen("알림",tag="notifications_screen",onBack=back,actions={TextButton({vm.markNoticesRead()}){Text("모두 읽음",color=TraceColors.Muted)}}) {
        if(state.notices.isEmpty()){Gap(32);Title("새로운 알림이 없어요",24)}
        state.notices.forEach { n ->
            ListRow(n.title,n.body+"\n"+dateLabel(n.at),value=if(n.read) null else "새 알림",tag="notice_${n.id}",onClick={vm.notice(n.id,n.route)})
            Rule()
        }
        Gap(28); Copy("이 알림함은 시연 앱 안에서만 동작합니다. 외부로 푸시를 전송하지 않습니다.",size=12)
    }
}
