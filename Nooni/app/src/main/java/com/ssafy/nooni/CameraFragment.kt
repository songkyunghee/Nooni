package com.ssafy.nooni

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.ssafy.nooni.adapter.AllergyRVAdapter
import com.ssafy.nooni.databinding.FragmentCameraBinding
import com.ssafy.nooni.util.ImageDetectUtil
import com.ssafy.nooni.util.PlayMediaUtil
import com.ssafy.nooni.util.ShakeUtil
import kotlinx.coroutines.*
import org.json.JSONArray
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.timer


private const val TAG = "CameraFragment"

class CameraFragment : Fragment() {
    lateinit var binding: FragmentCameraBinding
    lateinit var allergyRVAdapter: AllergyRVAdapter
    private lateinit var mainActivity: MainActivity
    private lateinit var behavior: BottomSheetBehavior<LinearLayout>

    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mShakeUtil: ShakeUtil

    private val mediaUtil = PlayMediaUtil()
    private lateinit var imageDetectUtil: ImageDetectUtil
    private var imageCapture: ImageCapture? = null

    private var dataId = -1

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        initSensor()
        initImageDetect()

        // 아래와 같이 url에서 음성파일 실행할 수 있음
        // TODO: 음성파일 이름 규칙을 만들어야 url 접근이 용이
        // 현재는 https://storage.googleapis.com/nooni-a587a.appspot.com/results/vocgan_{ }.wav 인데
        // 괄호안의 형태를 이미지 클래스 분류 output에 맞춰야 할 것같음

//        val url = URLEncoder.encode(
//            "https://storage.googleapis.com/nooni-a587a.appspot.com/results/vocgan_콘초 입니다 .wav",
//            "UTF-8"
//        )
//        mediaUtil.start(url)

    }


    private fun init() {

        var gestureListener = MyGesture()
        var doubleTapListener = MyDoubleGesture()
        var gestureDetector = GestureDetector(requireContext(), gestureListener)
        gestureDetector.setOnDoubleTapListener(doubleTapListener)
        binding.constraintLayoutCameraF.setOnTouchListener { v, event ->
            return@setOnTouchListener gestureDetector.onTouchEvent(event)
        }

        // 왜인지는 모르겠으나 onTouchListener만 달아놓으면 더블클릭 인식이 안되고 clickListener도 같이 달아놔야만 더블클릭 인식됨; 뭐징
        binding.constraintLayoutCameraF.setOnClickListener {}

        behavior = BottomSheetBehavior.from(binding.llCameraFBottomSheet)
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if(newState == BottomSheetBehavior.STATE_EXPANDED){
                    describeTTS()
                } else if(newState == BottomSheetBehavior.STATE_COLLAPSED){
                    mainActivity.tts.stop()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}

        })

        setBottomSheetRecyclerView()
    }


    private fun initSensor(){
        mSensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mShakeUtil = ShakeUtil()
        mShakeUtil.setOnShakeListener(object : ShakeUtil.OnShakeListener {
            override fun onShake(count: Int) {
                describeTTS()
            }
        })
    }

    private fun initImageDetect() {
        imageDetectUtil = ImageDetectUtil(requireContext())
    }

