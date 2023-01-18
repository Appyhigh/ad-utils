package com.appyhigh.adutils.callbacks

interface VersionControlCallback {
    fun OnSoftUpdate()
    fun OnHardUpdate()

}

interface VersionControllCallbackForced {
    fun OnHardUpdate()
}