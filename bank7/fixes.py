from pathlib import Path
r=Path('android-native');c=r/'core/src/main/kotlin/app/saeon/trace/core';u=r/'app/src/main/kotlin/app/saeon/trace/ui'
p=r/'app/src/androidTest/kotlin/app/saeon/trace/Bank7Test.kt';s=p.read_text().replace('official_new_transfer','official_route_use');p.write_text(s)
p=c/'BankEngine.kt';s=p.read_text();a='        requireBank(amount > 0 && amount <= state.savings,';assert a in s;s=s.replace(a,'        requireBank(!state.accountLocked, "ACCOUNT_LOCKED", "계정이 잠겨 있어요. 먼저 계정 보호를 확인해 주세요.")\n'+a);p.write_text(s)
# Ensure ordinary WARN uses the same centered confirmation underneath the sheet.
p=u/'screens/MotionWarn.kt';s=p.read_text().replace('ViewportReview(state,record,interaction,model,open,back)','BankReview(state,record,interaction,model,open,back)');p.write_text(s)
print('Applied bank7 deterministic regression alignment.')
