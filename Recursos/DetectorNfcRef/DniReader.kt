package com.oscar.detectornfc

import android.nfc.Tag
import android.nfc.TagLostException
import android.util.Log
import de.tsenger.androsmex.mrtd.DG1_Dnie
import de.tsenger.androsmex.mrtd.DG11
import de.tsenger.androsmex.mrtd.DG13
import es.gob.jmulticard.card.CryptoCardException
import es.gob.jmulticard.jse.provider.DnieProvider
import es.gob.jmulticard.jse.provider.MrtdKeyStoreImpl
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.security.GeneralSecurityException
import java.security.Security

class DniReader(private val tag: Tag?) {

    private val TAG = "DniReader"
    private val PROVIDER_NAME = "DNIeJCAProvider"

    fun readDniSync(can: String): RawNfcData {
        if (tag == null) {
            Log.e(TAG, "Tag NFC nulo")
            return RawNfcData(
                uid = null,
                can = null,
                dataGroups = emptyMap(),
                sod = null,
                sessionStatus = NfcSessionStatus.FAILED,
                sessionError = "No se detecto un tag NFC valido."
            )
        }

        val uid = tag.id.joinToString(":") { "%02X".format(it) }
        Log.i(TAG, "readDniSync() - uid=$uid, can=${maskSecret(can)}, techs=${tag.techList.joinToString()}")

        if (can.isBlank()) {
            Log.e(TAG, "CAN vacío — no se puede iniciar PACE")
            return RawNfcData(
                uid = uid,
                can = null,
                dataGroups = emptyMap(),
                sod = null,
                sessionStatus = NfcSessionStatus.FAILED,
                sessionError = "CAN vacio. No se puede iniciar la autenticacion PACE."
            )
        }

        if (!hasRequiredRuntimeDependencies()) {
            Log.e(TAG, "Faltan dependencias runtime requeridas por dniedroid/jmulticard")
            return RawNfcData(
                uid = uid,
                can = null,
                dataGroups = emptyMap(),
                sod = null,
                sessionStatus = NfcSessionStatus.FAILED,
                sessionError = "Faltan dependencias runtime para la lectura NFC."
            )
        }

        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(DnieProvider())
            Log.d(TAG, "DnieProvider registrado en Security como $PROVIDER_NAME")
        } else {
            Log.d(TAG, "Proveedor $PROVIDER_NAME ya registrado")
        }

