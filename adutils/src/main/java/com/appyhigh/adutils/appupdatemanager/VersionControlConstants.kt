package com.appyhigh.adutils.appupdatemanager

object VersionControlConstants {
    const val VERSION_CONTROL ="version_control"
    const val CURRENT_VERSION ="current_version"
    const val CRITICAL_VERSION ="critical_version"
    const val PACKAGE_NAME ="package_name"

    enum class UpdateType{
        SOFT_UPDATE,
        HARD_UPDATE,
        NO_UPDATE
    }
}