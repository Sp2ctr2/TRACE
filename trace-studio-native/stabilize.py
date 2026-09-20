"""Bounded root-reattachment wait. A missing screen still fails after 10 seconds."""
from pathlib import Path
import sys
root = Path(sys.argv[1] if len(sys.argv)>1 else 'android-native')
p = root / 'app/src/androidTest/kotlin/app/saeon/trace/UiHarness.kt'
s = p.read_text()
old = 'compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }'
new = '''// Activity recreation temporarily detaches the root. Treat that interval as not ready,
        // rather than throwing before waitUntil can evaluate its timeout. All assertions remain.
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }'''
assert old in s or new in s
p.write_text(s.replace(old,new))
print('Bounded reattachment wait installed; test count and assertions unchanged.')
