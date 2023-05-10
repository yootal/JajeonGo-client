package com.hansung.capstone

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hansung.capstone.Constants.ACTION_CREATE_SERVICE
import com.hansung.capstone.Constants.ACTION_PAUSE_SERVICE
import com.hansung.capstone.Constants.ACTION_START_OR_RESUME_SERVICE
import com.hansung.capstone.Constants.ACTION_STOP_SERVICE
import com.hansung.capstone.course.CourseActivity
import com.hansung.capstone.databinding.ActivityRidingBinding
import com.hansung.capstone.retrofit.PermissionUtils
import com.hansung.capstone.retrofit.Permissions
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.LocationOverlay
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.PathOverlay
import com.naver.maps.map.util.MarkerIcons
import kotlinx.android.synthetic.main.activity_riding.view.*
import kotlinx.android.synthetic.main.layout_bottom_sheet.*
import java.io.ByteArrayOutputStream

class RidingActivity : AppCompatActivity(), OnMapReadyCallback {

    val binding by lazy { ActivityRidingBinding.inflate(layoutInflater) } // 뷰바인딩
    private lateinit var nMap: NaverMap // 네이버 지도 객체
    private lateinit var locationOverlay: LocationOverlay
    private var fusedLocationClient: FusedLocationProviderClient? = null // 사용자 위치로 카메라 이동 용도
    private var pathOverlay = mutableListOf<LatLng>() // 경로 표시용 좌표 리스트
    private var pathWaypoints = mutableListOf<Waypoint>() // 경유지 저장용 리스트

    //    private var nMapMarkers = mutableListOf<Marker>() // 생성한 마커 저장용 리스트
    private var isRiding = false // 경로중 판별 변수

    // 권한 요청 후 처리용 Launcher
    private var requestLocationPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            if (permission.all { it.value }) {
//                Log.d("checkLocationPermission","권한 승인")
                checkLocationPermission()
            } else {
                Toast.makeText(this, "경로를 기록하려면 권한을 허용해 주세요.", Toast.LENGTH_SHORT).show()
                // 권한 허용 안돼있을 시 메인으로 복귀
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
        }

