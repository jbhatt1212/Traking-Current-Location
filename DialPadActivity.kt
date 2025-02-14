package amplify.call.activity.call

import amplify.call.MainActivity
import amplify.call.R
import amplify.call.activity.authenticate.NumberVerificationActivity
import amplify.call.activity.authenticate.SignInEmailActivity
import amplify.call.activity.drawer.NumberPurchaseActivity
import amplify.call.activity.intro.PricingActivity
import amplify.call.activity.message.MessageActivity
import amplify.call.activity.telnyx.CallInComingActivity
import amplify.call.activity.telnyx.CallOutGoingActivity
import amplify.call.adapters.ContactsAdapter
import amplify.call.adapters.DialCountryAdapter
import amplify.call.adapters.DialPadAdapter
import amplify.call.adapters.DialTypeAdapter
import amplify.call.databinding.ActivityDialPadBinding
import amplify.call.dialog.showPermissionRationaleDialog
import amplify.call.models.model.ContactsModel
import amplify.call.models.model.DialPad
import amplify.call.models.model.DialType
import amplify.call.models.model.DialTypeModel
import amplify.call.models.responses.CountriesList
import amplify.call.models.viewmodels.DialPadViewModel
import amplify.call.telnyx.TelnyxManager
import amplify.call.utils.Logger
import amplify.call.utils.Prefs
import amplify.call.utils.getCorrectNumberRegion
import amplify.call.utils.getFormattedNumber
import amplify.call.utils.hideKeyboard
import amplify.call.utils.isValidNumber
import amplify.call.utils.keyCallerId
import amplify.call.utils.keyCallerName
import amplify.call.utils.keyCallerNumber
import amplify.call.utils.keyChatUserName
import amplify.call.utils.keyChatUserNumber
import amplify.call.utils.keyShowCallRating
import amplify.call.utils.keyShowRating
import amplify.call.utils.showToast
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.text.intl.Locale
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.telnyx.webrtc.sdk.model.SocketMethod
import com.telnyx.webrtc.sdk.verto.receive.ByeResponse
import com.telnyx.webrtc.sdk.verto.receive.InviteResponse
import com.telnyx.webrtc.sdk.verto.receive.LoginResponse
import com.telnyx.webrtc.sdk.verto.receive.MediaResponse
import com.telnyx.webrtc.sdk.verto.receive.ReceivedMessageBody
import com.telnyx.webrtc.sdk.verto.receive.RingingResponse
import com.telnyx.webrtc.sdk.verto.receive.SocketObserver

class DialPadActivity : MainActivity(), TextWatcher {

    private val TAG = DialPadActivity::class.java.simpleName
    private lateinit var binding: ActivityDialPadBinding

    private lateinit var countryAdapter: DialCountryAdapter
    private var oldCountryCode = ""
    private var countryCode = ""
    private var countryCodeISO = ""
    private var countryName = ""

    private val dialTypeList = DialType.DialTypeList
    private lateinit var dialTypeAdapter: DialTypeAdapter
    private var callType = 0

    private var dialPadList = DialPad.DialPadList
    private lateinit var adapter: DialPadAdapter

    private val dialPadViewModel: DialPadViewModel by viewModels()

    private val phoneNumberUtils = PhoneNumberUtil.getInstance()
    private var selectionStart = -1

    private var countriesList: List<CountriesList> = emptyList()

    private var callingNumber = ""
    private var callingName = ""
    private lateinit var chatContacts: ContactsModel

    private lateinit var contactsAdapter: ContactsAdapter
    private var contactsList: List<ContactsModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDialPadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hideKeyboard(this)
        showData()
        handleResponse()
        handleClickListeners()
        setupBackPress()

