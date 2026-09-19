package dev.sp2ctr2.saeon

import android.app.Application
import dev.sp2ctr2.saeon.data.BankRepository
import dev.sp2ctr2.saeon.data.Preferences
import dev.sp2ctr2.saeon.security.KeystoreSigner

class SaeonApplication : Application() {
    val repository by lazy { BankRepository(this) }
    val preferences by lazy { Preferences(this) }
    val signer by lazy { KeystoreSigner() }
}