//    override fun onStart() {
//        super.onStart()
//        Toast.makeText(requireActivity(), "camera onStart called", Toast.LENGTH_SHORT).show()
//        mainActivity.tts.speak("상품 인식 화면입니다." + binding.tvCameraFDescription.text.toString(), TextToSpeech.QUEUE_FLUSH, null)
//    }

    override fun onResume() {
        super.onResume()
        startCamera()
        mainActivity.findViewById<TextView>(R.id.tv_title).text = "상품 인식"
//        Toast.makeText(requireActivity(), "camera onResume called", Toast.LENGTH_SHORT).show()
        mainActivity.ttsSpeak("상품 인식 화면입니다." + binding.tvCameraFDescription.text.toString())

        mSensorManager.registerListener(
            mShakeUtil,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewViewCameraF.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().setJpegQuality(75).setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY).build()

            //후면 카메라 기본으로 세팅
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 카메라와 라이프사이클 바인딩 전 모든 바인딩 해제
                cameraProvider.unbindAll()

                // 카메라와 라이프사이클 바인딩
                cameraProvider.bindToLifecycle(
                    requireActivity(),
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.d(TAG, "Use case binding failed: ", e)
            }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun takePicture() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                @SuppressLint("UnsafeExperimentalUsageError")
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer: ByteBuffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.capacity())
                    buffer.get(bytes)
                    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

                    image.close()

                    // 90도 돌리기
                    var rotateMatrix = Matrix()
                    rotateMatrix.postRotate(90.0f)

                    var cropImage = Bitmap.createScaledBitmap(bitmap, imageDetectUtil.IMAGE_SIZE, imageDetectUtil.IMAGE_SIZE, false)
                    cropImage = Bitmap.createBitmap(cropImage, 0, 0, cropImage.width, cropImage.height, rotateMatrix, false)

                    imageDetectUtil.classifyImage(cropImage)
                    super.onCaptureSuccess(image)
                }
            }
        )
    }

    private fun setBottomSheetRecyclerView() {
        allergyRVAdapter = AllergyRVAdapter()
        binding.rvCameraFBsAllergy.apply{
            adapter = allergyRVAdapter
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        }
        allergyRVAdapter.setData(listOf("밀", "우유", "콩"))
    }

    private fun setProductData() {
        // JSON 파일 열어서 String으로 취득
        val assetManager = resources.assets
        val inputStream = assetManager.open("data.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        // JSONArray로 파싱
        val jsonArray = JSONArray(jsonString)

        var bcode = ""
        for (index in 0 until jsonArray.length()){
            val jsonObject = jsonArray.getJSONObject(index)
            val id = jsonObject.getString("id")
            if(id == dataId.toString()) {
                bcode = jsonObject.getString("bcode")
                Log.d(TAG, "setBottomSheetData: bcode = $bcode")
            }
        }

        // 바코드 정보를 가지고 크롤링한 후 가져온 HTML을 파싱하여 가격정보 추출하고 표시
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://www.cvslove.com/product/product_view.asp?pcode=$bcode"
            val doc = Jsoup.connect(url).timeout(1000*10).get()
            val contentData: Elements = doc.select("#Table4")
            var price = ""
            Log.d(TAG, "setBottomSheetData: $contentData")

            for(data in contentData) {
                val element = data.select("td")
                for(j in 0 until element.size) {
                    val label = element[j].text()
                    if(label == "소비자가격")
                        price = element[j+1].text()
                }
                Log.d(TAG, "setBottomSheetData: price = $price")
                binding.tvCameraFBsPrice.text = "${price}"
            }

        }


    }

    inner class MyGesture : GestureDetector.OnGestureListener {
        override fun onDown(p0: MotionEvent?): Boolean {
            return false
        }

        override fun onShowPress(p0: MotionEvent?) {}

        override fun onSingleTapUp(p0: MotionEvent?): Boolean {
            return false
        }

        override fun onScroll(
            p0: MotionEvent?,
            p1: MotionEvent?,
            p2: Float,
            p3: Float
        ): Boolean {
            return false
        }

        override fun onLongPress(p0: MotionEvent?) {}

        override fun onFling(
            p0: MotionEvent?,
            p1: MotionEvent?,
            p2: Float,
            p3: Float
        ): Boolean {
            val SWIPE_THRESHOLD = 100
            val SWIPE_VELOCITY_THRESHOLD = 10

            var result = false
            try {
                val diffY = p1!!.y - p0!!.y
                val diffX = p1!!.x - p0!!.x
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(p3) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                    result = true
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            return result
        }


        private fun onSwipeBottom() {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        private fun onSwipeTop() {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

    }

    inner class MyDoubleGesture : GestureDetector.OnDoubleTapListener {
        override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
            return false
        }

        override fun onDoubleTap(p0: MotionEvent?): Boolean {
            imageDetectUtil.init()
            // TODO: tts로 안내하기
            Toast.makeText(context, "인식 중입니다.", Toast.LENGTH_SHORT).show()

            for(i in 1..3) {
                takePicture()
            }

            var time = imageDetectUtil.GIVEN_TIME
            timer(period = 1000) {
                time -= 1

                if(time < 0) {
                    requireActivity().runOnUiThread {
                        imageDetectUtil.evaluateImage()
                    }
                    this.cancel()
                }
            }
            return true
        }

        override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {
            return false
        }
    }


    private fun describeTTS() {
        // TODO : 추후 상품 인식 기능 넣어서 상품 정보 가져올 경우, 가져온 정보에 따라 출력할 문자열 가공 필요
        var string =
            "${binding.tvCameraFBsName.text.toString()}, 가격 23000원, 알레르기 유발성분 밀, 우유, 콩,  320 칼로리"
        mainActivity.ttsSpeak(string)
    }

    override fun onPause() {
        mSensorManager.unregisterListener(mShakeUtil)
        super.onPause()
    }

}

