package com.hansung.capstone.post

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hansung.capstone.MyApplication
import com.hansung.capstone.R
import com.hansung.capstone.databinding.ActivityPostDetailBinding
import com.hansung.capstone.databinding.ItemPostDetailCommentsBinding
import java.time.format.DateTimeFormatter

class PostCommentsAdapter(private val resultDetailPost: ResultGetPostDetail, private val context: PostDetailActivity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var noImage=-1
        override fun getItemCount(): Int {
            return resultDetailPost.data.commentList.count()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding =
                ItemPostDetailCommentsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return PostCommentsHolder(binding)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = holder as PostCommentsHolder
            viewHolder.bind(resultDetailPost.data.commentList[position])
        }

        inner class PostCommentsHolder(private val binding: ItemPostDetailCommentsBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(items: Comments) {
                Log.d("댓글","작성됨")
                binding.CommentContent.text = items.content
                binding.CommentUserName.text = items.userNickname
                val createdDate = MyApplication.convertDate(items.createdDate).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
                binding.CommentCreatedDate.text = createdDate
                Log.d("ImageId","${items.userProfileImageId}")
                if(items.userProfileImageId!=noImage.toLong()){
                    Glide.with(context)
                        .load("${MyApplication.getUrl()}image/${items.userProfileImageId}") // 불러올 이미지 url
                        .override(200,200)
                        .centerCrop()
                        .into(binding.CommentProfileImage) // 이미지를 넣을 뷰

                }else binding.CommentProfileImage.setImageResource(R.drawable.user)
                if(items.reCommentList.isNotEmpty()) {
                    binding.PostDetailReComment.adapter = PostReCommentsAdapter(items)
                }
                else
                    binding.PostDetailReComment.visibility= View.GONE

            }
        }

}