package app.saeon.trace.core

import org.junit.Test

class StateWalkTest {
    @Test fun twentyThousandDeterministicTransitionsPreserveLedgerInvariants() {
        repeat(100) { StateWalk.verify(it) }
    }
}
