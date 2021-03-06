package com.ssafy.nooni

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.core.view.GestureDetectorCompat
import com.ssafy.nooni.viewmodel.SttViewModel
import com.ssafy.nooni.databinding.ActivityRegisterAllergyBinding
import com.ssafy.nooni.util.STTUtil
import java.util.*
import android.content.SharedPreferences

import android.app.Activity
import androidx.fragment.app.Fragment


private const val TAG = "RegisterAllergy"

class RegisterAllergyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterAllergyBinding
    private lateinit var mDetector: GestureDetectorCompat
    private val sttViewModel: SttViewModel by viewModels()
    lateinit var onAnswerListener: OnAnswerListener
    var ready = false
    var tts2: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterAllergyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mDetector = GestureDetectorCompat(this, MyGestureListener())

        initViewModel()
        initSTT()

        val fragment = isFirstRun()

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fc_view)

        if (currentFragment == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fc_view, fragment)
                .commit()
        }
    }

    private fun initSTT() {
        STTUtil.owner = this
        STTUtil.STTVM()
        tts2 = TextToSpeech(this, TextToSpeech.OnInitListener {
            @Override
            fun onInit(status: Int) {
                if (status != ERROR) {
                    tts2?.language = Locale.KOREA
                }
            }
        })

        Log.d("tst6", "onCreate: " + sttViewModel.stt.value)
        //????????? ???????????? tts???????????? ?????? ???????????? ???????????? ?????? ????????? ?????? ????????? ????????? ????????? ???????????????
        tts2?.setSpeechRate(2f)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(Runnable {
            sttViewModel.setStt(resources.getString(R.string.init))
        }, 1000)
    }

    private fun initViewModel() {
        sttViewModel.stt.observe(this) {
            if (ready == false) {
                return@observe
            }

            Log.d("tst6", "onCreate: " + sttViewModel.stt.value)
            val resultString = sttViewModel.stt.value!!
            resources.getStringArray(R.array.yes).forEach {
                if (resultString.indexOf(it) > -1) {
                    Log.d("tst6", "onCreate: yes")
                    onAnswerListener.setAnswer(true)
                    return@observe
                }
            }
            resources.getStringArray(R.array.no).forEach {
                if (resultString.indexOf(it) > -1) {
                    Log.d("tst6", "onCreate: no")
                    onAnswerListener.setAnswer(false)
                    return@observe
                }
            }
        }
    }

    private fun isFirstRun(): Fragment {
        val pref = getSharedPreferences("firstAllergyRegister", MODE_PRIVATE)
        val first = pref.getBoolean("isFirst", false)

        return if (first == false) {
            Log.d("Is first Time?", "first")
            val editor = pref.edit()
            editor.putBoolean("isFirst", true)
            editor.commit()

            ready = false
            TutorialAllergyFragment()
        } else {
            Log.d("Is first Time?", "not first")

            ready = true
            RegisterAllergyFragment()
        }
    }

    fun ttsSpeak(text: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            tts2?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts2?.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    fun startRegisterAllergy() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fc_view, RegisterAllergyFragment())
            .commit()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onRestart() {
        STTUtil.owner = this
        STTUtil.STTVM()
        super.onRestart()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts2?.stop()
        tts2?.shutdown()
    }

    override fun onBackPressed() {
        tts2?.speak(resources.getString(R.string.GoBack), TextToSpeech.QUEUE_FLUSH, null)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(Runnable {
            tts2?.shutdown()
            finish()
        }, 1600)
    }

    inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            onAnswerListener.setAnswer(true)
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            onAnswerListener.setAnswer(false)
            return true
        }
    }

    interface OnAnswerListener {
        fun setAnswer(answer: Boolean)
    }
}