package com.google.firebase.udacity.friendlychat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

import com.firebase.ui.auth.AuthUI
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        val ANONYMOUS                = "anonymous"
        val DEFAULT_MSG_LENGTH_LIMIT = 1000
        val MSG_LENGTH_KEY           = "msg_length"
        val SIGN_IN                  = 1
        val PHOTO_PICKER             = 2
    }

    private var mMessageListView          : RecyclerView?       = null
    private var mMessageAdapter           : MessageAdapter?     = null
    private var mProgressBar              : ProgressBar?        = null
    private var mPhotoPickerButton        : ImageButton?        = null
    private var mMessageEditText          : EditText?           = null
    private var mSendButton               : Button?             = null
    private var mUsername                 : String?             = null
    private var mDisposable               : Disposable?         = null
    private var mDisposableChildMass      : Disposable?         = null

    private var mFirebaseDatabase         : FirebaseDatabase?               = null
    private var mMessageFirebaseReference : DatabaseReference?              = null
    private var mChildEventListener       : ChildEventListener?             = null
    private var mFirebaseAuth             : FirebaseAuth?                   = null
    private var mFirebaseAuthListenere    : FirebaseAuth.AuthStateListener? = null
    private var mFirebaseStorage          : FirebaseStorage?                = null
    private var mStorageReference         : StorageReference?               = null
    private var mFirebaseRemoteConf       : FirebaseRemoteConfig?           = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUsername = ANONYMOUS

        initDB()
        initStorage()
        initAuth()
        bindView()
        initAdapter()
        initRemoteConfig()
        removeProgressBar()

        val textChangeObservable = createTextChangeObservable()
        mDisposable = textChangeObservable.subscribe()

    }

    override fun onResume() {
        super.onResume()
        mFirebaseAuth?.addAuthStateListener(mFirebaseAuthListenere!!)
    }

    override fun onPause() {
        super.onPause()
        mFirebaseAuth?.removeAuthStateListener(mFirebaseAuthListenere!!)
        detachListener()
    }

    override fun onStop() {
        super.onStop()
        if (mDisposable != null || !mDisposable!!.isDisposed)
            mDisposable!!.dispose()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == SIGN_IN) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish()// Sign in failed
            } else if (requestCode == PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
                val selectedImageUri = data?.data
                if (selectedImageUri != null) {
                    val photoRef = mStorageReference?.child(selectedImageUri.lastPathSegment)
                    photoRef?.putFile(selectedImageUri)?.addOnSuccessListener {
                        taskSnapshot ->
                        val downloadUrl = taskSnapshot.downloadUrl
                        val message = ChatMessage("", mUsername!!, downloadUrl.toString())
                        mMessageFirebaseReference?.push()?.setValue(message)
                    }
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.sendButton        ->  {
                val textMessage = mMessageEditText?.text.toString()
                if (!TextUtils.isEmpty(textMessage)) {
                    val message = ChatMessage(textMessage, "", "")
                    mMessageFirebaseReference?.push()?.setValue(message)
                }}
            R.id.photoPickerButton  ->  {
                val intent  = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/jpeg"
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), PHOTO_PICKER)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun removeProgressBar() {
        mProgressBar?.visibility = ProgressBar.INVISIBLE
    }

    private fun initRemoteConfig() {
        mFirebaseRemoteConf = FirebaseRemoteConfig.getInstance()
        val configSetting   = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        mFirebaseRemoteConf?.setConfigSettings(configSetting)

        val defaultConfMap : HashMap<String, Any> = hashMapOf(MSG_LENGTH_KEY to DEFAULT_MSG_LENGTH_LIMIT)
        mFirebaseRemoteConf?.setDefaults(defaultConfMap)
        fetchConfig()
    }

    private fun fetchConfig() {
        var cacheExpiration : Long = 3000

        if (mFirebaseRemoteConf!!.info.configSettings.isDeveloperModeEnabled) {
            cacheExpiration = 0
        }
        mFirebaseRemoteConf?.fetch(cacheExpiration)
                ?.addOnSuccessListener {
                    mFirebaseRemoteConf?.activateFetched()
                    applyRetrievedLengthLimit()
                }
                ?.addOnFailureListener { exception -> Log.e("Fetch Config", exception.message)}
    }

    private fun applyRetrievedLengthLimit() {
        var msgLength = mFirebaseRemoteConf?.getLong(MSG_LENGTH_KEY)
    }

    private fun initAuth() {
        mFirebaseAuth = FirebaseAuth.getInstance()
        initReadListener()
    }

    private fun signedOutInitialize() {
        mUsername = ANONYMOUS
        detachListener()
    }

    private fun detachListener() {
        mMessageAdapter?.clear()
        mDisposableChildMass?.dispose()
    }

    private fun signedInInitialize(displayName: String?) {
        mUsername = displayName
        attachChildEventListener()
    }

    private fun initDB() {
        mFirebaseDatabase         = FirebaseDatabase.getInstance()
        mMessageFirebaseReference = mFirebaseDatabase?.reference?.child("Message")
    }

    private fun initStorage() {
        mFirebaseStorage  = FirebaseStorage.getInstance()
        mStorageReference = mFirebaseStorage!!.reference.child("chat_photos")
    }

    private fun attachChildEventListener() {
        mDisposableChildMass = initChildEventListener()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { message -> mMessageAdapter?.addMessage(message) },
                        { error -> Log.e("Error Message", error.message) })
    }

    private fun initAdapter() {
        val friendlyMessages = ArrayList<ChatMessage>()
        mMessageAdapter      = MessageAdapter()
        mMessageAdapter!!.addMessageList(friendlyMessages)
        mMessageListView?.adapter = mMessageAdapter
    }

    private fun bindView() {
        mProgressBar       = findViewById(R.id.progressBar)       as ProgressBar
        mMessageListView   = findViewById(R.id.messageListView)   as RecyclerView
        mPhotoPickerButton = findViewById(R.id.photoPickerButton) as ImageButton
        mMessageEditText   = findViewById(R.id.messageEditText)   as EditText
        mSendButton        = findViewById(R.id.sendButton)        as Button
        mSendButton!!.setOnClickListener(this)
    }

    private fun createTextChangeObservable(): Observable<String> {
        val observable = Observable.create(ObservableOnSubscribe<String>{ emitter ->
            val watcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    mSendButton?.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()}
                override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    emitter.onNext(s.toString())
                }
            }
            mMessageEditText?.addTextChangedListener(watcher)
            emitter.setCancellable { mMessageEditText?.removeTextChangedListener(watcher) }
        })
        return observable
    }

    private fun initChildEventListener() : Observable<ChatMessage>{
        val observable = Observable.create(ObservableOnSubscribe<ChatMessage> { emitter ->
            mChildEventListener = object : ChildEventListener{
                override fun onCancelled(p0: DatabaseError?) {
                    emitter.onError(p0?.toException())
                }

                override fun onChildMoved(p0: DataSnapshot?, p1: String?) { }

                override fun onChildChanged(p0: DataSnapshot?, p1: String?) { }

                override fun onChildAdded(p0: DataSnapshot?, p1: String?) {
                    emitter.onNext(p0?.getValue(ChatMessage::class.java))
                }

                override fun onChildRemoved(p0: DataSnapshot?) { }
            }
            mMessageFirebaseReference?.addChildEventListener(mChildEventListener)
            emitter.setCancellable { mMessageFirebaseReference?.addChildEventListener(null)}
        })
        return observable
    }

    private fun initReadListener() {
        mFirebaseAuthListenere = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                signedInInitialize(user.displayName)
            } else {
                signedOutInitialize()
                startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setProviders(Arrays.asList(AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build()))
                        .build(),
                        SIGN_IN)
            }
        }
    }
}
