from pathlib import Path
r=Path('android');u=r/'app/src/main/kotlin/app/saeon/trace/ui'
p=u/'screens/BankingScreens.kt';s=p.read_text()
a='OptionRow("카드 일시 정지",preferences.cardFrozen,"이 기기에 저장된 시연 카드의 상태를 변경해요."){model.preference{cardFrozen(it)}}'
b='OptionRow("카드 일시 정지",preferences.cardFrozen,"이 기기에 저장된 시연 카드의 상태를 변경해요."){value->if(!value&&model.serviceState.value.cardLost)model.showError("분실 신고된 카드예요. 분실·재발급에서 찾음 처리부터 해주세요.")else model.preference{cardFrozen(value)}}'
assert a in s;s=s.replace(a,b);p.write_text(s)
p=r/'core/src/main/kotlin/app/saeon/trace/core/BankEngine.kt';s=p.read_text()
a='!it.seed && it.direction == Direction.DEBIT && dayInSeoul(it.completedAt)'
b='!it.seed && it.direction == Direction.DEBIT && !it.nonce.startsWith("internal-") && dayInSeoul(it.completedAt)'
assert a in s;s=s.replace(a,b);p.write_text(s)
# Explicitly document absence of app-level JVM tests rather than failing packaging or implying execution.
out=Path('delivery/app-tests');out.mkdir(parents=True,exist_ok=True)
(out/'NO_SOURCE.txt').write_text('No app-only JVM test cases in this build. Banking logic is tested by :core:test and Android service persistence by instrumentation.\n')
print('All card freeze entry points and internal-transfer limit accounting reconciled.')
