package com.appyhigh.adutils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appyhigh.adutils.databinding.ActivityBannerAdBinding
import com.google.android.gms.ads.AdSize

class BannerAdActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBannerAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBannerAdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdSdk().loadBannerAd(
            binding.llRoot,
            bannerAdUnit,
            AdSize.BANNER,
            null
        )

        AdSdk().loadBannerAd(
            binding.llRoot,
            bannerAdUnit,
            AdSize.LARGE_BANNER,
            null
        )

        AdSdk().loadBannerAd(
            binding.llRoot,
            bannerAdUnit,
            AdSize.MEDIUM_RECTANGLE,
            null
        )
    }

    companion object {
        private val bannerAdUnit = "ca-app-pub-3940256099942544/6300978111"
    }
}