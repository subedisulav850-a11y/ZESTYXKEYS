package com.roha.ffaessextractor

object HookScript {
    val content = """
var _keys = [];
var _ivs  = [];

function toHex(b) {
    var h = "";
    for (var i = 0; i < b.length; i++)
        h += ("0" + (b[i] & 0xFF).toString(16)).slice(-2);
    return h;
}

function toStr(b) {
    var s = "";
    for (var i = 0; i < b.length; i++) {
        var c = b[i] & 0xFF;
        s += (c >= 32 && c < 127) ? String.fromCharCode(c) : ".";
    }
    return s;
}

function alreadySeen(arr, hex) {
    for (var i = 0; i < arr.length; i++)
        if (arr[i] === hex) return true;
    return false;
}

Java.perform(function () {
    var SecretKeySpec = Java.use("javax.crypto.spec.SecretKeySpec");
    SecretKeySpec.${'$'}init.overload("[B", "java.lang.String")
    .implementation = function(keyBytes, algo) {
        if (algo.toUpperCase().indexOf("AES") !== -1) {
            var hex = toHex(keyBytes);
            if (!alreadySeen(_keys, hex)) {
                _keys.push(hex);
                console.log("╔══════════════════════════════════════╗");
                console.log("║          AES KEY FOUND               ║");
                console.log("╠══════════════════════════════════════╣");
                console.log("║ HEX : " + hex);
                console.log("║ STR : " + toStr(keyBytes));
                console.log("║ LEN : " + keyBytes.length + " bytes (" + (keyBytes.length * 8) + "-bit)");
                console.log("║ ALGO: " + algo);
                console.log("╚══════════════════════════════════════╝");
                send({type: "KEY", hex: hex, str: toStr(keyBytes), bits: keyBytes.length * 8});
            }
        }
        return this.${'$'}init(keyBytes, algo);
    };

    var IvParameterSpec = Java.use("javax.crypto.spec.IvParameterSpec");
    IvParameterSpec.${'$'}init.overload("[B")
    .implementation = function(ivBytes) {
        var hex = toHex(ivBytes);
        if (ivBytes.length >= 16 && !alreadySeen(_ivs, hex)) {
            _ivs.push(hex);
            console.log("╔══════════════════════════════════════╗");
            console.log("║          AES IV FOUND                ║");
            console.log("╠══════════════════════════════════════╣");
            console.log("║ HEX : " + hex);
            console.log("║ STR : " + toStr(ivBytes));
            console.log("║ LEN : " + ivBytes.length + " bytes");
            console.log("╚══════════════════════════════════════╝");
            send({type: "IV", hex: hex, str: toStr(ivBytes)});
        }
        return this.${'$'}init(ivBytes);
    };

    try {
        var libcrypto = Module.findBaseAddress("libcrypto.so");
        if (libcrypto) {
            var EVP_EncryptInit = Module.findExportByName("libcrypto.so", "EVP_EncryptInit_ex");
            if (EVP_EncryptInit) {
                Interceptor.attach(EVP_EncryptInit, {
                    onEnter: function(args) {
                        try {
                            var keyPtr = args[3];
                            var ivPtr  = args[4];
                            if (!keyPtr.isNull() && !ivPtr.isNull()) {
                                var keyHex = keyPtr.readByteArray(16);
                                var ivHex  = ivPtr.readByteArray(16);
                                console.log("[NATIVE KEY] " + toHex(new Uint8Array(keyHex)));
                                console.log("[NATIVE IV ] " + toHex(new Uint8Array(ivHex)));
                                send({type: "NATIVE_KEY", hex: toHex(new Uint8Array(keyHex))});
                                send({type: "NATIVE_IV",  hex: toHex(new Uint8Array(ivHex))});
                            }
                        } catch(e) {}
                    }
                });
            }
        }
    } catch(e) {}

    console.log("[+] FF AES Extractor loaded — waiting for keys...");
});
""".trimIndent()
}
