package com.proconfig.app.service

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants

object ProcessRunner {

    init {
        System.loadLibrary("exec_helper")
    }

    private external fun nativeStart(
        path: String,
        args: Array<String>,
        env: Array<String>,
        inheritFds: IntArray?
    ): Int

    fun start(path: String, args: Array<String>, inheritFds: IntArray? = null): Int {
        val env = arrayOf(
            "PATH=/system/bin:/system/xbin:/vendor/bin",
            "HOME=/",
            "ANDROID_DATA=/data",
            "ANDROID_ROOT=/system"
        )
        return try {
            nativeStart(path, args, env, inheritFds)
        } catch (t: Throwable) {
            -1
        }
    }

    fun isRunning(pid: Int): Boolean {
        if (pid <= 0) return false
        return try {
            Os.kill(pid, 0)
            true
        } catch (e: ErrnoException) {
            false
        } catch (t: Throwable) {
            false
        }
    }

    fun kill(pid: Int) {
        if (pid <= 0) return
        try {
            Os.kill(pid, OsConstants.SIGKILL)
        } catch (_: Throwable) {
        }
        try {
            android.os.Process.killProcess(pid)
        } catch (_: Throwable) {
        }
    }
}
