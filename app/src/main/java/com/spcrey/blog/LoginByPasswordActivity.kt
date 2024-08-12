package com.spcrey.blog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.MessageReceiving
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class LoginByPasswordActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LoginByPasswordActivity"
        private const val SHARED_PREFERENCE_NAME = "user"
    }

    private val editTextPhoneNumber by lazy {
        findViewById<EditText>(R.id.editText_phone_number)
    }
    private val editTextPassword by lazy {
        findViewById<EditText>(R.id.editText_password)
    }
    private val btnLogin by lazy {
        findViewById<View>(R.id.btn_to_login)
    }
    private val textLogin by lazy {
        findViewById<TextView>(R.id.text_to_login)
    }
    private val btnToRegister by lazy {
        findViewById<View>(R.id.btn_to_register)
    }
    private val btnToLoginByCode by lazy {
        findViewById<View>(R.id.btn_to_login_by_code)
    }
    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)
    }
    private val icBack by lazy {
        findViewById<ImageView>(R.id.ic_back)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_by_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        icBack.setOnClickListener {
            finish()
        }

        btnToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        btnToLoginByCode.setOnClickListener {
            val intent = Intent(this, LoginByCodeActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            val commonData = validateEditText()
            commonData.data?.let { userLoginByPasswordForm ->
                lifecycleScope.launch {
                    btnLogin.isEnabled = false
                    textLogin.text = "登录中"
                    userLoginByPassword(userLoginByPasswordForm)
                    btnLogin.isEnabled = true
                    textLogin.text = "登录"
                    MessageReceiving.run()
                }
            } ?: run {
                Toast.makeText(
                    this@LoginByPasswordActivity, commonData.message, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateEditText(): ServerApiManager.CommonData<ServerApiManager.UserLoginByPasswordForm?> {
        val phoneNumber = editTextPhoneNumber.text.toString()
        if (phoneNumber.length != 11) {
            return ServerApiManager.CommonData(0, "手机号格式不正确", null)
        }
        val password = editTextPassword.text.toString()
        val passwordPattern = Regex("^(?=.*[a-zA-Z]).{8,24}$")
        if (!password.matches(passwordPattern)) {
            return ServerApiManager.CommonData(0, "密码需要8-24位，且需要包含字母", null)
        }
        return ServerApiManager.CommonData(
            1, "输入正确", ServerApiManager.UserLoginByPasswordForm(
                phoneNumber, password
            )
        )
    }

    private suspend fun runAfterUserLoginByPassword(data: String) {
        withContext(Dispatchers.Main) {
            CachedData.token = data
            val edit = sharedPreferences.edit()
            edit.putString("token", data)
            edit.apply()
            Toast.makeText(
                this@LoginByPasswordActivity, "登录成功", Toast.LENGTH_SHORT
            ).show()
            MessageReceiving.current_count = MessageReceiving.max_count
            EventBus.getDefault().post(MineFragment.UserInfoUpdateEvent())
            finish()
        }
    }

    private suspend fun userLoginByPassword(userLoginByPasswordForm: ServerApiManager.UserLoginByPasswordForm) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userLoginByPassword(
                    userLoginByPasswordForm
                ).await()
                when (commonData.code) {
                    1 -> {
                        runAfterUserLoginByPassword(commonData.data)
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@LoginByPasswordActivity, "手机号或密码错误", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LoginByPasswordActivity, "请求异常", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}