        return try {
            Log.d(TAG, "Creando MrtdKeyStoreImpl con CAN=${maskSecret(can)}")
            val mrtdImpl = MrtdKeyStoreImpl(can, tag)
            Log.d(TAG, "Iniciando sesión PACE + lectura NFC (engineLoad)")
            mrtdImpl.engineLoad(null as InputStream?, null)
            Log.i(TAG, "Sesión NFC completada correctamente")

            val dgMap = mutableMapOf<Int, ByteArray?>()
            val dgAnalysis = mutableMapOf<Int, DataGroupInfo>()
            var fatalSessionError: String? = null

            fatalSessionError = readDg(1, dgMap, dgAnalysis) {
                (mrtdImpl.getDataGroup1() as DG1_Dnie?)?.getBytes()
            }
            if (fatalSessionError == null) {
                fatalSessionError = readDg(11, dgMap, dgAnalysis) {
                    (mrtdImpl.getDataGroup11() as DG11?)?.getBytes()
                }
            }
            if (fatalSessionError == null) {
                fatalSessionError = readDg(13, dgMap, dgAnalysis) {
                    (mrtdImpl.getDataGroup13() as DG13?)?.getBytes()
                }
            }
            if (fatalSessionError == null) {
                fatalSessionError = readDg(2, dgMap, dgAnalysis) { readDgByReflection(mrtdImpl, 2) }
            }
            if (fatalSessionError == null) {
                fatalSessionError = readDg(7, dgMap, dgAnalysis) { readDgByReflection(mrtdImpl, 7) }
            }
            if (fatalSessionError == null) {
                fatalSessionError = readDg(15, dgMap, dgAnalysis) { readDgByReflection(mrtdImpl, 15) }
            }

            if (fatalSessionError != null) {
                for (pendingDg in listOf(1, 11, 13, 2, 7, 15)) {
                    if (!dgAnalysis.containsKey(pendingDg)) {
                        dgAnalysis[pendingDg] = DataGroupInfo.skipped(pendingDg, fatalSessionError)
                    }
                }
            }

            val available = dgAnalysis.filter { it.value.status == DGStatus.READ_OK }.keys.sorted()
            val sessionStatus = when {
                fatalSessionError == null -> NfcSessionStatus.SUCCESS
                available.isNotEmpty() -> NfcSessionStatus.PARTIAL
                else -> NfcSessionStatus.FAILED
            }
            Log.i(TAG, "Lectura completada. DGs OK: $available, sessionStatus=$sessionStatus")
            RawNfcData(uid, can, dgMap, null, dgAnalysis, sessionStatus, fatalSessionError)

        } catch (e: CryptoCardException) {
            Log.e(TAG, "Error de tarjeta (CAN incorrecto o tarjeta bloqueada): ${e.message}", e)
            RawNfcData(uid, null, emptyMap(), null, sessionStatus = NfcSessionStatus.FAILED, sessionError = "Error de tarjeta. Verifica CAN o estado del documento.")
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Error de seguridad durante PACE: ${e.message}", e)
            RawNfcData(uid, null, emptyMap(), null, sessionStatus = NfcSessionStatus.FAILED, sessionError = "Fallo de seguridad durante PACE.")
        } catch (e: IOException) {
            Log.e(TAG, "Error E/S al leer DNIe (documento retirado?): ${e.message}", e)
            RawNfcData(uid, null, emptyMap(), null, sessionStatus = NfcSessionStatus.FAILED, sessionError = "Se ha perdido la conexion con el DNIe. Manten el documento inmovil y reintenta.")
        } catch (e: LinkageError) {
            Log.e(TAG, "Error de dependencias/libreria durante la lectura NFC: ${e.message}", e)
            RawNfcData(uid, null, emptyMap(), null, sessionStatus = NfcSessionStatus.FAILED, sessionError = "Error de dependencias de la libreria NFC.")
        } catch (e: RuntimeException) {
            Log.e(TAG, "Error de ejecucion durante la lectura NFC: ${e.message}", e)
            RawNfcData(uid, null, emptyMap(), null, sessionStatus = NfcSessionStatus.FAILED, sessionError = "Error de ejecucion durante la lectura NFC.")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado: ${e.javaClass.simpleName}: ${e.message}", e)
            RawNfcData(uid, null, emptyMap(), null, sessionStatus = NfcSessionStatus.FAILED, sessionError = "Error inesperado durante la lectura NFC.")
        }
    }

    private fun hasRequiredRuntimeDependencies(): Boolean {
        val requiredClasses = listOf(
            "org.bouncycastle.asn1.ASN1Encodable",
            "org.bouncycastle.asn1.DERObjectIdentifier",
            "org.bouncycastle.cms.CMSSignedData",
            "org.bouncycastle.jce.provider.BouncyCastleProvider"
        )
        for (className in requiredClasses) {
            try {
                Class.forName(className)
                Log.d(TAG, "Dependencia runtime disponible: $className")
            } catch (e: Throwable) {
                Log.e(TAG, "Dependencia runtime ausente: $className", e)
                return false
            }
        }
        return true
    }

    private fun readDg(
        dgIndex: Int,
        dgMap: MutableMap<Int, ByteArray?>,
        dgAnalysis: MutableMap<Int, DataGroupInfo>,
        readBlock: () -> ByteArray?
    ): String? {
        try {
            val bytes = readBlock()
            if (bytes != null && bytes.isNotEmpty()) {
                dgMap[dgIndex] = bytes
                dgAnalysis[dgIndex] = DataGroupInfo.read(dgIndex, bytes)
                Log.d(TAG, "DG$dgIndex leido: ${bytes.size} bytes")
            } else {
                dgAnalysis[dgIndex] = DataGroupInfo.notPresent(dgIndex)
                Log.d(TAG, "DG$dgIndex no disponible")
            }
            return null
        } catch (e: Exception) {
            val cause = unwrapThrowable(e)
            val normalized = asException(cause)
            dgAnalysis[dgIndex] = DataGroupInfo.error(dgIndex, normalized)
            return if (isFatalCommunicationError(cause)) {
                val message = "Conexion NFC perdida durante DG$dgIndex. Mantenga el documento inmovil y reintente."
                Log.e(TAG, message, cause)
                message
            } else {
                Log.w(TAG, "DG$dgIndex error: ${cause.javaClass.simpleName}: ${cause.message}")
                null
            }
        }
    }

    private fun unwrapThrowable(error: Throwable): Throwable {
        var current: Throwable = error
        while (true) {
            val next = when (current) {
                is InvocationTargetException -> current.targetException ?: current.cause
                else -> current.cause
            } ?: break
            if (next === current) break
            current = next
        }
        return current
    }

    private fun asException(error: Throwable): Exception {
        return if (error is Exception) error else RuntimeException(error.message ?: "Error", error)
    }

    private fun isFatalCommunicationError(error: Throwable): Boolean {
        if (error is TagLostException || error is IOException) return true
        val className = error.javaClass.name
        val message = error.message ?: ""
        return className.contains("TagLostException") ||
            message.contains("Tag was lost", ignoreCase = true) ||
            message.contains("Se ha perdido la conexion", ignoreCase = true)
    }

    private fun readDgByReflection(mrtdImpl: MrtdKeyStoreImpl, dgIndex: Int): ByteArray? {
        val methodCandidates = listOf("getDataGroup$dgIndex", "getDatagroup$dgIndex", "getDg$dgIndex", "getDg${dgIndex}Bytes")
        val clazz = mrtdImpl.javaClass
        for (name in methodCandidates) {
            val method = clazz.methods.firstOrNull { it.name == name && it.parameterCount == 0 } ?: continue
            val result = method.invoke(mrtdImpl) ?: continue
            val bytes = when (result) {
                is ByteArray -> result
                else -> {
                    val getBytes = result.javaClass.methods.firstOrNull { (it.name == "getBytes" || it.name == "getEncoded") && it.parameterCount == 0 }
                    getBytes?.invoke(result) as? ByteArray
                }
            }
            if (bytes != null) return bytes
        }
        return null
    }

    private fun maskSecret(value: String): String {
        if (value.isBlank()) return "<vacío>"
        if (value.length <= 2) return "*".repeat(value.length)
        return "${value.take(1)}${"*".repeat(value.length - 2)}${value.takeLast(1)}"
    }
}
