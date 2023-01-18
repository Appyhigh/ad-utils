package com.example.speakinenglish.container

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.appyhigh.adutils.utils.container.*
import java.lang.ref.SoftReference

object AppPrefs {
    val PREFERENCE_NAME = "ADMODEL"
    private var sPreferenceList: SoftReference<ArrayList<Containable>>? = null
    val appdata = StringContainer("appdata", "")
    val ads = StringContainer("ads", "")
    val lastFetched = LongContainer("lastFetched", 0)
    val showAppAds = BooleanContainer("showAppAds", true)



    fun load(context: Context?): Boolean {
        val shared_preference: SharedPreferences? = context?.let { getPreference(it) }
        for (pref in getPreferenceList()!!) {
            if (pref != null) {
                pref.read(shared_preference)
            }
        }
        return true
    }

    fun commit(context: Context): Boolean {
        val editor =
            context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
        for (pref in getPreferenceList()!!) {
            if (pref != null) {
                pref.write(editor)
            }
        }
        return editor.commit()
    }

    fun commit(context: Context?, vararg prefs: Containable): Boolean {
        val editor: Editor? = context?.let { getPreference(it).edit() }
        for (pref in prefs) {
            pref.write(editor)
        }
        if (editor != null) {
            return editor.commit()
        }
        return false
    }

    fun getPreference(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    private fun getPreferenceList(): ArrayList<Containable>? {
        var list: ArrayList<Containable>? = null
        if (sPreferenceList != null && sPreferenceList!!.get().also { list = it } != null)
            return list
        list = ArrayList()
        try {
            val fields = AppPrefs::class.java.declaredFields
            for (field in fields) {
                val obj = field[null]
                if (obj is Containable) { // null returns false
                    list!!.add(obj)
                }
            }
        } catch (e: IllegalArgumentException) {
            ErrorReport.printAndWriteLog(e)
        } catch (e: IllegalAccessException) {
            ErrorReport.printAndWriteLog(e)
        }
        sPreferenceList = SoftReference(list)
        return list
    }
}