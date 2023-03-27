package com.hansung.capstone

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.hansung.capstone.databinding.ActivityWriteBinding
import com.hansung.capstone.retrofit.RepPost
import com.hansung.capstone.retrofit.ReqPost
import com.hansung.capstone.retrofit.RetrofitService
import kotlinx.android.synthetic.main.activity_post_detail.*
import kotlinx.android.synthetic.main.activity_write.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream

@Suppress("DEPRECATION")
class WriteActivity : AppCompatActivity() {
    private lateinit var rvImage: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var tvImageCount: TextView
    var countImage=0

    var postId:Long=0
    private val imageList: ArrayList<MultipartBody.Part> = ArrayList()
    var filePart: MultipartBody.Part? = null
    private var photouri: Uri? =null
    private val DEFAULT_GALLERY_REQUEST_CODE = 0
    private var serverinfo = MyApplication.getUrl() //username password1 password2 email
    private var retrofit = Retrofit.Builder().baseUrl("$serverinfo")
        .addConverterFactory(GsonConverterFactory.create()).build()
    private var service = retrofit.create(RetrofitService::class.java)

    private val binding by lazy { ActivityWriteBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""
        rvImage = findViewById(R.id.rv_image)

        binding.writebutton.isEnabled=false
        imageAdapter = ImageAdapter(this, binding )
        if(MainActivity.getInstance()?.getmodifyCheck()!!){
            modifyActivity()
            MainActivity.getInstance()?.setModifyCheck(false)
        }
        initAddImage()
        if(MainActivity.getInstance()?.getmodifyCheck()==false){
            binding.editTitle.addTextChangedListener( object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    binding.writebutton.isEnabled=false
                }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(p0: Editable?) {
                    binding.writebutton.isEnabled =binding.editTitle.text.toString() != ""
                }
            })
            binding.editWriting.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    binding.writebutton.isEnabled=false
                }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(p0: Editable?) {
                    binding.writebutton.isEnabled =
                        binding.editWriting.text.toString() != ""
                }
            })
        }
        binding.writebutton.setOnClickListener {
            val title = binding.editTitle.text.toString()
            val content = binding.editWriting.text.toString()
            val userId=MyApplication.prefs.getInt("userId",0)
            val postReqPost = ReqPost(userId, title,"FREE", content)
            Log.d("filesImage","${imageList}")
            service.postCreate(postReqPost, imageList).enqueue(object : Callback<RepPost> {
                //  @SuppressLint("Range")
                override fun onResponse(call: Call<RepPost>, response: Response<RepPost>) {
                    if (response.isSuccessful) {
                        Log.d("req", "OK")
                        val result: RepPost? = response.body()
                        if (response.code() == 201) {//수정해야함
                            if (result?.code == 100) {
                                Log.d("게시글작성", "성공: $title")
                                MainActivity.getInstance()?.writeCheck(true)
                                finish()
                            } else {
                                Log.d("ERR", "실패: " + result?.toString())
                            }
                        }
                    } else {
                        Log.d("ERR", "onResponse 실패")
                    }
                }
                override fun onFailure(call: Call<RepPost>, t: Throwable) {
                    Log.d("onFailure", "실패 ")
                }
            })
        }
    }

    private fun modifyActivity() {
        binding.writebutton.isEnabled=true
        Log.d("modify","e")
        binding.editTitle.setText(MainActivity.getInstance()?.modify_title)
        binding.editWriting.setText(MainActivity.getInstance()?.modify_content)
        countImage= MainActivity.getInstance()?.modify_imageList!!.size
        if(countImage>0)
            imageAdapter.setItem(MainActivity.getInstance()?.modify_imageList as List<Int>)


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //val imageView=findViewById<ImageView>(R.id.imageView)
        if (resultCode == Activity.RESULT_OK && requestCode == DEFAULT_GALLERY_REQUEST_CODE) {
            //photouri = data?.data
            val photoUri: Uri = data?.data!!
            ++countImage
            //setImage(photoUri!!)
//            var bitmap: Bitmap? = null
//            try {
//                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
//                //bitmap = rotateImage(bitmap, 90)
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
            //val a="${MyApplication.getUrl()}images/1"//test
            // imageView.setImageBitmap(bitmap)
            //val photo: Uri =a.toUri()//test
            imageAdapter.addItem(photoUri)
            Log.d("data.path","${data?.data?.path}")
            Log.d("data","$photouri")
            val filename=getFileName(photoUri)
            Log.d("filename","$filename")
            // 선택한 이미지를 imageList에 추가하는 코드
            val inputStream = contentResolver.openInputStream(photoUri)
            //val inputStream2 = contentResolver.openInputStream(photo)//test
            val file = File(cacheDir, photoUri.lastPathSegment)
            // val file2 = File(cacheDir, photo.lastPathSegment)//test
            val outputStream = FileOutputStream(file)
            // val outputStream2= FileOutputStream(file2)//test
            inputStream?.copyTo(outputStream)
            //inputStream2?.copyTo(outputStream)//test
            val requestBody = RequestBody.create(MediaType.parse(contentResolver.getType(photoUri)), file)
            // val requestBody2 = RequestBody.create(MediaType.parse(contentResolver.getType(photo)), file2)
            filePart = MultipartBody.Part.createFormData("imageList", filename, requestBody)
            imageList.add(filePart!!)

        }
    }
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val focusView = currentFocus
        if (focusView != null && ev != null) {
            val rect = Rect()
            focusView.getGlobalVisibleRect(rect)
            val x = ev.x.toInt()
            val y = ev.y.toInt()

            if (!rect.contains(x, y)) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(focusView.windowToken, 0)
                focusView.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun initAddImage() {
        tvImageCount = findViewById(R.id.tv_image_count)
        val addImageView = findViewById<ConstraintLayout>(R.id.cl_add_image)
        addImageView.setOnClickListener {
            addImage()
        }


        rvImage.adapter = imageAdapter
    }
    fun removeImage(inx:Int) {
        imageList.removeAt(inx)
        --countImage
    }
    @SuppressLint("SuspiciousIndentation")
    private fun addImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if(countImage<6){
            Log.d("countImage","$countImage")
            startActivityForResult(intent, DEFAULT_GALLERY_REQUEST_CODE)}
        else alertDialog()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    0)
            }
        }
    }
    @SuppressLint("Range")
    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                if (cursor != null) {
                    cursor.close()
                }
            }
        }
        if (result == null) {
            result = uri.lastPathSegment
        }
        return result
    }
    private fun alertDialog() {
        val builder= AlertDialog.Builder(this)
        builder.setTitle("알림")
            .setMessage("이미지는 최대 6까지 선택할 수 있습니다.")
            .setNegativeButton("닫기",null)
        builder.show()
    }
}