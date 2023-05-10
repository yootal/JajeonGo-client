package com.hansung.capstone

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.hansung.capstone.board.BoardAdapter
import com.hansung.capstone.board.BoardAdapterDecoration
import com.hansung.capstone.board.Posts
import com.hansung.capstone.board.ResultGetPosts
import com.hansung.capstone.databinding.FragmentBoardBinding
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class BoardFragment : Fragment() {
    lateinit var  binding: FragmentBoardBinding
    private lateinit var resultAllPost: RecyclerView
    private var page = 0
    var body:ResultGetPosts? = null
    val api = CommunityService.create()
    private val linearLayoutManager= LinearLayoutManager(activity)
    private lateinit var adapter: BoardAdapter
    var category:String="total"
    var totalPage:Int=0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBoardBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //view_=view
        adapter= BoardAdapter()
        resultAllPost = view.findViewById(R.id.resultAllPost)
        resultAllPost.addItemDecoration(BoardAdapterDecoration())
        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.BoardSwipe)
        swipe.setOnRefreshListener {
            MyApplication.prefs.removeCommentCount()
            MyApplication.prefs.removeDeletedCount()
           when(category){
               "total"->init()
               "free"->initFreeData()
                "course"->initCourseData()
           }
           // renewPage()
            swipe.isRefreshing = false
        }
        binding.totalCategory.setOnClickListener {
           DecorateButton(this@BoardFragment).decoTotalBt()
            category="total"
            page=0
            resultAllPost.scrollToPosition(0)
           init()

        }
        binding.courseCategory.setOnClickListener {
            DecorateButton(this@BoardFragment).decoCourseBt()
            category="course"
            page=0
            resultAllPost.scrollToPosition(0)
            initCourseData()

        }
        binding.freeCategory.setOnClickListener {
            DecorateButton(this@BoardFragment).decoFreeBt()
            category="free"
            page=0
            resultAllPost.scrollToPosition(0)
            initFreeData()

        }
        resultAllPost.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)&&page<body!!.totalPage) {
                        when(category){
                            "total"->getAllPost(++page)
                            "free"->getAllFreePost(++page)
                            "course"->getAllCoursePost(++page)
                        }
                }
            }
        })
        init()//첫 페이지 목록
        resultAllPost.adapter=adapter
        resultAllPost.layoutManager=linearLayoutManager
        binding.postB.setOnClickListener{
            if(MyApplication.prefs.getString("accessToken", "") != ""){
                val intent = Intent(activity, WriteActivity::class.java)
                startActivity(intent)
            }else{
                val intent = Intent(activity, LoginActivity::class.java)
                intent.putExtra("loginNeeded",true)
                startActivity(intent)
            }

        }
    }
    private fun init() {
        api.getAllPost(0)
            .enqueue(object : Callback<ResultGetPosts> {
                override fun onResponse(
                    call: Call<ResultGetPosts>,
                    response: Response<ResultGetPosts>,
                ) {
                    Log.d("getAllPost:", "성공 : ${response.body().toString()}")
                    body = response.body()
                    totalPage=body!!.totalPage
                    //if(body?.data?.isNotEmpty()!!){
                        adapter.setInitItems((body!!.data as ArrayList<Posts>))
                    //}
                }
                override fun onFailure(call: Call<ResultGetPosts>, t: Throwable) {
                    Log.d("getAllPost:", "실패 : $t")
                }
            })
    }
    @Suppress("DEPRECATION")
    private fun getAllPost(page:Int){//다음 페이지 요청
        api.getAllPost(page)
            .enqueue(object : Callback<ResultGetPosts> {
                override fun onResponse(
                    call: Call<ResultGetPosts>,
                    response: Response<ResultGetPosts>,
                ) {
                    Log.d("getAllPost:", "성공 : ${response.body().toString()}")
                    body = response.body()
                   // if(body?.data!!.isNotEmpty()){
                        adapter.run{
                            moreItems((body!!.data as ArrayList<Posts>))

                        }
                   // }
                }
                override fun onFailure(call: Call<ResultGetPosts>, t: Throwable) {
                    Log.d("getAllPost:", "실패 : $t")
                }
            })
        //  },1000)

    }
    private fun initFreeData() {
        api.getAllFreePost(0)
            .enqueue(object : Callback<ResultGetPosts> {
                override fun onResponse(
                    call: Call<ResultGetPosts>,
                    response: Response<ResultGetPosts>,
                ) {
                    Log.d("getAllPost:", "성공 : ${response.body().toString()}")
                    body = response.body()
                    adapter.setInitItems((body!!.data as ArrayList<Posts>))

                }
                override fun onFailure(call: Call<ResultGetPosts>, t: Throwable) {
                    Log.d("getAllPost:", "실패 : $t")
                }
            })
    }
    @Suppress("DEPRECATION")
    private fun getAllFreePost(page:Int){//다음 페이지 요청
        //   adapter.setLoadingView(true)
        // val handler = Handler()
        // handler.postDelayed({
        api.getAllFreePost(page)
            .enqueue(object : Callback<ResultGetPosts> {
                @SuppressLint("SuspiciousIndentation")
                override fun onResponse(
                    call: Call<ResultGetPosts>,
                    response: Response<ResultGetPosts>,
                ) {
                    Log.d("getAllPost:", "성공 : ${response.body().toString()}")
                    body = response.body()
                   // if(body?.data!!.isNotEmpty()){
                        adapter.run{
                            moreItems((body!!.data as ArrayList<Posts>))

                        }
                   // }
                }
                override fun onFailure(call: Call<ResultGetPosts>, t: Throwable) {
                    Log.d("getAllPost:", "실패 : $t")
                }
            })
        //  },1000)

    }
    private fun initCourseData() {
        api.getAllCoursePost(0)
            .enqueue(object : Callback<ResultGetPosts> {
                @SuppressLint("SuspiciousIndentation")
                override fun onResponse(
                    call: Call<ResultGetPosts>,
                    response: Response<ResultGetPosts>,
                ) {
                    Log.d("getAllPost:", "성공 : ${response.body().toString()}")
                    body = response.body()
                        adapter.setInitItems((body!!.data as ArrayList<Posts>))

                }
                override fun onFailure(call: Call<ResultGetPosts>, t: Throwable) {
                    Log.d("getAllPost:", "실패 : $t")
                }
            })
    }
    @Suppress("DEPRECATION")
    private fun getAllCoursePost(page:Int){//다음 페이지 요청
        api.getAllPost(page)
            .enqueue(object : Callback<ResultGetPosts> {
                @SuppressLint("SuspiciousIndentation")
                override fun onResponse(
                    call: Call<ResultGetPosts>,
                    response: Response<ResultGetPosts>,
                ) {
                    Log.d("getAllPost:", "성공 : ${response.body().toString()}")
                    body = response.body()
                   // if(body?.data!!.isNotEmpty()){
                        adapter.run{
                            moreItems((body!!.data as ArrayList<Posts>))

                        }
                   // }
                }
                override fun onFailure(call: Call<ResultGetPosts>, t: Throwable) {
                    Log.d("getAllPost:", "실패 : $t")
                }
            })
    }

    override fun onResume() {
        super.onResume()
        MainActivity.getInstance()?.setModifyCheck(false)
        val init = {
            when(category){
                "total" -> init()
                "free" -> initFreeData()
                "course" -> initCourseData()
            }
        }()
       when(MainActivity.getInstance()?.getStateCheck()){
           0->{ Toast.makeText(activity, "게시글이 삭제됐습니다", Toast.LENGTH_SHORT).show()
               page=0
               init
           }
           1->{ Toast.makeText(activity, "게시글이 등록됐습니다", Toast.LENGTH_SHORT).show()
               page=0
               init

           }
           2->{Toast.makeText(activity, "게시글이 수정됐습니다", Toast.LENGTH_SHORT).show()
               page=0
           }
       }
         if(MainActivity.getInstance()?.getCommentCount()!=0||MainActivity.getInstance()?.getDeletedCommentCount()!=0){
             adapter.changed(MainActivity.getInstance()!!.getChangedPost())
         }
        MainActivity.getInstance()?.stateCheck(-1)

    }

}


