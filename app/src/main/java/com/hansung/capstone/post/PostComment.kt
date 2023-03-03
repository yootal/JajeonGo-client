package com.hansung.capstone.post

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hansung.capstone.CommunityService
import com.hansung.capstone.MyApplication
import com.hansung.capstone.R
import com.hansung.capstone.databinding.ActivityPostDetailBinding
import com.hansung.capstone.retrofit.*
import kotlinx.android.synthetic.main.activity_post_detail.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.format.DateTimeFormatter

class PostComment(private val context: PostDetailActivity) {
    val gson: Gson = GsonBuilder()
        .setLenient()
        .create()
    private var serverInfo = MyApplication.getUrl() //username password1 password2 email
    var clientBuilder = OkHttpClient.Builder()
    var retrofit = Retrofit.Builder().baseUrl("$serverInfo")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(clientBuilder.build())
        .build()!!
    var service = retrofit.create(RetrofitService::class.java)!!
    var result:RepComment? = null
    var resultCode:Int = 0
    val success="success"
    val fail="fail"
    val api = CommunityService.create()
    var noImage=-1
    fun postComment(str: String, postId: Int,binding:ActivityPostDetailBinding) {
        Log.d("postcomment", "#####################")
        val userId = MyApplication.prefs.getInt("userId", 0)
        val postReqComment = ReqComment(postId, userId, str)
        service.postComment(postReqComment).enqueue(object : Callback<RepComment> {
            @SuppressLint("Range", "ResourceAsColor")
            override fun onResponse(call: Call<RepComment>, response: Response<RepComment>) {
                if (response.isSuccessful) {
                    val result = response.body()
                    if (response.code() == 201) { //수정해야함
                        if (result?.code == 100) {
                            Log.d("INFO comment", "댓글작성 성공" + result.toString())
                            postComments(postId.toLong(),binding)
                        } else {
                            Log.d("ERR", "댓글 작성 실패 " + result.toString())
                        }
                    }
                } else {
                    // 통신이 실패한 경우
                    Log.d("ERR comment", "onResponse 실패" + result?.toString())
                }
            }
            override fun onFailure(call: Call<RepComment>, t: Throwable) {
                // 통신 실패 (인터넷 끊킴, 예외 발생 등 시스템적인 이유)
                Log.d("ERR comment", "onFailure 에러: " + t.message.toString())
                //MyApplication.prefs.setInt("resultCode",0)
            }
        })

    }
fun postComments(postId:Long,binding:ActivityPostDetailBinding){
    api.getPostDetail(postId)
            .enqueue(object : Callback<ResultGetPostDetail> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<ResultGetPostDetail>,
                    response: Response<ResultGetPostDetail>,
                ) {
                    val body = response.body()
                    var count = 0
                    for (i in body?.data?.commentList!!) {
                        count += i.reCommentList.size
                    }
                    count += body.data.commentList.size
                    binding.CommentCount.text = count.toString()
                    Log.d("check", "############################")
                    // 이미지 등록

                        binding.PostDetailComment.adapter =
                            PostCommentsAdapter(body,context)

                }

                override fun onFailure(call: Call<ResultGetPostDetail>, t: Throwable) {
                    Log.d("getPostDetail:", "실패 : $t")
                }
            })
}

//    fun postComment(str:String,postId:Int):String{
//        Log.d("postcomment","#####################")
//        val userId=MyApplication.prefs.getInt("userId",0)
//        var postReqComment = ReqComment(postId, userId,str)
//        service.postComment(postReqComment)
//            .enqueue(object : Callback<RepComment> {
//                @SuppressLint("Range", "ResourceAsColor")
//                override fun onResponse(
//                    call: Call<RepComment>,
//                    response: Response<RepComment>,
//                ) { if (response.isSuccessful) {
//                        result= response.body()
//                        if (response.code() == 201) {//수정해야함
//                            if (result?.code == 100) {
//                                resultCode=100
//                                Log.d("INFO comment", "댓글작성 성공" + result?.toString())
//
//                            } else {
//                                Log.d("ERR", "댓글 작성 실패 " + result?.toString())
//                            }
//                        }
//                    } else {
//                        // 통신이 실패한 경우
//                        Log.d("ERR comment", "onResponse 실패"+result?.toString())
//                    }
//                }
//                override fun onFailure(call: Call<RepComment>, t: Throwable) {
//                    // 통신 실패 (인터넷 끊킴, 예외 발생 등 시스템적인 이유)
//                    Log.d("ERR comment", "onFailure 에러: " + t.message.toString())
//                }
//            })
//        Log.d("resultCode","${resultCode}")
//        Log.d("resultCode2","${result?.code}")
//
//        return if(resultCode==100)
//            "success"
//        else
//            "fail"
//    }
}