package com.chobo.gpsmap

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.chobo.gpsmap.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.PolylineOptions
import java.util.jar.Manifest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    //PolyLine option
    private val polylineOptions = PolylineOptions().width(5f).color(Color.RED)


    //object for location info
    private val fusedLocationProviderClient by lazy {
        FusedLocationProviderClient(this)
    }
    //location request info
    private val locationRequest by lazy {
        LocationRequest.create().apply {
            //GPS first
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            //update interval
            //위치정보가 없을때는 업데이트 안 함
            //상황에 따라 짧아질수 있음, 정확하지 않음
            //다른 앱에서 인터벌로 위치 정보를 요청하면 짧아질수 있음
            interval=10000

            //정확함. 이것보다 짧은 업데이트는 하지않음
            fastestInterval=5000
        }
    }

    //위치 정보를 얻으면 해야할 행동이 정의된 콜백 객체
    private val locationCallback = MyLocationCallBack()
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 권한 획득 성공 시
            addLocationListener()
        } else {
            // 권한 획득 거부 시
            Toast.makeText(this,"권한이 거부되었습니다.",Toast.LENGTH_SHORT).show()
        }
    }
    private fun showPermissionInfoDialog(){
        //다이얼로그에 권한이 필요한 이유를 설명
        AlertDialog.Builder(this).apply {
            setTitle("권한이 필요한 이유")
            setMessage("지도에 위치를 표시하려면 위치 정보 권한이 필요합니다.")
            setPositiveButton("권한 요청"){_,_,->
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_LOCATION)
            }
            setNegativeButton("거부",null)
        }.show()
    }

    @SuppressLint("MissingPermission")
    private fun addLocationListener(){
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }
    private fun checkPermission(cancel:()->Unit, ok: ()->Unit){
        //위치권한이 있는지 검사
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )!=PackageManager.PERMISSION_GRANTED
        ) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ){
                cancel()
            }else{
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            return
        }
        ok()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val map_Fragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        map_Fragment.getMapAsync(this)

        //화면꺼지지않게 하기
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //세로모드로 화면고정
        requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(binding.root)

        //SupportMapFragment를 가져와서 지도가 준비되면 알림을 받습니다
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        checkPermission(
            cancel={
                showPermissionInfoDialog()
            },
            ok={
                addLocationListener()
            }
        )
    }

    override fun onPause() {
        super.onPause()
        removeLocationListener()
    }
    private fun removeLocationListener(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }



    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }


    inner class MyLocationCallBack:LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            val location =  locationResult?.lastLocation

            location?.run{

                //14 level로 확대하며 현재 위치로 카메라 이동
                val latLng=LatLng(latitude,longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,17f))

                Log.d("MapsActivity","위도: $latitude, 경도: $longitude")
                //PolyLine에 좌표 추가
                polylineOptions.add(latLng)
                //선그리기
                mMap.addPolyline(polylineOptions)
            }
        }
    }
}