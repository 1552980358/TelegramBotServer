package com.tg.huli

class Utils {

    companion object {

        private const val MiB = 1048576

        fun fetchJarPath(): String {
            var path = this::class.java
                .protectionDomain
                .codeSource
                .location
                .toURI()
                .path
            if (System.getProperty("os.name").contains("dows")) {
                path = path.substring(1, path.length)
            }
            if (path.contains("jar")) {
                path = path.substring(0, path.lastIndexOf("."))
                return path.substring(0, path.lastIndexOf("/"))
            }
            return path.replace("target/classes/", "")
        }

        fun getAsMiB(string: String): Int {
            return string.toInt() / MiB
        }

    }

}