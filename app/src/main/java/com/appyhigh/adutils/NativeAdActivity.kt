package com.appyhigh.adutils

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.appyhigh.adutils.databinding.ActivityBannerAdBinding
import com.appyhigh.adutils.databinding.ActivityNativeAdBinding

class NativeAdActivity : AppCompatActivity() {
    lateinit var binding: ActivityNativeAdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNativeAdBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AdSdk().loadNativeAd(
            "ca-app-pub-3940256099942544/2247696110",
            binding.llRoot,
            null,
            R.layout.ad_item_big
        )
    }
}