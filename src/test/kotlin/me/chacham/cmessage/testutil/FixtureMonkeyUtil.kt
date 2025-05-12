package me.chacham.cmessage.testutil

import com.navercorp.fixturemonkey.FixtureMonkey
import com.navercorp.fixturemonkey.api.plugin.SimpleValueJqwikPlugin
import com.navercorp.fixturemonkey.kotlin.KotlinPlugin

object FixtureMonkeyUtil {
    val FM = FixtureMonkey.builder().plugin(KotlinPlugin()).plugin(SimpleValueJqwikPlugin().minStringLength(1)).build()
}
