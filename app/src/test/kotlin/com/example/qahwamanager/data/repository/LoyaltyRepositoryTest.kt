package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.testing.FakeLoyaltyCustomerDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LoyaltyRepositoryTest {

    private var currentMillis = 1_000_000L
    private val clock = object : Clock {
        override fun nowMillis(): Long = currentMillis
    }

    @Test
    fun enrollAddStampAndRedeemReward_updatesStampsAndRedemptions() = runTest {
        val dao = FakeLoyaltyCustomerDao()
        val repo = LoyaltyRepository(dao, clock)
        repo.updateConfig(stampsRequired = 3, rewardTitle = "Free Espresso", googleReviewUrl = "")

        val customer = repo.enrollCustomer("Yassine", "06 12 34 56 78", addInitialStamp = true)
        assertEquals("+212612345678", customer.phone)
        assertEquals(1, customer.currentStamps)
        assertEquals(1, customer.lifetimeStamps)

        // Cannot redeem before reaching 3 stamps
        assertNull(repo.redeemReward(customer.id))

        // Stamp by phone & by QR code URI
        repo.stampByCodeOrPhone("0612345678")
        val afterThird = repo.stampByCodeOrPhone("qahwa://loyalty/${customer.remoteId}")
        assertNotNull(afterThird)
        assertEquals(3, afterThird!!.currentStamps)
        assertEquals(3, afterThird.lifetimeStamps)

        // Redeem reward -> deducts 3 stamps and increments rewardsRedeemed
        val redeemed = repo.redeemReward(customer.id)
        assertNotNull(redeemed)
        assertEquals(0, redeemed!!.currentStamps)
        assertEquals(3, redeemed.lifetimeStamps)
        assertEquals(1, redeemed.rewardsRedeemed)
    }
}
