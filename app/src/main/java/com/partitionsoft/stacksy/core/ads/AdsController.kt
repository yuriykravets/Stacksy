package com.partitionsoft.stacksy.core.ads

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.partitionsoft.stacksy.BuildConfig
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdsUiState(
    val rewardedReady: Boolean = false,
    val rewardedLoading: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
)

class AdsController(
    private val activity: ComponentActivity,
) {
    private val consentInformation =
        UserMessagingPlatform.getConsentInformation(activity.applicationContext)
    private val adsStarted = AtomicBoolean(false)
    private val _uiState = MutableStateFlow(AdsUiState())
    val uiState: StateFlow<AdsUiState> = _uiState.asStateFlow()

    private var rewardedAd: RewardedAd? = null

    fun start() {
        val parameters = ConsentRequestParameters.Builder()
            .setAdMobAppId(BuildConfig.ADMOB_APP_ID)
            .setTagForUnderAgeOfConsent(true)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                updatePrivacyOptionsStatus()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updatePrivacyOptionsStatus()
                    startAdsIfAllowed()
                }
            },
            {
                updatePrivacyOptionsStatus()
                startAdsIfAllowed()
            },
        )

        // A valid choice from an earlier launch can already allow requests.
        startAdsIfAllowed()
    }

    fun showRewarded(onRewardEarned: () -> Unit) {
        val ad = rewardedAd ?: run {
            if (adsStarted.get()) loadRewarded()
            return
        }
        rewardedAd = null
        _uiState.update { it.copy(rewardedReady = false, rewardedLoading = false) }

        var rewardEarned = false
        var adClosed = false
        var rewardDelivered = false
        fun deliverRewardWhenReady() {
            if (rewardEarned && adClosed && !rewardDelivered) {
                rewardDelivered = true
                onRewardEarned()
            }
        }
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                adClosed = true
                deliverRewardWhenReady()
                loadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError,
            ) {
                loadRewarded()
            }
        }
        ad.show(
            activity,
            OnUserEarnedRewardListener {
                rewardEarned = true
                deliverRewardWhenReady()
            },
        )
    }

    fun showPrivacyOptions() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            updatePrivacyOptionsStatus()
            startAdsIfAllowed()
        }
    }

    private fun startAdsIfAllowed() {
        if (!consentInformation.canRequestAds() || !adsStarted.compareAndSet(false, true)) return

        val requestConfiguration = RequestConfiguration.Builder()
            .setAgeRestrictedTreatment(AgeRestrictedTreatment.CHILD)
            .setMaxAdContentRating(
                RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G
            )
            .setPublisherPrivacyPersonalizationState(
                RequestConfiguration.PublisherPrivacyPersonalizationState.DISABLED
            )
            .build()

        activity.lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(
                activity.applicationContext,
                InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID)
                    .setRequestConfiguration(requestConfiguration)
                    .build(),
            )
            withContext(Dispatchers.Main) {
                loadRewarded()
            }
        }
    }

    private fun loadRewarded() {
        if (BuildConfig.REWARDED_AD_UNIT_ID.isBlank()) return
        if (!adsStarted.get() || rewardedAd != null || _uiState.value.rewardedLoading) return
        _uiState.update { it.copy(rewardedReady = false, rewardedLoading = true) }
        RewardedAd.load(
            AdRequest.Builder(BuildConfig.REWARDED_AD_UNIT_ID).build(),
            object : AdLoadCallback<RewardedAd> {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    _uiState.update {
                        it.copy(rewardedReady = true, rewardedLoading = false)
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    _uiState.update {
                        it.copy(rewardedReady = false, rewardedLoading = false)
                    }
                }
            },
        )
    }

    private fun updatePrivacyOptionsStatus() {
        _uiState.update {
            it.copy(
                privacyOptionsRequired =
                    consentInformation.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
            )
        }
    }

}
