package com.hansung.capstone.retrofit

import com.google.gson.annotations.SerializedName

data class ReqModifyComment(
    @SerializedName("commentId")
    val commentId: Long,

    @SerializedName("userId")
    val  userId: Long,
    @SerializedName("content")
    val  content: String
)
data class ReqModifyReComment(
    @SerializedName("reCommentId")
    val reCommentId: Long,
    @SerializedName("userId")
    val  userId: Long,
    @SerializedName("content")
    val  content: String
)
data class ReqLogin(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)
data class ReqRegister(
@SerializedName("email")
val email: String,
@SerializedName("password")
val password:String,
@SerializedName("nickname")
val nickname:String,
@SerializedName("username")
val username:String,
@SerializedName("birthday")
val birthday:String

)
data class ReqDoubleCheckID(
    @SerializedName("email")
    val email:String
)
data class ReqDoubleCheckNickName(
    @SerializedName("email")
    val email:String,
    @SerializedName("nickname")
    val nickname:String

)
data class ReqWriting(
    @SerializedName("title")
    val title: String,
    @SerializedName("content")
    val content:String
)
data class ReqModifyPW(
    @SerializedName("email")
    val email:String,
    @SerializedName("password")
    val password:String
)
data class ReqModifyNick(
    @SerializedName("email")
    val email:String,
    @SerializedName("nickname")
    val nickname:String
)

data class ReqPost(
    @SerializedName("userId")
    val userId:Int,
    @SerializedName("title")
    val title:String,
    @SerializedName("category")
    val category:String,
    @SerializedName("content")
    val content:String
    )
data class ReqComment(
    @SerializedName("postId")
    val postId:Long,
    @SerializedName("userId")
    val userId:Int,
    @SerializedName("content")
    val content:String

)
data class ReqReComment(
    @SerializedName("postId")
    val postId:Long,
    @SerializedName("commentId")
    val commentId:Int,
    @SerializedName("userId")
    val userId:Int,
    @SerializedName("content")
    val content:String
)

