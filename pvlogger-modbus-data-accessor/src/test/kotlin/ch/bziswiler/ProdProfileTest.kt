package ch.bziswiler

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ADR-0025: in the profile every image runs, client timestamps are off and no
 * environment variable can turn them on; only the file a dev image build
 * writes can. Parsed unresolved, so a `${?VAR}` override would show up as an
 * unresolved value.
 */
class ProdProfileTest {

    @Test
    fun `client timestamps are off and have no environment override`() {
        val dev = ConfigFactory.parseResources("application-prod.conf").getConfig("dev")
        assertTrue(dev.isResolved, "dev.* must not read the environment")
        assertFalse(dev.getBoolean("acceptClientTimestamps"))
    }
}
