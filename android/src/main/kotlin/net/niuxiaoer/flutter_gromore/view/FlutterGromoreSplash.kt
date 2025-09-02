package net.niuxiaoer.flutter_gromore.view

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.mediation.ad.MediationAdSlot
import net.niuxiaoer.flutter_gromore.R
import net.niuxiaoer.flutter_gromore.event.AdEvent
import net.niuxiaoer.flutter_gromore.event.AdEventHandler
import net.niuxiaoer.flutter_gromore.utils.Utils
import net.niuxiaoer.flutter_gromore.utils.UIUtils
import java.util.*
import kotlin.concurrent.schedule
import com.jaeger.library.StatusBarUtil
import android.widget.FrameLayout;
import android.widget.LinearLayout;


// Activity实例
class FlutterGromoreSplash : Activity() {

    private val TAG: String = this::class.java.simpleName

    // 广告容器
    private lateinit var container: FrameLayout
    private var splashAd: CSJSplashAd? = null

    // activity id
    private lateinit var id: String

    // 广告容器宽高
    private var containerWidth: Int = 0
    private var containerHeight: Int = 0

    // 广告未展示时 自动关闭广告的延时器
    private var closeAdTimer = Timer()

    // 广告已经展示时 自动关闭广告的延时器
    private var skipAdTimer = Timer()

    // 广告已经展示
    private var adShow = false

    // 是否已经调用了关闭
    private var isFinishCalled = false

    // 初始化广告
    private fun initAd() {
        var tmp = intent.getStringExtra("id")
        require(tmp != null)
        id = tmp

        val adUnitId = intent.getStringExtra("adUnitId")
        require(adUnitId != null && adUnitId.isNotEmpty())
        val muted = intent.getBooleanExtra("muted", true)
        val preload = intent.getBooleanExtra("preload", true)
        val volume = intent.getFloatExtra("volume", 1f)
        val isSplashShakeButton = intent.getBooleanExtra("splashShakeButton", true)
        val isBidNotify = intent.getBooleanExtra("bidNotify", false)
        val timeout = intent.getIntExtra("timeout", 3500);
        val useSurfaceView = intent.getBooleanExtra("useSurfaceView", true)
        val adNativeLoader = TTAdSdk.getAdManager().createAdNative(this)
        val adSlot = AdSlot.Builder()
            .setCodeId(adUnitId)
            .setImageAcceptedSize(containerWidth, containerHeight)
            //.setExpressViewAcceptedSize(containerWidth/Utils.getDensity(this), containerHeight/Utils.getDensity(this))
            .setMediationAdSlot(
                MediationAdSlot.Builder()
                    .setSplashPreLoad(preload)
                    .setMuted(muted)
                    .setVolume(volume)
                    .setSplashShakeButton(isSplashShakeButton)
                    .setBidNotify(isBidNotify)
                    .setUseSurfaceView(useSurfaceView)
                    .build()
            )
            .build()
        adNativeLoader.loadSplashAd(adSlot, getCSJSplashAdListener(), timeout)
        // 6秒后广告未展示，延时自动关闭
        closeAdTimer.schedule(6000) {
            if (!isFinishing && !adShow) {
                runOnUiThread {
                    Log.d(TAG, "closeAdTimer exec")
                    sendEvent("onAutoClose")
                    finishActivity()
                }
            }
        }
    }

    // 初始化
    private fun init() {
        setContentView(R.layout.splash)
        container = findViewById(R.id.splash_container)
        val screenSize = UIUtils.getScreenSize(this)
        val screenW = screenSize[0];
        val screenH = screenSize[1];
        Log.d(TAG, "screenSize: ${screenSize[0]}, ${screenSize[1]}")
        val realW = UIUtils.getScreenWidthInPx(this)
        val realH = UIUtils.getRealHeight(this)
        containerWidth = realW
        containerHeight = realH
        Log.d(TAG, "screenSize: $realW, realH: $realH")
        // container.post {
        //     val cW = container.width
        //     val cH = container.height
        //     containerWidth = cW
        //     containerHeight = cH
        //     Log.d("FlutterGromoreSplash", "containerWidth: $cW, cH: $cH")
        //     //打 log 输出宽高
        //     Log.d(TAG, "containerWidth: $containerWidth, containerHeight: $containerHeight")
        //     initAd()
        // }
        initAd()
    }

    // logo的显示与否
    // private fun handleLogo() {
        // val logo = intent.getStringExtra("logo")