    private var requestCameraPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            if (permission.all { it.value }) {
                openCamera()
            } else {
                Toast.makeText(this, "사진을 촬영하려면 권한을 허용해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // intent 정상 작동 이후
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("RidingActivity", "onCreate")
        setContentView(binding.root)

        updateUI(isRiding)

        // 사용자 좌표 기록 서비스 시작 / 끝 버튼
        binding.ridingPlayButton.setOnClickListener {
            // 처음 시작을 누를 때 isRiding = false
            if (!isRiding) {
                sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            } else {
                sendCommandToService(ACTION_PAUSE_SERVICE)
                // isRiding -> false 변경
            }
        }

        binding.ridingCameraButton.setOnClickListener {
            if (!isRiding)
                checkCameraPermission()
        }

        binding.ridingStopButton.setOnClickListener {
            if (isRiding) {
                sendCommandToService(ACTION_STOP_SERVICE)
                nMap.takeSnapshot {
                    moveToCourseActivity(it)
                }
            }
        }

        binding.ridingCheckButton.setOnClickListener {
            addMarkerDialog(this)
        }
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("RidingActivity", "onReStart")
    }

    override fun onStart() {
        super.onStart()
        Log.d("RidingActivity", "onStart")
        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.riding_view) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.riding_view, it).commit()
            }
        mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        Log.d("RidingActivity", "onResume")
    }

    override fun onPause() {
        Log.d("RidingActivity", "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d("RidingActivity", "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d("RidingActivity", "onDestroy")
        // 액티비티가 종료 될 때 서비스 종료 액션 전달
        sendCommandToService(ACTION_STOP_SERVICE)
        super.onDestroy()
    }

    @UiThread
    private fun updateUI(isRiding: Boolean) { // 라이딩 상태에 따른 UI 변경
        this.isRiding = isRiding
        if (!isRiding) { // 주행중이 아닐 때
            binding.ridingPlayButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_play_arrow_24
                )
            )
            binding.ridingCameraButton.alpha = 1f
            binding.ridingCameraButton.isClickable = true
            binding.ridingStopButton.alpha = 1f
            binding.ridingStopButton.isClickable = true
            binding.ridingCheckButton.alpha = 1f
            binding.ridingCheckButton.isClickable = true

        } else {
            binding.ridingPlayButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.ic_baseline_pause_24
                )
            )
            binding.ridingCameraButton.alpha = 0.3f
            binding.ridingCameraButton.isClickable = false
            binding.ridingStopButton.alpha = 0.3f
            binding.ridingStopButton.isClickable = false
            binding.ridingCheckButton.alpha = 0.3f
            binding.ridingCheckButton.isClickable = false
        }
    }

    private fun moveToCourseActivity(bitmap: Bitmap) { // 코스 등록 화면으로 이동
        // Bitmap 객체를 Parcelable 형태로 변환
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
//        val byteArray = stream.toByteArray()

        val intent = Intent(this, CourseActivity::class.java)
        RidingService.timeLiveData.observe(this) { time ->
            intent.putExtra("ridingTime", time) // 시간
        }
//        intent.putExtra("bitmap", byteArray) // 스냅샷
//        intent.putExtra("courseName", "Android Development") // 거리
//        intent.putExtra("courseName", "Android Development") // 좌표
        startActivity(intent)
    }

    // 서비스에 명령 보내는 함수
    private fun sendCommandToService(action: String) {
        Intent(this, RidingService::class.java).also { Intent ->
            Intent.action = action
            this.startService(Intent)
        }
    }

    // LiveData 값 변화 조회
    @SuppressLint("SetTextI18n")
    private fun subscribeToObservers() {
        // 주행 중인지 판별
        RidingService.isRiding.observe(this) {
            this.isRiding = it
            updateUI(it)
        }

        RidingService.pathOverlay.observe(this) {
            pathOverlay = it
//            RidingService.distanceLiveData.postValue(RidingUtility.calculateDistance(it))
            drawPathOverlay()
            moveCameraToLastLocation()
        }

        // LiveData 라이딩 시간을 observe 해서 실시간으로 값을 바꿔서 출력한다
        RidingService.timeLiveData.observe(this) {
            binding.printTimer.text = RidingUtility.convertMs(it)

            // ############## 추가 ##############
            // 거리 수정
            RidingService.distanceLiveData.postValue(
                RidingService.pathOverlay.value?.let { it1 ->
                    RidingUtility.calculateDistance(
                        it1
                    )
                } ?: 0f
            )

            // 속도 수정
            RidingService.speedLiveData.postValue(RidingService.pathOverlay.value?.let { it1 ->
                RidingUtility.calculateDistance(
                    it1
                )
            }
                ?.let { it2 ->
                    RidingUtility.calculateSpeed(
                        it,
                        it2
                    )
                } ?: 0f
            )

            // 칼로리 수정
            RidingService.kcalLiveData.postValue(RidingUtility.calculateKcal(it).toInt())
        }

        // LiveData 거리를 observe
        RidingService.distanceLiveData.observe(this) {
            binding.printDistance.text = "%.2f".format(it)
        }

        // LiveData 속도를 observe
        RidingService.speedLiveData.observe(this) {
            binding.printSpeed.text = "%.2f".format(it)
        }

        // LiveData 칼로리를 observe
        RidingService.kcalLiveData.observe(this) {
            binding.printKcal.text = it.toString()
        }

        // LiveData Location 좌표를 이용해 오버레이 표시
        RidingService.currentLocation.observe(this) {
            locationOverlay.position = it
            locationOverlay.isVisible = true
        }
    }

    private fun moveCameraToLastLocation() {
        if (pathOverlay.isNotEmpty()) {
            val cameraUpdate = CameraUpdate.scrollAndZoomTo(pathOverlay.last(), 17.0)
                .animate(CameraAnimation.Easing)
            nMap.moveCamera(cameraUpdate)
        }
    }

    private fun drawPathOverlay() {
        if (pathOverlay.isNotEmpty() && pathOverlay.size > 1) {
            val path = PathOverlay()
            path.coords =
                listOf(pathOverlay.takeLast(2).firstOrNull(), pathOverlay.takeLast(1).firstOrNull())
            path.outlineWidth = 0
            path.color = Color.BLUE
            path.map = nMap
        }
    }

    private fun drawAllPathOverlay() {
        if (pathOverlay.isNotEmpty() && pathOverlay.size > 1) {
            val path = PathOverlay()
            path.coords = pathOverlay
            path.outlineWidth = 0
            path.color = Color.BLUE
            path.map = nMap
        }
    }

    @UiThread
    override fun onMapReady(naverMap: NaverMap) {
        this.nMap = naverMap // 메인의 객체와 연결
        naverMap.lightness = 0f // 밝기 조절

        // 지도 UI 설정
        val uiSettings = naverMap.uiSettings
        uiSettings.isScaleBarEnabled = true
        uiSettings.isCompassEnabled = false
        uiSettings.isZoomControlEnabled = false

        naverMap.mapType = NaverMap.MapType.Basic // 맵 타입 Basic
        locationOverlay = nMap.locationOverlay // 초기화
        checkLocationPermission() // 위치 권한 검사

        // 지도에 표시할 정보
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, false)
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, false)
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_MOUNTAIN, false)
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false)

        drawAllPathOverlay()
        subscribeToObservers()
    }

    @SuppressLint("MissingPermission")
    fun checkLocationPermission() {
        val permissionCheckResult =
            PermissionUtils.checkPermissions(this, Permissions.permissionsLocation)
        if (permissionCheckResult.isEmpty()) { // 권한이 모두 승인된 상태면
            sendCommandToService(ACTION_CREATE_SERVICE)
            if (isRiding) {
                val cameraUpdate =
                    CameraUpdate.scrollAndZoomTo(
                        LatLng(
                            RidingService.currentLocation.value!!.latitude,
                            RidingService.currentLocation.value!!.longitude
                        ).apply {
                            locationOverlay.position = this
                            locationOverlay.isVisible = true
                        }, 17.0
                    )
                        .animate(CameraAnimation.Fly)
                nMap.moveCamera(cameraUpdate)
            } else {
                fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(MyApplication.applicationContext()) // fusedLocationClient 초기화
                fusedLocationClient!!.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            // 카메라 이동
                            val cameraUpdate =
                                CameraUpdate.scrollAndZoomTo(
                                    LatLng(
                                        location.latitude,
                                        location.longitude
                                    ).apply {
                                        locationOverlay.position = this
                                        locationOverlay.isVisible = true
                                    }, 13.0
                                )
                                    .animate(CameraAnimation.Fly)
                            nMap.moveCamera(cameraUpdate)
                        }
                    }
                fusedLocationClient = null
            }
        } else {
            Toast.makeText(
                MyApplication.applicationContext(),
                "지도 사용을 위해 권한을 허용해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            requestLocationPermissionLauncher.launch(permissionCheckResult.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    fun checkCameraPermission() {
        val permissionCheckResult =
            PermissionUtils.checkPermissions(this, Permissions.permissionsCamera)
        if (permissionCheckResult.isEmpty()) {
            openCamera()
        } else {
            Toast.makeText(
                MyApplication.applicationContext(),
                "카메라 사용을 위해 권한을 허용해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            requestCameraPermissionLauncher.launch(permissionCheckResult.toTypedArray())
        }
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        val imageUri: Uri? =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraCaptureLauncher.launch(intent)
    }

    private fun addMarkerDialog(context: Context) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setMessage("현 위치에서 마커를 추가하시겠습니까?")
        alertDialogBuilder.setPositiveButton("등록") { dialogInterface: DialogInterface, _: Int ->
            addMarker()
            dialogInterface.dismiss() // 다이얼로그 닫기
        }
        alertDialogBuilder.setNegativeButton("취소") { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss() // 다이얼로그 닫기
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun addMarker() {
        val lastLocation = RidingService.currentLocation.value
        if (lastLocation != null) {
            pathWaypoints.add(
                Waypoint(
                    place_lat = lastLocation.latitude,
                    place_lng = lastLocation.longitude
                )
            )
            val marker = Marker()
            marker.position = lastLocation
            marker.icon = MarkerIcons.BLUE
            marker.map = nMap
//            nMapMarkers.add(marker)
            RidingService.pathWaypoints.postValue(pathWaypoints)
        }
    }
}
