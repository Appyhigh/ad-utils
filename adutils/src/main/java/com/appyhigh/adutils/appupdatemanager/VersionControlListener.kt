package com.appyhigh.adutils.appupdatemanager

interface VersionControlListener {
    fun onUpdateDetectionSuccess(updateType: VersionControlConstants.UpdateType)
}