        // val id = logo.takeIf {
        //     logo != null && logo.isNotEmpty()
        // }?.let {
        //     getMipmapId(it)
        // }

        // if (id != null && id > 0) {
        //     // 找得到图片资源，设置
        //     logoContainer.apply {
        //         visibility = View.VISIBLE
        //         setImageResource(id)
        //     }

        //     containerHeight -= logoContainer.layoutParams.height
        // } else {
        //     logoContainer.visibility = View.GONE
        //     Log.e(TAG, "Logo 名称不匹配或不在 mipmap 文件夹下，展示全屏")
        // }

    // }

    /**
     * 获取图片资源的id
     * @param resName 资源名称，不带后缀
     * @return 返回资源id
     */
    private fun getMipmapId(resName: String) =
        resources.getIdentifier(resName, "mipmap", packageName)

    // 发送事件
    private fun sendEvent(msg: String) = AdEventHandler.getInstance().sendEvent(AdEvent(id, msg))

    @Synchronized
    private fun finishActivity() {
        if (isFinishing) {
            return
        }
        if(isFinishCalled){
            return;
        }
        isFinishCalled = true
        handleFinishFlow()
        // container?.removeAllViews()
        // 设置退出动画
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    fun handleFinishFlow(){
        closeAdTimer.cancel()
        skipAdTimer.cancel()
        splashAd = null
        Utils.splashResult?.success(true);
        Utils.splashResult = null;
        sendEvent("onAdEnd")
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    private fun getCSJSplashAdListener(): TTAdNative.CSJSplashAdListener {
        return object : TTAdNative.CSJSplashAdListener {
            // 加载成功
            override fun onSplashLoadSuccess(ad: CSJSplashAd) {
                if (ad == null) {
                    Log.d(TAG, "splashView is null")
                    finishActivity()
                    return;
                }else{
                    splashAd = ad
                    splashAd?.showSplashView(container)
                }
            }

            // 加载失败
            override fun onSplashLoadFail(error: CSJAdError) {
                Log.d(TAG, "onSplashAdLoadFail ${error.msg}")
                sendEvent("onSplashAdLoadFail")
                finishActivity()
            }

            // 渲染成功
            override fun onSplashRenderSuccess(ad: CSJSplashAd) {
                Log.d(TAG, "onSplashRenderSuccess")
                sendEvent("onSplashRenderSuccess")
                ad.setSplashAdListener(getSplashAdListener())
                container.postDelayed({
                    val w = container.measuredWidth
                    val h = container.measuredHeight
                    Log.d(TAG, "adShowSize, container width: $w, height: $h") // 打印 container 的宽高
                    //遍历子 view
                    for (i in 0 until container.childCount) {
                        val child = container.getChildAt(i)
                        val w = child.width
                        val h = child.height
                        Log.d(TAG, "adShowSize, child $i width: $w, height: $h") // 打印每个子 view 的宽高
                    }
                }, 100)
            }

            override fun onSplashRenderFail(ad: CSJSplashAd, csjAdError: CSJAdError) {
                Log.d(TAG, "onSplashRenderFail ${csjAdError.msg}")
                sendEvent("onSplashRenderFail")

                finishActivity()
            }
        }
    }

    private fun getSplashAdListener(): CSJSplashAd.SplashAdListener {
        return object : CSJSplashAd.SplashAdListener {

            // 开屏展示
            override fun onSplashAdShow(p0: CSJSplashAd?) {
                adShow = true
                closeAdTimer.cancel()
                Log.d(TAG, "onAdShow")
                sendEvent("onAdShow")
                // 6s后自动跳过广告
                skipAdTimer.schedule(6000) {
                    if (!isFinishing) {
                        runOnUiThread {
                            Log.d(TAG, "skipAdTimer exec")
                            sendEvent("onAutoSkip")
                            finishActivity()
                        }
                    }
                }
            }

            // 开屏点击
            override fun onSplashAdClick(p0: CSJSplashAd?) {
                Log.d(TAG, "onAdClicked")
                sendEvent("onAdClicked")
            }

            // 开屏关闭，有些ADN会调用多次close回调需要开发者特殊处理
            override fun onSplashAdClose(p0: CSJSplashAd?, p1: Int) {
                Log.d(TAG, "onSplashAdClose")
                sendEvent("onSplashAdClose")
                finishActivity()
            }

        }
    }


}