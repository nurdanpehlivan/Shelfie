package com.shelfie.app.data.install

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstallReferrerSourceTest {

    @Test
    fun `reads the invite code from the referrer Play hands over`() {
        assertEquals("ABCD2345", InstallReferrerSource.inviteCodeFrom("joinCode=ABCD2345"))
        assertEquals("ABCD2345", InstallReferrerSource.inviteCodeFrom("utm_source=google-play&joinCode=abcd2345"))
    }

    @Test
    fun `ignores installs without a valid code`() {
        assertNull(InstallReferrerSource.inviteCodeFrom("utm_source=google-play&utm_medium=organic"))
        assertNull(InstallReferrerSource.inviteCodeFrom("joinCode=ABC"))
        assertNull(InstallReferrerSource.inviteCodeFrom("joinCode=ABCD0OIL"))
    }
}
