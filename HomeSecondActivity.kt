package com.example.myapp

import android.content.Intent
import android.view.KeyEvent

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.model.CallData
import com.example.myapp.model.ChatData
import com.example.myapp.model.ContactData
import com.example.myapp.model.CountryData
import com.example.myapp.model.DialerTypeData
import com.example.myapp.model.MissedCallData
import com.example.myapp.model.UnreadChatData
import com.example.myapp.simplemobiletools.activities.MyEditText
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationView
import java.text.NumberFormat
import java.util.Locale


class HomeSecondActivity : AppCompatActivity() {
    private var textlength = 0

    private val contactList = ArrayList<ContactData>()
    private val callList = ArrayList<CallData>()
    private val missedCallList = ArrayList<MissedCallData>()
    private val chatList = ArrayList<ChatData>()
    private val unreadChatList = ArrayList<UnreadChatData>()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var bottomSheetTeachersDialog: BottomSheetDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_second)
        val recyclerView: RecyclerView = findViewById(R.id.rvFavourite)
        val recyclerView1: RecyclerView = findViewById(R.id.rvCall)
        val recyclerView2: RecyclerView = findViewById(R.id.rvMissedCall)
        val recyclerView3: RecyclerView = findViewById(R.id.rvChat)
        val recyclerView4: RecyclerView = findViewById(R.id.rvUnreadChat)

        setupBottomSheet()

        // Set click listener for the dialer button
        findViewById<ImageView>(R.id.ivDialerIcon).setOnClickListener {
            if (!bottomSheetTeachersDialog.isShowing) {
                bottomSheetTeachersDialog.show()
            }
        }
     // set click listener for chat Button
        findViewById<ImageView>(R.id.ivChatIcon).setOnClickListener {
            val intent = Intent(this@HomeSecondActivity,NewMessageActivity::class.java)
            startActivity(intent)
        }

        // SET Navigation drawer
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.main)

        val toolbarMenu: ImageView = findViewById(R.id.toolbar_menu)

        val textView: TextView = toolbar.findViewById(R.id.toolbar_title)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.nav_open, R.string.nav_close
        )

        drawerLayout.addDrawerListener(toggle)


        toolbarMenu.setOnClickListener {
            drawerLayout.openDrawer(findViewById<NavigationView>(R.id.navigationView))
        }

        // set navigation item option select
        val navigation: NavigationView = findViewById(R.id.navigationView)

        navigation.setNavigationItemSelectedListener { menuItem ->
            val id = menuItem.itemId
            if (id == R.id.login) {
                val intent = Intent(this@HomeSecondActivity, LoginActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.setting) {
                val intent = Intent(this@HomeSecondActivity, SettingActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.contact) {
                val intent = Intent(this@HomeSecondActivity, ContactSupportActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.changePassword){
                startActivity(Intent(this@HomeSecondActivity,ChangePasswordActivity::class.java))
            }
            true
        }

        // for RecyclerView3 and RecyclerView4
        val chatText: TextView = findViewById(R.id.tvChatAll)
        val unreadChatText: TextView = findViewById(R.id.tvUnreadChat)


        chatText?.setOnClickListener {
            if (recyclerView3.visibility == View.GONE) {
                recyclerView3.visibility = View.VISIBLE
                recyclerView4.visibility = View.GONE
            } else {
                recyclerView3.visibility = View.GONE
                recyclerView4.visibility = View.VISIBLE
            }
        }

        unreadChatText?.setOnClickListener {
            if (recyclerView4.visibility == View.VISIBLE) {
                recyclerView4.visibility = View.GONE
                recyclerView3.visibility = View.VISIBLE
            } else {
                recyclerView4.visibility = View.VISIBLE
                recyclerView3.visibility = View.GONE
            }
        }
        // RecyclerView1 and RecyclerView2
        val textALL: TextView = findViewById(R.id.tvCallAll)
        val textMissed: TextView = findViewById(R.id.tvMissedCall)

        textALL?.setOnClickListener {
            if (recyclerView1.visibility == View.GONE) {
                recyclerView1.visibility = View.VISIBLE
                recyclerView2.visibility = View.GONE
            } else {
                recyclerView1.visibility = View.GONE
                recyclerView2.visibility = View.VISIBLE
            }
        }
        textMissed?.setOnClickListener {
            if (recyclerView2.visibility == View.VISIBLE) {
                recyclerView2.visibility = View.GONE
                recyclerView1.visibility = View.VISIBLE
            } else {
                recyclerView2.visibility = View.VISIBLE
                recyclerView1.visibility = View.GONE
            }
        }
        // for calls and chat
        val clCallIcon: ConstraintLayout = findViewById(R.id.clCallIcon)
        val clCall: ConstraintLayout = findViewById(R.id.clAll)
        val clChatIcon: ConstraintLayout = findViewById(R.id.clChatIcon)
        val clChat: ConstraintLayout = findViewById(R.id.clChat)

        val callImage: ImageView = findViewById(R.id.ivCall)
        val callText: TextView = clCallIcon.findViewById(R.id.tvCallTxt)
        val chatImage: ImageView = findViewById(R.id.ivChat)
        val chatTxt: TextView = clChatIcon.findViewById(R.id.tvChatTxt)
        val dialerIcon: ImageView = findViewById(R.id.ivDialerIcon)
        val chatIcon : ImageView = findViewById(R.id.ivChatIcon)

        clCallIcon.setOnClickListener{
            clCall.visibility = View.VISIBLE
            clChat.visibility = View.GONE
            dialerIcon.visibility = View.VISIBLE
            chatIcon.visibility = View.GONE
            callImage.setImageResource(R.drawable.ic_home_second_call)
            callText.setTextColor(Color.parseColor("#FF000000"))
            chatImage.setImageResource(R.drawable.ic_home_second_chat)
            chatTxt.setTextColor(Color.GRAY)
        }
        clChatIcon.setOnClickListener{
            clChat.visibility = View.VISIBLE
            clCall.visibility = View.GONE
            chatIcon.visibility = View.VISIBLE
            dialerIcon.visibility =  View.GONE
            chatImage.setImageResource(R.drawable.ic_home_second_chat_active)
            chatTxt.setTextColor(Color.parseColor("#FF000000"))
            callImage.setImageResource(R.drawable.ic_home_second_call_inactive)
            callText.setTextColor(Color.GRAY)
        }


        // for RecyclerView4
        unreadChatList.add(UnreadChatData(R.string.home_second_unread_chat))
        //for RecyclerView3
        chatList.add(
            ChatData(
                R.drawable.ic_home_second_chat_person,
                R.string.home_second_chat_name1,
                R.string.home_second_chat_msg1,
                R.string.home_second_chat_time1
            )
        )
        chatList.add(
            ChatData(
                R.drawable.ic_home_second_chat_person1,
                R.string.home_second_chat_name2,
                R.string.home_second_chat_msg2,
                R.string.home_second_chat_time2
            )
        )
        chatList.add(
            ChatData(
                R.drawable.ic_home_second_chat_person,
                R.string.home_second_chat_name3,
                R.string.home_second_chat_msg3,
                R.string.home_second_chat_time3
            )
        )
        chatList.add(
            ChatData(
                R.drawable.ic_home_second_chat_person,
                R.string.home_second_chat_name4,
                R.string.home_second_chat_msg4,
                R.string.home_second_chat_time4
            )
        )
        // for RecyclerView2
        missedCallList.add(MissedCallData(R.string.home_second_missed_call))
        //for RecycleVew1
        callList.add(
            CallData(
                R.drawable.ic_home_second_call_outgoing_icon,
                R.string.home_second_call1_name,
                R.string.home_second_call1_desc
            )
        )
        callList.add(
            CallData(
                R.drawable.ic_home_second_call_outgoing_icon,
                R.string.home_second_call1_name1,
                R.string.home_second_call1_desc1
            )
        )
        callList.add(
            CallData(
                R.drawable.ic_contact_1,
                R.string.home_second_call1_name2,
                R.string.home_second_call1_desc2
            )
        )

        callList.add(
            CallData(
                R.drawable.ic_home_second_call_outgoing_icon,
                R.string.home_second_call1_name3,
                R.string.home_second_call1_desc3
            )
        )
        callList.add(
            CallData(
                R.drawable.ic_home_second_call_outgoing_icon,
                R.string.home_second_call1_name4,
                R.string.home_second_call1_desc4
            )
        )
        callList.add(
            CallData(
                R.drawable.ic_home_second_call_outgoing_icon,
                R.string.home_second_call1_name,
                R.string.home_second_call1_desc
            )
        )
        callList.add(
            CallData(
                R.drawable.ic_home_second_call_outgoing_icon,
                R.string.home_second_call1_name1,
                R.string.home_second_call1_desc1
            )
        )
        callList.add(
            CallData(
                R.drawable.ic_contact_1,
                R.string.home_second_call1_name2,
                R.string.home_second_call1_desc2
            )
        )


        //  for RecyclerView
        contactList.add(
            ContactData(
                R.drawable.ic_contact_1,
                R.string.contact_name1,
                R.string.contact_num1
            )
        )
        contactList.add(
            ContactData(
                R.drawable.ic_contact_2,
                R.string.contact_name2,
                R.string.contact_num2
            )
        )
        contactList.add(
            ContactData(
                R.drawable.ic_contact_3,
                R.string.contact_name3,
                R.string.contact_num3
            )
        )
        contactList.add(
            ContactData(
                R.drawable.ic_contact_4,
                R.string.contact_name4,
                R.string.contact_num4
            )
        )

        contactList.add(
            ContactData(
                R.drawable.ic_contact_new,
                R.string.contact_new_name,
                R.string.contact_new_num
            )
        )
        // RecyclerView4

        val unreadChatAdapter = UnreadChatAdapter(unreadChatList)
        recyclerView4.adapter = unreadChatAdapter

        val verticalLayout4 =
            LinearLayoutManager(this@HomeSecondActivity, LinearLayoutManager.VERTICAL, false)
        recyclerView4.layoutManager = verticalLayout4
        // RecyclerView3
        val chatAdapter = ChatAdapter(chatList)
        recyclerView3.adapter = chatAdapter

        val verticalLayout3 =
            LinearLayoutManager(this@HomeSecondActivity, LinearLayoutManager.VERTICAL, false)
        recyclerView3.layoutManager = verticalLayout3
        // RecyclerView2
        val missedCallAdapter = MissedCallAdapter(missedCallList)
        recyclerView2.adapter = missedCallAdapter
        val verticalLayout2 =
            LinearLayoutManager(this@HomeSecondActivity, LinearLayoutManager.VERTICAL, false)
        recyclerView2.layoutManager = verticalLayout2

        // RecyclerView1
        val callAdapter = CallAdapter(callList)
        recyclerView1.adapter = callAdapter

        val verticalLayout1 =
            LinearLayoutManager(this@HomeSecondActivity, LinearLayoutManager.VERTICAL, false)
        recyclerView1.layoutManager = verticalLayout1

        // RecyclerView
        val adapter = ContactAdapter(contactList)
        recyclerView.adapter = adapter
        val horizontalLayout = LinearLayoutManager(
            this@HomeSecondActivity,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.layoutManager = horizontalLayout


    }


    private fun setupBottomSheet() {
        // Initialize the BottomSheetDialog
        bottomSheetTeachersDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)

        // Inflate the layout
        val layout: View = LayoutInflater.from(this).inflate(R.layout.dialer, null)
        bottomSheetTeachersDialog.setContentView(layout)

        // Set up the spinners and other components
        val spinner: Spinner = layout.findViewById(R.id.countrySpinner)
        val dialerTypeSpinner: Spinner = layout.findViewById(R.id.dialerTypeSpinner)

        // Populate the country spinner
        val countryItems = listOf(
            CountryData(R.drawable.ic_spinner_item_us, "United States"),
            CountryData(R.drawable.ic_spinner_dropdown_item, "India")
        )
        val adapter = CountryAdapter(this, countryItems)
        spinner.adapter = adapter

        // Populate the dialer type spinner
        val dialerTypeItems = listOf(
            DialerTypeData("Local call with Verizon"),
            DialerTypeData("Local call with Wifi")
        )
        val dialerTypeAdapter = DialerTypeAdapter(this, dialerTypeItems)
        dialerTypeSpinner.adapter = dialerTypeAdapter

        // Set up the dial pad input
        val dialPadInput: MyEditText = layout.findViewById(R.id.etNum)

        // for india country number format
        fun indianNumFormat() {
            Log.d("NumberFormat", "Formatting number: ${dialPadInput.text}")
            dialPadInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (spinner.selectedItemPosition == 1) {
                        dialPadInput.setSelection(dialPadInput.getText()!!.length)
                        val text: String = dialPadInput.getText().toString()
                        dialPadInput.setMaxLength(16)
                        textlength = dialPadInput.getText()?.length!!
                        if (text.endsWith(" ")) return
                        if (textlength == 4) {
                            dialPadInput.setText(
                                StringBuilder(text).insert(text.length - 1, " ").toString()
                            )
                            dialPadInput.setSelection(dialPadInput.getText()!!.length)

                        } else if (textlength == 8) {
                            dialPadInput.setText(
                                StringBuilder(text).insert(text.length - 1, " ").toString()
                            )
                            dialPadInput.setSelection(dialPadInput.getText()!!.length)

                        } else if (textlength == 12) {
                            dialPadInput.setText(
                                StringBuilder(text).insert(text.length - 1, " ").toString()
                            )
                            dialPadInput.setSelection(dialPadInput.getText()!!.length)
                        }

                    }
                }

                override fun afterTextChanged(s: Editable?) {

                }

            })
        }

        // for us country number format
        fun usNumFormat() {
            dialPadInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (spinner.selectedItemPosition == 0) {
                        dialPadInput.setSelection(dialPadInput.getText()!!.length)
                        val text: String = dialPadInput.getText().toString()
                        dialPadInput.setMaxLength(16)
                        textlength = dialPadInput.getText()?.length!!
                        if (text.endsWith(" ")) return
                        if (textlength == 3) {
                            if (!text.contains("(")) {
                                dialPadInput.setText(
                                    StringBuilder(text).insert(text.length - 1, "(").toString()
                                )
                                dialPadInput.setSelection(dialPadInput.getText()!!.length)
                            }
                        } else if (textlength == 7) {
                            if (!text.contains(")")) {
                                dialPadInput.setText(
                                    StringBuilder(text).insert(text.length - 1, ")").toString()
                                )
                                dialPadInput.setSelection(dialPadInput.getText()!!.length)
                            }
                        } else if (textlength == 8) {
                            dialPadInput.setText(
                                StringBuilder(text).insert(text.length - 1, " ").toString()
                            )
                            dialPadInput.setSelection(dialPadInput.getText()!!.length)
                        } else if (textlength == 12) {
                            dialPadInput.setText(
                                StringBuilder(text).insert(text.length - 1, "-").toString()
                            )
                            dialPadInput.setSelection(dialPadInput.getText()!!.length)
                        }

                    }
                }

                override fun afterTextChanged(s: Editable?) {
                }

            })
        }
        dialPadInput.showSoftInputOnFocus = false
        setupDialPadButtons(layout, dialPadInput)

        // Set up spinner listener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0 -> {  // United States
                        dialPadInput.setText("+1")
                        usNumFormat()
                    }

                    1 -> { // India
                        dialPadInput.setText("+91")
                        indianNumFormat()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        // Handle backspace click
        val backPaceBtn: ImageView = layout.findViewById(R.id.ivBackPace)
        backPaceBtn.setOnClickListener {
            handleBackPace(dialPadInput)
        }
    }

    private fun handleBackPace(dialPadInput: MyEditText) {
        val text: String = dialPadInput.text.toString()
        if (text.isNotEmpty()) {
            val lastChar = text[text.length - 1]

            if (lastChar == '(' || lastChar == ')' || lastChar == '-' || lastChar == ' ') {
                dialPadInput.setText(text.substring(0, text.length - 2))
            } else {
                dialPadInput.setText(text.substring(0, text.length - 1))
            }

            dialPadInput.setSelection(dialPadInput.text!!.length)
        }
    }


    private fun setupDialPadButtons(layout: View, dialPadInput: MyEditText) {
        val dialPadIds = listOf(
            R.id.dialPad_0_holder to "0",
            R.id.dialPad_1_holder to "1",
            R.id.dialPad_2_holder to "2",
            R.id.dialPad_3_holder to "3",
            R.id.dialPad_4_holder to "4",
            R.id.dialPad_5_holder to "5",
            R.id.dialPad_6_holder to "6",
            R.id.dialPad_7_holder to "7",
            R.id.dialPad_8_holder to "8",
            R.id.dialPad_9_holder to "9",
            R.id.dialPad_asterisk_holder to "*",
            R.id.dialPad_hashtag_holder to "#"
        )

        for ((id, value) in dialPadIds) {
            layout.findViewById<View>(id).setOnClickListener {
                onDialPadButtonClick(dialPadInput, value)
            }
        }
    }

    private fun onDialPadButtonClick(dialPadInput: MyEditText, value: String) {
        val currentText = dialPadInput.text.toString()
        dialPadInput.setText(currentText + value)
    }

    fun MyEditText.setMaxLength(maxLength: Int) {
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
    }
}

