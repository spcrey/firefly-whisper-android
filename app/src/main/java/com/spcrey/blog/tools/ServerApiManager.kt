package com.spcrey.blog.tools

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.google.gson.Gson
import com.google.gson.annotations.JsonAdapter
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object ServerApiManager {

    private val gson = Gson()

    private val interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val originalResponse = chain.proceed(originalRequest)
        val code = originalResponse.code()
        return@Interceptor originalResponse
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .callTimeout(20, TimeUnit.SECONDS).build()

    private val retrofit =
        Retrofit.Builder().baseUrl("http://120.26.13.9:9000").client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(CoroutineCallAdapterFactory()).build()

    interface ApiService {
        @Headers("content-type: application/json")
        @POST("/user/sendSms")
        fun userSendSms(@Body form: UserSendSmsForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/register")
        fun userRegister(@Body form: UserRegisterForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/loginByCode")
        fun userLoginByCode(@Body form: UserLoginByCodeForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/loginByPassword")
        fun userLoginByPassword(@Body form: UserLoginByPasswordForm): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @GET("/user/info")
        fun userInfo(@Header("Authorization") token: String): Deferred<CommonData<User>>

        @Headers("content-type: application/json")
        @GET("/user/infoOther")
        fun userInfoOther(
            @Header("Authorization") token: String?, @Query("userId") userId: Int
        ): Deferred<CommonData<User>>

        @Headers("content-type: application/json")
        @POST("/user/update")
        fun userUpdate(
            @Header("Authorization") token: String, @Body form: UserUpdateForm
        ): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/updateAvatar")
        fun userUpdateAvatar(
            @Header("Authorization") token: String, @Body form: UserUpdateAvatarForm
        ): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/follow")
        fun userFollow(
            @Header("Authorization") token: String, @Body form: UserFollowForm
        ): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/unfollow")
        fun userUnfollow(
            @Header("Authorization") token: String, @Body form: UserFollowForm
        ): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/user/logout")
        fun userLogout(@Header("Authorization") token: String): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @GET("/article/list")
        fun articleList(
            @Header("Authorization") token: String?,
            @Query("pageNum") pageNum: Int,
            @Query("pageSize") pageSize: Int
        ): Deferred<CommonData<ArticleList>>

        @Headers("content-type: application/json")
        @POST("/article/add")
        fun articleAdd(
            @Header("Authorization") token: String, @Body form: ArticleAddForm
        ): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/article/like")
        fun articleLike(
            @Header("Authorization") token: String, @Body form: ArticleLikeForm
        ): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/article/unlike")
        fun articleUnlike(
            @Header("Authorization") token: String, @Body form: ArticleLikeForm
        ): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @GET("/article/listComments")
        fun articleListComments(@Query("articleId") articleId: Int): Deferred<CommonData<List<ArticleComment>>>

        @Headers("content-type: application/json")
        @POST("/article/comment")
        fun articleComment(
            @Header("Authorization") token: String, @Body form: ArticleCommentForm
        ): Deferred<CommonData<String>>

        @Headers("content-type: application/json")
        @POST("/message/sendText")
        fun messageSendText(
            @Header("Authorization") token: String, @Body form: MessageSendTextForm
        ): Deferred<CommonData<MultiUserMessageList>>

        @Headers("content-type: application/json")
        @POST("/message/sendImage")
        fun messageSendImage(
            @Header("Authorization") token: String, @Body form: MessageSendImageForm
        ): Deferred<CommonData<MultiUserMessageList>>

        @Headers("content-type: application/json")
        @GET("/message/list")
        fun messageList(
            @Header("Authorization") token: String, @Query("lastId") lastId: Int
        ): Deferred<CommonData<MultiUserMessageList>>

        @Headers("content-type: application/json")
        @GET("/user/listFolloweds")
        fun userListFolloweds(
            @Header("Authorization") token: String
        ): Deferred<CommonData<List<User>>>
    }

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    data class CommonData<T>(val code: Int, val message: String, val data: T)

    data class ArticleAddForm(val content: String, val imageUrls: MutableList<String>)

    data class UserSendSmsForm(val phoneNumber: String)

    data class MessageSendTextForm(
        val textContent: String, val receivingUserId: Int, val lastId: Int?
    )

    data class MessageSendImageForm(
        val imageUrl: String, val receivingUserId: Int, val lastId: Int?
    )

    data class UserLoginByCodeForm(val phoneNumber: String, val code: String)

    data class UserUpdateAvatarForm(val avatarUrl: String)

    data class UserLoginByPasswordForm(val phoneNumber: String, val password: String)

    data class UserRegisterForm(
        val phoneNumber: String, val password: String, val rePassword: String
    )

    data class UserUpdateForm(
        var nickname: String, var email: String? = null, var personalSignature: String? = null
    )

    data class ArticleLikeForm(val id: Int)

    data class ArticleCommentForm(val content: String, val articleId: Int)

    data class UserFollowForm(val followedUserId: Int)

    data class Article(
        val id: Int,
        val content: String,
        val userId: Int,
        val createTime: String,
        val imageUrls: List<String>,
        val userNickname: String,
        val userAvatarUrl: String,
        var likeCount: Int,
        var likeStatus: Boolean?,
        val commentCount: Int
    )

    data class ArticleList(val total: Int, val items: List<Article>)

    data class User(
        val id: Int,
        val phoneNumber: String?,
        var nickname: String,
        var email: String?,
        var personalSignature: String?,
        val avatarUrl: String,
        var isFollowed: Boolean?,
        val isFollower: Boolean?,
        val createTime: String,
        val updateTime: String
    )

    data class ArticleComment(
        val id: Int,
        val content: String,
        val userNickname: String,
        val userAvatarUrl: String,
        val createTime: String
    )

    data class Message(
        val id: Int,
        val textContent: String?,
        val imageUrl: String?,
        val sendingUserId: Int,
        val receivingUserId: Int,
        val createTime: String,
        var isSendingUser: Boolean
    )

    data class UserMessageList(
        val withUserId: Int,
        val userAvatarUrl: String,
        val userNickname: String,
        var lastMessageId: Int,
        val messages: MutableList<Message>,
        var newMessageCount: Int?
    )

    data class MultiUserMessageList(
        var userMessageLists: MutableList<UserMessageList>, var lastMessageId: Int? = null
    )
}