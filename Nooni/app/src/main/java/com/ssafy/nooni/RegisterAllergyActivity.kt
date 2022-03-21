package com.ssafy.nooni

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR
import android.util.Log
import android.widget.Toast
import com.ssafy.nooni.databinding.ActivityRegisterAllergyBinding
import com.ssafy.nooni.util.SharedPrefArrayListUtil
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "RegisterAllergy"
class RegisterAllergyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterAllergyBinding
    private lateinit var tts2: TextToSpeech
    var sharePrefArrayListUtil = SharedPrefArrayListUtil()
    val list = listOf<String>("갑각류", "견과", "달걀", "땅콩", "밀", "생선", "우유", "조개", "콩")
    val allergyList = ArrayList<String>()
    var cnt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterAllergyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tts2 = TextToSpeech(this, TextToSpeech.OnInitListener {
            @Override
            fun onInit(status: Int){
                if(status != ERROR){
                    tts2.language = Locale.KOREA
                }
            }
        })

        init()
    }

    private fun ttsSpeak(text: String){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            tts2.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts2.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    
    private fun init() {
        // TODO: 맨 처음 멘트 안나오는거 수정해야함
        ttsSpeak("나는 갑각류 알레르기가 있다.")
        binding.tvAllergyAType.text = list[cnt]

        binding.btnAllergyANo.setOnClickListener {
            allergyNext()
        }
        binding.btnAllergyAYes.setOnClickListener {
            allergyList.add(list[cnt])
            allergyNext()
        }
    }

    private fun allergyNext(){
        if(++cnt >= list.size) save()
        else {
            binding.tvAllergyAType.text = list[cnt]
            ttsSpeak("나는 " + list[cnt] + " 알레르기가 있다.")
        }
    }

    private fun save(){
        sharePrefArrayListUtil.setStringArrayPref(this, "allergies", allergyList)
        Toast.makeText(this, "알레르기 정보 등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
        tts2.speak("알레르기 정보 등록이 완료되었습니다.", TextToSpeech.QUEUE_FLUSH, null)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        if(tts2 != null){
            tts2.stop()
            tts2.shutdown()
        }
    }
}