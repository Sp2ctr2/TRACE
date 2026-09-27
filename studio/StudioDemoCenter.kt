package app.saeon.trace.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable fun StudioDemoCenter(model:BankViewModel,open:(String)->Unit) {
    val stage by model.stage.collectAsStateWithLifecycle()
    val busy by model.interaction.collectAsStateWithLifecycle()
    var expanded by rememberSaveable{mutableStateOf(false)}
    if(!stage.unlocked){Page("시연센터","demo_center_locked"){EmptyState("시연센터가 잠겨 있어요.","앱 정보에서 활성화할 수 있습니다.")};return}
    Page("시연센터","demo_center",actions={IconAction(BankIcons.More,"전체 메뉴"){open("more")}}){
        Space(6);Headline("짧게 경험하는\nTRACE의 차이");Space(10)
        Caption("상황을 선택하면 가상 거래를 새로 준비해요. 송금 확인과 인증은 직접 진행합니다.")
        Space(24)
        listOf(
            Triple(DemoScenario.NORMAL,"정상 송금","평소 보내던 사람에게 32,000원"),
            Triple(DemoScenario.IMPERSONATION,"위험 송금","기관 사칭 직후, 처음 보는 계좌로 300만 원"),
            Triple(DemoScenario.LOAN,"대출 상환","개인 명의 계좌로 상환하라는 요청")
        ).forEach{(scenario,title,desc)->
            Column(Modifier.fillMaxWidth().background(TraceColors.Surface,RoundedCornerShape(18.dp))
                .clickable(enabled=!busy.busy,role=Role.Button){model.stageScenario(scenario){open("transfer_state")}}
                .testTag("demo_${scenario.name}").padding(16.dp)){
                Row(verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);AppIcon(BankIcons.Chevron,size=18,tint=TraceColors.Muted)}
                Space(5);Caption(desc)
            };Space(10)
        }
        Space(6)
        MenuRow("같은 300만 원, 다른 맥락","같은 수취인·금액·목적으로 두 상황 비교",BankIcons.Transfer,tag="demo_compare"){open("comparison")}
        Space(12);Rule();Space(8)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("다른 상황",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall);QuietButton(if(expanded)"접기"else"모두 보기",Modifier.testTag("demo_more")){expanded=!expanded}}
        Column(Modifier.animateContentSize()){
            val cases=if(expanded)listOf(DemoScenario.WARN,DemoScenario.NEW_ACCOUNT,DemoScenario.FAMILY_FAKE,DemoScenario.REMOTE,DemoScenario.INVESTMENT,DemoScenario.UNKNOWN,DemoScenario.EDUCATION,DemoScenario.UNRELATED,DemoScenario.EASY)else listOf(DemoScenario.WARN,DemoScenario.NEW_ACCOUNT)
            cases.forEach{s->MenuRow(when(s){DemoScenario.WARN->"확인하지 않은 링크";DemoScenario.NEW_ACCOUNT->"정상적인 첫 거래";DemoScenario.EDUCATION->"종료된 예방 교육";DemoScenario.UNRELATED->"다른 거래의 신호";else->s.label},icon=BankIcons.History,tag="demo_${s.name}"){
                if(!busy.busy)model.stageScenario(s){open("transfer_state")}
            }}
        }
        Space(16);Caption("실제 학습 가중치·금융망 없이 로컬 정책을 평가합니다. 장면을 바꾸면 가상 잔액과 거래 내역이 초기화됩니다.")
        Space(14);SecondaryButton("시연센터 숨기기",Modifier.testTag("demo_hide")){model.stageLock();open("home")}
    }
}
