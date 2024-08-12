package com.spcrey.blog.tools

object CachedData {
    var token: String? = null
    var user: ServerApiManager.User? = null
    val articles: MutableList<ServerApiManager.Article> = mutableListOf()
    var currentArticlePageNum = 1
    val multiUserMessageList = ServerApiManager.MultiUserMessageList(mutableListOf(), 0)
    var currentUserId = 0

    @JvmStatic
    fun main(args: Array<String>) {
    }
}