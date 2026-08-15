package com.example.data.repository

import android.util.Log
import com.example.data.model.User
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

sealed class CouponResult {
    data class Success(val message: String, val updatedUser: User) : CouponResult()
    data class Error(val message: String) : CouponResult()
}

class CouponRepository(
    private val firestoreService: FirestoreService = FirestoreService(),
    private val userRepository: UserRepository = UserRepository(firestoreService)
) {
    private val tag = "CouponRepository"

    suspend fun seedDefaultCouponsIfEmpty() {
        val db = firestoreService.db ?: return
        try {
            val snapshot = db.collection("coupons").get().await()
            if (snapshot.isEmpty) {
                val sampleCoupons = listOf(
                    mapOf(
                        "code" to "FRICTIONMEMBER2026",
                        "planType" to "LIFETIME",
                        "active" to true,
                        "maxUses" to 10000,
                        "currentUses" to 0,
                        "expiresAt" to System.currentTimeMillis() + (3650L * 24 * 3600 * 1000)
                    ),
                    mapOf(
                        "code" to "MONTHLYFRICTION",
                        "planType" to "MONTHLY",
                        "active" to true,
                        "maxUses" to 10000,
                        "currentUses" to 0,
                        "expiresAt" to System.currentTimeMillis() + (3650L * 24 * 3600 * 1000)
                    ),
                    mapOf(
                        "code" to "ANNUALFRICTION",
                        "planType" to "ANNUAL",
                        "active" to true,
                        "maxUses" to 10000,
                        "currentUses" to 0,
                        "expiresAt" to System.currentTimeMillis() + (3650L * 24 * 3600 * 1000)
                    )
                )
                for (coupon in sampleCoupons) {
                    val codeStr = coupon["code"] as String
                    db.collection("coupons").document(codeStr).set(coupon).await()
                }
                Log.i(tag, "Seeded default Firestore coupon documents in 'coupons' collection.")
            }
        } catch (e: Exception) {
            Log.w(tag, "Seeding default coupons skipped: ${e.message}")
        }
    }

    suspend fun redeemCoupon(code: String, user: User, authRepository: AuthRepository): CouponResult {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            return CouponResult.Error("Please enter a valid coupon code.")
        }

        val db = firestoreService.db
        if (db == null) {
            return CouponResult.Error("Server network error. Please check internet connection.")
        }

        return try {
            seedDefaultCouponsIfEmpty()

            // Check if user already redeemed this coupon
            val redemptionDocRef = db.collection("coupon_redemptions").document("${user.uid}_$cleanCode")
            val existingRedemption = redemptionDocRef.get().await()
            if (existingRedemption.exists()) {
                return CouponResult.Error("You have already redeemed this coupon code.")
            }

            val docRef = db.collection("coupons").document(cleanCode)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                return CouponResult.Error("Invalid or expired coupon.")
            }

            val active = doc.getBoolean("active") ?: false
            val maxUses = doc.getLong("maxUses") ?: 0L
            val currentUses = doc.getLong("currentUses") ?: 0L
            val expiresAt = doc.getLong("expiresAt") ?: 0L
            val now = System.currentTimeMillis()

            if (!active) {
                return CouponResult.Error("Invalid or expired coupon.")
            }

            if (maxUses > 0 && currentUses >= maxUses) {
                return CouponResult.Error("Invalid or expired coupon.")
            }

            if (expiresAt > 0 && now > expiresAt) {
                return CouponResult.Error("Invalid or expired coupon.")
            }

            val rawPlan = doc.getString("planType") ?: doc.getString("discountType") ?: "PRO"
            val subStatus = when (rawPlan.uppercase()) {
                "LIFETIME", "LIFETIME_PRO" -> "LIFETIME_PRO"
                "MONTHLY", "MONTHLY_PRO" -> "MONTHLY_PRO"
                "ANNUAL", "ANNUAL_PRO" -> "ANNUAL_PRO"
                else -> "COUPON_PRO"
            }
            val planName = when (rawPlan.uppercase()) {
                "LIFETIME", "LIFETIME_PRO" -> "LIFETIME"
                "MONTHLY", "MONTHLY_PRO" -> "MONTHLY"
                "ANNUAL", "ANNUAL_PRO" -> "ANNUAL"
                else -> "PRO"
            }
            val activatedText = when (planName) {
                "LIFETIME" -> "Lifetime Premium activated"
                "MONTHLY" -> "Monthly Premium activated"
                "ANNUAL" -> "Annual Premium activated"
                else -> "Pro Membership activated"
            }

            // Server-side record redemption & increment usage
            docRef.update("currentUses", FieldValue.increment(1)).await()
            val redemptionRecord = mapOf(
                "uid" to user.uid,
                "couponCode" to cleanCode,
                "redeemedAt" to now,
                "planType" to planName
            )
            redemptionDocRef.set(redemptionRecord).await()

            // Update user entitlement
            val updatedUser = user.copy(
                premium = true,
                isTrialActive = false,
                subscriptionStatus = subStatus,
                premiumPlan = planName
            )

            userRepository.createOrUpdateUser(updatedUser)
            authRepository.saveCachedUser(updatedUser)

            CouponResult.Success("Premium unlocked! $activatedText", updatedUser)
        } catch (e: Exception) {
            Log.e(tag, "Coupon validation failed", e)
            CouponResult.Error("Invalid or expired coupon.")
        }
    }
}
