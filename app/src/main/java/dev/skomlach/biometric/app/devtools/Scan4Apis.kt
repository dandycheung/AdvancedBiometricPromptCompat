/*
 *  Copyright (c) 2023 Sergey Komlach aka Salat-Cx65; Original project https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
 *  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package dev.skomlach.biometric.app.devtools

import androidx.annotation.WorkerThread
import dev.skomlach.biometric.app.BuildConfig
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.e
import dev.skomlach.common.contextprovider.AndroidContext
import dev.skomlach.common.network.NetworkApi
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStream
import java.io.StringWriter
import java.util.Arrays
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.Array
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Comparator
import kotlin.Exception
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throwable
import kotlin.Throws
import kotlin.arrayOf
import kotlin.plus

class Scan4Apis {
    @WorkerThread
    fun getList(): String? {
        try {
            val context = AndroidContext.appContext
            val file = File(context.cacheDir, "deviceapi.log")
            if (file.exists())
                file.delete()

            val cache = File(context.cacheDir, UUID.randomUUID().toString())
            val fileWriter = FileWriter(file)
            val writer = StringWriter()
            cache.mkdirs()
            try {
                val paths = HashSet<String>()
                val bootPath = splitString(System.getProperty("java.boot.class.path"), ":/")

                paths.addAll(bootPath.toList())

                val javaPath = splitString(System.getProperty("java.library.path"), ":/")
                paths.addAll(javaPath.toList())
                val javaHomePath = splitString(System.getProperty("java.home"), ":/")
                paths.addAll(javaHomePath.toList())
                val userDir = splitString(System.getProperty("user.dir"), ":/")
                paths.addAll(userDir.toList())

                val path = HashSet<File>()
                for (p in paths) {
                    path.add(File(p))
                }

                val roots = File.listRoots()
                for (r in roots) {
                    path.add(r)
                }

                val jars = HashSet<String>()
                for (r in path) {
                    if (r.isDirectory) {
                        BiometricLoggerImpl.d("Scan4Apis.check path $r")
                        scanRecursivly(r, jars)
                    } else
                        if (r.isFile)
                            jars.add(r.absolutePath)
                }
                val stringBuilder = StringBuilder("\n\n")
                for (s in jars) {
                    val dexInternalStoragePath = File(
                        cache,
                        File(s).name
                    )
                    try {
                        if (dexInternalStoragePath.exists()) dexInternalStoragePath.delete()
                        try {
                            FileInputStream(s).use { bis ->
                                FileOutputStream(dexInternalStoragePath).use { dexWriter ->
                                    NetworkApi.fastCopy(bis, dexWriter)
                                    dexWriter.flush()
                                }
                            }
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        }
                        val dexes: MutableList<File> = ArrayList()
                        if (isZipFileWithDex(dexInternalStoragePath)) {
                            dexes.addAll(extractedDex(dexInternalStoragePath, cache))
                        } else if (!isZipFile(dexInternalStoragePath)) {
                            dexes.add(dexInternalStoragePath)
                        }
                        var counter = 0
                        val sb = StringBuilder()
                        for (f in dexes) {
                            val dexBackedDexFile =
                                DexFileFactory.loadDexFile(f, Opcodes.getDefault())
                            for (d in dexBackedDexFile.classes) {
                                val type = d.type
                                val typeCopy = type.lowercase(Locale.ROOT)
                                if (typeCopy.contains("/fingerprint") || typeCopy.contains("/biometric") ||
                                    typeCopy.contains("/face") || typeCopy.contains("/iris")
                                /*|| typeCopy.contains("/voice") || typeCopy.contains("/heart")*/
                                ) {
                                    if (!typeCopy.contains("/facebook") && !typeCopy.contains("androidx/") && !typeCopy.contains(
                                            "/support/"
                                        )
                                    ) {
                                        try {
                                            val clzName =
                                                type.substring(1, type.length - 1).replace("/", ".")
                                            Class.forName(clzName)
                                            sb.append(type).append("\n")
                                            if (counter == 0) {
                                                writer.write("\n-------------------------\n")
                                                writer.write(s)
                                                writer.write("\n")
                                            }
                                            writer.write(type + "\n")
                                            counter++
                                        } catch (ignore: Throwable) {

                                        }

                                    }
                                }
                            }
                            System.gc()
                            f.delete()
                        }
                        if (counter > 0) {
                            stringBuilder.append("\n-------------------------\n")
                            stringBuilder.append(s)
                            stringBuilder.append("\n")
                            stringBuilder.append(sb.toString())
                            stringBuilder.append("\n")
                            writer.write("\n")
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    } finally {
                        dexInternalStoragePath.delete()
                    }
                }
                return stringBuilder.toString()
            } finally {
                deleteRecursive(cache)
                fileWriter.write(writer.toString())
                fileWriter.close()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return null
    }

    private fun isZipFile(fileZip: File): Boolean {
        try {
            val zis = ZipInputStream(FileInputStream(fileZip))
            val zipEntry = zis.nextEntry
            val isZip = zipEntry != null
            if (isZip) {
                zis.closeEntry()
            }
            zis.close()
            return isZip
        } catch (ignore: Throwable) {
        }
        return false
    }

    private fun isZipFileWithDex(fileZip: File): Boolean {
        try {
            val zipFile = ZipFile(fileZip)
            try {
                val entries = zipFile.entries()
                // iterate through all the entries
                while (entries.hasMoreElements()) {
                    // get the zip entry
                    val name = entries.nextElement()
                    if (name.name.lowercase(Locale.ROOT).endsWith(".dex")) return true
                }
            } finally {
                try {
                    zipFile.close()
                } catch (ignore: IOException) {
                }
            }
        } catch (ignore: Throwable) {
        }
        return false
    }

    @Throws(Exception::class)
    private fun extractedDex(fileZip: File, destDir: File): List<File> {
        val files: MutableList<File> = ArrayList()
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(fileZip)
            val entries = zipFile.entries()
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            val zipEntries: MutableList<ZipEntry> = ArrayList()

            // iterate through all the entries
            while (entries.hasMoreElements()) {
                // get the zip entry
                val name = entries.nextElement()
                if (name.name.lowercase(Locale.ROOT).endsWith(".dex")) zipEntries.add(name)
            }
            zipEntries.sortWith(Comparator { o1, o2 -> o1.name.compareTo(o2.name) })
            for (zipEntry in zipEntries) {
                if (!zipEntry.isDirectory) {
                    val file = File(destDir, fileZip.name + "_" + zipEntry.name.replace("/", "-"))
                    if (file.exists()) file.delete()
                    val fos = FileOutputStream(file)
                    NetworkApi.fastCopy(zipFile.getInputStream(zipEntry), fos)
                    fos.flush()
                    files.add(file)
                }
            }
            zipFile.close()
        } finally {
            try {
                zipFile?.close()
            } catch (ignore: IOException) {
            }
        }
        return files
    }

    private fun scanRecursivly(fileOrDirectory: File?, filesOut: HashSet<String>) {
        try {
            if (fileOrDirectory == null || !fileOrDirectory.exists() || fileOrDirectory.name.contains(
                    BuildConfig.APPLICATION_ID
                ) || fileOrDirectory.absolutePath.startsWith(
                    "/proc/"
                )
            ) return
            if (fileOrDirectory.isDirectory) {
                val files = fileOrDirectory.listFiles()
                if (files != null && files.isNotEmpty()) {
                    for (child in files) {
                        scanRecursivly(child, filesOut)
                    }
                }
            } else {
                val name = fileOrDirectory.name.lowercase(Locale.ROOT)
                if (name.endsWith(".odex") ||
                    //name.endsWith(".vdex") ||//??????
                    name.endsWith(".dex") ||
                    name.endsWith(".aot") ||
                    name.endsWith(".apk") ||
                    name.endsWith(".zip") ||
                    name.endsWith(".jar")
                ) filesOut.add(fileOrDirectory.absolutePath)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun deleteRecursive(fileOrDirectory: File?) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) return
        if (fileOrDirectory.isDirectory) {
            val files = fileOrDirectory.listFiles()
            if (files != null && files.isNotEmpty()) {
                for (child in files) {
                    deleteRecursive(child)
                }
            }
        }
        safeDelete(fileOrDirectory)
    }

    private fun safeDelete(fileOrDirectory: File?): Boolean {
        try {
            if (fileOrDirectory == null || !fileOrDirectory.exists()) {
                return true
            }
            try {
                if (fileOrDirectory.isFile) {
                    val size = fileOrDirectory.length()
                    if (size > 0) {
                        //When you delete a file using file.delete(), only the reference to the file is removed from the filesystem table.
                        // The file still exists on disk until other data overwrites it, leaving it vulnerable to recover
                        val data = ByteArray(
                            Math.min((2 * 1024 * 1024).toLong(), size).toInt()
                        )
                        Arrays.fill(data, 0x00.toByte())
                        val fileOutputStream: OutputStream =
                            FileOutputStream(fileOrDirectory, false)
                        val bufferedOutputStream = BufferedOutputStream(fileOutputStream, data.size)
                        var total = 0
                        while (total < size) {
                            bufferedOutputStream.write(data)
                            total += data.size
                        }
                        bufferedOutputStream.flush()
                        bufferedOutputStream.close()
                        fileOutputStream.close()
                    }
                }
            } catch (e: Throwable) {
                e(e)
            }
            if (fileOrDirectory.delete()) {
                return true
            }
        } catch (e: Throwable) {
            e(e)
        }

        //fallback
        try {
            val p = Runtime.getRuntime().exec(findPathForBinary("sh") + " -")
            val os = DataOutputStream(p.outputStream)
            try {
                if (fileOrDirectory?.isDirectory == true) os.writeBytes(
                    """
    rm -f ${fileOrDirectory.absolutePath}
    
    """.trimIndent()
                ) //remove file
                else os.writeBytes(
                    """
    rm -f -d ${fileOrDirectory?.absolutePath}
    
    """.trimIndent()
                ) //remove empty dir
            } catch (ignored: Exception) {
            }
            os.writeBytes("exit\n")
            os.close()
            p.destroy()
            return fileOrDirectory?.exists() == false
        } catch (e: Throwable) {
            e(e)
        }
        return false
    }

    private fun findPathForBinary(binaryName: String): String? {
        val places = arrayOf(
            "/sbin/",
            "/system/bin/",
            "/system/xbin/",
            "/data/local/xbin/",
            "/data/local/bin/",
            "/system/sd/xbin/",
            "/system/bin/failsafe/",
            "/data/local/"
        )
        for (where in places) {
            val target = File(where + binaryName)
            if (target.exists() && !target.isDirectory && target.canExecute()) {
                return where + binaryName
            }
        }
        return null
    }

    private fun splitString(str: String?, delimiter: String?): Array<String> {
        if (str.isNullOrEmpty()) {
            return arrayOf()
        }
        if (delimiter.isNullOrEmpty()) {
            return arrayOf(str)
        }
        val list = ArrayList<String>()
        var start = 0
        var end = str.indexOf(delimiter, start)
        while (end != -1) {
            list.add(str.substring(start, end))
            start = end + delimiter.length
            end = str.indexOf(delimiter, start)
        }
        list.add(str.substring(start))
        return list.toTypedArray()
    }

}