        if (!Prefs.isGuest && Prefs.isTelnyxUserLogin) {
            observeSocketResponses()
        }
    }

    override fun onResume() {
        super.onResume()
        callUserDetailApi(true)
    }

    private fun showData() {
        countryAdapter = DialCountryAdapter()
        binding.spDialerCountry.adapter = countryAdapter

        contactsAdapter = ContactsAdapter(
            onContactClick = { contacts ->
                binding.etDialNumber.setText(contacts.contactNumber)
            },
            onChatClick = { contact ->
                chatContacts = contact
                isValidForChat()
            },
            onDialerClick = { contacts ->
                binding.etDialNumber.setText(contacts.contactNumber)
                isValidForCall()
            }
        )
        binding.rvDialContactList.adapter = contactsAdapter
        adapter = DialPadAdapter({ dialNumber ->

            var textNumber = binding.etDialNumber.text.toString()
            /*if(selectionStart != 0){
                textNumber = textNumber.add(0, selectionStart)
            }*/
            if (textNumber.length <= 15) {
                textNumber = textNumber + dialNumber
                binding.etDialNumber.setText(textNumber)
            }

        }, { })
        dialTypeAdapter = DialTypeAdapter()
        dialTypeAdapter.addDialTypeList(dialTypeList)
        binding.spCallType.adapter = dialTypeAdapter

//        binding.rvDialerView.layoutManager = GridLayoutManager(this, 3)
        binding.rvDialerView.adapter = adapter
        adapter.addCallListData(dialPadList)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            hideKeyboard(this)
        }
        return super.dispatchTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleClickListeners() {

        binding.etDialNumber.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(
                v: View?,
                event: MotionEvent?
            ): Boolean {
                hideKeyboard(this@DialPadActivity)
                return true
            }
        })


        binding.ivCallDelNumber.setOnClickListener {
            var text = binding.etDialNumber.text.toString()
            selectionStart = binding.etDialNumber.selectionStart
            if (!text.isNullOrEmpty()) {
                text = if (selectionStart != 0) {
                    text.removeRange(selectionStart - 1, selectionStart)
                } else {
                    text.removeRange(text.length - 1, text.length)
                }
                binding.etDialNumber.setText(text)
            }
        }

        binding.ivCallDelNumber.setOnLongClickListener {
            binding.etDialNumber.setSelection(0)
            binding.etDialNumber.setText("")
            true
        }
        binding.spDialerCountry.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val country = countryAdapter.getItem(position) as CountriesList
                    val newCountryCode = country.code
                    var text = binding.etDialNumber.text.toString()


                    if (text == newCountryCode) return

                    if (text.isEmpty() || text == "+") {
                        binding.etDialNumber.setText(newCountryCode)
                        binding.etDialNumber.setSelection(newCountryCode.length)
                    } else {
                        val oldCountryCode = countryCode
                        if (oldCountryCode.isNotEmpty() && text.startsWith(oldCountryCode)) {
                            text = text.replaceFirst(oldCountryCode, newCountryCode)
                            binding.etDialNumber.setText(text)
                            binding.etDialNumber.setSelection(text.length)
                        }
                    }

                    countryCode = newCountryCode
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Logger.e(TAG, "onNothingSelected parent")
                }
            }

        binding.etDialNumber.addTextChangedListener(object : TextWatcher {
            private var isEditTextUpdate = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {


            }

            @SuppressLint("SuspiciousIndentation")
            override fun afterTextChanged(editable: Editable?) {
                if (isEditTextUpdate) {
                    isEditTextUpdate = false
                    return
                }
                var text = editable?.toString() ?: ""



                   if (!text.startsWith("+")) {
                    isEditTextUpdate = true
                    text = "+$text"
                    binding.etDialNumber.setText(text)
                    binding.etDialNumber.setSelection(text.length)
                    return
                }


                val currentPosition = binding.spDialerCountry.selectedItemPosition

                val currentCountry = countriesList.get(currentPosition)
                val formattedNumber = getFormattedNumber(text, currentCountry.countryIsoCode)
                Logger.e(TAG, "Original: $text, Formatted: $formattedNumber, Country ISO: ${currentCountry.countryIsoCode}")

                if (!formattedNumber.isNullOrEmpty() && formattedNumber != text) {
                    isEditTextUpdate = true
                    binding.etDialNumber.setText("${currentCountry.code} $formattedNumber")
                    binding.etDialNumber.setSelection(binding.etDialNumber.text.length)
                }

                val detectedRegion = getCorrectNumberRegion(text,currentCountry.countryIsoCode)

                var matchFound = false

                if (!detectedRegion.isNullOrEmpty()) {
                    for (i in 0 until countryAdapter.count) {
                        val country = countryAdapter.getItem(i) as CountriesList
                        if (country.countryIsoCode.equals(detectedRegion, ignoreCase = true)) {
                            if (currentPosition != i) {
                                isEditTextUpdate = true
                                binding.spDialerCountry.setSelection(i)
                            }
                            matchFound = true
                            break
                        }
                    }
                }
                if (!matchFound) {
                    for (i in 0 until countryAdapter.count) {
                        val country = countryAdapter.getItem(i) as CountriesList
                        if (text.startsWith(country.code)) {
                            if (currentPosition != i || country.countryIsoCode != currentCountry.countryIsoCode) {
                                isEditTextUpdate = true
                                binding.spDialerCountry.setSelection(i)
                            }
                            matchFound = true
                            break

                        }
                    }

                }
                if (!matchFound) {
                    Log.d(TAG, "No match found for text: $text")
                }

                Log.d(TAG, "After Selection: ${binding.spDialerCountry.selectedItemPosition}")
                isEditTextUpdate = false
           }

        })
        binding.spCallType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val dialType = dialTypeAdapter.getItem(position) as DialTypeModel
                callType = dialType.id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Logger.e(TAG,"onNothingSelected parent")
            }
        }

        binding.ivDialerCall.setOnClickListener {
            if (callType == 1) {
                isValidForCall()
            } else if (callType == 2) {
                isValidForLocalCall()
            }
        }
    }

    private fun isValidForCall() {
        if (binding.etDialNumber.text.toString().isEmpty()) {
            showToast(this, getString(R.string.validation_dial_number))
        } else if (!isValidNumber(binding.etDialNumber.text.toString())) {
            showToast(this, getString(R.string.validation_dial_number))
        } else if (!Prefs.isPlanActive) {
            pricingScreenLauncher.launch(
                Intent(this, PricingActivity::class.java)
                    .putExtra(keyShowRating, false)
            )
        } else if (Prefs.isGuest) {
            signInScreenLauncher.launch(
                Intent(this, SignInEmailActivity::class.java)
            )
        } else if (!Prefs.isVerifiedNumber) {
            phoneVerificationLauncher.launch(
                Intent(this, NumberVerificationActivity::class.java)
            )
        } else if (!Prefs.isNumberPurchased) {
            purchaseNumberLauncher.launch(
                Intent(this, NumberPurchaseActivity::class.java)
            )
        } else {
            callingNumber = binding.etDialNumber.text.toString()
            handleAudioPermission()
        }
    }

    private fun isValidForLocalCall() {
        if (binding.etDialNumber.text.toString().isEmpty()) {
            showToast(this, getString(R.string.validation_dial_number))
        } else if (!Prefs.isPlanActive) {
            pricingScreenLauncher.launch(
                Intent(this, PricingActivity::class.java)
                    .putExtra(keyShowRating, false)
            )
        } else if (Prefs.isGuest) {
            signInScreenLauncher.launch(
                Intent(this, SignInEmailActivity::class.java)
            )
        } else {
            startActivity(
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel: $callingNumber")
                )
            )
        }
    }

    private val pricingScreenLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                if (callType == 1) {
                    isValidForCall()
                } else if (callType == 2) {
                    isValidForLocalCall()
                }
            }
        }

    private val signInScreenLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                if (callType == 1) {
                    isValidForCall()
                } else if (callType == 2) {
                    isValidForLocalCall()
                }
            }
        }

    private val purchaseNumberLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                dialPadViewModel.getUserDetails()
                isValidForCall()
            }
        }

    private val phoneVerificationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                dialPadViewModel.getUserDetails()
                isValidForCall()
            }
        }

    private fun isValidForChat() {
        if (!Prefs.isPlanActive) {
            pricingScreenChatLauncher.launch(
                Intent(this, PricingActivity::class.java)
                    .putExtra(keyShowRating, false)
            )
        } else if (Prefs.isGuest) {
            signInScreenChatLauncher.launch(
                Intent(this, SignInEmailActivity::class.java)
            )
        } else {
            startActivity(
                Intent(this, MessageActivity::class.java)
                    .putExtra(keyChatUserNumber, chatContacts.contactNumber)
                    .putExtra(keyChatUserName, chatContacts.contactName)
            )
        }
    }

    private val pricingScreenChatLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                isValidForChat()
            }
        }

    private val signInScreenChatLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                isValidForChat()
            }
        }

    private val requestRecordPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showPermissionRationaleDialog(
                    this,
                    R.string.dlg_record_title,
                    R.string.dlg_record_dec,
                    R.string.dlg_record_btn_allow,
                    null
                )
            } else {
                dialPadViewModel.getSaveContact(binding.etDialNumber.text.toString())
            }
        }

    private fun handleAudioPermission() {
        val permission = android.Manifest.permission.RECORD_AUDIO
        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                dialPadViewModel.getSaveContact(binding.etDialNumber.text.toString())
            }

            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationaleDialog(
                    this,
                    R.string.dlg_record_title,
                    R.string.dlg_record_dec,
                    R.string.dlg_record_btn_allow,
                    null
                )
            }

            else -> {
                requestRecordPermission.launch(permission)
            }
        }
    }

    private fun handleResponse() {

        dialPadViewModel.getVerifyEmailError.observe(this) {
            if (!it.isNullOrEmpty()) {
                showEmailVerifyPopup()
            }
        }

        dialPadViewModel.getDialNumberData.observe(this) {
            if (it != null) {
                binding.rvDialContactList.visibility = View.VISIBLE
                binding.mdDividerFive.visibility = View.GONE
                contactsList = emptyList()
                contactsList = it
                contactsAdapter.addContacts(contactsList)
            }
        }

        dialPadViewModel.getDialNumberEmpty.observe(this) {
            Logger.d(TAG, "getDialNumberEmpty $it")
            binding.rvDialContactList.visibility = View.GONE
            binding.mdDividerFive.visibility = View.GONE
            contactsList = emptyList()
            contactsAdapter.addContacts(contactsList)
        }

        dialPadViewModel.getCountryList()

        dialPadViewModel.getCountries.observe(this) {
            if (!it.isNullOrEmpty()) {
                countriesList = it
                countryAdapter.addCallListData(it)
                countriesList.forEachIndexed { index, countriesList ->
                    if (countriesList.countryIsoCode == Locale.current.region) {
                        binding.spDialerCountry.setSelection(index)
                    }
                }
            }
        }

        dialPadViewModel.getContactData.observe(this) {
            if (it != null) {
                var fName = ""
                var lName = ""
                if (it.contactName.toString().contains(" ")) {
                    fName = it.contactName.toString().split(" ")[0]
                    lName = it.contactName.toString().split(" ")[1]
                } else {
                    fName = it.contactName.toString()
                }
                callingName = it.contactName
                dialPadViewModel.initiateCall(it.contactNumber, fName, lName)
            }
        }

        dialPadViewModel.getNoContactError.observe(this) {
            if (!it.isNullOrEmpty()) {
                callingName = ""
                dialPadViewModel.initiateCall(binding.etDialNumber.text.toString(), "", "")
            }
        }

        dialPadViewModel.getInitiateCallData.observe(this) {
            if (it != null) {
                dialPadViewModel.clearCallData()
                TelnyxManager.sendInvite(
                    callerName = Prefs.isTelnyxCallerIdName,
                    callerNumber = Prefs.isTelnyxCallerIdNumber,
                    destinationNumber = it.receiver.number,
                    clientState = "Sample Client State"
                )
            }
        }

        dialPadViewModel.getError.observe(this) {
            if (!it.isNullOrEmpty()) {
                showToast(this, it)
                dialPadViewModel.clearCallData()
            }
        }

        /*dialPadViewModel.getEmailVerifyError.observe(this) {
            if (!it.isNullOrEmpty()) {
                showToast(this, it)
                dialPadViewModel.clearResendEmail()
            }
        }*/

        dialPadViewModel.getEmailVerifySuccess.observe(this) {
            if (!it.isNullOrEmpty()) {
                showToast(this, it)
                dialPadViewModel.clearResendEmail()
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backPress()
            }
        })
    }

    private fun backPress() {
        setResult(RESULT_OK)
        finish()
    }

    fun observeSocketResponses() {
        TelnyxManager.getWsMessageResponse()?.observe(this) {
            it.let {
                Logger.d(TAG, "Socket Response WsMessage $it")
            }
        }

        TelnyxManager.getSocketResponse()
            ?.observe(this, object : SocketObserver<ReceivedMessageBody>() {
                override fun onConnectionEstablished() {
                    Logger.d(TAG, "Socket Response onConnectionEstablished")
                }

                override fun onError(message: String?) {
                    Logger.d(TAG, "Socket Response onError")
                }

                override fun onLoading() {
                    Logger.d(TAG, "Socket Response onLoading")
                }

                override fun onMessageReceived(data: ReceivedMessageBody?) {
                    Logger.d(TAG, "Socket Response onMessageReceived")
                    Logger.d(TAG, "Socket Response onMessageReceived data.method ${data?.method}")
                    Logger.d(TAG, "Socket Response onMessageReceived data.result ${data?.result}")
                    when (data?.method) {

                        SocketMethod.LOGIN.methodName -> {
                            Logger.d(TAG, "Socket Response LOGIN: ")
                            val loginResponse = (data.result as LoginResponse)
                            Prefs.isTelnyxUserLogin = true
                            Logger.d(TAG, "Current Session LOGIN: $loginResponse")
                        }

                        SocketMethod.INVITE.methodName -> {
                            Logger.d(TAG, "Socket Response INVITE: ")
                            val inviteResponse = data.result as InviteResponse
                            TelnyxManager.setCurrentCall(inviteResponse.callId)
                            Logger.d(
                                TAG,
                                "Socket Response INVITE: inviteResponse $inviteResponse"
                            )
                            onInComingCallLauncher.launch(
                                Intent(this@DialPadActivity, CallInComingActivity::class.java)
                                    .putExtra(keyCallerName, inviteResponse.callerIdName)
                                    .putExtra(keyCallerNumber, inviteResponse.callerIdNumber)
                                    .putExtra(keyCallerId, inviteResponse.callId.toString())
                            )
                        }

                        SocketMethod.RINGING.methodName -> {
                            Logger.d(TAG, "Socket Response RINGING: ")
                            val ringingResponse = (data.result as RingingResponse)
                            TelnyxManager.setCurrentCall(ringingResponse.callId)
                            Logger.d(
                                TAG,
                                "Socket Response RINGING: ringingResponse $ringingResponse"
                            )
                            onOutGoingCallLauncher.launch(
                                Intent(this@DialPadActivity, CallOutGoingActivity::class.java)
                                    .putExtra(keyCallerName, callingName)
                                    .putExtra(keyCallerNumber, callingNumber)
                                    .putExtra(keyCallerId, ringingResponse.callId.toString())
                            )
                        }

                        SocketMethod.MEDIA.methodName -> {
                            Logger.d(TAG, "Socket Response MEDIA: ")
                            val mediaResponse = (data.result as MediaResponse)
                            TelnyxManager.setCurrentCall(mediaResponse.callId)
                            Logger.d(
                                TAG,
                                "Socket Response RINGING: mediaResponse $mediaResponse"
                            )
                            onOutGoingCallLauncher.launch(
                                Intent(this@DialPadActivity, CallOutGoingActivity::class.java)
                                    .putExtra(keyCallerName, callingName)
                                    .putExtra(keyCallerNumber, callingNumber)
                                    .putExtra(keyCallerId, mediaResponse.callId.toString())
                            )
                        }

                        SocketMethod.BYE.methodName -> {
                            Logger.d(TAG, "Socket Response BYE: ")
                            val byeResponse = (data.result as ByeResponse)
                            Logger.d(TAG, "Socket Response BYE: byeResponse $byeResponse")
                            onResume()
                        }

                        else -> {
                            Logger.d(TAG, "Socket Response ELSE: ${data?.method}")
                        }
                    }
                }

                override fun onSocketDisconnect() {
                    Logger.d(TAG, "Socket Response onSocketDisconnect")
                }
            })
    }

    private val onOutGoingCallLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Logger.d(TAG,"onOutGoingCallLauncher resultCode $it")
            Logger.d(TAG,"onOutGoingCallLauncher resultCode ${it.resultCode}")
            if (it.resultCode == RESULT_OK) {
                Logger.d(TAG,"onOutGoingCallLauncher data ${it.data}")
                if (it.data != null) {
                    val data = it.data?.getBooleanExtra(keyShowCallRating, false)
                    Logger.d(TAG,"onOutGoingCallLauncher keyShowCallRating $data")
                    if (data == true) {
                        setResult(RESULT_OK, it.data)
                    }
                }
            }
            finish()
        }

    private val onInComingCallLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                finish()
            }
        }

    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) {
        Logger.e(TAG, "beforeTextChanged $s")
    }

    override fun onTextChanged(
        charSequence: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        Logger.e(TAG, "onTextChanged $charSequence")
        binding.etDialNumber.removeTextChangedListener(this)
        var oldNumber = binding.etDialNumber.text.toString()
        val oldLength = oldNumber.length

        if (charSequence.isNullOrEmpty() && oldNumber.isEmpty()) {
            oldNumber = countryCode
        } else {
            if (charSequence?.equals(countryCode) != false) {
                oldNumber = oldNumber + charSequence.toString()
            }
            if (oldNumber.contains(" ")) {
                oldNumber = oldNumber.replace(" ", "")
            }
            if (oldNumber.contains("-")) {
                oldNumber = oldNumber.replace("-", "")
            }
            oldNumber = try {
                val numbers = phoneNumberUtils.parse(oldNumber, countryCodeISO)
                if (phoneNumberUtils.isValidNumber(numbers)) {
                    phoneNumberUtils.format(
                        numbers,
                        PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
                    )
                } else {
                    oldNumber
                }
            } catch (e: Exception) {
                oldNumber
            }
        }
        val newLength = oldNumber.length
        val length = if (newLength > oldLength) {
            newLength - oldLength
        } else if (oldLength > newLength) {
            oldLength - newLength
        } else {
            newLength
        }
        Logger.e(TAG, "onTextChanged $oldNumber")
        binding.etDialNumber.setText(oldNumber)
        dialPadViewModel.getDialPadData(oldNumber)
        if (countriesList.isNotEmpty()) {
            val regionIso = getCorrectNumberRegion(oldNumber.toString(), "US")
            countriesList.forEachIndexed { index, countriesList ->
                if (countriesList.countryIsoCode == regionIso) {
                    binding.spDialerCountry.setSelection(
                        index)
                    countryCode = countriesList.code
                }
            }
        }
//        val simpleNumber = getSimpleNumber(binding.etDialNumber.text.toString())
//       binding.etDialNumber.setText(simpleNumber)
        binding.etDialNumber.addTextChangedListener(this)

    }
    override fun afterTextChanged(s: Editable?) {
        Logger.e(TAG, "afterTextChanged $s")
    }
}