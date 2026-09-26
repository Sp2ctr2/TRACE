package app.saeon.trace.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.saeon.trace.core.*
import app.saeon.trace.ui.design.*
import app.saeon.trace.ui.screens.BankRow

@Composable fun QuickDemoCenter(model:BankViewModel,open:(String)->Unit) {
    val session by model.stage.collectAsStateWithLifecycle()
    val interaction by model.interaction.collectAsStateWithLifecycle()
    var all by rememberSaveable{mutableStateOf(false)}
    var lock by remember{mutableStateOf(false)}
    if(!session.unlocked){Page("시연센터","demo_center_locked"){Body("앱 정보에서 시연센터를 활성화해 주세요.")};return}
    val featured=listOf(DemoScenario.NORMAL,DemoScenario.IMPERSONATION,DemoScenario.LOAN)
    val extra=listOf(DemoScenario.WARN,DemoScenario.NEW_ACCOUNT,DemoScenario.FAMILY_FAKE,DemoScenario.REMOTE,DemoScenario.INVESTMENT,DemoScenario.UNKNOWN,DemoScenario.EDUCATION,DemoScenario.UNRELATED,DemoScenario.EASY)
    Page("시연센터","demo_center",actions={IconAction(BankIcons.More,"전체 메뉴"){open("more")}}) {
        Space(8);Text("짧게 경험하는 TRACE",style=MaterialTheme.typography.headlineSmall);Space(10)
        Body("상황을 고르면 송금 확인부터 시작해요.",subdued=true);Space(18)
        featured.forEach{scenario->DemoCaseRow(scenario,!interaction.busy){model.stageScenario(scenario){open("transfer_state")}}}
        Space(12);Rule();Space(6)
        BankRow("같은 300만 원, 다른 맥락","계좌·금액은 그대로 두고 비교해요",BankIcons.Transfer,tag="demo_compare"){open("comparison")}
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("다른 상황",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall);QuietButton(if(all)"접기"else"모두 보기",Modifier.testTag("demo_more")){all=!all}}
        (if(all)extra else extra.take(2)).forEach{scenario->DemoCaseRow(scenario,!interaction.busy){model.stageScenario(scenario){open("transfer_state")}}}
        Space(18);Caption("상황마다 가상 잔액을 초기화합니다. 버튼이 결과를 정하지 않으며, 인증과 로컬 정책 판단을 그대로 거쳐요.")
        Space(12);QuietButton("시연센터 숨기기",Modifier.fillMaxWidth().testTag("demo_hide")){lock=true}
    }
    if(lock)AlertDialog(onDismissRequest={lock=false},title={Text("시연센터를 숨길까요?")},text={Text("하단 전체 메뉴로 돌아갑니다. 거래 기록은 유지해요.")},confirmButton={QuietButton("숨기기",Modifier.testTag("demo_hide_confirm")){model.stageLock();lock=false;open("home")}},dismissButton={QuietButton("취소"){lock=false}})
}
@Composable private fun DemoCaseRow(scenario:DemoScenario,enabled:Boolean,onClick:()->Unit) {
    val title=when(scenario){DemoScenario.NORMAL->"정상 송금";DemoScenario.IMPERSONATION->"위험 송금";DemoScenario.LOAN->"대출 상환";DemoScenario.WARN->"링크 유도";DemoScenario.NEW_ACCOUNT->"처음 보내는 계좌";DemoScenario.EDUCATION->"만료된 교육 신호";DemoScenario.UNRELATED->"다른 거래의 링크";else->scenario.label}
    val subtitle=when(scenario){DemoScenario.NORMAL->"이서연님에게 32,000원 정산";DemoScenario.IMPERSONATION->"기관 사칭과 즉시 300만 원 요구";DemoScenario.LOAN->"개인 계좌로 800만 원 선상환 요구";else->scenarioNote(scenario)}
    Row(Modifier.fillMaxWidth().heightIn(min=72.dp).clickable(enabled=enabled,role=Role.Button,onClick=onClick).testTag("demo_${scenario.name}").padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.SemiBold);Caption(subtitle)}
        AppIcon(BankIcons.Chevron,size=16,tint=TraceColors.Muted)
    }
}
